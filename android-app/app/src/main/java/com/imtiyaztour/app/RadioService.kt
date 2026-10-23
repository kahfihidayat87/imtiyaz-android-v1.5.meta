package com.imtiyaztour.app

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat

class RadioService : Service() {
    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        try {
            val channelId = "radio_foreground"
            val channelName = "Radio Imtiyaz"
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_LOW)
                val nm = getSystemService(NotificationManager::class.java)
                nm.createNotificationChannel(channel)
            }
            val notif = NotificationCompat.Builder(this, channelId)
                .setContentTitle("Radio Imtiyaz")
                .setContentText("Radio sedang berjalan di latar belakang")
                .setSmallIcon(android.R.drawable.ic_media_play)
                .setOngoing(true)
                .build()
            if (Build.VERSION.SDK_INT >= 34) {
                startForeground(101, notif, android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK or android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE)
            } else if (Build.VERSION.SDK_INT >= 30) {
                startForeground(101, notif, android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK or android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE)
            } else if (Build.VERSION.SDK_INT >= 29) {
                startForeground(101, notif, android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)
            } else {
                startForeground(101, notif)
            }
        } catch (e: Exception) {
            android.util.Log.e("RadioService", "StartForeground gagal: ${e.message}")
            stopSelf()
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        RadioLiveKit.berhenti()
    }
}
