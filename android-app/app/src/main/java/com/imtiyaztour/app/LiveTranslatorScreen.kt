package com.imtiyaztour.app

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LiveTranslatorScreen(onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var inputText by remember { mutableStateOf("") }
    var outputText by remember { mutableStateOf("") }
    var isConnected by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf("") }
    var translator by remember { mutableStateOf<GeminiLiveTranslator?>(null) }

    suspend fun fetchEphemeralToken(): String {
        return withContext(Dispatchers.IO) {
            val client = OkHttpClient()
            val request = Request.Builder()
                .url("https://bahasa.pastiumrah.com/gemini-token")
                .post("{}".toRequestBody("application/json".toMediaType()))
                .build()
            val response = client.newCall(request).execute()
            val json = JSONObject(response.body?.string() ?: "{}")
            if (json.has("token")) json.getString("token") else throw Exception("Gagal dapat token")
        }
    }

    // Fungsi untuk memulai koneksi
    fun startConnection() {
        scope.launch {
            try {
                val token = fetchEphemeralToken()
                translator = GeminiLiveTranslator(
                    context = context,
                    ephemeralToken = token,
                    onTranscript = { text, isInput ->
                        if (isInput) inputText = text else outputText = text
                    },
                    onError = { errorMsg = it; isConnected = false }
                ).also { it.connect() }
                isConnected = true
            } catch (e: Exception) {
                errorMsg = "Gagal memulai: ${e.message}"
            }
        }
    }

    // Launcher untuk meminta izin mikrofon
    val micPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            startConnection()
        } else {
            errorMsg = "Izin mikrofon ditolak. Aplikasi membutuhkan mikrofon untuk menerjemahkan."
        }
    }



    Column(Modifier.fillMaxSize().padding(24.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
            IconButton(onClick = { translator?.stop(); onBack() }) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Kembali", tint = Color(0xFF0F7A5A))
            }
        }

        Spacer(Modifier.height(16.dp))
        Text("Asisten Bahasa", fontWeight = FontWeight.Bold, fontSize = 24.sp, color = Color(0xFF0F7A5A))
        Text("Tekan tombol dan mulai berbicara", fontSize = 12.sp, color = Color.Gray)

        Spacer(Modifier.height(24.dp))

        if (inputText.isNotBlank()) {
            Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0xFFF0FDF4))) {
                Column(Modifier.padding(12.dp)) {
                    Text("Anda:", fontSize = 10.sp, color = Color.Gray)
                    Text(inputText, fontSize = 16.sp)
                }
            }
            Spacer(Modifier.height(8.dp))
        }
        if (outputText.isNotBlank()) {
            Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0xFFEFF6FF))) {
                Column(Modifier.padding(12.dp)) {
                    Text("Terjemahan (Arab):", fontSize = 10.sp, color = Color.Gray)
                    Text(outputText, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(Modifier.weight(1f))

        Button(
            onClick = {
                if (!isConnected) {
                    // Cek izin mikrofon terlebih dahulu
                    val hasPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
                    if (hasPermission) {
                        startConnection()
                    } else {
                        micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    }
                } else {
                    translator?.stop()
                    isConnected = false
                }
            },
            modifier = Modifier.size(80.dp),
            shape = CircleShape,
            colors = ButtonDefaults.buttonColors(containerColor = if (isConnected) Color.Red else Color(0xFF0F7A5A))
        ) {
            Icon(
                if (isConnected) Icons.Default.Stop else Icons.Default.Mic,
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = Color.White
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(if (isConnected) "Mendengarkan..." else "Tekan untuk memulai", fontSize = 12.sp, color = Color.Gray)

        if (errorMsg.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            Text(errorMsg, fontSize = 11.sp, color = Color.Red)
        }
    }
}
