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
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import java.util.Calendar

// ============================================================================
// ADZAN ALERT -- notifikasi otomatis saat masuk waktu shalat.
// Jadwal dari PrayerTimesCard diteruskan ke AdzanScheduler yang memasang alarm
// via AlarmManager. Receiver menampilkan notifikasi + menjadwalkan ulang untuk
// 24 jam ke depan (rantai alarm tetap berjalan walau app tidak dibuka).
// ============================================================================

object AdzanPrefs {
    private const val NAME = "adzan_prefs"
    private const val KEY_ENABLED = "enabled"
    private const val KEY_TIMES = "last_times"

    fun isEnabled(ctx: Context) = ctx.getSharedPreferences(NAME, Context.MODE_PRIVATE).getBoolean(KEY_ENABLED, false)
    fun setEnabled(ctx: Context, v: Boolean) =
        ctx.getSharedPreferences(NAME, Context.MODE_PRIVATE).edit().putBoolean(KEY_ENABLED, v).apply()

    fun saveTimes(ctx: Context, t: PrayerTimesResult) {
        val csv = "${t.fajr},${t.sunrise},${t.dhuhr},${t.asr},${t.maghrib},${t.isha}"
        ctx.getSharedPreferences(NAME, Context.MODE_PRIVATE).edit().putString(KEY_TIMES, csv).apply()
    }
    fun getTimes(ctx: Context): PrayerTimesResult? {
        val s = ctx.getSharedPreferences(NAME, Context.MODE_PRIVATE).getString(KEY_TIMES, null) ?: return null
        return try {
            val p = s.split(",").map { it.toDouble() }
            PrayerTimesResult(p[0], p[1], p[2], p[3], p[4], p[5])
        } catch (e: Exception) { null }
    }
}

object AdzanScheduler {
    const val CHANNEL_ID = "adzan_channel"
    const val ACTION_ADZAN = "com.imtiyaztour.app.ADZAN"
    const val EXTRA_PRAYER = "prayer_name"

    private val PRAYERS = listOf("Fajar", "Dzuhur", "Ashar", "Maghrib", "Isya")
    private val REQUEST_CODES = mapOf(
        "Fajar" to 5001, "Dzuhur" to 5002, "Ashar" to 5003, "Maghrib" to 5004, "Isya" to 5005
    )

    fun ensureChannel(ctx: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val mgr = ctx.getSystemService(NotificationManager::class.java) ?: return
        if (mgr.getNotificationChannel(CHANNEL_ID) != null) return
        mgr.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "Waktu Shalat", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Pengingat masuk waktu shalat"; enableVibration(true)
            }
        )
    }

    fun scheduleAll(ctx: Context, times: PrayerTimesResult) {
        AdzanPrefs.saveTimes(ctx, times)
        if (!AdzanPrefs.isEnabled(ctx)) return
        ensureChannel(ctx)

        val prayerTimes = mapOf(
            "Fajar" to times.fajr, "Dzuhur" to times.dhuhr,
            "Ashar" to times.asr, "Maghrib" to times.maghrib, "Isya" to times.isha
        )
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }
        val todayMidnight = cal.timeInMillis
        val now = System.currentTimeMillis()

        prayerTimes.forEach { (name, hourFloat) ->
            val h = hourFloat.toInt()
            val m = ((hourFloat - h) * 60).toInt()
            val targetToday = todayMidnight + h * 3_600_000L + m * 60_000L
            val trigger = if (targetToday > now) targetToday else targetToday + 24 * 3_600_000L
            schedule(ctx, name, trigger)
        }
    }

    fun cancelAll(ctx: Context) {
        val am = ctx.getSystemService(AlarmManager::class.java) ?: return
        PRAYERS.forEach { name ->
            val code = REQUEST_CODES[name] ?: return@forEach
            val intent = Intent(ctx, AdzanReceiver::class.java).apply { action = ACTION_ADZAN }
            val pi = PendingIntent.getBroadcast(ctx, code, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            am.cancel(pi)
        }
    }

    fun schedule(ctx: Context, prayer: String, triggerMs: Long) {
        val am = ctx.getSystemService(AlarmManager::class.java) ?: return
        val code = REQUEST_CODES[prayer] ?: return
        val intent = Intent(ctx, AdzanReceiver::class.java).apply {
            action = ACTION_ADZAN; putExtra(EXTRA_PRAYER, prayer)
        }
        val pi = PendingIntent.getBroadcast(ctx, code, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !am.canScheduleExactAlarms()) {
                am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMs, pi)
            } else {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMs, pi)
            }
        } catch (e: SecurityException) {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMs, pi)
        }
    }
}

class AdzanReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != AdzanScheduler.ACTION_ADZAN) return
        val prayer = intent.getStringExtra(AdzanScheduler.EXTRA_PRAYER) ?: return
        AdzanScheduler.ensureChannel(context)

        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentPi = PendingIntent.getActivity(context, prayer.hashCode(), openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val notif = NotificationCompat.Builder(context, AdzanScheduler.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Waktunya Shalat $prayer")
            .setContentText("Sudah masuk waktu shalat $prayer. Ayo tunaikan.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setContentIntent(contentPi)
            .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
            .build()
        try { NotificationManagerCompat.from(context).notify(prayer.hashCode(), notif) }
        catch (e: SecurityException) { }

        AdzanScheduler.schedule(context, prayer, System.currentTimeMillis() + 24 * 3_600_000L)
    }
}

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        if (!AdzanPrefs.isEnabled(context)) return
        val times = AdzanPrefs.getTimes(context) ?: return
        AdzanScheduler.scheduleAll(context, times)
    }
}
