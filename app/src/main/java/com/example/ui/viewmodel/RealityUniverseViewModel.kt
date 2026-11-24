package com.example.ui.viewmodel

import android.content.Context
import android.net.Uri
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

data class RealityFilterState(
    val selectedCity: String = "ALL", // "ALL" or city name from GlobalCityHubCatalog / custom
    val maxDistanceKm: Int? = null,   // null = no GPS distance filter
    val eventCategory: RealityEventCategory? = null,
    val eventVerificationFilter: EventVerificationTier? = null,
    val freeVerifiedOnly: Boolean = false,
    val upcomingOrActiveOnly: Boolean = true,
    val activityDomain: ActivityDomain? = null,
    val activitySubCategory: ActivitySubCategory? = null,
    val activitySkillLevel: ActivitySkillLevel? = null,
    val openSlotsOnly: Boolean = false,
    val missionModeFilter: MissionExecutionMode? = null,
    val challengeScopeFilter: ChallengeScope? = null
)

class RealityUniverseViewModel(
    private val realityRepo: RealityUniverseRepository,
    private val eventsIntelRepo: RealityEventsIntelligenceRepository,
    private val experienceRepo: ExperienceConnectionRepository,
    private val communityRepo: CommunityAndMessagingRepository,
    private val firestoreRepo: FirestoreDataRepository,
    private val locationRepo: LocationAndPlacesRepository,
    private val translationEngine: EgyptianTranslationEngine,
    private val ttsEngine: VoiceGenerationEngine,
    val currentUserId: String
) : ViewModel() {

    companion object {
        private const val TAG = "RealityUniverseVM"
        private const val STOP_TIMEOUT_MS = 5000L
    }

    private val _userProfile = MutableStateFlow<UserProfile?>(null)
    val myProfile: StateFlow<UserProfile?> = _userProfile.asStateFlow()

    private val _filters = MutableStateFlow(RealityFilterState())
    val filters: StateFlow<RealityFilterState> = _filters.asStateFlow()

    private val _localGps = MutableStateFlow<GpsCoordinates?>(null)
    val localGps: StateFlow<GpsCoordinates?> = _localGps.asStateFlow()

    private val _gpsMessage = MutableStateFlow("")
    val gpsMessage: StateFlow<String> = _gpsMessage.asStateFlow()

    private val _preferredSpokenLanguage = MutableStateFlow("en")
    private val _preferredTargetLanguage = MutableStateFlow("ar-EG")
    private val _preferEgyptianArabic = MutableStateFlow(true)

    private val _externalEvents = MutableStateFlow<LiveIntelligenceResult<List<ExternalOpenSourceEventNote>>?>(null)
    val externalEvents: StateFlow<LiveIntelligenceResult<List<ExternalOpenSourceEventNote>>?> = _externalEvents.asStateFlow()

    private val _isLoadingExternalEvents = MutableStateFlow(false)
    val isLoadingExternalEvents: StateFlow<Boolean> = _isLoadingExternalEvents.asStateFlow()

    private val _missionPhotoBase64 = MutableStateFlow("")
    val missionPhotoBase64: StateFlow<String> = _missionPhotoBase64.asStateFlow()

    private val _actionStatus = MutableStateFlow<Pair<String?, Boolean>>(null to false)
    val actionStatus: StateFlow<Pair<String?, Boolean>> = _actionStatus.asStateFlow()

    // Detail observation flows
    private val _selectedEventId = MutableStateFlow<String?>(null)
    private val _eventMessages = MutableStateFlow<List<RealityEventMessage>>(emptyList())
    val eventMessages: StateFlow<List<RealityEventMessage>> = _eventMessages.asStateFlow()

    private val _selectedActivityId = MutableStateFlow<String?>(null)
    private val _activityMessages = MutableStateFlow<List<ActivityGroupMessage>>(emptyList())
    val activityMessages: StateFlow<List<ActivityGroupMessage>> = _activityMessages.asStateFlow()

    private val _selectedMissionId = MutableStateFlow<String?>(null)
    private val _missionSubmissions = MutableStateFlow<List<MissionSubmission>>(emptyList())
    val missionSubmissions: StateFlow<List<MissionSubmission>> = _missionSubmissions.asStateFlow()

    private val _selectedChallengeId = MutableStateFlow<String?>(null)
    private val _challengeEntries = MutableStateFlow<List<ChallengeEntry>>(emptyList())
    val challengeEntries: StateFlow<List<ChallengeEntry>> = _challengeEntries.asStateFlow()

    private var eventMsgJob: Job? = null
    private var activityMsgJob: Job? = null
    private var missionSubJob: Job? = null
    private var challengeEntryJob: Job? = null

    val eventsState: StateFlow<UiState<List<RealityEvent>>> =
        realityRepo.observeRealityEvents().toUiState("reality_events")

    val teamActivitiesState: StateFlow<UiState<List<TeamActivityPost>>> =
        realityRepo.observeTeamActivities().toUiState("reality_activities")

    val missionsState: StateFlow<UiState<List<RealityMission>>> =
        realityRepo.observeMissions().toUiState("reality_missions")

    val challengesState: StateFlow<UiState<List<GlobalChallenge>>> =
        realityRepo.observeChallenges().toUiState("global_challenges")

    val bridgeLinksState: StateFlow<UiState<List<RealityBridgeLink>>> =
        realityRepo.observeMyRealityBridge(currentUserId).toUiState("reality_bridge")

    init {
        viewModelScope.launch {
            firestoreRepo.observeUserProfile(currentUserId)
                .catch { handleStreamError(it, "profile") }
                .collect { profile ->
                    _userProfile.value = profile
                    if (profile != null && profile.city.isNotBlank() && _filters.value.selectedCity == "ALL") {
                        // Do not hide other cities by default; still load open-source reference for user's city
                        refreshExternalEventFeed(profile.city, profile.country.ifBlank { "Egypt" })
                    }
                }
        }
        refreshExternalEventFeed("Cairo", "Egypt")
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

    fun clearStatus() {
        _actionStatus.value = null to false
    }

    fun applyLanguageIdentityDefaults(
        spokenCode: String,
        targetCode: String,
        preferEgyptian: Boolean
    ) {
        _preferredSpokenLanguage.value = spokenCode
        _preferredTargetLanguage.value = targetCode
        _preferEgyptianArabic.value = preferEgyptian
    }

    private fun notifyResult(res: Result<*>, okMessage: String) {
        res.onSuccess { _actionStatus.value = okMessage to false }
            .onFailure { err ->
                _actionStatus.value = (err.localizedMessage ?: "Action failed") to true
            }
    }

    // --- LOCAL LIFE CITY & OPT-IN GPS FILTERS ---

    fun selectCityHub(cityName: String) {
        _filters.update { it.copy(selectedCity = cityName) }
        val hub = GlobalCityHubCatalog.findHub(cityName)
        if (hub != null) {
            refreshExternalEventFeed(hub.cityName, hub.countryName)
        } else if (cityName != "ALL") {
            refreshExternalEventFeed(cityName, _userProfile.value?.country ?: "Egypt")
        }
    }

    fun setMaxDistanceKm(km: Int?) {
        _filters.update { it.copy(maxDistanceKm = km) }
    }

    fun requestLocalGpsForDiscovery() {
        viewModelScope.launch {
            _gpsMessage.value = "Checking device GPS locally (never shared)..."
            locationRepo.getCurrentGpsLocation()
                .onSuccess { coords ->
                    _localGps.value = coords
                    val nearest = GlobalCityHubCatalog.hubs.minByOrNull { hub ->
                        geoDistanceKm(coords.latitude, coords.longitude, hub.latitude, hub.longitude)
                    }
                    _gpsMessage.value = if (nearest != null) {
                        val dist = geoDistanceKm(coords.latitude, coords.longitude, nearest.latitude, nearest.longitude)
                        "GPS locked locally • Nearest hub: ${nearest.cityName} (${String.format("%.0f", dist)} km)"
                    } else {
                        "GPS locked locally (stays on device)."
                    }
                }
                .onFailure { err ->
                    _gpsMessage.value = err.localizedMessage ?: "GPS unavailable — use manual city picker."
                }
        }
    }

    fun refreshExternalEventFeed(cityName: String, countryName: String) {
        viewModelScope.launch {
            _isLoadingExternalEvents.value = true
            _externalEvents.value = eventsIntelRepo.fetchOpenSourceEventContext(cityName, countryName)
            _isLoadingExternalEvents.value = false
        }
    }

    fun setEventCategoryFilter(cat: RealityEventCategory?) {
        _filters.update { it.copy(eventCategory = cat) }
    }

    fun setEventVerificationFilter(tier: EventVerificationTier?) {
        _filters.update { it.copy(eventVerificationFilter = tier) }
    }

    fun toggleFreeVerifiedOnly() {
        _filters.update { it.copy(freeVerifiedOnly = !it.freeVerifiedOnly) }
    }

    fun toggleUpcomingOnly() {
        _filters.update { it.copy(upcomingOrActiveOnly = !it.upcomingOrActiveOnly) }
    }

    fun setActivityDomainFilter(domain: ActivityDomain?) {
        _filters.update { it.copy(activityDomain = domain, activitySubCategory = null) }
    }

    fun setActivitySubCategoryFilter(sub: ActivitySubCategory?) {
        _filters.update { it.copy(activitySubCategory = sub) }
    }

    fun setActivitySkillFilter(level: ActivitySkillLevel?) {
        _filters.update { it.copy(activitySkillLevel = level) }
    }

    fun toggleOpenSlotsOnly() {
        _filters.update { it.copy(openSlotsOnly = !it.openSlotsOnly) }
    }

    fun setMissionModeFilter(mode: MissionExecutionMode?) {
        _filters.update { it.copy(missionModeFilter = mode) }
    }

    fun setChallengeScopeFilter(scope: ChallengeScope?) {
        _filters.update { it.copy(challengeScopeFilter = scope) }
    }

    // --- DETAIL OBSERVATION ---

    fun openEventDetail(eventId: String, isParticipant: Boolean) {
        if (_selectedEventId.value == eventId && eventMsgJob != null) return
        _selectedEventId.value = eventId
        eventMsgJob?.cancel()
        if (!isParticipant) {
            _eventMessages.value = emptyList()
            return
        }
        eventMsgJob = viewModelScope.launch {
            realityRepo.observeEventMessages(eventId)
                .catch { handleStreamError(it, "event_messages") }
                .collect { _eventMessages.value = it }
        }
    }

    fun openActivityDetail(activityId: String, isParticipant: Boolean) {
        if (_selectedActivityId.value == activityId && activityMsgJob != null) return
        _selectedActivityId.value = activityId
        activityMsgJob?.cancel()
        if (!isParticipant) {
            _activityMessages.value = emptyList()
            return
        }
        activityMsgJob = viewModelScope.launch {
            realityRepo.observeActivityMessages(activityId)
                .catch { handleStreamError(it, "activity_messages") }
                .collect { _activityMessages.value = it }
        }
    }

    fun openMissionDetail(missionId: String) {
        if (_selectedMissionId.value == missionId && missionSubJob != null) return
        _selectedMissionId.value = missionId
        missionSubJob?.cancel()
        missionSubJob = viewModelScope.launch {
            realityRepo.observeMissionSubmissions(missionId)
                .catch { handleStreamError(it, "mission_submissions") }
                .collect { _missionSubmissions.value = it }
        }
    }

    fun openChallengeDetail(challengeId: String) {
        if (_selectedChallengeId.value == challengeId && challengeEntryJob != null) return
        _selectedChallengeId.value = challengeId
        challengeEntryJob?.cancel()
        challengeEntryJob = viewModelScope.launch {
            realityRepo.observeChallengeEntries(challengeId)
                .catch { handleStreamError(it, "challenge_entries") }
                .collect { _challengeEntries.value = it }
        }
    }

    // --- ACTIONS: REALITY EVENTS ---

    fun createRealityEvent(
        title: String,
        category: RealityEventCategory,
        description: String,
        cityName: String,
        countryName: String,
        countryCode: String,
        venueName: String,
        startDateIso: String,
        timeWindowLabel: String,
        freeVerified: Boolean,
        priceNote: String,
        organizerName: String,
        organizerType: EventOrganizerType,
        officialUrl: String,
        conditionsNote: String,
        safetyNote: String,
        maxParticipants: Int,
        joinPolicy: String,
        isAdmin: Boolean,
        onCreated: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            val profile = _userProfile.value
            val hub = GlobalCityHubCatalog.findHub(cityName)
            val res = realityRepo.createRealityEvent(
                creatorName = profile?.displayName ?: "NILORA Member",
                title = title,
                category = category,
                description = description,
                countryCode = countryCode,
                countryName = countryName,
                cityName = cityName,
                venueName = venueName,
                latitude = hub?.latitude,
                longitude = hub?.longitude,
                startDateIso = startDateIso,
                endDateIso = "",
                timeWindowLabel = timeWindowLabel,
                freeVerified = freeVerified,
                priceNote = priceNote,
                organizerName = organizerName,
                organizerType = organizerType,
                officialUrl = officialUrl,
                conditionsNote = conditionsNote,
                safetyNote = safetyNote,
                ageMinimum = 18,
                maxParticipants = maxParticipants,
                joinPolicy = joinPolicy,
                isAdmin = isAdmin,
                adminVerifiedImmediately = false
            )
            res.onSuccess { newId ->
                _actionStatus.value = "Event published as Unverified / User Submitted." to false
                onCreated(newId)
            }.onFailure { err ->
                _actionStatus.value = (err.localizedMessage ?: "Failed to create event") to true
            }
        }
    }

    fun joinOrRequestEvent(event: RealityEvent) {
        viewModelScope.launch {
            val myName = _userProfile.value?.displayName ?: "NILORA Member"
            val res = realityRepo.joinOrRequestEvent(event, myName)
            res.onSuccess { outcome ->
                val msg = if (outcome == "REQUESTED") {
                    "Join request sent to ${event.organizerName}."
                } else {
                    "Joined ${event.title}."
                }
                _actionStatus.value = msg to false
                openEventDetail(event.id, outcome == "JOINED")
                if (outcome == "JOINED" && event.linkedAdventureId.isNotBlank()) {
                    realityRepo.upsertRealityBridge(
                        virtualAdventureId = event.linkedAdventureId,
                        virtualAdventureTitle = event.linkedAdventureId,
                        destinationName = event.cityName,
                        targetType = "REALITY_EVENT",
                        targetId = event.id,
                        targetTitle = event.title,
                        stage = RealityBridgeStage.REGISTERED
                    )
                }
            }.onFailure { err ->
                _actionStatus.value = (err.localizedMessage ?: "Could not join event") to true
            }
        }
    }

    fun leaveEvent(event: RealityEvent) {
        viewModelScope.launch {
            notifyResult(realityRepo.leaveEvent(event), "Left ${event.title}.")
        }
    }

    fun approveEventRequest(event: RealityEvent, requesterId: String, approve: Boolean) {
        viewModelScope.launch {
            notifyResult(
                realityRepo.approveEventRequest(event, requesterId, "Member", approve),
                if (approve) "Participant approved." else "Request declined."
            )
        }
    }

    fun updateEventStatus(eventId: String, status: String) {
        viewModelScope.launch {
            notifyResult(realityRepo.updateRealityEventStatus(eventId, status), "Event status updated to $status.")
        }
    }

    fun moderateRealityEventVerification(eventId: String, tier: EventVerificationTier) {
        viewModelScope.launch {
            val label = if (tier == EventVerificationTier.ADMIN_VERIFIED) "Verified by NILORA Admin" else "Marked Unverified"
            notifyResult(
                realityRepo.moderateRealityEventVerification(eventId, tier, label),
                "Event verification set to ${tier.labelEn}."
            )
        }
    }

    fun sendEventMessage(eventId: String, text: String) {
        viewModelScope.launch {
            val p = _userProfile.value
            val lang = _preferredSpokenLanguage.value.ifBlank { p?.nativeLanguage?.ifBlank { p.preferredLanguage } ?: "en" }
            notifyResult(
                realityRepo.sendEventMessage(eventId, p?.displayName ?: "Participant", text, lang),
                ""
            )
        }
    }

    fun translateAndSpeakEventMessage(eventId: String, msg: RealityEventMessage) {
        viewModelScope.launch {
            val targetLangCode = _preferredTargetLanguage.value.ifBlank { _userProfile.value?.preferredLanguage ?: "en" }
            val targetLang = NiloraLanguage.fromCode(targetLangCode)
            val sourceLang = NiloraLanguage.fromCode(msg.sourceLanguage)
            val cached = msg.translations[targetLang.code]
            if (!cached.isNullOrBlank()) {
                ttsEngine.speak(cached, targetLang)
                return@launch
            }
            val result = translationEngine.translate(
                TranslationRequest(
                    sourceText = msg.originalText,
                    sourceLanguage = sourceLang,
                    targetLanguage = targetLang,
                    arabicDialectMode = if (_preferEgyptianArabic.value) ArabicDialectMode.EGYPTIAN_COLLOQUIAL else ArabicDialectMode.MODERN_STANDARD,
                    domain = TranslationContextDomain.TOURISM
                )
            )
            result.onSuccess { out ->
                realityRepo.cacheEventMessageTranslation(eventId, msg.id, msg.translations, targetLang.code, out.translatedText)
                ttsEngine.speak(out.translatedText, targetLang)
            }.onFailure { err ->
                _actionStatus.value = (err.localizedMessage ?: "Translation unavailable") to true
            }
        }
    }

    // --- ACTIONS: GO / PLAY / TEAM-UP / COMMUNITY IMPACT ---

    fun createTeamActivity(
        domain: ActivityDomain,
        subCategory: ActivitySubCategory,
        title: String,
        description: String,
        cityName: String,
        countryName: String,
        publicMeetingSpot: String,
        scheduledDateLabel: String,
        skillLevel: ActivitySkillLevel,
        equipmentNote: String,
        safetyRules: String,
        joinPolicy: String,
        maxParticipants: Int,
        impactOrganizationName: String = "",
        linkedAdventureId: String = "",
        onCreated: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            val p = _userProfile.value
            val hub = GlobalCityHubCatalog.findHub(cityName)
            val res = realityRepo.createTeamActivity(
                creatorName = p?.displayName ?: "NILORA Member",
                domain = domain,
                subCategory = subCategory,
                title = title,
                description = description,
                countryName = countryName,
                cityName = cityName,
                publicMeetingSpot = publicMeetingSpot,
                latitude = hub?.latitude,
                longitude = hub?.longitude,
                scheduledDateLabel = scheduledDateLabel,
                skillLevel = skillLevel,
                equipmentNote = equipmentNote,
                safetyRules = safetyRules,
                ageMinimum = 18,
                joinPolicy = joinPolicy,
                maxParticipants = maxParticipants,
                impactOrganizationName = impactOrganizationName,
                linkedAdventureId = linkedAdventureId
            )
            res.onSuccess { newId ->
                _actionStatus.value = "Team-Up post published." to false
                onCreated(newId)
            }.onFailure { err ->
                _actionStatus.value = (err.localizedMessage ?: "Failed to create activity") to true
            }
        }
    }

    fun joinOrRequestActivity(activity: TeamActivityPost) {
        viewModelScope.launch {
            val myName = _userProfile.value?.displayName ?: "NILORA Member"
            val res = realityRepo.joinOrRequestActivity(activity, myName)
            res.onSuccess { outcome ->
                val msg = if (outcome == "REQUESTED") {
                    "Join request sent to ${activity.creatorName}."
                } else {
                    "You joined ${activity.title}."
                }
                _actionStatus.value = msg to false
                openActivityDetail(activity.id, outcome == "JOINED")
            }.onFailure { err ->
                _actionStatus.value = (err.localizedMessage ?: "Could not join activity") to true
            }
        }
    }

    fun leaveActivity(activity: TeamActivityPost) {
        viewModelScope.launch {
            notifyResult(realityRepo.leaveActivity(activity), "Left ${activity.title}.")
        }
    }

    fun approveActivityRequest(activity: TeamActivityPost, requesterId: String, approve: Boolean) {
        viewModelScope.launch {
            notifyResult(
                realityRepo.approveActivityRequest(activity, requesterId, "Teammate", approve),
                if (approve) "Teammate approved." else "Request declined."
            )
        }
    }

    fun updateActivityStatus(activityId: String, status: TeamUpStatus) {
        viewModelScope.launch {
            notifyResult(realityRepo.updateActivityStatus(activityId, status), "Status updated to ${status.labelEn}.")
        }
    }

    fun moderateImpactVerification(activityId: String, verified: Boolean) {
        viewModelScope.launch {
            notifyResult(
                realityRepo.moderateImpactVerification(activityId, verified),
                if (verified) "Community Impact verified." else "Verification removed."
            )
        }
    }

    fun sendActivityMessage(activityId: String, text: String) {
        viewModelScope.launch {
            val p = _userProfile.value
            val lang = _preferredSpokenLanguage.value.ifBlank { p?.nativeLanguage?.ifBlank { p.preferredLanguage } ?: "en" }
            notifyResult(
                realityRepo.sendActivityMessage(activityId, p?.displayName ?: "Member", text, lang),
                ""
            )
        }
    }

    fun translateAndSpeakActivityMessage(activityId: String, msg: ActivityGroupMessage) {
        viewModelScope.launch {
            val targetLangCode = _preferredTargetLanguage.value.ifBlank { _userProfile.value?.preferredLanguage ?: "en" }
            val targetLang = NiloraLanguage.fromCode(targetLangCode)
            val sourceLang = NiloraLanguage.fromCode(msg.sourceLanguage)
            val cached = msg.translations[targetLang.code]
            if (!cached.isNullOrBlank()) {
                ttsEngine.speak(cached, targetLang)
                return@launch
            }
            val result = translationEngine.translate(
                TranslationRequest(
                    sourceText = msg.originalText,
                    sourceLanguage = sourceLang,
                    targetLanguage = targetLang,
                    arabicDialectMode = if (_preferEgyptianArabic.value) ArabicDialectMode.EGYPTIAN_COLLOQUIAL else ArabicDialectMode.MODERN_STANDARD,
                    domain = TranslationContextDomain.SPORTS
                )
            )
            result.onSuccess { out ->
                realityRepo.cacheActivityMessageTranslation(activityId, msg.id, msg.translations, targetLang.code, out.translatedText)
                ttsEngine.speak(out.translatedText, targetLang)
            }.onFailure { err ->
                _actionStatus.value = (err.localizedMessage ?: "Translation unavailable") to true
            }
        }
    }

    // --- ACTIONS: REAL-WORLD MISSIONS ---

    fun attachMissionPhoto(context: Context, uri: Uri?) {
        if (uri == null) return
        viewModelScope.launch {
            ImageEncodingHelper.encodePickerUriToBase64(context, uri)
                .onSuccess { b64 ->
                    _missionPhotoBase64.value = b64
                    _actionStatus.value = "Mission evidence photo attached." to false
                }
                .onFailure { err ->
                    _actionStatus.value = (err.localizedMessage ?: "Failed to attach photo") to true
                }
        }
    }

    fun clearMissionPhoto() {
        _missionPhotoBase64.value = ""
    }

    fun launchMissionFromTemplate(template: CuratedRealityMissionTemplate, onCreated: (String) -> Unit = {}) {
        viewModelScope.launch {
            val p = _userProfile.value
            val res = realityRepo.createMission(
                creatorName = p?.displayName ?: "Explorer",
                templateType = template.templateType,
                executionMode = template.executionMode,
                title = template.title,
                objective = template.objective,
                cityName = template.cityName,
                countryName = template.countryName,
                latitude = template.latitude,
                longitude = template.longitude,
                safetyAndEcoRules = template.safetyAndEcoRules,
                targetSteps = template.suggestedSteps
            )
            res.onSuccess { id ->
                _actionStatus.value = "Mission launched." to false
                onCreated(id)
            }.onFailure { err ->
                _actionStatus.value = (err.localizedMessage ?: "Could not launch mission") to true
            }
        }
    }

    fun createCustomMission(
        executionMode: MissionExecutionMode,
        title: String,
        objective: String,
        cityName: String,
        countryName: String,
        safetyRules: String,
        targetSteps: Int,
        onCreated: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            val p = _userProfile.value
            val hub = GlobalCityHubCatalog.findHub(cityName)
            val res = realityRepo.createMission(
                creatorName = p?.displayName ?: "Explorer",
                templateType = "CUSTOM",
                executionMode = executionMode,
                title = title,
                objective = objective,
                cityName = cityName,
                countryName = countryName,
                latitude = hub?.latitude,
                longitude = hub?.longitude,
                safetyAndEcoRules = safetyRules,
                targetSteps = targetSteps
            )
            res.onSuccess { id ->
                _actionStatus.value = "Mission created." to false
                onCreated(id)
            }.onFailure { err ->
                _actionStatus.value = (err.localizedMessage ?: "Could not create mission") to true
            }
        }
    }

    fun submitMissionEvidence(
        mission: RealityMission,
        reflectionText: String,
        placeLabel: String,
        ecoSafetyConfirmed: Boolean
    ) {
        viewModelScope.launch {
            val p = _userProfile.value
            val photo = _missionPhotoBase64.value
            val res = realityRepo.submitMissionEvidence(
                mission = mission,
                authorName = p?.displayName ?: "Explorer",
                reflectionText = reflectionText,
                placeLabel = placeLabel,
                photoBase64 = photo,
                ecoSafetyConfirmed = ecoSafetyConfirmed
            )
            res.onSuccess {
                _missionPhotoBase64.value = ""
                _actionStatus.value = if (mission.executionMode == MissionExecutionMode.VIRTUAL.key) {
                    "Virtual study log saved (kept separate from real-world physical logs)."
                } else {
                    "Real-world field log recorded!"
                } to false
            }.onFailure { err ->
                _actionStatus.value = (err.localizedMessage ?: "Failed to record mission evidence") to true
            }
        }
    }

    fun saveMissionToNiloraMemory(mission: RealityMission, reflectionSummary: String) {
        viewModelScope.launch {
            val p = _userProfile.value
            val res = experienceRepo.saveNiloraMemory(
                myProfile = p,
                memoryType = MemoryType.WORLD_QUEST,
                sourceId = mission.id,
                title = mission.title,
                summaryText = reflectionSummary.ifBlank { mission.objective },
                originalLanguage = p?.preferredLanguage ?: "en",
                translations = emptyMap(),
                countries = listOf(mission.countryName),
                partnerIds = mission.participantIds.filter { it != currentUserId },
                partnerNames = emptyList(),
                placeLabel = "${mission.cityName}, ${mission.countryName}",
                photoBase64 = "",
                audioBase64 = "",
                audioDurationMs = 0L,
                retainAudioConsent = false,
                visibility = MemoryVisibility.PRIVATE
            )
            notifyResult(res, "Mission saved to NILORA Memory (Private by default).")
        }
    }

    // --- ACTIONS: GLOBAL CHALLENGES ---

    fun createChallenge(
        scope: ChallengeScope,
        category: String,
        title: String,
        description: String,
        targetGoal: Int,
        cityName: String,
        countryName: String,
        onCreated: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            val p = _userProfile.value
            val hub = GlobalCityHubCatalog.findHub(cityName)
            val res = realityRepo.createChallenge(
                creatorName = p?.displayName ?: "Challenger",
                creatorCountry = p?.country ?: countryName,
                scope = scope,
                category = category,
                title = title,
                description = description,
                targetGoal = targetGoal,
                cityName = cityName,
                countryName = countryName,
                latitude = hub?.latitude,
                longitude = hub?.longitude
            )
            res.onSuccess { id ->
                _actionStatus.value = "Global Challenge created." to false
                onCreated(id)
            }.onFailure { err ->
                _actionStatus.value = (err.localizedMessage ?: "Failed to create challenge") to true
            }
        }
    }

    fun submitChallengeEntry(
        challenge: GlobalChallenge,
        teamName: String,
        summaryText: String,
        pointsClaimed: Int
    ) {
        viewModelScope.launch {
            val p = _userProfile.value
            val res = realityRepo.submitChallengeEntry(
                challenge = challenge,
                authorName = p?.displayName ?: "Member",
                countryName = p?.country?.ifBlank { challenge.countryName } ?: challenge.countryName,
                teamName = teamName,
                summaryText = summaryText,
                pointsClaimed = pointsClaimed
            )
            notifyResult(res, "Challenge entry submitted (USER_SUBMITTED provenance).")
        }
    }

    // --- ACTIONS: REALITY BRIDGE ---

    fun saveRealityBridgeItem(
        adventure: AdventureExperience,
        targetType: String,
        targetId: String,
        targetTitle: String,
        stage: RealityBridgeStage = RealityBridgeStage.PLANNED,
        notes: String = ""
    ) {
        viewModelScope.launch {
            val res = realityRepo.upsertRealityBridge(
                virtualAdventureId = adventure.id,
                virtualAdventureTitle = adventure.title,
                destinationName = adventure.destinationName,
                targetType = targetType,
                targetId = targetId,
                targetTitle = targetTitle,
                stage = stage,
                notes = notes
            )
            notifyResult(res, "Added to your Reality Bridge plan (${stage.labelEn}).")
        }
    }

    fun advanceBridgeStage(link: RealityBridgeLink, newStage: RealityBridgeStage) {
        viewModelScope.launch {
            val res = realityRepo.updateBridgeStage(link.id, newStage)
            notifyResult(res, "Bridge stage updated to ${newStage.labelEn}.")
            if (res.isSuccess && newStage == RealityBridgeStage.COMPLETED) {
                experienceRepo.saveNiloraMemory(
                    myProfile = _userProfile.value,
                    memoryType = MemoryType.WORLD_QUEST,
                    sourceId = link.targetId,
                    title = "${link.virtualAdventureTitle} -> ${link.targetTitle}",
                    summaryText = "Bridged from virtual exploration in ${link.destinationName} to completed real-world experience. ${link.notes}".trim(),
                    originalLanguage = _userProfile.value?.preferredLanguage ?: "en",
                    translations = emptyMap(),
                    countries = listOf("Egypt"),
                    partnerIds = emptyList(),
                    partnerNames = emptyList(),
                    placeLabel = link.destinationName,
                    photoBase64 = "",
                    audioBase64 = "",
                    audioDurationMs = 0L,
                    retainAudioConsent = false,
                    visibility = MemoryVisibility.PRIVATE
                )
            }
        }
    }

    fun removeBridgeItem(bridgeId: String) {
        viewModelScope.launch {
            notifyResult(realityRepo.deleteBridgeItem(bridgeId), "Removed from Reality Bridge.")
        }
    }

    // --- SAFETY: REPORT & BLOCK ---

    fun reportRealityContent(targetType: String, targetId: String, reason: String, details: String = "") {
        viewModelScope.launch {
            notifyResult(firestoreRepo.submitReport(targetType, targetId, reason, details), "Report submitted for admin review.")
        }
    }

    fun blockParticipant(userIdToBlock: String, displayName: String) {
        viewModelScope.launch {
            notifyResult(communityRepo.blockUser(userIdToBlock, displayName), "User blocked.")
        }
    }
}
