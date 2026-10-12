package com.imtiyaztour.app

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * v2.13.0: Screen detail fasilitas trip (Termasuk / Tidak Termasuk).
 * Fetch via /api/trip-fasilitas/{id}, cache 1 jam di server (WP).
 */
@Composable
fun TripFasilitasScreen(tripId: Int, onBack: () -> Unit) {
    val context = LocalContext.current
    var data by remember { mutableStateOf<TripFasilitasResponse?>(null) }
    var loading by remember { mutableStateOf(true) }
    var errorMsg by remember { mutableStateOf("") }

    LaunchedEffect(tripId) {
        loading = true; errorMsg = ""
        try {
            data = withContext(Dispatchers.IO) { JadwalApiClient.service.getTripFasilitas(tripId) }
        } catch (e: Exception) {
            errorMsg = "Gagal memuat fasilitas -- periksa koneksi internet"
        }
        loading = false
    }

    LazyColumn(
        Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Button(
                onClick = onBack,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F7A5A)),
                shape = RoundedCornerShape(8.dp)
            ) { Text("\u2190 Kembali") }
            Spacer(Modifier.height(12.dp))
        }

        if (loading) {
            item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
        } else if (errorMsg.isNotEmpty()) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFEE2E2)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        errorMsg, fontSize = 12.sp, color = Color(0xFF991B1B),
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }
        } else {
            val d = data
            if (d != null) {
                // === HEADER ===
                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F7A5A)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(Modifier.padding(20.dp)) {
                            Text(
                                d.nama_paket, color = Color.White,
                                fontWeight = FontWeight.Bold, fontSize = 20.sp
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(
                                "\uD83D\uDCC5 " + d.tanggal +
                                    (d.durasi_hari?.let { "  \u2022  $it hari" } ?: ""),
                                color = Color(0xFFD1FAE5), fontSize = 12.sp
                            )
                            Spacer(Modifier.height(8.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (!d.harga_asli_tampilan.isNullOrBlank()) {
                                    Text(
                                        d.harga_asli_tampilan,
                                        color = Color.White.copy(alpha = 0.6f),
                                        fontSize = 12.sp,
                                        textDecoration = TextDecoration.LineThrough
                                    )
                                    Spacer(Modifier.width(8.dp))
                                }
                                Text(
                                    d.harga_tampilan, color = Color(0xFFFFD700),
                                    fontWeight = FontWeight.Bold, fontSize = 16.sp
                                )
                            }
                        }
                    }
                }

                // === TERMASUK ===
                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(1.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            Text(
                                "Fasilitas", fontWeight = FontWeight.Bold,
                                fontSize = 18.sp, color = Color(0xFF0F7A5A)
                            )
                            Spacer(Modifier.height(12.dp))
                            Text("Termasuk", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Spacer(Modifier.height(8.dp))
                            if (d.termasuk.isEmpty()) {
                                Text("(data belum tersedia)", fontSize = 12.sp, color = Color.Gray)
                            } else {
                                d.termasuk.forEach { item ->
                                    Row(
                                        Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                        verticalAlignment = Alignment.Top
                                    ) {
                                        Text("\u2705", fontSize = 13.sp)
                                        Spacer(Modifier.width(8.dp))
                                        Text(item, fontSize = 13.sp, color = Color(0xFF374151))
                                    }
                                }
                            }
                        }
                    }
                }

                // === TIDAK TERMASUK ===
                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(1.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            Text(
                                "Tidak Termasuk", fontWeight = FontWeight.Bold,
                                fontSize = 14.sp, color = Color(0xFFDC2626)
                            )
                            Spacer(Modifier.height(8.dp))
                            if (d.tidak_termasuk.isEmpty()) {
                                Text("(data belum tersedia)", fontSize = 12.sp, color = Color.Gray)
                            } else {
                                d.tidak_termasuk.forEach { item ->
                                    Row(
                                        Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                        verticalAlignment = Alignment.Top
                                    ) {
                                        Text("\u274C", fontSize = 13.sp)
                                        Spacer(Modifier.width(8.dp))
                                        Text(item, fontSize = 13.sp, color = Color(0xFF374151))
                                    }
                                }
                            }
                        }
                    }
                }

                // === CTA WhatsApp ===
                item {
                    val pesan = "Assalamualaikum, saya tertarik paket ${d.nama_paket} " +
                        "keberangkatan ${d.tanggal} (${d.harga_tampilan}). " +
                        "Mohon informasi lebih lanjut."
                    Button(
                        onClick = {
                            val uri = Uri.parse(
                                "https://wa.me/" + AppData.wa.wa_admin + "?text=" +
                                    Uri.encode(pesan)
                            )
                            context.startActivity(Intent(Intent.ACTION_VIEW, uri))
                        },
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F7A5A)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Tanya / Daftar via WhatsApp", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
