package com.imtiyaztour.app

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

@Composable
fun RadioScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var hasMicPermission by remember {
        mutableStateOf(context.checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED)
    }
    var pendingStartKode by remember { mutableStateOf("") }
    var kodeGelombang by remember { mutableStateOf(Prefs.getActiveKodeGelombang(context)) }
    var gelombangError by remember { mutableStateOf("") }
    var liveStatus by remember { mutableStateOf("") }

    val micPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        hasMicPermission = granted
        if (!granted) {
            pendingStartKode = ""
            liveStatus = "Izin mikrofon diperlukan untuk siaran TL."
        }
    }

    LaunchedEffect(hasMicPermission, pendingStartKode) {
        val kode = pendingStartKode
        if (hasMicPermission && kode.isNotBlank()) {
            pendingStartKode = ""
            RadioLiveKit.mulai(
                context = context,
                jamaahId = Prefs.getJamaahId(context),
                token = Prefs.getToken(context),
                kodeGelombang = kode,
                onStatus = { liveStatus = it }
            )
        }
    }

    BackHandler(enabled = true) {
        if (RadioLiveKit.sedangAktif()) RadioLiveKit.berhenti()
        onBack()
    }

    Column(modifier = Modifier.padding(16.dp)) {
        Text("Radio Imtiyaz")
        val displayedError = if (kodeGelombang.isBlank()) {
            "Masukkan kode gelombang terlebih dahulu"
        } else {
            gelombangError
        }
        if (displayedError.isNotBlank()) Text(displayedError)
        if (liveStatus.isNotBlank()) Text(liveStatus)

        Button(onClick = {
            val kode = kodeGelombang.trim()
            if (kode.isBlank()) {
                gelombangError = "Masukkan kode gelombang terlebih dahulu"
            } else {
                gelombangError = ""
                RadioLiveKit.mulai(
                    context = context,
                    jamaahId = Prefs.getJamaahId(context),
                    token = Prefs.getToken(context),
                    kodeGelombang = kode,
                    onMicPermissionRequired = {
                        pendingStartKode = kode
                        micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    },
                    onStatus = { liveStatus = it }
                )
            }
        }) {
            Text("Mulai Siaran Live")
        }

        if (RadioLiveKit.sedangAktif()) {
            Button(onClick = {
                RadioLiveKit.berhenti()
                liveStatus = "Radio dihentikan"
            }) {
                Text("Hentikan Radio")
            }
        }
    }
}
