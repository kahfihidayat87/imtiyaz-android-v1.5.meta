package com.imtiyaztour.app

import android.content.Context
import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/**
 * v2.13.0 TAHAP 22 (Batch 4c): Kirim alert izin ke Firestore.
 *
 * Dipakai saat OtaDetector deteksi izin hilang (OTA / user revoke).
 * TL baca koleksi alert_izin real-time → tampil banner.
 */
object AlertIzinHelper {
    private const val TAG = "AlertIzinHelper"
    private const val THROTTLE_MS = 60 * 60 * 1000L // 1 jam

    fun kirimAlert(ctx: Context, reason: String) {
        val jamaahId = Prefs.getJamaahId(ctx)
        if (jamaahId.isBlank()) {
            Log.w(TAG, "Jamaah ID kosong, skip alert")
            return
        }
        // Throttle: jangan kirim lebih dari 1× per jam
        val lastAlert = Prefs.get(ctx).getLong("alert_izin_last", 0L)
        if (System.currentTimeMillis() - lastAlert < THROTTLE_MS) {
            Log.d(TAG, "Alert throttled, skip")
            return
        }
        Prefs.get(ctx).edit().putLong("alert_izin_last", System.currentTimeMillis()).apply()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val nama = Prefs.getNama(ctx)
                FirebaseFirestore.getInstance()
                    .collection("alert_izin")
                    .document(jamaahId)
                    .set(
                        hashMapOf(
                            "jamaah_id" to jamaahId,
                            "nama" to nama,
                            "reason" to reason,
                            "detected_at" to System.currentTimeMillis(),
                            "resolved" to false
                        ),
                        SetOptions.merge()
                    )
                    .await()
                Log.d(TAG, "Alert terkirim: $reason")
            } catch (e: Exception) {
                Log.e(TAG, "Gagal kirim alert", e)
            }
        }
    }

    fun clearAlert(ctx: Context) {
        val jamaahId = Prefs.getJamaahId(ctx)
        if (jamaahId.isBlank()) return
        CoroutineScope(Dispatchers.IO).launch {
            try {
                FirebaseFirestore.getInstance()
                    .collection("alert_izin")
                    .document(jamaahId)
                    .delete()
                    .await()
                Log.d(TAG, "Alert dihapus (izin restored)")
            } catch (e: Exception) {
                Log.w(TAG, "Gagal hapus alert", e)
            }
        }
    }
}
