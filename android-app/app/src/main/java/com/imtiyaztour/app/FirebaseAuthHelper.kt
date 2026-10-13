package com.imtiyaztour.app

import android.util.Log
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

// v2.14.0: Firebase Custom Auth untuk akses Firestore dengan rules berbasis custom claims.
data class FirebaseTokenResponse(
    val token: String? = null,
    val uid: String? = null,
    val role: String? = null,
    val error: String? = null
)

object FirebaseAuthHelper {
    private const val TAG = "FirebaseAuthHelper"

    /**
     * Login ke Firebase pakai custom token dari server (/api/firebase-token).
     * Return true kalau sukses, false kalau gagal (jangan block login WP).
     */
    suspend fun loginFirebase(jamaahId: String, wpToken: String): Boolean =
        withContext(Dispatchers.IO) {
            try {
                val resp = ApiClient.service.firebaseToken(
                    mapOf("jamaah_id" to jamaahId, "token" to wpToken)
                )
                if (resp.token.isNullOrBlank()) {
                    Log.w(TAG, "Token kosong: ${resp.error}")
                    return@withContext false
                }
                Firebase.auth.signInWithCustomToken(resp.token).await()
                Log.i(TAG, "Firebase login OK — uid=${resp.uid}, role=${resp.role}")
                true
            } catch (e: Exception) {
                Log.w(TAG, "Firebase login gagal: ${e.message}")
                false
            }
        }

    /** Logout Firebase (dipanggil saat user logout WP). */
    fun logout() {
        try {
            Firebase.auth.signOut()
        } catch (e: Exception) {
            Log.w(TAG, "signOut error: ${e.message}")
        }
    }
}
