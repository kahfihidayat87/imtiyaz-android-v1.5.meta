package com.imtiyaztour.app

import android.content.Context
import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

/**
 * v2.13.0: Manajemen FCM token.
 * Token disimpan lokal (SharedPreferences) dan dikirim ke Firestore
 * supaya server bisa target user tertentu.
 */
object FcmTokenStore {

    private const val TAG = "FcmTokenStore"
    private const val PREF_NAME = "fcm_prefs"
    private const val PREF_KEY = "fcm_token"

    fun simpanToken(context: Context, token: String) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit().putString(PREF_KEY, token).apply()
    }

    fun bacaToken(context: Context): String? =
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .getString(PREF_KEY, null)

    suspend fun kirimKeServer(context: Context, token: String) {
        val jamaahId = Prefs.getJamaahId(context)
        if (jamaahId.isBlank()) {
            Log.w(TAG, "Jamaah ID kosong, skip kirim token")
            return
        }
        val data = mapOf(
            "fcm_token" to token,
            "jamaah_id" to jamaahId,
            "updated_at" to System.currentTimeMillis()
        )
        try {
            FirebaseFirestore.getInstance()
                .collection("jamaah_tokens")
                .document(jamaahId)
                .set(data)
                .await()
            Log.d(TAG, "Token terkirim ke Firestore: $jamaahId")
        } catch (e: Exception) {
            Log.e(TAG, "Gagal kirim token", e)
        }
    }

    suspend fun subscribeTopicKanal(kanalId: String) {
        try {
            com.google.firebase.messaging.FirebaseMessaging.getInstance()
                .subscribeToTopic("kanal-$kanalId")
                .await()
            Log.d(TAG, "Subscribed ke kanal-$kanalId")
        } catch (e: Exception) {
            Log.e(TAG, "Gagal subscribe topic", e)
        }
    }
}
