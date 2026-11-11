package com.imtiyaztour.app

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat

/**
 * Foreground Service untuk LiveKit Radio.
 *
 * Root cause: Android membunuh proses background tanpa FGS saat layar mati.
 * FGS ini menjaga proses + WebRTC tetap hidup saat layar mati / app background.
 *
 * FGS Type (Android 14+):
 * - mediaPlayback (jamaah mendengarkan)
 * - microphone (TL siaran)
 */
class RadioForegroundService : Service() {

    private var wakeLock: PowerManager.WakeLock? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val mode = intent?.getStringExtra(EXTRA_MODE) ?: "listener"
        val title = if (mode == "broadcaster") {
            "\uD83D\uDD34 Siaran Radio Aktif"
        } else {
            "\uD83D\uDD0A Mendengarkan Radio"
        }

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText("Imtiyaz Radio")
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setSilent(true)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val types = if (mode == "broadcaster") {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE or
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
            } else {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
            }
            startForeground(NOTIFICATION_ID, notification, types)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }

        if (wakeLock == null) {
            try {
                val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
                wakeLock = pm.newWakeLock(
                    PowerManager.PARTIAL_WAKE_LOCK,
                    "ImtiyazRadio::LiveKitWakeLock"
                ).apply {
                    setReferenceCounted(false)
                    acquire(4 * 60 * 60 * 1000L)
                }
                Log.d(TAG, "WakeLock acquired")
            } catch (e: Exception) {
                Log.e(TAG, "WakeLock failed: ${e.message}")
            }
        }

        return START_NOT_STICKY
    }

    override fun onDestroy() {
        try {
            wakeLock?.let { if (it.isHeld) it.release() }
        } catch (_: Exception) {}
        wakeLock = null
        Log.d(TAG, "WakeLock released, service destroyed")
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Radio Imtiyaz",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Notifikasi siaran Radio Imtiyaz"
                setShowBadge(false)
            }
            val nm = getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(channel)
        }
    }

    companion object {
        private const val TAG = "RadioFGS"
        private const val CHANNEL_ID = "imtiyaz_radio_fgs"
        private const val NOTIFICATION_ID = 4517
        const val EXTRA_MODE = "mode"

        fun start(context: Context, isBroadcaster: Boolean) {
            try {
                val intent = Intent(context, RadioForegroundService::class.java).apply {
                    putExtra(EXTRA_MODE, if (isBroadcaster) "broadcaster" else "listener")
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
                Log.d(TAG, "Service started (broadcaster=$isBroadcaster)")
            } catch (e: Exception) {
                Log.e(TAG, "Start service failed: ${e.message}")
            }
        }

        fun stop(context: Context) {
            try {
                context.stopService(Intent(context, RadioForegroundService::class.java))
                Log.d(TAG, "Service stop requested")
            } catch (e: Exception) {
                Log.e(TAG, "Stop service failed: ${e.message}")
            }
        }
    }
}
