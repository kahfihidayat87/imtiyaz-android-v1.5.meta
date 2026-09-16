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
import androidx.compose.material.icons.filled.Headset
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
import kotlinx.coroutines.delay
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
// Kanal dibuat & dikelola Admin sendiri (tab "Kanal Radio", LEPAS dari Paket
// Umrah -- satu paket bisa punya banyak tanggal keberangkatan/kanal berbeda).
//
// FIX BESAR: sistem "Login TL" terpisah (username/password sendiri, layar
// login sendiri) DIHAPUS TOTAL -- setelah berkali-kali gagal di lapangan dan
// tidak bisa dilacak akar masalahnya tanpa akses langsung ke server produksi.
// Sekarang status Tour Leader HANYA berupa satu centang pada akun JAMAAH yang
// login lewat jalur yang SUDAH TERBUKTI bekerja (tidak pernah dilaporkan
// gagal). Tidak ada lagi layar/akun/token terpisah untuk TL -- login sekali
// sebagai jamaah, dan kalau akun itu ditandai "Tour Leader" oleh Admin, tombol
// bicara (ikon mikrofon) otomatis muncul. Jamaah biasa selalu melihat ikon
// earphone -- mode dengar saja, tidak ada tombol/gerbang untuk mencoba bicara.
// ============================================================================

data class RadioSendResponse(val success: Boolean? = null, val channel: String? = null, val error: String? = null)
data class RadioMessage(val id: String, val jamaah_id: String? = null, val nama: String, val time: Long, val audio: String? = null)
data class RadioPollRequest(val jamaah_id: String, val token: String, val after: Long)
data class RadioPollResponse(
    val channel: String? = null, val channel_nama: String? = null, val nama_saya: String? = null, val my_jamaah_id: String? = null,
    val is_fallback_channel: Boolean? = null, val is_tour_leader: Boolean? = null, val server_time: Long? = null,
    val messages: List<RadioMessage> = emptyList(), val error: String? = null
)

interface RadioApiService {
    @Multipart
    @POST("api/radio/send")
    suspend fun send(
        @Part("jamaah_id") jamaahId: okhttp3.RequestBody,
        @Part("token") token: okhttp3.RequestBody,
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
private fun String.asBody(): okhttp3.RequestBody = this.toRequestBody("text/plain".toMediaTypeOrNull())

@Composable
fun RadioScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var isJamaah by remember { mutableStateOf(Prefs.isLoggedIn(context)) }

    // Belum login -> gerbang login jamaah biasa (satu-satunya jalur sekarang; status
    // Tour Leader ditentukan Admin lewat centang pada akun jamaah, bukan login terpisah).
    if (!isJamaah) {
        androidx.activity.compose.BackHandler(enabled = true) { onBack() }
        LoginScreen(onLoggedIn = { isJamaah = true }, onCancel = onBack)
        return
    }

    val jamaahId = Prefs.getJamaahId(context)
    val jamaahToken = Prefs.getToken(context)
    // Back sistem dari layar radio yang sudah login -> keluar ke Beranda, bukan tutup app.
    androidx.activity.compose.BackHandler(enabled = true) { onBack() }

    var hasMicPermission by remember {
        mutableStateOf(context.checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED)
    }
    val micPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        hasMicPermission = granted
    }

    var channelNama by remember { mutableStateOf("") }
    var isFallbackChannel by remember { mutableStateOf(false) }
    var isTourLeader by remember { mutableStateOf(false) }
    var messages by remember { mutableStateOf(listOf<RadioMessage>()) }
    var lastAfter by remember { mutableStateOf(0L) }
    var myId by remember { mutableStateOf("") }
    var isRecording by remember { mutableStateOf(false) }
    var isSending by remember { mutableStateOf(false) }
    var statusMsg by remember { mutableStateOf("") }
    val playedIds = remember { mutableSetOf<String>() }
    val recorder = remember { RadioRecorder(context) }

    fun handleUnauthorized() { Prefs.clearLogin(context); isJamaah = false }

    // HANYA respons 401/403 dari server (bukti token memang ditolak) yang memicu
    // logout; error lain (jaringan, timeout, dll) cukup ditampilkan sebagai pesan
    // sementara, sesi TETAP AKTIF, dan boleh dicoba lagi.
    fun isAuthRejection(e: Exception): Boolean {
        val code = (e as? retrofit2.HttpException)?.code()
        return code == 401 || code == 403
    }

    // Polling loop -- ini yang menggantikan "streaming langsung" (lihat catatan di atas).
    LaunchedEffect(Unit) {
        while (isActive) {
            try {
                val resp = withContext(Dispatchers.IO) {
                    RadioApiClient.service.poll(RadioPollRequest(jamaahId, jamaahToken, lastAfter))
                }
                channelNama = resp.channel_nama ?: channelNama
                myId = resp.my_jamaah_id ?: myId
                isFallbackChannel = resp.is_fallback_channel ?: false
                isTourLeader = resp.is_tour_leader ?: false
                if (resp.messages.isNotEmpty()) {
                    messages = (messages + resp.messages).takeLast(30)
                    lastAfter = resp.messages.maxOf { it.time }
                    // Deteksi "pesan sendiri" (supaya tidak diputar ulang/gema) lewat ID unik
                    // dari server (my_jamaah_id).
                    for (m in resp.messages) {
                        if (m.id !in playedIds && m.jamaah_id != myId && m.audio != null) {
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
                if (isAuthRejection(e)) {
                    handleUnauthorized(); return@LaunchedEffect
                }
                statusMsg = "Tidak terhubung ke server -- akan mencoba lagi"
            }
            delay(2500)
        }
    }

    fun sendClip(file: File) {
        isSending = true
        scope.launch {
            try {
                val part = MultipartBody.Part.createFormData("audio", file.name, file.asRequestBody("audio/mp4".toMediaTypeOrNull()))
                withContext(Dispatchers.IO) {
                    RadioApiClient.service.send(jamaahId.asBody(), jamaahToken.asBody(), part)
                }
                statusMsg = ""
            } catch (e: Exception) {
                if (isAuthRejection(e)) {
                    handleUnauthorized()
                } else {
                    statusMsg = "Gagal mengirim (jaringan bermasalah) -- coba tekan & tahan lagi"
                }
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
                if (channelNama.isNotEmpty()) "Kanal: $channelNama" else "Menghubungkan ke kanal...",
                fontSize = 12.sp, color = Color.Gray
            )
            if (isTourLeader) {
                Text("Anda ditandai sebagai Tour Leader -- Anda bisa bicara di kanal ini", fontSize = 11.sp, color = Color(0xFF0F7A5A), fontWeight = FontWeight.Bold)
            } else {
                Text("Mode dengar -- hanya Tour Leader yang bisa bicara di kanal ini", fontSize = 11.sp, color = Color.Gray)
            }
            if (isFallbackChannel) {
                Spacer(Modifier.height(6.dp))
                Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3CD)), shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
                    Text(
                        "⚠ Akun Anda belum ditugaskan ke kanal radio tertentu oleh Admin, jadi masuk ke kanal umum. Kalau seharusnya Anda satu rombongan dengan jamaah lain, minta Admin memilih Kanal Radio akun Anda di WP Admin (Data Jamaah > Kanal Radio) -- tanpa itu, Anda tidak akan bisa saling dengar dengan rombongan yang benar.",
                        fontSize = 10.sp, color = Color(0xFF92400E), modifier = Modifier.padding(10.dp)
                    )
                }
            }
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

        // Status Tour Leader ditentukan Admin (centang pada akun jamaah), dikirim
        // server lewat is_tour_leader -- TIDAK ADA cara lain untuk membukanya dari
        // sisi aplikasi. Jamaah biasa selalu dapat ikon earphone, tanpa modifier
        // apa pun yang bisa memicu perekaman.
        if (isTourLeader) {
            PushToTalkButton(isRecording, isSending, interactionSource)
        } else {
            ListenOnlyIndicator()
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

// Indikator untuk jamaah (mode dengar saja) -- ikon earphone, TIDAK ada modifier
// clickable sama sekali, jadi tidak mungkin memicu perekaman dengan cara apa pun.
@Composable
private fun ListenOnlyIndicator() {
    Column(Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Mode dengar", fontSize = 12.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(12.dp))
        Box(
            modifier = Modifier.size(84.dp).background(Color(0xFF9CA3AF), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Headset, contentDescription = "Mode dengar", tint = Color.White, modifier = Modifier.size(36.dp))
        }
    }
}
