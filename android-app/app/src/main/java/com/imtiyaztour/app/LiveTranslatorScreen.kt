package com.imtiyaztour.app

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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LiveTranslatorScreen(onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    var inputText by remember { mutableStateOf("") }
    var outputText by remember { mutableStateOf("") }
    var isConnected by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf("") }
    var translator by remember { mutableStateOf<GeminiLiveTranslator?>(null) }

    suspend fun fetchEphemeralToken(): String {
        // TODO: Hubungkan dengan endpoint backend Anda nanti
        // Sementara, gunakan string statis agar compile sukses
        return "TOKEN_DARI_SERVER_ANDA_DISINI"
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
                    scope.launch {
                        try {
                            val token = fetchEphemeralToken()
                            translator = GeminiLiveTranslator(
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
