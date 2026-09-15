package com.imtiyaztour.app

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path

// ============================================================================
// E-QURAN -- pakai API gratis equran.id v2 (tanpa API key, berbahasa Indonesia,
// sudah termasuk audio murottal per-ayat dari 6 qari). Referensi dokumentasi:
// https://equran.id/apidev/v2
// ============================================================================
object QuranApiConfig {
    const val BASE_URL = "https://equran.id/api/v2/"
    const val DEFAULT_QARI = "05" // Misyari Rasyid Al-Afasi
}

data class QuranApiResponse<T>(val code: Int? = null, val message: String? = null, val data: T? = null)

data class SurahSummary(
    val nomor: Int,
    val nama: String,
    val namaLatin: String,
    val jumlahAyat: Int,
    val tempatTurun: String,
    val arti: String,
    val deskripsi: String? = null,
    val audioFull: Map<String, String>? = null
)

data class AyatItem(
    val nomorAyat: Int,
    val teksArab: String,
    val teksLatin: String,
    val teksIndonesia: String,
    val audio: Map<String, String>? = null
)

data class SurahDetail(
    val nomor: Int,
    val nama: String,
    val namaLatin: String,
    val jumlahAyat: Int,
    val tempatTurun: String,
    val arti: String,
    val deskripsi: String? = null,
    val audioFull: Map<String, String>? = null,
    val ayat: List<AyatItem> = emptyList()
)

interface QuranApiService {
    @GET("surat")
    suspend fun getAllSurah(): QuranApiResponse<List<SurahSummary>>

    @GET("surat/{nomor}")
    suspend fun getSurah(@Path("nomor") nomor: Int): QuranApiResponse<SurahDetail>
}

object QuranApiClient {
    val service: QuranApiService by lazy {
        Retrofit.Builder()
            .baseUrl(QuranApiConfig.BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(QuranApiService::class.java)
    }
}

object QuranData {
    var surahList by mutableStateOf<List<SurahSummary>>(emptyList())
    var isLoading by mutableStateOf(false)
    var errorMsg by mutableStateOf("")

    suspend fun loadIfNeeded() {
        if (surahList.isNotEmpty() || isLoading) return
        isLoading = true; errorMsg = ""
        try {
            val resp = withContext(Dispatchers.IO) { QuranApiClient.service.getAllSurah() }
            surahList = resp.data ?: emptyList()
            if (surahList.isEmpty()) errorMsg = "Data surat kosong, coba lagi"
        } catch (e: Exception) {
            errorMsg = "Gagal memuat daftar surat -- periksa koneksi internet"
        }
        isLoading = false
    }
}

@Composable
fun QuranScreen(onSurahClick: (Int) -> Unit) {
    val scope = rememberCoroutineScope()
    var query by remember { mutableStateOf("") }

    LaunchedEffect(Unit) { QuranData.loadIfNeeded() }

    val filtered = remember(query, QuranData.surahList) {
        if (query.isBlank()) QuranData.surahList
        else QuranData.surahList.filter {
            it.namaLatin.contains(query, ignoreCase = true) || it.arti.contains(query, ignoreCase = true)
        }
    }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("Al-Quran Digital", fontWeight = FontWeight.Bold, fontSize = 20.sp)
        Text("114 surat, teks Arab, Latin, terjemahan & audio murottal", fontSize = 11.sp, color = Color.Gray)
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = query, onValueChange = { query = it },
            label = { Text("Cari nama surat...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), singleLine = true
        )
        Spacer(Modifier.height(12.dp))

        if (QuranData.isLoading) {
            Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = Color(0xFF0F7A5A)) }
        }
        if (QuranData.errorMsg.isNotEmpty()) {
            Column(Modifier.fillMaxWidth().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(QuranData.errorMsg, fontSize = 12.sp, color = Color(0xFFDC2626))
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = { scope.launch { QuranData.surahList = emptyList(); QuranData.loadIfNeeded() } },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F7A5A))
                ) { Text("Coba Lagi") }
            }
        }

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(filtered) { surah ->
                Card(
                    shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(1.dp),
                    modifier = Modifier.fillMaxWidth().clickable { onSurahClick(surah.nomor) }
                ) {
                    Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier.size(36.dp).background(Color(0xFFF0FDF4), RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center
                        ) { Text("${surah.nomor}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F7A5A)) }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(surah.namaLatin, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text("${surah.arti} • ${surah.tempatTurun} • ${surah.jumlahAyat} ayat", fontSize = 10.sp, color = Color.Gray)
                        }
                        Text(surah.nama, fontSize = 16.sp, color = Color(0xFF0F7A5A))
                    }
                }
            }
        }
    }
}

@Composable
fun SurahDetailScreen(nomor: Int, onBack: () -> Unit) {
    var detail by remember { mutableStateOf<SurahDetail?>(null) }
    var loading by remember { mutableStateOf(true) }
    var errorMsg by remember { mutableStateOf("") }

    LaunchedEffect(nomor) {
        loading = true; errorMsg = ""; detail = null
        try {
            val resp = withContext(Dispatchers.IO) { QuranApiClient.service.getSurah(nomor) }
            detail = resp.data
            if (detail == null) errorMsg = "Data surat tidak ditemukan"
        } catch (e: Exception) {
            errorMsg = "Gagal memuat surat -- periksa koneksi internet"
        }
        loading = false
    }

    DisposableEffect(Unit) { onDispose { AudioPlayerManager.stop() } }

    LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            Button(onClick = onBack, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F7A5A)), shape = RoundedCornerShape(8.dp)) { Text("← Kembali") }
            Spacer(Modifier.height(12.dp))
            if (loading) { Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = Color(0xFF0F7A5A)) } }
            if (errorMsg.isNotEmpty()) { Text(errorMsg, fontSize = 12.sp, color = Color(0xFFDC2626)) }
            detail?.let { d ->
                Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF0F7A5A)), modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(18.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column {
                                Text(d.namaLatin, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                                Text("${d.arti} • ${d.tempatTurun} • ${d.jumlahAyat} ayat", color = Color(0xFFD1FAE5), fontSize = 11.sp)
                            }
                            Text(d.nama, color = Color.White, fontSize = 22.sp)
                        }
                        val fullAudio = d.audioFull?.get(QuranApiConfig.DEFAULT_QARI)
                        if (fullAudio != null) {
                            Spacer(Modifier.height(10.dp))
                            OutlinedButton(
                                onClick = { AudioPlayerManager.toggle(fullAudio) },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                            ) {
                                val playing = AudioPlayerManager.currentlyPlayingUrl == fullAudio
                                Icon(if (playing) Icons.Default.Stop else Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(if (playing) "Hentikan Audio Full Surat" else "Putar Audio Full Surat", fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        }
        detail?.let { d ->
            items(d.ayat) { ayat ->
                val audioUrl = ayat.audio?.get(QuranApiConfig.DEFAULT_QARI)
                val playing = AudioPlayerManager.currentlyPlayingUrl == audioUrl
                Card(shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(1.dp), modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(26.dp).background(Color(0xFFF0FDF4), RoundedCornerShape(6.dp)), contentAlignment = Alignment.Center) {
                                Text("${ayat.nomorAyat}", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F7A5A))
                            }
                            if (audioUrl != null) {
                                IconButton(onClick = { AudioPlayerManager.toggle(audioUrl) }, modifier = Modifier.size(28.dp)) {
                                    Icon(if (playing) Icons.Default.Stop else Icons.Default.PlayArrow, contentDescription = null, tint = Color(0xFF0F7A5A))
                                }
                            }
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(ayat.teksArab, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.Black, modifier = Modifier.fillMaxWidth())
                        Spacer(Modifier.height(8.dp))
                        Text(ayat.teksLatin, fontSize = 12.sp, color = Color(0xFF374151))
                        Spacer(Modifier.height(6.dp))
                        Text(ayat.teksIndonesia, fontSize = 12.sp, color = Color(0xFF0F7A5A))
                    }
                }
            }
        }
    }
}
