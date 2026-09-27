package com.imtiyaztour.app

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
                    KirimLokasiHelper.kirimLokasi(applicationContext, requestId)
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
