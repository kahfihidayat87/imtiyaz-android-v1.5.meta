package com.imtiyaztour.app

import android.content.Context
import android.util.Log
import io.livekit.android.LiveKit
import io.livekit.android.RoomOptions
import io.livekit.android.room.Room
import io.livekit.android.events.RoomEvent
import io.livekit.android.events.collect
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST

/**
 * v2.14.1: LiveKit radio TL - siaran real-time.
 *
 * FIX CRASH: LiveKit.create() dan setMicrophoneEnabled() WAJIB dipanggil di
 * Main thread (bukan IO). AudioSwitch initialization internal SDK butuh
 * Android Looper utama. Fetch token tetap di IO thread.
 */
object RadioLiveKit {

    private const val TAG = "RadioLiveKit"

    private var room: Room? = null
    private var eventJob: Job? = null

    data class LiveKitTokenResp(
        val token: String? = null,
        val url: String? = null,
        val is_tl: Boolean = false,
        val error: String? = null
    )

    interface LiveKitApi {
        @POST("api/radio/token")
        suspend fun getToken(@Body body: Map<String, String>): LiveKitTokenResp
    }

    private val api: LiveKitApi by lazy {
        Retrofit.Builder()
            .baseUrl(ApiConfig.BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(LiveKitApi::class.java)
    }

    fun mulai(
        context: Context,
        jamaahId: String,
        token: String,
        onStatus: (String) -> Unit
    ) {
        if (room != null) {
            onStatus("Sudah aktif")
            return
        }

        CoroutineScope(Dispatchers.Main).launch {
            try {
                onStatus("Menghubungkan...")

                // Fetch token di IO thread
                val resp = withContext(Dispatchers.IO) {
                    api.getToken(mapOf("jamaah_id" to jamaahId, "token" to token))
                }

                if (resp.token == null || resp.url == null) {
                    onStatus("Gagal dapat token: ${resp.error ?: "unknown"}")
                    return@launch
                }

                // LiveKit.create + connect di Main thread (wajib)
                val r = LiveKit.create(
                    appContext = context.applicationContext,
                    options = RoomOptions(
                        adaptiveStream = false,
                        dynacast = false
                    )
                )
                r.connect(url = resp.url, token = resp.token)
                room = r

                eventJob = CoroutineScope(Dispatchers.IO).launch {
                    r.events.collect { event ->
                        when (event) {
                            is RoomEvent.TrackSubscribed -> {
                                Log.d(TAG, "Track subscribed: ${event.track.kind}")
                                if (!resp.is_tl) {
                                    withContext(Dispatchers.Main) {
                                        onStatus("\uD83D\uDD0A Mendengarkan TL...")
                                    }
                                }
                            }
                            is RoomEvent.Disconnected -> {
                                Log.d(TAG, "Disconnected")
                                withContext(Dispatchers.Main) {
                                    onStatus("Koneksi terputus")
                                }
                            }
                            else -> {}
                        }
                    }
                }

                if (resp.is_tl) {
                    r.localParticipant.setMicrophoneEnabled(true)
                    onStatus("\uD83D\uDD34 Siaran aktif")
                    Log.d(TAG, "TL siaran aktif di room")
                } else {
                    onStatus("\uD83D\uDD0A Mendengarkan TL...")
                    Log.d(TAG, "Jamaah subscribe ke room")
                }

            } catch (e: Exception) {
                Log.e(TAG, "Error mulai LiveKit", e)
                onStatus("Gagal: ${e.message ?: "unknown"}")
                berhentiInternal()
            }
        }
    }

    fun berhenti() {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                berhentiInternal()
                Log.d(TAG, "LiveKit dihentikan")
            } catch (e: Exception) {
                Log.e(TAG, "Error stop", e)
            }
        }
    }

    private suspend fun berhentiInternal() {
        try {
            eventJob?.cancel()
            eventJob = null
            room?.localParticipant?.setMicrophoneEnabled(false)
            room?.disconnect()
        } catch (_: Exception) { }
        room = null
    }

    fun sedangAktif(): Boolean = room != null
}
