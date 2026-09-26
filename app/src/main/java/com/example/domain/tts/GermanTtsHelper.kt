package com.example.domain.tts

import android.content.Context
import android.speech.tts.TextToSpeech
import android.util.Log
import java.util.Locale

class GermanTtsHelper(context: Context) : TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = TextToSpeech(context.applicationContext, this)
    private var isReady = false

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts?.setLanguage(Locale.GERMAN)
            isReady = result != TextToSpeech.LANG_MISSING_DATA && result != TextToSpeech.LANG_NOT_SUPPORTED
            if (!isReady) {
                // Fallback to default
                tts?.setLanguage(Locale.getDefault())
                isReady = true
            }
        } else {
            Log.e("GermanTtsHelper", "TTS Initialization failed: $status")
            isReady = false
        }
    }

    fun speak(text: String, speed: Float = 0.95f) {
        if (!isReady || tts == null) return
        val clean = text.replace(Regex("[,;()/\"]"), " ").trim()
        tts?.setSpeechRate(speed)
        tts?.speak(clean, TextToSpeech.QUEUE_FLUSH, null, "GermanSpeechUtterance")
    }

    fun stop() {
        tts?.stop()
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        tts = null
    }
}
