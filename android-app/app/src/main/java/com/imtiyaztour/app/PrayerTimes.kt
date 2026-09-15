package com.imtiyaztour.app

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Looper
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.Calendar
import java.util.TimeZone
import kotlin.coroutines.resume
import kotlin.math.*

// ============================================================================
// PERHITUNGAN JADWAL SHALAT — metode Ummul Qura (Fajr 18.5°, Isya = Maghrib + 90
// menit di luar Ramadan). Dihitung LOKAL di perangkat dari posisi GPS + tanggal &
// zona waktu perangkat saat ini -- TIDAK butuh internet, penting karena jamaah
// sering tidak punya koneksi data di Masjidil Haram/Nabawi atau saat penerbangan.
//
// Referensi algoritma: posisi matahari & sudut jam berbasis rumus astronomi publik
// yang dipakai luas oleh aplikasi jadwal shalat (mis. praytimes.org). Hasil sudah
// disanity-check terhadap pola waktu shalat Mekkah/Madinah/Jakarta yang dikenal umum.
//
// CATATAN: ini estimasi astronomis, bukan jadwal resmi. Selisih 1-3 menit dengan
// pengumuman adzan setempat adalah wajar -- tetap rujuk adzan Masjidil Haram/Nabawi
// atau masjid setempat untuk kepastian.
//
// Zona waktu diambil dari zona waktu perangkat (TimeZone.getDefault()), dengan
// asumsi HP jamaah otomatis menyesuaikan zona waktu operator saat roaming di Arab
// Saudi. Kalau perangkat diset manual/pesawat mode tanpa auto time zone, jadwal
// bisa meleset -- ingatkan jamaah untuk mengaktifkan "tanggal & waktu otomatis".
// ============================================================================

data class PrayerTimesResult(
    val fajr: Double, val sunrise: Double, val dhuhr: Double,
    val asr: Double, val maghrib: Double, val isha: Double
)

private fun dsin(d: Double) = sin(Math.toRadians(d))
private fun dcos(d: Double) = cos(Math.toRadians(d))
private fun dtan(d: Double) = tan(Math.toRadians(d))
private fun darcsin(x: Double) = Math.toDegrees(asin(x))
private fun darccos(x: Double) = Math.toDegrees(acos(x.coerceIn(-1.0, 1.0)))
private fun darctan2(y: Double, x: Double) = Math.toDegrees(atan2(y, x))
private fun darccot(x: Double) = Math.toDegrees(atan(1.0 / x))
private fun fixAngle(a: Double): Double { var r = a % 360.0; if (r < 0) r += 360.0; return r }
private fun fixHour(h: Double): Double { var r = h % 24.0; if (r < 0) r += 24.0; return r }

private fun julianDate(year: Int, month: Int, day: Int): Double {
    var y = year; var m = month
    if (m <= 2) { y -= 1; m += 12 }
    val a = floor(y / 100.0)
    val b = 2 - a + floor(a / 4.0)
    return floor(365.25 * (y + 4716)) + floor(30.6001 * (m + 1)) + day + b - 1524.5
}

private data class SunPos(val declination: Double, val equation: Double)

private fun sunPosition(jd: Double): SunPos {
    val d = jd - 2451545.0
    val g = fixAngle(357.529 + 0.98560028 * d)
    val q = fixAngle(280.459 + 0.98564736 * d)
    val l = fixAngle(q + 1.915 * dsin(g) + 0.020 * dsin(2 * g))
    val e = 23.439 - 0.00000036 * d
    val ra = darctan2(dcos(e) * dsin(l), dcos(l)) / 15.0
    val eqt = q / 15.0 - fixHour(ra)
    val decl = darcsin(dsin(e) * dsin(l))
    return SunPos(decl, eqt)
}

/** angle: derajat di bawah ufuk (positif = di bawah ufuk; negatif dipakai untuk Ashar yang di atas ufuk) */
private fun hourAngle(angle: Double, lat: Double, decl: Double): Double? {
    val cosH = (-dsin(angle) - dsin(decl) * dsin(lat)) / (dcos(decl) * dcos(lat))
    if (cosH < -1.0 || cosH > 1.0) return null // matahari tidak mencapai sudut ini (lintang ekstrem/kutub)
    return darccos(cosH) / 15.0
}

/**
 * @param asrFactor 1.0 = mazhab Syafi'i/Maliki/Hanbali (default), 2.0 = Hanafi
 * @return null kalau lokasi berada di lintang ekstrem di mana sudut matahari tidak tercapai
 */
fun computePrayerTimes(
    year: Int, month: Int, day: Int,
    latitude: Double, longitude: Double, timezoneOffsetHours: Double,
    asrFactor: Double = 1.0
): PrayerTimesResult? {
    val jd = julianDate(year, month, day) - longitude / (15.0 * 24.0)
    val sun = sunPosition(jd + 0.5)

    val dhuhr = fixHour(fixHour(12.0 - sun.equation) + (timezoneOffsetHours - longitude / 15.0))

    val fajrH = hourAngle(18.5, latitude, sun.declination) ?: return null
    val sunriseH = hourAngle(0.833, latitude, sun.declination) ?: return null
    val asrAltitude = darccot(asrFactor + abs(dtan(latitude - sun.declination)))
    val asrH = hourAngle(-asrAltitude, latitude, sun.declination) ?: return null

    return PrayerTimesResult(
        fajr = fixHour(dhuhr - fajrH),
        sunrise = fixHour(dhuhr - sunriseH),
        dhuhr = dhuhr,
        asr = fixHour(dhuhr + asrH),
        maghrib = fixHour(dhuhr + sunriseH),
        isha = fixHour(fixHour(dhuhr + sunriseH) + 90.0 / 60.0)
    )
}

fun formatJamShalat(h: Double): String {
    val total = ((h % 24) + 24) % 24
    val hh = floor(total).toInt()
    val mm = floor((total - hh) * 60).toInt()
    return String.format("%02d:%02d", hh, mm)
}

// ============================================================================
// LOKASI — pakai LocationManager bawaan Android (bukan Google Play Services)
// supaya tidak menambah dependency baru yang bisa memicu masalah build lagi.
// ============================================================================
@Suppress("MissingPermission")
private suspend fun getCurrentLocation(context: Context): Location? = suspendCancellableCoroutine { cont ->
    val lm = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    val hasFine = context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
    val hasCoarse = context.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
    if (!hasFine && !hasCoarse) { cont.resume(null); return@suspendCancellableCoroutine }

    val providers = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER).filter {
        try { lm.isProviderEnabled(it) } catch (e: Exception) { false }
    }
    // Coba lastKnownLocation dulu (instan) -- kalau kosong, minta 1 update baru.
    val last = providers.mapNotNull { try { lm.getLastKnownLocation(it) } catch (e: Exception) { null } }
        .maxByOrNull { it.time }
    if (last != null) { cont.resume(last); return@suspendCancellableCoroutine }

    if (providers.isEmpty()) { cont.resume(null); return@suspendCancellableCoroutine }
    val listener = object : LocationListener {
        override fun onLocationChanged(location: Location) {
            try { lm.removeUpdates(this) } catch (e: Exception) {}
            if (cont.isActive) cont.resume(location)
        }
        override fun onProviderDisabled(provider: String) {}
        override fun onProviderEnabled(provider: String) {}
        @Deprecated("Deprecated in Java")
        override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
    }
    try {
        lm.requestLocationUpdates(providers.first(), 0L, 0f, listener, Looper.getMainLooper())
    } catch (e: Exception) { cont.resume(null); return@suspendCancellableCoroutine }
    cont.invokeOnCancellation { try { lm.removeUpdates(listener) } catch (e: Exception) {} }
}

private val PRAYER_LABELS = listOf("Fajar" to "fajr", "Terbit" to "sunrise", "Dzuhur" to "dhuhr", "Ashar" to "asr", "Maghrib" to "maghrib", "Isya" to "isha")

private fun currentPrayerName(now: Double, t: PrayerTimesResult): String {
    val points = listOf("Isya" to t.isha, "Maghrib" to t.maghrib, "Ashar" to t.asr, "Dzuhur" to t.dhuhr, "Fajar" to t.fajr)
    for ((name, time) in points) { if (now >= time) return name }
    return "Isya" // sebelum Fajar = masih waktu Isya malam sebelumnya
}

@Composable
fun JadwalShalatScreen() {
    val context = LocalContext.current
    var hasPermission by remember {
        mutableStateOf(
            context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            context.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        )
    }
    var locationLabel by remember { mutableStateOf("") }
    var times by remember { mutableStateOf<PrayerTimesResult?>(null) }
    var loading by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf("") }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
        hasPermission = result.values.any { it }
        if (!hasPermission) errorMsg = "Izin lokasi ditolak -- jadwal memakai lokasi Mekkah sebagai default"
    }

    suspend fun loadTimes() {
        loading = true; errorMsg = ""
        val loc = if (hasPermission) getCurrentLocation(context) else null
        val lat: Double; val lng: Double
        if (loc != null) {
            lat = loc.latitude; lng = loc.longitude
            locationLabel = "Lokasi GPS saat ini (%.4f, %.4f)".format(lat, lng)
        } else {
            lat = 21.4225; lng = 39.8262 // fallback: Masjidil Haram, Mekkah
            locationLabel = if (hasPermission) "Lokasi tidak terdeteksi -- pakai default Mekkah" else "Pakai default Mekkah (izin lokasi belum diberikan)"
        }
        val tzHours = TimeZone.getDefault().rawOffset / 3600000.0
        val cal = Calendar.getInstance()
        val result = computePrayerTimes(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1, cal.get(Calendar.DAY_OF_MONTH), lat, lng, tzHours)
        if (result == null) errorMsg = "Gagal menghitung jadwal untuk lokasi ini" else times = result
        loading = false
    }

    LaunchedEffect(hasPermission) { loadTimes() }

    val nowHour = remember {
        val c = Calendar.getInstance(); c.get(Calendar.HOUR_OF_DAY) + c.get(Calendar.MINUTE) / 60.0
    }

    LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Text("Jadwal Waktu Shalat", fontWeight = FontWeight.Bold, fontSize = 20.sp)
            Text("Metode Ummul Qura -- otomatis menyesuaikan lokasi", fontSize = 12.sp, color = Color.Gray)
            Spacer(Modifier.height(8.dp))

            if (!hasPermission) {
                Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3CD)), shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(14.dp)) {
                        Text("Aktifkan lokasi untuk jadwal yang sesuai posisi Anda", fontSize = 12.sp)
                        Spacer(Modifier.height(8.dp))
                        Button(onClick = { permissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)) }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F7A5A))) {
                            Text("Izinkan Akses Lokasi")
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.LocationOn, contentDescription = null, tint = Color(0xFF0F7A5A), modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(4.dp))
                Text(locationLabel, fontSize = 10.sp, color = Color.Gray)
            }
            if (errorMsg.isNotEmpty()) { Text(errorMsg, fontSize = 10.sp, color = Color(0xFFDC2626)) }
            Spacer(Modifier.height(12.dp))
        }

        if (loading) {
            item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
        }

        times?.let { t ->
            val current = currentPrayerName(nowHour, t)
            val rows = listOf(
                "Fajar" to t.fajr, "Terbit" to t.sunrise, "Dzuhur" to t.dhuhr,
                "Ashar" to t.asr, "Maghrib" to t.maghrib, "Isya" to t.isha
            )
            items(rows) { (label, time) ->
                val active = label == current
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = if (active) Color(0xFF0F7A5A) else Color.White),
                    elevation = CardDefaults.cardElevation(if (active) 4.dp else 1.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text(label, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = if (active) Color.White else Color.Black)
                        Text(formatJamShalat(time), fontWeight = FontWeight.Bold, fontSize = 18.sp, color = if (active) Color.White else Color(0xFF0F7A5A))
                    }
                }
            }
            item {
                Spacer(Modifier.height(8.dp))
                Text(
                    "Estimasi astronomis (Fajar 18,5° -- Isya = Maghrib + 90 menit). Selisih beberapa menit dengan adzan Masjidil Haram/Nabawi setempat adalah wajar; jadikan pengumuman adzan setempat sebagai rujukan utama.",
                    fontSize = 10.sp, color = Color.Gray
                )
            }
        }
    }
}
