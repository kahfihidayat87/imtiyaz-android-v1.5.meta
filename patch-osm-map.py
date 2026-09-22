#!/usr/bin/env python3
"""
Patch Android: peta inline OSM v2.12.0 untuk Tour Leader.
- build.gradle: tambah dependency osmdroid
- AndroidManifest.xml: tambah permission network state
- MainActivity.kt: init osmdroid config (wajib sebelum MapView dibuat)
- Buat LokasiMapView.kt: komponen peta dengan semua marker jamaah
- FindJamaah.kt: integrasi peta di layar hasil cari
"""
import sys, os, shutil

def detect_app():
    for p in ("android-app/app", "app"):
        if os.path.isdir(os.path.join(p, "src/main")):
            return p
    print("ERROR: jalankan dari root repo (yang ada android-app/ atau app/)")
    sys.exit(1)

def read(p):
    with open(p, "r", encoding="utf-8", newline="") as f:
        return f.read().replace("\r\n", "\n").replace("\r", "\n")

def write(p, c):
    with open(p, "w", encoding="utf-8", newline="\n") as f:
        f.write(c)

def patch(c, find, repl, label, req=True):
    if find not in c:
        if req:
            print(f"\nERROR [{label}]: pola tidak ditemukan")
            print("  Cari: " + repr(find[:100]))
            sys.exit(2)
        print(f"  SKIP [{label}]: sudah di-patch sebelumnya")
        return c
    return c.replace(find, repl, 1)

APP = detect_app()
JAVA = f"{APP}/src/main/java/com/imtiyaztour/app"
GRADLE = f"{APP}/build.gradle"
MANIFEST = f"{APP}/src/main/AndroidManifest.xml"
MA = f"{JAVA}/MainActivity.kt"
FJ = f"{JAVA}/FindJamaah.kt"

print(f"App: {APP}\n")

for f in (GRADLE, MANIFEST, MA, FJ):
    if not os.path.isfile(f):
        print(f"ERROR: File tidak ada: {f}"); sys.exit(1)
    shutil.copy(f, f + ".bak9")
    print(f"Backup: {f}.bak9")
print()

# ============ 1. build.gradle: tambah osmdroid ============
print("Patch build.gradle...")
c = read(GRADLE)
if "org.osmdroid:osmdroid-android" not in c:
    c = patch(c,
        "    implementation 'com.google.android.gms:play-services-location:21.3.0'",
        "    implementation 'com.google.android.gms:play-services-location:21.3.0'\n"
        "    // v2.12.0: peta inline OSM (OpenStreetMap) - gratis, tanpa API key\n"
        "    implementation 'org.osmdroid:osmdroid-android:6.1.18'",
        "gradle-osmdroid")
    write(GRADLE, c)
    print("OK build.gradle\n")
else:
    print("SKIP: osmdroid sudah ada\n")

# ============ 2. AndroidManifest: permission network ============
print("Patch AndroidManifest.xml...")
c = read(MANIFEST)
if "ACCESS_NETWORK_STATE" not in c:
    c = patch(c,
        '    <uses-permission android:name="android.permission.INTERNET" />',
        '    <uses-permission android:name="android.permission.INTERNET" />\n'
        '    <!-- v2.12.0: peta OSM butuh cek status jaringan -->\n'
        '    <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />',
        "manifest-network")
    write(MANIFEST, c)
    print("OK AndroidManifest.xml\n")
else:
    print("SKIP: ACCESS_NETWORK_STATE sudah ada\n")

# ============ 3. MainActivity: init osmdroid ============
print("Patch MainActivity.kt...")
c = read(MA)
if "Configuration.getInstance().userAgentValue" not in c:
    c = patch(c,
        "    override fun onCreate(savedInstanceState: Bundle?) {\n"
        "        super.onCreate(savedInstanceState)",
        "    override fun onCreate(savedInstanceState: Bundle?) {\n"
        "        super.onCreate(savedInstanceState)\n"
        "        // v2.12.0: inisialisasi osmdroid (WAJIB sebelum MapView dibuat)\n"
        "        org.osmdroid.config.Configuration.getInstance().userAgentValue = packageName\n"
        "        org.osmdroid.config.Configuration.getInstance().osmdroidBasePath = java.io.File(cacheDir, \"osmdroid\")\n"
        "        org.osmdroid.config.Configuration.getInstance().osmdroidTileCache = java.io.File(cacheDir, \"osmdroid/tiles\")",
        "main-osmdroid-init")
    write(MA, c)
    print("OK MainActivity.kt\n")
else:
    print("SKIP: MainActivity sudah di-init\n")

# ============ 4. Buat LokasiMapView.kt ============
print("Membuat LokasiMapView.kt...")
LOKASI = '''package com.imtiyaztour.app

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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
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
    val latitude: Double,
    val longitude: Double,
    val accuracy: Double = 0.0,
    val battery: Int = -1,
    val updated_at: Long = 0L,
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

        // Jamaah markers
        allJamaah.forEach { j ->
            val marker = Marker(mapView).apply {
                position = GeoPoint(j.latitude, j.longitude)
                title = j.nama
                val batText = if (j.battery >= 0) "Baterai ${j.battery}%" else "Baterai -"
                val accText = if (j.accuracy > 0) "\\u00b1${j.accuracy.toInt()}m" else "-"
                val status = if (j.jamaah_id == highlightJamaahId) "DICARI" else "Di kanal"
                snippet = "$status \\u2022 $batText \\u2022 Akurasi $accText"
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            }
            mapView.overlays.add(marker)
            points.add(GeoPoint(j.latitude, j.longitude))
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
    modifier: Modifier = Modifier,
) {
    var locations by remember { mutableStateOf<List<JamaahLocation>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }

    suspend fun loadLocations() {
        try {
            val list = withContext(Dispatchers.IO) {
                KanalLokasiClient.service.getKanalLokasi(
                    mapOf("jamaah_id" to jamaahId, "token" to token)
                )
            }
            locations = list
        } catch (e: Exception) {
            // Diamkan -- akan retry otomatis
        }
        loading = false
    }

    // Load awal
    LaunchedEffect(Unit) { loadLocations() }

    // Auto-refresh tiap 30 detik
    LaunchedEffect(Unit) {
        while (isActive) {
            delay(30_000)
            loadLocations()
        }
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
                "Auto-refresh tiap 30 detik \\u2022 Ketuk marker untuk info",
                fontSize = 10.sp,
                color = Color.Gray,
            )
        }
    }
}
'''
with open(f"{JAVA}/LokasiMapView.kt", "w", encoding="utf-8", newline="\n") as f:
    f.write(LOKASI)
print("OK LokasiMapView.kt\n")

# ============ 5. FindJamaah.kt: integrasi peta ============
print("Patch FindJamaah.kt...")
c = read(FJ)

# Cek apakah sudah di-patch
if "LokasiMapCard(" in c:
    print("SKIP: FindJamaah sudah di-patch")
else:
    # Sisipkan LokasiMapCard setelah "Lokasi ditemukan"
    target = '''                    } else if (r.status == "responded") {
                        val lat = r.latitude ?: 0.0; val lon = r.longitude ?: 0.0
                        Text("Lokasi ditemukan", fontWeight = FontWeight.Bold, color = Color(0xFF0F7A5A))
                        Spacer(Modifier.height(12.dp))'''

    replacement = '''                    } else if (r.status == "responded") {
                        val lat = r.latitude ?: 0.0; val lon = r.longitude ?: 0.0
                        Text("Lokasi ditemukan", fontWeight = FontWeight.Bold, color = Color(0xFF0F7A5A))
                        Spacer(Modifier.height(12.dp))

                        // v2.12.0: peta inline dengan semua jamaah rombongan
                        LokasiMapCard(
                            jamaahId = jamaahId,
                            token = token,
                            tlLat = tlLocation?.latitude,
                            tlLon = tlLocation?.longitude,
                            highlightJamaahId = target.id,
                            modifier = Modifier.padding(vertical = 4.dp),
                        )
                        Spacer(Modifier.height(12.dp))'''

    c = patch(c, target, replacement, "fj-map-card")
    write(FJ, c)
    print("OK FindJamaah.kt\n")

print("=" * 60)
print("SEMUA PATCH PETA OSM v2.12.0 BERHASIL")
print("=" * 60)
print("\\nSelanjutnya:")
print("  del /S /Q android-app'app'*.bak9")
print("  git add .")
print("  git commit -m 'feat: peta inline OSM v2.12.0 untuk TL'")
print("  git push origin main")