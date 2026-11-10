package com.imtiyaztour.app

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
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
 * LiveKit.create() and setMicrophoneEnabled() must run on the main thread.
 * Token fetching remains on the IO dispatcher.
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
        kodeGelombang: String? = null,
        onMicPermissionRequired: () -> Unit = {},
        onStatus: (String) -> Unit
    ) {
        if (room != null) {
            onStatus("Sudah aktif")
            return
        }

        val kode = kodeGelombang?.trim()
        if (kode.isNullOrBlank()) {
            onStatus("Gagal: kode gelombang belum diisi")
            Log.w(TAG, "LiveKit start blocked: kodeGelombang is blank")
            return
        }

        CoroutineScope(Dispatchers.Main).launch {
            try {
                onStatus("Menghubungkan...")

                // Fetch token and role before deciding whether microphone access is needed.
                val resp = withContext(Dispatchers.IO) {
                    val body = mutableMapOf("jamaah_id" to jamaahId, "token" to token)
                    body["kode_gelombang"] = kode
                    api.getToken(body)
                }

                if (resp.token.isNullOrBlank() || resp.url.isNullOrBlank()) {
                    onStatus("Gagal: ${resp.error ?: "Token tidak valid"}")
                    return@launch
                }

                if (resp.is_tl &&
                    context.checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED
                ) {
                    onStatus("Izin mikrofon diperlukan untuk siaran TL.")
                    onMicPermissionRequired()
                    return@launch
                }

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
                                        onStatus("🔊 Mendengarkan TL...")
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
                    try {
                        r.localParticipant.setMicrophoneEnabled(true)
                    } catch (e: Throwable) {
                        Log.e(TAG, "Mic error: ${e.message}", e)
                        onStatus("Mic gagal diakses. Cek izin atau tutup aplikasi lain.")
                        berhentiInternal()
                        return@launch
                    }
                    onStatus("🔴 Siaran aktif")
                    Log.d(TAG, "TL siaran aktif di room")
                } else {
                    onStatus("🔊 Mendengarkan TL...")
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
