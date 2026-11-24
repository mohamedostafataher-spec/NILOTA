package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.adventure3d.Camera3D
import com.example.data.model.*
import com.example.data.repository.*
import com.example.niloralive.*
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class AdventureWorldUiState(
    val selectedAdventure: AdventureExperience = AdventureCatalog.egyptAdventures.first(),
    val selectedDestinationFilter: String = "ALL",
    val selectedTypeFilter: AdventureType? = null,
    val weatherState: LiveIntelligenceResult<LiveWeatherSnapshot>? = null,
    val isLoadingWeather: Boolean = false,
    val wikiState: LiveIntelligenceResult<WikipediaSummaryResult>? = null,
    val isLoadingWiki: Boolean = false,
    val surpriseRevealAdventure: AdventureExperience? = null,
    val camera: Camera3D = Camera3D(),
    val discoveredPoiIds: Set<String> = emptySet(),
    val selectedPoi: AdventurePoi? = null,
    val audioMuted: Boolean = false,
    val activeSession: LiveAdventureSession? = null,
    val sessionParticipants: List<SessionParticipantPresence> = emptyList(),
    val sessionMessages: List<SessionChatMessage> = emptyList(),
    val targetTranslationLanguage: String = "en",
    val spokenLanguageCode: String = "en",
    val showSubtitles: Boolean = true,
    val speakTranslationsAloud: Boolean = true,
    val isListeningSpeech: Boolean = false,
    val isTranslatingChat: Boolean = false,
    val lastDiscoveredPoiMessage: String? = null
)

class AdventureWorldViewModel(
    private val adventureRepo: AdventureWorldRepository,
    private val intelRepo: DestinationIntelligenceRepository,
    private val experienceRepo: ExperienceConnectionRepository,
    private val communityRepo: CommunityAndMessagingRepository,
    private val firestoreRepo: FirestoreDataRepository,
    private val translationEngine: EgyptianTranslationEngine,
    private val sttEngine: SpeechRecognitionEngine,
    private val ttsEngine: VoiceGenerationEngine,
    val currentUserId: String
) : ViewModel() {

    companion object {
        private const val TAG = "AdventureWorldVM"
        private const val STOP_TIMEOUT_MS = 5000L
        private const val PRESENCE_SYNC_THROTTLE_MS = 450L
    }

    private val _userProfile = MutableStateFlow<UserProfile?>(null)
    val myProfile: StateFlow<UserProfile?> = _userProfile.asStateFlow()

    private val _worldUi = MutableStateFlow(AdventureWorldUiState())
    val worldUi: StateFlow<AdventureWorldUiState> = _worldUi.asStateFlow()

    private val _actionStatus = MutableStateFlow<Pair<String?, Boolean>>(null to false)
    val actionStatus: StateFlow<Pair<String?, Boolean>> = _actionStatus.asStateFlow()

    private var activeSessionJob: Job? = null
    private var crewDetailJob: Job? = null
    private var lastPresenceSyncMs = 0L

    private val _selectedCrew = MutableStateFlow<NiloraCrew?>(null)
    val selectedCrew: StateFlow<NiloraCrew?> = _selectedCrew.asStateFlow()

    private val _crewMessages = MutableStateFlow<List<CrewMessage>>(emptyList())
    val crewMessages: StateFlow<List<CrewMessage>> = _crewMessages.asStateFlow()

    val joinableSessionsState: StateFlow<UiState<List<LiveAdventureSession>>> =
        adventureRepo.observeJoinableSessions().toUiState("joinableSessions")

    val mySessionsState: StateFlow<UiState<List<LiveAdventureSession>>> =
        adventureRepo.observeMySessions().toUiState("mySessions")

    val matchProfilesState: StateFlow<UiState<List<AdventureMatchProfile>>> =
        adventureRepo.observeAvailableMatchers().toUiState("matchProfiles")

    val myCrewsState: StateFlow<UiState<List<NiloraCrew>>> =
        adventureRepo.observeMyCrews().toUiState("myCrews")

    val openCrewsState: StateFlow<UiState<List<NiloraCrew>>> =
        adventureRepo.observeOpenCrews().toUiState("openCrews")

    val blockedUsersState: StateFlow<UiState<List<BlockedUserRecord>>> =
        communityRepo.observeBlockedUsers().toUiState("blockedUsers")

    init {
        viewModelScope.launch {
            firestoreRepo.observeUserProfile(currentUserId)
                .catch { handleStreamError(it, "profile") }
                .collect { profile ->
                    _userProfile.value = profile
                    if (profile != null) {
                        val langCode = if (profile.preferredLanguage == "ar") "ar-EG" else profile.preferredLanguage
                        _worldUi.update { it.copy(targetTranslationLanguage = langCode) }
                    }
                }
        }
        selectAdventure(AdventureCatalog.egyptAdventures.first())
    }

    private fun <T> Flow<T>.toUiState(label: String): StateFlow<UiState<T>> =
        map<T, UiState<T>> { UiState.Success(it) }
            .catch { error ->
                val fsErr = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (fsErr == null && error is CancellationException) throw error
                Log.w(TAG, "Stream error ($label)", fsErr ?: error)
                emit(UiState.Error((fsErr ?: error).localizedMessage ?: "Failed to load $label"))
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), UiState.Loading)

    private fun handleStreamError(error: Throwable, label: String) {
        val fsErr = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (fsErr == null && error is CancellationException) throw error
        Log.w(TAG, "Error observing $label", fsErr ?: error)
    }

    fun selectAdventure(adventure: AdventureExperience) {
        _worldUi.update {
            it.copy(
                selectedAdventure = adventure,
                camera = Camera3D(),
                discoveredPoiIds = emptySet(),
                selectedPoi = adventure.pois.firstOrNull(),
                isLoadingWeather = true,
                isLoadingWiki = true
            )
        }
        refreshDestinationIntelligence(adventure)
    }

    fun setDestinationFilter(destination: String) {
        _worldUi.update { it.copy(selectedDestinationFilter = destination) }
    }

    fun setTypeFilter(type: AdventureType?) {
        _worldUi.update { it.copy(selectedTypeFilter = type) }
    }

    fun triggerSurpriseMe() {
        val wind = (_worldUi.value.weatherState as? LiveIntelligenceResult.Available)?.data?.windSpeedKmh
        val spot = AdventureCatalog.pickSurpriseSecretSpot(
            preferredType = _worldUi.value.selectedTypeFilter,
            windSpeedKmh = wind,
            excludeId = _worldUi.value.selectedAdventure.id
        )
        _worldUi.update { it.copy(surpriseRevealAdventure = spot) }
        selectAdventure(spot)
    }

    fun clearSurpriseBanner() {
        _worldUi.update { it.copy(surpriseRevealAdventure = null) }
    }

    fun refreshDestinationIntelligence(adventure: AdventureExperience = _worldUi.value.selectedAdventure) {
        viewModelScope.launch {
            _worldUi.update { it.copy(isLoadingWeather = true, isLoadingWiki = true) }
            val weather = intelRepo.fetchLiveConditions(
                latitude = adventure.latitude,
                longitude = adventure.longitude,
                includeMarine = adventure.isCoastalMarine
            )
            _worldUi.update { it.copy(weatherState = weather, isLoadingWeather = false) }
            val wiki = intelRepo.fetchWikipediaSummary(adventure.wikipediaSlug)
            _worldUi.update { it.copy(wikiState = wiki, isLoadingWiki = false) }
        }
    }

    // --- 3D WORLD INTERACTION & PRESENCE SYNC ---

    fun updateCamera(next: Camera3D) {
        _worldUi.update { it.copy(camera = next) }
        val active = _worldUi.value.activeSession ?: return
        val now = System.currentTimeMillis()
        if (now - lastPresenceSyncMs >= PRESENCE_SYNC_THROTTLE_MS) {
            lastPresenceSyncMs = now
            viewModelScope.launch {
                val poi = _worldUi.value.selectedPoi
                adventureRepo.updateMyPresence(
                    sessionId = active.id,
                    profile = _userProfile.value,
                    posX = next.x,
                    posY = next.y,
                    posZ = next.z,
                    yawDeg = (next.yawRad * 180f / Math.PI.toFloat()),
                    pointedPoiId = poi?.id.orEmpty(),
                    pointedX = poi?.posX ?: 0f,
                    pointedY = poi?.posY ?: 0f,
                    pointedZ = poi?.posZ ?: 0f,
                    voiceMuted = _worldUi.value.audioMuted
                )
            }
        }
    }

    fun discoverPoi(poi: AdventurePoi, onPlayChime: () -> Unit = {}) {
        val isNew = poi.id !in _worldUi.value.discoveredPoiIds
        _worldUi.update {
            it.copy(
                selectedPoi = poi,
                discoveredPoiIds = it.discoveredPoiIds + poi.id,
                lastDiscoveredPoiMessage = if (isNew) "Discovered: ${poi.name}" else null
            )
        }
        if (isNew) onPlayChime()
    }

    fun pointAtPoi(poi: AdventurePoi) {
        val active = _worldUi.value.activeSession
        _worldUi.update { it.copy(selectedPoi = poi) }
        if (active != null) {
            viewModelScope.launch {
                val cam = _worldUi.value.camera
                adventureRepo.updateMyPresence(
                    sessionId = active.id,
                    profile = _userProfile.value,
                    posX = cam.x,
                    posY = cam.y,
                    posZ = cam.z,
                    yawDeg = (cam.yawRad * 180f / Math.PI.toFloat()),
                    pointedPoiId = poi.id,
                    pointedX = poi.posX,
                    pointedY = poi.posY,
                    pointedZ = poi.posZ,
                    voiceMuted = _worldUi.value.audioMuted
                )
                val myLang = _userProfile.value?.nativeLanguage?.ifBlank { "en" } ?: "en"
                adventureRepo.sendSessionMessage(
                    sessionId = active.id,
                    senderProfile = _userProfile.value,
                    originalText = "Look here: ${poi.name} (${poi.category})",
                    sourceLanguage = myLang,
                    discoveryPoiId = poi.id
                )
            }
        } else {
            _actionStatus.value = "Pointing at ${poi.name} (Solo Preview)." to false
        }
    }

    fun toggleAudioMuted() {
        _worldUi.update { it.copy(audioMuted = !it.audioMuted) }
        val active = _worldUi.value.activeSession ?: return
        viewModelScope.launch {
            val cam = _worldUi.value.camera
            val poi = _worldUi.value.selectedPoi
            adventureRepo.updateMyPresence(
                sessionId = active.id,
                profile = _userProfile.value,
                posX = cam.x,
                posY = cam.y,
                posZ = cam.z,
                yawDeg = (cam.yawRad * 180f / Math.PI.toFloat()),
                pointedPoiId = poi?.id.orEmpty(),
                pointedX = poi?.posX ?: 0f,
                pointedY = poi?.posY ?: 0f,
                pointedZ = poi?.posZ ?: 0f,
                voiceMuted = _worldUi.value.audioMuted
            )
        }
    }

    // --- LIVE ADVENTURE SESSION MANAGEMENT ---

    fun startAdventureSession(
        mode: AdventureSessionMode,
        maxParticipants: Int = 6,
        voicePreferred: Boolean = true,
        level: AdventureLevel = AdventureLevel.BEGINNER,
        durationMinutes: Int = 30,
        availabilitySlot: String = "Live Now",
        crewId: String = "",
        onEnteredSession: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            val adv = _worldUi.value.selectedAdventure
            adventureRepo.createSession(
                adventure = adv,
                mode = mode,
                hostProfile = _userProfile.value,
                maxParticipants = if (mode == AdventureSessionMode.SOLO) 1 else maxParticipants,
                voicePreferred = voicePreferred,
                experienceLevel = level,
                durationMinutes = durationMinutes,
                availabilitySlot = availabilitySlot,
                crewId = crewId
            ).onSuccess { sessionId ->
                attachToSession(sessionId)
                onEnteredSession(sessionId)
            }.onFailure { err ->
                _actionStatus.value = (err.localizedMessage ?: "Failed to start adventure session") to true
            }
        }
    }

    fun joinLiveSession(session: LiveAdventureSession, onJoined: () -> Unit = {}) {
        viewModelScope.launch {
            val adv = AdventureCatalog.findById(session.adventureId)
            selectAdventure(adv)
            adventureRepo.joinSession(session, _userProfile.value)
                .onSuccess {
                    attachToSession(session.id)
                    onJoined()
                }
                .onFailure { err ->
                    _actionStatus.value = (err.localizedMessage ?: "Failed to join session") to true
                }
        }
    }

    fun attachToSession(sessionId: String) {
        activeSessionJob?.cancel()
        activeSessionJob = viewModelScope.launch {
            launch {
                adventureRepo.observeSession(sessionId)
                    .catch { handleStreamError(it, "session") }
                    .collect { sess -> _worldUi.update { it.copy(activeSession = sess) } }
            }
            launch {
                adventureRepo.observeSessionParticipants(sessionId)
                    .catch { handleStreamError(it, "participants") }
                    .collect { list -> _worldUi.update { it.copy(sessionParticipants = list) } }
            }
            launch {
                adventureRepo.observeSessionMessages(sessionId)
                    .catch { handleStreamError(it, "sessionMessages") }
                    .collect { msgs -> _worldUi.update { it.copy(sessionMessages = msgs) } }
            }
        }
    }

    fun leaveActiveSession() {
        val active = _worldUi.value.activeSession ?: return
        activeSessionJob?.cancel()
        activeSessionJob = null
        _worldUi.update {
            it.copy(
                activeSession = null,
                sessionParticipants = emptyList(),
                sessionMessages = emptyList()
            )
        }
        viewModelScope.launch {
            adventureRepo.leaveSession(active)
        }
    }

    // --- NILORA LIVE MULTILINGUAL SESSION COMMUNICATION ---

    fun setTargetTranslationLanguage(code: String) {
        _worldUi.update { it.copy(targetTranslationLanguage = code) }
    }

    fun applyLanguageIdentityDefaults(
        spokenCode: String,
        targetCode: String,
        subtitleMode: SubtitleDisplayMode
    ) {
        _worldUi.update { current ->
            current.copy(
                spokenLanguageCode = spokenCode,
                targetTranslationLanguage = if (current.activeSession == null) targetCode else current.targetTranslationLanguage,
                showSubtitles = subtitleMode != SubtitleDisplayMode.NONE
            )
        }
    }

    fun toggleSubtitles() {
        _worldUi.update { it.copy(showSubtitles = !it.showSubtitles) }
    }

    fun toggleSpeakTranslations() {
        _worldUi.update { it.copy(speakTranslationsAloud = !it.speakTranslationsAloud) }
    }

    fun speakMessage(text: String, languageCode: String) {
        ttsEngine.speak(text, NiloraLanguage.fromCode(languageCode))
    }

    fun sendSessionTextMessage(text: String) {
        val clean = text.trim()
        val active = _worldUi.value.activeSession ?: return
        if (clean.isBlank()) return
        val myLang = _worldUi.value.spokenLanguageCode.ifBlank { _userProfile.value?.nativeLanguage?.ifBlank { "en" } ?: "en" }
        val targetLang = _worldUi.value.targetTranslationLanguage

        viewModelScope.launch {
            _worldUi.update { it.copy(isTranslatingChat = true) }
            val translations = mutableMapOf<String, String>()
            var note = ""
            if (!targetLang.equals(myLang, ignoreCase = true)) {
                translationEngine.translateWorldLanguage(
                    sourceText = clean,
                    sourceCode = myLang,
                    targetCode = targetLang,
                    domain = TranslationContextDomain.ADVENTURE_INSTRUCTIONS
                )
                    .onSuccess { out ->
                        translations[targetLang] = out.translatedText
                        note = out.ambiguityWarning.ifBlank { out.validationWarnings.joinToString("; ") }
                    }
                    .onFailure { err ->
                        note = err.localizedMessage ?: "Translation unavailable"
                    }
            }
            adventureRepo.sendSessionMessage(
                sessionId = active.id,
                senderProfile = _userProfile.value,
                originalText = clean,
                sourceLanguage = myLang,
                translations = translations,
                translationNote = note,
                discoveryPoiId = _worldUi.value.selectedPoi?.id.orEmpty()
            ).onFailure { err ->
                _actionStatus.value = (err.localizedMessage ?: "Failed to send message") to true
            }
            _worldUi.update { it.copy(isTranslatingChat = false) }
        }
    }

    fun dictateAndSendSessionVoice() {
        val myLang = _userProfile.value?.nativeLanguage?.ifBlank { "en" } ?: "en"
        if (!sttEngine.isAvailable()) {
            _actionStatus.value = "Speech recognition unavailable on this device. Please type your message." to true
            return
        }
        _worldUi.update { it.copy(isListeningSpeech = true) }
        sttEngine.startRecognition(
            language = NiloraLanguage.fromCode(myLang),
            onSpeechDetected = {},
            onPartialResult = {},
            onFinalResult = { result ->
                _worldUi.update { it.copy(isListeningSpeech = false) }
                if (result.transcript.isNotBlank()) {
                    sendSessionTextMessage(result.transcript)
                }
            },
            onError = { errMsg ->
                _worldUi.update { it.copy(isListeningSpeech = false) }
                _actionStatus.value = errMsg to true
            }
        )
    }

    fun translateSessionMessage(message: SessionChatMessage) {
        val active = _worldUi.value.activeSession ?: return
        val targetLang = _worldUi.value.targetTranslationLanguage
        val existing = message.translations[targetLang]
        if (!existing.isNullOrBlank()) {
            if (_worldUi.value.speakTranslationsAloud) ttsEngine.speak(existing, NiloraLanguage.fromCode(targetLang))
            return
        }
        viewModelScope.launch {
            _worldUi.update { it.copy(isTranslatingChat = true) }
            translationEngine.translateWorldLanguage(message.originalText, message.sourceLanguage, targetLang)
                .onSuccess { out ->
                    val updated = message.translations + (targetLang to out.translatedText)
                    val note = out.ambiguityWarning.ifBlank { out.validationWarnings.joinToString("; ") }
                    adventureRepo.cacheSessionMessageTranslation(
                        sessionId = active.id,
                        messageId = message.id,
                        updatedTranslations = updated,
                        note = note
                    )
                    if (_worldUi.value.speakTranslationsAloud) {
                        ttsEngine.speak(out.translatedText, NiloraLanguage.fromCode(targetLang))
                    }
                }
                .onFailure { err ->
                    _actionStatus.value = (err.localizedMessage ?: "Translation failed") to true
                }
            _worldUi.update { it.copy(isTranslatingChat = false) }
        }
    }

    // --- ADVENTURE MATCHMAKING ---

    fun publishMatchAvailability(
        adventure: AdventureExperience,
        interests: List<String>,
        preferredLang: String,
        spokenLangs: List<String>,
        level: AdventureLevel,
        durationMinutes: Int,
        availableSlot: String,
        voicePreferred: Boolean,
        isAvailable: Boolean
    ) {
        viewModelScope.launch {
            val me = _userProfile.value
            val profile = AdventureMatchProfile(
                userId = currentUserId,
                displayName = me?.displayName?.ifBlank { "NILORA Explorer" } ?: "NILORA Explorer",
                country = me?.country?.ifBlank { "Egypt" } ?: "Egypt",
                targetAdventureId = adventure.id,
                targetDestination = adventure.destinationName,
                interests = interests,
                preferredLanguage = preferredLang,
                spokenLanguages = spokenLangs,
                experienceLevel = level.key,
                preferredDurationMinutes = durationMinutes,
                availableTimeSlot = availableSlot,
                voicePreferred = voicePreferred,
                isAvailable = isAvailable
            )
            adventureRepo.saveMyMatchProfile(profile)
                .onSuccess {
                    _actionStatus.value = if (isAvailable) {
                        "Your Adventure Match profile is live for real explorers." to false
                    } else {
                        "Adventure Match visibility paused." to false
                    }
                }
                .onFailure { err ->
                    _actionStatus.value = (err.localizedMessage ?: "Failed to update match preferences") to true
                }
        }
    }

    // --- SHARED POST-ADVENTURE JOURNEY & NILORA CREWS ---

    fun saveAdventureToMemory(
        adventure: AdventureExperience,
        summaryNote: String,
        partners: List<SessionParticipantPresence>,
        visibility: MemoryVisibility
    ) {
        viewModelScope.launch {
            val otherIds = partners.map { it.userId }.filter { it != currentUserId }.distinct()
            val otherNames = partners.filter { it.userId != currentUserId }.map { it.displayName }.distinct()
            val countries = (partners.map { it.country } + (_userProfile.value?.country ?: "Egypt")).filter { it.isNotBlank() }.distinct()
            val body = buildString {
                append(summaryNote.trim().ifBlank { adventure.description })
                val found = _worldUi.value.discoveredPoiIds
                if (found.isNotEmpty()) {
                    append("\n\nDiscovered in 3D (${found.size}/${adventure.pois.size}): ")
                    append(adventure.pois.filter { it.id in found }.joinToString(", ") { it.name })
                }
            }
            experienceRepo.saveNiloraMemory(
                myProfile = _userProfile.value,
                memoryType = MemoryType.WORLD_POSTCARD,
                sourceId = adventure.id,
                title = "${adventure.destinationName}: ${adventure.title}",
                summaryText = body,
                originalLanguage = _userProfile.value?.nativeLanguage ?: "en",
                translations = emptyMap<String, String>(),
                countries = countries,
                partnerIds = otherIds,
                partnerNames = otherNames,
                placeLabel = "${adventure.destinationName}, ${adventure.regionLabel}",
                photoBase64 = "",
                audioBase64 = "",
                audioDurationMs = 0L,
                retainAudioConsent = false,
                visibility = visibility
            ).onSuccess {
                _actionStatus.value = "Saved adventure memory to NILORA Memory." to false
            }.onFailure { err ->
                _actionStatus.value = (err.localizedMessage ?: "Failed to save memory") to true
            }
        }
    }

    fun sendCompanionConnectionRequest(
        partnerId: String,
        partnerName: String,
        partnerCountry: String,
        partnerLanguage: String,
        adventureTitle: String,
        onOpenedChat: (String) -> Unit
    ) {
        viewModelScope.launch {
            experienceRepo.openPrivateContinuation(
                myProfile = _userProfile.value,
                partnerId = partnerId,
                partnerName = partnerName,
                partnerCountry = partnerCountry,
                partnerLanguage = partnerLanguage,
                contextOriginTitle = adventureTitle
            ).onSuccess { convId ->
                _actionStatus.value = "Mutual consent chat opened with $partnerName." to false
                onOpenedChat(convId)
            }.onFailure { err ->
                _actionStatus.value = (err.localizedMessage ?: "Failed to open chat") to true
            }
        }
    }

    fun createCrew(
        name: String,
        description: String,
        focusDestination: String,
        sharedInterests: List<String>,
        plannedAdventureId: String?,
        historyTitle: String?,
        joinPolicy: String,
        voiceEnabled: Boolean,
        onCreated: (String) -> Unit
    ) {
        viewModelScope.launch {
            adventureRepo.createCrew(
                name = name,
                description = description,
                focusDestination = focusDestination,
                sharedInterests = sharedInterests,
                plannedAdventureId = plannedAdventureId,
                historyTitle = historyTitle,
                joinPolicy = joinPolicy,
                voiceChannelEnabled = voiceEnabled,
                leaderProfile = _userProfile.value
            ).onSuccess { id ->
                _actionStatus.value = "NILORA Crew \"$name\" created." to false
                onCreated(id)
            }.onFailure { err ->
                _actionStatus.value = (err.localizedMessage ?: "Failed to create crew") to true
            }
        }
    }

    fun joinCrew(crew: NiloraCrew, onJoined: () -> Unit = {}) {
        viewModelScope.launch {
            adventureRepo.joinOpenCrew(crew, _userProfile.value)
                .onSuccess {
                    _actionStatus.value = "Joined crew ${crew.name}." to false
                    onJoined()
                }
                .onFailure { err ->
                    _actionStatus.value = (err.localizedMessage ?: "Failed to join crew") to true
                }
        }
    }

    fun openCrewDetail(crewId: String) {
        crewDetailJob?.cancel()
        crewDetailJob = viewModelScope.launch {
            launch {
                adventureRepo.observeCrewDetail(crewId)
                    .catch { handleStreamError(it, "crewDetail") }
                    .collect { _selectedCrew.value = it }
            }
            launch {
                adventureRepo.observeCrewMessages(crewId)
                    .catch { handleStreamError(it, "crewMessages") }
                    .collect { _crewMessages.value = it }
            }
        }
    }

    fun sendCrewMessage(crewId: String, text: String) {
        val clean = text.trim()
        if (clean.isBlank()) return
        val myLang = _userProfile.value?.nativeLanguage?.ifBlank { "en" } ?: "en"
        viewModelScope.launch {
            adventureRepo.sendCrewMessage(crewId, _userProfile.value, clean, myLang)
                .onFailure { err ->
                    _actionStatus.value = (err.localizedMessage ?: "Failed to send crew message") to true
                }
        }
    }

    fun translateCrewMessage(crewId: String, msg: CrewMessage, targetLang: String) {
        if (!msg.translations[targetLang].isNullOrBlank()) return
        viewModelScope.launch {
            translationEngine.translateWorldLanguage(msg.originalText, msg.sourceLanguage, targetLang)
                .onSuccess { out ->
                    val updated = msg.translations + (targetLang to out.translatedText)
                    adventureRepo.cacheCrewMessageTranslation(crewId, msg.id, updated)
                }
                .onFailure { err ->
                    _actionStatus.value = (err.localizedMessage ?: "Translation failed") to true
                }
        }
    }

    fun planCrewAdventure(crew: NiloraCrew, adventure: AdventureExperience) {
        viewModelScope.launch {
            adventureRepo.planCrewAdventure(crew, adventure)
                .onSuccess {
                    _actionStatus.value = "Added ${adventure.title} to crew itinerary." to false
                }
                .onFailure { err ->
                    _actionStatus.value = (err.localizedMessage ?: "Failed to update crew plan") to true
                }
        }
    }

    fun clearStatus() {
        _actionStatus.value = null to false
    }

    override fun onCleared() {
        super.onCleared()
        activeSessionJob?.cancel()
        crewDetailJob?.cancel()
    }
}
