package com.imtiyaztour.app

import android.content.Context
import com.google.gson.Gson
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

// ============================================================================
// TRACKING ISTIQAMAH -- penyimpanan lokal (SharedPreferences + JSON)
// 100% offline, tanpa server, tanpa grup. Sesuai filosofi "privat personal".
// ============================================================================

data class TrackingDailyLog(
    val date: String = "",
    val checked: Map<String, Boolean> = emptyMap(),
    val quranPages: Int = 0,
    val points: Int = 0
)

data class TrackingState(
    val currentStreak: Int = 0,
    val longestStreak: Int = 0,
    val totalPoints: Int = 0,
    val logs: Map<String, TrackingDailyLog> = emptyMap(),
    val alarmEnabled: Map<String, Boolean> = emptyMap()
) {
    fun todayChecked(): Map<String, Boolean> = logs[todayKey()]?.checked ?: emptyMap()
    fun todayQuranPages(): Int = logs[todayKey()]?.quranPages ?: 0

    fun last7Days(): List<Pair<String, Int>> {
        val labelFmt = SimpleDateFormat("EEE", Locale("id", "ID"))
        val keyFmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val result = mutableListOf<Pair<String, Int>>()
        for (i in 6 downTo 0) {
            val c = Calendar.getInstance()
            c.add(Calendar.DAY_OF_YEAR, -i)
            val key = keyFmt.format(c.time)
            val label = labelFmt.format(c.time)
            val points = logs[key]?.points ?: 0
            result.add(label to points)
        }
        return result
    }
}

private fun todayKey(): String =
    SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

object TrackingStore {
    private const val NAME = "tracking_istiqamah_prefs"
    private const val KEY_STATE = "state_json"
    private val gson = Gson()

    fun load(ctx: Context): TrackingState {
        return try {
            val json = ctx.getSharedPreferences(NAME, Context.MODE_PRIVATE)
                .getString(KEY_STATE, null) ?: return TrackingState()
            gson.fromJson(json, TrackingState::class.java) ?: TrackingState()
        } catch (e: Exception) {
            TrackingState()
        }
    }

    private fun save(ctx: Context, state: TrackingState) {
        try {
            ctx.getSharedPreferences(NAME, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_STATE, gson.toJson(state))
                .apply()
        } catch (e: Exception) { }
    }

    /** Dipakai oleh TrackingAlarmHelper untuk persist perubahan alarm. */
    fun saveAlarmState(ctx: Context, state: TrackingState) = save(ctx, state)

    fun saveToday(ctx: Context, checked: Map<String, Boolean>, quranPages: Int): TrackingState {
        val old = load(ctx)
        val today = todayKey()
        var points = 0
        TRACKING_KATEGORI.forEach { k ->
            k.items.forEach { item ->
                if (item.type == "counter" && item.id == "quran_pages") points += item.points * quranPages
                else if (checked[item.id] == true) {
                    points += item.points
                    // Jamaah bonus (kalau checkbox "Berjamaah" dicentang)
                    if (item.jamaahBonus && checked["${item.id}_jamaah"] == true) points += 5
                }
            }
        }
        val oldLog = old.logs[today]
        val oldPoints = oldLog?.points ?: 0
        val newLog = TrackingDailyLog(date = today, checked = checked, quranPages = quranPages, points = points)
        val newLogs = old.logs.toMutableMap().apply { put(today, newLog) }

        // Streak: kalau belum pernah log hari ini → cek kemarin
        val yesterday = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
        val yesterdayKey = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(yesterday.time)
        val hadYesterday = newLogs.containsKey(yesterdayKey)
        val alreadyToday = oldLog != null
        val newStreak = when {
            alreadyToday -> old.currentStreak
            hadYesterday -> old.currentStreak + 1
            else -> 1
        }
        val newTotal = (old.totalPoints - oldPoints + points).coerceAtLeast(0)

        val newState = old.copy(
            currentStreak = newStreak,
            longestStreak = maxOf(old.longestStreak, newStreak),
            totalPoints = newTotal,
            logs = newLogs
        )
        save(ctx, newState)
        return newState
    }
}

// ============================================================================
// DATA CLASS + KATEGORI (dipakai juga oleh TrackingIstiqamah.kt)
// ============================================================================
data class TrackingAmalan(
    val id: String,
    val label: String,
    val sub: String? = null,
    val points: Int,
    val type: String = "check", // "check" | "counter"
    val max: Int = 20,
    val jamaahBonus: Boolean = false // true = ada toggle "Berjamaah" +5 poin
)

data class TrackingKategori(
    val id: String,
    val title: String,
    val icon: String,
    val desc: String,
    val items: List<TrackingAmalan>
)

val TRACKING_KATEGORI: List<TrackingKategori> = listOf(
    TrackingKategori("sholat", "Sholat Wajib & Sunnah", "\uD83D\uDD4C", "Fondasi hari - 5 waktu + sunnah", listOf(
        TrackingAmalan("subuh", "Subuh", null, 5, jamaahBonus = true),
        TrackingAmalan("dzuhur", "Dzuhur", null, 5, jamaahBonus = true),
        TrackingAmalan("ashar", "Ashar", null, 5, jamaahBonus = true),
        TrackingAmalan("maghrib", "Maghrib", null, 5, jamaahBonus = true),
        TrackingAmalan("isya", "Isya", null, 5, jamaahBonus = true),
        TrackingAmalan("rawatib", "Rawatib 10 rakaat", "Qabliyah & Ba'diyah", 5),
        TrackingAmalan("tahajud", "Tahajud 2 rakaat + Witir", "Qiyamullail", 20),
        TrackingAmalan("dhuha", "Dhuha 2-8 rakaat", null, 15)
    )),
    TrackingKategori("quran", "Al-Quran", "\uD83D\uDCD6", "Target 1 juz / hari (20 halaman)", listOf(
        TrackingAmalan("quran_pages", "Halaman hari ini", "5 poin / halaman", 5, "counter", 20),
        TrackingAmalan("taddabur", "Taddabur 1 ayat", "Renungi makna", 15),
        TrackingAmalan("khatam_progress", "Murajaah & progress khatam", null, 10)
    )),
    TrackingKategori("dzikir", "Dzikir Pagi Petang", "\u2728", "Perisai harian", listOf(
        TrackingAmalan("dzikir_pagi", "Dzikir Pagi (8 doa)", null, 15),
        TrackingAmalan("dzikir_petang", "Dzikir Petang (8 doa)", null, 15),
        TrackingAmalan("tasbih", "Tasbih 33x", "Subhanallah, Alhamdulillah, Allahuakbar", 10),
        TrackingAmalan("kursi", "Ayat Kursi pagi & petang", null, 10)
    )),
    TrackingKategori("puasa", "Puasa Sunnah", "\uD83C\uDF19", "Senin-Kamis & Ayyamul Bidh", listOf(
        TrackingAmalan("puasa_senin_kamis", "Puasa Senin / Kamis", null, 15),
        TrackingAmalan("puasa_bidh", "Ayyamul Bidh 13-14-15", "Kalender hijriah", 15),
        TrackingAmalan("puasa_niat", "Niat & keistiqomahan puasa", null, 5)
    )),
    TrackingKategori("sedekah", "Sedekah", "\uD83E\uDD32", "Pembersih harta & hati", listOf(
        TrackingAmalan("sedekah_harian", "Sedekah harian", "Nominal bebas", 10),
        TrackingAmalan("infak", "Infak / wakaf", null, 10),
        TrackingAmalan("amal_kecil", "Bonus amal kecil", "Senyum, bantu, singkirkan duri", 5)
    )),
    TrackingKategori("kajian", "Kajian", "\uD83D\uDCDA", "Menjaga ilmu pasca Umrah", listOf(
        TrackingAmalan("baca_buku", "Baca buku agama 15 menit", null, 10),
        TrackingAmalan("kultum", "Dengar kultum / kajian", "Cek kajian di menu Pembimbing Umrah", 10),
        TrackingAmalan("menulis", "Menulis resume kajian", "Ringkasan materi yang dipelajari", 15)
    )),
    TrackingKategori("muhasabah", "Muhasabah Malam", "\uD83C\uDF0C", "Jurnal refleksi 2 menit", listOf(
        TrackingAmalan("jurnal", "Jurnal refleksi 2 menit", "Apa yang Allah mudahkan hari ini?", 15),
        TrackingAmalan("self_accounting", "Meminta maaf & memaafkan", "Bersihkan hati sebelum tidur", 10)
    )),
    TrackingKategori("nilai", "Nilai Umrah", "\uD83D\uDD4B", "Akhlaq dari tanah suci", listOf(
        TrackingAmalan("sederhana", "Tidak rafats", "Menjauhi perkataan kotor", 10),
        TrackingAmalan("sabar", "Tidak fasiq", "Menjauhi perbuatan maksiat & durhaka", 10),
        TrackingAmalan("suci", "Tidak berdebat", "Menghindari pertengkaran & debat", 10)
    ))
)


// ============================================================================
// HELPER: Alarm "Ingatkan Aku" untuk kategori Tracking Istiqamah
// Reuse ReminderScheduler existing — jangan bikin scheduler baru.
// ============================================================================

/**
 * Daftar alarm per kategori. Total 3 kategori aktif = 4 alarm.
 * - dzikir   → 05:00 (pagi) + 17:00 (petang)
 * - sedekah  → 10:00
 * - muhasabah → 21:00
 */
fun trackingAlarmsFor(kategoriId: String): List<ReminderItem> = when (kategoriId) {
    "dzikir" -> listOf(
        ReminderItem("tracking_dzikir_pagi", "Dzikir Pagi", "Waktunya dzikir pagi (8 doa)", 5, 0),
        ReminderItem("tracking_dzikir_petang", "Dzikir Petang", "Waktunya dzikir petang (8 doa)", 17, 0)
    )
    "sedekah" -> listOf(
        ReminderItem("tracking_sedekah", "Sedekah Hari Ini", "Jangan lupa sedekah pagi ini", 10, 0)
    )
    "muhasabah" -> listOf(
        ReminderItem("tracking_muhasabah", "Muhasabah Malam", "Refleksi 2 menit sebelum tidur", 21, 0)
    )
    else -> emptyList()
}

fun trackingAlarmTimesText(kategoriId: String): String = when (kategoriId) {
    "dzikir" -> "05:00 & 17:00"
    "sedekah" -> "10:00"
    "muhasabah" -> "21:00"
    else -> ""
}

object TrackingAlarmHelper {
    /** Kategori yang punya toggle alarm. */
    val ALARM_KATEGORI = setOf("dzikir", "sedekah", "muhasabah")

    /** Toggle ON/OFF alarm untuk kategori. Return state baru. */
    fun toggle(ctx: Context, kategoriId: String, enable: Boolean): TrackingState {
        val old = TrackingStore.load(ctx)
        val newMap = old.alarmEnabled.toMutableMap().apply { put(kategoriId, enable) }
        val newState = old.copy(alarmEnabled = newMap)
        TrackingStore.saveAlarmState(ctx, newState)

        trackingAlarmsFor(kategoriId).forEach { item ->
            if (enable) ReminderScheduler.schedule(ctx, item)
            else ReminderScheduler.cancel(ctx, item)
        }
        return newState
    }

    /** Re-schedule semua alarm yang ON (dipanggil saat app dibuka). */
    fun rescheduleAll(ctx: Context) {
        val state = TrackingStore.load(ctx)
        state.alarmEnabled.forEach { (kategoriId, enabled) ->
            if (enabled) {
                trackingAlarmsFor(kategoriId).forEach { item ->
                    ReminderScheduler.schedule(ctx, item)
                }
            }
        }
    }
}
