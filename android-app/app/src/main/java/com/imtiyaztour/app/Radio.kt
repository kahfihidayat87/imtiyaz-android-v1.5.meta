package com.imtiyaztour.app

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaRecorder
import android.os.Build
import android.widget.Toast
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
// Pembimbing tekan-tahan tombol untuk merekam, lepas untuk kirim; jamaah
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
// Sekarang status Pembimbing HANYA berupa satu centang pada akun JAMAAH yang
// login lewat jalur yang SUDAH TERBUKTI bekerja (tidak pernah dilaporkan
// gagal). Tidak ada lagi layar/akun/token terpisah untuk TL -- login sekali
// sebagai jamaah, dan kalau akun itu ditandai "Pembimbing" oleh Admin, tombol
// bicara (ikon mikrofon) otomatis muncul. Jamaah biasa selalu melihat ikon
// earphone -- mode dengar saja, tidak ada tombol/gerbang untuk mencoba bicara.
// ============================================================================

data class RadioSendResponse(val success: Boolean? = null, val channel: String? = null, val error: String? = null)
data class RadioMessage(val id: String, val jamaah_id: String? = null, val nama: String, val time: Long, val audio: String? = null)
data class RadioPollRequest(val jamaah_id: String, val token: String, val after: Long)
data class GelombangInfo(
    val kode: String? = null,
    val nama: String? = null,
    val tl_nama: String? = null,
    val is_tl: Boolean? = null
)

data class RadioPollResponse(
    val channel: String? = null, val channel_nama: String? = null, val nama_saya: String? = null, val my_jamaah_id: String? = null,
    val is_fallback_channel: Boolean? = null, val is_tour_leader: Boolean? = null, val server_time: Long? = null,
    val mode: String? = null, val gelombang: GelombangInfo? = null,
    val messages: List<RadioMessage> = emptyList(), val error: String? = null
)

interface RadioApiService {
    @Multipart
    @POST("api/radio/send")
    suspend fun send(
        @Part("jamaah_id") jamaahId: okhttp3.RequestBody,
        @Part("token") token: okhttp3.RequestBody,
        @Part("kode_gelombang") kodeGelombang: okhttp3.RequestBody,
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

    /** @// return File hasil rekaman, atau null kalau gagal / terlalu pendek (< 400ms, dianggap salah pencet). */
    fun stop(): File? {
        val durationMs = System.currentTimeMillis() - startTime
        return try {
            recorder?.stop()
            recorder?.release()
            recorder = null
            if (durationMs < 400) {
                outputFile?.delete()
                null
            } else {
                outputFile
            }
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

    // v2.15.1: bypass login gate — guest mode untuk audien umum
    androidx.activity.compose.BackHandler(enabled = true) { onBack() }

    // v2.15.1: guest mode — audien umum radio pengajian tanpa login
    val loggedJamaahId = Prefs.getJamaahId(context)
    val loggedJamaahToken = Prefs.getToken(context)
    val isGuest = loggedJamaahId.isBlank()
    val jamaahId = if (isGuest) Prefs.getOrCreateGuestId(context) else loggedJamaahId
    val jamaahToken = loggedJamaahToken
    // Back sistem dari layar radio yang sudah login -> keluar ke Beranda, bukan tutup app.
    androidx.activity.compose.BackHandler(enabled = true) { onBack() }

    var hasMicPermission by remember {
        mutableStateOf(context.checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED)
    }
    val micPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        hasMicPermission = granted
    }

    var isTourLeader by remember { mutableStateOf(false) }
    var messages by remember { mutableStateOf(listOf<RadioMessage>()) }
    var lastAfter by remember { mutableStateOf(0L) }
    var myId by remember { mutableStateOf("") }
    var isRecording by remember { mutableStateOf(false) }
    var isSending by remember { mutableStateOf(false) }
    var statusMsg by remember { mutableStateOf("") }
    // v2.14.0: mode UI -- "voice" (voice note lama) atau "live" (LiveKit)
    var mode by remember { mutableStateOf("voice") }
    var liveStatus by remember { mutableStateOf("") }
    var liveActive by remember { mutableStateOf(false) }
    val playedIds = remember { mutableSetOf<String>() }
    val recorder = remember { RadioRecorder(context) }

    // v2.15.0 FIX: fetch is_tour_leader sekali di awal supaya gate UI (TL vs Jamaah)
    // benar SEBELUM user join gelombang. Sebelumnya hanya di-set oleh poll loop,
    // padahal poll loop baru aktif setelah join — jadi TL selalu tampil sebagai jamaah.
    LaunchedEffect(Unit) {
        try {
            val resp = withContext(Dispatchers.IO) {
                RadioApiClient.service.poll(RadioPollRequest(jamaahId, jamaahToken, 0L))
            }
            isTourLeader = resp.is_tour_leader ?: false
            myId = resp.my_jamaah_id ?: ""
        } catch (e: Exception) {
            // Diam-diam skip — bukan blocker utama. User bisa retry join.
        }
    }
    // v2.15.0: Gelombang Radio
    var kodeGelombang by remember { mutableStateOf("") }

    // Auto-connect LiveKit saat tab Siaran Live dibuka
    LaunchedEffect(mode, kodeGelombang, isTourLeader) {
        if (mode == "live" && kodeGelombang.isNotBlank() && isTourLeader) {
            if (!RadioLiveKit.sedangAktif()) {
                liveStatus = "Menghubungkan..."
                RadioLiveKit.mulai(context, jamaahId, jamaahToken, kodeGelombang) { status ->
                    liveStatus = status
                    liveActive = RadioLiveKit.sedangAktif()
                }
            }
        } else if (mode != "live") {
            RadioLiveKit.berhenti()
            liveActive = false
            liveStatus = ""
        }
    }
    var gelombangNama by remember { mutableStateOf("") }
    var gelombangTLNama by remember { mutableStateOf("") }
    var gelombangPeserta by remember { mutableIntStateOf(0) }
    var isCreatingGelombang by remember { mutableStateOf(false) }
    var isJoiningGelombang by remember { mutableStateOf(false) }
    var gelombangError by remember { mutableStateOf("") }
    var showVoiceCreateDialog by remember { mutableStateOf(false) }
    var kodeManualInput by remember { mutableStateOf("") }
    var showVoiceJoinDialog by remember { mutableStateOf(false) }

    fun handleCreateGelombang(nama: String, kodeManual: String) {
        isCreatingGelombang = true
        gelombangError = ""
        scope.launch {
            try {
                val resp = withContext(Dispatchers.IO) {
                    GelombangApiClient.service.create(mapOf(
                        "jamaah_id" to jamaahId,
                        "token" to jamaahToken,
                        "nama" to nama,
                        "kode_manual" to kodeManual
                    ))
                }
                if (resp.kode != null) {
                    kodeGelombang = resp.kode!!
                    gelombangNama = resp.nama ?: nama
                    gelombangTLNama = resp.tl_nama ?: ""
                    gelombangPeserta = 1
                    gelombangError = ""
                } else {
                    gelombangError = resp.error ?: "Gagal buat gelombang"
                }
            } catch (e: Exception) {
                gelombangError = "Gagal: ${e.message}"
            }
            isCreatingGelombang = false
        }
    }

    fun handleJoinGelombang(kode: String) {
        isJoiningGelombang = true
        gelombangError = ""
        scope.launch {
            try {
                val resp = withContext(Dispatchers.IO) {
                    GelombangApiClient.service.join(mapOf(
                        "jamaah_id" to jamaahId,
                        "token" to jamaahToken,
                        "kode" to kode.uppercase().trim()
                    ))
                }
                if (resp.kode != null) {
                    kodeGelombang = resp.kode!!
                    gelombangNama = resp.nama ?: ""
                    gelombangTLNama = resp.tl_nama ?: ""
                    gelombangPeserta = resp.jumlah_peserta ?: 1
                    gelombangError = ""
                } else {
                    gelombangError = resp.error ?: "Gagal join"
                }
            } catch (e: Exception) {
                gelombangError = "Gagal: ${e.message}"
            }
            isJoiningGelombang = false
        }
    }

    fun handleLeaveGelombang() {
        scope.launch {
            try {
                RadioLiveKit.berhenti()
                liveActive = false; liveStatus = ""
                withContext(Dispatchers.IO) {
                    GelombangApiClient.service.leave(mapOf(
                        "jamaah_id" to jamaahId, "token" to jamaahToken
                    ))
                }
            } catch (_: Exception) { }
            kodeGelombang = ""; gelombangNama = ""
            gelombangTLNama = ""; gelombangPeserta = 0
        }
    }

    fun handleRevokeGelombang() {
        scope.launch {
            try {
                RadioLiveKit.berhenti()
                liveActive = false; liveStatus = ""
                withContext(Dispatchers.IO) {
                    GelombangApiClient.service.revoke(mapOf(
                        "jamaah_id" to jamaahId, "token" to jamaahToken,
                        "kode" to kodeGelombang
                    ))
                }
            } catch (_: Exception) { }
            kodeGelombang = ""; gelombangNama = ""
            gelombangTLNama = ""; gelombangPeserta = 0
        }
    }


    fun handleUnauthorized() {
        if (!isGuest) {
            Prefs.clearLogin(context)
            isJamaah = false
        }
        // guest: abaikan, tidak ada data login untuk dibersihkan
    }

    // HANYA respons 401/403 dari server (bukti token memang ditolak) yang memicu
    // logout; error lain (jaringan, timeout, dll) cukup ditampilkan sebagai pesan
    // sementara, sesi TETAP AKTIF, dan boleh dicoba lagi.
    fun isAuthRejection(e: Exception): Boolean {
        val code = (e as? retrofit2.HttpException)?.code()
        return code == 401 || code == 403
    }

    // Polling loop -- ini yang menggantikan "streaming langsung" (lihat catatan di atas).
    LaunchedEffect(kodeGelombang) {
        if (kodeGelombang.isBlank()) return@LaunchedEffect
        while (isActive) {
            try {
                val resp = withContext(Dispatchers.IO) {
                    RadioApiClient.service.poll(RadioPollRequest(jamaahId, jamaahToken, lastAfter))
                }
                myId = resp.my_jamaah_id ?: myId
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

    // v2.14.1: cleanup LiveKit hanya saat keluar RadioScreen.
    // Start/stop siaran sekarang manual via tombol di LiveKitSection.
    DisposableEffect(Unit) {
        onDispose {
            RadioLiveKit.berhenti()
        }
    }

    fun sendClip(file: File) {
        isSending = true
        scope.launch {
            try {
                val part = MultipartBody.Part.createFormData("audio", file.name, file.asRequestBody("audio/mp4".toMediaTypeOrNull()))
                withContext(Dispatchers.IO) {
                    RadioApiClient.service.send(
                        jamaahId.asBody(),
                        jamaahToken.asBody(),
                        kodeGelombang.asBody(),
                        part
                    )
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

    Column(Modifier.fillMaxSize().navigationBarsPadding().imePadding()) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onBack) { Text("← Kembali") }
        }
        Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
            Text("Radio Imtiyaz", fontWeight = FontWeight.Bold, fontSize = 20.sp)
            Text(
                "Siaran & Manasik",
                fontSize = 12.sp, color = Color.Gray
            )
            if (isTourLeader) {
                Text("Anda ditandai sebagai pembimbing -- Anda bisa bicara di gelombang ini", fontSize = 11.sp, color = Color(0xFF0F7A5A), fontWeight = FontWeight.Bold)
            } else {
                Text("Hanya pembimbing yang bisa bicara di gelombang ini", fontSize = 11.sp, color = Color.Gray)
            }
            if (statusMsg.isNotEmpty()) Text(statusMsg, fontSize = 11.sp, color = Color(0xFFDC2626))
        }

        // v2.14.0: tab switcher -- Voice Note vs Siaran Live (LiveKit)
        TabRow(
            selectedTabIndex = if (mode == "voice") 0 else 1,
            containerColor = Color(0xFFF0FDF4),
            contentColor = Color(0xFF0F7A5A),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)
        ) {
            Tab(
                selected = mode == "voice",
                onClick = { mode = "voice" },
                text = { Text("\uD83C\uDF99 Voice Note", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
            )
            Tab(
                selected = mode == "live",
                onClick = { mode = "live" },
                text = { Text("\uD83D\uDD34 Siaran Live", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
            )
        }
        Spacer(Modifier.height(8.dp))

        if (mode == "voice") {
            if (kodeGelombang.isBlank()) {
                Column(
                    Modifier.fillMaxWidth().padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(Modifier.height(16.dp))
                    Box(
                        modifier = Modifier.size(72.dp).background(Color(0xFF9CA3AF), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            if (isTourLeader) Icons.Default.Mic else Icons.Default.Headset,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    Text(
                        if (isTourLeader) "Mode Voice Note (TL)" else "Mode Mendengarkan (Voice Note)",
                        fontWeight = FontWeight.Bold, fontSize = 14.sp
                    )
                    Spacer(Modifier.height(16.dp))
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF0FDF4)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            Text(
                                if (isTourLeader) "Buat Gelombang Baru" else "Masukkan Kode Gelombang",
                                fontWeight = FontWeight.Bold, fontSize = 14.sp,
                                color = Color(0xFF0F7A5A)
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                if (isTourLeader)
                                    "Anda akan mendapat kode unik. Bagikan kode ke jamaah agar bisa bergabung."
                                else
                                    "Minta kode gelombang dari Pembimbing Anda, lalu masukkan di sini.",
                                fontSize = 11.sp, color = Color.Gray
                            )
                            Spacer(Modifier.height(12.dp))
                            Button(
                                onClick = {
                                    if (isTourLeader) showVoiceCreateDialog = true
                                    else showVoiceJoinDialog = true
                                },
                                modifier = Modifier.fillMaxWidth().height(56.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F7A5A)),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text(
                                    if (isTourLeader) "BUAT GELOMBANG BARU" else "MASUKKAN KODE",
                                    fontWeight = FontWeight.Bold, fontSize = 13.sp
                                )
                            }
                        }
                    }
                    if (gelombangError.isNotEmpty()) {
                        Spacer(Modifier.height(8.dp))
                        Text(gelombangError, color = Color(0xFFDC2626), fontSize = 11.sp)
                    }
                }

                if (showVoiceCreateDialog) {
                    CreateGelombangDialog(
                        isLoading = isCreatingGelombang,
                        onDismiss = { showVoiceCreateDialog = false },
                        onCreate = { nama, kode -> showVoiceCreateDialog = false; handleCreateGelombang(nama, kode) }
                    )
                }
                if (showVoiceJoinDialog) {
                    JoinGelombangDialog(
                        isLoading = isJoiningGelombang,
                        onDismiss = { showVoiceJoinDialog = false },
                        onJoin = { kode -> showVoiceJoinDialog = false; handleJoinGelombang(kode) }
                    )
                }
            } else {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF7ED)),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)
                ) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("📻 Kode: $kodeGelombang", fontSize = 11.sp,
                                color = Color(0xFFD97706), fontWeight = FontWeight.Bold)
                            Text(gelombangNama, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            if (!isTourLeader && gelombangTLNama.isNotBlank()) {
                                Text("TL: $gelombangTLNama", fontSize = 10.sp, color = Color.Gray)
                            }
                        }
                        IconButton(onClick = {
                            val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            cm.setPrimaryClip(ClipData.newPlainText("Kode", kodeGelombang))
                            Toast.makeText(context, "Kode disalin", Toast.LENGTH_SHORT).show()
                        }) { Text("📋", fontSize = 18.sp) }
                        IconButton(onClick = {
                            val txt = "Assalamualaikum\n\nGabung gelombang radio IMTIYAZ:\n\nKode: $kodeGelombang\nNama: $gelombangNama\nTL: $gelombangTLNama\n\nCara gabung:\n1. Buka app Imtiyaz Tour\n2. Tab Radio - Voice Note\n3. Masukkan kode di atas\n\nIMTIYAZ TOUR & TRAVEL"
                            val i = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, txt)
                            }
                            context.startActivity(Intent.createChooser(i, "Bagikan Kode"))
                        }) { Text("📤", fontSize = 18.sp) }
                    }
                }
                Spacer(Modifier.height(4.dp))

                LazyColumn(Modifier.weight(1f).padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (messages.isEmpty()) {
                        item { Text("Belum ada suara masuk di gelombang ini.", fontSize = 12.sp, color = Color.Gray, modifier = Modifier.padding(top = 16.dp)) }
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

                if (isTourLeader) {
                    PushToTalkButton(isRecording, isSending, interactionSource)
                } else {
                    ListenOnlyIndicator()
                }
                Spacer(Modifier.height(8.dp))
                TextButton(onClick = {
                    if (isTourLeader) handleRevokeGelombang() else handleLeaveGelombang()
                }) {
                    Text(
                        if (isTourLeader) "Akhiri Gelombang" else "Keluar dari Gelombang",
                        color = Color(0xFFDC2626), fontSize = 12.sp
                    )
                }
            }
        } else {
            // v2.14.0: Mode Siaran Live (LiveKit WebRTC)
            LiveKitSection(
                isTourLeader = isTourLeader,
                liveStatus = liveStatus,
                liveActive = liveActive,
                kodeGelombang = kodeGelombang,
                gelombangNama = gelombangNama,
                gelombangTLNama = gelombangTLNama,
                gelombangPeserta = gelombangPeserta,
                isCreatingGelombang = isCreatingGelombang,
                isJoiningGelombang = isJoiningGelombang,
                gelombangError = gelombangError,
                onStart = {
                    if (!hasMicPermission) {
                        micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    } else {
                        RadioLiveKit.mulai(context, jamaahId, jamaahToken, kodeGelombang) { s ->
                            liveStatus = s
                            liveActive = RadioLiveKit.sedangAktif()
                        }
                    }
                },
                onStop = {
                    RadioLiveKit.berhenti()
                    liveStatus = ""
                    liveActive = false
                },
                onCreateGelombang = { nama, kode -> handleCreateGelombang(nama, kode) },
                onJoinGelombang = { kode -> handleJoinGelombang(kode) },
                onLeaveGelombang = { handleLeaveGelombang() },
                onRevokeGelombang = { handleRevokeGelombang() }
            )
        }
    }
}

@Composable
private fun LiveKitSection(
    isTourLeader: Boolean,
    liveStatus: String,
    liveActive: Boolean,
    kodeGelombang: String,
    gelombangNama: String,
    gelombangTLNama: String,
    gelombangPeserta: Int,
    isCreatingGelombang: Boolean,
    isJoiningGelombang: Boolean,
    gelombangError: String,
    onStart: () -> Unit,
    onStop: () -> Unit,
    onCreateGelombang: (String, String) -> Unit,
    onJoinGelombang: (String) -> Unit,
    onLeaveGelombang: () -> Unit,
    onRevokeGelombang: () -> Unit
) {
    val context = LocalContext.current
    var showCreateDialog by remember { mutableStateOf(false) }
    var showJoinDialog by remember { mutableStateOf(false) }
    var showLeaveConfirm by remember { mutableStateOf(false) }
    var showRevokeConfirm by remember { mutableStateOf(false) }

    Column(
        Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .background(
                    if (liveActive) Color(0xFFDC2626) else Color(0xFF9CA3AF),
                    CircleShape
                )
                .clickable { if (liveActive) onStop() else onStart() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                if (isTourLeader) Icons.Default.Mic else Icons.Default.Headset,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(32.dp)
            )
        }
        Spacer(Modifier.height(12.dp))
        Text(
            if (isTourLeader) "Mode Siaran Langsung (TL)" else "Mode Mendengarkan (Live)",
            fontWeight = FontWeight.Bold, fontSize = 14.sp
        )
        if (liveStatus.isNotEmpty()) {
            Spacer(Modifier.height(4.dp))
            Text(
                liveStatus,
                fontSize = 12.sp,
                color = if (liveActive) Color(0xFF0F7A5A) else Color.Gray,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(Modifier.height(20.dp))

        if (kodeGelombang.isBlank()) {
            // Belum di gelombang
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF0FDF4)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text(
                        if (isTourLeader) "Buat Gelombang Baru"
                        else "Masukkan Kode Gelombang",
                        fontWeight = FontWeight.Bold, fontSize = 14.sp,
                        color = Color(0xFF0F7A5A)
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        if (isTourLeader)
                            "Anda akan mendapat kode unik. Bagikan kode ke jamaah agar bisa bergabung."
                        else
                            "Minta kode gelombang dari Pembimbing Anda, lalu masukkan di sini.",
                        fontSize = 11.sp, color = Color.Gray
                    )
                    Spacer(Modifier.height(12.dp))
                    Button(
                        onClick = {
                            if (isTourLeader) showCreateDialog = true
                            else showJoinDialog = true
                        },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F7A5A)),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            if (isTourLeader) "BUAT GELOMBANG BARU" else "MASUKKAN KODE",
                            fontWeight = FontWeight.Bold, fontSize = 13.sp
                        )
                    }
                }
            }
        } else {
            // Sudah di gelombang
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF7ED)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text("📻 KODE GELOMBANG",
                        fontSize = 10.sp, color = Color(0xFFD97706), fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(4.dp))
                    Text(kodeGelombang,
                        fontSize = 28.sp, fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F7A5A))
                    Spacer(Modifier.height(8.dp))
                    Text("Nama: $gelombangNama", fontSize = 12.sp)
                    if (!isTourLeader) {
                        Text("TL: $gelombangTLNama", fontSize = 12.sp)
                    } else {
                        Text("Peserta: $gelombangPeserta jamaah", fontSize = 12.sp)
                    }
                    Spacer(Modifier.height(12.dp))
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                cm.setPrimaryClip(ClipData.newPlainText("Kode Gelombang", kodeGelombang))
                                Toast.makeText(context, "Kode disalin: $kodeGelombang", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.weight(1f).height(42.dp),
                            shape = RoundedCornerShape(10.dp)
                        ) { Text("📋 Salin", fontSize = 12.sp) }
                        Button(
                            onClick = {
                                val shareText = "Assalamualaikum 🌙\n\n" +
                                    "Silakan gabung di gelombang radio IMTIYAZ:\n\n" +
                                    "📻 Kode: $kodeGelombang\n" +
                                    "Nama: $gelombangNama\n" +
                                    "TL: $gelombangTLNama\n\n" +
                                    "Cara gabung:\n" +
                                    "1. Buka aplikasi Imtiyaz Tour\n" +
                                    "2. Tab Radio → Siaran Live\n" +
                                    "3. Masukkan kode di atas\n\n" +
                                    "📿 IMTIYAZ TOUR & TRAVEL"
                                val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, shareText)
                                }
                                context.startActivity(Intent.createChooser(sendIntent, "Bagikan Kode Gelombang"))
                            },
                            modifier = Modifier.weight(1f).height(42.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)),
                            shape = RoundedCornerShape(10.dp)
                        ) { Text("📤 Share WA", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            if (liveActive) {
                Button(
                    onClick = onStop,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        if (isTourLeader) "AKHIRI SIARAN" else "BERHENTI MENDENGARKAN",
                        fontSize = 13.sp, fontWeight = FontWeight.Bold
                    )
                }
            } else {
                Button(
                    onClick = onStart,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F7A5A)),
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        if (isTourLeader) "MULAI SIARAN LIVE" else "MULAI MENDENGARKAN",
                        fontSize = 13.sp, fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(Modifier.height(8.dp))
            TextButton(
                onClick = {
                    if (isTourLeader) showRevokeConfirm = true else showLeaveConfirm = true
                }
            ) {
                Text(
                    if (isTourLeader) "Akhiri & Hapus Gelombang" else "Keluar dari Gelombang",
                    color = Color(0xFFDC2626), fontSize = 12.sp
                )
            }
        }

        if (gelombangError.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            Text(gelombangError, color = Color(0xFFDC2626), fontSize = 11.sp)
        }
    }

    // Dialogs
    if (showCreateDialog) {
        CreateGelombangDialog(
            isLoading = isCreatingGelombang,
            onDismiss = { showCreateDialog = false },
            onCreate = { nama, kode -> showCreateDialog = false; onCreateGelombang(nama, kode) }
        )
    }
    if (showJoinDialog) {
        JoinGelombangDialog(
            isLoading = isJoiningGelombang,
            onDismiss = { showJoinDialog = false },
            onJoin = { kode -> showJoinDialog = false; onJoinGelombang(kode) }
        )
    }
    if (showLeaveConfirm) {
        AlertDialog(
            onDismissRequest = { showLeaveConfirm = false },
            title = { Text("Keluar dari Gelombang?") },
            text = { Text("Anda akan berhenti mendengarkan gelombang ini.") },
            confirmButton = {
                TextButton(onClick = { showLeaveConfirm = false; onLeaveGelombang() }) {
                    Text("Keluar", color = Color(0xFFDC2626), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLeaveConfirm = false }) { Text("Batal") }
            }
        )
    }
    if (showRevokeConfirm) {
        AlertDialog(
            onDismissRequest = { showRevokeConfirm = false },
            title = { Text("Akhiri Gelombang?") },
            text = { Text("Gelombang akan dihapus dan semua peserta otomatis keluar.") },
            confirmButton = {
                TextButton(onClick = { showRevokeConfirm = false; onRevokeGelombang() }) {
                    Text("Akhiri", color = Color(0xFFDC2626), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showRevokeConfirm = false }) { Text("Batal") }
            }
        )
    }
}

@Composable
private fun CreateGelombangDialog(
    isLoading: Boolean,
    onDismiss: () -> Unit,
    onCreate: (String, String) -> Unit
) {
    var nama by remember { mutableStateOf("") }
    var kodeManual by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = { if (!isLoading) onDismiss() },
        title = { Text("Buat Gelombang Radio") },
        text = {
            Column {
                Text("Beri nama gelombang (misal: Linuwih 17 Okt). Sistem akan generate kode unik.",
                    fontSize = 12.sp, color = Color.Gray)
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = nama,
                    onValueChange = { nama = it },
                    label = { Text("Nama Gelombang")
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = kodeManual,
                onValueChange = { kodeManual = it.uppercase() },
                label = { Text("Kode Manual (Opsional)") },
                placeholder = { Text("Misal: KAJI-UMUM") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            ) },
                    placeholder = { Text("Linuwih 17 Okt 2026") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { if (nama.isNotBlank() && !isLoading) onCreate(nama.trim(), kodeManual.trim()) },
                enabled = nama.isNotBlank() && !isLoading
            ) {
                if (isLoading) CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                else Text("BUAT", fontWeight = FontWeight.Bold, color = Color(0xFF0F7A5A))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isLoading) { Text("Batal") }
        }
    )
}

@Composable
private fun JoinGelombangDialog(
    isLoading: Boolean,
    onDismiss: () -> Unit,
    onJoin: (String) -> Unit
) {
    var kode by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = { if (!isLoading) onDismiss() },
        title = { Text("Gabung Gelombang") },
        text = {
            Column {
                Text("Masukkan kode gelombang dari Tour Leader Anda.",
                    fontSize = 12.sp, color = Color.Gray)
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = kode,
                    onValueChange = { kode = it.uppercase() },
                    label = { Text("Kode Gelombang") },
                    placeholder = { Text("LINU-7K3M") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { if (kode.isNotBlank() && !isLoading) onJoin(kode.trim()) },
                enabled = kode.isNotBlank() && !isLoading
            ) {
                if (isLoading) CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                else Text("GABUNG", fontWeight = FontWeight.Bold, color = Color(0xFF0F7A5A))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isLoading) { Text("Batal") }
        }
    )
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
        Text("Mendengarkan", fontSize = 12.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(12.dp))
        Box(
            modifier = Modifier.size(84.dp).background(Color(0xFF9CA3AF), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Headset, contentDescription = "Mendengarkan", tint = Color.White, modifier = Modifier.size(36.dp))
        }
    }
}
