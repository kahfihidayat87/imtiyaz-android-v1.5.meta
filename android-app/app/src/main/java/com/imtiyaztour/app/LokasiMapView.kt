package com.imtiyaztour.app

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.graphics.drawable.GradientDrawable
import android.graphics.Color as AndroidColor
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import com.google.firebase.firestore.FirebaseFirestore
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST

// ============================================================================
// PETA INLINE OSM v2.12.0 -- tampilkan semua jamaah di kanal TL.
// Marker: TL (default), jamaah yang dicari (merah via icon), jamaah lain (default).
// Auto-refresh tiap 30 detik. Ketuk marker = info nama + baterai + akurasi.
// ============================================================================

data class JamaahLocation(
    val jamaah_id: String,
    val nama: String,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val accuracy: Double = 0.0,
    val battery: Int = -1,
    val updated_at: Long = 0L,
    val source: String = "",
    val age_ms: Long = 0L,
)

interface KanalLokasiApi {
    @POST("api/kanal-lokasi")
    suspend fun getKanalLokasi(@Body body: Map<String, String>): List<JamaahLocation>
}

object KanalLokasiClient {
    val service: KanalLokasiApi by lazy {
        Retrofit.Builder()
            .baseUrl(ApiConfig.BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(KanalLokasiApi::class.java)
    }
}

@Composable
fun LokasiMapView(
    tlLat: Double?,
    tlLon: Double?,
    allJamaah: List<JamaahLocation>,
    highlightJamaahId: String?,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val mapView = remember {
        MapView(context).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            controller.setZoom(15.0)
        }
    }

    // Lifecycle: peta butuh onResume/onPause
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            mapView.onPause()
            mapView.onDetach()
        }
    }

    // Update markers saat data berubah
    LaunchedEffect(tlLat, tlLon, allJamaah, highlightJamaahId) {
        mapView.overlays.clear()
        val points = mutableListOf<GeoPoint>()

        // TL marker
        if (tlLat != null && tlLon != null) {
            val tlMarker = Marker(mapView).apply {
                position = GeoPoint(tlLat, tlLon)
                title = "Anda (Tour Leader)"
                snippet = "Posisi Anda saat ini"
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            }
            mapView.overlays.add(tlMarker)
            points.add(GeoPoint(tlLat, tlLon))
        }

        // Jamaah markers — hanya yang punya lokasi (skip null)
        // v2.13.0 FIX: sort supaya marker "highlight" (target yang dicari) di-add
        // paling akhir -> muncul paling atas saat marker bertumpuk.
        val sortedJamaah = allJamaah.sortedBy { it.jamaah_id == highlightJamaahId }
        sortedJamaah.forEach { j ->
            val lat = j.latitude ?: return@forEach
            val lon = j.longitude ?: return@forEach
            // v2.13.0 TAHAP 22: warna marker sesuai source + umur data
            val srcColor = markerColor(j.source, j.age_ms)
            val ageText = ageLabel(j.age_ms)
            val marker = Marker(mapView).apply {
                position = GeoPoint(lat, lon)
                title = j.nama
                val batText = if (j.battery >= 0) "Baterai ${j.battery}%" else "Baterai -"
                val accText = if (j.accuracy > 0) "\u00b1${j.accuracy.toInt()}m" else "-"
                val status = if (j.jamaah_id == highlightJamaahId) "DICARI" else "Di kanal"
                snippet = "$status \u2022 $batText \u2022 Akurasi $accText \u2022 $ageText"
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                // Ganti icon default dengan bulatan berwarna
                icon = circleDrawable(srcColor, if (j.jamaah_id == highlightJamaahId) 48 else 36)
            }
            mapView.overlays.add(marker)
            points.add(GeoPoint(lat, lon))
        }

        // Center peta ke rata-rata titik
        if (points.isNotEmpty()) {
            val avgLat = points.sumOf { it.latitude } / points.size
            val avgLon = points.sumOf { it.longitude } / points.size
            mapView.controller.setCenter(GeoPoint(avgLat, avgLon))
        }

        mapView.invalidate()
    }

    AndroidView(
        factory = { mapView },
        modifier = modifier,
    )
}

@Composable
fun LokasiMapCard(
    jamaahId: String,
    token: String,
    tlLat: Double?,
    tlLon: Double?,
    highlightJamaahId: String?,
    daftarJamaahKanal: Map<String, String> = emptyMap(),  // v2.13.0: id -> nama
    modifier: Modifier = Modifier,
) {
    var locations by remember { mutableStateOf<List<JamaahLocation>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }

    // v2.13.0: baca dari Firestore real-time (konsisten dengan tombol Google Maps).
    // Sebelumnya pakai /api/kanal-lokasi yang di-update via ntfy -- sekarang deprecated.
    DisposableEffect(daftarJamaahKanal) {
        val registration = FirebaseFirestore.getInstance()
            .collection("lokasi_jamaah")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    loading = false
                    return@addSnapshotListener
                }
                val list = snapshot?.documents?.mapNotNull { doc ->
                    val data = doc.data ?: return@mapNotNull null
                    val jid = doc.id
                    // Filter: hanya tampilkan jamaah yang ada di kanal TL ini
                    if (!daftarJamaahKanal.containsKey(jid)) return@mapNotNull null
                    JamaahLocation(
                        jamaah_id = jid,
                        nama = daftarJamaahKanal[jid] ?: "Jamaah",
                        latitude = data["latitude"] as? Double,
                        longitude = data["longitude"] as? Double,
                        accuracy = (data["accuracy"] as? Double) ?: 0.0,
                        battery = (data["battery"] as? Long)?.toInt() ?: -1,
                        updated_at = (data["updated_at"] as? Long) ?: 0L,
                        source = data["source"] as? String ?: "",
                        age_ms = (data["age_ms"] as? Long) ?: 0L,
                    )
                } ?: emptyList()
                locations = list
                loading = false
            }
        onDispose { registration.remove() }
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp),
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Peta Rombongan",
                    fontWeight = FontWeight.Bold, fontSize = 14.sp,
                    color = Color(0xFF0F7A5A))
                Spacer(Modifier.weight(1f))
                if (loading) {
                    CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                } else {
                    Text("${locations.size} jamaah", fontSize = 11.sp, color = Color.Gray)
                }
            }
            Spacer(Modifier.height(8.dp))

            LokasiMapView(
                tlLat = tlLat,
                tlLon = tlLon,
                allJamaah = locations,
                highlightJamaahId = highlightJamaahId,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(280.dp),
            )

            Spacer(Modifier.height(6.dp))
            Text(
                "Real-time \u2022 Ketuk marker untuk info",
                fontSize = 10.sp,
                color = Color.Gray,
            )
        }
    }
}

// ============================================================================
// v2.13.0 TAHAP 22: Helper warna marker + label umur data
// ============================================================================

/**
 * Warna marker sesuai source + umur data.
 *  - gps_akurat + < 2 menit  = hijau  (#0F7A5A)
 *  - cache + < 10 menit       = kuning (#F59E0B)
 *  - mode_aman_service < 30 m = abu    (#6B7280)
 *  - stale (> 30 menit)       = merah  (#DC2626)
 */
private fun markerColor(source: String, ageMs: Long): Int {
    val stale = ageMs > 30 * 60 * 1000L
    return when {
        stale -> 0xFFDC2626.toInt()
        source == "gps_akurat" -> 0xFF0F7A5A.toInt()
        source == "cache" -> 0xFFF59E0B.toInt()
        source == "mode_aman_service" -> 0xFF6B7280.toInt()
        else -> 0xFF9CA3AF.toInt()
    }
}

private fun ageLabel(ageMs: Long): String = when {
    ageMs < 1_000 -> "baru"
    ageMs < 60_000 -> "${ageMs / 1_000}s lalu"
    ageMs < 3_600_000 -> "${ageMs / 60_000}m lalu"
    else -> "${ageMs / 3_600_000}j lalu"
}

private fun circleDrawable(colorInt: Int, sizePx: Int): GradientDrawable {
    return GradientDrawable().apply {
        shape = GradientDrawable.OVAL
        setColor(colorInt)
        setSize(sizePx, sizePx)
        setStroke(4, AndroidColor.WHITE)
    }
}

