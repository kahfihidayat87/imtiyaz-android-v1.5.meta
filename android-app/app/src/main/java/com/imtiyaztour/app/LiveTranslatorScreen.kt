package com.imtiyaztour.app

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LiveTranslatorScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    var inputText by remember { mutableStateOf("") }
    var outputText by remember { mutableStateOf("") }
    var isConnected by remember { mutableStateOf(false) }
    var isConnecting by remember { mutableStateOf(false) }
    var isRecording by remember { mutableStateOf(false) }
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

    fun startConnection() {
        if (isConnecting || isConnected) return
        isConnecting = true
        errorMsg = ""
        scope.launch {
            try {
                val token = fetchEphemeralToken()
                translator = GeminiLiveTranslator(
                    context = context,
                    ephemeralToken = token,
                    onTranscript = { text, isInput ->
                        if (isInput) inputText = text else outputText = text
                    },
                    onError = { errorMsg = it; isConnected = false; isConnecting = false; isRecording = false }
                ).also { it.connect() }
                isConnected = true
            } catch (e: Exception) {
                errorMsg = "Gagal memulai: ${e.message}"
            } finally {
                isConnecting = false
            }
        }
    }

    val micPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            startConnection()
        } else {
            errorMsg = "Izin mikrofon ditolak."
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

        // Tombol Mic Pintar
        Box(
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .size(100.dp)
                .clip(CircleShape)
                .background(
                    when {
                        isRecording -> Color.Red
                        isConnected -> Color(0xFF0F7A5A)
                        isConnecting -> Color(0xFFF59E0B) // Kuning
                        else -> Color.Gray
                    }
                )
                .pointerInput(isConnected, isConnecting) {
                    detectTapGestures(
                        onPress = {
                            if (isConnected) {
                                isRecording = true
                                translator?.startAudioRecording()
                            } else if (!isConnecting) {
                                val hasPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
                                if (hasPermission) {
                                    startConnection()
                                } else {
                                    micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                }
                            }
                            tryAwaitRelease()
                            if (isConnected && isRecording) {
                                isRecording = false
                                translator?.stopRecording()
                            }
                        }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.Mic,
                contentDescription = "Tombol Mic",
                modifier = Modifier.size(48.dp),
                tint = Color.White
            )
        }
        Spacer(Modifier.height(12.dp))
        Text(
            text = when {
                isRecording -> "Merekam... Lepaskan untuk berhenti"
                isConnected -> "Tekan & Tahan untuk Bicara"
                isConnecting -> "Menghubungkan..."
                else -> "Tap untuk Mulai"
            },
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = if (isConnected || isRecording) Color(0xFF0F7A5A) else Color.DarkGray,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )

        if (errorMsg.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            Text(
                errorMsg,
                fontSize = 11.sp,
                color = Color.Red,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        }
    }
}
