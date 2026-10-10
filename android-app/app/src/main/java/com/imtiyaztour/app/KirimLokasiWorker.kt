package com.imtiyaztour.app

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

/**
 * v2.13.0 TAHAP 22 (Batch 4b): WorkManager fallback.
 *
 * Dipakai kalau start FGS ditolak OS (Android 12+ foreground service restrictions)
 * atau app di-kill agresif oleh OEM (Xiaomi/Oppo/Vivo). Worker jalan di
 * background, kirim lokasi tanpa FGS.
 *
 * Data payload: request_id (String)
 * Retry: max 3x dengan exponential backoff (built-in WorkManager).
 */
class KirimLokasiWorker(
    ctx: Context,
    params: WorkerParameters
) : CoroutineWorker(ctx, params) {

    companion object {
        private const val TAG = "KirimLokasiWorker"
        const val KEY_REQUEST_ID = "request_id"
    }

    override suspend fun doWork(): Result {
        val requestId = inputData.getString(KEY_REQUEST_ID)
        if (requestId.isNullOrBlank()) {
            Log.w(TAG, "request_id kosong, skip")
            return Result.failure()
        }
        return try {
            Log.d(TAG, "Kirim lokasi via WorkManager: $requestId")
            KirimLokasiHelper.kirimLokasi(applicationContext, requestId)  // suspend, await
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Gagal kirim lokasi (retry=${runAttemptCount})", e)
            if (runAttemptCount >= 3) Result.failure() else Result.retry()
        }
    }
}
