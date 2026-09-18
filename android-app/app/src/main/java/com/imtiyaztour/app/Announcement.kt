package com.imtiyaztour.app

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import java.text.SimpleDateFormat
import java.util.*

// ============================================================================
// NOTIFIKASI UPDATE DARI ADMIN -- Admin bisa mengirim pengumuman (info penting,
// perubahan jadwal, reminder pembayaran, dll) dari WP Admin. Aplikasi cek tiap
// kali dibuka; kalau ada update baru, tampilkan notifikasi sistem + kartu
// highlight "Info Terbaru" di Beranda, sampai user membukanya (ditandai terbaca).
//
// Sumber data: endpoint /api/announcements (proxy ke WordPress option
// "imtiyaz_announcements"). Kalau endpoint belum ada / offline, fitur ini
// diam-diam di-skip -- tidak akan pernah bikin app crash atau error tampil.
// ============================================================================

data class Announcement(
    val id: String,
    val judul: String,
    val pesan: String,
    val tanggal: Long = System.currentTimeMillis(),
    val penting: Boolean = false
)

interface AnnouncementApiService {
    @GET("api/announcements")
    suspend fun getAnnouncements(): List<Announcement>
}

object AnnouncementApiClient {
    val service: AnnouncementApiService by lazy {
        Retrofit.Builder()
            .baseUrl(ApiConfig.BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(AnnouncementApiService::class.java)
    }
}

object AnnouncementStore {
    private const val NAME = "announcement_prefs"
    private const val KEY_LAST_ID = "last_seen_id"

    fun getLastSeenId(ctx: Context): String =
        ctx.getSharedPreferences(NAME, Context.MODE_PRIVATE).getString(KEY_LAST_ID, "") ?: ""

    fun setLastSeenId(ctx: Context, id: String) =
        ctx.getSharedPreferences(NAME, Context.MODE_PRIVATE).edit().putString(KEY_LAST_ID, id).apply()

    // State global untuk reaktivitas Compose
    var latest by mutableStateOf<Announcement?>(null)
    var isUnread by mutableStateOf(false)
}

object AnnouncementNotifier {
    const val CHANNEL_ID = "announcement_channel"

    fun ensureChannel(ctx: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val mgr = ctx.getSystemService(NotificationManager::class.java) ?: return
        if (mgr.getNotificationChannel(CHANNEL_ID) != null) return
        mgr.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "Info dari Admin", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Update informasi & pengumuman dari Admin Imtiyaz Tour"
                enableVibration(true)
            }
        )
    }

    fun notify(ctx: Context, a: Announcement) {
        ensureChannel(ctx)
        val openIntent = Intent(ctx, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pi = PendingIntent.getActivity(
            ctx, a.id.hashCode(), openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notif = NotificationCompat.Builder(ctx, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Info: ${a.judul}")
            .setContentText(a.pesan.take(120))
            .setStyle(NotificationCompat.BigTextStyle().bigText(a.pesan))
            .setPriority(if (a.penting) NotificationCompat.PRIORITY_HIGH else NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pi)
            .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
            .build()
        try { NotificationManagerCompat.from(ctx).notify(a.id.hashCode(), notif) }
        catch (e: SecurityException) { }
    }
}

/** Cek update dari server. Aman dipanggil berulang; kalau gagal, diam-diam skip. */
suspend fun checkAnnouncements(context: Context) {
    try {
        val list = withContext(Dispatchers.IO) { AnnouncementApiClient.service.getAnnouncements() }
        if (list.isEmpty()) return
        val newest = list.maxByOrNull { it.tanggal } ?: return
        AnnouncementStore.latest = newest
        val lastSeen = AnnouncementStore.getLastSeenId(context)
        if (newest.id != lastSeen) {
            AnnouncementStore.isUnread = true
            AnnouncementNotifier.notify(context, newest)
        } else {
            AnnouncementStore.isUnread = false
        }
    } catch (e: Exception) {
        // Offline / endpoint belum ada -- skip tanpa error
    }
}

fun markAnnouncementRead(context: Context) {
    AnnouncementStore.latest?.let { AnnouncementStore.setLastSeenId(context, it.id) }
    AnnouncementStore.isUnread = false
}

fun formatTanggalAnnouncement(ms: Long): String =
    SimpleDateFormat("d MMM yyyy, HH:mm", Locale("id", "ID")).format(Date(ms))

@Composable
fun AnnouncementCard(onClick: () -> Unit) {
    val a = AnnouncementStore.latest ?: return
    if (!AnnouncementStore.isUnread) return
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (a.penting) Color(0xFFFEF3C7) else Color(0xFFF0FDF4)
        ),
        elevation = CardDefaults.cardElevation(2.dp),
        modifier = Modifier.fillMaxWidth().clickable { onClick() }
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(
                if (a.penting) Icons.Default.PriorityHigh else Icons.Default.Notifications,
                contentDescription = null,
                tint = if (a.penting) Color(0xFFDC2626) else Color(0xFF0F7A5A)
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        if (a.penting) "INFO PENTING" else "INFO TERBARU",
                        fontSize = 10.sp, fontWeight = FontWeight.Bold,
                        color = if (a.penting) Color(0xFFDC2626) else Color(0xFF0F7A5A)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(formatTanggalAnnouncement(a.tanggal), fontSize = 9.sp, color = Color.Gray)
                }
                Spacer(Modifier.height(2.dp))
                Text(a.judul, fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 1)
                Text(
                    a.pesan.take(80) + if (a.pesan.length > 80) "..." else "",
                    fontSize = 11.sp, color = Color(0xFF374151), maxLines = 2
                )
            }
            Icon(Icons.Default.ArrowForward, contentDescription = null, tint = Color(0xFF0F7A5A))
        }
    }
}

@Composable
fun AnnouncementDetailDialog(onDismiss: () -> Unit) {
    val a = AnnouncementStore.latest ?: return
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(
                    if (a.penting) "Info Penting dari Admin" else "Info Terbaru",
                    fontWeight = FontWeight.Bold, color = Color(0xFF0F7A5A)
                )
                Text(formatTanggalAnnouncement(a.tanggal), fontSize = 11.sp, color = Color.Gray)
            }
        },
        text = {
            Column {
                Text(a.judul, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Spacer(Modifier.height(8.dp))
                Text(a.pesan, fontSize = 13.sp, color = Color(0xFF374151))
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Tutup", color = Color(0xFF0F7A5A), fontWeight = FontWeight.Bold)
            }
        }
    )
}
