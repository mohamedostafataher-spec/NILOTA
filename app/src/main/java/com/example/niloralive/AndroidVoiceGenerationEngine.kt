package com.example.niloralive

import android.content.Context
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import com.example.data.model.NiloraLanguage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

class AndroidVoiceGenerationEngine(context: Context) : VoiceGenerationEngine {
    private val appContext = context.applicationContext
    private var tts: TextToSpeech? = null
    private var lastText: String = ""
    private var lastLang: NiloraLanguage = NiloraLanguage.ENGLISH
    private var activeOnFinished: (() -> Unit)? = null

    private val _capabilityReport = MutableStateFlow(
        VoiceCapabilityReport(
            ttsInitialized = false,
            installedVoiceCount = 0,
            hasExactEgyptianArabicVoice = false,
            bestArabicVoiceName = "Inspecting...",
            hasEnglishVoice = false,
            hasRussianVoice = false,
            hasFrenchVoice = false,
            notes = "Initializing Android TextToSpeech..."
        )
    )
    override val capabilityReport: StateFlow<VoiceCapabilityReport> = _capabilityReport.asStateFlow()

    private val _availableVoices = MutableStateFlow<List<TtsVoiceOption>>(emptyList())
    override val availableVoices: StateFlow<List<TtsVoiceOption>> = _availableVoices.asStateFlow()

    private val _selectedVoiceName = MutableStateFlow("")
    override val selectedVoiceName: StateFlow<String> = _selectedVoiceName.asStateFlow()

    private val _isSpeaking = MutableStateFlow(false)
    override val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    private val _isMuted = MutableStateFlow(false)
    override val isMuted: StateFlow<Boolean> = _isMuted.asStateFlow()

    private val _speed = MutableStateFlow(1.0f)
    override val speed: StateFlow<Float> = _speed.asStateFlow()

    private val _volume = MutableStateFlow(1.0f)
    override val volume: StateFlow<Float> = _volume.asStateFlow()

    init {
        try {
            tts = TextToSpeech(appContext) { status ->
                val ok = status == TextToSpeech.SUCCESS
                if (ok) {
                    inspectVoices()
                } else {
                    _capabilityReport.value = VoiceCapabilityReport(
                        ttsInitialized = false,
                        installedVoiceCount = 0,
                        hasExactEgyptianArabicVoice = false,
                        bestArabicVoiceName = "Unavailable",
                        hasEnglishVoice = false,
                        hasRussianVoice = false,
                        hasFrenchVoice = false,
                        notes = "Android TextToSpeech engine failed to initialize (status $status)."
                    )
                }
            }
        } catch (e: Exception) {
            Log.w("VoiceGenEngine", "TTS init failed: ${e.message}")
            _capabilityReport.value = _capabilityReport.value.copy(
                ttsInitialized = false,
                notes = e.localizedMessage ?: "TextToSpeech unavailable."
            )
        }
    }

    private fun inspectVoices() {
        val engine = tts ?: return
        val voices = try {
            engine.voices?.toList().orEmpty()
        } catch (_: Exception) {
            emptyList()
        }
        _availableVoices.value = voices
            .map { v ->
                TtsVoiceOption(
                    name = v.name,
                    localeTag = v.locale.toLanguageTag(),
                    isNetworkRequired = v.isNetworkConnectionRequired
                )
            }
            .sortedWith(compareBy({ it.localeTag }, { it.name }))

        val exactEgyptVoice = voices.firstOrNull {
            val tag = it.locale.toLanguageTag().lowercase()
            tag == "ar-eg" || (it.locale.language.equals("ar", true) && it.locale.country.equals("EG", true))
        }
        val genericArabicVoice = voices.firstOrNull {
            it.locale.language.equals("ar", true)
        }
        val hasEn = voices.any { it.locale.language.equals("en", true) } ||
            engine.isLanguageAvailable(Locale.US) >= TextToSpeech.LANG_AVAILABLE
        val hasRu = voices.any { it.locale.language.equals("ru", true) } ||
            engine.isLanguageAvailable(Locale.forLanguageTag("ru-RU")) >= TextToSpeech.LANG_AVAILABLE
        val hasFr = voices.any { it.locale.language.equals("fr", true) } ||
            engine.isLanguageAvailable(Locale.FRANCE) >= TextToSpeech.LANG_AVAILABLE

        val arabicDesc = when {
            exactEgyptVoice != null -> "Egyptian Arabic (${exactEgyptVoice.name})"
            genericArabicVoice != null -> "Generic Arabic (${genericArabicVoice.locale.toLanguageTag()}) — no dedicated ar-EG voice installed"
            else -> "No Arabic voice package installed"
        }

        val notes = buildString {
            if (exactEgyptVoice != null) {
                append("Dedicated ar-EG voice detected.")
            } else if (genericArabicVoice != null) {
                append("Using generic Arabic voice (${genericArabicVoice.locale.toLanguageTag()}); device does not expose a dedicated Egyptian ar-EG voice.")
            } else {
                append("Arabic TTS voice missing on device; install Arabic speech data in Android Settings.")
            }
            val missing = buildList {
                if (!hasEn) add("en")
                if (!hasRu) add("ru")
                if (!hasFr) add("fr")
            }
            if (missing.isNotEmpty()) {
                append(" Missing voice packages: ${missing.joinToString()}.")
            }
        }

        _capabilityReport.value = VoiceCapabilityReport(
            ttsInitialized = true,
            installedVoiceCount = voices.size,
            hasExactEgyptianArabicVoice = exactEgyptVoice != null,
            bestArabicVoiceName = arabicDesc,
            hasEnglishVoice = hasEn,
            hasRussianVoice = hasRu,
            hasFrenchVoice = hasFr,
            notes = notes
        )
    }

    override fun selectVoice(voiceName: String) {
        _selectedVoiceName.value = voiceName
    }

    override fun speak(
        text: String,
        language: NiloraLanguage,
        onStarted: () -> Unit,
        onFinished: () -> Unit,
        onError: (String) -> Unit
    ) {
        if (text.isBlank()) {
            onError("No text to speak.")
            return
        }
        lastText = text
        lastLang = language
        if (_isMuted.value) {
            onFinished()
            return
        }
        val engine = tts
        if (engine == null || !_capabilityReport.value.ttsInitialized) {
            onError("TextToSpeech engine is not ready on this device. Subtitles remain visible.")
            return
        }
        try {
            val targetLocale = selectBestLocale(engine, language)
            val langSupport = engine.setLanguage(targetLocale)
            if (langSupport == TextToSpeech.LANG_MISSING_DATA || langSupport == TextToSpeech.LANG_NOT_SUPPORTED) {
                onError("Voice package for ${language.displayName} (${targetLocale.toLanguageTag()}) is not installed — using live subtitles.")
                return
            }

            engine.setSpeechRate(_speed.value)
            activeOnFinished = onFinished
            engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    _isSpeaking.value = true
                    onStarted()
                }

                override fun onDone(utteranceId: String?) {
                    _isSpeaking.value = false
                    activeOnFinished?.invoke()
                    activeOnFinished = null
                }

                @Deprecated("Deprecated in Java")
                override fun onError(utteranceId: String?) {
                    _isSpeaking.value = false
                    activeOnFinished = null
                    onError("Audio synthesis error during playback.")
                }

                override fun onError(utteranceId: String?, errorCode: Int) {
                    _isSpeaking.value = false
                    activeOnFinished = null
                    onError("Audio synthesis failed (code $errorCode).")
                }
            })

            val params = Bundle().apply {
                putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, _volume.value.coerceIn(0f, 1f))
            }
            val utteranceId = "nilora_tts_${System.currentTimeMillis()}"
            engine.speak(text, TextToSpeech.QUEUE_FLUSH, params, utteranceId)
        } catch (e: Exception) {
            _isSpeaking.value = false
            onError(e.localizedMessage ?: "Failed to generate audio.")
        }
    }

    private fun selectBestLocale(engine: TextToSpeech, language: NiloraLanguage): Locale {
        val voices = try {
            engine.voices?.toList().orEmpty()
        } catch (_: Exception) {
            emptyList()
        }
        val preferredName = _selectedVoiceName.value
        if (preferredName.isNotBlank()) {
            val matchedVoice = voices.firstOrNull {
                it.name == preferredName && it.locale.language.equals(language.code, ignoreCase = true)
            }
            if (matchedVoice != null) {
                engine.voice = matchedVoice
                return matchedVoice.locale
            }
        }
        if (language != NiloraLanguage.ARABIC) {
            val matchingVoice = voices.firstOrNull {
                it.locale.language.equals(language.code, ignoreCase = true)
            }
            if (matchingVoice != null) {
                engine.voice = matchingVoice
                return matchingVoice.locale
            }
            return Locale.forLanguageTag(language.bcp47Tag)
        }
        val exactEg = voices.firstOrNull {
            it.locale.language.equals("ar", true) && it.locale.country.equals("EG", true)
        }
        if (exactEg != null) {
            engine.voice = exactEg
            return exactEg.locale
        }
        val anyArabic = voices.firstOrNull { it.locale.language.equals("ar", true) }
        if (anyArabic != null) {
            engine.voice = anyArabic
            return anyArabic.locale
        }
        return Locale.forLanguageTag("ar-EG")
    }

    override fun pauseOrStop() {
        try {
            tts?.stop()
        } catch (_: Exception) {
        }
        _isSpeaking.value = false
    }

    override fun replayLast(onStarted: () -> Unit, onFinished: () -> Unit, onError: (String) -> Unit) {
        if (lastText.isBlank()) {
            onError("No previous translated message to replay.")
            return
        }
        speak(lastText, lastLang, onStarted, onFinished, onError)
    }

    override fun toggleMute() {
        _isMuted.value = !_isMuted.value
        if (_isMuted.value) {
            pauseOrStop()
        }
    }

    override fun setSpeed(value: Float) {
        _speed.value = value.coerceIn(0.6f, 1.6f)
    }

    override fun setVolume(value: Float) {
        _volume.value = value.coerceIn(0f, 1f)
    }

    override fun release() {
        try {
            tts?.stop()
            tts?.shutdown()
        } catch (_: Exception) {
        }
        tts = null
        _isSpeaking.value = false
    }
}
