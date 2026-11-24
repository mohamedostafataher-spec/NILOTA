package com.example.ui.viewmodel

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.*
import com.example.data.repository.CommunityAndMessagingRepository
import com.example.data.repository.ExperienceConnectionRepository
import com.example.data.repository.FirestoreDataRepository
import com.example.niloralive.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class VoiceMediaDraftState(
    val isRecording: Boolean = false,
    val isTranscribing: Boolean = false,
    val recordedClip: RecordedVoiceClip? = null,
    val dictatedText: String = "",
    val photoBase64: String = "",
    val isEncodingPhoto: Boolean = false,
    val playingAudioId: String? = null,
    val errorBanner: String? = null
)

data class PostcardDetailUiState(
    val postcard: WorldPostcard? = null,
    val contributions: List<PostcardContribution> = emptyList(),
    val targetLanguageCode: String = "en",
    val isTranslating: Boolean = false,
    val isSubmitting: Boolean = false,
    val errorBanner: String? = null
)

data class SwapDetailUiState(
    val swap: CultureSwap? = null,
    val entries: List<CultureSwapEntry> = emptyList(),
    val targetLanguageCode: String = "en",
    val isTranslating: Boolean = false,
    val isSubmitting: Boolean = false,
    val errorBanner: String? = null
)

data class QuestDetailUiState(
    val quest: WorldQuest? = null,
    val contributions: List<WorldQuestContribution> = emptyList(),
    val targetLanguageCode: String = "en",
    val isTranslating: Boolean = false,
    val isSubmitting: Boolean = false,
    val errorBanner: String? = null
)

class ExperienceConnectionViewModel(
    private val experienceRepo: ExperienceConnectionRepository,
    private val communityRepo: CommunityAndMessagingRepository,
    private val firestoreRepo: FirestoreDataRepository,
    private val translationEngine: EgyptianTranslationEngine,
    private val sttEngine: SpeechRecognitionEngine,
    private val ttsEngine: VoiceGenerationEngine,
    private val voiceRecorder: VoiceMessageAudioRecorder,
    val currentUserId: String
) : ViewModel() {

    companion object {
        private const val TAG = "ExperienceConnVM"
        private const val STOP_TIMEOUT_MS = 5000L
    }

    private val _userProfile = MutableStateFlow<UserProfile?>(null)
    val myProfile: StateFlow<UserProfile?> = _userProfile.asStateFlow()

    private val _actionStatus = MutableStateFlow<Pair<String?, Boolean>>(null to false)
    val actionStatus: StateFlow<Pair<String?, Boolean>> = _actionStatus.asStateFlow()

    private val _mediaDraft = MutableStateFlow(VoiceMediaDraftState())
    val mediaDraft: StateFlow<VoiceMediaDraftState> = _mediaDraft.asStateFlow()

    private val _postcardDetail = MutableStateFlow(PostcardDetailUiState())
    val postcardDetail: StateFlow<PostcardDetailUiState> = _postcardDetail.asStateFlow()

    private val _swapDetail = MutableStateFlow(SwapDetailUiState())
    val swapDetail: StateFlow<SwapDetailUiState> = _swapDetail.asStateFlow()

    private val _questDetail = MutableStateFlow(QuestDetailUiState())
    val questDetail: StateFlow<QuestDetailUiState> = _questDetail.asStateFlow()

    private var postcardJob: Job? = null
    private var postcardContribJob: Job? = null
    private var swapJob: Job? = null
    private var swapEntriesJob: Job? = null
    private var questJob: Job? = null
    private var questContribJob: Job? = null

    init {
        viewModelScope.launch {
            firestoreRepo.observeUserProfile(currentUserId)
                .catch { Log.w(TAG, "Profile observe error", it) }
                .collect { prof ->
                    _userProfile.value = prof
                    if (prof != null) {
                        val pref = prof.nativeLanguage.ifBlank { prof.preferredLanguage }
                        _postcardDetail.update { if (it.postcard == null) it.copy(targetLanguageCode = pref) else it }
                        _swapDetail.update { if (it.swap == null) it.copy(targetLanguageCode = pref) else it }
                        _questDetail.update { if (it.quest == null) it.copy(targetLanguageCode = pref) else it }
                    }
                }
        }
    }

    val blockedIdsState: StateFlow<Set<String>> = communityRepo.observeBlockedUsers()
        .map { list -> list.map { it.blockedUserId }.toSet() }
        .catch { emit(emptySet()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptySet())

    val postcardsState: StateFlow<UiState<List<WorldPostcard>>> = combine(
        experienceRepo.observeWorldPostcards(),
        blockedIdsState
    ) { list, blocked ->
        UiState.Success(list.filter { it.authorId !in blocked }) as UiState<List<WorldPostcard>>
    }.catch { err ->
        emit(UiState.Error(err.localizedMessage ?: "Failed to load World Postcards."))
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), UiState.Loading)

    val cultureSwapsState: StateFlow<UiState<List<CultureSwap>>> = combine(
        experienceRepo.observeCultureSwaps(),
        blockedIdsState
    ) { list, blocked ->
        UiState.Success(list.filter { it.creatorId !in blocked }) as UiState<List<CultureSwap>>
    }.catch { err ->
        emit(UiState.Error(err.localizedMessage ?: "Failed to load Culture Swaps."))
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), UiState.Loading)

    val worldQuestsState: StateFlow<UiState<List<WorldQuest>>> = combine(
        experienceRepo.observeWorldQuests(),
        blockedIdsState
    ) { list, blocked ->
        UiState.Success(list.filter { it.creatorId !in blocked }) as UiState<List<WorldQuest>>
    }.catch { err ->
        emit(UiState.Error(err.localizedMessage ?: "Failed to load World Quests."))
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), UiState.Loading)

    val memoriesState: StateFlow<UiState<List<NiloraMemory>>> = combine(
        experienceRepo.observeAccessibleMemories(),
        blockedIdsState
    ) { list, blocked ->
        UiState.Success(list.filter { it.ownerId !in blocked }) as UiState<List<NiloraMemory>>
    }.catch { err ->
        emit(UiState.Error(err.localizedMessage ?: "Failed to load NILORA Memories."))
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), UiState.Loading)

    fun clearStatus() {
        _actionStatus.value = null to false
    }

    fun applyLanguageIdentityDefaults(targetCode: String) {
        _postcardDetail.update { if (it.postcard == null) it.copy(targetLanguageCode = targetCode) else it }
        _swapDetail.update { if (it.swap == null) it.copy(targetLanguageCode = targetCode) else it }
        _questDetail.update { if (it.quest == null) it.copy(targetLanguageCode = targetCode) else it }
    }

    private fun emitMessage(msg: String, isError: Boolean = false) {
        _actionStatus.value = msg to isError
    }

    // --- MEDIA & VOICE HELPERS ---

    fun resetMediaDraft() {
        voiceRecorder.cancelRecording()
        _mediaDraft.value = VoiceMediaDraftState()
    }

    fun attachPhotoFromPicker(context: Context, uri: Uri?) {
        if (uri == null) return
        viewModelScope.launch {
            _mediaDraft.update { it.copy(isEncodingPhoto = true, errorBanner = null) }
            ImageEncodingHelper.encodePickerUriToBase64(context, uri)
                .onSuccess { b64 ->
                    _mediaDraft.update { it.copy(photoBase64 = b64, isEncodingPhoto = false) }
                }
                .onFailure { err ->
                    _mediaDraft.update {
                        it.copy(isEncodingPhoto = false, errorBanner = err.localizedMessage ?: "Failed to attach photo.")
                    }
                }
        }
    }

    fun clearPhotoAttachment() {
        _mediaDraft.update { it.copy(photoBase64 = "") }
    }

    fun startVoiceRecording() {
        voiceRecorder.startRecording()
            .onSuccess { _mediaDraft.update { it.copy(isRecording = true, errorBanner = null) } }
            .onFailure { err -> _mediaDraft.update { it.copy(errorBanner = err.localizedMessage) } }
    }

    fun stopVoiceRecording() {
        voiceRecorder.stopRecording()
            .onSuccess { clip -> _mediaDraft.update { it.copy(isRecording = false, recordedClip = clip) } }
            .onFailure { err -> _mediaDraft.update { it.copy(isRecording = false, errorBanner = err.localizedMessage) } }
    }

    fun clearRecordedClip() {
        _mediaDraft.update { it.copy(recordedClip = null) }
    }

    fun dictateIntoDraft(languageCode: String) {
        if (!sttEngine.isAvailable()) {
            _mediaDraft.update {
                it.copy(errorBanner = "Speech-to-text is unavailable on this device. You can type or attach a recorded voice clip.")
            }
            return
        }
        val niloraLang = toNiloraLanguage(languageCode)
        _mediaDraft.update { it.copy(isTranscribing = true, errorBanner = null) }
        sttEngine.startRecognition(
            language = niloraLang,
            onSpeechDetected = {},
            onPartialResult = { partial -> _mediaDraft.update { it.copy(dictatedText = partial) } },
            onFinalResult = { res -> _mediaDraft.update { it.copy(isTranscribing = false, dictatedText = res.transcript) } },
            onError = { err -> _mediaDraft.update { it.copy(isTranscribing = false, errorBanner = "Transcription unavailable: $err") } }
        )
    }

    fun clearDictatedText() {
        _mediaDraft.update { it.copy(dictatedText = "") }
    }

    fun toggleAudioPlayback(clipId: String, audioBase64: String) {
        if (_mediaDraft.value.playingAudioId == clipId) {
            voiceRecorder.stopPlayback()
            _mediaDraft.update { it.copy(playingAudioId = null) }
            return
        }
        if (audioBase64.isBlank()) {
            emitMessage("No voice clip attached.", isError = true)
            return
        }
        _mediaDraft.update { it.copy(playingAudioId = clipId) }
        voiceRecorder.playVoiceClip(
            messageId = clipId,
            audioBase64 = audioBase64,
            onComplete = { _mediaDraft.update { it.copy(playingAudioId = null) } },
            onError = { err ->
                _mediaDraft.update { it.copy(playingAudioId = null) }
                emitMessage(err, isError = true)
            }
        )
    }

    fun speakText(text: String, languageCode: String) {
        if (text.isBlank()) return
        ttsEngine.speak(
            text = text,
            language = toNiloraLanguage(languageCode),
            onError = { err -> emitMessage(err, isError = true) }
        )
    }

    // --- WORLD POSTCARDS ---

    fun createWorldPostcard(
        category: PostcardCategory,
        title: String,
        storyText: String,
        localExpression: String,
        questionPrompt: String,
        country: String,
        city: String,
        languageCode: String,
        allowReplies: Boolean,
        allowPrivateConnect: Boolean,
        visibility: String,
        onCreated: (String) -> Unit
    ) {
        viewModelScope.launch {
            val draft = _mediaDraft.value
            val res = experienceRepo.createWorldPostcard(
                myProfile = _userProfile.value,
                category = category,
                title = title,
                storyText = storyText,
                localExpression = localExpression,
                questionPrompt = questionPrompt,
                country = country,
                city = city,
                languageCode = languageCode,
                photoBase64 = draft.photoBase64,
                audioBase64 = draft.recordedClip?.audioBase64.orEmpty(),
                audioDurationMs = draft.recordedClip?.durationMs ?: 0L,
                allowReplies = allowReplies,
                allowPrivateConnect = allowPrivateConnect,
                visibility = visibility
            )
            res.onSuccess { id ->
                resetMediaDraft()
                emitMessage("World Postcard shared!")
                onCreated(id)
            }.onFailure { err ->
                emitMessage(err.localizedMessage ?: "Failed to create World Postcard.", isError = true)
            }
        }
    }

    fun openPostcardDetail(postcardId: String) {
        postcardJob?.cancel()
        postcardContribJob?.cancel()
        _postcardDetail.update { it.copy(contributions = emptyList(), errorBanner = null) }

        postcardJob = viewModelScope.launch {
            experienceRepo.observePostcard(postcardId)
                .catch { err -> _postcardDetail.update { it.copy(errorBanner = err.localizedMessage) } }
                .collect { card -> _postcardDetail.update { it.copy(postcard = card) } }
        }
        postcardContribJob = viewModelScope.launch {
            experienceRepo.observePostcardContributions(postcardId)
                .catch { err -> _postcardDetail.update { it.copy(errorBanner = err.localizedMessage) } }
                .collect { list -> _postcardDetail.update { it.copy(contributions = list) } }
        }
    }

    fun setPostcardTargetLanguage(code: String) = _postcardDetail.update { it.copy(targetLanguageCode = code) }

    fun addPostcardContribution(
        postcard: WorldPostcard,
        responseText: String,
        localExpression: String,
        country: String,
        city: String,
        languageCode: String,
        allowPrivateConnect: Boolean,
        onAdded: () -> Unit = {}
    ) {
        viewModelScope.launch {
            _postcardDetail.update { it.copy(isSubmitting = true, errorBanner = null) }
            val draft = _mediaDraft.value
            val res = experienceRepo.addPostcardContribution(
                postcard = postcard,
                myProfile = _userProfile.value,
                responseText = responseText,
                localExpression = localExpression,
                country = country,
                city = city,
                languageCode = languageCode,
                photoBase64 = draft.photoBase64,
                audioBase64 = draft.recordedClip?.audioBase64.orEmpty(),
                audioDurationMs = draft.recordedClip?.durationMs ?: 0L,
                allowPrivateConnect = allowPrivateConnect
            )
            res.onSuccess {
                resetMediaDraft()
                _postcardDetail.update { it.copy(isSubmitting = false) }
                emitMessage("Contribution added to World Postcard.")
                onAdded()
            }.onFailure { err ->
                _postcardDetail.update { it.copy(isSubmitting = false, errorBanner = err.localizedMessage) }
            }
        }
    }

    fun translatePostcard(postcard: WorldPostcard, targetCode: String = _postcardDetail.value.targetLanguageCode) {
        viewModelScope.launch {
            _postcardDetail.update { it.copy(isTranslating = true, errorBanner = null) }
            val textToTranslate = buildString {
                append(postcard.storyText)
                if (postcard.localExpression.isNotBlank()) append("\nExpression: ${postcard.localExpression}")
                if (postcard.questionPrompt.isNotBlank()) append("\nQuestion: ${postcard.questionPrompt}")
            }
            translationEngine.translateWorldLanguage(
                sourceText = textToTranslate,
                sourceCode = postcard.languageCode,
                targetCode = targetCode
            ).onSuccess { out ->
                val note = out.ambiguityWarning.ifBlank { out.validationWarnings.firstOrNull().orEmpty() }
                val merged = postcard.translations + (targetCode to out.translatedText)
                experienceRepo.cachePostcardTranslation(postcard.id, merged, note)
                _postcardDetail.update { it.copy(isTranslating = false) }
            }.onFailure { err ->
                _postcardDetail.update {
                    it.copy(isTranslating = false, errorBanner = "Translation unavailable: ${err.localizedMessage ?: "Retry"}")
                }
            }
        }
    }

    fun translatePostcardContribution(
        postcardId: String,
        contribution: PostcardContribution,
        targetCode: String = _postcardDetail.value.targetLanguageCode
    ) {
        viewModelScope.launch {
            _postcardDetail.update { it.copy(isTranslating = true, errorBanner = null) }
            val text = buildString {
                append(contribution.responseText)
                if (contribution.localExpression.isNotBlank()) append("\nExpression: ${contribution.localExpression}")
            }
            translationEngine.translateWorldLanguage(
                sourceText = text,
                sourceCode = contribution.languageCode,
                targetCode = targetCode
            ).onSuccess { out ->
                val note = out.ambiguityWarning.ifBlank { out.validationWarnings.firstOrNull().orEmpty() }
                val merged = contribution.translations + (targetCode to out.translatedText)
                experienceRepo.cacheContributionTranslation(postcardId, contribution.id, merged, note)
                _postcardDetail.update { it.copy(isTranslating = false) }
            }.onFailure { err ->
                _postcardDetail.update {
                    it.copy(isTranslating = false, errorBanner = "Translation unavailable: ${err.localizedMessage ?: "Retry"}")
                }
            }
        }
    }

    // --- CULTURE SWAPS ---

    fun createCultureSwap(
        promptCategory: SwapPromptCategory,
        promptText: String,
        participationMode: String,
        maxParticipants: Long,
        initialResponseText: String,
        expressionSample: String,
        languageCode: String,
        allowPrivateConnect: Boolean,
        onCreated: (String) -> Unit
    ) {
        viewModelScope.launch {
            val draft = _mediaDraft.value
            val res = experienceRepo.createCultureSwap(
                myProfile = _userProfile.value,
                promptCategory = promptCategory,
                promptText = promptText,
                participationMode = participationMode,
                maxParticipants = maxParticipants,
                initialResponseText = initialResponseText,
                expressionSample = expressionSample,
                languageCode = languageCode,
                photoBase64 = draft.photoBase64,
                audioBase64 = draft.recordedClip?.audioBase64.orEmpty(),
                audioDurationMs = draft.recordedClip?.durationMs ?: 0L,
                allowPrivateConnect = allowPrivateConnect
            )
            res.onSuccess { id ->
                resetMediaDraft()
                emitMessage("Culture Swap opened!")
                onCreated(id)
            }.onFailure { err ->
                emitMessage(err.localizedMessage ?: "Failed to create Culture Swap.", isError = true)
            }
        }
    }

    fun openCultureSwapDetail(swapId: String) {
        swapJob?.cancel()
        swapEntriesJob?.cancel()
        _swapDetail.update { it.copy(entries = emptyList(), errorBanner = null) }

        swapJob = viewModelScope.launch {
            experienceRepo.observeCultureSwap(swapId)
                .catch { err -> _swapDetail.update { it.copy(errorBanner = err.localizedMessage) } }
                .collect { s -> _swapDetail.update { it.copy(swap = s) } }
        }
        swapEntriesJob = viewModelScope.launch {
            experienceRepo.observeCultureSwapEntries(swapId)
                .catch { err -> _swapDetail.update { it.copy(errorBanner = err.localizedMessage) } }
                .collect { entries -> _swapDetail.update { it.copy(entries = entries) } }
        }
    }

    fun setSwapTargetLanguage(code: String) = _swapDetail.update { it.copy(targetLanguageCode = code) }

    fun addCultureSwapEntry(
        swap: CultureSwap,
        contentText: String,
        expressionSample: String,
        country: String,
        languageCode: String,
        onAdded: () -> Unit = {}
    ) {
        viewModelScope.launch {
            _swapDetail.update { it.copy(isSubmitting = true, errorBanner = null) }
            val draft = _mediaDraft.value
            val res = experienceRepo.addCultureSwapEntry(
                swap = swap,
                myProfile = _userProfile.value,
                contentText = contentText,
                expressionSample = expressionSample,
                country = country,
                languageCode = languageCode,
                photoBase64 = draft.photoBase64,
                audioBase64 = draft.recordedClip?.audioBase64.orEmpty(),
                audioDurationMs = draft.recordedClip?.durationMs ?: 0L
            )
            res.onSuccess {
                resetMediaDraft()
                _swapDetail.update { it.copy(isSubmitting = false) }
                emitMessage("Cultural exchange added!")
                onAdded()
            }.onFailure { err ->
                _swapDetail.update { it.copy(isSubmitting = false, errorBanner = err.localizedMessage) }
            }
        }
    }

    fun translateSwapEntry(
        swapId: String,
        entry: CultureSwapEntry,
        targetCode: String = _swapDetail.value.targetLanguageCode
    ) {
        viewModelScope.launch {
            _swapDetail.update { it.copy(isTranslating = true, errorBanner = null) }
            val text = buildString {
                append(entry.contentText)
                if (entry.expressionSample.isNotBlank()) append("\nPhrase: ${entry.expressionSample}")
            }
            translationEngine.translateWorldLanguage(
                sourceText = text,
                sourceCode = entry.languageCode,
                targetCode = targetCode
            ).onSuccess { out ->
                val note = out.ambiguityWarning.ifBlank { out.validationWarnings.firstOrNull().orEmpty() }
                val merged = entry.translations + (targetCode to out.translatedText)
                experienceRepo.cacheSwapEntryTranslation(swapId, entry.id, merged, note)
                _swapDetail.update { it.copy(isTranslating = false) }
            }.onFailure { err ->
                _swapDetail.update {
                    it.copy(isTranslating = false, errorBanner = "Translation unavailable: ${err.localizedMessage ?: "Retry"}")
                }
            }
        }
    }

    // --- WORLD QUESTS ---

    fun createWorldQuest(
        missionType: QuestMissionType,
        title: String,
        objective: String,
        targetGoal: Long,
        onCreated: (String) -> Unit
    ) {
        viewModelScope.launch {
            val res = experienceRepo.createWorldQuest(
                myProfile = _userProfile.value,
                missionType = missionType,
                title = title,
                objective = objective,
                targetGoal = targetGoal
            )
            res.onSuccess { id ->
                emitMessage("World Quest launched!")
                onCreated(id)
            }.onFailure { err ->
                emitMessage(err.localizedMessage ?: "Failed to create World Quest.", isError = true)
            }
        }
    }

    fun openWorldQuestDetail(questId: String) {
        questJob?.cancel()
        questContribJob?.cancel()
        _questDetail.update { it.copy(contributions = emptyList(), errorBanner = null) }

        questJob = viewModelScope.launch {
            experienceRepo.observeWorldQuest(questId)
                .catch { err -> _questDetail.update { it.copy(errorBanner = err.localizedMessage) } }
                .collect { q -> _questDetail.update { it.copy(quest = q) } }
        }
        questContribJob = viewModelScope.launch {
            experienceRepo.observeWorldQuestContributions(questId)
                .catch { err -> _questDetail.update { it.copy(errorBanner = err.localizedMessage) } }
                .collect { list -> _questDetail.update { it.copy(contributions = list) } }
        }
    }

    fun setQuestTargetLanguage(code: String) = _questDetail.update { it.copy(targetLanguageCode = code) }

    fun addWorldQuestContribution(
        quest: WorldQuest,
        contentText: String,
        country: String,
        languageCode: String,
        onAdded: () -> Unit = {}
    ) {
        viewModelScope.launch {
            _questDetail.update { it.copy(isSubmitting = true, errorBanner = null) }
            val draft = _mediaDraft.value
            val res = experienceRepo.addWorldQuestContribution(
                quest = quest,
                myProfile = _userProfile.value,
                contentText = contentText,
                country = country,
                languageCode = languageCode,
                photoBase64 = draft.photoBase64,
                audioBase64 = draft.recordedClip?.audioBase64.orEmpty(),
                audioDurationMs = draft.recordedClip?.durationMs ?: 0L
            )
            res.onSuccess {
                resetMediaDraft()
                _questDetail.update { it.copy(isSubmitting = false) }
                val completedNow = quest.contributionsCount + 1 >= quest.targetGoal
                emitMessage(
                    if (completedNow) "World Quest goal reached & completed!"
                    else "Quest contribution recorded!"
                )
                onAdded()
            }.onFailure { err ->
                _questDetail.update { it.copy(isSubmitting = false, errorBanner = err.localizedMessage) }
            }
        }
    }

    fun translateQuestContribution(
        questId: String,
        contribution: WorldQuestContribution,
        targetCode: String = _questDetail.value.targetLanguageCode
    ) {
        viewModelScope.launch {
            _questDetail.update { it.copy(isTranslating = true, errorBanner = null) }
            translationEngine.translateWorldLanguage(
                sourceText = contribution.contentText,
                sourceCode = contribution.languageCode,
                targetCode = targetCode
            ).onSuccess { out ->
                val note = out.ambiguityWarning.ifBlank { out.validationWarnings.firstOrNull().orEmpty() }
                val merged = contribution.translations + (targetCode to out.translatedText)
                experienceRepo.cacheQuestContributionTranslation(questId, contribution.id, merged, note)
                _questDetail.update { it.copy(isTranslating = false) }
            }.onFailure { err ->
                _questDetail.update {
                    it.copy(isTranslating = false, errorBanner = "Translation unavailable: ${err.localizedMessage ?: "Retry"}")
                }
            }
        }
    }

    // --- NILORA MEMORY ---

    fun saveExperienceMemory(
        memoryType: MemoryType,
        sourceId: String,
        title: String,
        summaryText: String,
        originalLanguage: String,
        translations: Map<String, String>,
        countries: List<String>,
        partnerIds: List<String>,
        partnerNames: List<String>,
        placeLabel: String,
        photoBase64: String,
        audioBase64: String,
        audioDurationMs: Long,
        retainAudioConsent: Boolean,
        visibility: MemoryVisibility,
        onSaved: () -> Unit = {}
    ) {
        viewModelScope.launch {
            val res = experienceRepo.saveNiloraMemory(
                myProfile = _userProfile.value,
                memoryType = memoryType,
                sourceId = sourceId,
                title = title,
                summaryText = summaryText,
                originalLanguage = originalLanguage,
                translations = translations,
                countries = countries,
                partnerIds = partnerIds,
                partnerNames = partnerNames,
                placeLabel = placeLabel,
                photoBase64 = photoBase64,
                audioBase64 = audioBase64,
                audioDurationMs = audioDurationMs,
                retainAudioConsent = retainAudioConsent,
                visibility = visibility
            )
            res.onSuccess {
                emitMessage("Saved to NILORA Memory (${visibility.labelEn}).")
                onSaved()
            }.onFailure { err ->
                emitMessage(err.localizedMessage ?: "Failed to save memory.", isError = true)
            }
        }
    }

    fun createPersonalMemory(
        title: String,
        summaryText: String,
        country: String,
        placeLabel: String,
        languageCode: String,
        retainAudioConsent: Boolean,
        visibility: MemoryVisibility,
        onSaved: () -> Unit = {}
    ) {
        val draft = _mediaDraft.value
        saveExperienceMemory(
            memoryType = MemoryType.VOICE_MEMORY,
            sourceId = "",
            title = title,
            summaryText = summaryText,
            originalLanguage = languageCode,
            translations = emptyMap(),
            countries = listOf(country.ifBlank { _userProfile.value?.country ?: "Egypt" }),
            partnerIds = emptyList(),
            partnerNames = emptyList(),
            placeLabel = placeLabel,
            photoBase64 = draft.photoBase64,
            audioBase64 = if (retainAudioConsent) draft.recordedClip?.audioBase64.orEmpty() else "",
            audioDurationMs = if (retainAudioConsent) (draft.recordedClip?.durationMs ?: 0L) else 0L,
            retainAudioConsent = retainAudioConsent,
            visibility = visibility,
            onSaved = {
                resetMediaDraft()
                onSaved()
            }
        )
    }

    fun updateMemoryVisibility(memoryId: String, visibility: MemoryVisibility) {
        viewModelScope.launch {
            experienceRepo.updateMemoryVisibility(memoryId, visibility)
                .onSuccess { emitMessage("Memory visibility updated to ${visibility.labelEn}.") }
                .onFailure { err -> emitMessage(err.localizedMessage ?: "Could not update visibility.", isError = true) }
        }
    }

    fun revokeMemoryAudio(memoryId: String) {
        viewModelScope.launch {
            experienceRepo.revokeMemoryAudio(memoryId)
                .onSuccess { emitMessage("Voice audio permanently removed from this memory.") }
                .onFailure { err -> emitMessage(err.localizedMessage ?: "Could not remove audio.", isError = true) }
        }
    }

    fun deleteMemory(memoryId: String) {
        viewModelScope.launch {
            experienceRepo.deleteMemory(memoryId)
                .onSuccess { emitMessage("Memory deleted.") }
                .onFailure { err -> emitMessage(err.localizedMessage ?: "Could not delete memory.", isError = true) }
        }
    }

    // --- MUTUAL-CONSENT CONTINUATION & TRUST/SAFETY ---

    fun continuePrivatelyWith(
        partnerId: String,
        partnerName: String,
        partnerCountry: String,
        partnerLanguage: String,
        contextOriginTitle: String,
        onConversationReady: (String) -> Unit
    ) {
        viewModelScope.launch {
            experienceRepo.openPrivateContinuation(
                myProfile = _userProfile.value,
                partnerId = partnerId,
                partnerName = partnerName,
                partnerCountry = partnerCountry,
                partnerLanguage = partnerLanguage,
                contextOriginTitle = contextOriginTitle
            ).onSuccess { convId ->
                emitMessage("Private multilingual chat opened with $partnerName.")
                onConversationReady(convId)
            }.onFailure { err ->
                emitMessage(err.localizedMessage ?: "Unable to open private continuation.", isError = true)
            }
        }
    }

    fun blockParticipant(userId: String, userName: String, reason: String = "Blocked from experience") {
        viewModelScope.launch {
            communityRepo.blockUser(userId, userName, reason)
                .onSuccess { emitMessage("$userName blocked.") }
                .onFailure { err -> emitMessage(err.localizedMessage ?: "Failed to block user.", isError = true) }
        }
    }

    fun reportExperienceContent(targetType: String, targetId: String, reason: String, details: String = "") {
        viewModelScope.launch {
            firestoreRepo.submitReport(targetType, targetId, reason, details)
                .onSuccess { emitMessage("Report submitted for review.") }
                .onFailure { err -> emitMessage(err.localizedMessage ?: "Failed to submit report.", isError = true) }
        }
    }

    private fun toNiloraLanguage(code: String): NiloraLanguage = when {
        code.startsWith("ar", ignoreCase = true) -> NiloraLanguage.ARABIC
        code.startsWith("fr", ignoreCase = true) -> NiloraLanguage.FRENCH
        code.startsWith("ru", ignoreCase = true) -> NiloraLanguage.RUSSIAN
        else -> NiloraLanguage.ENGLISH
    }

    override fun onCleared() {
        super.onCleared()
        voiceRecorder.release()
    }
}
