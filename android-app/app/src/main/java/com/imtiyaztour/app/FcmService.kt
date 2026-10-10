package com.imtiyaztour.app

import android.content.Context
import android.os.PowerManager
import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * v2.13.0: Penerima pesan Firebase Cloud Messaging (FCM).
 *
 * Menggantikan ntfy.sh yang punya limit 250 pesan/hari dan sering nyangkut.
 * FCM gratis unlimited, reliable, dan real-time.
 */
class FcmService : FirebaseMessagingService() {

    companion object {
        private const val TAG = "FcmService"
        private const val WAKELOCK_TIMEOUT_MS = 30_000L

        /**
         * v2.13.0 TAHAP 22: Jalankan blok dengan PARTIAL_WAKE_LOCK.
         * Auto-release setelah timeout 30 detik atau setelah blok selesai.
         */
        private fun withWakeLock(ctx: Context, block: () -> Unit) {
            val pm = ctx.getSystemService(Context.POWER_SERVICE) as? PowerManager
            val lock = pm?.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "imtiyaz:kirim_lokasi"
            )
            try {
                lock?.acquire(WAKELOCK_TIMEOUT_MS)
                block()
            } finally {
                try {
                    if (lock?.isHeld == true) lock.release()
                } catch (e: Exception) {
                    Log.w(TAG, "WakeLock release error: ${e.message}")
                }
            }
        }
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "FCM token baru: $token")

        CoroutineScope(Dispatchers.IO).launch {
            try {
                FcmTokenStore.simpanToken(applicationContext, token)
                FcmTokenStore.kirimKeServer(applicationContext, token)
            } catch (e: Exception) {
                Log.e(TAG, "Gagal simpan/kirim token", e)
            }
        }
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        Log.d(TAG, "Pesan dari: ${remoteMessage.from}")

        val data = remoteMessage.data
        if (data.isNotEmpty()) {
            when (data["tipe"]) {
                "minta-lokasi" -> {
                    val requestId = data["request_id"] ?: return
                    Log.d(TAG, "Minta lokasi: request_id=$requestId")
                    // v2.13.0 TAHAP 22 (Batch 4d): WakeLock guard 30 detik
                    // supaya coroutine kirim lokasi tidak dipotong Doze
                    withWakeLock(applicationContext) {
                        KirimLokasiHelper.kirimLokasi(applicationContext, requestId)
                    }
                }
                "pengumuman" -> {
                    NotifikasiHelper.tampilkanNotifikasi(
                        applicationContext,
                        data["judul"] ?: "Imtiyaz Tour",
                        data["pesan"] ?: ""
                    )
                }
            }
        }

        remoteMessage.notification?.let {
            NotifikasiHelper.tampilkanNotifikasi(
                applicationContext,
                it.title ?: "Imtiyaz Tour",
                it.body ?: ""
            )
        }
    }
}
