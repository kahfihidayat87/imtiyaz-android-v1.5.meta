package com.imtiyaztour.app

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat

/**
 * v2.13.0 TAHAP 22 (Batch 4c): Deteksi OTA Android update + cek izin.
 *
 * OTA bisa mencabut izin tanpa pemberitahuan (khusus BACKGROUND_LOCATION di
 * Android 11+). Deteksi via Build.FINGERPRINT berubah, lalu verifikasi izin.
 */
object OtaDetector {
    private const val TAG = "OtaDetector"
    private const val KEY_LAST_FINGERPRINT = "last_fingerprint"

    data class PermissionStatus(
        val locationFine: Boolean,
        val locationBackground: Boolean,
        val notif: Boolean,
        val batteryUnrestricted: Boolean
    ) {
        fun isAllOk(): Boolean = locationFine && notif
        fun summary(): String = buildString {
            if (!locationFine) append("Lokasi ")
            if (!notif) append("Notif ")
            if (!locationBackground) append("BG-Loc ")
            if (!batteryUnrestricted) append("Baterai")
        }.trim().ifBlank { "OK" }
    }

    /** Cek izin aktual saat ini. */
    fun checkPermissions(ctx: Context): PermissionStatus {
        val fine = ContextCompat.checkSelfPermission(
            ctx, android.Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val bg = if (Build.VERSION.SDK_INT >= 29) {
            ContextCompat.checkSelfPermission(
                ctx, "android.permission.ACCESS_BACKGROUND_LOCATION"
            ) == PackageManager.PERMISSION_GRANTED
        } else true
        val notif = if (Build.VERSION.SDK_INT >= 33) {
            ContextCompat.checkSelfPermission(
                ctx, "android.permission.POST_NOTIFICATIONS"
            ) == PackageManager.PERMISSION_GRANTED
        } else true
        val battery = try {
            val pm = ctx.getSystemService(Context.POWER_SERVICE) as android.os.PowerManager
            pm.isIgnoringBatteryOptimizations(ctx.packageName)
        } catch (e: Exception) { false }

        return PermissionStatus(fine, bg, notif, battery)
    }

    /**
     * Cek apakah OTA baru terjadi. Return true kalau ada perubahan FINGERPRINT.
     * Kalau true, caller harus verifikasi izin + kirim alert kalau ada yang hilang.
     */
    fun detectOta(ctx: Context): Boolean {
        val current = Build.FINGERPRINT ?: return false
        val last = Prefs.get(ctx).getString(KEY_LAST_FINGERPRINT, null)
        Prefs.get(ctx).edit().putString(KEY_LAST_FINGERPRINT, current).apply()

        if (last == null) {
            Log.d(TAG, "First run, simpan FINGERPRINT")
            return false
        }
        if (last != current) {
            Log.w(TAG, "OTA terdeteksi: $last -> $current")
            return true
        }
        return false
    }

    /**
     * Cek lengkap: deteksi OTA + verifikasi izin. Return true kalau izin HILANG.
     */
    fun checkAndAlertIfNeeded(ctx: Context): Boolean {
        val otaHappened = detectOta(ctx)
        val perms = checkPermissions(ctx)
        if (!perms.isAllOk()) {
            Log.w(TAG, "Izin hilang: ${perms.summary()} (OTA=${otaHappened})")
            AlertIzinHelper.kirimAlert(ctx, perms.summary())
            return true
        }
        if (otaHappened) {
            Log.d(TAG, "OTA terjadi tapi izin masih OK")
        }
        return false
    }
}
