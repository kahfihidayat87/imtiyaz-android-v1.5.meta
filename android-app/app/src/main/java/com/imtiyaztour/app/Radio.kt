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
        mutableStateOf(
            context.checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        )
    }

    val micPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        hasMicPermission = granted
    }

    var kodeGelombang by remember { mutableStateOf(Prefs.getActiveKodeGelombang(context)) }
    var gelombangError by remember { mutableStateOf("") }

    BackHandler(enabled = true) { onBack() }

    Column(modifier = Modifier.padding(16.dp)) {
        Text("Radio Imtiyaz")

        // Keep composition free of state writes. Updating gelombangError while
        // rendering caused a recomposition loop whenever the code was blank.
        val displayedGelombangError = if (kodeGelombang.isBlank()) {
            "Masukkan kode gelombang terlebih dahulu"
        } else {
            gelombangError
        }
        if (displayedGelombangError.isNotBlank()) {
            Text(displayedGelombangError)
        }

        Button(
            onClick = {
                val kode = kodeGelombang.trim()
                if (kode.isBlank()) {
                    gelombangError = "Masukkan kode gelombang terlebih dahulu"
                    return@Button
                }
                gelombangError = ""
                if (!hasMicPermission) {
                    micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    return@Button
                }
                RadioLiveKit.mulai(context, Prefs.getJamaahId(context), Prefs.getToken(context), kode) {
                    // Status updates are handled by LiveKit internally.
                }
            }
        ) {
            Text("Mulai Siaran Live")
        }
    }
}
