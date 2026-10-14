package com.imtiyaztour.app

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ============================================================================
// CHECKLIST BARANG BAWAAN UMRAH
// 4 kategori: Perlengkapan, Pakaian, Obat, Makanan. State lokal via Prefs.
// ============================================================================

private data class BarangItem(
    val id: String,
    val nama: String
)

private data class BarangSection(
    val judul: String,
    val emoji: String,
    val warna: Long,
    val items: List<BarangItem>
)

private val BARANG_SECTIONS = listOf(
    BarangSection(
        judul = "Perlengkapan", emoji = "🎒", warna = 0xFF0F7A5A,
        items = listOf(
            BarangItem("quran", "Al-Qur'an terjemahan"),
            BarangItem("hp_charger", "HP + charger + kuota internet"),
            BarangItem("dompet_ktp", "Dompet + KTP (untuk cek-in) + ATM"),
            BarangItem("stop_kontak", "Stop kontak kaki 3 + kabel colokan"),
            BarangItem("hanger", "Hanger plastik"),
            BarangItem("sandal", "Sandal jepit"),
            BarangItem("tali_raffia", "Tali raffia untuk ikhram"),
            BarangItem("kacamata", "Kacamata hitam (city tour, opsional)"),
        )
    ),
    BarangSection(
        judul = "Pakaian", emoji = "👕", warna = 0xFF1E40AF,
        items = listOf(
            BarangItem("ihram_mukena", "Kain ihram 1 set (pa) / Mukena (pi)"),
            BarangItem("pakaian_ganti", "4-5 set pakaian ganti"),
            BarangItem("seragam", "Pakaian seragam Imtiyaz"),
            BarangItem("jaket", "Jaket (jika perlu)"),
            BarangItem("kaos_dalam", "Kaos dalam, pakaian dalam, kaos kaki"),
        )
    ),
    BarangSection(
        judul = "Obat-obatan", emoji = "💊", warna = 0xFFDC2626,
        items = listOf(
            BarangItem("obat_rutin", "Obat rutin pribadi"),
            BarangItem("obat_warung", "Obat warung (Biogesic, Procold, batuk, Cataflam, mansyur, anti muntah)"),
            BarangItem("jamu", "Jamu (Antangin, Tolak Angin, IMBOSS)"),
        )
    ),
    BarangSection(
        judul = "Makanan / Lauk", emoji = "🍲", warna = 0xFFD97706,
        items = listOf(
            BarangItem("garam", "Garam"),
            BarangItem("sambel_abon", "Sambel pecel, abon, dll"),
            BarangItem("pop_mie", "Pop Mie (beberapa, jika perlu)"),
        )
    ),
)

private val ALL_KEYS: List<String> = BARANG_SECTIONS.flatMap { s -> s.items.map { it.id } }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChecklistBarangScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var refreshKey by remember { mutableIntStateOf(0) }
    var showReset by remember { mutableStateOf(false) }

    // Mutable state map dengan init dari Prefs
    val checklist = remember(refreshKey) {
        val map = mutableStateMapOf<String, Boolean>()
        ALL_KEYS.forEach { k -> map[k] = Prefs.getBarangItem(context, k) }
        map
    }

    val totalItems = ALL_KEYS.size
    val totalDone = ALL_KEYS.count { checklist[it] == true }
    val progress: Float = if (totalItems > 0) totalDone.toFloat() / totalItems else 0f
    val progressPct = (progress * 100).toInt()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Checklist Barang Bawaan",
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F7A5A), fontSize = 16.sp)
                        Text("$totalDone / $totalItems selesai",
                            fontSize = 11.sp, color = Color.Gray)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back",
                            tint = Color(0xFF0F7A5A))
                    }
                },
                actions = {
                    IconButton(onClick = { showReset = true }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Reset",
                            tint = Color(0xFFDC2626))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        }
    ) { padding ->
        LazyColumn(
            Modifier.padding(padding).fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Progress card
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF0FDF4)),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.padding(14.dp)) {
                        Row(Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically) {
                            Text("Progress", fontWeight = FontWeight.Bold,
                                color = Color(0xFF0F7A5A), fontSize = 13.sp)
                            Text("$progressPct%",
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0F7A5A), fontSize = 15.sp)
                        }
                        Spacer(Modifier.height(6.dp))
                        LinearProgressIndicator(
                            progress = progress,
                            modifier = Modifier.fillMaxWidth().height(8.dp),
                            color = Color(0xFF0F7A5A),
                            trackColor = Color(0xFF0F7A5A).copy(alpha = 0.15f)
                        )
                        Spacer(Modifier.height(6.dp))
                        Text("$totalDone dari $totalItems barang sudah disiapkan",
                            fontSize = 11.sp, color = Color.Gray)
                    }
                }
            }

            // Info koper
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF7ED)),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.padding(14.dp)) {
                        Text("🧳 Ketentuan Berat & Isi Koper",
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFD97706), fontSize = 13.sp)
                        Spacer(Modifier.height(8.dp))
                        Text("Koper Bagasi (24 inch) — maks 20 kg",
                            fontWeight = FontWeight.Bold, fontSize = 12.sp,
                            color = Color.Black)
                        Text("• Baju ganti, sandal, hanger, dll\n• ❌ Power bank dilarang\n• ❌ Cairan dilarang\n• ❌ Benda tajam dilarang",
                            fontSize = 11.sp, color = Color.DarkGray)
                        Spacer(Modifier.height(8.dp))
                        Text("Koper Kabin (18 inch) — maks 7 kg",
                            fontWeight = FontWeight.Bold, fontSize = 12.sp,
                            color = Color.Black)
                        Text("• ✅ Cairan (parfum/sabun/minyak) maks 100 ml per pcs\n• ✅ Power bank maks 10.000 mAh (kapasitas harus tertulis)\n• ✅ Uang & barang berharga\n• ❌ Benda tajam dilarang",
                            fontSize = 11.sp, color = Color.DarkGray)
                        Spacer(Modifier.height(8.dp))
                        Text("Tas Dokumen (slempang) — untuk paspor, KTP, tiket, visa, dll",
                            fontSize = 11.sp, color = Color.DarkGray,
                            fontWeight = FontWeight.Medium)
                    }
                }
            }

            // Sections
            BARANG_SECTIONS.forEach { section ->
                item(key = "header_${section.judul}") {
                    Row(Modifier.fillMaxWidth().padding(top = 4.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        Text(section.emoji, fontSize = 20.sp)
                        Spacer(Modifier.width(8.dp))
                        Text(section.judul, fontWeight = FontWeight.Bold,
                            fontSize = 14.sp, color = Color(section.warna.toInt()))
                        Spacer(Modifier.weight(1f))
                        val done = section.items.count { checklist[it.id] == true }
                        Text("$done/${section.items.size}",
                            fontSize = 11.sp, color = Color.Gray,
                            fontWeight = FontWeight.Bold)
                    }
                }
                items(section.items, key = { "item_${it.id}" }) { item ->
                    val checked = checklist[item.id] == true
                    BarangRow(
                        item = item,
                        checked = checked,
                        accent = Color(section.warna.toInt()),
                        onToggle = { v ->
                            Prefs.setBarangItem(context, item.id, v)
                            checklist[item.id] = v
                        }
                    )
                }
            }

            item { Spacer(Modifier.height(24.dp)) }
        }
    }

    if (showReset) {
        AlertDialog(
            onDismissRequest = { showReset = false },
            title = { Text("Reset Checklist?") },
            text = { Text("Semua tanda centang akan dihapus. Yakin?") },
            confirmButton = {
                TextButton(onClick = {
                    Prefs.resetBarangItems(context, ALL_KEYS)
                    showReset = false
                    refreshKey++
                }) {
                    Text("Reset", color = Color(0xFFDC2626),
                        fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showReset = false }) { Text("Batal") }
            }
        )
    }
}

@Composable
private fun BarangRow(
    item: BarangItem,
    checked: Boolean,
    accent: Color,
    onToggle: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (checked) accent.copy(alpha = 0.08f) else Color.White
        ),
        elevation = CardDefaults.cardElevation(1.dp),
        border = BorderStroke(
            1.dp,
            if (checked) accent.copy(alpha = 0.35f) else Color(0xFFE5E7EB)
        )
    ) {
        Row(
            Modifier.fillMaxWidth().clickable { onToggle(!checked) }.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = checked,
                onCheckedChange = { onToggle(it) },
                colors = CheckboxDefaults.colors(checkedColor = accent)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                item.nama,
                fontSize = 12.5.sp,
                color = if (checked) accent else Color(0xFF1F2937),
                fontWeight = if (checked) FontWeight.Medium else FontWeight.Normal,
                modifier = Modifier.weight(1f)
            )
        }
    }
}
