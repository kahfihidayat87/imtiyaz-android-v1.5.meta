package com.imtiyaztour.app

import android.content.Context
import android.os.BatteryManager
import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.GeoPoint
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await

/**
 * v2.13.0 TAHAP 22 (Batch 4d, FIX bug-1): Kirim lokasi GPS ke Firestore saat FCM.
 *
 * PENTING: fungsi ini suspend, JANGAN spawn coroutine sendiri.
 * Caller (FcmService) WAJIB hold WakeLock sampai fungsi ini selesai,
 * karena onMessageReceived return instan -- kalau coroutine di-spawn sendiri,
 * OS bisa kill process sebelum Firestore write.
 */
object KirimLokasiHelper {

    private const val TAG = "KirimLokasiHelper"
    private const val MAX_CACHE_AGE_MS = 10 * 60 * 1000L

    suspend fun kirimLokasi(context: Context, requestId: String) {
        val jamaahId = Prefs.getJamaahId(context)
        if (jamaahId.isBlank()) {
            Log.w(TAG, "Jamaah ID kosong, skip kirim lokasi")
            return
        }

        // [1] KIRIM CACHE DULU (fast) -- penting: request_id WAJIB ada,
        // supaya TL listener bisa match.
        val cached = LocationCache.get(context)
        if (cached != null && cached.isFresh(MAX_CACHE_AGE_MS)) {
            try {
                sendToFirestore(
                    jamaahId = jamaahId, requestId = requestId,
                    lat = cached.lat, lon = cached.lon,
                    accuracy = cached.accuracy,
                    battery = getBatteryLevel(context),
                    source = "cache",
                    ageMs = cached.ageMs()
                )
                Log.d(TAG, "Cache terkirim (age=${cached.ageMs()}ms)")
            } catch (e: Exception) {
                Log.w(TAG, "Gagal kirim cache", e)
            }
        }

        // [2] AMBIL GPS AKURAT (slow)
        try {
            val fix = LocationHelper.getBestLocation(
                context = context,
                targetAccuracyM = LocationHelper.DEFAULT_TARGET_ACCURACY_M,
                timeoutMs = LocationHelper.DEFAULT_TIMEOUT_MS,
                maxSamples = LocationHelper.DEFAULT_MAX_SAMPLES,
                adaptiveByBattery = true
            )
            val loc = fix.location
            if (loc == null) {
                Log.w(TAG, "Lokasi akurat null, hanya cache yang terkirim")
                return
            }

            // [3] KIRIM GPS AKURAT
            sendToFirestore(
                jamaahId = jamaahId, requestId = requestId,
                lat = loc.latitude, lon = loc.longitude,
                accuracy = loc.accuracy,
                battery = fix.batteryLevel,
                source = "gps_akurat",
                ageMs = 0L
            )

            // [4] UPDATE CACHE
            LocationCache.save(context, loc.latitude, loc.longitude, loc.accuracy, "gps_akurat")
            Log.d(TAG, "GPS akurat terkirim & cache diperbarui: $requestId")
        } catch (e: Exception) {
            Log.e(TAG, "Gagal kirim lokasi akurat", e)
        }
    }

    private suspend fun sendToFirestore(
        jamaahId: String, requestId: String,
        lat: Double, lon: Double, accuracy: Float,
        battery: Int, source: String, ageMs: Long
    ) {
        val data = hashMapOf<String, Any>(
            "latitude" to lat,
            "longitude" to lon,
            "accuracy" to accuracy.toDouble(),
            "battery" to battery,
            "geo" to GeoPoint(lat, lon),
            "request_id" to requestId,
            "updated_at" to System.currentTimeMillis(),
            "source" to source,
            "age_ms" to ageMs
        )
        FirebaseFirestore.getInstance()
            .collection("lokasi_jamaah")
            .document(jamaahId)
            .set(data, SetOptions.merge())
            .await()
    }

    private fun getBatteryLevel(ctx: Context): Int {
        return try {
            val bm = ctx.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
            bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
        } catch (e: Exception) { -1 }
    }
}
