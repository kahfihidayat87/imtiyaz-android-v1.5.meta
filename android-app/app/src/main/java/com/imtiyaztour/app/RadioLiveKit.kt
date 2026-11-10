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

/** LiveKit radio. Token fetching uses IO; LiveKit and microphone operations use Main. */
object RadioLiveKit {
    private const val TAG = "RadioLiveKit"

    private var room: Room? = null
    private var eventJob: Job? = null
    private var connecting = false
    private var appContext: Context? = null

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
        if (room != null || connecting) {
            onStatus(if (room != null) "Sudah aktif" else "Menghubungkan...")
            return
        }
        val kode = kodeGelombang?.trim()
        if (kode.isNullOrBlank()) {
            onStatus("Gagal: kode gelombang belum diisi")
            return
        }

        connecting = true
        appContext = context.applicationContext
        CoroutineScope(Dispatchers.Main).launch {
            var serviceStarted = false
            try {
                onStatus("Menghubungkan...")
                val response = withContext(Dispatchers.IO) {
                    api.getToken(mapOf("jamaah_id" to jamaahId, "token" to token, "kode_gelombang" to kode))
                }
                val liveToken = response.token
                val liveUrl = response.url
                if (liveToken.isNullOrBlank() || liveUrl.isNullOrBlank()) {
                    onStatus("Gagal: ${response.error ?: "Token tidak valid"}")
                    return@launch
                }

                // Ask for microphone only after the backend confirms the user is TL.
                if (response.is_tl && context.checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                    onStatus("Izin mikrofon diperlukan untuk siaran TL.")
                    onMicPermissionRequired()
                    return@launch
                }

                // The foreground service and its wake lock must be active before connecting.
                RadioService.start(context.applicationContext, response.is_tl)
                serviceStarted = true

                val newRoom = LiveKit.create(
                    appContext = context.applicationContext,
                    options = RoomOptions(adaptiveStream = false, dynacast = false)
                )
                newRoom.connect(url = liveUrl, token = liveToken)
                room = newRoom

                eventJob = CoroutineScope(Dispatchers.IO).launch {
                    newRoom.events.collect { event ->
                        when (event) {
                            is RoomEvent.TrackSubscribed -> {
                                Log.d(TAG, "Track subscribed: ${event.track.kind}")
                                if (!response.is_tl) withContext(Dispatchers.Main) {
                                    onStatus("🔊 Mendengarkan TL...")
                                }
                            }
                            is RoomEvent.Disconnected -> {
                                Log.d(TAG, "Disconnected")
                                withContext(Dispatchers.Main) {
                                    onStatus("Koneksi terputus")
                                    berhenti()
                                }
                            }
                            else -> Unit
                        }
                    }
                }

                if (response.is_tl) {
                    try {
                        newRoom.localParticipant.setMicrophoneEnabled(true)
                    } catch (e: Throwable) {
                        Log.e(TAG, "Microphone activation failed", e)
                        onStatus("Mic gagal diakses. Cek izin atau tutup aplikasi lain.")
                        berhentiInternal()
                        RadioService.stop(context.applicationContext)
                        return@launch
                    }
                    onStatus("🔴 Siaran aktif")
                } else {
                    onStatus("🔊 Mendengarkan TL...")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Unable to start LiveKit", e)
                onStatus("Gagal: ${e.message ?: "unknown"}")
                berhentiInternal()
                if (serviceStarted) RadioService.stop(context.applicationContext)
            } finally {
                connecting = false
            }
        }
    }

    fun berhenti() {
        appContext?.let(RadioService::stop)
        CoroutineScope(Dispatchers.Main).launch { berhentiInternal() }
    }

    private suspend fun berhentiInternal() {
        try {
            eventJob?.cancel()
            eventJob = null
            room?.localParticipant?.setMicrophoneEnabled(false)
            room?.disconnect()
        } catch (e: Exception) {
            Log.w(TAG, "Error while stopping LiveKit", e)
        } finally {
            room = null
        }
    }

    fun sedangAktif(): Boolean = room != null
}
