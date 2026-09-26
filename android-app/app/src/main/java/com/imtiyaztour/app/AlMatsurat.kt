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
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

// ============================================================================
// AL-MA'TSURAT SUGHRA (Wazhifah Sughra) - susunan Imam Hasan Al-Banna.
// Sumber urutan & jumlah pengulangan: dicocokkan dari beberapa rujukan yang
// saling konsisten (detik.com, liputan6.com, solatdoa.my, akuislam.com) -
// bukan dari ingatan sendiri.
//
// PENTING soal akurasi teks:
// - Bagian AYAT AL-QURAN (Al-Fatihah, Al-Baqarah 1-5, Ayat Kursi+2 ayat
//   setelahnya, Al-Baqarah 284-286, Al-Ikhlas, Al-Falaq, An-Nas) diambil
//   LANGSUNG dari equran.id (QuranApiClient yang sama dengan fitur Quran &
//   Hafalan) - BUKAN diketik manual, supaya akurasinya terjamin.
// - Bagian DZIKIR/DOA yang bukan ayat Quran (Istighfar, Selawat, Doa Pagi,
//   Doa Petang, Doa Perlindungan, Tasbih/Tahmid/Takbir) memang harus berupa
//   teks tetap (equran.id tidak mencakup hadits/dzikir non-Quran) - teksnya
//   diambil dari rujukan di atas, sudah dicek silang beberapa sumber.
//
// SOAL OFFLINE: layar Doa yang sudah ada di app ini SENGAJA didesain bisa
// dibaca tanpa internet (dipakai pas di pesawat / area minim sinyal Masjidil
// Haram). Karena bagian ayat Quran di sini butuh API, saya CACHE hasil fetch
// pertama ke SharedPreferences (MatsuratQuranCache) - fetch cuma perlu
// internet SEKALI, sesudahnya tetap bisa dibaca offline, tanpa mengorbankan
// akurasi (cache-nya tetap berasal dari API, bukan diketik).
// ============================================================================

data class MatsuratItem(
    val id: String,
    val judul: String,
    val jenis: String, // "quran" | "dzikir"
    val nomorSurah: Int? = null,
    val ayatMulai: Int? = null,
    val ayatSelesai: Int? = null,
    val pengulangan: Int = 1,
    val teksArab: String? = null,
    val teksLatin: String? = null,
    val terjemahan: String? = null,
    val waktu: String = "keduanya" // "pagi" | "petang" | "keduanya"
)

val MATSURAT_SUGHRA = listOf(
    MatsuratItem("fatihah", "Surah Al-Fatihah", "quran", nomorSurah = 1, ayatMulai = 1, ayatSelesai = 7),
    MatsuratItem("baqarah_1_5", "Al-Baqarah Ayat 1-5", "quran", nomorSurah = 2, ayatMulai = 1, ayatSelesai = 5),
    MatsuratItem("ayat_kursi", "Ayat Kursi & 2 Ayat Setelahnya", "quran", nomorSurah = 2, ayatMulai = 255, ayatSelesai = 257),
    MatsuratItem("baqarah_284_286", "Al-Baqarah Ayat 284-286", "quran", nomorSurah = 2, ayatMulai = 284, ayatSelesai = 286),
    MatsuratItem("ikhlas", "Surah Al-Ikhlas", "quran", nomorSurah = 112, ayatMulai = 1, ayatSelesai = 4, pengulangan = 3),
    MatsuratItem("falaq", "Surah Al-Falaq", "quran", nomorSurah = 113, ayatMulai = 1, ayatSelesai = 5, pengulangan = 3),
    MatsuratItem("naas", "Surah An-Nas", "quran", nomorSurah = 114, ayatMulai = 1, ayatSelesai = 6, pengulangan = 3),
    MatsuratItem(
        "istighfar", "Istighfar", "dzikir", pengulangan = 3,
        teksArab = "أَسْتَغْفِرُ اللهَ الْعَظِيمَ",
        teksLatin = "Astaghfirullahal 'Azhiim",
        terjemahan = "Aku memohon ampun kepada Allah Yang Maha Agung"
    ),
    MatsuratItem(
        "selawat", "Selawat Nabi", "dzikir", pengulangan = 10,
        teksArab = "اَللّٰهُمَّ صَلِّ عَلٰى سَيِّدِنَا مُحَمَّدٍ وَعَلٰى آلِ سَيِّدِنَا مُحَمَّدٍ",
        teksLatin = "Allahumma shalli 'ala sayyidina Muhammad wa 'ala ali sayyidina Muhammad",
        terjemahan = "Ya Allah, limpahkanlah rahmat kepada junjungan kami Nabi Muhammad dan keluarganya"
    ),
    MatsuratItem(
        "doa_pagi", "Doa Pagi", "dzikir", waktu = "pagi",
        teksArab = "اَللّٰهُمَّ بِكَ أَصْبَحْنَا وَبِكَ أَمْسَيْنَا وَبِكَ نَحْيَا وَبِكَ نَمُوتُ وَإِلَيْكَ النُّشُورُ",
        teksLatin = "Allahumma bika ashbahnaa wa bika amsainaa wa bika nahyaa wa bika namuutu wa ilaikan nusyuur",
        terjemahan = "Ya Allah, dengan-Mu kami memasuki waktu pagi, dengan-Mu kami memasuki waktu petang, dengan-Mu kami hidup, dengan-Mu kami mati, dan kepada-Mu kami dibangkitkan"
    ),
    MatsuratItem(
        "doa_petang", "Doa Petang", "dzikir", waktu = "petang",
        teksArab = "اَللّٰهُمَّ بِكَ أَمْسَيْنَا وَبِكَ أَصْبَحْنَا وَبِكَ نَحْيَا وَبِكَ نَمُوتُ وَإِلَيْكَ الْمَصِيْرُ",
        teksLatin = "Allahumma bika amsainaa wa bika ashbahnaa wa bika nahyaa wa bika namuutu wa ilaikal mashiir",
        terjemahan = "Ya Allah, dengan-Mu kami memasuki waktu petang, dengan-Mu kami memasuki waktu pagi, dengan-Mu kami hidup, dengan-Mu kami mati, dan kepada-Mu tempat kembali"
    ),
    MatsuratItem(
        "doa_perlindungan", "Doa Perlindungan", "dzikir", pengulangan = 3,
        teksArab = "بِسْمِ اللهِ الَّذِي لَا يَضُرُّ مَعَ اسْمِهِ شَيْءٌ فِي الْأَرْضِ وَلَا فِي السَّمَاءِ وَهُوَ السَّمِيعُ الْعَلِيمُ",
        teksLatin = "Bismillaahil ladzii laa yadhurru ma'asmihii syai-un fil ardhi wa laa fis samaa-i wa huwas samii'ul 'aliim",
        terjemahan = "Dengan nama Allah yang dengan nama-Nya tidak ada sesuatu pun yang membahayakan di bumi maupun di langit, dan Dia Maha Mendengar lagi Maha Mengetahui"
    ),
    MatsuratItem(
        "tasbih", "Tasbih", "dzikir", pengulangan = 33,
        teksArab = "سُبْحَانَ اللهِ", teksLatin = "Subhaanallah", terjemahan = "Maha Suci Allah"
    ),
    MatsuratItem(
        "tahmid", "Tahmid", "dzikir", pengulangan = 33,
        teksArab = "الْحَمْدُ لِلّٰهِ", teksLatin = "Alhamdulillah", terjemahan = "Segala puji bagi Allah"
    ),
    MatsuratItem(
        "takbir", "Takbir", "dzikir", pengulangan = 33,
        teksArab = "اللهُ أَكْبَرُ", teksLatin = "Allahu Akbar", terjemahan = "Allah Maha Besar"
    ),
)

/* ======================= CACHE LOKAL AYAT (untuk mode offline) ======================= */

private data class AyatCacheEntry(val teksArab: String, val teksIndonesia: String)

object MatsuratQuranCache {
    private val gson = Gson()
    private const val PREF = "matsurat_quran_cache"
    private fun prefs(ctx: android.content.Context) = ctx.getSharedPreferences(PREF, android.content.Context.MODE_PRIVATE)

    fun get(ctx: android.content.Context, nomorSurah: Int): List<AyatItem>? {
        val json = prefs(ctx).getString("surah_$nomorSurah", null) ?: return null
        return try {
            val type = object : TypeToken<List<AyatCacheEntry>>() {}.type
            val entries = gson.fromJson<List<AyatCacheEntry>>(json, type)
            entries.mapIndexed { i, e -> AyatItem(nomorAyat = i + 1, teksArab = e.teksArab, teksLatin = "", teksIndonesia = e.teksIndonesia) }
        } catch (e: Exception) { null }
    }

    fun save(ctx: android.content.Context, nomorSurah: Int, ayat: List<AyatItem>) {
        val entries = ayat.map { AyatCacheEntry(it.teksArab, it.teksIndonesia) }
        try { prefs(ctx).edit().putString("surah_$nomorSurah", gson.toJson(entries)).apply() } catch (e: Exception) { }
    }
}

/** Ambil ayat surah - coba cache dulu (jalan offline), baru API kalau belum pernah di-cache. */
private suspend fun ambilAyatDenganCache(context: android.content.Context, nomorSurah: Int): List<AyatItem>? {
    MatsuratQuranCache.get(context, nomorSurah)?.let { return it }
    return try {
        val resp = withContext(Dispatchers.IO) { QuranApiClient.service.getSurah(nomorSurah) }
        val ayat = resp.data?.ayat
        if (!ayat.isNullOrEmpty()) MatsuratQuranCache.save(context, nomorSurah, ayat)
        ayat
    } catch (e: Exception) { null }
}

/* ======================= PROGRES HARIAN (lokal, privat) ======================= */

object MatsuratProgress {
    private fun prefs(ctx: android.content.Context) = ctx.getSharedPreferences("matsurat_progress", android.content.Context.MODE_PRIVATE)
    private fun todayKey(waktu: String, itemId: String): String {
        val tgl = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        return "${tgl}_${waktu}_$itemId"
    }
    fun isDone(ctx: android.content.Context, waktu: String, itemId: String) = prefs(ctx).getBoolean(todayKey(waktu, itemId), false)
    fun setDone(ctx: android.content.Context, waktu: String, itemId: String, done: Boolean) =
        prefs(ctx).edit().putBoolean(todayKey(waktu, itemId), done).apply()
}

/* ======================= UI ======================= */

@Composable
fun AlMatsuratScreen(onBack: () -> Unit) {
    var waktu by remember { mutableStateOf(if (Calendar.getInstance().get(Calendar.HOUR_OF_DAY) < 15) "pagi" else "petang") }
    val daftar = remember(waktu) { MATSURAT_SUGHRA.filter { it.waktu == "keduanya" || it.waktu == waktu } }
    val doneCount = remember { mutableIntStateOf(0) }
    val context = LocalContext.current

    LaunchedEffect(waktu) {
        doneCount.intValue = daftar.count { MatsuratProgress.isDone(context, waktu, it.id) }
    }

    LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            Button(onClick = onBack, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F7A5A)), shape = RoundedCornerShape(8.dp)) { Text("\u2190 Kembali") }
            Spacer(Modifier.height(12.dp))
            Text("Al-Ma'tsurat Sughra", fontWeight = FontWeight.Bold, fontSize = 20.sp)
            Text("Susunan Imam Hasan Al-Banna - bisa dibaca offline setelah pertama kali dibuka", fontSize = 11.5.sp, color = Color.Gray)
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth().background(Color(0xFFF0FDF4), RoundedCornerShape(12.dp)).padding(4.dp)) {
                listOf("pagi" to "Pagi", "petang" to "Petang").forEach { (value, label) ->
                    val dipilih = waktu == value
                    Box(
                        Modifier.weight(1f)
                            .background(if (dipilih) Color(0xFF0F7A5A) else Color.Transparent, RoundedCornerShape(10.dp))
                            .clickable { waktu = value }
                            .padding(vertical = 9.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(label, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (dipilih) Color.White else Color(0xFF0F7A5A))
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            Text("${doneCount.intValue} dari ${daftar.size} bacaan selesai hari ini", fontSize = 11.sp, color = Color(0xFF0F7A5A), fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
        }
        items(daftar, key = { it.id }) { matsuratItem ->
            MatsuratCard(
                item = matsuratItem, waktu = waktu,
                onSelesaiChange = { selesai ->
                    MatsuratProgress.setDone(context, waktu, matsuratItem.id, selesai)
                    doneCount.intValue = daftar.count { MatsuratProgress.isDone(context, waktu, it.id) }
                }
            )
        }
        item { Spacer(Modifier.height(16.dp)) }
    }
}

@Composable
private fun MatsuratCard(item: MatsuratItem, waktu: String, onSelesaiChange: (Boolean) -> Unit) {
    val context = LocalContext.current
    var expanded by remember { mutableStateOf(false) }
    var ayatQuran by remember(item.id) { mutableStateOf<List<AyatItem>?>(null) }
    var loading by remember(item.id) { mutableStateOf(item.jenis == "quran") }
    var errorMsg by remember(item.id) { mutableStateOf("") }
    var sisaUlang by remember(item.id) { mutableIntStateOf(item.pengulangan) }
    var selesai by remember(item.id) { mutableStateOf(MatsuratProgress.isDone(context, waktu, item.id)) }

    LaunchedEffect(item.id, expanded) {
        if (item.jenis == "quran" && expanded && ayatQuran == null) {
            loading = true
            val semuaAyat = ambilAyatDenganCache(context, item.nomorSurah!!)
            if (semuaAyat == null) {
                errorMsg = "Belum tersedia offline - buka sekali saat ada internet untuk menyimpannya"
            } else {
                ayatQuran = semuaAyat.filter { it.nomorAyat in item.ayatMulai!!..item.ayatSelesai!! }
            }
            loading = false
        }
    }

    Card(shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(1.dp), modifier = Modifier.fillMaxWidth()) {
        Column {
            Row(Modifier.fillMaxWidth().clickable { expanded = !expanded }.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    if (selesai) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                    contentDescription = null, tint = if (selesai) Color(0xFF0F7A5A) else Color.LightGray,
                    modifier = Modifier.size(22.dp).clickable {
                        selesai = !selesai
                        onSelesaiChange(selesai)
                    }
                )
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(item.judul, fontWeight = FontWeight.Bold, fontSize = 13.5.sp, color = if (selesai) Color.Gray else Color(0xFF111827))
                    if (item.pengulangan > 1) Text("Dibaca ${item.pengulangan}x", fontSize = 10.sp, color = Color(0xFF0F7A5A))
                }
                Icon(if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown, contentDescription = null, tint = Color.Gray)
            }
            AnimatedVisibility(visible = expanded) {
                Column(Modifier.padding(start = 14.dp, end = 14.dp, bottom = 14.dp)) {
                    Divider(Modifier.padding(bottom = 10.dp))
                    when {
                        item.jenis == "dzikir" -> {
                            Text(item.teksArab ?: "", fontSize = 18.sp, fontWeight = FontWeight.Bold, lineHeight = 30.sp)
                            Spacer(Modifier.height(6.dp))
                            Text(item.teksLatin ?: "", fontSize = 12.sp, color = Color(0xFF374151), fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)
                            Spacer(Modifier.height(4.dp))
                            Text(item.terjemahan ?: "", fontSize = 12.sp, color = Color(0xFF0F7A5A))
                        }
                        loading -> Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = Color(0xFF0F7A5A)) }
                        errorMsg.isNotEmpty() -> Text(errorMsg, fontSize = 11.sp, color = Color(0xFFDC2626))
                        else -> ayatQuran?.forEach { ayat ->
                            Text(ayat.teksArab, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, lineHeight = 30.sp, modifier = Modifier.padding(bottom = 4.dp))
                            Text(ayat.teksIndonesia, fontSize = 11.sp, color = Color(0xFF6B7F78), modifier = Modifier.padding(bottom = 10.dp))
                        }
                    }

                    if (item.pengulangan > 1) {
                        Spacer(Modifier.height(10.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Hitungan: ", fontSize = 11.sp, color = Color.Gray)
                            IconButton(onClick = { if (sisaUlang > 0) sisaUlang-- }, modifier = Modifier.size(28.dp)) {
                                Icon(Icons.Default.RemoveCircleOutline, contentDescription = null, tint = Color(0xFF0F7A5A))
                            }
                            Text("$sisaUlang / ${item.pengulangan}", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            IconButton(onClick = { if (sisaUlang < item.pengulangan) sisaUlang++ }, modifier = Modifier.size(28.dp)) {
                                Icon(Icons.Default.AddCircleOutline, contentDescription = null, tint = Color(0xFF0F7A5A))
                            }
                            if (sisaUlang == 0 && !selesai) {
                                Spacer(Modifier.weight(1f))
                                TextButton(onClick = { selesai = true; onSelesaiChange(true) }) { Text("Tandai Selesai", fontSize = 11.sp) }
                            }
                        }
                    }
                }
            }
        }
    }
}
