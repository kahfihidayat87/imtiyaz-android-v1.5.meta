package com.imtiyaztour.app

import android.media.MediaPlayer
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

// ============================================================================
// PEMUTAR AUDIO SEDERHANA -- dipakai bersama untuk audio Doa (Fitur 4) dan
// audio ayat Al-Quran (Fitur E-Quran). Pakai MediaPlayer bawaan Android supaya
// tidak perlu dependency baru (menghindari risiko gagal build seperti sebelumnya).
// Cuma satu track yang boleh main dalam satu waktu -- mulai track baru otomatis
// menghentikan yang sedang jalan.
// ============================================================================
object AudioPlayerManager {
    private var player: MediaPlayer? = null
    var currentlyPlayingUrl by mutableStateOf<String?>(null)
    var isLoading by mutableStateOf(false)
    var errorMessage by mutableStateOf<String?>(null)

    fun toggle(url: String) {
        if (currentlyPlayingUrl == url) {
            stop()
            return
        }
        play(url)
    }

    private fun play(url: String) {
        stop()
        errorMessage = null
        isLoading = true
        try {
            player = MediaPlayer().apply {
                setDataSource(url)
                setOnPreparedListener {
                    isLoading = false
                    it.start()
                }
                setOnCompletionListener { stop() }
                setOnErrorListener { _, _, _ ->
                    isLoading = false
                    errorMessage = "Gagal memutar audio -- periksa koneksi internet"
                    stop()
                    true
                }
                prepareAsync()
            }
            currentlyPlayingUrl = url
        } catch (e: Exception) {
            isLoading = false
            errorMessage = "Gagal memutar audio: ${e.message}"
            currentlyPlayingUrl = null
        }
    }

    fun stop() {
        try { player?.stop(); player?.release() } catch (e: Exception) {}
        player = null
        currentlyPlayingUrl = null
        isLoading = false
    }
}
