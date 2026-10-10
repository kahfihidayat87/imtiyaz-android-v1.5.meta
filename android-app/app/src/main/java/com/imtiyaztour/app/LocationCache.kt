package com.imtiyaztour.app

import android.content.Context

/**
 * v2.13.0 TAHAP 22 (Batch 4d): Cache GPS terakhir di SharedPreferences.
 *
 * Tujuan: TL lihat marker muncul <1 detik (dari cache) saat minta lokasi,
 * lalu refresh saat GPS akurat tiba. Sebelumnya, kalau GPS belum lock
 * (indoor/basement), lokasi tidak terkirim sama sekali.
 */
data class CachedLocation(
    val lat: Double,
    val lon: Double,
    val accuracy: Float,
    val timestamp: Long,
    val source: String
) {
    fun ageMs(): Long = System.currentTimeMillis() - timestamp
    fun isFresh(maxAgeMs: Long = 10 * 60 * 1000L): Boolean = ageMs() in 0 until maxAgeMs
}

object LocationCache {
    private const val KEY_LAT = "loc_cache_lat"
    private const val KEY_LON = "loc_cache_lon"
    private const val KEY_ACC = "loc_cache_acc"
    private const val KEY_TS  = "loc_cache_ts"
    private const val KEY_SRC = "loc_cache_src"

    fun save(ctx: Context, lat: Double, lon: Double, accuracy: Float, source: String) {
        Prefs.get(ctx).edit()
            .putString(KEY_LAT, lat.toString())
            .putString(KEY_LON, lon.toString())
            .putFloat(KEY_ACC, accuracy)
            .putLong(KEY_TS, System.currentTimeMillis())
            .putString(KEY_SRC, source)
            .apply()
    }

    fun get(ctx: Context): CachedLocation? {
        val p = Prefs.get(ctx)
        val lat = p.getString(KEY_LAT, null)?.toDoubleOrNull() ?: return null
        val lon = p.getString(KEY_LON, null)?.toDoubleOrNull() ?: return null
        val acc = p.getFloat(KEY_ACC, 0f)
        val ts  = p.getLong(KEY_TS, 0L)
        if (ts <= 0L) return null
        val src = p.getString(KEY_SRC, "cache") ?: "cache"
        return CachedLocation(lat, lon, acc, ts, src)
    }

    fun clear(ctx: Context) {
        Prefs.get(ctx).edit()
            .remove(KEY_LAT).remove(KEY_LON).remove(KEY_ACC)
            .remove(KEY_TS).remove(KEY_SRC)
            .apply()
    }
}
