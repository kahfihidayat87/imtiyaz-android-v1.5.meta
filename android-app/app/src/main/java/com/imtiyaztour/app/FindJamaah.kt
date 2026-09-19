package com.imtiyaztour.app

import android.content.Intent
import android.net.Uri
import android.location.Location
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import kotlin.coroutines.resume
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

// ============================================================================
// FIND JAMAAH -- UI untuk Tour Leader.
// Daftar jamaah di kanal TL -> tombol "Cari" per jamaah -> polling status
// -> tampil koordinat + jarak + tombol Buka Peta.
// ============================================================================

data class KanalJamaah(val id: String, val nama: String)
data class FindRequest(val jamaah_id: String, val token: String, val target_jamaah_id: String)
data class FindResponse(val request_id: String? = null, val status: String? = null, val error: String? = null)
data class FindStatus(
    val status: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val accuracy: Double? = null,
    val battery: Int? = null,
    val requested_at: Long? = null,
    val responded_at: Long? = null,
    val error: String? = null
)

interface FindApiService {
    @POST("api/kanal-jamaah")
    suspend fun getKanalJamaah(@Body body: Map<String, String>): List<KanalJamaah>

    @POST("api/find")
    suspend fun requestFind(@Body body: FindRequest): FindResponse

    @GET("api/find/{id}/status")
    suspend fun getFindStatus(@Path("id") id: String): FindStatus
}

object FindApiClient {
    val service: FindApiService by lazy {
        Retrofit.Builder()
            .baseUrl(ApiConfig.BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(FindApiService::class.java)
    }
}

@Suppress("MissingPermission")
private suspend fun getTLLocation(context: android.content.Context): Location? = withTimeoutOrNull(10_000) {
    suspendCancellableCoroutine { cont ->
        val c = LocationServices.getFusedLocationProviderClient(context)
        val cts = com.google.android.gms.tasks.CancellationTokenSource()
        c.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, cts.token)
            .addOnSuccessListener { if (cont.isActive) cont.resume(it) }
            .addOnFailureListener { if (cont.isActive) cont.resume(null) }
        cont.invokeOnCancellation { cts.cancel() }
    }
}

private fun hitungJarak(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    val r = 6371000.0
    val p1 = Math.toRadians(lat1); val p2 = Math.toRadians(lat2)
    val dp = Math.toRadians(lat2 - lat1); val dl = Math.toRadians(lon2 - lon1)
    val a = sin(dp/2).pow(2) + cos(p1)*cos(p2)*sin(dl/2).pow(2)
    return r * 2 * atan2(sqrt(a), sqrt(1-a))
}

private fun formatJarak(m: Double): String = when {
    m < 1000 -> "${m.toInt()} meter"
    else -> String.format("%.1f km", m/1000)
}

@Composable
fun FindJamaahScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val jamaahId = Prefs.getJamaahId(context)
    val token = Prefs.getToken(context)

    var list by remember { mutableStateOf<List<KanalJamaah>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var errorMsg by remember { mutableStateOf("") }
    var activeFind by remember { mutableStateOf<Pair<KanalJamaah, String>?>(null) }
    var findResult by remember { mutableStateOf<FindStatus?>(null) }
    var tlLocation by remember { mutableStateOf<Location?>(null) }

    LaunchedEffect(Unit) {
        loading = true; errorMsg = ""
        try {
            list = withContext(Dispatchers.IO) {
                FindApiClient.service.getKanalJamaah(mapOf("jamaah_id" to jamaahId, "token" to token))
            }
            tlLocation = getTLLocation(context)
        } catch (e: Exception) {
            errorMsg = "Gagal memuat daftar jamaah -- pastikan Anda Tour Leader dan koneksi stabil"
        }
        loading = false
    }

    LaunchedEffect(activeFind?.second) {
        val reqId = activeFind?.second ?: return@LaunchedEffect
        val start = System.currentTimeMillis()
        while (System.currentTimeMillis() - start < 90_000) {
            try {
                val s = withContext(Dispatchers.IO) { FindApiClient.service.getFindStatus(reqId) }
                if (s.status == "responded") { findResult = s; return@LaunchedEffect }
            } catch (e: Exception) { }
            delay(2000)
        }
        findResult = FindStatus(status = "timeout")
    }

    if (activeFind != null) {
        val (target, _) = activeFind!!
        val r = findResult
        Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            TextButton(onClick = { activeFind = null; findResult = null }) { Text("<- Kembali ke daftar") }
            Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(2.dp), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Mencari Lokasi", fontSize = 12.sp, color = Color.Gray)
                    Text(target.nama, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color(0xFF0F7A5A))
                    Spacer(Modifier.height(16.dp))
                    if (r == null || r.status == "sent" || r.status == "pending") {
                        CircularProgressIndicator(color = Color(0xFF0F7A5A))
                        Spacer(Modifier.height(8.dp))
                        Text("Menunggu HP merespons...", fontSize = 12.sp, color = Color.Gray)
                        Text("Biasanya 15-30 detik", fontSize = 10.sp, color = Color.LightGray)
                    } else if (r.status == "responded") {
                        val lat = r.latitude ?: 0.0; val lon = r.longitude ?: 0.0
                        Text("Lokasi ditemukan", fontWeight = FontWeight.Bold, color = Color(0xFF0F7A5A))
                        Spacer(Modifier.height(12.dp))
                        val tl = tlLocation
                        if (tl != null) {
                            val jarak = hitungJarak(tl.latitude, tl.longitude, lat, lon)
                            Text("Jarak dari Anda: ${formatJarak(jarak)}", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Spacer(Modifier.height(8.dp))
                        }
                        Text("Koordinat: ${"%.5f".format(lat)}, ${"%.5f".format(lon)}", fontSize = 12.sp)
                        Text("Akurasi: ±${r.accuracy?.toInt() ?: 0} meter", fontSize = 11.sp, color = Color.Gray)
                        Text("Baterai: ${r.battery ?: 0}%", fontSize = 11.sp, color = Color.Gray)
                        Spacer(Modifier.height(16.dp))
                        Button(
                            onClick = {
                                val uri = Uri.parse("geo:$lat,$lon?q=$lat,$lon(${Uri.encode(target.nama)})")
                                context.startActivity(Intent(Intent.ACTION_VIEW, uri))
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F7A5A))
                        ) { Text("Buka di Peta") }
                        Spacer(Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = { activeFind = null; findResult = null },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("Cari Jamaah Lain") }
                    } else {
                        Text("HP tidak merespons", fontWeight = FontWeight.Bold, color = Color(0xFFDC2626))
                        Text("Jamaah mungkin tidak membawa HP, HP mati, atau offline.", fontSize = 12.sp, color = Color.Gray)
                        Spacer(Modifier.height(12.dp))
                        OutlinedButton(onClick = { activeFind = null; findResult = null }, modifier = Modifier.fillMaxWidth()) { Text("Coba Jamaah Lain") }
                    }
                }
            }
        }
        return
    }

    LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            Button(onClick = onBack, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F7A5A)), shape = RoundedCornerShape(8.dp)) { Text("<- Kembali") }
            Spacer(Modifier.height(12.dp))
            Text("Cari Lokasi Jamaah", fontWeight = FontWeight.Bold, fontSize = 22.sp)
            Text("Ketuk tombol cari untuk minta lokasi terkini dari HP jamaah.", fontSize = 12.sp, color = Color.Gray)
            Spacer(Modifier.height(8.dp))
            if (loading) LinearProgressIndicator(Modifier.fillMaxWidth())
            if (errorMsg.isNotEmpty()) { Text(errorMsg, fontSize = 12.sp, color = Color(0xFFDC2626)); Spacer(Modifier.height(8.dp)) }
        }
        items(list) { j ->
            Card(shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(1.dp), modifier = Modifier.fillMaxWidth()) {
                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Person, contentDescription = null, tint = Color(0xFF0F7A5A))
                    Spacer(Modifier.width(12.dp))
                    Text(j.nama, fontWeight = FontWeight.Bold, fontSize = 14.sp, modifier = Modifier.weight(1f))
                    Button(
                        onClick = {
                            scope.launch {
                                try {
                                    val resp = withContext(Dispatchers.IO) {
                                        FindApiClient.service.requestFind(FindRequest(jamaahId, token, j.id))
                                    }
                                    if (resp.request_id != null) {
                                        activeFind = j to resp.request_id
                                        findResult = null
                                    } else {
                                        errorMsg = resp.error ?: "Gagal meminta lokasi"
                                    }
                                } catch (e: Exception) {
                                    errorMsg = "Gagal mengirim permintaan: ${e.message}"
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F7A5A)),
                        shape = RoundedCornerShape(8.dp)
                    ) { Text("Cari", fontSize = 12.sp) }
                }
            }
        }
        if (!loading && list.isEmpty() && errorMsg.isEmpty()) {
            item {
                Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3CD)), shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
                    Text("Belum ada jamaah di kanal Anda. Minta Admin menugaskan jamaah ke kanal radio Anda terlebih dahulu.", fontSize = 11.sp, color = Color(0xFF92400E), modifier = Modifier.padding(12.dp))
                }
            }
        }
    }
}
