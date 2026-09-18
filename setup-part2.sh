#!/usr/bin/env bash
set -euo pipefail

REPO="$(pwd)"

if [ -d "android-app/app/src/main" ]; then
  APP="android-app/app"
elif [ -d "app/src/main" ]; then
  APP="app"
else
  echo "❌ Jalankan script ini DARI dalam folder repo (yang ada android-app/ atau app/)"
  exit 1
fi

JAVA="$APP/src/main/java/com/imtiyaztour/app"
RES="$APP/src/main/res"

mkdir -p "$RES/drawable"

echo "📝 Membuat file baru..."

# ==================== Journal.kt ====================
cat > "$JAVA/Journal.kt" << 'EOF_JOURNAL'
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
EOF_JOURNAL
echo "   ✓ Journal.kt"

# ==================== Reminder.kt ====================
cat > "$JAVA/Reminder.kt" << 'EOF_REMINDER'
package com.imtiyaztour.app

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import androidx.compose.foundation.background
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File
import java.util.*

// ============================================================================
// REMINDER IBADAH & KESEHATAN -- pengingat harian yang dibuat jamaah sendiri.
// Tersimpan lokal, dijadwalkan pakai AlarmManager.
// ============================================================================

data class ReminderItem(
    val id: String,
    val judul: String,
    val deskripsi: String = "",
    val hour: Int,
    val minute: Int,
    val aktif: Boolean = true
)

object ReminderStore {
    private val gson = Gson()
    private const val FILE_NAME = "reminders.json"
    private fun file(ctx: Context) = File(ctx.filesDir, FILE_NAME)

    fun load(ctx: Context): List<ReminderItem> = try {
        if (!file(ctx).exists()) {
            val seeds = listOf(
                ReminderItem(UUID.randomUUID().toString(), "Minum Air", "Cuaca panas -- jangan lupa hidrasi", 10, 0),
                ReminderItem(UUID.randomUUID().toString(), "Minum Air Sore", "Minum air lagi untuk cegah dehidrasi", 16, 0),
                ReminderItem(UUID.randomUUID().toString(), "Istirahat", "Istirahat cukup agar fit untuk ibadah", 14, 0)
            )
            save(ctx, seeds); seeds
        } else {
            val type = object : TypeToken<List<ReminderItem>>() {}.type
            gson.fromJson<List<ReminderItem>>(file(ctx).readText(), type) ?: emptyList()
        }
    } catch (e: Exception) { emptyList() }

    private fun save(ctx: Context, list: List<ReminderItem>) {
        try { file(ctx).writeText(gson.toJson(list)) } catch (e: Exception) {}
    }

    fun add(ctx: Context, item: ReminderItem) { save(ctx, load(ctx) + item) }
    fun update(ctx: Context, item: ReminderItem) {
        save(ctx, load(ctx).map { if (it.id == item.id) item else it })
    }
    fun delete(ctx: Context, id: String) { save(ctx, load(ctx).filterNot { it.id == id }) }
}

object ReminderScheduler {
    const val CHANNEL_ID = "reminder_channel"
    const val ACTION_REMINDER = "com.imtiyaztour.app.REMINDER"
    const val EXTRA_ID = "reminder_id"
    const val EXTRA_JUDUL = "reminder_judul"
    const val EXTRA_DESKRIPSI = "reminder_deskripsi"

    fun ensureChannel(ctx: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val mgr = ctx.getSystemService(NotificationManager::class.java) ?: return
        if (mgr.getNotificationChannel(CHANNEL_ID) != null) return
        mgr.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "Pengingat Ibadah & Kesehatan", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Pengingat minum obat, air, istirahat, dll"
            }
        )
    }

    fun schedule(ctx: Context, item: ReminderItem) {
        if (!item.aktif) { cancel(ctx, item); return }
        val am = ctx.getSystemService(AlarmManager::class.java) ?: return
        ensureChannel(ctx)
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, item.hour); set(Calendar.MINUTE, item.minute)
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }
        if (cal.timeInMillis <= System.currentTimeMillis()) cal.add(Calendar.DAY_OF_YEAR, 1)
        val pi = pendingIntent(ctx, item)
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !am.canScheduleExactAlarms()) {
                am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, cal.timeInMillis, pi)
            } else {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, cal.timeInMillis, pi)
            }
        } catch (e: SecurityException) {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, cal.timeInMillis, pi)
        }
    }

    fun cancel(ctx: Context, item: ReminderItem) {
        val am = ctx.getSystemService(AlarmManager::class.java) ?: return
        am.cancel(pendingIntent(ctx, item))
    }

    private fun pendingIntent(ctx: Context, item: ReminderItem): PendingIntent {
        val intent = Intent(ctx, ReminderReceiver::class.java).apply {
            action = ACTION_REMINDER
            putExtra(EXTRA_ID, item.id)
            putExtra(EXTRA_JUDUL, item.judul)
            putExtra(EXTRA_DESKRIPSI, item.deskripsi)
        }
        return PendingIntent.getBroadcast(ctx, item.id.hashCode(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }

    fun rescheduleAll(ctx: Context) {
        ReminderStore.load(ctx).forEach { if (it.aktif) schedule(ctx, it) }
    }
}

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ReminderScheduler.ACTION_REMINDER) return
        val id = intent.getStringExtra(ReminderScheduler.EXTRA_ID) ?: return
        val judul = intent.getStringExtra(ReminderScheduler.EXTRA_JUDUL) ?: "Pengingat"
        val deskripsi = intent.getStringExtra(ReminderScheduler.EXTRA_DESKRIPSI) ?: ""
        ReminderScheduler.ensureChannel(context)
        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentPi = PendingIntent.getActivity(context, id.hashCode(), openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val notif = NotificationCompat.Builder(context, ReminderScheduler.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(judul).setContentText(deskripsi)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true).setContentIntent(contentPi)
            .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
            .build()
        try { NotificationManagerCompat.from(context).notify(id.hashCode(), notif) }
        catch (e: SecurityException) {}
        ReminderStore.load(context).find { it.id == id && it.aktif }?.let {
            ReminderScheduler.schedule(context, it)
        }
    }
}

@Composable
fun ReminderScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var items by remember { mutableStateOf(ReminderStore.load(context)) }
    var editing by remember { mutableStateOf<ReminderItem?>(null) }
    var showEditor by remember { mutableStateOf(false) }

    if (showEditor) {
        ReminderEditDialog(existing = editing,
            onDismiss = { showEditor = false; editing = null },
            onSave = { item ->
                if (editing == null) ReminderStore.add(context, item)
                else ReminderStore.update(context, item)
                ReminderScheduler.schedule(context, item)
                items = ReminderStore.load(context)
                showEditor = false; editing = null
            })
        return
    }

    LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Button(onClick = onBack,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F7A5A)),
                    shape = RoundedCornerShape(8.dp)) { Text("<- Kembali") }
                Spacer(Modifier.weight(1f))
                Button(onClick = { editing = null; showEditor = true },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F7A5A)),
                    shape = RoundedCornerShape(8.dp)) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp)); Text("Tambah")
                }
            }
            Spacer(Modifier.height(12.dp))
            Text("Pengingat Ibadah & Kesehatan", fontWeight = FontWeight.Bold, fontSize = 22.sp)
            Text("Jadwalkan pengingat minum obat, air, istirahat, dll", fontSize = 12.sp, color = Color.Gray)
            Spacer(Modifier.height(8.dp))
        }
        if (items.isEmpty()) {
            item { Text("Belum ada pengingat. Ketuk 'Tambah' di atas.", fontSize = 12.sp, color = Color.Gray) }
        }
        items(items) { item ->
            Card(shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(1.dp),
                modifier = Modifier.fillMaxWidth()) {
                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.width(64.dp)
                        .background(Color(0xFFF0FDF4), RoundedCornerShape(8.dp))
                        .padding(vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(String.format("%02d:%02d", item.hour, item.minute),
                            fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F7A5A))
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(item.judul, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        if (item.deskripsi.isNotBlank())
                            Text(item.deskripsi, fontSize = 11.sp, color = Color.Gray)
                        Text("Setiap hari", fontSize = 10.sp, color = Color.LightGray)
                    }
                    Switch(checked = item.aktif, onCheckedChange = { v ->
                        val updated = item.copy(aktif = v)
                        ReminderStore.update(context, updated)
                        if (v) ReminderScheduler.schedule(context, updated)
                        else ReminderScheduler.cancel(context, updated)
                        items = ReminderStore.load(context)
                    })
                    IconButton(onClick = { editing = item; showEditor = true }) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit",
                            tint = Color.Gray, modifier = Modifier.size(18.dp))
                    }
                    IconButton(onClick = {
                        ReminderScheduler.cancel(context, item)
                        ReminderStore.delete(context, item.id)
                        items = ReminderStore.load(context)
                    }) {
                        Icon(Icons.Default.Delete, contentDescription = "Hapus",
                            tint = Color(0xFFDC2626), modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
        item { Spacer(Modifier.height(8.dp)) }
    }
}

@Composable
private fun ReminderEditDialog(existing: ReminderItem?, onDismiss: () -> Unit, onSave: (ReminderItem) -> Unit) {
    var judul by remember { mutableStateOf(existing?.judul ?: "") }
    var deskripsi by remember { mutableStateOf(existing?.deskripsi ?: "") }
    var hour by remember { mutableIntStateOf(existing?.hour ?: 8) }
    var minute by remember { mutableIntStateOf(existing?.minute ?: 0) }

    AlertDialog(onDismissRequest = onDismiss,
        title = { Text(if (existing == null) "Tambah Pengingat" else "Edit Pengingat", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                OutlinedTextField(value = judul, onValueChange = { judul = it },
                    label = { Text("Judul") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(value = deskripsi, onValueChange = { deskripsi = it },
                    label = { Text("Keterangan (opsional)") }, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(12.dp))
                Text("Waktu", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Jam", fontSize = 10.sp, color = Color.Gray)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { hour = (hour + 23) % 24 }) {
                                Icon(Icons.Default.KeyboardArrowDown, contentDescription = null)
                            }
                            Text("$hour", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                            IconButton(onClick = { hour = (hour + 1) % 24 }) {
                                Icon(Icons.Default.KeyboardArrowUp, contentDescription = null)
                            }
                        }
                    }
                    Column(Modifier.weight(1f)) {
                        Text("Menit", fontSize = 10.sp, color = Color.Gray)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { minute = (minute + 55) % 60 }) {
                                Icon(Icons.Default.KeyboardArrowDown, contentDescription = null)
                            }
                            Text(String.format("%02d", minute), fontWeight = FontWeight.Bold, fontSize = 18.sp)
                            IconButton(onClick = { minute = (minute + 5) % 60 }) {
                                Icon(Icons.Default.KeyboardArrowUp, contentDescription = null)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (judul.isNotBlank()) {
                    onSave(ReminderItem(
                        id = existing?.id ?: UUID.randomUUID().toString(),
                        judul = judul.trim(), deskripsi = deskripsi.trim(),
                        hour = hour, minute = minute, aktif = existing?.aktif ?: true))
                }
            }, enabled = judul.isNotBlank()) {
                Text("Simpan", color = Color(0xFF0F7A5A), fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Batal") } })
}
EOF_REMINDER
echo "   ✓ Reminder.kt"

# ==================== ic_notification.xml ====================
cat > "$RES/drawable/ic_notification.xml" << 'EOF_ICON'
<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp" android:height="24dp"
    android:viewportWidth="24" android:viewportHeight="24">
    <path android:fillColor="#FFFFFF"
        android:pathData="M12,22c1.1,0 2,-0.9 2,-2h-4c0,1.1 0.9,2 2,2zM18,16v-5c0,-3.07 -1.64,-5.64 -4.5,-6.32V4c0,-0.83 -0.67,-1.5 -1.5,-1.5s-1.5,0.67 -1.5,1.5v0.68C7.63,5.36 6,7.92 6,11v5l-2,2v1h16v-1l-2,-2z"/>
</vector>
EOF_ICON
echo "   ✓ ic_notification.xml"

echo ""
echo "✅ Part 2 selesai -- 3 file baru berhasil dibuat."
echo ""
echo "📌 Lanjut ke STEP 2: patch file existing (lihat panduan di chat)."