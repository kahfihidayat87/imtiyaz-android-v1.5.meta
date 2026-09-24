package com.imtiyaztour.app

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Receipt
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
import retrofit2.http.Body
import retrofit2.http.POST
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// ============================================================================
// INVOICE JAMAAH v1.0 -- daftar invoice PDF milik jamaah yang login.
// Endpoint ada di Node.js (api.pastiumrah.com), bukan WordPress.
// ============================================================================

object InvoiceConfig {
    const val BASE_URL = "https://api.pastiumrah.com/"
}

data class InvoiceListItem(
    val invoice_number: String? = null,
    val invoice_date: Long = 0L,
    val amount_due: Long = 0L,
    val total: Long = 0L,
    val pdf_url: String? = null
)

interface InvoiceApiService {
    @POST("api/invoice-list")
    suspend fun listInvoice(@Body body: Map<String, String>): List<InvoiceListItem>
}

object InvoiceApiClient {
    val service: InvoiceApiService by lazy {
        Retrofit.Builder()
            .baseUrl(InvoiceConfig.BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(InvoiceApiService::class.java)
    }
}

private fun formatTanggalInvoice(ms: Long): String {
    if (ms <= 0L) return "-"
    return SimpleDateFormat("d MMM yyyy", Locale("id", "ID")).format(Date(ms))
}

@Composable
fun InvoiceListScreen(jamaahId: String, token: String, onBack: () -> Unit) {
    val context = LocalContext.current
    var list by remember { mutableStateOf<List<InvoiceListItem>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var errorMsg by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        loading = true; errorMsg = ""
        try {
            list = withContext(Dispatchers.IO) {
                InvoiceApiClient.service.listInvoice(mapOf("jamaah_id" to jamaahId, "token" to token))
            }
        } catch (e: Exception) {
            errorMsg = "Gagal memuat invoice -- periksa koneksi internet"
        }
        loading = false
    }

    LazyColumn(
        Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Button(
                onClick = onBack,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F7A5A)),
                shape = RoundedCornerShape(8.dp)
            ) { Text("<- Kembali") }
            Spacer(Modifier.height(12.dp))
            Text("Invoice Saya", fontWeight = FontWeight.Bold, fontSize = 22.sp)
            Text("Riwayat invoice PDF dari Admin Imtiyaz Tour", fontSize = 12.sp, color = Color.Gray)
            Spacer(Modifier.height(12.dp))

            if (loading) LinearProgressIndicator(Modifier.fillMaxWidth())
            if (errorMsg.isNotEmpty()) Text(errorMsg, fontSize = 12.sp, color = Color(0xFFDC2626))
        }

        if (!loading && errorMsg.isEmpty() && list.isEmpty()) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF0FDF4)),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.Receipt, contentDescription = null,
                            tint = Color(0xFF0F7A5A), modifier = Modifier.size(48.dp)
                        )
                        Spacer(Modifier.height(12.dp))
                        Text("Belum ada invoice", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF0F7A5A))
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Admin belum menerbitkan invoice untuk akun Anda. Hubungi Admin kalau sudah ada pembayaran.",
                            fontSize = 11.sp, color = Color.Gray,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            }
        }

        items(list) { inv ->
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(1.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        val url = inv.pdf_url ?: return@clickable
                        try {
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                        } catch (e: Exception) { }
                    }
            ) {
                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(42.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.PictureAsPdf,
                            contentDescription = null,
                            tint = Color(0xFFDC2626),
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            inv.invoice_number ?: "-",
                            fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF0F7A5A)
                        )
                        Text(
                            formatTanggalInvoice(inv.invoice_date),
                            fontSize = 11.sp, color = Color.Gray
                        )
                        Spacer(Modifier.height(4.dp))
                        Row {
                            Text(
                                "Total: ${formatRupiah(inv.total.toString())}",
                                fontSize = 11.sp, color = Color(0xFF374151)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "Sisa: ${formatRupiah(inv.amount_due.toString())}",
                                fontSize = 11.sp, fontWeight = FontWeight.Bold,
                                color = if (inv.amount_due > 0) Color(0xFFDC2626) else Color(0xFF0F7A5A)
                            )
                        }
                    }
                    Icon(
                        Icons.Default.PictureAsPdf,
                        contentDescription = "Buka PDF",
                        tint = Color(0xFF0F7A5A),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        item { Spacer(Modifier.height(8.dp)) }
    }
}
