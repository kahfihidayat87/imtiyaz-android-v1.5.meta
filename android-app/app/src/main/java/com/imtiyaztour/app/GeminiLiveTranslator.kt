package com.imtiyaztour.app

import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import android.Manifest
import kotlinx.coroutines.Job
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder
import android.util.Base64
import android.util.Log
import kotlinx.coroutines.*
import okhttp3.*
import okio.ByteString
import okhttp3.logging.HttpLoggingInterceptor
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class GeminiLiveTranslator(
    private val context: Context,
    private val ephemeralToken: String,
    private val onTranscript: (String, Boolean) -> Unit,
    private val onError: (String) -> Unit
) {
    private val client = OkHttpClient.Builder()
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .pingInterval(15, TimeUnit.SECONDS)
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        })
        .build()

    private var webSocket: WebSocket? = null
    private var audioRecord: AudioRecord? = null
    private var audioTrack: AudioTrack? = null
    @Volatile private var isRecording = false
    private var audioJob: Job? = null
    private var isPlayingAudio = false
    private var playJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    fun connect() {
        val url = "wss://generativelanguage.googleapis.com/ws/google.ai.generativelanguage.v1beta.GenerativeService.BidiGenerateContentConstrained?access_token=$ephemeralToken"
        val request = Request.Builder().url(url).build()

        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.d("GeminiLive", "WebSocket opened")
                val setupJson = """
                    {
                      "setup": {
                        "model": "models/gemini-2.5-flash-native-audio-latest",
                        "generationConfig": {
                          "responseModalities": ["AUDIO"],
                          "speechConfig": {
                            "voiceConfig": {
                              "prebuiltVoiceConfig": {
                                "voiceName": "Puck"
                              }
                            }
                          }
                        },
                        "systemInstruction": {
                          "parts": [
                            {
                              "text": "Anda adalah mesin penerjemah suara. Tugas Anda HANYA menerjemahkan. Jika input Bahasa Indonesia, output WAJIB Bahasa Arab. Jika input Bahasa Arab, output WAJIB Bahasa Indonesia. DILARANG KERAS menggunakan bahasa Inggris. Jangan pernah menjawab pertanyaan atau mengobrol. Cukup berikan terjemahan suaranya saja."
                            }
                          ]
                        },
                        "inputAudioTranscription": {},
                        "outputAudioTranscription": {},
                        "realtimeInputConfig": {
                          "automaticActivityDetection": {
                            "disabled": false
                          }
                        }
                      }
                    }
                """.trimIndent().trimIndent().trimIndent().trimIndent().trimIndent().trimIndent().trimIndent().trimIndent().trimIndent().trimIndent()
                Log.d("GeminiLive", "Mengirim Setup JSON: $setupJson")
                webSocket.send(setupJson)
                // Tunggu setupComplete dari server sebelum mulai audio
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                Log.d("GeminiLive", "Pesan diterima: $text")
                try {
                    val json = JSONObject(text)
                    if (json.has("setupComplete")) {
                        Log.d("GeminiLive", "Setup Complete! Memulai mikrofon...")
                        // startAudioRecording() dipanggil dari UI saat tombol ditekan
                        return
                    }
                    if (json.has("serverContent")) {
                        val serverContent = json.getJSONObject("serverContent")
                        if (serverContent.has("inputTranscription")) {
                            val t = serverContent.getJSONObject("inputTranscription").optString("text")
                            if (t.isNotBlank()) onTranscript(t, true)
                        }
                        if (serverContent.has("outputTranscription")) {
                            val t = serverContent.getJSONObject("outputTranscription").optString("text")
                            if (t.isNotBlank()) onTranscript(t, false)
                        }
                        // Handle interruption
                        if (serverContent.has("interrupted") && serverContent.getBoolean("interrupted")) {
                            audioTrack?.pause()
                            audioTrack?.flush()
                            audioTrack?.play()
                            Log.d("GeminiLive", "Audio interrupted, buffer flushed")
                        }
                        if (serverContent.has("modelTurn")) {
                            val parts = serverContent.getJSONObject("modelTurn").optJSONArray("parts")
                            if (parts != null) {
                                for (i in 0 until parts.length()) {
                                    val part = parts.getJSONObject(i)
                                    if (part.has("inlineData")) {
                                        val data = part.getJSONObject("inlineData").getString("data")
                                        playAudioChunk(Base64.decode(data, Base64.DEFAULT))
                                    }
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e("GeminiLive", "Parse error", e)
                }
            }
            // ✅ TAMBAHAN: Handler untuk frame biner dari Gemini Live API
            override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
                try {
                    val text = bytes.utf8()
                    Log.d("GeminiLive", "Pesan biner diterima: $text")
                    // Proses JSON yang sama seperti handler teks
                    val json = JSONObject(text)
                    if (json.has("setupComplete")) {
                        Log.d("GeminiLive", "Setup Complete! Memulai mikrofon...")
                        // startAudioRecording() dipanggil dari UI saat tombol ditekan
                        return
                    }
                    if (json.has("serverContent")) {
                        val serverContent = json.getJSONObject("serverContent")
                        if (serverContent.has("inputTranscription")) {
                            val t = serverContent.getJSONObject("inputTranscription").optString("text")
                            if (t.isNotBlank()) onTranscript(t, true)
                        }
                        if (serverContent.has("outputTranscription")) {
                            val t = serverContent.getJSONObject("outputTranscription").optString("text")
                            if (t.isNotBlank()) onTranscript(t, false)
                        }
                        if (serverContent.has("modelTurn")) {
                            val parts = serverContent.getJSONObject("modelTurn").optJSONArray("parts")
                            if (parts != null) {
                                for (i in 0 until parts.length()) {
                                    val part = parts.getJSONObject(i)
                                    if (part.has("inlineData")) {
                                        val data = part.getJSONObject("inlineData").getString("data")
                                        playAudioChunk(Base64.decode(data, Base64.DEFAULT))
                                    }
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e("GeminiLive", "Parse binary error", e)
                }
            }


            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                Log.w("GeminiLive", "WebSocket Closing: code=$code, reason=$reason")
                webSocket.close(1000, null)
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                Log.w("GeminiLive", "WebSocket Closed: code=$code, reason=$reason")
                stop()
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.e("GeminiLive", "WebSocket Failure: ${t.message}", t)
                if (response != null) {
                    Log.e("GeminiLive", "Response Code: ${response.code}")
                    try {
                        Log.e("GeminiLive", "Response Body: ${response.body?.string()}")
                    } catch (e: Exception) {}
                }
                onError("Koneksi terputus: ${t.message}")
                stop()
            }
        })
    }

    fun startAudioRecording() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            onError("Izin mikrofon belum diberikan.")
            return
        }

        // Release resource lama jika ada
        try { audioRecord?.stop(); audioRecord?.release() } catch (_: Exception) {}
        try { audioTrack?.stop(); audioTrack?.release() } catch (_: Exception) {}
        audioRecord = null
        audioTrack = null

        val sampleRate = 16000
        val bufferSize = AudioRecord.getMinBufferSize(sampleRate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)
        if (bufferSize <= 0) {
            onError("Gagal mendapatkan buffer size yang valid.")
            return
        }

        val record = AudioRecord(MediaRecorder.AudioSource.MIC, sampleRate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, bufferSize)
        if (record.state != AudioRecord.STATE_INITIALIZED) {
            record.release()
            onError("Gagal menginisialisasi mikrofon.")
            return
        }
        audioRecord = record

        val outBufferSize = AudioTrack.getMinBufferSize(24000, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT)
        val track = AudioTrack(
            android.media.AudioManager.STREAM_MUSIC, 24000, AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT, outBufferSize, AudioTrack.MODE_STREAM
        )
        track.play()
        audioTrack = track

        try {
            record.startRecording()
            isRecording = true
        } catch (e: Exception) {
            onError("Gagal memulai perekaman: ${e.message}")
            return
        }

        audioJob = scope.launch(Dispatchers.IO) { // WAJIB Dispatchers.IO untuk blocking call
            val buffer = ShortArray(bufferSize / 2)
            val localRecord = audioRecord // Local reference untuk hindari race condition
            while (isRecording && localRecord != null) {
                val read = localRecord.read(buffer, 0, buffer.size)
                if (read > 0) {
                    val bytes = shortArrayToByteArray(buffer.copyOf(read))
                    val base64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
                    val msg = """{"realtimeInput":{"mediaChunks":[{"mimeType":"audio/pcm;rate=${sampleRate}","data":"$base64"}]}}"""
                    webSocket?.send(msg)
                    Log.d("GeminiLive", "Mengirim Audio Chunk (${read} bytes)")
                }
            }
        }
    }

    fun stopRecording() {
        isRecording = false
        
        scope.launch(Dispatchers.IO) {
            // Gunakan join() agar chunk terakhir dipastikan selesai dikirim
            audioJob?.join()
            audioJob = null
            
            try { audioRecord?.stop(); audioRecord?.release() } catch (_: Exception) {}
            audioRecord = null
            
            Log.d("GeminiLive", "Mikrofon dihentikan, menunggu VAD server...")
        }
    }

    

    private fun playAudioChunk(pcmData: ByteArray) {
        isPlayingAudio = true
        audioTrack?.write(pcmData, 0, pcmData.size)
        
        // Reset timer setiap kali chunk baru masuk. 
        // Jika tidak ada chunk baru selama 800ms, baru izinkan mikrofon merekam lagi.
                playJob = scope.launch {
            delay(800)
            isPlayingAudio = false
        }
    }

    private fun shortArrayToByteArray(shorts: ShortArray): ByteArray {
        val bytes = ByteArray(shorts.size * 2)
        for (i in shorts.indices) {
            bytes[i * 2] = (shorts[i].toInt() and 0xFF).toByte()
            bytes[i * 2 + 1] = ((shorts[i].toInt() shr 8) and 0xFF).toByte()
        }
        return bytes
    }

    fun stop() {
        isRecording = false
                
        // Cleanup AEC dan AudioManager
                
        try { audioRecord?.stop(); audioRecord?.release() } catch (_: Exception) {}
        try { audioTrack?.stop(); audioTrack?.release() } catch (_: Exception) {}
        audioRecord = null; audioTrack = null
        webSocket?.close(1000, "User stopped")
        webSocket = null
        scope.cancel()
    }
}
