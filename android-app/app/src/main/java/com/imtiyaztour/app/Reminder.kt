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
