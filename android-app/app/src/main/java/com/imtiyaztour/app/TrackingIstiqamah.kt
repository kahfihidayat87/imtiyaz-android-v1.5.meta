package com.imtiyaztour.app

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val TR_HIJAU = Color(0xFF0F7A5A)
private val TR_HIJAU_MUDA = Color(0xFFF0FDF4)
private val TR_ABU = Color(0xFF6B7F78)

private data class LevelInfo(val name: String, val level: Int)
private fun levelFromPoints(p: Int): LevelInfo = when {
    p < 201 -> LevelInfo("Pemula", 1)
    p < 1001 -> LevelInfo("Istiqomah", 2)
    p < 3001 -> LevelInfo("Hafidz", 3)
    else -> LevelInfo("Muttaqin", 4)
}

private fun kategoriPoints(k: TrackingKategori, checked: Map<String, Boolean>, quranPages: Int): Int {
    var total = 0
    k.items.forEach { item ->
        if (item.type == "counter" && item.id == "quran_pages") total += item.points * quranPages
        else if (checked[item.id] == true) {
            total += item.points
            if (item.jamaahBonus && checked["${item.id}_jamaah"] == true) total += 5
        }
    }
    return total
}

@Composable
fun TrackingIstiqamahScreen() {
    val context = LocalContext.current
    var state by remember { mutableStateOf(TrackingStore.load(context)) }
    var checked by remember { mutableStateOf<Map<String, Boolean>>(state.todayChecked()) }
    var quranPages by remember { mutableStateOf(state.todayQuranPages()) }
    var expandedId by remember { mutableStateOf<String?>("sholat") }
    var showHistori by remember { mutableStateOf(false) }
    var toastMsg by remember { mutableStateOf("") }

    val todayPoints = remember(checked, quranPages) {
        var total = 0
        TRACKING_KATEGORI.forEach { k ->
            k.items.forEach { item ->
                if (item.type == "counter" && item.id == "quran_pages") total += item.points * quranPages
                else if (checked[item.id] == true) {
                    total += item.points
                    if (item.jamaahBonus && checked["${item.id}_jamaah"] == true) total += 5
                }
            }
        }
        total
    }
    val levelInfo = levelFromPoints(state.totalPoints + todayPoints)

    LazyColumn(
        Modifier.fillMaxSize().background(Color(0xFFF7F9F7)).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Header streak + level
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("STREAK ISTIQOMAH", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TR_HIJAU.copy(alpha = 0.6f))
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text("${state.currentStreak}", fontSize = 44.sp, fontWeight = FontWeight.Bold, color = TR_HIJAU)
                            Spacer(Modifier.width(6.dp))
                            Text("hari", fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 6.dp))
                        }
                        Text("MasyaAllah, lanjutkan! Target 40 hari.", fontSize = 11.sp, color = TR_ABU)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("LEVEL", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TR_ABU)
                        Text(levelInfo.name, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TR_HIJAU)
                        Text("Lv.${levelInfo.level}", fontSize = 10.sp, color = TR_HIJAU)
                    }
                }
            }
        }

        // Poin hari ini
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text("POIN HARI INI", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TR_ABU)
                    Spacer(Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text("$todayPoints", fontSize = 32.sp, fontWeight = FontWeight.Bold, color = Color(0xFF111827))
                        Spacer(Modifier.width(8.dp))
                        Text("target 150", fontSize = 11.sp, color = TR_HIJAU, modifier = Modifier.padding(bottom = 6.dp))
                    }
                    Spacer(Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = (todayPoints / 150f).coerceIn(0f, 1f),
                        modifier = Modifier.fillMaxWidth().height(6.dp),
                        color = TR_HIJAU,
                        trackColor = TR_HIJAU.copy(alpha = 0.1f)
                    )
                }
            }
        }

        // Grafik mingguan
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text("Grafik Mingguan", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Text("7 hari terakhir - privat", fontSize = 10.sp, color = TR_ABU)
                    Spacer(Modifier.height(10.dp))
                    val week = state.last7Days()
                    val max = week.maxOfOrNull { it.second }?.coerceAtLeast(1) ?: 1
                    Row(Modifier.fillMaxWidth().height(100.dp), verticalAlignment = Alignment.Bottom) {
                        week.forEach { (label, value) ->
                            Column(
                                Modifier.weight(1f),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Bottom
                            ) {
                                Text("$value", fontSize = 9.sp, color = TR_ABU)
                                Spacer(Modifier.height(2.dp))
                                Box(
                                    Modifier
                                        .width(20.dp)
                                        .fillMaxHeight((value.toFloat() / max).coerceAtLeast(0.05f))
                                        .background(TR_HIJAU, RoundedCornerShape(6.dp))
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(label, fontSize = 9.sp, color = TR_ABU)
                            }
                        }
                    }
                }
            }
        }

        item {
            Text("8 Kategori Amalan Harian", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Text("Klik kategori untuk buka checklist", fontSize = 11.sp, color = TR_ABU)
        }

        // 8 kategori accordion
        items(TRACKING_KATEGORI) { kategori ->
            val isExpanded = expandedId == kategori.id
            val checkedCount = kategori.items.count {
                if (it.type == "counter" && it.id == "quran_pages") quranPages > 0
                else checked[it.id] == true
            }
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(1.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    Row(
                        Modifier.fillMaxWidth()
                            .clickable { expandedId = if (isExpanded) null else kategori.id }
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            Modifier.size(40.dp).background(TR_HIJAU_MUDA, RoundedCornerShape(10.dp)),
                            contentAlignment = Alignment.Center
                        ) { Text(kategori.icon, fontSize = 18.sp) }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(kategori.title, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text(kategori.desc, fontSize = 10.sp, color = TR_ABU)
                            Text(
                                "$checkedCount / ${kategori.items.size} - ${kategoriPoints(kategori, checked, quranPages)} poin",
                                fontSize = 10.sp, color = TR_HIJAU, fontWeight = FontWeight.Bold
                            )
                        }
                        Icon(
                            if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = null,
                            tint = TR_ABU
                        )
                    }
                    if (isExpanded) {
                        Box(Modifier.fillMaxWidth().height(1.dp).background(Color.Black.copy(alpha = 0.05f)))
                        Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            kategori.items.forEach { item ->
                                if (item.type == "counter") {
                                    Row(
                                        Modifier.fillMaxWidth().background(Color(0xFFF7F9F7), RoundedCornerShape(10.dp)).padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(Modifier.weight(1f)) {
                                            Text(item.label, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                            item.sub?.let { Text(it, fontSize = 10.sp, color = TR_ABU) }
                                        }
                                        IconButton(onClick = { if (quranPages > 0) quranPages-- }) {
                                            Text("\u2212", fontSize = 20.sp, color = TR_HIJAU, fontWeight = FontWeight.Bold)
                                        }
                                        Text("$quranPages", fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(24.dp), textAlign = TextAlign.Center)
                                        IconButton(onClick = { if (quranPages < item.max) quranPages++ }) {
                                            Text("+", fontSize = 20.sp, color = TR_HIJAU, fontWeight = FontWeight.Bold)
                                        }
                                        Text("+${item.points * quranPages}", fontSize = 10.sp, color = TR_HIJAU, fontWeight = FontWeight.Bold)
                                    }
                                } else {
                                    val isChecked = checked[item.id] == true
                                    val isJamaah = checked["${item.id}_jamaah"] == true
                                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Row(
                                            Modifier.fillMaxWidth()
                                                .background(if (isChecked) TR_HIJAU_MUDA else Color(0xFFF7F9F7), RoundedCornerShape(10.dp))
                                                .clickable {
                                                    checked = checked.toMutableMap().apply { put(item.id, !isChecked) }
                                                }
                                                .padding(6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Checkbox(
                                                checked = isChecked,
                                                onCheckedChange = { v ->
                                                    checked = checked.toMutableMap().apply { put(item.id, v) }
                                                },
                                                colors = CheckboxDefaults.colors(checkedColor = TR_HIJAU)
                                            )
                                            Column(Modifier.weight(1f)) {
                                                Text(item.label, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                                item.sub?.let { Text(it, fontSize = 10.sp, color = TR_ABU) }
                                            }
                                            Text("+${item.points}", fontSize = 10.sp, color = TR_HIJAU, fontWeight = FontWeight.Bold, modifier = Modifier.padding(end = 8.dp))
                                        }
                                        // Toggle "Berjamaah" — hanya muncul kalau sholat sudah dicentang
                                        if (item.jamaahBonus && isChecked) {
                                            Row(
                                                Modifier
                                                    .fillMaxWidth()
                                                    .padding(start = 32.dp)
                                                    .background(if (isJamaah) TR_HIJAU.copy(alpha = 0.08f) else Color.Transparent, RoundedCornerShape(8.dp))
                                                    .clickable {
                                                        checked = checked.toMutableMap().apply { put("${item.id}_jamaah", !isJamaah) }
                                                    }
                                                    .padding(4.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Checkbox(
                                                    checked = isJamaah,
                                                    onCheckedChange = { v ->
                                                        checked = checked.toMutableMap().apply { put("${item.id}_jamaah", v) }
                                                    },
                                                    colors = CheckboxDefaults.colors(checkedColor = TR_HIJAU)
                                                )
                                                Text("\uD83D\uDD4C Berjamaah", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TR_HIJAU)
                                                Spacer(Modifier.weight(1f))
                                                Text("+5", fontSize = 10.sp, color = TR_HIJAU, fontWeight = FontWeight.Bold, modifier = Modifier.padding(end = 8.dp))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Tombol simpan + histori
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                Button(
                    onClick = {
                        val newState = TrackingStore.saveToday(context, checked, quranPages)
                        state = newState
                        toastMsg = "Alhamdulillah! $todayPoints poin tersimpan hari ini."
                    },
                    modifier = Modifier.weight(1f).height(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = TR_HIJAU),
                    shape = RoundedCornerShape(12.dp)
                ) { Text("Simpan Hari Ini", fontWeight = FontWeight.Bold) }
                OutlinedButton(
                    onClick = { showHistori = true },
                    modifier = Modifier.height(50.dp),
                    shape = RoundedCornerShape(12.dp)
                ) { Text("Histori") }
            }
        }

        // Muhasabah quote
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = TR_HIJAU),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text("MUHASABAH HARI INI", color = Color.White.copy(alpha = 0.7f), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "\"Barang siapa yang hari ini lebih baik dari hari kemarin, dialah tergolong orang yang beruntung, (dan) barang siapa yang hari ini sama dengan hari kemarin dialah tergolong orang yang merugi dan bahkan, barang siapa yang hari ini lebih buruk dari hari kemarin dialah tergolong orang yang celaka.\"",
                        color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(8.dp))
                    Text("(HR. Al Hakim)", color = Color.White.copy(alpha = 0.8f), fontSize = 10.sp)
                }
            }
        }

        item { Spacer(Modifier.height(24.dp)) }
    }

    if (showHistori) {
        AlertDialog(
            onDismissRequest = { showHistori = false },
            title = { Text("Histori 7 Hari Terakhir", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    val week = state.last7Days()
                    if (week.all { it.second == 0 }) {
                        Text("Belum ada histori. Yuk mulai dari hari ini!", fontSize = 12.sp, color = TR_ABU)
                    } else {
                        week.reversed().forEach { (label, value) ->
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(label, fontSize = 12.sp)
                                Text("$value poin", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (value > 100) TR_HIJAU else TR_ABU)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showHistori = false }) { Text("Tutup", color = TR_HIJAU) }
            }
        )
    }

    if (toastMsg.isNotEmpty()) {
        LaunchedEffect(toastMsg) {
            kotlinx.coroutines.delay(2500)
            toastMsg = ""
        }
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
            Card(
                Modifier.padding(bottom = 24.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1A2E25)),
                shape = RoundedCornerShape(24.dp)
            ) {
                Text(toastMsg, color = Color.White, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp))
            }
        }
    }
}
