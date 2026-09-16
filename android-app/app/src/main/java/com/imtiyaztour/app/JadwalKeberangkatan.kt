package com.imtiyaztour.app

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET

// ============================================================================
// JADWAL KEBERANGKATAN -- diintegrasikan dari plugin imtiyaz-jadwal-sync.php yang
// Anda kirim (dipakai untuk shortcode website). Sumber datanya WP Travel Engine
// (custom post type "trip") yang berisi tanggal keberangkatan RIIL dan harga
// LIVE (termasuk diskon) -- berbeda dari tab "Paket Umrah" yang isinya kategori
// paket generik (Ekonomis/Reguler/Premium) yang diisi manual oleh Admin.
//
// Keduanya sengaja dibiarkan terpisah, bukan saling mengganti:
// - "Paket Umrah" = kategori/tingkatan paket, jadi acuan pendaftaran jamaah
//   (paket_id jamaah, kanal radio, dsb bergantung ke sini -- jangan diubah).
// - "Jadwal Keberangkatan" (halaman ini) = tanggal riil & harga LIVE per trip,
//   murni untuk jamaah/calon jamaah MELIHAT info terkini sebelum mendaftar.
// ============================================================================

data class JadwalKeberangkatan(
    val id: Int,
    val tanggal: String,
    val timestamp: Long,
    val nama_paket: String,
    val durasi_hari: Int? = null,
    val harga_tampilan: String,
    val harga_asli_tampilan: String? = null,
    val harga_angka: Double? = null,
    val url: String? = null
)

interface JadwalApiService {
    @GET("api/jadwal-keberangkatan")
    suspend fun getJadwal(): List<JadwalKeberangkatan>
}

object JadwalApiClient {
    val service: JadwalApiService by lazy {
        Retrofit.Builder()
            .baseUrl(ApiConfig.BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(JadwalApiService::class.java)
    }
}

@Composable
fun JadwalKeberangkatanScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var jadwal by remember { mutableStateOf<List<JadwalKeberangkatan>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var errorMsg by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        loading = true; errorMsg = ""
        try {
            jadwal = withContext(Dispatchers.IO) { JadwalApiClient.service.getJadwal() }
        } catch (e: Exception) {
            errorMsg = "Gagal memuat jadwal keberangkatan -- periksa koneksi internet"
        }
        loading = false
    }

    LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            Button(onClick = onBack, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F7A5A)), shape = RoundedCornerShape(8.dp)) { Text("← Kembali") }
            Spacer(Modifier.height(12.dp))
            Text("Jadwal Keberangkatan Terbaru", fontWeight = FontWeight.Bold, fontSize = 20.sp)
            Text("Tanggal & harga langsung dari sistem -- jadwal yang sudah lewat otomatis tidak tampil", fontSize = 11.sp, color = Color.Gray)
            Spacer(Modifier.height(8.dp))
            if (loading) { LinearProgressIndicator(Modifier.fillMaxWidth()) }
            if (errorMsg.isNotEmpty()) { Text(errorMsg, fontSize = 12.sp, color = Color(0xFFDC2626)) }
            if (!loading && errorMsg.isEmpty() && jadwal.isEmpty()) {
                Text("Belum ada jadwal keberangkatan baru yang akan datang. Hubungi kami untuk info terbaru.", fontSize = 12.sp, color = Color.Gray)
            }
        }
        items(jadwal) { j ->
            Card(shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(1.dp), modifier = Modifier.fillMaxWidth()) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(
                        modifier = Modifier
                            .width(64.dp)
                            .background(Color(0xFFF0FDF4), RoundedCornerShape(8.dp))
                            .padding(vertical = 8.dp),
                    ) { Text(j.tanggal, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F7A5A), textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(j.nama_paket, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text(
                            (j.durasi_hari?.let { "$it hari" } ?: "-"),
                            fontSize = 11.sp, color = Color.Gray
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (!j.harga_asli_tampilan.isNullOrBlank()) {
                                Text(j.harga_asli_tampilan, fontSize = 10.sp, color = Color.LightGray, textDecoration = TextDecoration.LineThrough)
                                Spacer(Modifier.width(6.dp))
                            }
                            Text(j.harga_tampilan, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F7A5A))
                        }
                    }
                }
            }
        }
    }
}
