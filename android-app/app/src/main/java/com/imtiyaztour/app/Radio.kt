package com.imtiyaztour.app

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.MediaRecorder
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale

// ============================================================================
// RADIO (WALKIE-TALKIE) TOUR LEADER -- MODEL SIARAN
//
// CATATAN JUJUR SOAL BATASAN: ini model "push-to-talk" ala Zello/voice-note
// instan -- BUKAN audio streaming langsung sungguhan seperti HT/radio fisik.
// Tour Leader tekan-tahan tombol untuk merekam, lepas untuk kirim; jamaah
// menerimanya lewat polling setiap ~2,5 detik (ada delay singkat, bukan
// sepersekian detik). Audio streaming real-time sungguhan butuh infrastruktur
// WebRTC + server TURN/SFU yang jauh lebih besar dari stack WordPress+Node ini
// -- kalau ke depan dibutuhkan itu, itu proyek infrastruktur terpisah.
//
// FIX: sebelumnya semua jamaah bisa "bicara" (model grup walkie-talkie ramai).
// Sekarang model diubah jadi SIARAN satu-arah sesuai permintaan: 1 Tour Leader
// bicara, jamaah mendengarkan. Yang bisa bicara HANYA yang tahu "Kode Radio TL"
// (diatur Admin per paket di WP Admin, tab Paket Umrah) -- dicek di SERVER, jadi
// tidak bisa dilewati dengan sekadar mengedit tampilan aplikasi. Kalau Admin
// tidak mengisi kode untuk suatu paket, kanal itu tetap terbuka (siapa saja
// boleh bicara) -- supaya kompatibel untuk rombongan yang belum diatur kodenya.
//
// Kanal ditentukan otomatis dari paket_id jamaah (di-resolve server dari token),
// jadi serombongan satu paket otomatis satu kanal radio, tanpa perlu isi kode
// kanal manual, dan tidak bisa dengar kanal rombongan lain.
// ============================================================================

data class RadioSendResponse(val success: Boolean? = null, val channel: String? = null, val error: String? = null)
data class RadioMessage(val id: String, val nama: String, val time: Long, val audio: String? = null)
data class RadioPollRequest(val jamaah_id: String, val token: String, val after: Long)
data class RadioPollResponse(val channel: String? = null, val nama_saya: String? = null, val server_time: Long? = null, val requires_tl_code: Boolean? = null, val messages: List<RadioMessage> = emptyList(), val error: String? = null)

interface RadioApiService {
    @Multipart
    @POST("api/radio/send")
    suspend fun send(
        @Part("jamaah_id") jamaahId: okhttp3.RequestBody,
        @Part("token") token: okhttp3.RequestBody,
        @Part("tl_code") tlCode: okhttp3.RequestBody,
        @Part audio: MultipartBody.Part
    ): RadioSendResponse

    @POST("api/radio/poll")
    suspend fun poll(@Body body: RadioPollRequest): RadioPollResponse
}

object RadioApiClient {
    val service: RadioApiService by lazy {
        Retrofit.Builder()
            .baseUrl(ApiConfig.BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(RadioApiService::class.java)
    }
}

// Merekam klip suara pendek pakai MediaRecorder bawaan Android (tanpa dependency baru).
class RadioRecorder(private val context: Context) {
    private var recorder: MediaRecorder? = null
    private var outputFile: File? = null
    var startTime: Long = 0L

    fun start(): Boolean {
        return try {
            val dir = File(context.cacheDir, "radio").apply { mkdirs() }
            val file = File(dir, "radio_${System.currentTimeMillis()}.m4a")
            outputFile = file
            @Suppress("DEPRECATION")
            recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) MediaRecorder(context) else MediaRecorder()
            recorder?.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioEncodingBitRate(48000)
                setAudioSamplingRate(22050)
                setMaxDuration(30000) // maksimal 30 detik per klip
                setOutputFile(file.absolutePath)
                prepare()
                start()
            }
            startTime = System.currentTimeMillis()
            true
        } catch (e: Exception) {
            recorder = null
            false
        }
    }

    /** @return File hasil rekaman, atau null kalau gagal / terlalu pendek (< 400ms, dianggap salah pencet). */
    fun stop(): File? {
        val durationMs = System.currentTimeMillis() - startTime
        return try {
            recorder?.stop(); recorder?.release()
            recorder = null
            if (durationMs < 400) { outputFile?.delete(); null } else outputFile
        } catch (e: Exception) {
            try { recorder?.release() } catch (e2: Exception) {}
            recorder = null
            outputFile?.delete()
            null
        }
    }

    fun cancel() {
        try { recorder?.stop(); recorder?.release() } catch (e: Exception) {}
        recorder = null
        outputFile?.delete()
        outputFile = null
    }
}

private fun formatWaktu(millis: Long): String = SimpleDateFormat("HH:mm", Locale("id","ID")).format(millis)

@Composable
fun RadioScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    if (!Prefs.isLoggedIn(context)) {
        Column(Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Text("Radio hanya bisa diakses setelah login", fontWeight = FontWeight.Bold, fontSize = 16.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            Spacer(Modifier.height(8.dp))
            Text("Silakan login lewat tab Saya terlebih dahulu.", fontSize = 12.sp, color = Color.Gray, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            Spacer(Modifier.height(16.dp))
            Button(onClick = onBack, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F7A5A))) { Text("← Kembali") }
        }
        return
    }

    val jamaahId = Prefs.getJamaahId(context)
    val token = Prefs.getToken(context)

    var hasMicPermission by remember {
        mutableStateOf(context.checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED)
    }
    val micPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        hasMicPermission = granted
    }

    var channel by remember { mutableStateOf("") }
    var requiresTlCode by remember { mutableStateOf(false) }
    var messages by remember { mutableStateOf(listOf<RadioMessage>()) }
    var lastAfter by remember { mutableStateOf(0L) }
    var myName by remember { mutableStateOf("") }
    var isRecording by remember { mutableStateOf(false) }
    var isSending by remember { mutableStateOf(false) }
    var statusMsg by remember { mutableStateOf("") }
    val playedIds = remember { mutableSetOf<String>() }
    val recorder = remember { RadioRecorder(context) }

    // Status "TL": kosong = belum coba jadi TL, isi = kode yang dipakai jamaah ini
    // untuk mencoba bicara. Server yang memutuskan valid/tidak, ini cuma input lokal.
    var tlCodeInput by remember { mutableStateOf("") }
    var isTl by remember { mutableStateOf(false) }
    var tlError by remember { mutableStateOf("") }

    // Polling loop -- ini yang menggantikan "streaming langsung" (lihat catatan di atas).
    LaunchedEffect(Unit) {
        while (isActive) {
            try {
                val resp = withContext(Dispatchers.IO) { RadioApiClient.service.poll(RadioPollRequest(jamaahId, token, lastAfter)) }
                channel = resp.channel ?: channel
                myName = resp.nama_saya ?: myName
                requiresTlCode = resp.requires_tl_code ?: false
                if (resp.messages.isNotEmpty()) {
                    messages = (messages + resp.messages).takeLast(30)
                    lastAfter = resp.messages.maxOf { it.time }
                    // Auto-play pesan baru dari orang lain (bukan pesan sendiri, untuk hindari "gema")
                    for (m in resp.messages) {
                        if (m.id !in playedIds && m.nama != myName && m.audio != null) {
                            playedIds.add(m.id)
                            try {
                                val dir = File(context.cacheDir, "radio").apply { mkdirs() }
                                val f = File(dir, "recv_${m.id}.m4a")
                                f.writeBytes(android.util.Base64.decode(m.audio, android.util.Base64.DEFAULT))
                                AudioPlayerManager.toggle(f.absolutePath)
                            } catch (e: Exception) {}
                        } else {
                            playedIds.add(m.id)
                        }
                    }
                }
                statusMsg = ""
            } catch (e: Exception) {
                statusMsg = "Tidak terhubung ke server -- akan mencoba lagi"
            }
            kotlinx.coroutines.delay(2500)
        }
    }

    fun sendClip(file: File) {
        isSending = true
        scope.launch {
            try {
                val idBody = jamaahId.toRequestBody("text/plain".toMediaTypeOrNull())
                val tokenBody = token.toRequestBody("text/plain".toMediaTypeOrNull())
                val codeBody = tlCodeInput.toRequestBody("text/plain".toMediaTypeOrNull())
                val reqFile = file.asRequestBody("audio/mp4".toMediaTypeOrNull())
                val part = MultipartBody.Part.createFormData("audio", file.name, reqFile)
                withContext(Dispatchers.IO) { RadioApiClient.service.send(idBody, tokenBody, codeBody, part) }
                tlError = ""
            } catch (e: Exception) {
                // Kalau kode TL ternyata salah, server menolak (403) -- turunkan lagi ke mode dengar.
                isTl = false
                tlError = "Kode Radio TL salah atau kosong -- hanya Tour Leader yang bisa bicara di kanal ini"
                statusMsg = ""
            }
            isSending = false
            file.delete()
        }
    }

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    LaunchedEffect(isPressed) {
        if (isPressed) {
            if (!hasMicPermission) {
                micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                return@LaunchedEffect
            }
            if (recorder.start()) isRecording = true
        } else {
            if (isRecording) {
                isRecording = false
                val file = recorder.stop()
                if (file != null) sendClip(file)
            }
        }
    }

    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onBack) { Text("← Kembali") }
        }
        Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
            Text("Radio Tour Leader", fontWeight = FontWeight.Bold, fontSize = 20.sp)
            Text(
                if (channel.isNotEmpty()) "Kanal siaran rombongan paket \"$channel\"" else "Menghubungkan ke kanal...",
                fontSize = 12.sp, color = Color.Gray
            )
            if (statusMsg.isNotEmpty()) Text(statusMsg, fontSize = 11.sp, color = Color(0xFFDC2626))
        }
        Spacer(Modifier.height(8.dp))

        LazyColumn(Modifier.weight(1f).padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (messages.isEmpty()) {
                item { Text("Belum ada suara masuk di kanal ini.", fontSize = 12.sp, color = Color.Gray, modifier = Modifier.padding(top = 16.dp)) }
            }
            items(messages) { m ->
                Card(shape = RoundedCornerShape(10.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(1.dp), modifier = Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Mic, contentDescription = null, tint = Color(0xFF0F7A5A), modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(m.nama, fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.weight(1f))
                        Text(formatWaktu(m.time), fontSize = 10.sp, color = Color.Gray)
                    }
                }
            }
        }

        // Kanal terbuka (Admin belum isi Kode Radio TL untuk paket ini) -> semua orang
        // boleh bicara, tombol PTT langsung tampil (perilaku lama, tetap kompatibel).
        if (!requiresTlCode) {
            PushToTalkButton(isRecording, isSending, interactionSource)
        }
        // Kanal siaran (Admin sudah isi kode) & belum jadi TL -> tampilkan gerbang kode,
        // bukan tombol bicara. Jamaah biasa tinggal dengar saja tanpa perlu isi apa pun.
        else if (!isTl) {
            Column(Modifier.fillMaxWidth().padding(16.dp)) {
                Text("Kanal siaran satu-arah -- hanya Tour Leader yang bisa bicara di kanal ini.", fontSize = 11.sp, color = Color.Gray)
                if (tlError.isNotEmpty()) { Spacer(Modifier.height(4.dp)); Text(tlError, fontSize = 11.sp, color = Color(0xFFDC2626)) }
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = tlCodeInput, onValueChange = { tlCodeInput = it },
                        label = { Text("Kode Radio TL (khusus Tour Leader)") },
                        modifier = Modifier.weight(1f), singleLine = true, shape = RoundedCornerShape(8.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = { if (tlCodeInput.isNotBlank()) { isTl = true; tlError = "" } },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F7A5A))
                    ) { Text("Aktifkan") }
                }
            }
        }
        // Sudah masukkan kode -> tampilkan tombol bicara. Kalau kodenya ternyata salah,
        // percobaan bicara pertama akan ditolak server dan otomatis balik ke mode dengar.
        else {
            PushToTalkButton(isRecording, isSending, interactionSource)
        }
    }
}

@Composable
private fun PushToTalkButton(isRecording: Boolean, isSending: Boolean, interactionSource: MutableInteractionSource) {
    Column(Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            when { isSending -> "Mengirim..."; isRecording -> "Merekam... lepas untuk kirim"; else -> "Tekan & tahan untuk bicara" },
            fontSize = 12.sp, color = if (isRecording) Color(0xFFDC2626) else Color.Gray, fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(12.dp))
        Box(
            modifier = Modifier
                .size(84.dp)
                .background(if (isRecording) Color(0xFFDC2626) else Color(0xFF0F7A5A), CircleShape)
                .clickable(interactionSource = interactionSource, indication = null) { },
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Mic, contentDescription = "Tekan untuk bicara", tint = Color.White, modifier = Modifier.size(36.dp))
        }
    }
}
