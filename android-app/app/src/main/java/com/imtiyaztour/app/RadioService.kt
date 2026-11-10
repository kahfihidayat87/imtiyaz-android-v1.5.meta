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

class RadioService : Service() {
    companion object {
        private const val TAG = "RadioService"
        private const val CHANNEL_ID = "radio_foreground"
        private const val NOTIFICATION_ID = 101
        private const val EXTRA_IS_TL = "is_tl"

        fun start(context: Context, isTl: Boolean) {
            val intent = Intent(context, RadioService::class.java)
                .putExtra(EXTRA_IS_TL, isTl)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, RadioService::class.java))
        }
    }

    private var wakeLock: PowerManager.WakeLock? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val isTl = intent?.getBooleanExtra(EXTRA_IS_TL, false) ?: false
        try {
            val notification = NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Radio Imtiyaz")
                .setContentText(if (isTl) "Siaran live sedang berjalan" else "Radio sedang diputar")
                .setSmallIcon(android.R.drawable.ic_media_play)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .build()

            val serviceType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK or
                    if (isTl) ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE else 0
            } else {
                0
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(NOTIFICATION_ID, notification, serviceType)
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }

            // Keep the LiveKit audio/WebRTC work alive while the display is off.
            if (wakeLock?.isHeld != true) {
                val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
                wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "ImtiyazRadio::WakeLock")
                    .apply { acquire() }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Unable to start radio foreground service", e)
            stopSelf(startId)
            return START_NOT_STICKY
        }
        return START_NOT_STICKY
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(NotificationManager::class.java)
            if (manager.getNotificationChannel(CHANNEL_ID) == null) {
                manager.createNotificationChannel(
                    NotificationChannel(CHANNEL_ID, "Radio Imtiyaz", NotificationManager.IMPORTANCE_LOW)
                )
            }
        }
    }

    override fun onDestroy() {
        if (wakeLock?.isHeld == true) wakeLock?.release()
        wakeLock = null
        super.onDestroy()
    }
}
