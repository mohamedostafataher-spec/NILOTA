package com.example.ui.viewmodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.*
import com.example.data.repository.*
import com.example.niloralive.*
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class SharedTranslationMessage(
    val id: String,
    val speakerSide: String, // TOP (Person A) | BOTTOM (Person B)
    val sourceLang: NiloraLanguage,
    val targetLang: NiloraLanguage,
    val dialectMode: ArabicDialectMode,
    val originalText: String,
    val translatedText: String,
    val detectedDialect: String,
    val confidence: String,
    val warning: String,
    val sttLatencyMs: Long,
    val translationLatencyMs: Long
)

sealed interface AssistantMessageRole {
    data object User : AssistantMessageRole
    data object Assistant : AssistantMessageRole
}

data class AssistantChatMessage(
    val id: String,
    val role: AssistantMessageRole,
    val text: String,
    val isStreaming: Boolean = false
)

data class TranslatorUiState(
    val pipelineStage: PipelineStage = PipelineStage.IDLE,
    val pipelineError: String? = null,
    val partialTranscript: String = "",
    val pendingEditableTranscript: String = "",
    val pendingSpeakerSide: String = "BOTTOM", // TOP | BOTTOM | ROOM
    val lastSttLatencyMs: Long = 0L,
    val lastSttConfidence: Float? = null,
    val arabicDialectMode: ArabicDialectMode = ArabicDialectMode.EGYPTIAN_COLLOQUIAL,
    val autoTranslateAfterSpeech: Boolean = true,
    val voiceConsentGranted: Boolean = false,
    val rotateTopPanel: Boolean = true,
    // Universal One Language Identity & Live Subtitles / Audio Controls
    val spokenLanguageCode: String = "en",
    val targetTranslationLanguageCode: String = "ar-EG",
    val subtitleDisplayMode: SubtitleDisplayMode = SubtitleDisplayMode.BOTH,
    val preferOriginalAudio: Boolean = false,
    val selectedTtsVoiceName: String = "",
    val translationDomain: TranslationContextDomain = TranslationContextDomain.CASUAL,
    val autoDetectSource: Boolean = false,
    // Two-Phone Room state
    val activeRoom: ConversationRoom? = null,
    val roomMessages: List<ConversationMessage> = emptyList(),
    val isConnectingRoom: Boolean = false,
    val roomConnectionStatus: String = "DISCONNECTED", // DISCONNECTED | CONNECTING | CONNECTED | RECONNECTING | ENDED
    val roomError: String? = null,
    // One-Phone Shared Translator state
    val topLanguage: NiloraLanguage = NiloraLanguage.ARABIC,
    val bottomLanguage: NiloraLanguage = NiloraLanguage.ENGLISH,
    val activeSharedSpeaker: String = "BOTTOM", // TOP | BOTTOM
    val sharedMessages: List<SharedTranslationMessage> = emptyList(),
    // Two-Phone local language selection
    val myRoomLanguage: NiloraLanguage = NiloraLanguage.ENGLISH,
    val autoPlayTts: Boolean = true,
    // Hardware, Voice & Model Diagnostics + Real Speech Evaluation Lab
    val hardwareCapabilities: HardwareAndSpeechCapabilities? = null,
    val evaluatedModels: List<ModelSpecInfo> = emptyList(),
    val lastAmbientSample: AmbientAudioSample? = null,
    val ambientSampleError: String? = null,
    val isSamplingAmbient: Boolean = false,
    val evaluationTrials: List<SpeechEvaluationTrial> = emptyList(),
    // AI Travel Assistant
    val assistantMessages: List<AssistantChatMessage> = emptyList(),
    val isAssistantStreaming: Boolean = false,
    val assistantError: String? = null
)

class TranslatorViewModel(
    private val firestoreRepo: FirestoreDataRepository,
    private val aiRepo: TranslationAiRepository,
    private val speechEngine: SpeechRecognitionEngine,
    private val translationEngine: TranslationEngine,
    val voiceEngine: VoiceGenerationEngine,
    private val locationRepo: LocationAndPlacesRepository,
    private val prefsRepo: UserPreferencesRepository,
    private val appContext: Context,
    val currentUserId: String
) : ViewModel() {

    companion object {
        private const val TAG = "TranslatorVM"
    }

    private val _state = MutableStateFlow(TranslatorUiState())
    val state: StateFlow<TranslatorUiState> = _state.asStateFlow()

    val rmsNormalized: StateFlow<Float> = speechEngine.rmsNormalized
    val isMuted: StateFlow<Boolean> = voiceEngine.isMuted
    val isSpeaking: StateFlow<Boolean> = voiceEngine.isSpeaking
    val ttsSpeed: StateFlow<Float> = voiceEngine.speed
    val ttsVolume: StateFlow<Float> = voiceEngine.volume
    val voiceCapabilities: StateFlow<VoiceCapabilityReport> = voiceEngine.capabilityReport
    val availableTtsVoices: StateFlow<List<TtsVoiceOption>> = voiceEngine.availableVoices

    private var roomJob: Job? = null
    private var messagesJob: Job? = null
    private var assistantJob: Job? = null
    private var syncedIdentityInitially = false

    init {
        refreshHardwareAndModelDiagnostics()
        viewModelScope.launch {
            prefsRepo.preferencesFlow.collect { prefs ->
                voiceEngine.setSpeed(prefs.ttsSpeed)
                voiceEngine.setVolume(prefs.ttsVolume)
                voiceEngine.selectVoice(prefs.selectedTtsVoiceName)
                val spokenCore = codeToCoreLanguage(prefs.spokenLanguageCode)
                val targetCore = codeToCoreLanguage(prefs.targetTranslationLanguageCode)
                _state.update { current ->
                    val shouldApplyDefaults = !syncedIdentityInitially && current.sharedMessages.isEmpty() && current.activeRoom == null
                    syncedIdentityInitially = true
                    current.copy(
                        voiceConsentGranted = prefs.voiceTransmissionConsent,
                        arabicDialectMode = if (prefs.preferEgyptianColloquial) {
                            ArabicDialectMode.EGYPTIAN_COLLOQUIAL
                        } else {
                            ArabicDialectMode.MODERN_STANDARD
                        },
                        autoTranslateAfterSpeech = prefs.autoTranslateAfterSpeech,
                        spokenLanguageCode = prefs.spokenLanguageCode,
                        targetTranslationLanguageCode = prefs.targetTranslationLanguageCode,
                        subtitleDisplayMode = SubtitleDisplayMode.fromKey(prefs.subtitleDisplayMode),
                        preferOriginalAudio = prefs.preferOriginalAudio,
                        selectedTtsVoiceName = prefs.selectedTtsVoiceName,
                        bottomLanguage = if (shouldApplyDefaults) spokenCore else current.bottomLanguage,
                        topLanguage = if (shouldApplyDefaults) {
                            if (targetCore == spokenCore) {
                                if (spokenCore == NiloraLanguage.ARABIC) NiloraLanguage.ENGLISH else NiloraLanguage.ARABIC
                            } else targetCore
                        } else current.topLanguage,
                        myRoomLanguage = if (shouldApplyDefaults) spokenCore else current.myRoomLanguage
                    )
                }
            }
        }
    }

    private fun codeToCoreLanguage(code: String): NiloraLanguage = when {
        code.startsWith("ar", ignoreCase = true) -> NiloraLanguage.ARABIC
        code.startsWith("fr", ignoreCase = true) -> NiloraLanguage.FRENCH
        code.startsWith("ru", ignoreCase = true) -> NiloraLanguage.RUSSIAN
        else -> NiloraLanguage.ENGLISH
    }

    fun refreshHardwareAndModelDiagnostics() {
        val hw = AudioCaptureAndHardwareProfiler.inspectCapabilities(appContext)
        val models = AudioCaptureAndHardwareProfiler.getEvaluatedModelCatalog(hw)
        _state.update { it.copy(hardwareCapabilities = hw, evaluatedModels = models) }
    }

    fun runAmbientMicrophoneCheck() {
        viewModelScope.launch {
            _state.update { it.copy(isSamplingAmbient = true, ambientSampleError = null) }
            val res = AudioCaptureAndHardwareProfiler.measureRealAmbientNoiseSample(appContext)
            res.onSuccess { sample ->
                _state.update { it.copy(isSamplingAmbient = false, lastAmbientSample = sample) }
            }.onFailure { err ->
                _state.update {
                    it.copy(
                        isSamplingAmbient = false,
                        ambientSampleError = err.localizedMessage ?: "AudioRecord microphone test failed."
                    )
                }
            }
        }
    }

    fun grantVoiceConsent(granted: Boolean) {
        viewModelScope.launch {
            prefsRepo.setVoiceTransmissionConsent(granted)
        }
    }

    fun setArabicDialectMode(mode: ArabicDialectMode) {
        viewModelScope.launch {
            prefsRepo.setPreferEgyptianColloquial(mode == ArabicDialectMode.EGYPTIAN_COLLOQUIAL)
        }
    }

    fun setAutoTranslateAfterSpeech(enabled: Boolean) {
        viewModelScope.launch {
            prefsRepo.setAutoTranslateAfterSpeech(enabled)
        }
    }

    fun setSubtitleDisplayMode(mode: SubtitleDisplayMode) {
        _state.update { it.copy(subtitleDisplayMode = mode) }
        viewModelScope.launch {
            prefsRepo.setSubtitleDisplayMode(mode.key)
        }
    }

    fun setPreferOriginalAudio(preferOriginal: Boolean) {
        _state.update { it.copy(preferOriginalAudio = preferOriginal) }
        viewModelScope.launch {
            prefsRepo.setPreferOriginalAudio(preferOriginal)
        }
    }

    fun selectTtsVoice(voiceName: String) {
        voiceEngine.selectVoice(voiceName)
        _state.update { it.copy(selectedTtsVoiceName = voiceName) }
        viewModelScope.launch {
            prefsRepo.setSelectedTtsVoiceName(voiceName)
        }
    }

    fun setTranslationDomain(domain: TranslationContextDomain) {
        _state.update { it.copy(translationDomain = domain) }
    }

    fun setAutoDetectSource(enabled: Boolean) {
        _state.update { it.copy(autoDetectSource = enabled) }
    }

    fun toggleTopPanelRotation() {
        _state.update { it.copy(rotateTopPanel = !it.rotateTopPanel) }
    }

    fun setTopLanguage(lang: NiloraLanguage) {
        _state.update { it.copy(topLanguage = lang) }
    }

    fun setBottomLanguage(lang: NiloraLanguage) {
        _state.update { it.copy(bottomLanguage = lang) }
    }

    fun setTtsSpeed(speed: Float) {
        voiceEngine.setSpeed(speed)
        viewModelScope.launch { prefsRepo.setTtsSpeed(speed) }
    }

    fun setTtsVolume(volume: Float) {
        voiceEngine.setVolume(volume)
        viewModelScope.launch { prefsRepo.setTtsVolume(volume) }
    }

    fun setMyRoomLanguage(lang: NiloraLanguage) {
        _state.update { it.copy(myRoomLanguage = lang) }
        val room = _state.value.activeRoom ?: return
        val isHost = room.hostId == currentUserId
        viewModelScope.launch {
            firestoreRepo.updateRoomLanguage(room.roomCode, isHost, lang.code)
        }
    }

    fun toggleAutoPlayTts() {
        _state.update { it.copy(autoPlayTts = !it.autoPlayTts) }
    }

    fun updatePendingTranscript(edited: String) {
        _state.update { it.copy(pendingEditableTranscript = edited) }
    }

    fun dismissPendingCorrection() {
        _state.update {
            it.copy(
                pendingEditableTranscript = "",
                pipelineStage = PipelineStage.IDLE
            )
        }
    }

    // --- VOICE CAPTURE & REAL-TIME PIPELINE (ONE-PHONE MODE) ---
    fun startVoiceCaptureShared(speakerSide: String) {
        if (!_state.value.voiceConsentGranted) {
            _state.update {
                it.copy(
                    pipelineStage = PipelineStage.ERROR,
                    pipelineError = "Explicit consent is required before capturing and translating speech."
                )
            }
            return
        }
        // Prevent overlapping TTS while listening
        voiceEngine.pauseOrStop()
        val lang = if (speakerSide == "TOP") _state.value.topLanguage else _state.value.bottomLanguage
        _state.update {
            it.copy(
                activeSharedSpeaker = speakerSide,
                pendingSpeakerSide = speakerSide,
                pipelineStage = PipelineStage.LISTENING,
                pipelineError = null,
                partialTranscript = ""
            )
        }
        speechEngine.startRecognition(
            language = lang,
            onSpeechDetected = {
                _state.update { it.copy(pipelineStage = PipelineStage.SPEECH_DETECTED) }
            },
            onPartialResult = { partial ->
                // Unstable partial transcripts are NEVER spoken or sent to translation
                _state.update {
                    it.copy(
                        pipelineStage = PipelineStage.RECOGNIZING,
                        partialTranscript = partial
                    )
                }
            },
            onFinalResult = { result ->
                _state.update {
                    it.copy(
                        partialTranscript = "",
                        pendingEditableTranscript = result.transcript,
                        lastSttLatencyMs = result.latencyMs,
                        lastSttConfidence = result.confidenceScore
                    )
                }
                if (_state.value.autoTranslateAfterSpeech) {
                    translateSharedUtterance(
                        speakerSide = speakerSide,
                        text = result.transcript,
                        sttLatencyMs = result.latencyMs,
                        sttConfidence = result.confidenceScore
                    )
                } else {
                    _state.update { it.copy(pipelineStage = PipelineStage.AWAITING_CORRECTION) }
                }
            },
            onError = { errMsg ->
                _state.update {
                    it.copy(
                        pipelineStage = PipelineStage.ERROR,
                        pipelineError = errMsg,
                        partialTranscript = ""
                    )
                }
            }
        )
    }

    fun stopVoiceCapture() {
        speechEngine.stopRecognition()
        if (_state.value.pipelineStage == PipelineStage.LISTENING ||
            _state.value.pipelineStage == PipelineStage.SPEECH_DETECTED ||
            _state.value.pipelineStage == PipelineStage.RECOGNIZING
        ) {
            _state.update { it.copy(pipelineStage = PipelineStage.IDLE, partialTranscript = "") }
        }
    }

    fun submitEditedPendingTranscript(roomSenderName: String? = null) {
        val edited = _state.value.pendingEditableTranscript.trim()
        val side = _state.value.pendingSpeakerSide
        if (edited.isBlank()) return
        if (side == "ROOM") {
            sendRoomUtterance(roomSenderName ?: "Speaker", edited)
        } else {
            translateSharedUtterance(
                speakerSide = side,
                text = edited,
                sttLatencyMs = _state.value.lastSttLatencyMs,
                sttConfidence = _state.value.lastSttConfidence
            )
        }
    }

    fun translateSharedUtterance(
        speakerSide: String,
        text: String,
        sttLatencyMs: Long = 0L,
        sttConfidence: Float? = null
    ) {
        if (!_state.value.voiceConsentGranted) {
            _state.update {
                it.copy(
                    pipelineStage = PipelineStage.ERROR,
                    pipelineError = "Please accept the NILORA LIVE privacy & external processing disclosure before translating."
                )
            }
            return
        }
        if (!locationRepo.isNetworkOnline()) {
            _state.update {
                it.copy(
                    pipelineStage = PipelineStage.ERROR,
                    pipelineError = "Network disconnected. Real-time translation engine requires an active internet connection."
                )
            }
            return
        }
        val sourceLang = if (speakerSide == "TOP") _state.value.topLanguage else _state.value.bottomLanguage
        val targetLang = if (speakerSide == "TOP") _state.value.bottomLanguage else _state.value.topLanguage
        val dialectMode = _state.value.arabicDialectMode
        val contextSnippets = _state.value.sharedMessages.takeLast(4).map {
            "${it.sourceLang.code}: ${it.originalText} -> ${it.targetLang.code}: ${it.translatedText}"
        }

        viewModelScope.launch {
            _state.update {
                it.copy(
                    pipelineStage = PipelineStage.TRANSLATING,
                    pipelineError = null,
                    pendingEditableTranscript = text,
                    pendingSpeakerSide = speakerSide
                )
            }
            val result = translationEngine.translate(
                TranslationRequest(
                    sourceText = text,
                    sourceLanguage = sourceLang,
                    targetLanguage = targetLang,
                    arabicDialectMode = dialectMode,
                    conversationContext = contextSnippets,
                    domain = _state.value.translationDomain,
                    autoDetectSource = _state.value.autoDetectSource
                )
            )
            result.onSuccess { res ->
                _state.update { it.copy(pipelineStage = PipelineStage.VALIDATING) }
                val combinedWarnings = buildList {
                    if (res.ambiguityWarning.isNotBlank()) add(res.ambiguityWarning)
                    addAll(res.validationWarnings)
                    if (sttConfidence != null && sttConfidence in 0.0f..0.45f) {
                        add("Low microphone recognition confidence; please verify recognized text.")
                    }
                }
                val finalTranslated = if (res.confidenceLabel == "LOW" && res.translatedText.isBlank()) {
                    "[Unclear speech — please repeat or edit recognized text]"
                } else {
                    res.translatedText
                }
                val item = SharedTranslationMessage(
                    id = "msg_${System.currentTimeMillis()}",
                    speakerSide = speakerSide,
                    sourceLang = sourceLang,
                    targetLang = targetLang,
                    dialectMode = dialectMode,
                    originalText = text,
                    translatedText = finalTranslated,
                    detectedDialect = res.detectedSourceDialect,
                    confidence = res.confidenceLabel,
                    warning = combinedWarnings.joinToString(" "),
                    sttLatencyMs = sttLatencyMs,
                    translationLatencyMs = res.latencyMs
                )
                _state.update {
                    it.copy(
                        sharedMessages = (it.sharedMessages + item).takeLast(50),
                        pendingEditableTranscript = text
                    )
                }
                if (_state.value.autoPlayTts && res.confidenceLabel != "LOW" && res.translatedText.isNotBlank()) {
                    if (_state.value.preferOriginalAudio) {
                        speakTranslatedAudio(text, sourceLang)
                    } else {
                        speakTranslatedAudio(res.translatedText, targetLang)
                    }
                } else {
                    _state.update { it.copy(pipelineStage = PipelineStage.IDLE) }
                }
            }.onFailure { err ->
                _state.update {
                    it.copy(
                        pipelineStage = PipelineStage.ERROR,
                        pipelineError = err.localizedMessage ?: "Translation failed."
                    )
                }
            }
        }
    }

    fun speakTranslatedAudio(text: String, language: NiloraLanguage) {
        _state.update { it.copy(pipelineStage = PipelineStage.GENERATING_VOICE) }
        voiceEngine.speak(
            text = text,
            language = language,
            onStarted = {
                _state.update { it.copy(pipelineStage = PipelineStage.PLAYING) }
            },
            onFinished = {
                _state.update {
                    if (it.pipelineStage == PipelineStage.PLAYING || it.pipelineStage == PipelineStage.GENERATING_VOICE) {
                        it.copy(pipelineStage = PipelineStage.IDLE)
                    } else it
                }
            },
            onError = { err ->
                _state.update {
                    it.copy(
                        pipelineStage = PipelineStage.ERROR,
                        pipelineError = err
                    )
                }
            }
        )
    }

    fun pauseOrStopAudio() {
        voiceEngine.pauseOrStop()
        _state.update { it.copy(pipelineStage = PipelineStage.PAUSED) }
    }

    fun replayLastAudio() {
        _state.update { it.copy(pipelineStage = PipelineStage.GENERATING_VOICE) }
        voiceEngine.replayLast(
            onStarted = { _state.update { it.copy(pipelineStage = PipelineStage.PLAYING) } },
            onFinished = { _state.update { it.copy(pipelineStage = PipelineStage.IDLE) } },
            onError = { err -> _state.update { it.copy(pipelineStage = PipelineStage.ERROR, pipelineError = err) } }
        )
    }

    fun clearSharedSession() {
        speechEngine.stopRecognition()
        voiceEngine.pauseOrStop()
        _state.update {
            it.copy(
                sharedMessages = emptyList(),
                partialTranscript = "",
                pendingEditableTranscript = "",
                pipelineStage = PipelineStage.IDLE,
                pipelineError = null
            )
        }
    }

    // --- REAL HUMAN SPEECH EVALUATION LAB ---
    fun recordEvaluationTrial(
        sourceLang: NiloraLanguage,
        targetLang: NiloraLanguage,
        scenarioLabel: String,
        referenceTranscript: String,
        recognizedTranscript: String,
        referenceTranslation: String,
        humanNotes: String,
        onComplete: () -> Unit = {}
    ) {
        if (recognizedTranscript.isBlank()) return
        viewModelScope.launch {
            _state.update { it.copy(pipelineStage = PipelineStage.TRANSLATING, pipelineError = null) }
            val result = translationEngine.translate(
                TranslationRequest(
                    sourceText = recognizedTranscript,
                    sourceLanguage = sourceLang,
                    targetLanguage = targetLang,
                    arabicDialectMode = _state.value.arabicDialectMode
                )
            )
            result.onSuccess { res ->
                val wer = SpeechEvaluationHarness.computeWordErrorRate(referenceTranscript, recognizedTranscript)
                val trial = SpeechEvaluationTrial(
                    id = "trial_${System.currentTimeMillis()}",
                    sourceLanguage = sourceLang,
                    targetLanguage = targetLang,
                    scenarioLabel = scenarioLabel,
                    referenceTranscript = referenceTranscript.trim(),
                    recognizedTranscript = recognizedTranscript.trim(),
                    computedWer = wer,
                    referenceTranslation = referenceTranslation.trim(),
                    actualTranslation = res.translatedText,
                    detectedDialect = res.detectedSourceDialect,
                    sttLatencyMs = _state.value.lastSttLatencyMs,
                    translationLatencyMs = res.latencyMs,
                    validationWarnings = res.validationWarnings,
                    humanNaturalnessNotes = humanNotes.trim()
                )
                _state.update {
                    it.copy(
                        pipelineStage = PipelineStage.IDLE,
                        evaluationTrials = listOf(trial) + it.evaluationTrials
                    )
                }
                onComplete()
            }.onFailure { err ->
                _state.update {
                    it.copy(
                        pipelineStage = PipelineStage.ERROR,
                        pipelineError = err.localizedMessage ?: "Evaluation translation failed."
                    )
                }
            }
        }
    }

    fun clearEvaluationTrials() {
        _state.update { it.copy(evaluationTrials = emptyList()) }
    }

    // --- TWO-PHONE CONVERSATION ROOMS ---
    fun createRoom(hostName: String, hostLanguage: NiloraLanguage, retainTranscript: Boolean, onCreated: (String) -> Unit) {
        viewModelScope.launch {
            _state.update {
                it.copy(
                    isConnectingRoom = true,
                    roomConnectionStatus = "CONNECTING",
                    roomError = null,
                    myRoomLanguage = hostLanguage
                )
            }
            val res = firestoreRepo.createConversationRoom(hostName, hostLanguage.code, retainTranscript)
            res.onSuccess { code ->
                _state.update { it.copy(isConnectingRoom = false, roomConnectionStatus = "CONNECTED") }
                attachToRoom(code)
                onCreated(code)
            }.onFailure { err ->
                _state.update {
                    it.copy(
                        isConnectingRoom = false,
                        roomConnectionStatus = "DISCONNECTED",
                        roomError = err.localizedMessage ?: "Failed to create room"
                    )
                }
            }
        }
    }

    fun joinRoom(roomCode: String, guestName: String, guestLanguage: NiloraLanguage, onJoined: () -> Unit) {
        viewModelScope.launch {
            _state.update {
                it.copy(
                    isConnectingRoom = true,
                    roomConnectionStatus = "CONNECTING",
                    roomError = null,
                    myRoomLanguage = guestLanguage
                )
            }
            val res = firestoreRepo.joinConversationRoom(roomCode, guestName, guestLanguage.code)
            res.onSuccess { room ->
                _state.update {
                    it.copy(
                        isConnectingRoom = false,
                        roomConnectionStatus = "CONNECTED",
                        activeRoom = room
                    )
                }
                attachToRoom(room.roomCode)
                onJoined()
            }.onFailure { err ->
                _state.update {
                    it.copy(
                        isConnectingRoom = false,
                        roomConnectionStatus = "DISCONNECTED",
                        roomError = err.localizedMessage ?: "Unable to join room"
                    )
                }
            }
        }
    }

    fun attachToRoom(roomCode: String) {
        roomJob?.cancel()
        messagesJob?.cancel()
        _state.update { it.copy(roomConnectionStatus = "CONNECTING", roomError = null) }
        roomJob = viewModelScope.launch {
            firestoreRepo.observeRoom(roomCode)
                .catch { e ->
                    val fs = generateSequence(e) { it.cause }.filterIsInstance<FirebaseFirestoreException>().firstOrNull()
                    if (fs == null && e is CancellationException) throw e
                    _state.update {
                        it.copy(
                            roomConnectionStatus = "RECONNECTING",
                            roomError = (fs ?: e).localizedMessage ?: "Room connection interrupted"
                        )
                    }
                }
                .collect { room ->
                    val status = when {
                        room == null -> "DISCONNECTED"
                        room.status == "ENDED" -> "ENDED"
                        else -> "CONNECTED"
                    }
                    _state.update { it.copy(activeRoom = room, roomConnectionStatus = status) }
                }
        }
        messagesJob = viewModelScope.launch {
            firestoreRepo.observeRoomMessages(roomCode)
                .catch { e ->
                    val fs = generateSequence(e) { it.cause }.filterIsInstance<FirebaseFirestoreException>().firstOrNull()
                    if (fs == null && e is CancellationException) throw e
                    _state.update {
                        it.copy(
                            roomConnectionStatus = "RECONNECTING",
                            roomError = (fs ?: e).localizedMessage ?: "Messages stream interrupted"
                        )
                    }
                }
                .collect { msgs ->
                    val previousCount = _state.value.roomMessages.size
                    _state.update { it.copy(roomMessages = msgs) }
                    if (msgs.size > previousCount && msgs.isNotEmpty()) {
                        val latest = msgs.last()
                        if (latest.senderId != currentUserId && _state.value.autoPlayTts) {
                            if (_state.value.preferOriginalAudio) {
                                speakTranslatedAudio(
                                    latest.originalText,
                                    NiloraLanguage.fromCode(latest.sourceLanguage)
                                )
                            } else {
                                speakTranslatedAudio(
                                    latest.translatedText,
                                    NiloraLanguage.fromCode(latest.targetLanguage)
                                )
                            }
                        }
                    }
                }
        }
    }

    fun startVoiceCaptureForRoom(senderName: String) {
        if (!_state.value.voiceConsentGranted) {
            _state.update {
                it.copy(
                    pipelineStage = PipelineStage.ERROR,
                    pipelineError = "Please accept NILORA LIVE privacy consent before capturing speech."
                )
            }
            return
        }
        val room = _state.value.activeRoom ?: return
        voiceEngine.pauseOrStop()
        val isHost = room.hostId == currentUserId
        val myLang = NiloraLanguage.fromCode(if (isHost) room.hostLanguage else room.guestLanguage)
        _state.update {
            it.copy(
                pendingSpeakerSide = "ROOM",
                pipelineStage = PipelineStage.LISTENING,
                pipelineError = null,
                partialTranscript = ""
            )
        }
        speechEngine.startRecognition(
            language = myLang,
            onSpeechDetected = { _state.update { it.copy(pipelineStage = PipelineStage.SPEECH_DETECTED) } },
            onPartialResult = { p ->
                _state.update { it.copy(pipelineStage = PipelineStage.RECOGNIZING, partialTranscript = p) }
            },
            onFinalResult = { result ->
                _state.update {
                    it.copy(
                        partialTranscript = "",
                        pendingEditableTranscript = result.transcript,
                        lastSttLatencyMs = result.latencyMs,
                        lastSttConfidence = result.confidenceScore
                    )
                }
                if (_state.value.autoTranslateAfterSpeech) {
                    sendRoomUtterance(senderName, result.transcript)
                } else {
                    _state.update { it.copy(pipelineStage = PipelineStage.AWAITING_CORRECTION) }
                }
            },
            onError = { err ->
                _state.update {
                    it.copy(
                        pipelineStage = PipelineStage.ERROR,
                        pipelineError = err,
                        partialTranscript = ""
                    )
                }
            }
        )
    }

    fun sendRoomUtterance(senderName: String, originalText: String) {
        if (!_state.value.voiceConsentGranted) {
            _state.update {
                it.copy(
                    pipelineStage = PipelineStage.ERROR,
                    pipelineError = "Explicit consent is required before sending speech/text for translation."
                )
            }
            return
        }
        val room = _state.value.activeRoom ?: return
        if (!locationRepo.isNetworkOnline()) {
            _state.update {
                it.copy(
                    pipelineStage = PipelineStage.ERROR,
                    pipelineError = "Network offline. Cannot translate or transmit room messages."
                )
            }
            return
        }
        val isHost = room.hostId == currentUserId
        val sourceLang = NiloraLanguage.fromCode(if (isHost) room.hostLanguage else room.guestLanguage)
        val targetLang = NiloraLanguage.fromCode(if (isHost) room.guestLanguage else room.hostLanguage)
        val contextSnippets = _state.value.roomMessages.takeLast(4).map {
            "${it.sourceLanguage}: ${it.originalText} -> ${it.targetLanguage}: ${it.translatedText}"
        }

        viewModelScope.launch {
            _state.update { it.copy(pipelineStage = PipelineStage.TRANSLATING, pipelineError = null) }
            val trRes = translationEngine.translate(
                TranslationRequest(
                    sourceText = originalText,
                    sourceLanguage = sourceLang,
                    targetLanguage = targetLang,
                    arabicDialectMode = _state.value.arabicDialectMode,
                    conversationContext = contextSnippets,
                    domain = _state.value.translationDomain,
                    autoDetectSource = _state.value.autoDetectSource
                )
            )
            trRes.onSuccess { tr ->
                _state.update { it.copy(pipelineStage = PipelineStage.VALIDATING) }
                val outText = if (tr.confidenceLabel == "LOW" && tr.translatedText.isBlank()) {
                    "[Unclear speech — please repeat]"
                } else {
                    tr.translatedText
                }
                val note = buildList {
                    if (tr.ambiguityWarning.isNotBlank()) add(tr.ambiguityWarning)
                    addAll(tr.validationWarnings)
                }.joinToString(" ")
                val sendRes = firestoreRepo.sendRoomMessage(
                    room = room,
                    senderName = senderName,
                    sourceLanguage = sourceLang.code,
                    targetLanguage = targetLang.code,
                    originalText = originalText,
                    translatedText = outText,
                    confidenceNote = note
                )
                _state.update {
                    it.copy(
                        pipelineStage = if (sendRes.isFailure) PipelineStage.ERROR else PipelineStage.IDLE,
                        pipelineError = sendRes.exceptionOrNull()?.localizedMessage,
                        pendingEditableTranscript = ""
                    )
                }
            }.onFailure { err ->
                _state.update {
                    it.copy(
                        pipelineStage = PipelineStage.ERROR,
                        pipelineError = err.localizedMessage ?: "Translation failed."
                    )
                }
            }
        }
    }

    fun leaveRoomSessionLocally() {
        speechEngine.stopRecognition()
        voiceEngine.pauseOrStop()
        roomJob?.cancel()
        messagesJob?.cancel()
        _state.update {
            it.copy(
                activeRoom = null,
                roomMessages = emptyList(),
                roomConnectionStatus = "DISCONNECTED",
                pipelineStage = PipelineStage.IDLE
            )
        }
    }

    fun endActiveRoom() {
        val room = _state.value.activeRoom ?: return
        viewModelScope.launch {
            speechEngine.stopRecognition()
            voiceEngine.pauseOrStop()
            firestoreRepo.endConversationRoom(room.roomCode, deleteMessagesIfTemporary = !room.retainTranscript)
            roomJob?.cancel()
            messagesJob?.cancel()
            _state.update {
                it.copy(
                    activeRoom = null,
                    roomMessages = emptyList(),
                    roomConnectionStatus = "ENDED",
                    pipelineStage = PipelineStage.IDLE
                )
            }
        }
    }

    // --- AI TRAVEL ASSISTANT ---
    fun askAssistant(
        query: String,
        language: NiloraLanguage,
        verifiedDestinations: List<Destination>,
        verifiedActivities: List<ActivityListing>,
        verifiedProviders: List<ProviderProfile>,
        realityEvents: List<RealityEvent> = emptyList(),
        teamActivities: List<TeamActivityPost> = emptyList(),
        missions: List<RealityMission> = emptyList(),
        challenges: List<GlobalChallenge> = emptyList(),
        bridgeLinks: List<RealityBridgeLink> = emptyList()
    ) {
        if (query.isBlank()) return
        if (!locationRepo.isNetworkOnline()) {
            _state.update { it.copy(assistantError = "Internet connection required to query NILORA AI Assistant.") }
            return
        }
        assistantJob?.cancel()
        val userMsg = AssistantChatMessage("u_${System.currentTimeMillis()}", AssistantMessageRole.User, query.trim())
        val assistantMsgId = "a_${System.currentTimeMillis() + 1}"
        val placeholder = AssistantChatMessage(assistantMsgId, AssistantMessageRole.Assistant, "", isStreaming = true)

        _state.update {
            it.copy(
                assistantMessages = it.assistantMessages + userMsg + placeholder,
                isAssistantStreaming = true,
                assistantError = null
            )
        }

        assistantJob = viewModelScope.launch {
            try {
                aiRepo.streamAssistantResponse(
                    userQuery = query.trim(),
                    responseLanguage = language,
                    verifiedDestinations = verifiedDestinations,
                    verifiedActivities = verifiedActivities,
                    verifiedProviders = verifiedProviders,
                    realityEvents = realityEvents,
                    teamActivities = teamActivities,
                    missions = missions,
                    challenges = challenges,
                    bridgeLinks = bridgeLinks
                ).collect { streamedText ->
                    _state.update { current ->
                        current.copy(
                            assistantMessages = current.assistantMessages.map { msg ->
                                if (msg.id == assistantMsgId) msg.copy(text = streamedText, isStreaming = true) else msg
                            }
                        )
                    }
                }
                _state.update { current ->
                    current.copy(
                        isAssistantStreaming = false,
                        assistantMessages = current.assistantMessages.map { msg ->
                            if (msg.id == assistantMsgId) msg.copy(isStreaming = false) else msg
                        }
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Assistant stream failed", e)
                _state.update { current ->
                    current.copy(
                        isAssistantStreaming = false,
                        assistantError = e.localizedMessage ?: "AI Travel Assistant request failed.",
                        assistantMessages = current.assistantMessages.filterNot { it.id == assistantMsgId && it.text.isBlank() }
                    )
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        speechEngine.release()
        voiceEngine.release()
    }
}
