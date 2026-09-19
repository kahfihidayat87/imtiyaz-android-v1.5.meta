#!/usr/bin/env python3
"""
Patch Tracking Lansia v2.11.0 untuk Android
- Patch AndroidManifest.xml (tambah service + permission)
- Patch MainActivity.kt (integrasi navigasi + tombol Cari)
- Buat LocateService.kt (HP LANSIA - foreground service ntfy listener)
- Buat FindJamaah.kt (HP TOUR LEADER - UI cari lokasi)
"""
import sys, os, shutil

def detect_app():
    for p in ("android-app/app", "app"):
        if os.path.isdir(os.path.join(p, "src/main")):
            return p
    print("ERROR: Jalankan dari root repo (yang ada android-app/ atau app/)")
    sys.exit(1)

def read(p):
    with open(p, "r", encoding="utf-8", newline="") as f:
        return f.read().replace("\r\n", "\n").replace("\r", "\n")

def write(p, c):
    with open(p, "w", encoding="utf-8", newline="\n") as f:
        f.write(c)

def patch(c, find, repl, label, req=True):
    if find not in c:
        if req:
            print(f"\nERROR [{label}]: pola tidak ditemukan")
            print("  Cari: " + repr(find[:100]))
            sys.exit(2)
        print(f"  SKIP [{label}]: sudah di-patch sebelumnya")
        return c
    return c.replace(find, repl, 1)

APP = detect_app()
JAVA = f"{APP}/src/main/java/com/imtiyaztour/app"
MANIFEST = f"{APP}/src/main/AndroidManifest.xml"
MA = f"{JAVA}/MainActivity.kt"

print(f"App: {APP}\n")

for f in (MANIFEST, MA):
    if not os.path.isfile(f):
        print(f"ERROR: File tidak ada: {f}"); sys.exit(1)
    shutil.copy(f, f + ".bak4")
    print(f"Backup: {f}.bak4")
print()

# ============ 1. AndroidManifest.xml ============
print("Patch AndroidManifest.xml...")
c = read(MANIFEST)
c = patch(c,
    '    <uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED" />',
    '    <uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED" />\n'
    '    <!-- Fitur Tracking Lansia -->\n'
    '    <uses-permission android:name="android.permission.FOREGROUND_SERVICE" />\n'
    '    <uses-permission android:name="android.permission.FOREGROUND_SERVICE_LOCATION" />',
    "manifest-perm")

c = patch(c,
    '        <receiver android:name=".ReminderReceiver" android:exported="false" />',
    '        <receiver android:name=".ReminderReceiver" android:exported="false" />\n'
    '\n'
    '        <!-- Tracking Lansia: foreground service siaga ntfy -->\n'
    '        <service\n'
    '            android:name=".LocateService"\n'
    '            android:exported="false"\n'
    '            android:foregroundServiceType="location" />',
    "manifest-service")
write(MANIFEST, c)
print("OK AndroidManifest.xml\n")

# ============ 2. MainActivity.kt ============
print("Patch MainActivity.kt...")
c = read(MA)

# 2a. State
c = patch(c,
    '    var showReminder by remember { mutableStateOf(false) }',
    '    var showReminder by remember { mutableStateOf(false) }\n'
    '    var showFindJamaah by remember { mutableStateOf(false) }',
    "state")

# 2b. BackHandler enable
c = patch(c,
    '                  selectedPembimbing != null || showManasik || showJournalList ||\n'
    '                  showJournalEditActive || showReminder',
    '                  selectedPembimbing != null || showManasik || showJournalList ||\n'
    '                  showJournalEditActive || showReminder || showFindJamaah',
    "backhandler-enable")

# 2c. BackHandler when
c = patch(c,
    '            showReminder -> showReminder = false\n        }',
    '            showReminder -> showReminder = false\n'
    '            showFindJamaah -> showFindJamaah = false\n        }',
    "backhandler-when")

# 2d. Navigation
c = patch(c,
    '                showReminder -> ReminderScreen(onBack = { showReminder = false })',
    '                showReminder -> ReminderScreen(onBack = { showReminder = false })\n'
    '                showFindJamaah -> FindJamaahScreen(onBack = { showFindJamaah = false })',
    "nav")

# 2e. Tambah tombol "Cari Lokasi Jamaah" di kartu Radio tab Saya
c = patch(c,
    '                    Button(onClick = onRadioClick, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F7A5A)), shape = RoundedCornerShape(12.dp)) { Text("Buka Radio") }',
    '                    Button(onClick = onRadioClick, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F7A5A)), shape = RoundedCornerShape(12.dp)) { Text("Buka Radio") }\n'
    '                    Spacer(Modifier.height(8.dp))\n'
    '                    OutlinedButton(\n'
    '                        onClick = { onFindJamaahClick() },\n'
    '                        modifier = Modifier.fillMaxWidth(),\n'
    '                        shape = RoundedCornerShape(12.dp)\n'
    '                    ) { Text("Cari Lokasi Jamaah") }',
    "saya-button", req=False)

# 2f. Tambah parameter onFindJamaahClick di SayaScreen
c = patch(c,
    'fun SayaScreen(onRadioClick: () -> Unit) {',
    'fun SayaScreen(onRadioClick: () -> Unit, onFindJamaahClick: () -> Unit = {}) {',
    "sayascreen-sig", req=False)

# 2g. Pass parameter di panggilan SayaScreen
c = patch(c,
    '                    5 -> SayaScreen(onRadioClick = { showRadio = true })',
    '                    5 -> SayaScreen(onRadioClick = { showRadio = true }, onFindJamaahClick = { showFindJamaah = true })',
    "sayascreen-call")

write(MA, c)
print("OK MainActivity.kt\n")

# ============ 3. LocateService.kt (HP LANSIA) ============
print("Membuat LocateService.kt...")
LOCATE = '''package com.imtiyaztour.app

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.BatteryManager
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume

// ============================================================================
// LOCATE SERVICE -- berjalan siaga di HP LANSIA.
// Buka stream ke ntfy.sh/{topic}/json, tunggu pesan "locate".
// Begitu pesan masuk: ambil GPS baru, kirim balik ke server via /api/track-ping.
// Notifikasi permanen supaya OS tidak mematikan service + transparansi ke lansia.
// ============================================================================

data class TrackPingRequest(
    val jamaah_id: String,
    val token: String,
    val request_id: String?,
    val latitude: Double,
    val longitude: Double,
    val accuracy: Float,
    val battery: Int
)

data class TrackPingResponse(val success: Boolean? = null, val error: String? = null)

interface TrackApiService {
    @POST("api/track-ping")
    suspend fun sendTrackPing(@Body body: TrackPingRequest): TrackPingResponse
}

object TrackApiClient {
    val service: TrackApiService by lazy {
        Retrofit.Builder()
            .baseUrl(ApiConfig.BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(TrackApiService::class.java)
    }
}

class LocateService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val client = OkHttpClient.Builder()
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .connectTimeout(15, TimeUnit.SECONDS)
        .build()
    private var listeningJob: Job? = null

    companion object {
        const val CHANNEL_ID = "locate_service_channel"
        const val NOTIF_ID = 9001
        const val ACTION_STOP = "com.imtiyaztour.app.STOP_LOCATE"

        fun start(ctx: Context) {
            val i = Intent(ctx, LocateService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) ctx.startForegroundService(i)
            else ctx.startService(i)
        }

        fun stop(ctx: Context) {
            ctx.stopService(Intent(ctx, LocateService::class.java))
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) { stopSelf(); return START_NOT_STICKY }
        startForeground(NOTIF_ID, buildNotification())
        if (listeningJob?.isActive != true) listeningJob = scope.launch { listenLoop() }
        return START_STICKY
    }

    private fun buildNotification(): Notification {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val mgr = getSystemService(NotificationManager::class.java)
            if (mgr.getNotificationChannel(CHANNEL_ID) == null) {
                mgr.createNotificationChannel(
                    NotificationChannel(CHANNEL_ID, "Mode Aman", NotificationManager.IMPORTANCE_LOW).apply {
                        description = "Menjaga koneksi dengan Tour Leader"
                    }
                )
            }
        }
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Mode Aman Aktif")
            .setContentText("Lokasi Anda siap dicari oleh Tour Leader")
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private suspend fun listenLoop() {
        while (scope.isActive) {
            try {
                val jamaahId = Prefs.getJamaahId(this)
                if (jamaahId.isBlank()) { delay(30_000); continue }
                val topic = "imtiyaz-loc-j$jamaahId"
                val req = Request.Builder().url("https://ntfy.sh/$topic/json").build()
                client.newCall(req).execute().use { resp ->
                    val source = resp.body?.source() ?: return@use
                    while (!source.exhausted()) {
                        val line = source.readUtf8Line() ?: break
                        handleLine(line)
                    }
                }
            } catch (e: Exception) {
                // Jaringan error -- tunggu lalu retry
            }
            delay(5_000)
        }
    }

    private fun handleLine(line: String) {
        try {
            val j = JSONObject(line)
            if (j.optString("event") != "message") return
            val requestId = j.optString("message").takeIf { it.isNotBlank() } ?: return
            scope.launch { respondWithLocation(requestId) }
        } catch (e: Exception) { }
    }

    @Suppress("MissingPermission")
    private suspend fun respondWithLocation(requestId: String) {
        val loc = try {
            withTimeoutOrNull(15_000) {
                suspendCancellableCoroutine<android.location.Location?> { cont ->
                    val c = LocationServices.getFusedLocationProviderClient(this@LocateService)
                    val cts = com.google.android.gms.tasks.CancellationTokenSource()
                    c.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cts.token)
                        .addOnSuccessListener { if (cont.isActive) cont.resume(it) }
                        .addOnFailureListener { if (cont.isActive) cont.resume(null) }
                    cont.invokeOnCancellation { cts.cancel() }
                }
            }
        } catch (e: Exception) { null }

        if (loc == null) return

        try {
            TrackApiClient.service.sendTrackPing(
                TrackPingRequest(
                    jamaah_id = Prefs.getJamaahId(this),
                    token = Prefs.getToken(this),
                    request_id = requestId,
                    latitude = loc.latitude,
                    longitude = loc.longitude,
                    accuracy = loc.accuracy,
                    battery = getBattery()
                )
            )
        } catch (e: Exception) { }
    }

    private fun getBattery(): Int = try {
        val bm = getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
    } catch (e: Exception) { -1 }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }
}
'''
with open(f"{JAVA}/LocateService.kt", "w", encoding="utf-8", newline="\n") as f:
    f.write(LOCATE)
print("OK LocateService.kt\n")

# ============ 4. FindJamaah.kt (HP TL) ============
print("Membuat FindJamaah.kt...")
FIND = '''package com.imtiyaztour.app

import android.content.Intent
import android.net.Uri
import android.location.Location
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import kotlin.coroutines.resume
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

// ============================================================================
// FIND JAMAAH -- UI untuk Tour Leader.
// Daftar jamaah di kanal TL -> tombol "Cari" per jamaah -> polling status
// -> tampil koordinat + jarak + tombol Buka Peta.
// ============================================================================

data class KanalJamaah(val id: String, val nama: String)
data class FindRequest(val jamaah_id: String, val token: String, val target_jamaah_id: String)
data class FindResponse(val request_id: String? = null, val status: String? = null, val error: String? = null)
data class FindStatus(
    val status: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val accuracy: Double? = null,
    val battery: Int? = null,
    val requested_at: Long? = null,
    val responded_at: Long? = null,
    val error: String? = null
)

interface FindApiService {
    @POST("api/kanal-jamaah")
    suspend fun getKanalJamaah(@Body body: Map<String, String>): List<KanalJamaah>

    @POST("api/find")
    suspend fun requestFind(@Body body: FindRequest): FindResponse

    @GET("api/find/{id}/status")
    suspend fun getFindStatus(@Path("id") id: String): FindStatus
}

object FindApiClient {
    val service: FindApiService by lazy {
        Retrofit.Builder()
            .baseUrl(ApiConfig.BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(FindApiService::class.java)
    }
}

@Suppress("MissingPermission")
private suspend fun getTLLocation(context: android.content.Context): Location? = withTimeoutOrNull(10_000) {
    suspendCancellableCoroutine { cont ->
        val c = LocationServices.getFusedLocationProviderClient(context)
        val cts = com.google.android.gms.tasks.CancellationTokenSource()
        c.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, cts.token)
            .addOnSuccessListener { if (cont.isActive) cont.resume(it) }
            .addOnFailureListener { if (cont.isActive) cont.resume(null) }
        cont.invokeOnCancellation { cts.cancel() }
    }
}

private fun hitungJarak(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    val r = 6371000.0
    val p1 = Math.toRadians(lat1); val p2 = Math.toRadians(lat2)
    val dp = Math.toRadians(lat2 - lat1); val dl = Math.toRadians(lon2 - lon1)
    val a = sin(dp/2).pow(2) + cos(p1)*cos(p2)*sin(dl/2).pow(2)
    return r * 2 * atan2(sqrt(a), sqrt(1-a))
}

private fun formatJarak(m: Double): String = when {
    m < 1000 -> "${m.toInt()} meter"
    else -> String.format("%.1f km", m/1000)
}

@Composable
fun FindJamaahScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val jamaahId = Prefs.getJamaahId(context)
    val token = Prefs.getToken(context)

    var list by remember { mutableStateOf<List<KanalJamaah>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var errorMsg by remember { mutableStateOf("") }
    var activeFind by remember { mutableStateOf<Pair<KanalJamaah, String>?>(null) }
    var findResult by remember { mutableStateOf<FindStatus?>(null) }
    var tlLocation by remember { mutableStateOf<Location?>(null) }

    LaunchedEffect(Unit) {
        loading = true; errorMsg = ""
        try {
            list = withContext(Dispatchers.IO) {
                FindApiClient.service.getKanalJamaah(mapOf("jamaah_id" to jamaahId, "token" to token))
            }
            tlLocation = getTLLocation(context)
        } catch (e: Exception) {
            errorMsg = "Gagal memuat daftar jamaah -- pastikan Anda Tour Leader dan koneksi stabil"
        }
        loading = false
    }

    LaunchedEffect(activeFind?.second) {
        val reqId = activeFind?.second ?: return@LaunchedEffect
        val start = System.currentTimeMillis()
        while (System.currentTimeMillis() - start < 90_000) {
            try {
                val s = withContext(Dispatchers.IO) { FindApiClient.service.getFindStatus(reqId) }
                if (s.status == "responded") { findResult = s; return@LaunchedEffect }
            } catch (e: Exception) { }
            delay(2000)
        }
        findResult = FindStatus(status = "timeout")
    }

    if (activeFind != null) {
        val (target, _) = activeFind!!
        val r = findResult
        Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            TextButton(onClick = { activeFind = null; findResult = null }) { Text("<- Kembali ke daftar") }
            Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(2.dp), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Mencari Lokasi", fontSize = 12.sp, color = Color.Gray)
                    Text(target.nama, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color(0xFF0F7A5A))
                    Spacer(Modifier.height(16.dp))
                    if (r == null || r.status == "sent" || r.status == "pending") {
                        CircularProgressIndicator(color = Color(0xFF0F7A5A))
                        Spacer(Modifier.height(8.dp))
                        Text("Menunggu HP merespons...", fontSize = 12.sp, color = Color.Gray)
                        Text("Biasanya 15-30 detik", fontSize = 10.sp, color = Color.LightGray)
                    } else if (r.status == "responded") {
                        val lat = r.latitude ?: 0.0; val lon = r.longitude ?: 0.0
                        Text("Lokasi ditemukan", fontWeight = FontWeight.Bold, color = Color(0xFF0F7A5A))
                        Spacer(Modifier.height(12.dp))
                        val tl = tlLocation
                        if (tl != null) {
                            val jarak = hitungJarak(tl.latitude, tl.longitude, lat, lon)
                            Text("Jarak dari Anda: ${formatJarak(jarak)}", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Spacer(Modifier.height(8.dp))
                        }
                        Text("Koordinat: ${"%.5f".format(lat)}, ${"%.5f".format(lon)}", fontSize = 12.sp)
                        Text("Akurasi: ±${r.accuracy?.toInt() ?: 0} meter", fontSize = 11.sp, color = Color.Gray)
                        Text("Baterai: ${r.battery ?: 0}%", fontSize = 11.sp, color = Color.Gray)
                        Spacer(Modifier.height(16.dp))
                        Button(
                            onClick = {
                                val uri = Uri.parse("geo:$lat,$lon?q=$lat,$lon(${Uri.encode(target.nama)})")
                                context.startActivity(Intent(Intent.ACTION_VIEW, uri))
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F7A5A))
                        ) { Text("Buka di Peta") }
                        Spacer(Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = { activeFind = null; findResult = null },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("Cari Jamaah Lain") }
                    } else {
                        Text("HP tidak merespons", fontWeight = FontWeight.Bold, color = Color(0xFFDC2626))
                        Text("Jamaah mungkin tidak membawa HP, HP mati, atau offline.", fontSize = 12.sp, color = Color.Gray)
                        Spacer(Modifier.height(12.dp))
                        OutlinedButton(onClick = { activeFind = null; findResult = null }, modifier = Modifier.fillMaxWidth()) { Text("Coba Jamaah Lain") }
                    }
                }
            }
        }
        return
    }

    LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            Button(onClick = onBack, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F7A5A)), shape = RoundedCornerShape(8.dp)) { Text("<- Kembali") }
            Spacer(Modifier.height(12.dp))
            Text("Cari Lokasi Jamaah", fontWeight = FontWeight.Bold, fontSize = 22.sp)
            Text("Ketuk tombol cari untuk minta lokasi terkini dari HP jamaah.", fontSize = 12.sp, color = Color.Gray)
            Spacer(Modifier.height(8.dp))
            if (loading) LinearProgressIndicator(Modifier.fillMaxWidth())
            if (errorMsg.isNotEmpty()) { Text(errorMsg, fontSize = 12.sp, color = Color(0xFFDC2626)); Spacer(Modifier.height(8.dp)) }
        }
        items(list) { j ->
            Card(shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(1.dp), modifier = Modifier.fillMaxWidth()) {
                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Person, contentDescription = null, tint = Color(0xFF0F7A5A))
                    Spacer(Modifier.width(12.dp))
                    Text(j.nama, fontWeight = FontWeight.Bold, fontSize = 14.sp, modifier = Modifier.weight(1f))
                    Button(
                        onClick = {
                            scope.launch {
                                try {
                                    val resp = withContext(Dispatchers.IO) {
                                        FindApiClient.service.requestFind(FindRequest(jamaahId, token, j.id))
                                    }
                                    if (resp.request_id != null) {
                                        activeFind = j to resp.request_id
                                        findResult = null
                                    } else {
                                        errorMsg = resp.error ?: "Gagal meminta lokasi"
                                    }
                                } catch (e: Exception) {
                                    errorMsg = "Gagal mengirim permintaan: ${e.message}"
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F7A5A)),
                        shape = RoundedCornerShape(8.dp)
                    ) { Text("Cari", fontSize = 12.sp) }
                }
            }
        }
        if (!loading && list.isEmpty() && errorMsg.isEmpty()) {
            item {
                Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3CD)), shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
                    Text("Belum ada jamaah di kanal Anda. Minta Admin menugaskan jamaah ke kanal radio Anda terlebih dahulu.", fontSize = 11.sp, color = Color(0xFF92400E), modifier = Modifier.padding(12.dp))
                }
            }
        }
    }
}
'''
with open(f"{JAVA}/FindJamaah.kt", "w", encoding="utf-8", newline="\n") as f:
    f.write(FIND)
print("OK FindJamaah.kt\n")

print("=" * 60)
print("SEMUA PATCH TRACKING v2.11.0 BERHASIL")
print("=" * 60)
print("\nSelanjutnya:")
print("  1. Edit MainActivity.kt manual (tambah LaunchedEffect LocateService.start)")
print("  2. git add . && git commit && git push")
print("\nRollback: *.bak4 restore manual")