package com.imtiyaztour.app

import android.content.Context
import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.GeoPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/**
 * v2.13.0: Kirim lokasi GPS ke Firestore saat dapat FCM "minta-lokasi".
 * Menggantikan mekanisme ntfy.sh lama.
 */
object KirimLokasiHelper {

    private const val TAG = "KirimLokasiHelper"

    fun kirimLokasi(context: Context, requestId: String) {
        CoroutineScope(Dispatchers.IO).launch {
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
                    Log.w(TAG, "Lokasi null, tidak dikirim")
                    return@launch
                }

                val jamaahId = Prefs.getJamaahId(context)
                if (jamaahId.isBlank()) {
                    Log.w(TAG, "Jamaah ID kosong, skip kirim lokasi")
                    return@launch
                }
                val data = hashMapOf<String, Any>(
                    "latitude" to loc.latitude,
                    "longitude" to loc.longitude,
                    "accuracy" to loc.accuracy.toDouble(),
                    "battery" to fix.batteryLevel,
                    "geo" to GeoPoint(loc.latitude, loc.longitude),
                    "request_id" to requestId,
                    "updated_at" to System.currentTimeMillis()
                )

                FirebaseFirestore.getInstance()
                    .collection("lokasi_jamaah")
                    .document(jamaahId)
                    .set(data)
                    .await()

                Log.d(TAG, "Lokasi terkirim ke Firestore: $requestId")
            } catch (e: Exception) {
                Log.e(TAG, "Gagal kirim lokasi", e)
            }
        }
    }
}
