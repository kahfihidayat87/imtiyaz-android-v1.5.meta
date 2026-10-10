@file:OptIn(ExperimentalMaterial3Api::class)
@file:Suppress("BatteryLife")

package com.imtiyaztour.app

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat

/**
 * v2.13.0 TAHAP 22: Wizard setup 3 langkah untuk jamaah lansia.
 * Ditampilkan otomatis setelah login pertama kali. Tujuan:
 *  - Kumpulkan izin lokasi, notifikasi, dan battery exemption SEKALI di awal.
 *  - Setelah ini, Mode Aman start otomatis — user tidak perlu klik apapun lagi.
 */
@Composable
fun SetupWizardScreen(
    onFinish: () -> Unit,
    onSkip: () -> Unit
) {
    val context = LocalContext.current
    var step by remember { mutableStateOf(0) }
    var locGranted by remember { mutableStateOf(hasFineLocation(context)) }
    var notifGranted by remember { mutableStateOf(hasNotifPermission(context)) }
    var batteryOk by remember { mutableStateOf(isBatteryUnrestricted(context)) }

    val locLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        locGranted = result[android.Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                     result[android.Manifest.permission.ACCESS_COARSE_LOCATION] == true ||
                     hasFineLocation(context)
    }
    val notifLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> notifGranted = granted || hasNotifPermission(context) }

    Box(
        Modifier.fillMaxSize().background(Color(0xCC000000)),
        contentAlignment = Alignment.Center
    ) {
        Card(
            Modifier.fillMaxWidth(0.92f).padding(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(Modifier.padding(24.dp)) {
                Text("Setup Awal", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F7A5A))
                Text("Langkah ${step + 1} dari 3", fontSize = 12.sp, color = Color.Gray)
                Spacer(Modifier.height(12.dp))
                LinearProgressIndicator(
                    progress = (step + 1) / 3f,
                    modifier = Modifier.fillMaxWidth(),
                    color = Color(0xFF0F7A5A)
                )
                Spacer(Modifier.height(24.dp))

                when (step) {
                    0 -> {
                        Text("Izin Lokasi", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Aplikasi ini perlu tahu lokasi Anda agar Tour Leader dapat menemukan Anda jika terpisah dari rombongan.",
                            fontSize = 13.sp, color = Color(0xFF374151)
                        )
                        Spacer(Modifier.height(16.dp))
                        StatusRow("Lokasi diizinkan", locGranted)
                        Spacer(Modifier.height(16.dp))
                        Button(
                            onClick = {
                                if (!locGranted) {
                                    locLauncher.launch(arrayOf(
                                        android.Manifest.permission.ACCESS_FINE_LOCATION,
                                        android.Manifest.permission.ACCESS_COARSE_LOCATION
                                    ))
                                } else {
                                    step = 1
                                }
                            },
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F7A5A)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(if (locGranted) "Lanjut" else "Izinkan Lokasi", fontWeight = FontWeight.Bold)
                        }
                    }
                    1 -> {
                        Text("Izin Notifikasi", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Notifikasi diperlukan agar Anda tetap bisa dihubungi Tour Leader.",
                            fontSize = 13.sp, color = Color(0xFF374151)
                        )
                        Spacer(Modifier.height(16.dp))
                        StatusRow("Notifikasi diizinkan", notifGranted)
                        Spacer(Modifier.height(16.dp))
                        Button(
                            onClick = {
                                if (!notifGranted && Build.VERSION.SDK_INT >= 33) {
                                    notifLauncher.launch("android.permission.POST_NOTIFICATIONS")
                                } else {
                                    step = 2
                                }
                            },
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F7A5A)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(if (notifGranted) "Lanjut" else "Izinkan Notifikasi", fontWeight = FontWeight.Bold)
                        }
                    }
                    2 -> {
                        Text("Optimasi Baterai", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Aktifkan agar HP Anda tidak mematikan aplikasi saat tidur, sehingga lokasi tetap bisa dicari Tour Leader.",
                            fontSize = 13.sp, color = Color(0xFF374151)
                        )
                        Spacer(Modifier.height(16.dp))
                        StatusRow("Baterai tidak dibatasi", batteryOk)
                        Spacer(Modifier.height(16.dp))
                        Button(
                            onClick = {
                                if (!batteryOk) {
                                    requestIgnoreBattery(context)
                                    batteryOk = isBatteryUnrestricted(context)
                                }
                            },
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F7A5A)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Buka Pengaturan Baterai", fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.height(8.dp))
                        Button(
                            onClick = onFinish,
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF374151)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Selesai", fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))
                TextButton(onClick = onSkip, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                    Text("Lewati (bisa diaktifkan nanti)", fontSize = 11.sp, color = Color.Gray)
                }
            }
        }
    }
}

@Composable
private fun StatusRow(label: String, ok: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = if (ok) Icons.Default.CheckCircle else Icons.Default.Info,
            contentDescription = null,
            tint = if (ok) Color(0xFF0F7A5A) else Color(0xFFB45309),
            modifier = Modifier.size(20.dp)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            label + (if (ok) " - OK" else " - belum"),
            fontSize = 13.sp,
            color = if (ok) Color(0xFF0F7A5A) else Color(0xFFB45309)
        )
    }
}

private fun hasFineLocation(ctx: Context): Boolean =
    ContextCompat.checkSelfPermission(ctx, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED

private fun hasNotifPermission(ctx: Context): Boolean {
    return if (Build.VERSION.SDK_INT >= 33)
        ContextCompat.checkSelfPermission(ctx, "android.permission.POST_NOTIFICATIONS") == PackageManager.PERMISSION_GRANTED
    else true
}

private fun isBatteryUnrestricted(ctx: Context): Boolean {
    val pm = ctx.getSystemService(Context.POWER_SERVICE) as PowerManager
    return pm.isIgnoringBatteryOptimizations(ctx.packageName)
}

private fun requestIgnoreBattery(ctx: Context) {
    try {
        val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
            data = Uri.parse("package:${ctx.packageName}")
        }
        ctx.startActivity(intent)
    } catch (e: Exception) {
        try {
            ctx.startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
        } catch (_: Exception) {}
    }
}
