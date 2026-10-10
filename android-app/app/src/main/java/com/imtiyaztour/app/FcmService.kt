package com.imtiyaztour.app

import android.content.Context
import android.os.PowerManager
import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.concurrent.TimeUnit

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

        /**
         * v2.13.0 TAHAP 22 (Batch 4b): Fallback kalau FGS start ditolak OS
         * atau coroutine terputus. WorkManager jalan di background.
         */
        fun enqueueFallbackKirim(ctx: Context, requestId: String) {
            try {
                val constraints = Constraints.Builder()
                    .setRequiresBatteryNotLow(false)
                    .build()
                val req = OneTimeWorkRequestBuilder<KirimLokasiWorker>()
                    .setInputData(workDataOf(KirimLokasiWorker.KEY_REQUEST_ID to requestId))
                    .setConstraints(constraints)
                    .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 15, TimeUnit.SECONDS)
                    .build()
                WorkManager.getInstance(ctx)
                    .enqueueUniqueWork(
                        "kirim_lokasi_$requestId",
                        ExistingWorkPolicy.KEEP,
                        req
                    )
                Log.d(TAG, "Fallback WorkManager enqueued: $requestId")
            } catch (e: Exception) {
                Log.e(TAG, "Gagal enqueue fallback", e)
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
                    // v2.13.0 TAHAP 22 (Batch 4b): try/catch + fallback WorkManager
                    try {
                        withWakeLock(applicationContext) {
                            KirimLokasiHelper.kirimLokasi(applicationContext, requestId)
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Kirim lokasi gagal, pakai WorkManager fallback", e)
                        enqueueFallbackKirim(applicationContext, requestId)
                    }
                }
                "pengumuman" -> {
                    NotifikasiHelper.tampilkanNotifikasi(
                        applicationContext,
                        data["judul"] ?: "Imtiyaz Tour",
                        data["pesan"] ?: ""
                    )
                }
                "heartbeat" -> {
                    // v2.13.0 TAHAP 22 (Batch 4c): heartbeat dari server (cron 6 jam).
                    // Tujuan: bangunkan HP dari standby bucket + reset FCM quota.
                    Log.d(TAG, "Heartbeat diterima: ts=${data["ts"]}")
                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            // 1) Refresh FCM token ke server (biar server tahu masih aktif)
                            val token = com.google.firebase.messaging.FirebaseMessaging
                                .getInstance().token.await()
                            FcmTokenStore.simpanToken(applicationContext, token)
                            FcmTokenStore.kirimKeServer(applicationContext, token)
                            // 2) Cek OTA + izin, kirim alert kalau ada yang hilang
                            OtaDetector.checkAndAlertIfNeeded(applicationContext)
                        } catch (e: Exception) {
                            Log.w(TAG, "Heartbeat handling gagal: ${e.message}")
                        }
                    }
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
