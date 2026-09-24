package com.imtiyaztour.app

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.BatteryManager
import android.os.Looper
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

// ============================================================================
// LOCATION HELPER v1.0 -- logika GPS terpusat dengan multi-sampling & validasi.
//
// Masalah yang diperbaiki:
// 1. Posisi TL pakai BALANCED_POWER_ACCURACY (akurasi ~100m) -- tidak presisi.
//    Sekarang semua pakai HIGH_ACCURACY.
// 2. Tidak ada validasi akurasi -- sekarang ambil best dari 3 sampel.
// 3. Tidak ada deteksi mode GPS HP (Battery Saving, Approximate, dsb).
// 4. Tidak ada adaptive timeout by battery.
// 5. Tidak ada fallback status (SUCCESS/PARTIAL/POOR/TIMEOUT).
// ============================================================================
object LocationHelper {

    const val DEFAULT_TARGET_ACCURACY_M = 50f
    const val DEFAULT_TIMEOUT_MS = 30_000L
    const val DEFAULT_MAX_SAMPLES = 3

    private const val BATTERY_HIGH = 50
    private const val BATTERY_LOW = 15

    // ---------------------------------------------------------------------
    // Result object
    // ---------------------------------------------------------------------
    data class LocationFix(
        val location: Location?,
        val samples: List<Location>,
        val bestAccuracy: Float?,
        val durationMs: Long,
        val status: FixStatus,
        val batteryLevel: Int,
        val hasPrecisePermission: Boolean
    )

    enum class FixStatus {
        SUCCESS,           // akurasi <= target (mis. <= 50m)
        PARTIAL,           // 50m < akurasi <= 500m
        POOR,              // akurasi > 500m
        PERMISSION_DENIED,
        APPROXIMATE_ONLY,  // izin cuma coarse (Android 12+)
        GPS_DISABLED,
        TIMEOUT,
        UNAVAILABLE
    }

    // ---------------------------------------------------------------------
    // MAIN ENTRY: getBestLocation
    // ---------------------------------------------------------------------
    suspend fun getBestLocation(
        context: Context,
        targetAccuracyM: Float = DEFAULT_TARGET_ACCURACY_M,
        timeoutMs: Long = DEFAULT_TIMEOUT_MS,
        maxSamples: Int = DEFAULT_MAX_SAMPLES,
        adaptiveByBattery: Boolean = true,
        onProgress: ((Int, Location?) -> Unit)? = null
    ): LocationFix {

        val startTime = System.currentTimeMillis()
        val batteryLevel = getBatteryLevel(context)
        val hasPrecise = hasPrecisePermission(context)

        // 1. Cek izin lokasi
        if (!hasLocationPermission(context)) {
            return LocationFix(
                null, emptyList(), null,
                System.currentTimeMillis() - startTime,
                FixStatus.PERMISSION_DENIED, batteryLevel, hasPrecise
            )
        }

        // 2. Cek GPS provider aktif
        if (!isGpsEnabled(context)) {
            return LocationFix(
                null, emptyList(), null,
                System.currentTimeMillis() - startTime,
                FixStatus.GPS_DISABLED, batteryLevel, hasPrecise
            )
        }

        // 3. Adaptive timeout + samples by battery
        var effectiveTimeout = timeoutMs
        var effectiveMaxSamples = maxSamples
        if (adaptiveByBattery) {
            when {
                batteryLevel > BATTERY_HIGH -> { /* Full power — pakai default */ }
                batteryLevel > BATTERY_LOW -> {
                    effectiveTimeout = (timeoutMs * 7 / 10)
                    effectiveMaxSamples = minOf(maxSamples, 2)
                }
                else -> {
                    effectiveTimeout = (timeoutMs / 2)
                    effectiveMaxSamples = 1
                }
            }
        }

        // 4. Kumpulkan samples
        val samples = collectSamples(
            context = context,
            maxSamples = effectiveMaxSamples,
            targetAccuracyM = targetAccuracyM,
            timeoutMs = effectiveTimeout,
            onProgress = onProgress
        )

        // 5. Pilih fix terbaik (akurasi terkecil)
        val best = samples.minByOrNull { it.accuracy }

        // 6. Tentukan status
        val status = when {
            best == null -> FixStatus.TIMEOUT
            !hasPrecise && best.accuracy > 1000f -> FixStatus.APPROXIMATE_ONLY
            best.accuracy <= targetAccuracyM -> FixStatus.SUCCESS
            best.accuracy <= 500f -> FixStatus.PARTIAL
            else -> FixStatus.POOR
        }

        return LocationFix(
            location = best,
            samples = samples,
            bestAccuracy = best?.accuracy,
            durationMs = System.currentTimeMillis() - startTime,
            status = status,
            batteryLevel = batteryLevel,
            hasPrecisePermission = hasPrecise
        )
    }

    // ---------------------------------------------------------------------
    // Multi-sample collector via requestLocationUpdates
    // ---------------------------------------------------------------------
    @android.annotation.SuppressLint("MissingPermission")
    private suspend fun collectSamples(
        context: Context,
        maxSamples: Int,
        targetAccuracyM: Float,
        timeoutMs: Long,
        onProgress: ((Int, Location?) -> Unit)?
    ): List<Location> = withTimeoutOrNull(timeoutMs) {
        val samples = mutableListOf<Location>()
        val client = LocationServices.getFusedLocationProviderClient(context)

        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1000L)
            .setMaxUpdates(maxSamples)
            .setWaitForAccurateLocation(false)
            .build()

        suspendCancellableCoroutine<List<Location>> { cont ->
            val callback = object : LocationCallback() {
                override fun onLocationResult(result: LocationResult) {
                    if (!cont.isActive) return
                    for (loc in result.locations) {
                        samples.add(loc)
                        val best = samples.minByOrNull { it.accuracy }
                        onProgress?.invoke(samples.size, best)

                        if (loc.accuracy <= targetAccuracyM || samples.size >= maxSamples) {
                            client.removeLocationUpdates(this)
                            if (cont.isActive) cont.resume(samples.toList())
                            return
                        }
                    }
                }
            }

            try {
                client.requestLocationUpdates(request, callback, Looper.getMainLooper())
                    .addOnFailureListener {
                        if (cont.isActive) {
                            try { client.removeLocationUpdates(callback) } catch (_: Exception) {}
                            cont.resume(samples.toList())
                        }
                    }
            } catch (e: SecurityException) {
                if (cont.isActive) cont.resume(samples.toList())
            }

            cont.invokeOnCancellation {
                try { client.removeLocationUpdates(callback) } catch (_: Exception) {}
            }
        }
    } ?: emptyList()

    // ---------------------------------------------------------------------
    // Permission & environment checks
    // ---------------------------------------------------------------------
    private fun hasLocationPermission(context: Context): Boolean {
        val fine = context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val coarse = context.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        return fine || coarse
    }

    fun hasPrecisePermission(context: Context): Boolean {
        return context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
    }

    private fun isGpsEnabled(context: Context): Boolean {
        val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return false
        return lm.isProviderEnabled(LocationManager.GPS_PROVIDER)
    }

    fun getBatteryLevel(context: Context): Int {
        return try {
            val bm = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
            bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
        } catch (e: Exception) { -1 }
    }

    // ---------------------------------------------------------------------
    // UI helpers -- label & warna untuk badge akurasi
    // ---------------------------------------------------------------------
    fun accuracyLabel(accuracyM: Float?): String = when {
        accuracyM == null -> "?"
        accuracyM <= 50f -> "Bagus"
        accuracyM <= 500f -> "Sedang"
        else -> "Buruk"
    }

    fun accuracyColorHex(accuracyM: Float?): Long = when {
        accuracyM == null -> 0xFF9CA3AF
        accuracyM <= 50f -> 0xFF0F7A5A
        accuracyM <= 500f -> 0xFFF59E0B
        else -> 0xFFDC2626
    }

    fun statusMessage(status: FixStatus): String = when (status) {
        FixStatus.SUCCESS -> "Lokasi presisi ditemukan"
        FixStatus.PARTIAL -> "Lokasi ditemukan (akurasi sedang)"
        FixStatus.POOR -> "Lokasi ditemukan (akurasi rendah)"
        FixStatus.PERMISSION_DENIED -> "Izin lokasi ditolak"
        FixStatus.APPROXIMATE_ONLY -> "Izin lokasi hanya 'Perkiraan' -- aktifkan 'Presisi' di pengaturan HP"
        FixStatus.GPS_DISABLED -> "GPS HP tidak aktif -- nyalakan GPS"
        FixStatus.TIMEOUT -> "Sinyal GPS tidak ditemukan (di dalam gedung?)"
        FixStatus.UNAVAILABLE -> "Lokasi tidak tersedia"
    }
}
