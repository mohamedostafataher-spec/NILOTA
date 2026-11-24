package com.example.ui.screens

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.*
import com.example.ui.components.CommunicationHubTabs
import com.example.ui.components.NiloraAnimatedStatusPulse
import com.example.ui.components.NiloraConversationPreviewCard
import com.example.ui.components.NiloraFooter
import com.example.ui.components.NiloraSearchFilterBar
import com.example.ui.components.NiloraTopBar
import com.example.ui.components.NiloraVoiceMessagePlayer
import com.example.ui.components.UnavailableDataCard
import com.example.ui.theme.*
import com.example.ui.viewmodel.DirectChatUiState
import java.text.DateFormat
import java.util.Date

@Composable
fun GlobalMessagesInboxScreen(
    currentUserId: String,
    conversationsState: UiState<List<DirectConversation>>,
    blockedState: UiState<List<BlockedUserRecord>>,
    requestsState: UiState<List<ConnectionRequest>>,
    onOpenConversation: (String) -> Unit,
    onOpenDiscover: () -> Unit,
    onOpenRequests: () -> Unit,
    onOpenVoiceTranslator: () -> Unit,
    onOpenGroupsHub: () -> Unit = {}
) {
    val blockedIds = remember(blockedState) {
        (blockedState as? UiState.Success)?.data?.map { it.blockedUserId }?.toSet().orEmpty()
    }
    val pendingCount = remember(requestsState) {
        (requestsState as? UiState.Success)?.data?.count { it.receiverId == currentUserId && it.status == "PENDING" } ?: 0
    }
    var search by remember { mutableStateOf("") }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("messages_inbox_list"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Global Messages", style = MaterialTheme.typography.headlineLarge)
                Text(
                    text = "1:1 multilingual conversations preserving every original message alongside translations.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                CommunicationHubTabs(
                    activeTab = "MESSAGES",
                    pendingRequestsCount = pendingCount,
                    onSelectMessages = {},
                    onSelectDiscover = onOpenDiscover,
                    onSelectGroups = onOpenGroupsHub
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    FilledTonalButton(
                        onClick = onOpenDiscover,
                        modifier = Modifier.weight(1f).testTag("messages_find_people_btn")
                    ) {
                        Icon(Icons.Filled.PersonSearch, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Find People", maxLines = 1)
                    }
                    OutlinedButton(
                        onClick = onOpenRequests,
                        modifier = Modifier.weight(1f).testTag("messages_requests_btn")
                    ) {
                        Text(if (pendingCount > 0) "Requests ($pendingCount)" else "Requests", maxLines = 1)
                    }
                    OutlinedButton(
                        onClick = onOpenVoiceTranslator,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Filled.Translate, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Live", maxLines = 1)
                    }
                }
            }
        }

        item {
            NiloraSearchFilterBar(
                query = search,
                placeholder = "Filter conversations by name",
                onQueryChange = { search = it },
                testTag = "messages_search_input"
            )
        }

        item {
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Transport security: Messages are stored in Firebase Cloud Firestore under TLS with rule-enforced participant & block isolation. End-to-end encryption (E2EE) is not implemented.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(12.dp)
                )
            }
        }

        when (conversationsState) {
            is UiState.Loading -> {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
            }
            is UiState.Error -> {
                item { UnavailableDataCard("Messages Unavailable", conversationsState.message) }
            }
            is UiState.Success -> {
                val visible = conversationsState.data.filter { conv ->
                    val partnerId = conv.partnerId(currentUserId)
                    partnerId !in blockedIds &&
                        (search.isBlank() || conv.partnerName(currentUserId).contains(search, ignoreCase = true))
                }
                if (visible.isEmpty()) {
                    item {
                        UnavailableDataCard(
                            title = "No Active Direct Conversations",
                            explanation = "Discover people in the Global Community tab or accept a connection request to start a 1:1 multilingual conversation.",
                            icon = Icons.Filled.ChatBubbleOutline,
                            actionLabel = "Browse Community",
                            onActionClick = onOpenDiscover
                        )
                    }
                } else {
                    items(visible, key = { it.id }) { conv ->
                        NiloraConversationPreviewCard(
                            conversation = conv,
                            currentUserId = currentUserId,
                            onClick = { onOpenConversation(conv.id) }
                        )
                    }
                }
            }
        }

        item { NiloraFooter() }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DirectChatScreen(
    currentUserId: String,
    state: DirectChatUiState,
    hasMicPermission: Boolean,
    onSelectSourceLanguage: (String) -> Unit,
    onSelectTargetTranslationLanguage: (String) -> Unit,
    onSendText: (String, Boolean) -> Unit,
    onTranslateMessage: (DirectMessage, String) -> Unit,
    onStartVoiceRecording: () -> Unit,
    onStopVoiceRecording: () -> Unit,
    onDiscardVoiceRecording: () -> Unit,
    onSendVoiceRecording: (String, Boolean) -> Unit,
    onDictateMessage: () -> Unit,
    onTogglePlayVoiceClip: (DirectMessage) -> Unit,
    onSpeakText: (String, String) -> Unit,
    onBlockPartner: (String, String) -> Unit,
    onReportPartner: (String, String, String) -> Unit,
    onBack: () -> Unit
) {
    var draft by remember { mutableStateOf("") }
    var voiceCaption by remember { mutableStateOf("") }
    var autoTranslateOutgoing by remember { mutableStateOf(true) }
    var showTranslations by remember { mutableStateOf(true) }
    var showSourceMenu by remember { mutableStateOf(false) }
    var showTargetMenu by remember { mutableStateOf(false) }
    var showTopMenu by remember { mutableStateOf(false) }
    var showVoiceConsentDialog by remember { mutableStateOf(false) }
    var pendingMicAction by remember { mutableStateOf<String?>(null) }
    var consentGiven by remember { mutableStateOf(false) }
    var showReportDialog by remember { mutableStateOf(false) }
    var reportReason by remember { mutableStateOf("Abusive or spam messages") }

    LaunchedEffect(state.pendingVoiceSttTranscript) {
        if (state.pendingVoiceSttTranscript.isNotBlank()) {
            draft = state.pendingVoiceSttTranscript
        }
    }

    val micPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            if (pendingMicAction == "DICTATE") onDictateMessage() else onStartVoiceRecording()
        }
        pendingMicAction = null
    }

    fun triggerMicAction(action: String) {
        if (!consentGiven) {
            pendingMicAction = action
            showVoiceConsentDialog = true
            return
        }
        if (!hasMicPermission) {
            pendingMicAction = action
            micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            return
        }
        if (action == "DICTATE") onDictateMessage() else onStartVoiceRecording()
    }

    val partnerName = state.conversation?.partnerName(currentUserId) ?: "Direct Conversation"
    val partnerId = state.conversation?.partnerId(currentUserId).orEmpty()
    val partnerLang = state.conversation?.partnerLanguage(currentUserId).orEmpty()

    Scaffold(
        topBar = {
            NiloraTopBar(
                title = partnerName,
                subtitle = if (partnerLang.isNotBlank()) "Native: $partnerLang • Original preserved" else "Original messages preserved",
                onBackClick = onBack,
                actions = {
                    IconButton(onClick = { showTopMenu = true }) {
                        Icon(Icons.Filled.MoreVert, contentDescription = "Chat options")
                    }
                    DropdownMenu(expanded = showTopMenu, onDismissRequest = { showTopMenu = false }) {
                        if (partnerId.isNotBlank()) {
                            DropdownMenuItem(
                                text = { Text("Block $partnerName") },
                                leadingIcon = { Icon(Icons.Filled.Block, contentDescription = null) },
                                onClick = {
                                    showTopMenu = false
                                    onBlockPartner(partnerId, partnerName)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Report Conversation") },
                                leadingIcon = { Icon(Icons.Filled.Flag, contentDescription = null) },
                                onClick = {
                                    showTopMenu = false
                                    showReportDialog = true
                                }
                            )
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Language selector bar
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(modifier = Modifier.weight(1f)) {
                            OutlinedButton(
                                onClick = { showSourceMenu = true },
                                modifier = Modifier.fillMaxWidth().testTag("direct_source_lang_btn")
                            ) {
                                Text("From: ${state.sourceInputLanguageCode}", maxLines = 1)
                            }
                            DropdownMenu(expanded = showSourceMenu, onDismissRequest = { showSourceMenu = false }) {
                                NiloraTaxonomy.worldLanguages.forEach { lang ->
                                    DropdownMenuItem(
                                        text = { Text("${lang.nativeName} (${lang.code})") },
                                        onClick = {
                                            onSelectSourceLanguage(lang.code)
                                            showSourceMenu = false
                                        }
                                    )
                                }
                            }
                        }
                        Box(modifier = Modifier.weight(1f)) {
                            FilledTonalButton(
                                onClick = { showTargetMenu = true },
                                modifier = Modifier.fillMaxWidth().testTag("direct_target_lang_btn")
                            ) {
                                Text("Translate: ${state.targetTranslationCode}", maxLines = 1)
                            }
                            DropdownMenu(expanded = showTargetMenu, onDismissRequest = { showTargetMenu = false }) {
                                NiloraTaxonomy.worldLanguages.forEach { lang ->
                                    DropdownMenuItem(
                                        text = { Text("${lang.nativeName} (${lang.code})") },
                                        onClick = {
                                            onSelectTargetTranslationLanguage(lang.code)
                                            showTargetMenu = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = autoTranslateOutgoing, onCheckedChange = { autoTranslateOutgoing = it })
                            Text("Auto-translate send", style = MaterialTheme.typography.labelMedium)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = showTranslations, onCheckedChange = { showTranslations = it })
                            Text("Show translations", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }

            state.errorBanner?.let { err ->
                Surface(color = ErrorCoralContainer, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = err,
                        color = ErrorCoral,
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                    )
                }
            }
            state.statusNote?.let { note ->
                Surface(color = TurquoiseContainer, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = note,
                        color = TurquoiseOnContainer,
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                    )
                }
            }

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .testTag("direct_messages_list"),
                contentPadding = PaddingValues(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (state.messages.isEmpty()) {
                    item {
                        UnavailableDataCard(
                            title = "Conversation Ready",
                            explanation = "Write a message or record a voice clip. Original text is always preserved alongside optional NILORA LIVE translation.",
                            icon = Icons.Filled.Forum
                        )
                    }
                } else {
                    items(state.messages, key = { it.id }) { msg ->
                        DirectMessageBubble(
                            msg = msg,
                            isMine = msg.senderId == currentUserId,
                            targetLanguageCode = state.targetTranslationCode,
                            showTranslation = showTranslations,
                            isPlayingAudio = state.playingMessageId == msg.id,
                            onTranslateNow = { onTranslateMessage(msg, state.targetTranslationCode) },
                            onToggleVoicePlay = { onTogglePlayVoiceClip(msg) },
                            onSpeakText = { txt, code -> onSpeakText(txt, code) },
                            onReportMessage = {
                                onReportPartner("${msg.conversationId}/${msg.id}", "Inappropriate message", msg.originalText)
                            }
                        )
                    }
                }
            }

            // Recorded Voice Clip Preview Bar
            state.recordedClip?.let { clip ->
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Voice note recorded (${"%.1f".format(clip.durationMs / 1000.0)}s AAC)",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        OutlinedTextField(
                            value = voiceCaption,
                            onValueChange = { voiceCaption = it },
                            label = { Text("Optional transcript or note") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            OutlinedButton(onClick = onDiscardVoiceRecording, modifier = Modifier.weight(1f)) {
                                Text("Discard")
                            }
                            Button(
                                onClick = {
                                    onSendVoiceRecording(voiceCaption, autoTranslateOutgoing)
                                    voiceCaption = ""
                                },
                                enabled = !state.isSending,
                                modifier = Modifier.weight(1f).testTag("send_voice_note_btn")
                            ) {
                                Text("Send Voice Note")
                            }
                        }
                    }
                }
            }

            // Message Composer
            Surface(
                tonalElevation = 3.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    IconButton(
                        onClick = {
                            if (state.isRecordingVoice) onStopVoiceRecording() else triggerMicAction("VOICE")
                        },
                        modifier = Modifier.testTag("direct_mic_record_btn")
                    ) {
                        Box(contentAlignment = Alignment.TopEnd) {
                            Icon(
                                imageVector = if (state.isRecordingVoice) Icons.Filled.StopCircle else Icons.Filled.Mic,
                                contentDescription = if (state.isRecordingVoice) "Stop recording" else "Record voice message",
                                tint = if (state.isRecordingVoice) ErrorCoral else MaterialTheme.colorScheme.primary
                            )
                            if (state.isRecordingVoice) {
                                NiloraAnimatedStatusPulse(isActive = true, sizeDp = 8)
                            }
                        }
                    }

                    IconButton(
                        onClick = { triggerMicAction("DICTATE") },
                        enabled = !state.isTranscribingVoice && !state.isRecordingVoice,
                        modifier = Modifier.testTag("direct_stt_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.RecordVoiceOver,
                            contentDescription = "Dictate speech to text"
                        )
                    }

                    OutlinedTextField(
                        value = draft,
                        onValueChange = { draft = it },
                        placeholder = { Text("Message ($partnerName)...") },
                        maxLines = 3,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("direct_message_input")
                    )

                    IconButton(
                        onClick = {
                            val txt = draft.trim()
                            if (txt.isNotEmpty()) {
                                onSendText(txt, autoTranslateOutgoing)
                                draft = ""
                            }
                        },
                        enabled = draft.isNotBlank() && !state.isSending,
                        modifier = Modifier.testTag("direct_send_btn")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send message")
                    }
                }
            }
        }
    }

    if (showVoiceConsentDialog) {
        AlertDialog(
            onDismissRequest = {
                showVoiceConsentDialog = false
                pendingMicAction = null
            },
            title = { Text("Microphone & Voice Disclosure") },
            text = {
                Text(
                    "NILORA accesses your microphone only when you press record or dictate. " +
                        "Speech-to-text uses your device's configured speech service; voice notes record AAC audio " +
                        "strictly for sending to this conversation."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        consentGiven = true
                        showVoiceConsentDialog = false
                        val act = pendingMicAction ?: "VOICE"
                        if (!hasMicPermission) {
                            micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        } else {
                            pendingMicAction = null
                            if (act == "DICTATE") onDictateMessage() else onStartVoiceRecording()
                        }
                    },
                    modifier = Modifier.testTag("consent_mic_accept_btn")
                ) { Text("I Consent & Continue") }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showVoiceConsentDialog = false
                        pendingMicAction = null
                    }
                ) { Text("Cancel") }
            }
        )
    }

    if (showReportDialog) {
        AlertDialog(
            onDismissRequest = { showReportDialog = false },
            title = { Text("Report Conversation") },
            text = {
                OutlinedTextField(
                    value = reportReason,
                    onValueChange = { reportReason = it },
                    label = { Text("Reason") },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onReportPartner(state.conversation?.id.orEmpty(), reportReason, "")
                        showReportDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ErrorCoral)
                ) { Text("Report") }
            },
            dismissButton = { TextButton(onClick = { showReportDialog = false }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun DirectMessageBubble(
    msg: DirectMessage,
    isMine: Boolean,
    targetLanguageCode: String,
    showTranslation: Boolean,
    isPlayingAudio: Boolean,
    onTranslateNow: () -> Unit,
    onToggleVoicePlay: () -> Unit,
    onSpeakText: (String, String) -> Unit,
    onReportMessage: () -> Unit
) {
    val containerColor = if (isMine) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
    val timeText = msg.createdAt?.toDate()?.let {
        DateFormat.getTimeInstance(DateFormat.SHORT).format(it)
    } ?: "Sending"

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isMine) Arrangement.End else Arrangement.Start
    ) {
        Card(
            modifier = Modifier
                .widthIn(max = 320.dp)
                .testTag("direct_msg_${msg.id}"),
            colors = CardDefaults.cardColors(containerColor = containerColor),
            border = if (!isMine) BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)) else null,
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${msg.senderName} • ${msg.sourceLanguage}",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    if (!isMine) {
                        IconButton(onClick = onReportMessage, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Filled.Flag, contentDescription = "Report message", modifier = Modifier.size(14.dp))
                        }
                    }
                }

                if (msg.messageType == "VOICE_NOTE") {
                    NiloraVoiceMessagePlayer(
                        durationMs = msg.audioDurationMs,
                        isPlaying = isPlayingAudio,
                        onTogglePlay = onToggleVoicePlay,
                        testTag = "play_voice_${msg.id}"
                    )
                }

                // Original message is ALWAYS preserved verbatim
                Text(
                    text = msg.originalText,
                    style = MaterialTheme.typography.bodyLarge
                )

                val translated = msg.translations[targetLanguageCode]
                if (showTranslation && !translated.isNullOrBlank()) {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 2.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Translation ($targetLanguageCode):",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        IconButton(
                            onClick = { onSpeakText(translated, targetLanguageCode) },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.VolumeUp,
                                contentDescription = "Speak translation",
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                    Text(
                        text = translated,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (msg.translationNote.isNotBlank()) {
                        Text(
                            text = msg.translationNote,
                            style = MaterialTheme.typography.labelSmall,
                            color = WarningAmber
                        )
                    }
                } else if (showTranslation && !msg.sourceLanguage.equals(targetLanguageCode, ignoreCase = true)) {
                    TextButton(
                        onClick = onTranslateNow,
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                        modifier = Modifier.testTag("translate_msg_btn_${msg.id}")
                    ) {
                        Icon(Icons.Filled.Translate, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Translate to $targetLanguageCode", style = MaterialTheme.typography.labelSmall)
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = buildString {
                            append(timeText)
                            if (isMine) append(" • ${msg.deliveryStatus}")
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
