package com.imtiyaztour.app

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
