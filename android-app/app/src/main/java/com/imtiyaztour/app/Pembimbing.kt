package com.imtiyaztour.app

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET

// ============================================================================
// PEMBIMBING UMRAH -- daftar ustadz/ustadzah pembimbing, masing-masing punya
// halaman profil sendiri dengan materi ceramah singkat (audio, opsional). Semua
// dikelola Admin lewat WP Admin (tab "Pembimbing"), termasuk mengganti nama --
// tidak ada satu pun yang di-hardcode di aplikasi.
// ============================================================================

data class PembimbingAudio(val judul: String = "", val url: String = "")

data class Pembimbing(
    val id: String,
    val nama: String,
    val jabatan: String = "",
    val pengalaman_tahun: Int? = null,
    val spesialisasi: String = "",
    val deskripsi: String = "",
    val audio_list: List<PembimbingAudio> = emptyList()
)

interface PembimbingApiService {
    @GET("api/pembimbing")
    suspend fun getPembimbing(): List<Pembimbing>
}

object PembimbingApiClient {
    val service: PembimbingApiService by lazy {
        Retrofit.Builder()
            .baseUrl(ApiConfig.BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(PembimbingApiService::class.java)
    }
}

// Ambil 2 huruf inisial dari nama, melewati gelar di depan (Ustadz, H., dst)
// supaya "Ustadz H. Ahmad Fauzi, Lc." jadi "AF", bukan "UH".
private val LEADING_HONORIFICS = setOf("ustadz", "ustadzah", "ust", "usth", "h", "hj", "dr", "kh", "prof")
private fun initialsFromName(nama: String): String {
    val words = nama.split(Regex("\\s+")).filter { it.isNotBlank() }
    val filtered = words.dropWhile { w -> w.trim('.', ',').lowercase() in LEADING_HONORIFICS }
    val picked = (if (filtered.size >= 2) filtered else words).take(2)
    val initials = picked.joinToString("") { it.trim('.', ',').take(1).uppercase() }
    return initials.ifEmpty { "??" }.take(2)
}

@Composable
fun PembimbingListScreen(onPembimbingClick: (Pembimbing) -> Unit, onBack: () -> Unit) {
    var list by remember { mutableStateOf<List<Pembimbing>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var errorMsg by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        loading = true; errorMsg = ""
        try {
            list = withContext(Dispatchers.IO) { PembimbingApiClient.service.getPembimbing() }
        } catch (e: Exception) {
            errorMsg = "Gagal memuat daftar pembimbing -- periksa koneksi internet"
        }
        loading = false
    }

    LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Button(onClick = onBack, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F7A5A)), shape = RoundedCornerShape(8.dp)) { Text("← Kembali") }
            Spacer(Modifier.height(12.dp))
            Text("Pembimbing Umrah", fontWeight = FontWeight.Bold, fontSize = 22.sp)
            Text("Ustadz & ustadzah yang mendampingi rombongan -- ketuk untuk lihat profil & materi ceramah", fontSize = 12.sp, color = Color.Gray)
            Spacer(Modifier.height(4.dp))
            if (loading) LinearProgressIndicator(Modifier.fillMaxWidth())
            if (errorMsg.isNotEmpty()) Text(errorMsg, fontSize = 12.sp, color = Color(0xFFDC2626))
            if (!loading && errorMsg.isEmpty() && list.isEmpty()) {
                Text("Belum ada pembimbing yang diisi Admin.", fontSize = 12.sp, color = Color.Gray)
            }
        }
        itemsIndexed(list) { index, p ->
            Card(
                shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(1.dp),
                modifier = Modifier.fillMaxWidth().clickable { onPembimbingClick(p) }
            ) {
                Column(Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.Top) {
                        Box(
                            modifier = Modifier.size(56.dp).background(Color(0xFF0F7A5A), RoundedCornerShape(14.dp)),
                            contentAlignment = Alignment.Center
                        ) { Text(initialsFromName(p.nama), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp) }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(p.nama, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF0F7A5A))
                            if (p.jabatan.isNotBlank()) Text(p.jabatan, fontSize = 12.sp, color = Color.Gray)
                        }
                        if (p.pengalaman_tahun != null && p.pengalaman_tahun > 0) {
                            Box(modifier = Modifier.background(Color.Black, RoundedCornerShape(20.dp)).padding(horizontal = 10.dp, vertical = 6.dp)) {
                                Text("${p.pengalaman_tahun} tahun", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    if (p.spesialisasi.isNotBlank()) {
                        Spacer(Modifier.height(10.dp))
                        Box(modifier = Modifier.background(Color(0xFFE6F4EE), RoundedCornerShape(20.dp)).padding(horizontal = 12.dp, vertical = 6.dp)) {
                            Text(p.spesialisasi, color = Color(0xFF0F7A5A), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    if (p.deskripsi.isNotBlank()) {
                        Spacer(Modifier.height(10.dp))
                        Text(p.deskripsi, fontSize = 13.sp, color = Color(0xFF374151))
                    }
                    Spacer(Modifier.height(12.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("ID • #${(index + 1).toString().padStart(2, '0')}", fontSize = 11.sp, color = Color.LightGray)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Lihat Profil", color = Color(0xFF0F7A5A), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Spacer(Modifier.width(6.dp))
                            Box(modifier = Modifier.size(28.dp).background(Color(0xFF0F7A5A), RoundedCornerShape(14.dp)), contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.ArrowForward, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }
        }
        item { Spacer(Modifier.height(8.dp)) }
    }
}

@Composable
fun PembimbingDetailScreen(pembimbing: Pembimbing, onBack: () -> Unit) {
    // FIX: sebelumnya 1 audio per pembimbing -- sekarang bisa BANYAK (minimal 5 slot
    // disediakan Admin), jadi ditampilkan sebagai daftar, bukan satu tombol. Slot yang
    // judul & URL-nya kosong (belum diisi Admin) tidak ditampilkan sama sekali.
    val audioItems = pembimbing.audio_list.filter { it.url.isNotBlank() }
    DisposableEffect(Unit) { onDispose { AudioPlayerManager.stop() } }

    LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            Button(onClick = onBack, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F7A5A)), shape = RoundedCornerShape(8.dp)) { Text("← Kembali") }
            Spacer(Modifier.height(12.dp))
            Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(2.dp), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier.size(84.dp).background(Color(0xFF0F7A5A), RoundedCornerShape(20.dp)),
                        contentAlignment = Alignment.Center
                    ) { Text(initialsFromName(pembimbing.nama), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 26.sp) }
                    Spacer(Modifier.height(12.dp))
                    Text(pembimbing.nama, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color(0xFF0F7A5A))
                    if (pembimbing.jabatan.isNotBlank()) Text(pembimbing.jabatan, fontSize = 13.sp, color = Color.Gray)
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (pembimbing.spesialisasi.isNotBlank()) {
                            Box(modifier = Modifier.background(Color(0xFFE6F4EE), RoundedCornerShape(20.dp)).padding(horizontal = 12.dp, vertical = 6.dp)) {
                                Text(pembimbing.spesialisasi, color = Color(0xFF0F7A5A), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        if (pembimbing.pengalaman_tahun != null && pembimbing.pengalaman_tahun > 0) {
                            Box(modifier = Modifier.background(Color.Black, RoundedCornerShape(20.dp)).padding(horizontal = 12.dp, vertical = 6.dp)) {
                                Text("${pembimbing.pengalaman_tahun} tahun pengalaman", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
        if (pembimbing.deskripsi.isNotBlank()) {
            item {
                Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(1.dp), modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Text("Tentang", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF0F7A5A))
                        Spacer(Modifier.height(6.dp))
                        Text(pembimbing.deskripsi, fontSize = 13.sp, color = Color(0xFF374151))
                    }
                }
            }
        }
        item {
            Text("Materi Ceramah", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF0F7A5A))
            Spacer(Modifier.height(4.dp))
            if (audioItems.isEmpty()) {
                Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(1.dp), modifier = Modifier.fillMaxWidth()) {
                    Text("Belum ada materi audio dari pembimbing ini.", fontSize = 12.sp, color = Color.Gray, modifier = Modifier.padding(16.dp))
                }
            }
        }
        items(audioItems) { audio ->
            val playing = AudioPlayerManager.currentlyPlayingUrl == audio.url
            Card(shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(1.dp), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { AudioPlayerManager.toggle(audio.url) }, modifier = Modifier.size(44.dp)) {
                            Icon(
                                if (playing) Icons.Default.Stop else Icons.Default.PlayCircle,
                                contentDescription = "Putar materi ceramah",
                                tint = Color(0xFF0F7A5A), modifier = Modifier.size(36.dp)
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        Column(Modifier.weight(1f)) {
                            Text(audio.judul.ifBlank { "Materi Ceramah" }, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF111827))
                            Text(if (playing) "Sedang memutar..." else "Ketuk untuk dengar", fontSize = 11.sp, color = if (playing) Color(0xFF0F7A5A) else Color.Gray)
                        }
                    }
                    if (AudioPlayerManager.isLoading && playing) { Spacer(Modifier.height(4.dp)); LinearProgressIndicator(Modifier.fillMaxWidth()) }
                    AudioPlayerManager.errorMessage?.let { if (playing) { Spacer(Modifier.height(4.dp)); Text(it, fontSize = 11.sp, color = Color(0xFFDC2626)) } }
                }
            }
        }
        item { Spacer(Modifier.height(8.dp)) }
    }
}
