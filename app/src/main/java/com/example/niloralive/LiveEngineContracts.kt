package com.example.niloralive

import com.example.data.model.NiloraLanguage
import kotlinx.coroutines.flow.StateFlow

enum class PipelineStage(val label: String) {
    IDLE("Ready"),
    LISTENING("Listening"),
    SPEECH_DETECTED("Speech detected"),
    RECOGNIZING("Recognizing"),
    AWAITING_CORRECTION("Review & confirm transcript"),
    TRANSLATING("Translating"),
    VALIDATING("Validating translation"),
    GENERATING_VOICE("Generating voice"),
    PLAYING("Playing"),
    PAUSED("Paused"),
    ERROR("Error")
}

enum class ArabicDialectMode(val label: String) {
    EGYPTIAN_COLLOQUIAL("Egyptian Colloquial (ar-EG)"),
    MODERN_STANDARD("Modern Standard Arabic (ar-MSA)")
}

enum class TranslationContextDomain(val label: String, val directive: String) {
    CASUAL("Casual Chat", "Natural warm everyday conversation"),
    TRAVEL("Travel & Logistics", "Tickets, pricing, directions, transport, safety, and bookings"),
    TOURISM("Tourism & Landmarks", "Cultural heritage, museums, beaches, and guided commentary"),
    SPORTS("Sports & Active Play", "Team coordination, rules, equipment, meeting spots, and timing"),
    GROUP_ACTIVITIES("Group Live & Crews", "Multi-speaker events, scheduling, invitations, and introductions"),
    CULTURAL("Cultural Exchange", "Idioms, local expressions, traditions, and food storytelling"),
    ADVENTURE_INSTRUCTIONS("Adventure & Safety", "Clear spatial cues, discovery prompts, and safety guidance")
}

enum class SubtitleDisplayMode(val key: String, val label: String) {
    BOTH("BOTH", "Original + Translated"),
    TRANSLATED_ONLY("TRANSLATED_ONLY", "Translated Only"),
    ORIGINAL_ONLY("ORIGINAL_ONLY", "Original Only"),
    NONE("NONE", "Subtitles Off");

    companion object {
        fun fromKey(key: String?): SubtitleDisplayMode =
            entries.firstOrNull { it.key == key } ?: BOTH
    }
}

data class TtsVoiceOption(
    val name: String,
    val localeTag: String,
    val isNetworkRequired: Boolean = false
)

data class HardwareAndSpeechCapabilities(
    val micPermissionGranted: Boolean,
    val speechRecognizerAvailable: Boolean,
    val recognitionServicePackages: List<String>,
    val audioRecord16kSupported: Boolean,
    val hardwareNoiseSuppressorAvailable: Boolean,
    val hardwareEchoCancelerAvailable: Boolean,
    val totalRamMb: Long,
    val cpuCores: Int,
    val supportedAbis: List<String>,
    val canHostWhisperTinyInTheory: Boolean,
    val canHostWhisperSmallInTheory: Boolean,
    val whisperNativeBinaryBundled: Boolean = false
)

data class ModelSpecInfo(
    val id: String,
    val name: String,
    val role: String,
    val license: String,
    val sizeEstimate: String,
    val hardwareRequirement: String,
    val supportedLanguages: String,
    val runtimeStatus: String,
    val limitations: String
)

data class SpeechRecognitionResult(
    val transcript: String,
    val isFinal: Boolean,
    val startedAtMs: Long,
    val endedAtMs: Long,
    val latencyMs: Long,
    val confidenceScore: Float?
)

data class TranslationRequest(
    val sourceText: String,
    val sourceLanguage: NiloraLanguage,
    val targetLanguage: NiloraLanguage,
    val arabicDialectMode: ArabicDialectMode = ArabicDialectMode.EGYPTIAN_COLLOQUIAL,
    val preserveAmbiguity: Boolean = true,
    val conversationContext: List<String> = emptyList(),
    val domain: TranslationContextDomain = TranslationContextDomain.CASUAL,
    val autoDetectSource: Boolean = false
)

data class TranslationOutput(
    val translatedText: String,
    val detectedSourceDialect: String,
    val confidenceLabel: String, // HIGH | MEDIUM | LOW
    val ambiguityWarning: String,
    val validationWarnings: List<String>,
    val latencyMs: Long
)

data class VoiceCapabilityReport(
    val ttsInitialized: Boolean,
    val installedVoiceCount: Int,
    val hasExactEgyptianArabicVoice: Boolean,
    val bestArabicVoiceName: String,
    val hasEnglishVoice: Boolean,
    val hasRussianVoice: Boolean,
    val hasFrenchVoice: Boolean,
    val notes: String
)

interface SpeechRecognitionEngine {
    val rmsNormalized: StateFlow<Float>
    val isRecognizing: StateFlow<Boolean>
    fun isAvailable(): Boolean
    fun startRecognition(
        language: NiloraLanguage,
        onSpeechDetected: () -> Unit,
        onPartialResult: (String) -> Unit,
        onFinalResult: (SpeechRecognitionResult) -> Unit,
        onError: (String) -> Unit
    )
    fun stopRecognition()
    fun release()
}

interface TranslationEngine {
    val engineId: String
    suspend fun translate(request: TranslationRequest): Result<TranslationOutput>
}

interface VoiceGenerationEngine {
    val capabilityReport: StateFlow<VoiceCapabilityReport>
    val availableVoices: StateFlow<List<TtsVoiceOption>>
    val selectedVoiceName: StateFlow<String>
    val isSpeaking: StateFlow<Boolean>
    val isMuted: StateFlow<Boolean>
    val speed: StateFlow<Float>
    val volume: StateFlow<Float>
    fun speak(
        text: String,
        language: NiloraLanguage,
        onStarted: () -> Unit = {},
        onFinished: () -> Unit = {},
        onError: (String) -> Unit = {}
    )
    fun selectVoice(voiceName: String)
    fun pauseOrStop()
    fun replayLast(onStarted: () -> Unit = {}, onFinished: () -> Unit = {}, onError: (String) -> Unit = {})
    fun toggleMute()
    fun setSpeed(value: Float)
    fun setVolume(value: Float)
    fun release()
}
