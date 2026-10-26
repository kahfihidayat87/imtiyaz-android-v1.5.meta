package com.imtiyaztour.app

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.widget.Toast

object KajianReminderScheduler {
    fun setReminder(context: Context, triggerTime: Long, tema: String) {
        if (triggerTime <= System.currentTimeMillis()) {
            Toast.makeText(context, "Waktu pengingat sudah lewat", Toast.LENGTH_SHORT).show()
            return
        }

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, KajianReminderReceiver::class.java).apply {
            putExtra("tema", tema)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context, 
            tema.hashCode(), 
            intent, 
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
            } else {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
            }
            Toast.makeText(context, "Pengingat diaktifkan 1 jam sebelum kajian", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(context, "Gagal set pengingat: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}
