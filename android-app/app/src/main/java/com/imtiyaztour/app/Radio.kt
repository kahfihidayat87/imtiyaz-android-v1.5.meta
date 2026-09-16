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
import androidx.compose.ui.text.style.TextAlign
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
// FIX: sesuai permintaan, akses bicara sekarang TERTUTUP TOTAL untuk jamaah
// biasa. Jalur "Kode TL" (jamaah bisa jadi TL sementara dengan memasukkan
// kode) sudah DIHAPUS. SATU-SATUNYA cara bicara adalah Akun Login TL -- login
// dengan username/password sendiri (TIDAK perlu akun jamaah palsu), langsung
// berwenang siaran di kanal yang ditugaskan tanpa perlu isi kode apa pun.
// Jamaah yang login sebagai jamaah SELALU mode dengar saja -- ikon di layar
// berubah jadi earphone (bukan mikrofon) untuk menegaskan mereka tidak bisa
// bicara, sama sekali tidak ada tombol/gerbang untuk mencoba bicara.
// ============================================================================

data class RadioSendResponse(val success: Boolean? = null, val channel: String? = null, val error: String? = null)
data class RadioMessage(val id: String, val jamaah_id: String? = null, val nama: String, val time: Long, val audio: String? = null)
data class RadioPollRequest(
    val jamaah_id: String, val token: String, val after: Long,
    val tl_kanal_id: String = "", val tl_token: String = ""
)
data class RadioPollResponse(
    val channel: String? = null, val channel_nama: String? = null, val nama_saya: String? = null, val my_jamaah_id: String? = null,
    val is_fallback_channel: Boolean? = null, val is_tl_session: Boolean? = null, val server_time: Long? = null,
    val messages: List<RadioMessage> = emptyList(), val error: String? = null
)
data class TlLoginRequest(val username: String, val password: String)
data class TlLoginResponse(val success: Boolean? = null, val kanal_id: String? = null, val kanal_nama: String? = null, val token: String? = null, val error: String? = null)
data class TlLogoutRequest(val kanal_id: String, val token: String)

interface RadioApiService {
    @Multipart
    @POST("api/radio/send")
    suspend fun send(
        @Part("tl_kanal_id") tlKanalId: okhttp3.RequestBody,
        @Part("tl_token") tlToken: okhttp3.RequestBody,
        @Part audio: MultipartBody.Part
    ): RadioSendResponse

    @POST("api/radio/poll")
    suspend fun poll(@Body body: RadioPollRequest): RadioPollResponse

    @POST("api/tl-login")
    suspend fun tlLogin(@Body body: TlLoginRequest): TlLoginResponse

    @POST("api/tl-logout")
    suspend fun tlLogout(@Body body: TlLogoutRequest): Map<String, Boolean>
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

// FIX: akun Login TL terpisah dari login jamaah -- Admin tidak perlu lagi
// membuatkan akun "jamaah" palsu hanya supaya staf TL bisa masuk aplikasi.
@Composable
fun TlLoginScreen(onLoggedIn: () -> Unit, onBackToChoice: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf("") }

    Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Center) {
        Text("Login Tour Leader", fontWeight = FontWeight.Bold, fontSize = 22.sp, color = Color(0xFF0F7A5A))
        Text("Username & password ini terpisah dari akun jamaah -- diberikan Admin khusus untuk memandu satu kanal radio.", fontSize = 12.sp, color = Color.Gray)
        Spacer(Modifier.height(20.dp))
        OutlinedTextField(value = username, onValueChange = { username = it }, label = { Text("Username TL") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp))
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = password, onValueChange = { password = it }, label = { Text("Password TL") },
            visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp)
        )
        Spacer(Modifier.height(16.dp))
        if (errorMsg.isNotEmpty()) { Text(errorMsg, fontSize = 12.sp, color = Color(0xFFDC2626)); Spacer(Modifier.height(8.dp)) }
        Button(
            onClick = {
                if (username.isBlank() || password.isBlank()) { errorMsg = "Username dan password wajib diisi"; return@Button }
                isLoading = true; errorMsg = ""
                scope.launch {
                    try {
                        val resp = withContext(Dispatchers.IO) { RadioApiClient.service.tlLogin(TlLoginRequest(username.trim(), password)) }
                        if (resp.success == true && resp.token != null && resp.kanal_id != null) {
                            Prefs.saveTlLogin(context, resp.kanal_id, resp.token, resp.kanal_nama ?: resp.kanal_id)
                            onLoggedIn()
                        } else {
                            errorMsg = resp.error ?: "Username atau password TL salah"
                        }
                    } catch (e: Exception) {
                        errorMsg = "Username atau password TL salah, atau tidak ada koneksi internet"
                    }
                    isLoading = false
                }
            },
            enabled = !isLoading,
            modifier = Modifier.fillMaxWidth().height(50.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F7A5A)),
            shape = RoundedCornerShape(12.dp)
        ) { if (isLoading) CircularProgressIndicator(Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp) else Text("Masuk sebagai TL", fontWeight = FontWeight.Bold) }
        Spacer(Modifier.height(12.dp))
        TextButton(onClick = onBackToChoice) { Text("← Kembali", fontSize = 12.sp) }
    }
}

@Composable
fun RadioScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var isTlAccount by remember { mutableStateOf(Prefs.isTlLoggedIn(context)) }
    var isJamaah by remember { mutableStateOf(Prefs.isLoggedIn(context)) }

    // Belum login sama sekali (baik jamaah maupun TL) -> tawarkan dua pilihan,
    // supaya TL yang tidak punya akun jamaah tetap bisa masuk lewat jalur sendiri.
    if (!isTlAccount && !isJamaah) {
        var choice by remember { mutableStateOf("") } // "", "jamaah", "tl"
        // Back sistem: dari sub-layar login -> kembali ke pilihan; dari pilihan -> onBack().
        androidx.activity.compose.BackHandler(enabled = true) {
            if (choice.isNotEmpty()) choice = "" else onBack()
        }
        when (choice) {
            "jamaah" -> LoginScreen(onLoggedIn = { isJamaah = true }, onCancel = { choice = "" })
            "tl" -> TlLoginScreen(onLoggedIn = { isTlAccount = true }, onBackToChoice = { choice = "" })
            else -> Column(Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                Text("Radio Tour Leader", fontWeight = FontWeight.Bold, fontSize = 18.sp, textAlign = TextAlign.Center)
                Spacer(Modifier.height(8.dp))
                Text("Pilih cara masuk sesuai peran Anda.", fontSize = 12.sp, color = Color.Gray, textAlign = TextAlign.Center)
                Spacer(Modifier.height(20.dp))
                Button(onClick = { choice = "jamaah" }, modifier = Modifier.fillMaxWidth().height(48.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F7A5A)), shape = RoundedCornerShape(12.dp)) { Text("Saya Jamaah") }
                Spacer(Modifier.height(10.dp))
                OutlinedButton(onClick = { choice = "tl" }, modifier = Modifier.fillMaxWidth().height(48.dp), shape = RoundedCornerShape(12.dp)) { Text("Saya Tour Leader") }
                Spacer(Modifier.height(16.dp))
                TextButton(onClick = onBack) { Text("← Kembali", fontSize = 12.sp) }
            }
        }
        return
    }

    // --- Kredensial: salah satu jalur berikut aktif, yang lain dikosongkan ---
    val jamaahId = if (isJamaah) Prefs.getJamaahId(context) else ""
    val jamaahToken = if (isJamaah) Prefs.getToken(context) else ""
    // Back sistem dari layar radio yang sudah login -> keluar ke Beranda, bukan tutup app.
    androidx.activity.compose.BackHandler(enabled = true) { onBack() }
    val tlKanalId = if (isTlAccount) Prefs.getTlKanalId(context) else ""
    val tlToken = if (isTlAccount) Prefs.getTlToken(context) else ""

    var hasMicPermission by remember {
        mutableStateOf(context.checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED)
    }
    val micPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        hasMicPermission = granted
    }

    var channel by remember { mutableStateOf("") }
    var channelNama by remember { mutableStateOf(if (isTlAccount) Prefs.getTlKanalNama(context) else "") }
    var isFallbackChannel by remember { mutableStateOf(false) }
    var messages by remember { mutableStateOf(listOf<RadioMessage>()) }
    var lastAfter by remember { mutableStateOf(0L) }
    var myId by remember { mutableStateOf("") }
    var isRecording by remember { mutableStateOf(false) }
    var isSending by remember { mutableStateOf(false) }
    var statusMsg by remember { mutableStateOf("") }
    val playedIds = remember { mutableSetOf<String>() }
    val recorder = remember { RadioRecorder(context) }

    fun handleUnauthorized() {
        if (isTlAccount) { Prefs.clearTlLogin(context); isTlAccount = false }
        else { Prefs.clearLogin(context); isJamaah = false }
    }

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
                    RadioApiClient.service.poll(RadioPollRequest(jamaahId, jamaahToken, lastAfter, tlKanalId, tlToken))
                }
                channel = resp.channel ?: channel
                channelNama = resp.channel_nama ?: channelNama
                myId = resp.my_jamaah_id ?: myId
                isFallbackChannel = resp.is_fallback_channel ?: false
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
                    RadioApiClient.service.send(tlKanalId.asBody(), tlToken.asBody(), part)
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
        Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onBack) { Text("← Kembali") }
            if (isTlAccount) {
                TextButton(onClick = {
                    scope.launch {
                        try { withContext(Dispatchers.IO) { RadioApiClient.service.tlLogout(TlLogoutRequest(tlKanalId, tlToken)) } } catch (e: Exception) {}
                        Prefs.clearTlLogin(context); isTlAccount = false
                    }
                }) { Text("Keluar (TL)", fontSize = 12.sp) }
            }
        }
        Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
            Text("Radio Tour Leader", fontWeight = FontWeight.Bold, fontSize = 20.sp)
            Text(
                if (channelNama.isNotEmpty()) "Kanal: $channelNama" else "Menghubungkan ke kanal...",
                fontSize = 12.sp, color = Color.Gray
            )
            if (isTlAccount) {
                Text("Masuk sebagai Tour Leader kanal ini -- Anda bisa bicara", fontSize = 11.sp, color = Color(0xFF0F7A5A), fontWeight = FontWeight.Bold)
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

        // FIX: sesuai permintaan, jamaah TIDAK PERNAH bisa bicara -- tidak ada gerbang
        // kode, tidak ada pengecualian. Hanya akun Login TL yang mendapat tombol
        // bicara (ikon mikrofon); jamaah selalu melihat indikator dengar (ikon
        // earphone) yang tidak bisa ditekan sama sekali.
        if (isTlAccount) {
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
