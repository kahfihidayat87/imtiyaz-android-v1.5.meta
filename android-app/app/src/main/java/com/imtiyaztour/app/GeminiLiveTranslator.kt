package com.imtiyaztour.app

import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import android.Manifest
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder
import android.util.Base64
import android.util.Log
import kotlinx.coroutines.*
import okhttp3.*
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
    private var isRecording = false
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
                        "model": "models/gemini-3.5-live-translate-preview",
                        "generationConfig": {
                          "responseModalities": ["AUDIO"],
                          "translationConfig": {
                            "targetLanguageCode": "ar",
                            "echoTargetLanguage": false
                          }
                        },
                        "inputAudioTranscription": {},
                        "outputAudioTranscription": {},
                        "sessionResumption": {}
                      }
                    }
                """.trimIndent()
                webSocket.send(setupJson)
                startAudioRecording()
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                try {
                    val json = JSONObject(text)
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

    private fun startAudioRecording() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            onError("Izin mikrofon belum diberikan.")
            return
        }

        var sampleRate = 16000
        var bufferSize = AudioRecord.getMinBufferSize(sampleRate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)
        var record = AudioRecord(MediaRecorder.AudioSource.MIC, sampleRate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, bufferSize)

        if (record.state != AudioRecord.STATE_INITIALIZED) {
            Log.w("GeminiLive", "Fallback ke 44100 Hz")
            record.release()
            sampleRate = 44100
            bufferSize = AudioRecord.getMinBufferSize(sampleRate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)
            record = AudioRecord(MediaRecorder.AudioSource.MIC, sampleRate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, bufferSize)
        }

        if (record.state != AudioRecord.STATE_INITIALIZED) {
            record.release()
            onError("Gagal menginisialisasi mikrofon.")
            return
        }

        audioRecord = record

        val outBufferSize = AudioTrack.getMinBufferSize(24000, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT)
        audioTrack = AudioTrack(
            AudioManager.STREAM_MUSIC, 24000, AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT, outBufferSize, AudioTrack.MODE_STREAM
        ).apply { play() }

        try {
            audioRecord?.startRecording()
            isRecording = true
        } catch (e: Exception) {
            onError("Gagal memulai perekaman: ${e.message}")
            return
        }

        scope.launch {
            val buffer = ShortArray(bufferSize / 2)
            while (isRecording) {
                val read = audioRecord?.read(buffer, 0, buffer.size) ?: 0
                if (read > 0) {
                    val bytes = shortArrayToByteArray(buffer.copyOf(read))
                    val base64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
                    val msg = """{"realtimeInput":{"mediaChunks":[{"mimeType":"audio/pcm;rate=${sampleRate}","data":"$base64"}]}}"""
                    webSocket?.send(msg)
                }
            }
        }
    }

    private fun playAudioChunk(pcmData: ByteArray) {
        audioTrack?.write(pcmData, 0, pcmData.size)
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
        try { audioRecord?.stop(); audioRecord?.release() } catch (_: Exception) {}
        try { audioTrack?.stop(); audioTrack?.release() } catch (_: Exception) {}
        audioRecord = null; audioTrack = null
        webSocket?.close(1000, "User stopped")
        webSocket = null
        scope.cancel()
    }
}
