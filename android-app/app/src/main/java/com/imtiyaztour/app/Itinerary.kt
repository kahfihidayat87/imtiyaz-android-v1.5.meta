package com.imtiyaztour.app

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ============================================================================
// ITINERARY UMRAH 9 HARI -- template umum (bukan tanggal keberangkatan spesifik,
// karena tanggal/hari berbeda-beda tiap grup keberangkatan). Dua versi disediakan:
// Ramadhan (dengan sahur/buka puasa/tarawih) dan Reguler/non-Ramadhan (jadwal makan
// & shalat normal, tanpa unsur puasa). Rute kota dan titik ziarah sama di kedua
// versi -- yang berbeda adalah unsur ibadah puasanya saja.
// ============================================================================

data class ItineraryDay(val hari: Int, val rute: String, val kegiatan: String)

val itineraryRamadhan = listOf(
    ItineraryDay(1, "Yogyakarta (YIA) – Jakarta – Jeddah – Madinah",
        "Kumpul di YIA pukul 05:30 → terbang pukul 07:00 menuju Jakarta → lanjut penerbangan pukul 10:30 menuju Jeddah, tiba pukul 16:40 (Dzuhur-Ashar dijama' di pesawat). Perjalanan darat ke Madinah, check-in hotel, berbuka puasa, Maghrib-Isya dijama' ta'khir di Masjid Nabawi."),
    ItineraryDay(2, "Madinah",
        "Sahur pukul 04:00 → Shalat Subuh di Masjid Nabawi → ziarah ke makam Rasulullah ﷺ, Abu Bakar, Umar, dan pemakaman Baqi' → waktu Dhuha → istirahat. Pukul 16:30: ziarah sekitar Nabawi (Tsaqifah Bani Sa'idah, Masjid Ghamamah, Masjid Abu Bakar). Berbuka puasa dan shalat Tarawih."),
    ItineraryDay(3, "Madinah – Ziarah Kota",
        "Sahur → Shalat Subuh → tadarus Al-Qur'an hingga syuruq. Pukul 08:30: ziarah kota Madinah (Masjid Quba, Jabal Uhud, Masjid Qiblatain). Pukul 16:30: penjelasan teknis keberangkatan menuju Makkah."),
    ItineraryDay(4, "Madinah – Makkah",
        "Pukul 07:00 koper disiapkan. Pukul 09:00 berangkat, mengambil miqat di Bir Ali. Tiba di hotel Makkah pukul 15:00, Dzuhur-Ashar dijama' ta'khir lalu langsung melaksanakan ibadah umrah (thawaf, sa'i, tahallul). Berbuka puasa, Maghrib-Isya-Tarawih di Masjidil Haram."),
    ItineraryDay(5, "Makkah",
        "Sahur → Shalat Subuh → tadarus hingga syuruq. Program bebas dan istirahat."),
    ItineraryDay(6, "Makkah – Ziarah",
        "Pukul 09:00 ziarah (Jabal Tsur, Jabal Nur/Gua Hira, Padang Arafah, Jabal Rahmah), singgah di Ju'ranah (lokasi miqat umrah kedua bagi yang berkenan). Pukul 16:30 taushiyah. Berbuka puasa dan shalat Tarawih."),
    ItineraryDay(7, "Makkah",
        "Sahur → Shalat Subuh → menunggu syuruq sambil taushiyah → waktu Dhuha → persiapan Shalat Jumat. Pukul 16:30 taushiyah dan penjelasan teknis kepulangan. Berbuka puasa dan shalat Tarawih."),
    ItineraryDay(8, "Makkah – Jeddah",
        "Sahur, Shalat Subuh, thawaf wada' (perpisahan). Pukul 08:00 koper dikunci. Pukul 10:00 menuju Jeddah melewati Corniche, Dzuhur-Ashar dijama' taqdim di Jeddah. Pukul 18:20 penerbangan Jeddah–Jakarta (berbuka puasa, Maghrib, Isya, dan Subuh dilaksanakan di pesawat)."),
    ItineraryDay(9, "Jakarta – Yogyakarta",
        "Mendarat di Soekarno-Hatta pukul 08:30, proses imigrasi dan bea cukai. Pukul 11:30 lanjut penerbangan menuju YIA. Doa penutup perjalanan, program selesai.")
)

val itineraryReguler = listOf(
    ItineraryDay(1, "Yogyakarta (YIA) – Jakarta – Jeddah – Madinah",
        "Kumpul di YIA pukul 05:30 → terbang pukul 07:00 menuju Jakarta → lanjut penerbangan pukul 10:30 menuju Jeddah, tiba pukul 16:40 (Dzuhur-Ashar dijama' di pesawat). Perjalanan darat ke Madinah, check-in hotel, makan malam, Maghrib-Isya dijama' ta'khir di Masjid Nabawi."),
    ItineraryDay(2, "Madinah",
        "Shalat Subuh di Masjid Nabawi → ziarah ke makam Rasulullah ﷺ, Abu Bakar, Umar, dan pemakaman Baqi' → sarapan → waktu Dhuha → istirahat. Pukul 16:30: ziarah sekitar Nabawi (Tsaqifah Bani Sa'idah, Masjid Ghamamah, Masjid Abu Bakar). Makan malam."),
    ItineraryDay(3, "Madinah – Ziarah Kota",
        "Shalat Subuh → tadarus Al-Qur'an hingga syuruq → sarapan. Pukul 08:30: ziarah kota Madinah (Masjid Quba, Jabal Uhud, Masjid Qiblatain). Pukul 16:30: penjelasan teknis keberangkatan menuju Makkah."),
    ItineraryDay(4, "Madinah – Makkah",
        "Pukul 07:00 koper disiapkan. Pukul 09:00 berangkat, mengambil miqat di Bir Ali. Tiba di hotel Makkah pukul 15:00, Dzuhur-Ashar dijama' ta'khir lalu langsung melaksanakan ibadah umrah (thawaf, sa'i, tahallul). Makan malam, Maghrib-Isya di Masjidil Haram."),
    ItineraryDay(5, "Makkah",
        "Shalat Subuh → sarapan → program bebas dan istirahat."),
    ItineraryDay(6, "Makkah – Ziarah",
        "Pukul 09:00 ziarah (Jabal Tsur, Jabal Nur/Gua Hira, Padang Arafah, Jabal Rahmah), singgah di Ju'ranah (lokasi miqat umrah kedua bagi yang berkenan). Pukul 16:30 taushiyah. Makan malam."),
    ItineraryDay(7, "Makkah",
        "Shalat Subuh → sarapan → waktu Dhuha → persiapan Shalat Jumat (bila bertepatan hari Jumat). Pukul 16:30 taushiyah dan penjelasan teknis kepulangan. Makan malam."),
    ItineraryDay(8, "Makkah – Jeddah",
        "Shalat Subuh, thawaf wada' (perpisahan). Pukul 08:00 koper dikunci. Pukul 10:00 menuju Jeddah melewati Corniche, Dzuhur-Ashar dijama' taqdim di Jeddah. Pukul 18:20 penerbangan Jeddah–Jakarta (makan malam, Maghrib, Isya, dan Subuh dilaksanakan di pesawat)."),
    ItineraryDay(9, "Jakarta – Yogyakarta",
        "Mendarat di Soekarno-Hatta pukul 08:30, proses imigrasi dan bea cukai. Pukul 11:30 lanjut penerbangan menuju YIA. Doa penutup perjalanan, program selesai.")
)

@Composable
fun ItineraryScreen(onBack: () -> Unit) {
    var isRamadhan by remember { mutableStateOf(true) }
    val days = if (isRamadhan) itineraryRamadhan else itineraryReguler

    LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            Button(onClick = onBack, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F7A5A)), shape = RoundedCornerShape(8.dp)) { Text("← Kembali") }
            Spacer(Modifier.height(12.dp))
            Text("Itinerary Umrah 9 Hari", fontWeight = FontWeight.Bold, fontSize = 20.sp)
            Text(
                "Jadwal umum -- tanggal, jam, dan urutan detail bisa berbeda tergantung grup keberangkatan. Konfirmasi jadwal pasti ke pembimbing/Tour Leader.",
                fontSize = 11.sp, color = Color.Gray
            )
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth().background(Color(0xFFF0FDF4), RoundedCornerShape(12.dp)).padding(4.dp)) {
                listOf(true to "Ramadhan", false to "Reguler (Non-Ramadhan)").forEach { (value, label) ->
                    val selected = isRamadhan == value
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(if (selected) Color(0xFF0F7A5A) else Color.Transparent, RoundedCornerShape(10.dp))
                            .clickable { isRamadhan = value }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(label, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (selected) Color.White else Color(0xFF0F7A5A))
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
        }
        items(days) { day ->
            Card(shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(1.dp), modifier = Modifier.fillMaxWidth()) {
                Row(Modifier.padding(16.dp)) {
                    Box(
                        modifier = Modifier.size(32.dp).background(Color(0xFF0F7A5A), RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) { Text("${day.hari}", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp) }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(day.rute, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF0F7A5A))
                        Spacer(Modifier.height(4.dp))
                        Text(day.kegiatan, fontSize = 12.sp, color = Color(0xFF374151))
                    }
                }
            }
        }
        item { Spacer(Modifier.height(8.dp)) }
    }
}
