package com.imtiyaztour.app

import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.LocationServices
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/**
 * v2.13.0 (TAHAP 16): Foreground Service "Mode Aman".
 *
 * Fungsi:
 *  - Notifikasi permanen "Mode Aman Aktif" (transparansi ke jamaah)
 *  - Refresh lokasi ke Firestore setiap 60 detik (last-known cache)
 *  - onTaskRemoved(): restart otomatis via AlarmManager saat app di-swipe dari Recents
 *  - START_STICKY: OS restart service jika di-kill low-memory
 *
 * Batasan yang disadari:
 *  - Force Stop oleh user -> TIDAK bisa restart (batasan Android)
 *  - Android 15+ lebih agresif mematikan foreground service
 */
class ModeAmanService : Service() {

    companion object {
        const val TAG = "ModeAmanService"
        const val CHANNEL_ID = "mode_aman_channel"
        const val NOTIF_ID = 9002
        const val ACTION_STOP = "com.imtiyaztour.app.STOP_MODE_AMAN"
        const val REFRESH_INTERVAL_MS = 60_000L

        fun start(ctx: Context) {
            val i = Intent(ctx, ModeAmanService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                ctx.startForegroundService(i)
            } else {
                ctx.startService(i)
            }
        }

        fun stop(ctx: Context) {
            ctx.stopService(Intent(ctx, ModeAmanService::class.java))
        }
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var refreshJob: Job? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }
        startForeground(NOTIF_ID, buildNotification())
        if (refreshJob?.isActive != true) {
            refreshJob = scope.launch { refreshLoop() }
        }
        return START_STICKY
    }

    /**
     * Dipanggil saat user swipe app dari Recents.
     * Jadwalkan restart service via AlarmManager (delay 1 detik).
     */
    override fun onTaskRemoved(rootIntent: Intent?) {
        Log.d(TAG, "onTaskRemoved - jadwalkan restart")
        val restartIntent = Intent(applicationContext, ModeAmanService::class.java)
        val pendingIntent = PendingIntent.getService(
            applicationContext,
            1,
            restartIntent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )
        val alarm = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarm.set(
            AlarmManager.RTC,
            System.currentTimeMillis() + 1000,
            pendingIntent
        )
        super.onTaskRemoved(rootIntent)
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val mgr = getSystemService(NotificationManager::class.java)
            if (mgr.getNotificationChannel(CHANNEL_ID) == null) {
                mgr.createNotificationChannel(
                    NotificationChannel(
                        CHANNEL_ID,
                        "Mode Aman",
                        NotificationManager.IMPORTANCE_LOW
                    ).apply {
                        description = "Menjaga lokasi Anda agar bisa ditemukan Tour Leader"
                        setShowBadge(false)
                    }
                )
            }
        }
    }

    private fun buildNotification(): Notification {
        val tapIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val tapPending = PendingIntent.getActivity(
            this, 0, tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, ModeAmanService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPending = PendingIntent.getService(
            this, 1, stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Mode Aman Aktif")
            .setContentText("Lokasi Anda siap dicari Tour Leader")
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(tapPending)
            .addAction(0, "Matikan", stopPending)
            .build()
    }

    private suspend fun refreshLoop() {
        val fused = LocationServices.getFusedLocationProviderClient(this)
        while (scope.isActive) {
            try {
                val loc = fused.lastLocation.await()
                if (loc != null) {
                    val jamaahId = Prefs.getJamaahId(this)
                    if (jamaahId.isNotBlank()) {
                        FirebaseFirestore.getInstance()
                            .collection("lokasi_jamaah")
                            .document(jamaahId)
                            .set(
                                mapOf(
                                    "latitude" to loc.latitude,
                                    "longitude" to loc.longitude,
                                    "accuracy" to loc.accuracy.toDouble(),
                                    "updated_at" to System.currentTimeMillis(),
                                    "source" to "mode_aman_service"
                                ),
                                SetOptions.merge()
                            )
                            .await()
                        Log.d(TAG, "Cache lokasi terkirim: $jamaahId")
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Refresh lokasi gagal: ${e.message}")
            }
            delay(REFRESH_INTERVAL_MS)
        }
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }
}
