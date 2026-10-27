package com.imtiyaztour.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.material3.Icon
import androidx.compose.foundation.clickable
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Card
import androidx.compose.material.icons.filled.Headset
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.Calendar

data class KajianJamaah(
    val id: String = "",
    val tema: String = "",
    val pemateri: String = "",
    val tanggal: Long = 0L,
    val jam: String = "",
    val kode_gelombang: String = ""
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MabrurScreen(onRadioClick: () -> Unit = {}) {
    val context = LocalContext.current
    var list by remember { mutableStateOf<List<KajianJamaah>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        try {
            val snap = FirebaseFirestore.getInstance().collection("kajian")
                .orderBy("tanggal", Query.Direction.DESCENDING)
                .limit(20)
                .get().await()
            list = snap.documents.mapNotNull { doc ->
                doc.toObject(KajianJamaah::class.java)?.copy(id = doc.id)
            }
        } catch (e: Exception) { }
        loading = false
    }

    Box(Modifier.fillMaxSize()) {
Column(Modifier.fillMaxSize().padding(16.dp)) {
            Text("Jadwal Kajian Bersama Asatidz Imtiyaz", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF111827))
            Text("Istiqamah menjaga kemabruran pasca umrah", fontSize = 12.sp, color = Color.Gray)

            Spacer(Modifier.height(16.dp))

            // --- KARTU RADIO IMTIYAZ ---
            Card(
                modifier = Modifier.fillMaxWidth().clickable { onRadioClick() },
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F7A5A)), // Warna hijau khas
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Headset, contentDescription = null, tint = Color.White, modifier = Modifier.size(32.dp))
                    Spacer(Modifier.width(16.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Radio Imtiyaz", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White)
                        Text("Dengarkan kajian dan manasik virtual", fontSize = 12.sp, color = Color(0xFFD1FAE5))
                    }
                }
            }
            // ----------------------------
            
            Spacer(Modifier.height(16.dp))

            if (loading) {
                CircularProgressIndicator(Modifier.align(Alignment.CenterHorizontally), color = Color(0xFF0F7A5A))
            } else if (list.isEmpty()) {
                Text("Belum ada jadwal kajian.", modifier = Modifier.align(Alignment.CenterHorizontally), color = Color.Gray)
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(list) { item ->
                        val tgl = SimpleDateFormat("EEEE, dd MMM yyyy", Locale("id", "ID")).format(Date(item.tanggal))
                        Card(shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(2.dp)) {
                            Column(Modifier.padding(16.dp)) {
                                Text(item.tema, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color(0xFF0F7A5A))
                                Spacer(Modifier.height(8.dp))
                                Text("Pemateri: ${item.pemateri}", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                Text("Waktu: $tgl", fontSize = 12.sp, color = Color.Gray)
                                Text("Jam: ${item.jam} WIB", fontSize = 12.sp, color = Color.Gray)
                                if (item.kode_gelombang.isNotBlank()) {
                                    Spacer(Modifier.height(12.dp))
                                    Box(Modifier.background(Color(0xFFFFF3CD), RoundedCornerShape(6.dp)).padding(8.dp)) {
                                        Text("Kode Radio: ${item.kode_gelombang}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF92400E))
                                    }
                                }
                                Spacer(Modifier.height(12.dp))
                                Button(
                                    onClick = {
                                        val kajianTime = getKajianTime(item.tanggal, item.jam)
                                        val reminderTime = kajianTime - (60 * 60 * 1000) // 1 jam sebelum
                                        KajianReminderScheduler.setReminder(context, reminderTime, item.tema)
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F7A5A)),
                                    modifier = Modifier.fillMaxWidth().height(36.dp)
                                ) {
                                    Icon(Icons.Default.Notifications, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Ingatkan Saya", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

fun getKajianTime(tanggalMs: Long, jamStr: String): Long {
    val cal = Calendar.getInstance()
    cal.timeInMillis = tanggalMs
    val parts = jamStr.split(":")
    if (parts.size == 2) {
        cal.set(Calendar.HOUR_OF_DAY, parts[0].toIntOrNull() ?: 8)
        cal.set(Calendar.MINUTE, parts[1].toIntOrNull() ?: 0)
        cal.set(Calendar.SECOND, 0)
    }
    return cal.timeInMillis
}
