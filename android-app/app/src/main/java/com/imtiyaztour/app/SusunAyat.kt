package com.imtiyaztour.app

import androidx.compose.foundation.background
import androidx.compose.ui.draw.alpha
import androidx.compose.foundation.border
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

// ============================================================================
// SUSUN AYAT (mode alternatif dari "Tebak Kata") -- SETIAP potongan adalah
// SATU AYAT UTUH (tidak pernah dipotong per kata/huruf), diseret dari tray ke
// slot urutan yang benar. Ini justru salah satu metode hafalan yang lazim
// dipakai (mengurutkan ayat yang sudah dikenal) - beda dengan mengacak teks
// di dalam satu ayat yang sudah kita hindari sebelumnya.
//
// Maks MAKS_POTONGAN ayat per ronde supaya tetap proporsional di layar HP -
// surah yang ayatnya lebih banyak dipecah jadi beberapa ronde berurutan,
// TIDAK memotong ayat itu sendiri, hanya membagi rondenya.
// ============================================================================

private const val MAKS_POTONGAN = 8

@Composable
fun SusunAyatScreen(nomorSurah: Int, onBack: () -> Unit) {
    var detail by remember { mutableStateOf<SurahDetail?>(null) }
    var loading by remember { mutableStateOf(true) }
    var errorMsg by remember { mutableStateOf("") }
    var rondeIndex by remember { mutableIntStateOf(0) }

    LaunchedEffect(nomorSurah) {
        try {
            val resp = withContext(Dispatchers.IO) { QuranApiClient.service.getSurah(nomorSurah) }
            detail = resp.data
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
                val semuaRonde = d.ayat.chunked(MAKS_POTONGAN)
                Text(d.namaLatin, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color(0xFF0F7A5A))
                Text(
                    if (semuaRonde.size > 1) "Susun Ayat - Ronde ${rondeIndex + 1} dari ${semuaRonde.size}" else "Susun Ayat",
                    fontSize = 11.sp, color = Color.Gray
                )
                Spacer(Modifier.height(12.dp))

                if (rondeIndex >= semuaRonde.size) {
                    SelesaiSurahCard(d.namaLatin, onBack)
                } else {
                    key(rondeIndex) {
                        RondeSusunAyat(
                            ronde = semuaRonde[rondeIndex],
                            onRondeSelesai = { rondeIndex++ }
                        )
                    }
                }
            }
        }
    }
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun ColumnScope.RondeSusunAyat(ronde: List<AyatItem>, onRondeSelesai: () -> Unit) {
    // slotIsi[i] = nomorAyat yang menempati slot urutan ke-i (index 0-based), null kalau kosong.
    val slotIsi = remember(ronde) { mutableStateListOf(*arrayOfNulls<Int>(ronde.size)) }
    val slotBounds = remember(ronde) { mutableStateMapOf<Int, Rect>() } // slot index -> posisi di layar
    val potonganTray = remember(ronde) { ronde.shuffled().map { it.nomorAyat }.toMutableStateList() }
    var selesaiDicek by remember(ronde) { mutableStateOf(false) }
    var semuaBenar by remember(ronde) { mutableStateOf(false) }

    // State untuk drag ghost overlay - agar item yang didrag muncul di depan semua konten
    var dragAktif by remember { mutableStateOf(false) }
    var dragTeks by remember { mutableStateOf("") }
    var dragWindow by remember { mutableStateOf(androidx.compose.ui.geometry.Offset.Zero) }
    var boxWindow by remember { mutableStateOf(androidx.compose.ui.geometry.Offset.Zero) }

    fun ayatOf(nomor: Int) = ronde.first { it.nomorAyat == nomor }

    Box(
        Modifier
            .weight(1f)
            .fillMaxWidth()
            .onGloballyPositioned { boxWindow = it.boundsInWindow().topLeft }
    ) {
    Column(Modifier.fillMaxSize()) {
        Text("Seret tiap ayat ke urutan yang benar:", fontSize = 11.5.sp, color = Color.Gray)
        Spacer(Modifier.height(10.dp))

        // SLOT URUTAN - target tempat ayat diletakkan, diberi nomor urut saja (bukan isi jawabannya).
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            ronde.indices.forEach { i ->
                val isi = slotIsi.getOrNull(i)
                Row(
                    Modifier
                        .fillMaxWidth()
                        .heightIn(min = 46.dp)
                        .background(
                            when {
                                selesaiDicek && isi == ronde[i].nomorAyat -> Color(0xFFE8F5E9)
                                selesaiDicek && isi != null -> Color(0xFFFFEBEE)
                                isi != null -> Color(0xFFF0FDF4)
                                else -> Color(0xFFF5F5F5)
                            },
                            RoundedCornerShape(10.dp)
                        )
                        .onGloballyPositioned { coords -> slotBounds[i] = coords.boundsInWindow() }
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("${i + 1}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F7A5A), modifier = Modifier.width(20.dp))
                    if (isi != null) {
                        Text(ayatOf(isi).teksArab, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                    } else {
                        Text("(kosong - seret ayat ke sini)", fontSize = 10.sp, color = Color.LightGray)
                    }
                }
            }
        }

        Column(
            Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState())
        ) {
        Spacer(Modifier.height(16.dp))
        Text("Potongan ayat (acak):", fontSize = 11.5.sp, color = Color.Gray)
        Spacer(Modifier.height(6.dp))

        // TRAY - potongan yang belum ditempatkan, disusun mengalir (wrap) supaya muat tanpa scroll tambahan.
        androidx.compose.foundation.layout.FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            potonganTray.forEach { nomorAyat ->
                PotonganAyatDraggable(
                    teks = ayatOf(nomorAyat).teksArab,
                    slotBounds = slotBounds,
                    slotTerisi = { i -> slotIsi.getOrNull(i) != null },
                    onDropDiSlot = { i ->
                        slotIsi[i] = nomorAyat
                        potonganTray.remove(nomorAyat)
                    },
                    onDragMulai = { teks, posAwal ->
                        dragAktif = true
                        dragTeks = teks
                        dragWindow = posAwal
                    },
                    onDragUpdate = { delta -> dragWindow += delta },
                    onDragSelesai = { dragAktif = false }
                )
            }
        }

        if (potonganTray.isEmpty() && !selesaiDicek) {
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = {
                    selesaiDicek = true
                    semuaBenar = ronde.indices.all { slotIsi[it] == ronde[it].nomorAyat }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F7A5A)),
                modifier = Modifier.fillMaxWidth()
            ) { Text("Periksa Urutan") }
        }

        if (selesaiDicek) {
            Spacer(Modifier.height(12.dp))
            Text(
                if (semuaBenar) "Benar semua! Lanjut ke ronde berikutnya." else "Ada yang belum tepat - urutan yang salah ditandai merah, tetap boleh lanjut.",
                color = if (semuaBenar) Color(0xFF0F7A5A) else Color(0xFFDC2626), fontWeight = FontWeight.Bold, fontSize = 12.5.sp
            )
            Spacer(Modifier.height(8.dp))
            Button(onClick = onRondeSelesai, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F7A5A)), modifier = Modifier.fillMaxWidth()) {
                Text("Lanjut")
            }
        }
        }
    }

    // Drag ghost overlay - di luar Column/scroll, tidak ter-clip, berada di depan semua
    if (dragAktif) {
        Text(
            dragTeks,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier
                .offset {
                    IntOffset(
                        (dragWindow.x - boxWindow.x).roundToInt(),
                        (dragWindow.y - boxWindow.y).roundToInt()
                    )
                }
                .background(Color(0xFFFFF8E1), RoundedCornerShape(10.dp))
                .border(1.5.dp, Color(0xFF0F7A5A), RoundedCornerShape(10.dp))
                .padding(horizontal = 14.dp, vertical = 10.dp)
        )
    }
    }
}

@Composable
private fun PotonganAyatDraggable(
    teks: String,
    slotBounds: Map<Int, Rect>,
    slotTerisi: (Int) -> Boolean,
    onDropDiSlot: (Int) -> Unit,
    onDragMulai: (teks: String, posAwalWindow: androidx.compose.ui.geometry.Offset) -> Unit,
    onDragUpdate: (delta: androidx.compose.ui.geometry.Offset) -> Unit,
    onDragSelesai: () -> Unit
) {
    var posisiAwalItem by remember { mutableStateOf<Rect?>(null) }
    var posisiJariWindow by remember { mutableStateOf<androidx.compose.ui.geometry.Offset?>(null) }
    var sedangDiseret by remember { mutableStateOf(false) }

    Box(
        Modifier
            .alpha(if (sedangDiseret) 0f else 1f)
            .onGloballyPositioned { coords ->
                // Simpan bounds awal hanya ketika TIDAK sedang drag,
                // supaya referensi posisi jari tetap stabil.
                if (!sedangDiseret) posisiAwalItem = coords.boundsInWindow()
            }
            .background(Color.White, RoundedCornerShape(10.dp))
            .then(
                Modifier.pointerInput(teks) {
                    detectDragGestures(
                        onDragStart = { startOffset ->
                            sedangDiseret = true
                            // Konversi posisi jari dari lokal Box ke koordinat window
                            posisiAwalItem?.let { r ->
                                posisiJariWindow = androidx.compose.ui.geometry.Offset(
                                    r.left + startOffset.x,
                                    r.top + startOffset.y
                                )
                                onDragMulai(teks, androidx.compose.ui.geometry.Offset(r.left, r.top))
                            }
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            posisiJariWindow = posisiJariWindow?.let {
                                androidx.compose.ui.geometry.Offset(it.x + dragAmount.x, it.y + dragAmount.y)
                            }
                            onDragUpdate(dragAmount)
                        },
                        onDragEnd = {
                            sedangDiseret = false
                            // Deteksi drop pakai POSISI JARI (tidak ter-clip parent scroll),
                            // bukan pusat item (yang bisa terpotong oleh scroll container).
                            val posJari = posisiJariWindow
                            val slotTujuan = if (posJari != null) {
                                slotBounds.entries.firstOrNull { (idx, rect) ->
                                    rect.contains(posJari) && !slotTerisi(idx)
                                }?.key
                            } else null
                            if (slotTujuan != null) onDropDiSlot(slotTujuan)
                            onDragSelesai()
                            posisiJariWindow = null
                        },
                        onDragCancel = {
                            sedangDiseret = false
                            onDragSelesai()
                            posisiJariWindow = null
                        }
                    )
                }
            )
            .border(1.5.dp, if (sedangDiseret) Color(0xFF0F7A5A) else Color(0xFFE0E0E0), RoundedCornerShape(10.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Text(teks, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
    }
}
