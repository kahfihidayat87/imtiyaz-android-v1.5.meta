package com.imtiyaztour.app

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
// JADWAL KEBERANGKATAN -- data dari WP Travel Engine (custom post type "trip").
// Tanggal + harga LIVE per trip, murni untuk jamaah/calon jamaah MELIHAT info.
//
// v2.13.0: dipindah dari Beranda ke tab "Jadwal" (sebelumnya "Paket").
// Dipecah jadi:
//  - JadwalKeberangkatanSection : inline di dalam LazyColumn parent (bukan LazyColumn)
//  - JadwalKeberangkatanScreen  : fullscreen wrapper (backward compat, kalau dipanggil terpisah)
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

/**
 * v2.13.0: Section inline — pakai Column biasa, BUKAN LazyColumn.
 * Aman dipanggil di dalam LazyColumn parent (tab Jadwal / Paket).
 */
@Composable
fun JadwalKeberangkatanSection(
    showHeader: Boolean = true,
    modifier: Modifier = Modifier
) {
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

    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (showHeader) {
            Text("Jadwal Keberangkatan Terbaru", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Text(
                "Tanggal & harga langsung dari sistem -- jadwal yang sudah lewat otomatis tidak tampil",
                fontSize = 11.sp, color = Color.Gray
            )
            Spacer(Modifier.height(4.dp))
        }

        if (loading) LinearProgressIndicator(Modifier.fillMaxWidth())
        if (errorMsg.isNotEmpty()) {
            Text(errorMsg, fontSize = 12.sp, color = Color(0xFFDC2626))
        }
        if (!loading && errorMsg.isEmpty() && jadwal.isEmpty()) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3CD)),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    "Belum ada jadwal keberangkatan baru yang akan datang. Hubungi kami untuk info terbaru.",
                    fontSize = 11.sp, color = Color(0xFF92400E),
                    modifier = Modifier.padding(12.dp)
                )
            }
        }

        jadwal.forEach { j -> JadwalCard(j) }
    }
}

@Composable
fun JadwalCard(j: JadwalKeberangkatan) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(
                modifier = Modifier
                    .width(64.dp)
                    .background(Color(0xFFF0FDF4), RoundedCornerShape(8.dp))
                    .padding(vertical = 8.dp),
            ) {
                Text(
                    j.tanggal, fontSize = 10.sp, fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F7A5A), textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(j.nama_paket, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text(
                    (j.durasi_hari?.let { "$it hari" } ?: "-"),
                    fontSize = 11.sp, color = Color.Gray
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (!j.harga_asli_tampilan.isNullOrBlank()) {
                        Text(
                            j.harga_asli_tampilan, fontSize = 10.sp, color = Color.LightGray,
                            textDecoration = TextDecoration.LineThrough
                        )
                        Spacer(Modifier.width(6.dp))
                    }
                    Text(
                        j.harga_tampilan, fontSize = 13.sp,
                        fontWeight = FontWeight.Bold, color = Color(0xFF0F7A5A)
                    )
                }
            }
        }
    }
}

/**
 * Fullscreen wrapper (backward compat). Tidak dipanggil lagi dari Beranda
 * setelah v2.13.0, tapi tetap ada kalau perlu dibuka terpisah dari suatu tempat.
 */
@Composable
fun JadwalKeberangkatanScreen(onBack: () -> Unit) {
    LazyColumn(
        Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Button(
                onClick = onBack,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F7A5A)),
                shape = RoundedCornerShape(8.dp)
            ) { Text("← Kembali") }
            Spacer(Modifier.height(12.dp))
        }
        item {
            JadwalKeberangkatanSection(showHeader = true)
        }
    }
}
