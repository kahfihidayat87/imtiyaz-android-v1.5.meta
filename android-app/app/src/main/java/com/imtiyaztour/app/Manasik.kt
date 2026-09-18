package com.imtiyaztour.app

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET

// ============================================================================
// PANDUAN MANASIK INTERAKTIF -- langkah ibadah umrah, tiap langkah bisa dibuka
// (doa + tips) dan dicentang saat selesai. Progress tersimpan lokal.
// ============================================================================

data class ManasikStep(
    val id: String, val urutan: Int, val kategori: String, val judul: String,
    val deskripsi: String = "", val doa_id: String? = null, val tips: String? = null
)

interface ManasikApiService {
    @GET("api/manasik")
    suspend fun getManasik(): List<ManasikStep>
}

object ManasikApiClient {
    val service: ManasikApiService by lazy {
        Retrofit.Builder().baseUrl(ApiConfig.BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build().create(ManasikApiService::class.java)
    }
}

val defaultManasik = listOf(
    ManasikStep("p1", 1, "Persiapan", "Mandi & Memakai Wewangian",
        "Mandi sunnah ihram, berwudhu, dan memakai wewangian pada BADAN (bukan pakaian ihram).", null,
        "Laki-laki: pakai 2 lembar kain ihram tanpa jahitan. Perempuan: pakaian tertutup biasa, hindari menutup wajah & sarung tangan."),
    ManasikStep("p2", 2, "Persiapan", "Shalat Sunnah Ihram 2 Rakaat",
        "Shalat 2 rakaat di rumah atau di miqat sebelum berniat ihram.", null, null),
    ManasikStep("m1", 3, "Miqat & Niat", "Niat Ihram Umrah",
        "Berniat umrah di miqat yang ditentukan (Bir Ali untuk jamaah dari Madinah, atau di atas pesawat).",
        "niat", "Talbiyah mulai dibaca sejak niat ihram hingga melihat Ka'bah."),
    ManasikStep("m2", 4, "Miqat & Niat", "Membaca Talbiyah",
        "Perbanyak membaca talbiyah sepanjang perjalanan menuju Makkah.", "talbiyah", null),
    ManasikStep("t1", 5, "Memasuki Masjidil Haram", "Masuk dengan Kaki Kanan + Doa",
        "Masuk masjid dengan kaki kanan sambil membaca doa masuk masjid.", "masuk_masjid", null),
    ManasikStep("t2", 6, "Memasuki Masjidil Haram", "Melihat Ka'bah & Berdoa",
        "Saat pertama kali melihat Ka'bah, berdoa dengan doa melihat Ka'bah. Ini waktu mustajab.",
        "lihat_kabah", null),
    ManasikStep("tw1", 7, "Tawaf", "Memulai Tawaf dari Hajar Aswad",
        "Posisi sejajar Hajar Aswad, ucapkan 'Bismillah, Allahu Akbar' setiap putaran.",
        "mulai_tawaf", "Tawaf dimulai dan diakhiri di Hajar Aswad, berlawanan arah jarum jam. Total 7 putaran."),
    ManasikStep("tw2", 8, "Tawaf", "Doa antara Rukun Yamani & Hajar Aswad",
        "Baca doa 'Rabbana atina...' sepanjang jarak antara Rukun Yamani dan Hajar Aswad.",
        "rukun_yamani", "Putaran 1-6: jalan & berdoa bebas. Putaran 7: selesai di Hajar Aswad."),
    ManasikStep("tw3", 9, "Tawaf", "Shalat 2 Rakaat di Maqam Ibrahim",
        "Setelah tawaf, shalat 2 rakaat di belakang Maqam Ibrahim (atau di tempat lain kalau penuh).", null, null),
    ManasikStep("tw4", 10, "Tawaf", "Minum Air Zamzam",
        "Minum air zamzam sambil membaca doa minum zamzam.", "zamzam", null),
    ManasikStep("s1", 11, "Sa'i", "Menuju Bukit Shafa",
        "Naik ke bukit Shafa, menghadap Ka'bah, membaca 'Innash-shafa wal-marwata...' lalu bertakbir.",
        "sai", "Sa'i = 7 kali perjalanan Shafa ke Marwah dihitung 1, Marwah ke Shafa dihitung 1. Total 7."),
    ManasikStep("s2", 12, "Sa'i", "Berjalan Shafa - Marwah",
        "Berjalan dari Shafa ke Marwah (1), lalu Marwah ke Shafa (2), dst hingga 7.",
        null, "Laki-laki disunnahkan berlari kecil di area hijau. Perempuan cukup berjalan biasa."),
    ManasikStep("th1", 13, "Tahallul", "Mencukur / Memotong Rambut",
        "Laki-laki: cukur habis (lebih utama) atau potong pendek. Perempuan: potong sepanjang ujung jari.",
        "tahallul", "Dengan tahallul, rangkaian umrah SELESAI. Semua larangan ihram kembali halal."),
    ManasikStep("th2", 14, "Tahallul", "Doa & Syukur",
        "Berdoa syukur dan perbanyak dzikir. Umrah Anda telah sempurna.",
        null, "Jangan lupa foto bersama rombongan untuk kenang-kenangan!")
)

object ManasikData {
    var steps by mutableStateOf(defaultManasik)
    var loading by mutableStateOf(false)
    var fetched by mutableStateOf(false)

    suspend fun loadIfNeeded() {
        if (fetched || loading) return
        loading = true
        try {
            val remote = withContext(Dispatchers.IO) { ManasikApiClient.service.getManasik() }
            if (remote.isNotEmpty()) steps = remote.sortedBy { it.urutan }
        } catch (e: Exception) { }
        fetched = true; loading = false
    }
}

object ManasikProgress {
    private const val NAME = "manasik_progress"
    private fun prefs(ctx: android.content.Context) =
        ctx.getSharedPreferences(NAME, android.content.Context.MODE_PRIVATE)
    fun isDone(ctx: android.content.Context, id: String) = prefs(ctx).getBoolean(id, false)
    fun setDone(ctx: android.content.Context, id: String, done: Boolean) =
        prefs(ctx).edit().putBoolean(id, done).apply()
    fun reset(ctx: android.content.Context) = prefs(ctx).edit().clear().apply()
}

@Composable
fun ManasikScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var expandedId by remember { mutableStateOf<String?>(null) }
    var version by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) { ManasikData.loadIfNeeded() }

    val total = ManasikData.steps.size
    val done = ManasikData.steps.count { ManasikProgress.isDone(context, it.id) }
    val progress = if (total > 0) done / total.toFloat() else 0f

    LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item {
            Button(onClick = onBack,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F7A5A)),
                shape = RoundedCornerShape(8.dp)) { Text("<- Kembali") }
            Spacer(Modifier.height(12.dp))
            Text("Panduan Manasik Umrah", fontWeight = FontWeight.Bold, fontSize = 22.sp)
            Text("Ikuti langkah demi langkah -- centang yang sudah dilakukan", fontSize = 12.sp, color = Color.Gray)
            Spacer(Modifier.height(12.dp))
            Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFF0FDF4)),
                shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Progres Anda", fontWeight = FontWeight.Bold, fontSize = 14.sp,
                            color = Color(0xFF0F7A5A), modifier = Modifier.weight(1f))
                        Text("$done / $total", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF0F7A5A))
                    }
                    Spacer(Modifier.height(8.dp))
                    LinearProgressIndicator(progress = progress,
                        modifier = Modifier.fillMaxWidth().height(10.dp),
                        color = Color(0xFF0F7A5A), trackColor = Color.White)
                    Spacer(Modifier.height(6.dp))
                    Text("${(progress * 100).toInt()}% selesai", fontSize = 11.sp, color = Color.Gray)
                    if (done > 0) {
                        Spacer(Modifier.height(8.dp))
                        TextButton(onClick = { ManasikProgress.reset(context); version++ }) {
                            Text("Reset Progres", fontSize = 11.sp)
                        }
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
        }
        items(ManasikData.steps, key = { it.id }) { step ->
            @Suppress("UNUSED_EXPRESSION") version
            val isDone = ManasikProgress.isDone(context, step.id)
            val isExpanded = expandedId == step.id
            Card(shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(1.dp), modifier = Modifier.fillMaxWidth()) {
                Column {
                    Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(34.dp).background(
                            if (isDone) Color(0xFF0F7A5A) else Color(0xFFF0FDF4), RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center) {
                            if (isDone) Icon(Icons.Default.Check, contentDescription = null,
                                tint = Color.White, modifier = Modifier.size(20.dp))
                            else Text("${step.urutan}", color = Color(0xFF0F7A5A), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f).clickable {
                            expandedId = if (isExpanded) null else step.id
                        }) {
                            Text(step.judul, fontWeight = FontWeight.Bold, fontSize = 14.sp,
                                color = if (isDone) Color.Gray else Color(0xFF111827))
                            Text(step.kategori, fontSize = 10.sp, color = Color(0xFF0F7A5A))
                        }
                        IconButton(onClick = {
                            ManasikProgress.setDone(context, step.id, !isDone); version++
                        }) {
                            Icon(if (isDone) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                                contentDescription = "Tandai selesai",
                                tint = if (isDone) Color(0xFF0F7A5A) else Color.LightGray)
                        }
                    }
                    AnimatedVisibility(visible = isExpanded) {
                        Column(Modifier.padding(start = 14.dp, end = 14.dp, bottom = 14.dp)) {
                            Divider(Modifier.padding(bottom = 10.dp))
                            if (step.deskripsi.isNotBlank()) {
                                Text(step.deskripsi, fontSize = 13.sp, color = Color(0xFF374151))
                                Spacer(Modifier.height(10.dp))
                            }
                            step.tips?.let { tips ->
                                Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3CD)),
                                    shape = RoundedCornerShape(8.dp)) {
                                    Row(Modifier.padding(10.dp)) {
                                        Icon(Icons.Default.Lightbulb, contentDescription = null,
                                            tint = Color(0xFF92400E), modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(8.dp))
                                        Text(tips, fontSize = 11.sp, color = Color(0xFF92400E))
                                    }
                                }
                                Spacer(Modifier.height(10.dp))
                            }
                            step.doa_id?.let { doaId ->
                                val doa = listDoa.find { it.id == doaId }
                                if (doa != null) {
                                    Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFF0FDF4)),
                                        shape = RoundedCornerShape(8.dp)) {
                                        Column(Modifier.padding(10.dp)) {
                                            Text("Doa terkait: ${doa.judul}", fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold, color = Color(0xFF0F7A5A))
                                            Spacer(Modifier.height(4.dp))
                                            Text(doa.arab, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                            Text(doa.latin, fontSize = 11.sp, color = Color(0xFF374151))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        item { Spacer(Modifier.height(16.dp)) }
    }
}
