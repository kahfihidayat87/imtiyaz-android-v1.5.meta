package com.imtiyaztour.app

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
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

// ============================================================================
// LATIHAN HAFALAN AL-QURAN - sengaja BUKAN dinamakan/dijadikan "puzzle".
// Model: tebak SATU kata yang disembunyikan dari sebuah ayat, dari surah
// pendek yang biasa dihafal duluan (An-Nas mundur ke Al-Ma'un). Semua teks
// ayat DIAMBIL LANGSUNG dari equran.id (sama seperti Quran.kt) setiap kali
// dibuka - termasuk PILIHAN JAWABAN SALAH, yang diambil dari kata lain di
// surah yang sama (bukan saya karang) - supaya tidak ada risiko kutipan
// keliru sedikit pun. Progres (surah mana yang sudah tuntas) tersimpan
// LOKAL di HP saja, pola sama seperti Istiqamah.kt/Journal.kt.
// ============================================================================

// Seluruh Juz 30 (Juz Amma): An-Naba (78) sampai An-Nas (114), 37 surah.
// Urutan MUNDUR dari 114 (paling pendek/lazim dihafal duluan) ke 78 (paling
// panjang di juz ini, 40 ayat) - progres jadi makin panjang seiring naik
// level, bukan acak.
val SURAH_HAFALAN_URUTAN = (114 downTo 78).toList()

object HafalanStore {
    private const val PREF = "hafalan_quran"
    private fun prefs(ctx: android.content.Context) = ctx.getSharedPreferences(PREF, android.content.Context.MODE_PRIVATE)

    fun ayatSelesai(ctx: android.content.Context, nomorSurah: Int, nomorAyat: Int): Boolean =
        prefs(ctx).getBoolean("ayat_${nomorSurah}_$nomorAyat", false)

    fun tandaiSelesai(ctx: android.content.Context, nomorSurah: Int, nomorAyat: Int) {
        prefs(ctx).edit().putBoolean("ayat_${nomorSurah}_$nomorAyat", true).apply()
    }

    fun surahTuntas(ctx: android.content.Context, nomorSurah: Int, jumlahAyat: Int): Boolean =
        (1..jumlahAyat).all { ayatSelesai(ctx, nomorSurah, it) }

    /** Surah pertama di urutan yang BELUM tuntas - urutan sebelumnya harus sudah tuntas dulu (progres berurutan). */
    fun surahTerbukaTerakhir(ctx: android.content.Context, jumlahAyatMap: Map<Int, Int>): Int {
        for (nomor in SURAH_HAFALAN_URUTAN) {
            val total = jumlahAyatMap[nomor] ?: continue
            if (!surahTuntas(ctx, nomor, total)) return nomor
        }
        return SURAH_HAFALAN_URUTAN.last()
    }
}

@Composable
fun HafalanQuranScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var daftarSurah by remember { mutableStateOf<List<SurahSummary>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var errorMsg by remember { mutableStateOf("") }
    var surahDipilih by remember { mutableStateOf<Int?>(null) }
    var mode by remember { mutableStateOf("tebak") } // "tebak" | "susun"
    var refreshKey by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        try {
            val resp = withContext(Dispatchers.IO) { QuranApiClient.service.getAllSurah() }
            daftarSurah = (resp.data ?: emptyList()).filter { it.nomor in SURAH_HAFALAN_URUTAN }
        } catch (e: Exception) {
            errorMsg = "Gagal memuat daftar surah - periksa koneksi internet"
        }
        loading = false
    }

    if (surahDipilih != null) {
        if (mode == "susun") {
            SusunAyatScreen(nomorSurah = surahDipilih!!, onBack = { surahDipilih = null; refreshKey++ })
        } else {
            LatihanSurahScreen(nomorSurah = surahDipilih!!, onBack = { surahDipilih = null; refreshKey++ })
        }
        return
    }

    val jumlahAyatMap = remember(daftarSurah) { daftarSurah.associate { it.nomor to it.jumlahAyat } }
    val terbukaTerakhir = remember(daftarSurah, refreshKey) {
        if (jumlahAyatMap.isEmpty()) null else HafalanStore.surahTerbukaTerakhir(context, jumlahAyatMap)
    }
    val terbukaIndex = SURAH_HAFALAN_URUTAN.indexOf(terbukaTerakhir)

    LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            Button(onClick = onBack, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F7A5A)), shape = RoundedCornerShape(8.dp)) { Text("\u2190 Kembali") }
            Spacer(Modifier.height(12.dp))
            Text("Latihan Hafalan Al-Quran", fontWeight = FontWeight.Bold, fontSize = 20.sp)
            Text("Tebak kata yang hilang - mulai dari surah pendek, urut membuka surah berikutnya", fontSize = 11.5.sp, color = Color.Gray)
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth().background(Color(0xFFF0FDF4), RoundedCornerShape(12.dp)).padding(4.dp)) {
                listOf("tebak" to "Tebak Kata", "susun" to "Susun Ayat").forEach { (value, label) ->
                    val dipilih = mode == value
                    Box(
                        Modifier.weight(1f)
                            .background(if (dipilih) Color(0xFF0F7A5A) else Color.Transparent, RoundedCornerShape(10.dp))
                            .clickable { mode = value }
                            .padding(vertical = 9.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(label, fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = if (dipilih) Color.White else Color(0xFF0F7A5A))
                    }
                }
            }
            Spacer(Modifier.height(4.dp))
            if (loading) LinearProgressIndicator(Modifier.fillMaxWidth())
            if (errorMsg.isNotEmpty()) Text(errorMsg, fontSize = 12.sp, color = Color(0xFFDC2626))
        }
        itemsIndexed(daftarSurah.sortedByDescending { it.nomor }) { _, surah ->
            val idxUrutan = SURAH_HAFALAN_URUTAN.indexOf(surah.nomor)
            val terkunci = terbukaIndex >= 0 && idxUrutan > terbukaIndex
            val tuntas = HafalanStore.surahTuntas(context, surah.nomor, surah.jumlahAyat)
            SurahHafalanRow(surah, terkunci, tuntas) { if (!terkunci) surahDipilih = surah.nomor }
        }
        item { Spacer(Modifier.height(8.dp)) }
    }
}

@Composable
private fun SurahHafalanRow(surah: SurahSummary, terkunci: Boolean, tuntas: Boolean, onClick: () -> Unit) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = if (terkunci) Color(0xFFF5F5F5) else Color.White),
        elevation = CardDefaults.cardElevation(if (terkunci) 0.dp else 1.dp),
        modifier = Modifier.fillMaxWidth().clickable(enabled = !terkunci, onClick = onClick)
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(38.dp).background(if (tuntas) Color(0xFF0F7A5A) else if (terkunci) Color(0xFFE0E0E0) else Color(0xFFF0FDF4), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                when {
                    tuntas -> Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                    terkunci -> Icon(Icons.Default.Lock, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(16.dp))
                    else -> Text("${surah.nomor}", color = Color(0xFF0F7A5A), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(surah.namaLatin, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = if (terkunci) Color.Gray else Color(0xFF111827))
                Text("${surah.arti} \u2022 ${surah.jumlahAyat} ayat", fontSize = 10.sp, color = Color.Gray)
            }
            if (!terkunci) Text(surah.nama, fontSize = 15.sp, color = Color(0xFF0F7A5A))
        }
    }
}

@Composable
private fun LatihanSurahScreen(nomorSurah: Int, onBack: () -> Unit) {
    val context = LocalContext.current
    var detail by remember { mutableStateOf<SurahDetail?>(null) }
    var loading by remember { mutableStateOf(true) }
    var errorMsg by remember { mutableStateOf("") }
    var indexAyat by remember { mutableIntStateOf(0) }

    LaunchedEffect(nomorSurah) {
        try {
            val resp = withContext(Dispatchers.IO) { QuranApiClient.service.getSurah(nomorSurah) }
            detail = resp.data
            indexAyat = (detail?.ayat ?: emptyList()).indexOfFirst { !HafalanStore.ayatSelesai(context, nomorSurah, it.nomorAyat) }.let { if (it < 0) 0 else it }
        } catch (e: Exception) {
            errorMsg = "Gagal memuat surah - periksa koneksi internet"
        }
        loading = false
    }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Button(onClick = onBack, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F7A5A)), shape = RoundedCornerShape(8.dp)) { Text("\u2190 Kembali") }
        Spacer(Modifier.height(12.dp))

        val d = detail
        when {
            loading -> Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = Color(0xFF0F7A5A)) }
            errorMsg.isNotEmpty() -> Text(errorMsg, fontSize = 12.sp, color = Color(0xFFDC2626))
            d == null -> Text("Data surah tidak ditemukan", fontSize = 12.sp, color = Color.Gray)
            else -> {
                val semuaAyat = d.ayat

                Text(d.namaLatin, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color(0xFF0F7A5A))
                Text("Ayat ${indexAyat + 1} dari ${semuaAyat.size}", fontSize = 11.sp, color = Color.Gray)
                Spacer(Modifier.height(6.dp))
                LinearProgressIndicator(
                    progress = (indexAyat).toFloat() / semuaAyat.size.coerceAtLeast(1),
                    modifier = Modifier.fillMaxWidth().height(6.dp), color = Color(0xFF0F7A5A), trackColor = Color(0xFFF0FDF4)
                )
                Spacer(Modifier.height(16.dp))

                if (indexAyat >= semuaAyat.size) {
                    SelesaiSurahCard(d.namaLatin, onBack)
                } else {
                    val ayatSekarang = semuaAyat[indexAyat]
                    TebakKataCard(
                        ayat = ayatSekarang,
                        semuaAyatSurah = semuaAyat,
                        onBenar = {
                            HafalanStore.tandaiSelesai(context, nomorSurah, ayatSekarang.nomorAyat)
                            indexAyat++
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun SelesaiSurahCard(namaSurah: String, onBack: () -> Unit) {
    Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFFF0FDF4)), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("\uD83C\uDF89", fontSize = 40.sp)
            Spacer(Modifier.height(10.dp))
            Text("Surah $namaSurah Tuntas!", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF0F7A5A))
            Text("Surah berikutnya sudah terbuka.", fontSize = 12.sp, color = Color.Gray)
            Spacer(Modifier.height(16.dp))
            Button(onClick = onBack, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F7A5A))) { Text("Kembali ke Daftar Surah") }
        }
    }
}

/**
 * Sembunyikan SATU kata dari ayat ini, pilihan jawaban = kata asli + 2 kata
 * lain yang diambil dari ayat-ayat LAIN pada surah yang SAMA (bukan dikarang) -
 * jadi walau pilihan yang salah, tetap kata yang benar-benar ada di surah itu.
 */
@Composable
private fun TebakKataCard(ayat: AyatItem, semuaAyatSurah: List<AyatItem>, onBenar: () -> Unit) {
    var terjawab by remember(ayat.nomorAyat) { mutableStateOf(false) }
    var pilihanUser by remember(ayat.nomorAyat) { mutableStateOf<String?>(null) }

    val soal = remember(ayat.nomorAyat) {
        val kataAyat = ayat.teksArab.split(" ").filter { it.isNotBlank() }
        val kandidatIndex = kataAyat.indices.filter { kataAyat[it].length >= 3 }
        val idxDisembunyikan = if (kandidatIndex.isNotEmpty()) kandidatIndex.random() else kataAyat.indices.random()
        val kataBenar = kataAyat.getOrElse(idxDisembunyikan) { kataAyat.first() }

        val kataLain = semuaAyatSurah.filter { it.nomorAyat != ayat.nomorAyat }
            .flatMap { it.teksArab.split(" ") }
            .filter { it.isNotBlank() && it.length >= 3 && it != kataBenar }
            .distinct()
        val pengecoh = if (kataLain.size >= 2) kataLain.shuffled().take(2) else listOf("\u0644\u0644\u0647", "\u0645ِنْ").filter { it != kataBenar }

        val tampilan = kataAyat.mapIndexed { i, k -> if (i == idxDisembunyikan) "____" else k }.joinToString(" ")
        Triple(tampilan, kataBenar, (pengecoh + kataBenar).shuffled())
    }
    val (teksSoal, jawabanBenar, pilihan) = soal

    Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(2.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(20.dp)) {
            Text("Lengkapi ayat berikut:", fontSize = 12.sp, color = Color.Gray)
            Spacer(Modifier.height(10.dp))
            Text(teksSoal, fontSize = 22.sp, fontWeight = FontWeight.Bold, lineHeight = 38.sp, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(4.dp))
            Text(ayat.teksIndonesia, fontSize = 11.5.sp, color = Color(0xFF6B7F78), fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)
            Spacer(Modifier.height(18.dp))

            pilihan.forEach { opsi ->
                val ini = pilihanUser == opsi
                val warna = when {
                    !terjawab -> Color.White
                    opsi == jawabanBenar -> Color(0xFFE8F5E9)
                    ini -> Color(0xFFFFEBEE)
                    else -> Color.White
                }
                val border = when {
                    !terjawab -> Color(0xFFE0E0E0)
                    opsi == jawabanBenar -> Color(0xFF0F7A5A)
                    ini -> Color(0xFFDC2626)
                    else -> Color(0xFFE0E0E0)
                }
                Box(
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .background(warna, RoundedCornerShape(12.dp))
                        .clickable(enabled = !terjawab) { pilihanUser = opsi; terjawab = true }
                        .padding(14.dp)
                ) {
                    Text(opsi, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, textAlign = androidx.compose.ui.text.style.TextAlign.Center, modifier = Modifier.fillMaxWidth())
                }
            }

            if (terjawab) {
                Spacer(Modifier.height(12.dp))
                val benar = pilihanUser == jawabanBenar
                Text(
                    if (benar) "Benar! Lanjut ke ayat berikutnya." else "Belum tepat - jawaban yang benar sudah ditandai hijau.",
                    color = if (benar) Color(0xFF0F7A5A) else Color(0xFFDC2626), fontWeight = FontWeight.Bold, fontSize = 12.5.sp
                )
                Spacer(Modifier.height(10.dp))
                Button(onClick = onBenar, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F7A5A)), modifier = Modifier.fillMaxWidth()) {
                    Text(if (benar) "Lanjut" else "Lanjut (tetap boleh lanjut)")
                }
            }
        }
    }
}
