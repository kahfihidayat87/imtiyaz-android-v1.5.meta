package com.imtiyaztour.app

import android.content.Context
import android.speech.tts.TextToSpeech
import java.util.Locale

/**
 * Wrapper TTS bahasa Indonesia untuk simulasi penerbangan.
 * Tanpa file audio, tanpa copyright — pakai engine TTS bawaan Android.
 */
object TerbangKeMakkahTts {
    private var tts: TextToSpeech? = null
    private var ready = false
    private var pending: String? = null

    fun init(ctx: Context) {
        if (tts != null) return
        tts = TextToSpeech(ctx.applicationContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale("id", "ID")
                tts?.setPitch(0.92f)
                tts?.setSpeechRate(0.94f)
                ready = true
                pending?.let { speak(it) }
                pending = null
            }
        }
    }

    fun speak(text: String, flush: Boolean = true) {
        if (!ready || tts == null) { pending = text; return }
        val mode = if (flush) TextToSpeech.QUEUE_FLUSH else TextToSpeech.QUEUE_ADD
        tts?.speak(text, mode, null, "tkm_${System.currentTimeMillis()}")
    }

    fun stop() { tts?.stop() }

    fun shutdown() {
        try { tts?.shutdown() } catch (_: Exception) {}
        tts = null; ready = false
    }
}
