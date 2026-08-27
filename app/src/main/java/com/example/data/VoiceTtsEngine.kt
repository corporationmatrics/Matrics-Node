package com.example.data

import android.content.Context
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import java.util.Locale

/**
 * Text-to-Speech engine supporting Indian English and regional accents,
 * with completion callbacks to trigger bidirectional speech-listening loops.
 */
class VoiceTtsEngine(
    private val context: Context,
    private val onTtsReady: (() -> Unit)? = null
) : TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null
    private var isInitialized = false
    private var onSpeechDoneCallback: (() -> Unit)? = null

    init {
        tts = TextToSpeech(context, this)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts?.setLanguage(Locale("en", "IN"))
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                tts?.language = Locale.US
            }
            tts?.setPitch(1.05f)
            tts?.setSpeechRate(1.02f)

            tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    Log.d("VoiceTtsEngine", "TTS started: $utteranceId")
                }

                override fun onDone(utteranceId: String?) {
                    Log.d("VoiceTtsEngine", "TTS completed: $utteranceId")
                    onSpeechDoneCallback?.invoke()
                }

                @Deprecated("Deprecated in Java")
                override fun onError(utteranceId: String?) {
                    Log.e("VoiceTtsEngine", "TTS error on: $utteranceId")
                    onSpeechDoneCallback?.invoke()
                }

                override fun onError(utteranceId: String?, errorCode: Int) {
                    Log.e("VoiceTtsEngine", "TTS error code $errorCode on: $utteranceId")
                    onSpeechDoneCallback?.invoke()
                }
            })

            isInitialized = true
            onTtsReady?.invoke()
        } else {
            Log.e("VoiceTtsEngine", "TTS initialization failed status: $status")
            isInitialized = false
        }
    }

    fun speak(
        text: String,
        utteranceId: String = "cyphr_tts_${System.currentTimeMillis()}",
        onDone: (() -> Unit)? = null
    ) {
        if (!isInitialized || tts == null) {
            onDone?.invoke()
            return
        }

        onSpeechDoneCallback = onDone
        val params = Bundle().apply {
            putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, utteranceId)
        }
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, params, utteranceId)
    }

    fun stop() {
        try {
            tts?.stop()
        } catch (e: Exception) {
            Log.e("VoiceTtsEngine", "Error stopping TTS", e)
        }
    }

    fun shutdown() {
        try {
            tts?.stop()
            tts?.shutdown()
            tts = null
            isInitialized = false
        } catch (e: Exception) {
            Log.e("VoiceTtsEngine", "Error shutting down TTS", e)
        }
    }
}
