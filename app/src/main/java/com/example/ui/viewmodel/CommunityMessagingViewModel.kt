package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.*
import com.example.data.repository.CommunityAndMessagingRepository
import com.example.data.repository.FirestoreDataRepository
import com.example.data.repository.LocationAndPlacesRepository
import com.example.niloralive.*
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class CommunityDiscoveryFilters(
    val searchQuery: String = "",
    val languageFilter: String = "ALL",
    val countryFilter: String = "ALL",
    val interestFilter: String = "ALL",
    val languageExchangeOnly: Boolean = false
)

data class DirectChatUiState(
    val conversation: DirectConversation? = null,
    val messages: List<DirectMessage> = emptyList(),
    val targetTranslationCode: String = "ar-EG",
    val sourceInputLanguageCode: String = "en",
    val isTranslating: Boolean = false,
    val isSending: Boolean = false,
    val isRecordingVoice: Boolean = false,
    val isTranscribingVoice: Boolean = false,
    val recordedClip: RecordedVoiceClip? = null,
    val playingMessageId: String? = null,
    val pendingVoiceSttTranscript: String = "",
    val statusNote: String? = null,
    val errorBanner: String? = null
)

data class GroupRoomUiState(
    val group: GroupRoom? = null,
    val messages: List<GroupMessage> = emptyList(),
    val joinRequests: List<GroupJoinRequest> = emptyList(),
    val targetTranslationCode: String = "ar-EG",
    val sourceInputLanguageCode: String = "en",
    val subtitleMode: SubtitleDisplayMode = SubtitleDisplayMode.BOTH,
    val autoSpeakIncoming: Boolean = false,
    val preferOriginalAudio: Boolean = false,
    val isTranscribingVoice: Boolean = false,
    val pendingVoiceSttTranscript: String = "",
    val isTranslating: Boolean = false,
    val isSending: Boolean = false,
    val playingMessageId: String? = null,
    val errorBanner: String? = null
)

/**
 * Coordinates NILORA Phase 2:
 * - Global community member discovery, filters, connections, blocking, reporting
 * - 1:1 Direct Messaging with NILORA LIVE translation preserving originalText intact
 * - Real AAC voice message recording/playback & optional SpeechRecognizer transcription
 * - Multilingual group conversations with join policies & owner/moderator tools
 */
class CommunityMessagingViewModel(
    private val communityRepo: CommunityAndMessagingRepository,
    private val firestoreRepo: FirestoreDataRepository,
    private val locationRepo: LocationAndPlacesRepository,
    private val translationEngine: EgyptianTranslationEngine,
    private val sttEngine: SpeechRecognitionEngine,
    private val ttsEngine: VoiceGenerationEngine,
    private val voiceRecorder: VoiceMessageAudioRecorder,
    val currentUserId: String
) : ViewModel() {

    companion object {
        private const val TAG = "CommunityMsgVM"
        private const val STOP_TIMEOUT_MS = 5000L
    }

    private val _filters = MutableStateFlow(CommunityDiscoveryFilters())
    val filters: StateFlow<CommunityDiscoveryFilters> = _filters.asStateFlow()

    private val _actionStatus = MutableStateFlow<Pair<String?, Boolean>>(null to false)
    val actionStatus: StateFlow<Pair<String?, Boolean>> = _actionStatus.asStateFlow()

    private val _directChat = MutableStateFlow(DirectChatUiState())
    val directChat: StateFlow<DirectChatUiState> = _directChat.asStateFlow()

    private val _groupRoom = MutableStateFlow(GroupRoomUiState())
    val groupRoom: StateFlow<GroupRoomUiState> = _groupRoom.asStateFlow()

    private val _userProfile = MutableStateFlow<UserProfile?>(null)
    val myProfile: StateFlow<UserProfile?> = _userProfile.asStateFlow()

    private var directChatJob: Job? = null
    private var directConvJob: Job? = null
    private var groupJob: Job? = null
    private var groupMsgJob: Job? = null
    private var groupReqJob: Job? = null

    init {
        viewModelScope.launch {
            firestoreRepo.observeUserProfile(currentUserId)
                .catch { Log.w(TAG, "Profile observe error", it) }
                .collect { prof ->
                    _userProfile.value = prof
                    if (prof != null) {
                        val pref = prof.nativeLanguage.ifBlank { prof.preferredLanguage }
                        _directChat.update {
                            it.copy(
                                sourceInputLanguageCode = if (it.conversation == null) pref else it.sourceInputLanguageCode,
                                targetTranslationCode = if (it.conversation == null) pref else it.targetTranslationCode
                            )
                        }
                        _groupRoom.update {
                            it.copy(
                                sourceInputLanguageCode = if (it.group == null) pref else it.sourceInputLanguageCode,
                                targetTranslationCode = if (it.group == null) pref else it.targetTranslationCode
                            )
                        }
                    }
                }
        }
    }

    private fun <T> Flow<T>.toUiState(): StateFlow<UiState<T>> =
        map<T, UiState<T>> { UiState.Success(it) }
            .catch { error ->
                val fsErr = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (fsErr == null && error is CancellationException) throw error
                emit(UiState.Error((fsErr ?: error).localizedMessage ?: "Unable to load community data"))
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), UiState.Loading)

    val publicProfilesState: StateFlow<UiState<List<PublicProfile>>> =
        communityRepo.observePublicProfiles().toUiState()

    val blockedUsersState: StateFlow<UiState<List<BlockedUserRecord>>> =
        communityRepo.observeBlockedUsers().toUiState()

    val connectionRequestsState: StateFlow<UiState<List<ConnectionRequest>>> =
        communityRepo.observeConnectionRequests().toUiState()

    val directConversationsState: StateFlow<UiState<List<DirectConversation>>> =
        communityRepo.observeMyDirectConversations().toUiState()

    val discoverableGroupsState: StateFlow<UiState<List<GroupRoom>>> =
        communityRepo.observeDiscoverableGroups().toUiState()

    val myGroupsState: StateFlow<UiState<List<GroupRoom>>> =
        communityRepo.observeMyGroups().toUiState()

    fun isOnline(): Boolean = locationRepo.isNetworkOnline()

    fun hasMicPermission(): Boolean = voiceRecorder.hasRecordPermission()

    fun clearStatus() {
        _actionStatus.value = null to false
        _directChat.update { it.copy(errorBanner = null, statusNote = null) }
        _groupRoom.update { it.copy(errorBanner = null) }
    }

    fun applyLanguageIdentityDefaults(
        spokenCode: String,
        targetCode: String,
        subtitleMode: SubtitleDisplayMode,
        preferOriginalAudio: Boolean
    ) {
        _directChat.update { current ->
            if (current.conversation == null) {
                current.copy(
                    sourceInputLanguageCode = spokenCode,
                    targetTranslationCode = targetCode
                )
            } else current
        }
        _groupRoom.update { current ->
            current.copy(
                sourceInputLanguageCode = if (current.group == null) spokenCode else current.sourceInputLanguageCode,
                targetTranslationCode = if (current.group == null) targetCode else current.targetTranslationCode,
                subtitleMode = subtitleMode,
                preferOriginalAudio = preferOriginalAudio
            )
        }
    }

    private fun emitMessage(msg: String, isError: Boolean = false) {
        _actionStatus.value = msg to isError
    }

    // --- FILTERS ---
    fun updateSearchQuery(q: String) = _filters.update { it.copy(searchQuery = q) }
    fun updateLanguageFilter(code: String) = _filters.update { it.copy(languageFilter = code) }
    fun updateCountryFilter(country: String) = _filters.update { it.copy(countryFilter = country) }
    fun updateInterestFilter(interest: String) = _filters.update { it.copy(interestFilter = interest) }
    fun toggleLanguageExchangeFilter(enabled: Boolean) = _filters.update { it.copy(languageExchangeOnly = enabled) }

    // --- CONNECTIONS, BLOCK, REPORT ---
    fun sendConnectionRequest(target: PublicProfile, introMessage: String) {
        viewModelScope.launch {
            val res = communityRepo.sendConnectionRequest(target, _userProfile.value, introMessage)
            res.onSuccess { emitMessage("Connection request sent to ${target.displayName}.") }
                .onFailure { emitMessage(it.localizedMessage ?: "Failed to send request.", isError = true) }
        }
    }

    fun respondToConnectionRequest(request: ConnectionRequest, newStatus: String, onOpenChat: (String) -> Unit = {}) {
        viewModelScope.launch {
            val res = communityRepo.respondToConnectionRequest(request, newStatus, _userProfile.value)
            res.onSuccess { convId ->
                emitMessage(
                    when (newStatus) {
                        "ACCEPTED" -> "Connected! Direct chat ready."
                        "DECLINED" -> "Connection request declined."
                        else -> "Connection request cancelled."
                    }
                )
                if (newStatus == "ACCEPTED" && !convId.isNullOrBlank()) {
                    onOpenChat(convId)
                }
            }.onFailure {
                emitMessage(it.localizedMessage ?: "Unable to update request.", isError = true)
            }
        }
    }

    fun startDirectChatWith(partner: PublicProfile, onOpenChat: (String) -> Unit) {
        viewModelScope.launch {
            val res = communityRepo.openOrCreateDirectConversation(partner, _userProfile.value)
            res.onSuccess { convId -> onOpenChat(convId) }
                .onFailure { emitMessage(it.localizedMessage ?: "Unable to open conversation.", isError = true) }
        }
    }

    fun blockUser(targetUid: String, targetName: String, reason: String = "User blocked") {
        viewModelScope.launch {
            val res = communityRepo.blockUser(targetUid, targetName, reason)
            res.onSuccess { emitMessage("$targetName has been blocked.") }
                .onFailure { emitMessage(it.localizedMessage ?: "Failed to block user.", isError = true) }
        }
    }

    fun unblockUser(targetUid: String) {
        viewModelScope.launch {
            val res = communityRepo.unblockUser(targetUid)
            res.onSuccess { emitMessage("User unblocked.") }
                .onFailure { emitMessage(it.localizedMessage ?: "Failed to unblock user.", isError = true) }
        }
    }

    fun reportContent(targetType: String, targetId: String, reason: String, details: String) {
        viewModelScope.launch {
            val res = firestoreRepo.submitReport(targetType, targetId, reason, details)
            res.onSuccess { emitMessage("Report submitted to NILORA safety moderation.") }
                .onFailure { emitMessage(it.localizedMessage ?: "Failed to submit report.", isError = true) }
        }
    }

    // --- 1:1 DIRECT CHAT SESSION ---
    fun openDirectConversation(conversationId: String) {
        directChatJob?.cancel()
        directConvJob?.cancel()
        _directChat.update {
            it.copy(
                messages = emptyList(),
                recordedClip = null,
                pendingVoiceSttTranscript = "",
                errorBanner = null,
                statusNote = null
            )
        }
        directConvJob = viewModelScope.launch {
            communityRepo.observeMyDirectConversations()
                .catch { Log.w(TAG, "Direct conv list error", it) }
                .collect { list ->
                    val match = list.firstOrNull { it.id == conversationId }
                    if (match != null) {
                        val partnerLang = match.partnerLanguage(currentUserId)
                        val myLang = _userProfile.value?.nativeLanguage?.ifBlank { _userProfile.value?.preferredLanguage } ?: "en"
                        _directChat.update { st ->
                            st.copy(
                                conversation = match,
                                sourceInputLanguageCode = if (st.conversation == null) myLang else st.sourceInputLanguageCode,
                                targetTranslationCode = if (st.conversation == null) partnerLang else st.targetTranslationCode
                            )
                        }
                    }
                }
        }
        directChatJob = viewModelScope.launch {
            communityRepo.observeDirectMessages(conversationId)
                .catch { err ->
                    _directChat.update { it.copy(errorBanner = err.localizedMessage ?: "Unable to load messages.") }
                }
                .collect { msgs ->
                    _directChat.update { it.copy(messages = msgs) }
                    // Mark incoming unread messages as READ
                    msgs.filter { it.senderId != currentUserId && it.deliveryStatus != "READ" }
                        .forEach { msg ->
                            viewModelScope.launch {
                                communityRepo.markDirectMessageRead(conversationId, msg.id)
                            }
                        }
                }
        }
    }

    fun setDirectTargetTranslationLanguage(code: String) {
        _directChat.update { it.copy(targetTranslationCode = code) }
    }

    fun setDirectSourceLanguage(code: String) {
        _directChat.update { it.copy(sourceInputLanguageCode = code) }
    }

    fun sendDirectTextMessage(text: String, translateBeforeSend: Boolean) {
        val conv = _directChat.value.conversation ?: run {
            _directChat.update { it.copy(errorBanner = "Conversation is not ready yet.") }
            return
        }
        val clean = text.trim()
        if (clean.isEmpty()) return

        val sourceCode = _directChat.value.sourceInputLanguageCode
        val targetCode = _directChat.value.targetTranslationCode
        val senderName = _userProfile.value?.displayName?.ifBlank { "Member" } ?: "Member"

        viewModelScope.launch {
            _directChat.update { it.copy(isSending = true, errorBanner = null, statusNote = null) }
            val translations = mutableMapOf<String, String>()
            var conf = ""
            var note = ""

            if (translateBeforeSend && !sourceCode.equals(targetCode, ignoreCase = true)) {
                if (!locationRepo.isNetworkOnline()) {
                    note = "Translation unavailable offline — original message preserved."
                } else {
                    val trRes = translationEngine.translateWorldLanguage(
                        sourceText = clean,
                        sourceCode = sourceCode,
                        targetCode = targetCode,
                        conversationContext = _directChat.value.messages.takeLast(3).map { "${it.senderName}: ${it.originalText}" }
                    )
                    trRes.onSuccess { out ->
                        if (out.translatedText.isNotBlank()) {
                            translations[targetCode] = out.translatedText
                            conf = out.confidenceLabel
                            note = listOfNotNull(
                                out.ambiguityWarning.takeIf { it.isNotBlank() },
                                out.validationWarnings.firstOrNull()
                            ).joinToString(" | ")
                        }
                    }.onFailure { err ->
                        note = "Translation unavailable (${err.localizedMessage ?: "service error"}) — original sent."
                    }
                }
            }

            val sendRes = communityRepo.sendDirectMessage(
                conversation = conv,
                senderName = senderName,
                originalText = clean,
                sourceLanguage = sourceCode,
                initialTranslations = translations,
                translationConfidence = conf,
                translationNote = note,
                messageType = "TEXT"
            )
            sendRes.onSuccess {
                _directChat.update {
                    it.copy(isSending = false, statusNote = note.takeIf { n -> n.isNotBlank() })
                }
            }.onFailure { err ->
                _directChat.update {
                    it.copy(
                        isSending = false,
                        errorBanner = err.localizedMessage ?: "Message failed to send."
                    )
                }
            }
        }
    }

    fun translateDirectMessageOnDemand(msg: DirectMessage, targetCode: String) {
        if (msg.translations.containsKey(targetCode)) return
        if (!locationRepo.isNetworkOnline()) {
            _directChat.update { it.copy(errorBanner = "Cannot translate: device is offline. Original text preserved.") }
            return
        }
        viewModelScope.launch {
            _directChat.update { it.copy(isTranslating = true, errorBanner = null) }
            val res = translationEngine.translateWorldLanguage(
                sourceText = msg.originalText,
                sourceCode = msg.sourceLanguage,
                targetCode = targetCode
            )
            res.onSuccess { out ->
                if (out.translatedText.isNotBlank()) {
                    val updatedMap = msg.translations + (targetCode to out.translatedText)
                    val note = out.ambiguityWarning.ifBlank { out.validationWarnings.firstOrNull().orEmpty() }
                    communityRepo.cacheDirectMessageTranslation(
                        conversationId = msg.conversationId,
                        messageId = msg.id,
                        mergedTranslations = updatedMap,
                        confidence = out.confidenceLabel,
                        note = note
                    )
                } else {
                    _directChat.update { it.copy(errorBanner = "Translation returned empty output. Original preserved.") }
                }
                _directChat.update { it.copy(isTranslating = false) }
            }.onFailure { err ->
                _directChat.update {
                    it.copy(
                        isTranslating = false,
                        errorBanner = "Translation unavailable: ${err.localizedMessage ?: "Try again later"}"
                    )
                }
            }
        }
    }

    // --- VOICE RECORDING, PLAYBACK & TRANSCRIPTION ---
    fun startVoiceNoteRecording() {
        if (!voiceRecorder.hasRecordPermission()) {
            _directChat.update { it.copy(errorBanner = "Microphone permission not granted.") }
            return
        }
        voiceRecorder.stopPlayback()
        val res = voiceRecorder.startRecording()
        res.onSuccess {
            _directChat.update {
                it.copy(
                    isRecordingVoice = true,
                    recordedClip = null,
                    playingMessageId = null,
                    errorBanner = null,
                    statusNote = "Recording live microphone audio (max 20s AAC)..."
                )
            }
        }.onFailure { err ->
            _directChat.update {
                it.copy(isRecordingVoice = false, errorBanner = err.localizedMessage ?: "Microphone unavailable.")
            }
        }
    }

    fun stopVoiceNoteRecording() {
        val res = voiceRecorder.stopRecording()
        res.onSuccess { clip ->
            _directChat.update {
                it.copy(
                    isRecordingVoice = false,
                    recordedClip = clip,
                    statusNote = "Recorded ${clip.durationMs / 1000.0}s voice clip ready to send."
                )
            }
        }.onFailure { err ->
            _directChat.update {
                it.copy(isRecordingVoice = false, recordedClip = null, errorBanner = err.localizedMessage ?: "Recording failed.")
            }
        }
    }

    fun discardRecordedVoiceClip() {
        voiceRecorder.cancelRecording()
        _directChat.update { it.copy(isRecordingVoice = false, recordedClip = null, pendingVoiceSttTranscript = "") }
    }

    fun sendRecordedVoiceNote(captionOrTranscript: String, translateBeforeSend: Boolean) {
        val conv = _directChat.value.conversation ?: return
        val clip = _directChat.value.recordedClip ?: return
        val sourceCode = _directChat.value.sourceInputLanguageCode
        val targetCode = _directChat.value.targetTranslationCode
        val senderName = _userProfile.value?.displayName?.ifBlank { "Member" } ?: "Member"
        val cleanText = captionOrTranscript.trim().ifBlank {
            "Voice message (${(clip.durationMs / 1000.0).let { "%.1f".format(it) }}s)"
        }

        viewModelScope.launch {
            _directChat.update { it.copy(isSending = true, errorBanner = null) }
            val translations = mutableMapOf<String, String>()
            var note = ""
            if (translateBeforeSend && captionOrTranscript.isNotBlank() && !sourceCode.equals(targetCode, ignoreCase = true)) {
                if (locationRepo.isNetworkOnline()) {
                    translationEngine.translateWorldLanguage(cleanText, sourceCode, targetCode)
                        .onSuccess { out ->
                            if (out.translatedText.isNotBlank()) translations[targetCode] = out.translatedText
                            note = out.ambiguityWarning
                        }
                        .onFailure {
                            note = "Voice audio sent; text translation unavailable."
                        }
                }
            }

            val res = communityRepo.sendDirectMessage(
                conversation = conv,
                senderName = senderName,
                originalText = cleanText,
                sourceLanguage = sourceCode,
                initialTranslations = translations,
                translationNote = note,
                messageType = "VOICE_NOTE",
                audioBase64 = clip.audioBase64,
                audioDurationMs = clip.durationMs,
                audioMimeType = clip.mimeType
            )
            res.onSuccess {
                _directChat.update {
                    it.copy(
                        isSending = false,
                        recordedClip = null,
                        pendingVoiceSttTranscript = "",
                        statusNote = "Voice message sent."
                    )
                }
            }.onFailure { err ->
                _directChat.update { it.copy(isSending = false, errorBanner = err.localizedMessage ?: "Failed to upload voice note.") }
            }
        }
    }

    fun dictateMessageIntoComposer() {
        val langCode = _directChat.value.sourceInputLanguageCode
        val niloraLang = when {
            langCode.startsWith("ar") -> NiloraLanguage.ARABIC
            langCode.startsWith("fr") -> NiloraLanguage.FRENCH
            langCode.startsWith("ru") -> NiloraLanguage.RUSSIAN
            else -> NiloraLanguage.ENGLISH
        }
        if (!sttEngine.isAvailable()) {
            _directChat.update {
                it.copy(errorBanner = "Speech-to-text recognition service is unavailable on this device. You can still type or record an audio voice clip.")
            }
            return
        }
        _directChat.update { it.copy(isTranscribingVoice = true, errorBanner = null, statusNote = "Listening for speech-to-text ($langCode)...") }
        sttEngine.startRecognition(
            language = niloraLang,
            onSpeechDetected = {},
            onPartialResult = { partial ->
                _directChat.update { it.copy(pendingVoiceSttTranscript = partial) }
            },
            onFinalResult = { result ->
                _directChat.update {
                    it.copy(
                        isTranscribingVoice = false,
                        pendingVoiceSttTranscript = result.transcript,
                        statusNote = "Transcribed in ${result.latencyMs} ms."
                    )
                }
            },
            onError = { err ->
                _directChat.update {
                    it.copy(isTranscribingVoice = false, errorBanner = "Transcription unavailable: $err")
                }
            }
        )
    }

    fun toggleVoiceClipPlayback(msg: DirectMessage) {
        if (_directChat.value.playingMessageId == msg.id) {
            voiceRecorder.stopPlayback()
            _directChat.update { it.copy(playingMessageId = null) }
            return
        }
        if (msg.audioBase64.isBlank()) {
            _directChat.update { it.copy(errorBanner = "No audio payload in this message.") }
            return
        }
        _directChat.update { it.copy(playingMessageId = msg.id, errorBanner = null) }
        voiceRecorder.playVoiceClip(
            messageId = msg.id,
            audioBase64 = msg.audioBase64,
            onComplete = { _directChat.update { it.copy(playingMessageId = null) } },
            onError = { err ->
                _directChat.update { it.copy(playingMessageId = null, errorBanner = err) }
            }
        )
    }

    fun speakMessageText(text: String, languageCode: String) {
        val niloraLang = when {
            languageCode.startsWith("ar") -> NiloraLanguage.ARABIC
            languageCode.startsWith("fr") -> NiloraLanguage.FRENCH
            languageCode.startsWith("ru") -> NiloraLanguage.RUSSIAN
            else -> NiloraLanguage.ENGLISH
        }
        ttsEngine.speak(
            text = text,
            language = niloraLang,
            onError = { err ->
                _directChat.update { it.copy(errorBanner = err) }
                _groupRoom.update { it.copy(errorBanner = err) }
            }
        )
    }

    // --- MULTILINGUAL GROUP CONVERSATIONS ---
    fun createGroup(
        name: String,
        description: String,
        primaryLanguages: List<String>,
        joinPolicy: String,
        autoTranslateDefault: Boolean,
        onCreated: (String) -> Unit
    ) {
        viewModelScope.launch {
            val res = communityRepo.createGroup(name, description, primaryLanguages, joinPolicy, autoTranslateDefault)
            res.onSuccess { gid ->
                emitMessage("Group '$name' created.")
                onCreated(gid)
            }.onFailure { err ->
                emitMessage(err.localizedMessage ?: "Failed to create group.", isError = true)
            }
        }
    }

    fun joinOrRequestGroup(group: GroupRoom, introMessage: String = "Hi! I'd love to join this multilingual room.", onEntered: (String) -> Unit = {}) {
        viewModelScope.launch {
            if (currentUserId in group.memberIds) {
                onEntered(group.id)
                return@launch
            }
            if (group.joinPolicy == "OPEN") {
                val res = communityRepo.joinOpenGroup(group)
                res.onSuccess {
                    emitMessage("Joined ${group.name}.")
                    onEntered(group.id)
                }.onFailure { err ->
                    emitMessage(err.localizedMessage ?: "Could not join group.", isError = true)
                }
            } else {
                val res = communityRepo.requestJoinGroup(group, _userProfile.value, introMessage)
                res.onSuccess { emitMessage("Join request sent to ${group.name} moderators.") }
                    .onFailure { err -> emitMessage(err.localizedMessage ?: "Request failed.", isError = true) }
            }
        }
    }

    fun openGroupRoom(groupId: String) {
        groupJob?.cancel()
        groupMsgJob?.cancel()
        groupReqJob?.cancel()
        _groupRoom.update { it.copy(messages = emptyList(), joinRequests = emptyList(), errorBanner = null) }

        groupJob = viewModelScope.launch {
            communityRepo.observeGroup(groupId)
                .catch { err -> _groupRoom.update { it.copy(errorBanner = err.localizedMessage) } }
                .collect { grp ->
                    _groupRoom.update { it.copy(group = grp) }
                    if (grp != null && (grp.ownerId == currentUserId || currentUserId in grp.moderatorIds) && groupReqJob == null) {
                        groupReqJob = viewModelScope.launch {
                            communityRepo.observeGroupJoinRequests(groupId)
                                .catch { Log.w(TAG, "Join req observe error", it) }
                                .collect { reqs -> _groupRoom.update { it.copy(joinRequests = reqs) } }
                        }
                    }
                }
        }

        groupMsgJob = viewModelScope.launch {
            communityRepo.observeGroupMessages(groupId)
                .catch { err ->
                    _groupRoom.update { it.copy(errorBanner = err.localizedMessage ?: "Join the group to view messages.") }
                }
                .collect { msgs ->
                    val prevCount = _groupRoom.value.messages.size
                    _groupRoom.update { it.copy(messages = msgs) }
                    if (msgs.size > prevCount && msgs.isNotEmpty()) {
                        val latest = msgs.last()
                        val targetCode = _groupRoom.value.targetTranslationCode
                        if (latest.senderId != currentUserId &&
                            !latest.sourceLanguage.equals(targetCode, ignoreCase = true) &&
                            !latest.translations.containsKey(targetCode) &&
                            locationRepo.isNetworkOnline()
                        ) {
                            translateGroupMessageOnDemand(latest, targetCode)
                        } else if (latest.senderId != currentUserId && _groupRoom.value.autoSpeakIncoming) {
                            val spokenText = if (_groupRoom.value.preferOriginalAudio) {
                                latest.originalText
                            } else {
                                latest.translations[targetCode] ?: latest.originalText
                            }
                            val spokenLang = if (_groupRoom.value.preferOriginalAudio) {
                                latest.sourceLanguage
                            } else {
                                targetCode
                            }
                            speakMessageText(spokenText, spokenLang)
                        }
                    }
                }
        }
    }

    fun setGroupTargetLanguage(code: String) = _groupRoom.update { it.copy(targetTranslationCode = code) }
    fun setGroupSourceLanguage(code: String) = _groupRoom.update { it.copy(sourceInputLanguageCode = code) }
    fun setGroupSubtitleMode(mode: SubtitleDisplayMode) = _groupRoom.update { it.copy(subtitleMode = mode) }
    fun toggleGroupAutoSpeak(enabled: Boolean) = _groupRoom.update { it.copy(autoSpeakIncoming = enabled) }
    fun clearGroupVoiceDraft() = _groupRoom.update { it.copy(pendingVoiceSttTranscript = "") }

    fun dictateMessageIntoGroup() {
        val langCode = _groupRoom.value.sourceInputLanguageCode
        val niloraLang = when {
            langCode.startsWith("ar", ignoreCase = true) -> NiloraLanguage.ARABIC
            langCode.startsWith("fr", ignoreCase = true) -> NiloraLanguage.FRENCH
            langCode.startsWith("ru", ignoreCase = true) -> NiloraLanguage.RUSSIAN
            else -> NiloraLanguage.ENGLISH
        }
        if (!sttEngine.isAvailable()) {
            _groupRoom.update {
                it.copy(errorBanner = "Speech-to-text recognition service is unavailable on this device. You can still type messages directly.")
            }
            return
        }
        _groupRoom.update { it.copy(isTranscribingVoice = true, errorBanner = null) }
        sttEngine.startRecognition(
            language = niloraLang,
            onSpeechDetected = {},
            onPartialResult = { partial ->
                _groupRoom.update { it.copy(pendingVoiceSttTranscript = partial) }
            },
            onFinalResult = { result ->
                _groupRoom.update {
                    it.copy(
                        isTranscribingVoice = false,
                        pendingVoiceSttTranscript = result.transcript
                    )
                }
            },
            onError = { err ->
                _groupRoom.update {
                    it.copy(isTranscribingVoice = false, errorBanner = "Transcription unavailable: $err")
                }
            }
        )
    }

    fun sendGroupMessage(text: String, autoTranslate: Boolean) {
        val group = _groupRoom.value.group ?: return
        val clean = text.trim()
        if (clean.isEmpty()) return
        val sourceCode = _groupRoom.value.sourceInputLanguageCode
        val targetCode = _groupRoom.value.targetTranslationCode
        val senderName = _userProfile.value?.displayName?.ifBlank { "Member" } ?: "Member"

        viewModelScope.launch {
            _groupRoom.update { it.copy(isSending = true, errorBanner = null) }
            val translations = mutableMapOf<String, String>()
            var note = ""
            if (autoTranslate && !sourceCode.equals(targetCode, ignoreCase = true)) {
                if (locationRepo.isNetworkOnline()) {
                    translationEngine.translateWorldLanguage(
                        sourceText = clean,
                        sourceCode = sourceCode,
                        targetCode = targetCode,
                        conversationContext = _groupRoom.value.messages.takeLast(3).map { "${it.senderName}: ${it.originalText}" },
                        domain = TranslationContextDomain.GROUP_ACTIVITIES
                    ).onSuccess { out ->
                        if (out.translatedText.isNotBlank()) translations[targetCode] = out.translatedText
                        note = out.ambiguityWarning
                    }.onFailure { err ->
                        note = "Translation unavailable (${err.localizedMessage ?: "error"}) — original preserved."
                    }
                } else {
                    note = "Offline — original text preserved."
                }
            }
            val res = communityRepo.sendGroupMessage(
                groupId = group.id,
                senderName = senderName,
                originalText = clean,
                sourceLanguage = sourceCode,
                initialTranslations = translations,
                translationNote = note
            )
            res.onSuccess { _groupRoom.update { it.copy(isSending = false, pendingVoiceSttTranscript = "") } }
                .onFailure { err ->
                    _groupRoom.update { it.copy(isSending = false, errorBanner = err.localizedMessage ?: "Failed to send message.") }
                }
        }
    }

    fun translateGroupMessageOnDemand(msg: GroupMessage, targetCode: String) {
        if (msg.translations.containsKey(targetCode)) return
        if (!locationRepo.isNetworkOnline()) {
            _groupRoom.update { it.copy(errorBanner = "Offline — cannot translate message.") }
            return
        }
        viewModelScope.launch {
            _groupRoom.update { it.copy(isTranslating = true, errorBanner = null) }
            translationEngine.translateWorldLanguage(msg.originalText, msg.sourceLanguage, targetCode)
                .onSuccess { out ->
                    if (out.translatedText.isNotBlank()) {
                        communityRepo.cacheGroupMessageTranslation(
                            groupId = msg.groupId,
                            messageId = msg.id,
                            mergedTranslations = msg.translations + (targetCode to out.translatedText),
                            note = out.ambiguityWarning
                        )
                    }
                    _groupRoom.update { it.copy(isTranslating = false) }
                }
                .onFailure { err ->
                    _groupRoom.update {
                        it.copy(isTranslating = false, errorBanner = "Translation unavailable: ${err.localizedMessage ?: "Try again"}")
                    }
                }
        }
    }

    fun reviewGroupJoinRequest(req: GroupJoinRequest, approve: Boolean) {
        val group = _groupRoom.value.group ?: return
        viewModelScope.launch {
            communityRepo.reviewGroupJoinRequest(group, req, approve)
                .onSuccess { emitMessage(if (approve) "${req.userName} approved." else "Request declined.") }
                .onFailure { emitMessage(it.localizedMessage ?: "Review failed.", isError = true) }
        }
    }

    fun inviteMemberToCurrentGroup(target: PublicProfile) {
        val group = _groupRoom.value.group ?: return
        viewModelScope.launch {
            communityRepo.inviteUserToGroup(group, target)
                .onSuccess { emitMessage("Invited ${target.displayName} to ${group.name}.") }
                .onFailure { emitMessage(it.localizedMessage ?: "Invite failed.", isError = true) }
        }
    }

    fun promoteGroupModerator(targetUid: String) {
        val group = _groupRoom.value.group ?: return
        viewModelScope.launch {
            communityRepo.promoteModerator(group, targetUid)
                .onSuccess { emitMessage("Member promoted to moderator.") }
                .onFailure { emitMessage(it.localizedMessage ?: "Action failed.", isError = true) }
        }
    }

    fun removeOrBanGroupMember(targetUid: String, ban: Boolean) {
        val group = _groupRoom.value.group ?: return
        viewModelScope.launch {
            communityRepo.removeOrBanMember(group, targetUid, ban)
                .onSuccess { emitMessage(if (ban) "Member banned from group." else "Member removed.") }
                .onFailure { emitMessage(it.localizedMessage ?: "Action failed.", isError = true) }
        }
    }

    fun deleteGroupMessage(messageId: String) {
        val group = _groupRoom.value.group ?: return
        viewModelScope.launch {
            communityRepo.deleteGroupMessage(group.id, messageId)
                .onSuccess { emitMessage("Message deleted.") }
                .onFailure { emitMessage(it.localizedMessage ?: "Failed to delete message.", isError = true) }
        }
    }

    fun leaveCurrentGroup(onLeft: () -> Unit) {
        val group = _groupRoom.value.group ?: return
        viewModelScope.launch {
            communityRepo.leaveGroup(group)
                .onSuccess {
                    emitMessage("You left ${group.name}.")
                    onLeft()
                }
                .onFailure { emitMessage(it.localizedMessage ?: "Could not leave group.", isError = true) }
        }
    }

    override fun onCleared() {
        super.onCleared()
        voiceRecorder.release()
    }
}
