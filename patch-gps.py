#!/usr/bin/env python3
"""
Patch GPS accuracy:
- Buat LocationHelper.kt (new)
- Patch LocateService.kt (pakai helper)
- Patch FindJamaah.kt (pakai helper untuk TL + badge akurasi)
"""
import sys, os, shutil

JAVA = "android-app/app/src/main/java/com/imtiyaztour/app"
if not os.path.isdir(JAVA):
    print("ERROR: jalankan dari root repo imtiyaz-android-v1.5.meta"); sys.exit(1)

# ============================================================
# 1. BUAT LocationHelper.kt
# ============================================================
print("[1/3] Membuat LocationHelper.kt...")
LOCATION_HELPER = '''package com.imtiyaztour.app

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
'''
with open(f"{JAVA}/LocationHelper.kt", "w", encoding="utf-8", newline="\n") as f:
    f.write(LOCATION_HELPER)
print("  OK: LocationHelper.kt dibuat")

# ============================================================
# 2. PATCH LocateService.kt
# ============================================================
print("[2/3] Patch LocateService.kt...")
LOCATE_PATH = f"{JAVA}/LocateService.kt"
if not os.path.isfile(LOCATE_PATH):
    print(f"  ERROR: {LOCATE_PATH} tidak ada"); sys.exit(1)

shutil.copy(LOCATE_PATH, LOCATE_PATH + ".bak-gps")

with open(LOCATE_PATH, "r", encoding="utf-8", newline="") as f:
    c = f.read().replace("\r\n", "\n").replace("\r", "\n")

# Pattern lama
OLD_RESPOND = '''    @Suppress("MissingPermission")
    private suspend fun respondWithLocation(requestId: String) {
        val loc = try {
            withTimeoutOrNull(15_000) {
                suspendCancellableCoroutine<android.location.Location?> { cont ->
                    val c = LocationServices.getFusedLocationProviderClient(this@LocateService)
                    val cts = com.google.android.gms.tasks.CancellationTokenSource()
                    c.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cts.token)
                        .addOnSuccessListener { if (cont.isActive) cont.resume(it) }
                        .addOnFailureListener { if (cont.isActive) cont.resume(null) }
                    cont.invokeOnCancellation { cts.cancel() }
                }
            }
        } catch (e: Exception) { null }

        if (loc == null) return

        try {
            TrackApiClient.service.sendTrackPing(
                TrackPingRequest(
                    jamaah_id = Prefs.getJamaahId(this),
                    token = Prefs.getToken(this),
                    request_id = requestId,
                    latitude = loc.latitude,
                    longitude = loc.longitude,
                    accuracy = loc.accuracy,
                    battery = getBattery()
                )
            )
        } catch (e: Exception) { }
    }'''

NEW_RESPOND = '''    private suspend fun respondWithLocation(requestId: String) {
        // Pakai LocationHelper: multi-sample (3x), HIGH_ACCURACY,
        // target 50m, timeout 30s, adaptive by battery.
        val fix = LocationHelper.getBestLocation(
            context = this,
            targetAccuracyM = LocationHelper.DEFAULT_TARGET_ACCURACY_M,
            timeoutMs = LocationHelper.DEFAULT_TIMEOUT_MS,
            maxSamples = LocationHelper.DEFAULT_MAX_SAMPLES,
            adaptiveByBattery = true
        )

        // Hanya kirim kalau ada fix. Kalau tidak ada / sangat jelek,
        // TL akan timeout & pakai last-known location dari server.
        val loc = fix.location ?: return
        if (fix.status == LocationHelper.FixStatus.TIMEOUT) return
        if (fix.status == LocationHelper.FixStatus.PERMISSION_DENIED) return
        if (fix.status == LocationHelper.FixStatus.GPS_DISABLED) return

        try {
            TrackApiClient.service.sendTrackPing(
                TrackPingRequest(
                    jamaah_id = Prefs.getJamaahId(this),
                    token = Prefs.getToken(this),
                    request_id = requestId,
                    latitude = loc.latitude,
                    longitude = loc.longitude,
                    accuracy = loc.accuracy,
                    battery = fix.batteryLevel
                )
            )
        } catch (e: Exception) { }
    }'''

if OLD_RESPOND not in c:
    print("  ERROR: pattern respondWithLocation tidak ditemukan")
    print("  Cek dulu: apakah kode sudah dimodifikasi manual?")
    sys.exit(2)

c = c.replace(OLD_RESPOND, NEW_RESPOND, 1)

with open(LOCATE_PATH, "w", encoding="utf-8", newline="\n") as f:
    f.write(c)
print("  OK: respondWithLocation() di-refactor")

# ============================================================
# 3. PATCH FindJamaah.kt
# ============================================================
print("[3/3] Patch FindJamaah.kt...")
FIND_PATH = f"{JAVA}/FindJamaah.kt"
if not os.path.isfile(FIND_PATH):
    print(f"  ERROR: {FIND_PATH} tidak ada"); sys.exit(1)

shutil.copy(FIND_PATH, FIND_PATH + ".bak-gps")

with open(FIND_PATH, "r", encoding="utf-8", newline="") as f:
    c = f.read().replace("\r\n", "\n").replace("\r", "\n")

# 3a. Replace getTLLocation function
OLD_TL = '''@Suppress("MissingPermission")
private suspend fun getTLLocation(context: android.content.Context): Location? = withTimeoutOrNull(10_000) {
    suspendCancellableCoroutine { cont ->
        val c = LocationServices.getFusedLocationProviderClient(context)
        val cts = com.google.android.gms.tasks.CancellationTokenSource()
        c.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, cts.token)
            .addOnSuccessListener { if (cont.isActive) cont.resume(it) }
            .addOnFailureListener { if (cont.isActive) cont.resume(null) }
        cont.invokeOnCancellation { cts.cancel() }
    }
}'''

NEW_TL = '''// Posisi TL sendiri -- pakai LocationHelper (HIGH_ACCURACY, multi-sample).
// Sebelumnya BALANCED_POWER_ACCURACY (~100m), sekarang setara dengan posisi jamaah.
@Suppress("MissingPermission")
private suspend fun getTLLocation(context: android.content.Context): Location? {
    val fix = LocationHelper.getBestLocation(
        context = context,
        targetAccuracyM = LocationHelper.DEFAULT_TARGET_ACCURACY_M,
        timeoutMs = LocationHelper.DEFAULT_TIMEOUT_MS,
        maxSamples = LocationHelper.DEFAULT_MAX_SAMPLES,
        adaptiveByBattery = true
    )
    return fix.location
}'''

if OLD_TL not in c:
    print("  ERROR: pattern getTLLocation tidak ditemukan")
    sys.exit(2)
c = c.replace(OLD_TL, NEW_TL, 1)

# 3b. Tambah badge akurasi + warning di kartu hasil
OLD_ACCURACY = '''                        Text("Koordinat: ${"%.5f".format(lat)}, ${"%.5f".format(lon)}", fontSize = 12.sp)
                        Text("Akurasi: \u00b1${r.accuracy?.toInt() ?: 0} meter", fontSize = 11.sp, color = Color.Gray)
                        Text("Baterai: ${r.battery ?: 0}%", fontSize = 11.sp, color = Color.Gray)'''

NEW_ACCURACY = '''                        Text("Koordinat: ${"%.5f".format(lat)}, ${"%.5f".format(lon)}", fontSize = 12.sp)

                        // Badge akurasi berwarna (v1.1)
                        val accVal = r.accuracy?.toFloat()
                        val accColor = Color(LocationHelper.accuracyColorHex(accVal))
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 4.dp)) {
                            Box(
                                modifier = Modifier
                                    .background(accColor.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    "\\u00b1${accVal?.toInt() ?: 0}m  ${LocationHelper.accuracyLabel(accVal)}",
                                    fontSize = 12.sp,
                                    color = accColor,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(Modifier.width(8.dp))
                            Text("Baterai: ${r.battery ?: 0}%", fontSize = 11.sp, color = Color.Gray)
                        }

                        // Warning kalau akurasi > 100m
                        if (accVal != null && accVal > 100f) {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3CD)),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                            ) {
                                Text(
                                    "\\u26a0 Akurasi rendah. Jamaah mungkin di dalam gedung. Titik di peta bisa meleset " +
                                        "\\u00b1${accVal.toInt()} meter dari lokasi sebenarnya.",
                                    fontSize = 10.sp,
                                    color = Color(0xFF92400E),
                                    modifier = Modifier.padding(10.dp)
                                )
                            }
                        }'''

if OLD_ACCURACY not in c:
    print("  ERROR: pattern tampilan akurasi tidak ditemukan")
    sys.exit(2)
c = c.replace(OLD_ACCURACY, NEW_ACCURACY, 1)

# 3c. Tambah import yang perlu
if "import androidx.compose.ui.text.font.FontWeight" not in c:
    c = c.replace(
        "import androidx.compose.ui.text.style.TextAlign",
        "import androidx.compose.ui.text.font.FontWeight\nimport androidx.compose.ui.text.style.TextAlign",
        1
    )

with open(FIND_PATH, "w", encoding="utf-8", newline="\n") as f:
    f.write(c)
print("  OK: FindJamaah.kt di-patch (TL + badge akurasi + warning)")

print("\n" + "=" * 60)
print("PATCH GPS ACCURACY SELESAI")
print("=" * 60)
print("\nSelanjutnya:")
print("  1. git add . && git commit -m 'feat: GPS accuracy v1.1'")
print("  2. git push origin main")
print("  3. Tunggu CI, install APK baru")
print("  4. Test akurasi GPS di 3 skenario")