package com.imtiyaztour.app

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

// ============================================================================
// BUKU CATATAN PERJALANAN (JOURNAL) -- catatan pribadi jamaah, TERSIMPAN LOKAL.
// Tidak perlu internet, tidak di-upload ke server -- privasi jamaah terjaga.
// ============================================================================

data class JournalEntry(
    val id: String,
    val judul: String,
    val isi: String,
    val fotoPath: String? = null,
    val lokasi: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

object JournalStore {
    private val gson = Gson()
    private const val FILE_NAME = "journal.json"
    private fun file(ctx: Context) = File(ctx.filesDir, FILE_NAME)

    fun load(ctx: Context): List<JournalEntry> = try {
        if (!file(ctx).exists()) emptyList()
        else {
            val type = object : TypeToken<List<JournalEntry>>() {}.type
            gson.fromJson<List<JournalEntry>>(file(ctx).readText(), type) ?: emptyList()
        }
    } catch (e: Exception) { emptyList() }

    private fun save(ctx: Context, list: List<JournalEntry>) {
        try { file(ctx).writeText(gson.toJson(list)) } catch (e: Exception) {}
    }

    fun add(ctx: Context, entry: JournalEntry): List<JournalEntry> {
        val list = load(ctx).toMutableList()
        list.add(0, entry)
        save(ctx, list); return list
    }
    fun update(ctx: Context, entry: JournalEntry): List<JournalEntry> {
        val list = load(ctx).toMutableList()
        val i = list.indexOfFirst { it.id == entry.id }
        if (i >= 0) list[i] = entry
        save(ctx, list); return list
    }
    fun delete(ctx: Context, id: String): List<JournalEntry> {
        val list = load(ctx).toMutableList()
        list.find { it.id == id }?.fotoPath?.let { try { File(it).delete() } catch (e: Exception) {} }
        list.removeAll { it.id == id }
        save(ctx, list); return list
    }

    fun copyPhotoToAppStorage(ctx: Context, src: Uri): String? = try {
        val dir = File(ctx.filesDir, "journal_photos").apply { mkdirs() }
        val dst = File(dir, "photo_${System.currentTimeMillis()}.jpg")
        ctx.contentResolver.openInputStream(src)?.use { input ->
            dst.outputStream().use { output -> input.copyTo(output) }
        }
        dst.absolutePath
    } catch (e: Exception) { null }
}

private fun formatTanggalJournal(ms: Long): String =
    SimpleDateFormat("EEEE, d MMMM yyyy - HH:mm", Locale("id", "ID")).format(Date(ms))

@Composable
fun JournalListScreen(onBack: () -> Unit, onOpen: (JournalEntry?) -> Unit) {
    val context = LocalContext.current
    var entries by remember { mutableStateOf(JournalStore.load(context)) }

    LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Button(onClick = onBack,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F7A5A)),
                    shape = RoundedCornerShape(8.dp)) { Text("<- Kembali") }
                Spacer(Modifier.weight(1f))
                Button(onClick = { onOpen(null) },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F7A5A)),
                    shape = RoundedCornerShape(8.dp)) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp)); Text("Tulis")
                }
            }
            Spacer(Modifier.height(12.dp))
            Text("Catatan Perjalanan", fontWeight = FontWeight.Bold, fontSize = 22.sp)
            Text("${entries.size} catatan -- tersimpan privat di HP Anda", fontSize = 12.sp, color = Color.Gray)
            Spacer(Modifier.height(8.dp))
        }
        if (entries.isEmpty()) {
            item {
                Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFF0FDF4)),
                    shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Book, contentDescription = null,
                            tint = Color(0xFF0F7A5A), modifier = Modifier.size(48.dp))
                        Spacer(Modifier.height(12.dp))
                        Text("Belum ada catatan", fontWeight = FontWeight.Bold,
                            fontSize = 14.sp, color = Color(0xFF0F7A5A))
                        Spacer(Modifier.height(4.dp))
                        Text("Tulis pengalaman, doa, atau momen berharga Anda selama umrah. Catatan tersimpan hanya di HP ini.",
                            fontSize = 11.sp, color = Color.Gray, textAlign = TextAlign.Center)
                    }
                }
            }
        }
        items(entries) { entry ->
            Card(shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(1.dp),
                modifier = Modifier.fillMaxWidth().clickable { onOpen(entry) }) {
                Row(Modifier.padding(12.dp)) {
                    entry.fotoPath?.let { path ->
                        if (File(path).exists()) {
                            val bmp = remember(path) {
                                try { BitmapFactory.decodeFile(path)?.asImageBitmap() } catch (e: Exception) { null }
                            }
                            if (bmp != null) {
                                Image(bitmap = bmp, contentDescription = null,
                                    modifier = Modifier.size(72.dp).background(Color.LightGray, RoundedCornerShape(8.dp)),
                                    contentScale = ContentScale.Crop)
                                Spacer(Modifier.width(12.dp))
                            }
                        }
                    }
                    Column(Modifier.weight(1f)) {
                        Text(entry.judul, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Spacer(Modifier.height(2.dp))
                        Text(entry.isi.take(80) + if (entry.isi.length > 80) "..." else "",
                            fontSize = 11.sp, color = Color(0xFF374151), maxLines = 2)
                        Spacer(Modifier.height(6.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Schedule, contentDescription = null,
                                tint = Color.Gray, modifier = Modifier.size(10.dp))
                            Spacer(Modifier.width(3.dp))
                            Text(formatTanggalJournal(entry.timestamp), fontSize = 9.sp, color = Color.Gray)
                        }
                        entry.lokasi?.let {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 2.dp)) {
                                Icon(Icons.Default.LocationOn, contentDescription = null,
                                    tint = Color.Gray, modifier = Modifier.size(10.dp))
                                Spacer(Modifier.width(3.dp))
                                Text(it, fontSize = 9.sp, color = Color.Gray)
                            }
                        }
                    }
                }
            }
        }
        item { Spacer(Modifier.height(8.dp)) }
    }
    LaunchedEffect(Unit) { entries = JournalStore.load(context) }
}

@Composable
fun JournalEditScreen(existing: JournalEntry?, onBack: () -> Unit, onSaved: () -> Unit) {
    val context = LocalContext.current
    var judul by remember { mutableStateOf(existing?.judul ?: "") }
    var isi by remember { mutableStateOf(existing?.isi ?: "") }
    var fotoPath by remember { mutableStateOf(existing?.fotoPath) }
    var lokasi by remember { mutableStateOf(existing?.lokasi) }

    val photoLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) JournalStore.copyPhotoToAppStorage(context, uri)?.let { fotoPath = it }
    }

    LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Button(onClick = onBack,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F7A5A)),
                shape = RoundedCornerShape(8.dp)) { Text("<- Kembali") }
            Spacer(Modifier.height(12.dp))
            Text(if (existing == null) "Catatan Baru" else "Edit Catatan",
                fontWeight = FontWeight.Bold, fontSize = 20.sp)
            Spacer(Modifier.height(12.dp))
        }
        item {
            OutlinedTextField(value = judul, onValueChange = { judul = it },
                label = { Text("Judul") }, modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp), singleLine = true)
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(value = isi, onValueChange = { isi = it },
                label = { Text("Isi catatan...") },
                modifier = Modifier.fillMaxWidth().heightIn(min = 180.dp),
                shape = RoundedCornerShape(12.dp))
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFF9FAFB)),
                shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp)) {
                    Text("Lampiran Foto (opsional)", fontSize = 12.sp,
                        fontWeight = FontWeight.Bold, color = Color.Gray)
                    Spacer(Modifier.height(8.dp))
                    if (fotoPath != null && File(fotoPath!!).exists()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            val bmp = remember(fotoPath) {
                                try { BitmapFactory.decodeFile(fotoPath)?.asImageBitmap() } catch (e: Exception) { null }
                            }
                            if (bmp != null) {
                                Image(bitmap = bmp, contentDescription = null,
                                    modifier = Modifier.size(80.dp).background(Color.LightGray, RoundedCornerShape(8.dp)),
                                    contentScale = ContentScale.Crop)
                                Spacer(Modifier.width(12.dp))
                            }
                            TextButton(onClick = { fotoPath = null }) {
                                Text("Hapus Foto", fontSize = 11.sp, color = Color(0xFFDC2626))
                            }
                        }
                    } else {
                        OutlinedButton(onClick = { photoLauncher.launch("image/*") },
                            modifier = Modifier.fillMaxWidth()) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp)); Text("Pilih Foto dari Galeri")
                        }
                    }
                }
            }
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFF9FAFB)),
                shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Sertakan lokasi", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Text("Ditandai dari lokasi perangkat saat ini", fontSize = 10.sp, color = Color.Gray)
                    }
                    Switch(checked = lokasi != null, onCheckedChange = { checked ->
                        lokasi = if (checked) "Lokasi perangkat saat ini" else null
                    })
                }
            }
        }
        item {
            Button(onClick = {
                if (judul.isBlank()) return@Button
                val entry = JournalEntry(
                    id = existing?.id ?: UUID.randomUUID().toString(),
                    judul = judul.trim(), isi = isi.trim(),
                    fotoPath = fotoPath, lokasi = lokasi,
                    timestamp = existing?.timestamp ?: System.currentTimeMillis()
                )
                if (existing == null) JournalStore.add(context, entry)
                else JournalStore.update(context, entry)
                onSaved()
            }, enabled = judul.isNotBlank(),
                modifier = Modifier.fillMaxWidth().height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F7A5A)),
                shape = RoundedCornerShape(12.dp)) {
                Text(if (existing == null) "Simpan Catatan" else "Simpan Perubahan",
                    fontWeight = FontWeight.Bold)
            }
            if (existing != null) {
                Spacer(Modifier.height(8.dp))
                OutlinedButton(onClick = { JournalStore.delete(context, existing.id); onSaved() },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFDC2626))) {
                    Text("Hapus Catatan")
                }
            }
        }
    }
}
