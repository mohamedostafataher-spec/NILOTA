package com.example.niloralive

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import com.example.data.model.NiloraLanguage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AndroidSpeechRecognitionEngine(context: Context) : SpeechRecognitionEngine {
    private val appContext = context.applicationContext
    private var recognizer: SpeechRecognizer? = null
    private var listenStartMs: Long = 0L

    private val _rmsNormalized = MutableStateFlow(0f)
    override val rmsNormalized: StateFlow<Float> = _rmsNormalized.asStateFlow()

    private val _isRecognizing = MutableStateFlow(false)
    override val isRecognizing: StateFlow<Boolean> = _isRecognizing.asStateFlow()

    override fun isAvailable(): Boolean = try {
        SpeechRecognizer.isRecognitionAvailable(appContext)
    } catch (_: Exception) {
        false
    }

    override fun startRecognition(
        language: NiloraLanguage,
        onSpeechDetected: () -> Unit,
        onPartialResult: (String) -> Unit,
        onFinalResult: (SpeechRecognitionResult) -> Unit,
        onError: (String) -> Unit
    ) {
        if (!isAvailable()) {
            onError("Speech recognition service unavailable on this device. Install/enable Google Speech Services or edit/type text manually.")
            return
        }
        try {
            recognizer?.destroy()
            listenStartMs = System.currentTimeMillis()
            _isRecognizing.value = true
            _rmsNormalized.value = 0f

            recognizer = SpeechRecognizer.createSpeechRecognizer(appContext).apply {
                setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) {
                        listenStartMs = System.currentTimeMillis()
                    }

                    override fun onBeginningOfSpeech() {
                        onSpeechDetected()
                    }

                    override fun onRmsChanged(rmsdB: Float) {
                        // Typical Android SpeechRecognizer rmsdB ranges roughly -2 dB to +10 dB
                        val norm = ((rmsdB + 2f) / 12f).coerceIn(0f, 1f)
                        _rmsNormalized.value = norm
                    }

                    override fun onBufferReceived(buffer: ByteArray?) {}
                    override fun onEndOfSpeech() {
                        _rmsNormalized.value = 0f
                    }

                    override fun onError(error: Int) {
                        _isRecognizing.value = false
                        _rmsNormalized.value = 0f
                        val msg = when (error) {
                            SpeechRecognizer.ERROR_AUDIO -> "Audio capture failed. Check device microphone."
                            SpeechRecognizer.ERROR_CLIENT -> "Speech recognizer client error."
                            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Microphone permission is required."
                            SpeechRecognizer.ERROR_NETWORK, SpeechRecognizer.ERROR_NETWORK_TIMEOUT ->
                                "Speech recognition network error. Check connectivity or install offline language packs."
                            SpeechRecognizer.ERROR_NO_MATCH ->
                                "No intelligible speech recognized. Speak clearly or correct text manually."
                            SpeechRecognizer.ERROR_RECOGNIZER_BUSY ->
                                "Speech recognizer is busy. Wait a moment and retry."
                            SpeechRecognizer.ERROR_SERVER -> "Speech recognition server error."
                            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech detected before timeout."
                            else -> "Speech recognition failed (error code $error)."
                        }
                        onError(msg)
                    }

                    override fun onResults(results: Bundle?) {
                        _isRecognizing.value = false
                        _rmsNormalized.value = 0f
                        val endMs = System.currentTimeMillis()
                        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        val scores = results?.getFloatArray(SpeechRecognizer.CONFIDENCE_SCORES)
                        val transcript = matches?.firstOrNull().orEmpty().trim()
                        val confidence = scores?.firstOrNull()
                        if (transcript.isBlank()) {
                            onError("Empty transcription returned. Please speak again or enter text manually.")
                        } else {
                            onFinalResult(
                                SpeechRecognitionResult(
                                    transcript = transcript,
                                    isFinal = true,
                                    startedAtMs = listenStartMs,
                                    endedAtMs = endMs,
                                    latencyMs = (endMs - listenStartMs).coerceAtLeast(0L),
                                    confidenceScore = confidence
                                )
                            )
                        }
                    }

                    override fun onPartialResults(partialResults: Bundle?) {
                        val partial = partialResults
                            ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                            ?.firstOrNull()
                            .orEmpty()
                            .trim()
                        if (partial.isNotBlank()) {
                            onPartialResult(partial)
                        }
                    }

                    override fun onEvent(eventType: Int, params: Bundle?) {}
                })
            }

            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, language.bcp47Tag)
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 1400L)
            }
            recognizer?.startListening(intent)
        } catch (e: Exception) {
            _isRecognizing.value = false
            _rmsNormalized.value = 0f
            onError(e.localizedMessage ?: "Failed to start SpeechRecognizer.")
        }
    }

    override fun stopRecognition() {
        try {
            recognizer?.stopListening()
        } catch (_: Exception) {
        }
        _isRecognizing.value = false
        _rmsNormalized.value = 0f
    }

    override fun release() {
        try {
            recognizer?.destroy()
        } catch (_: Exception) {
        }
        recognizer = null
        _isRecognizing.value = false
        _rmsNormalized.value = 0f
    }
}
