package com.imtiyaztour.app

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
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
// LOKASI — sebelumnya pakai LocationManager mentah Android supaya tidak menambah
// dependency baru; TERBUKTI TIDAK CUKUP ANDAL (dua kali dilaporkan tidak update)
// -- raw LocationManager memang dikenal lambat/tidak konsisten mendapat fix baru,
// terutama di dalam gedung. Sekarang pakai FusedLocationProviderClient (Google
// Play Services) yang menggabungkan GPS+WiFi+seluler, jauh lebih cepat & andal,
// dan merupakan dependency resmi Google yang sangat umum dipakai (risiko build
// gagal jauh lebih kecil daripada risiko "tidak pernah dapat lokasi" sebelumnya).
// ============================================================================
@Suppress("MissingPermission")
private suspend fun getCurrentLocation(context: Context): Location? = withTimeoutOrNull(15000) {
    suspendCancellableCoroutine { cont ->
        val hasFine = context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val hasCoarse = context.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (!hasFine && !hasCoarse) { cont.resume(null); return@suspendCancellableCoroutine }

        val client = com.google.android.gms.location.LocationServices.getFusedLocationProviderClient(context)
        val cts = com.google.android.gms.tasks.CancellationTokenSource()

        // getCurrentLocation() secara aktif minta fix BARU (bukan cache lama seperti
        // getLastLocation()), dengan prioritas akurasi tinggi kalau izin FINE ada.
        val priority = if (hasFine) com.google.android.gms.location.Priority.PRIORITY_HIGH_ACCURACY
                       else com.google.android.gms.location.Priority.PRIORITY_BALANCED_POWER_ACCURACY
        client.getCurrentLocation(priority, cts.token)
            .addOnSuccessListener { location -> if (cont.isActive) cont.resume(location) }
            .addOnFailureListener { if (cont.isActive) cont.resume(null) }

        cont.invokeOnCancellation { cts.cancel() }
    }
}

private val PRAYER_LABELS = listOf("Fajar" to "fajr", "Terbit" to "sunrise", "Dzuhur" to "dhuhr", "Ashar" to "asr", "Maghrib" to "maghrib", "Isya" to "isha")

private fun currentPrayerName(now: Double, t: PrayerTimesResult): String {
    val points = listOf("Isya" to t.isha, "Maghrib" to t.maghrib, "Ashar" to t.asr, "Dzuhur" to t.dhuhr, "Fajar" to t.fajr)
    for ((name, time) in points) { if (now >= time) return name }
    return "Isya" // sebelum Fajar = masih waktu Isya malam sebelumnya
}

// Kartu ringkas untuk halaman Beranda (dipindah dari tab terpisah "Shalat" sebelumnya,
// sesuai permintaan supaya lebih menyatu dengan halaman utama). Izin lokasi TIDAK
// diminta dari sini lagi -- sudah diminta di awal saat app pertama dibuka (lihat
// ImtiyazApp -> LaunchedEffect), kartu ini cuma memakai hasilnya.
@Composable
fun PrayerTimesCard() {
    val context = LocalContext.current
    var hasPermission by remember {
        mutableStateOf(
            context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            context.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        )
    }
    var locationLabel by remember { mutableStateOf("") }
    var times by remember { mutableStateOf<PrayerTimesResult?>(null) }
    var errorMsg by remember { mutableStateOf("") }

    // Kalau user baru saja mengizinkan lokasi lewat dialog sistem (diminta di awal app),
    // recheck saat kartu ini pertama kali muncul supaya langsung dapat lokasi GPS asli.
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
        hasPermission = result.values.any { it }
    }

    suspend fun loadTimes() {
        errorMsg = ""
        val loc = if (hasPermission) getCurrentLocation(context) else null
        val lat: Double; val lng: Double
        if (loc != null) {
            lat = loc.latitude; lng = loc.longitude
            locationLabel = "Berdasarkan lokasi Anda saat ini"
        } else {
            lat = 21.4225; lng = 39.8262 // fallback: Masjidil Haram, Mekkah
            locationLabel = if (hasPermission) "Lokasi tidak terdeteksi -- pakai Mekkah" else "Aktifkan lokasi untuk jadwal sesuai posisi Anda"
        }
        val tzHours = TimeZone.getDefault().rawOffset / 3600000.0
        val cal = Calendar.getInstance()
        val result = computePrayerTimes(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1, cal.get(Calendar.DAY_OF_MONTH), lat, lng, tzHours)
        if (result == null) errorMsg = "Gagal menghitung jadwal untuk lokasi ini" else times = result
    }

    LaunchedEffect(hasPermission) {
        // FIX: sebelumnya hanya dihitung SEKALI saat kartu pertama muncul (atau saat
        // status izin berubah), tidak pernah diperbarui lagi -- kalau jamaah berpindah
        // kota (mis. dari Indonesia ke Madinah lalu Makkah), jadwal tidak ikut berubah
        // sampai app ditutup-buka ulang. Sekarang diperbarui otomatis setiap 5 menit.
        while (isActive) {
            loadTimes()
            delay(5 * 60 * 1000L)
        }
    }

    // FIX: sebelumnya dibungkus remember{} tanpa key -- nilainya "membeku" di waktu
    // pertama kartu ini muncul dan TIDAK PERNAH diperbarui, jadi sorotan "shalat yang
    // sedang berlangsung" bisa salah kalau kartu sudah lama terbuka. Perhitungannya
    // sangat murah, jadi dihitung ulang setiap kali times diperbarui (tanpa remember).
    val nowHour = run {
        val c = Calendar.getInstance(); c.get(Calendar.HOUR_OF_DAY) + c.get(Calendar.MINUTE) / 60.0
    }

    Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(2.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Schedule, contentDescription = null, tint = Color(0xFF0F7A5A), modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Jadwal Shalat Hari Ini", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
                if (!hasPermission) {
                    TextButton(onClick = { permissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)) }) {
                        Text("Aktifkan Lokasi", fontSize = 10.sp)
                    }
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 2.dp)) {
                Icon(Icons.Default.LocationOn, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(11.dp))
                Spacer(Modifier.width(3.dp))
                Text(locationLabel, fontSize = 10.sp, color = Color.Gray)
            }
            if (errorMsg.isNotEmpty()) { Text(errorMsg, fontSize = 10.sp, color = Color(0xFFDC2626)) }
            Spacer(Modifier.height(10.dp))

            times?.let { t ->
                val current = currentPrayerName(nowHour, t)
                val rows = listOf(
                    "Fajar" to t.fajr, "Terbit" to t.sunrise, "Dzuhur" to t.dhuhr,
                    "Ashar" to t.asr, "Maghrib" to t.maghrib, "Isya" to t.isha
                )
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    rows.forEach { (label, time) ->
                        val active = label == current
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .weight(1f)
                                .background(if (active) Color(0xFF0F7A5A) else Color.Transparent, RoundedCornerShape(8.dp))
                                .padding(vertical = 6.dp, horizontal = 2.dp)
                        ) {
                            Text(label, fontSize = 9.sp, color = if (active) Color.White else Color.Gray)
                            Spacer(Modifier.height(2.dp))
                            Text(formatJamShalat(time), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (active) Color.White else Color(0xFF0F7A5A))
                        }
                    }
                }
            } ?: run {
                LinearProgressIndicator(Modifier.fillMaxWidth())
            }
            Spacer(Modifier.height(6.dp))
            Text("Estimasi Ummul Qura -- rujuk adzan Masjidil Haram/Nabawi setempat", fontSize = 8.sp, color = Color.LightGray)
        }
    }
}
