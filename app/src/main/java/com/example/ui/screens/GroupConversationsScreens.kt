package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.data.model.*
import com.example.niloralive.SubtitleDisplayMode
import com.example.ui.components.CommunicationHubTabs
import com.example.ui.components.LiveSubtitleItem
import com.example.ui.components.LiveSubtitlesOverlayCard
import com.example.ui.components.NiloraFooter
import com.example.ui.components.NiloraTopBar
import com.example.ui.components.UnavailableDataCard
import com.example.ui.theme.*
import com.example.ui.viewmodel.GroupRoomUiState
import java.text.DateFormat

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun GroupsHubScreen(
    currentUserId: String,
    myGroupsState: UiState<List<GroupRoom>>,
    discoverGroupsState: UiState<List<GroupRoom>>,
    onCreateGroup: (name: String, desc: String, langs: List<String>, policy: String, autoTranslate: Boolean) -> Unit,
    onEnterGroup: (String) -> Unit,
    onJoinOrRequestGroup: (GroupRoom, String) -> Unit,
    pendingRequestsCount: Int = 0,
    onOpenMessagesHub: () -> Unit = {},
    onOpenDiscoverHub: () -> Unit = {}
) {
    var showCreateDialog by remember { mutableStateOf(false) }
    var groupName by remember { mutableStateOf("") }
    var groupDesc by remember { mutableStateOf("") }
    var selectedPolicy by remember { mutableStateOf("OPEN") }
    var selectedLangs by remember { mutableStateOf(setOf("ar-EG", "en")) }
    var autoTranslate by remember { mutableStateOf(true) }
    var joinRequestTarget by remember { mutableStateOf<GroupRoom?>(null) }
    var joinRequestMessage by remember { mutableStateOf("") }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("groups_hub_list"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Multilingual Groups", style = MaterialTheme.typography.headlineLarge)
                        Text(
                            text = "Practice languages, plan cultural exchanges, and collaborate globally",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    FilledTonalButton(
                        onClick = { showCreateDialog = true },
                        modifier = Modifier.testTag("create_group_button")
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Create")
                    }
                }
                CommunicationHubTabs(
                    activeTab = "GROUPS",
                    pendingRequestsCount = pendingRequestsCount,
                    onSelectMessages = onOpenMessagesHub,
                    onSelectDiscover = onOpenDiscoverHub,
                    onSelectGroups = {}
                )
            }
        }

        item { Text("Your Groups", style = MaterialTheme.typography.titleLarge) }
        when (myGroupsState) {
            is UiState.Loading -> item { LinearProgressIndicator(modifier = Modifier.fillMaxWidth()) }
            is UiState.Error -> item { UnavailableDataCard("Groups Unavailable", myGroupsState.message) }
            is UiState.Success -> {
                if (myGroupsState.data.isEmpty()) {
                    item {
                        Text(
                            text = "You haven't joined any groups yet. Create a room or join an open community group below.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    items(myGroupsState.data, key = { "my_${it.id}" }) { grp ->
                        GroupRoomCard(
                            group = grp,
                            currentUserId = currentUserId,
                            isMember = true,
                            onAction = { onEnterGroup(grp.id) }
                        )
                    }
                }
            }
        }

        item { Text("Discover Open & Approval Groups", style = MaterialTheme.typography.titleLarge) }
        when (discoverGroupsState) {
            is UiState.Loading -> item { LinearProgressIndicator(modifier = Modifier.fillMaxWidth()) }
            is UiState.Error -> item { UnavailableDataCard("Discoverable Groups Unavailable", discoverGroupsState.message) }
            is UiState.Success -> {
                val otherGroups = discoverGroupsState.data.filter { currentUserId !in it.memberIds }
                if (otherGroups.isEmpty()) {
                    item {
                        UnavailableDataCard(
                            title = "No Public Groups Listed",
                            explanation = "Create a new multilingual conversation room to connect members around shared languages or interests.",
                            icon = Icons.Filled.Groups,
                            actionLabel = "Create Group",
                            onActionClick = { showCreateDialog = true }
                        )
                    }
                } else {
                    items(otherGroups, key = { "disc_${it.id}" }) { grp ->
                        GroupRoomCard(
                            group = grp,
                            currentUserId = currentUserId,
                            isMember = false,
                            onAction = {
                                if (grp.joinPolicy == "OPEN") {
                                    onJoinOrRequestGroup(grp, "")
                                } else {
                                    joinRequestTarget = grp
                                    joinRequestMessage = ""
                                }
                            }
                        )
                    }
                }
            }
        }

        item { NiloraFooter() }
    }

    if (showCreateDialog) {
        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = { Text("Create Multilingual Group") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = groupName,
                        onValueChange = { groupName = it },
                        label = { Text("Group Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("create_group_name_input")
                    )
                    OutlinedTextField(
                        value = groupDesc,
                        onValueChange = { groupDesc = it },
                        label = { Text("Purpose / Topic") },
                        minLines = 2,
                        modifier = Modifier.fillMaxWidth().testTag("create_group_desc_input")
                    )
                    Text("Join Policy", style = MaterialTheme.typography.labelLarge)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("OPEN" to "Open", "REQUEST_APPROVAL" to "Approval", "INVITE_ONLY" to "Invite Only").forEach { (key, lbl) ->
                            FilterChip(
                                selected = selectedPolicy == key,
                                onClick = { selectedPolicy = key },
                                label = { Text(lbl) },
                                modifier = Modifier.testTag("group_policy_$key")
                            )
                        }
                    }
                    Text("Primary Languages", style = MaterialTheme.typography.labelLarge)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        NiloraTaxonomy.worldLanguages.take(6).forEach { lang ->
                            val picked = lang.code in selectedLangs
                            FilterChip(
                                selected = picked,
                                onClick = {
                                    selectedLangs = if (picked && selectedLangs.size > 1) selectedLangs - lang.code else selectedLangs + lang.code
                                },
                                label = { Text(lang.code) }
                            )
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = autoTranslate, onCheckedChange = { autoTranslate = it })
                        Text("Default auto-translate in room", style = MaterialTheme.typography.bodySmall)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (groupName.trim().length >= 2) {
                            onCreateGroup(groupName, groupDesc, selectedLangs.toList(), selectedPolicy, autoTranslate)
                            groupName = ""
                            groupDesc = ""
                            showCreateDialog = false
                        }
                    },
                    enabled = groupName.trim().length >= 2,
                    modifier = Modifier.testTag("confirm_create_group_btn")
                ) { Text("Create Group") }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false }) { Text("Cancel") }
            }
        )
    }

    joinRequestTarget?.let { grp ->
        AlertDialog(
            onDismissRequest = { joinRequestTarget = null },
            title = { Text("Request to Join ${grp.name}") },
            text = {
                OutlinedTextField(
                    value = joinRequestMessage,
                    onValueChange = { joinRequestMessage = it },
                    label = { Text("Introduction for moderators (Optional)") },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onJoinOrRequestGroup(grp, joinRequestMessage)
                        joinRequestTarget = null
                    }
                ) { Text("Send Request") }
            },
            dismissButton = {
                TextButton(onClick = { joinRequestTarget = null }) { Text("Cancel") }
            }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun GroupRoomCard(
    group: GroupRoom,
    currentUserId: String,
    isMember: Boolean,
    onAction: () -> Unit
) {
    val policyLabel = when (group.joinPolicy) {
        "OPEN" -> "Open"
        "REQUEST_APPROVAL" -> "Approval Required"
        "INVITE_ONLY" -> "Invite Only"
        else -> group.joinPolicy
    }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("group_card_${group.id}")
            .clickable(enabled = isMember) { onAction() },
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(group.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(
                        text = "${group.memberIds.size} members • $policyLabel" +
                            if (group.ownerId == currentUserId) " • Owner" else if (currentUserId in group.moderatorIds) " • Moderator" else "",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
                FilledTonalButton(
                    onClick = onAction,
                    modifier = Modifier.testTag("group_action_${group.id}")
                ) {
                    Text(
                        when {
                            isMember -> "Open"
                            group.joinPolicy == "OPEN" -> "Join"
                            else -> "Request"
                        }
                    )
                }
            }
            if (group.description.isNotBlank()) {
                Text(group.description, style = MaterialTheme.typography.bodyMedium)
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                group.primaryLanguages.forEach { code ->
                    Surface(
                        color = TurquoiseContainer,
                        shape = RoundedCornerShape(999.dp)
                    ) {
                        Text(
                            text = code,
                            style = MaterialTheme.typography.labelSmall,
                            color = TurquoiseOnContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun GroupRoomScreen(
    currentUserId: String,
    state: GroupRoomUiState,
    publicProfilesState: UiState<List<PublicProfile>>,
    onSelectSourceLanguage: (String) -> Unit,
    onSelectTargetLanguage: (String) -> Unit,
    onSendMessage: (String, Boolean) -> Unit,
    onTranslateMessage: (GroupMessage, String) -> Unit,
    onSpeakText: (String, String) -> Unit,
    onReviewJoinRequest: (GroupJoinRequest, Boolean) -> Unit,
    onInviteMember: (PublicProfile) -> Unit,
    onPromoteModerator: (String) -> Unit,
    onRemoveOrBanMember: (String, Boolean) -> Unit,
    onDeleteMessage: (String) -> Unit,
    onLeaveGroup: () -> Unit,
    onReportGroup: (String, String, String) -> Unit,
    onBack: () -> Unit,
    onSelectSubtitleMode: (SubtitleDisplayMode) -> Unit = {},
    onToggleAutoSpeakIncoming: (Boolean) -> Unit = {},
    onDictateVoice: () -> Unit = {},
    onClearVoiceDraft: () -> Unit = {}
) {
    val context = LocalContext.current
    val group = state.group
    val isOwner = group?.ownerId == currentUserId
    val isModOrOwner = isOwner || (group != null && currentUserId in group.moderatorIds)
    val isMember = group != null && currentUserId in group.memberIds

    var draft by remember { mutableStateOf("") }
    var micDeniedNote by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(state.pendingVoiceSttTranscript) {
        if (state.pendingVoiceSttTranscript.isNotBlank()) {
            draft = state.pendingVoiceSttTranscript
        }
    }
    val micLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            micDeniedNote = null
            onDictateVoice()
        } else {
            micDeniedNote = "Microphone permission denied. Type below to participate in Group Live."
        }
    }
    var autoTranslate by remember(group?.autoTranslateDefault) { mutableStateOf(group?.autoTranslateDefault ?: true) }
    var showSourceMenu by remember { mutableStateOf(false) }
    var showTargetMenu by remember { mutableStateOf(false) }
    var showManagementDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            NiloraTopBar(
                title = group?.name ?: "Group Room",
                subtitle = if (group != null) "${group.memberIds.size} members • Original text preserved" else null,
                onBackClick = onBack,
                actions = {
                    IconButton(
                        onClick = { showManagementDialog = true },
                        modifier = Modifier.testTag("group_manage_button")
                    ) {
                        Icon(Icons.Filled.ManageAccounts, contentDescription = "Participants & Moderation")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        Box(modifier = Modifier.weight(1f)) {
                            OutlinedButton(onClick = { showSourceMenu = true }, modifier = Modifier.fillMaxWidth()) {
                                Text("Speak: ${state.sourceInputLanguageCode}", maxLines = 1)
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
                            FilledTonalButton(onClick = { showTargetMenu = true }, modifier = Modifier.fillMaxWidth()) {
                                Text("Read in: ${state.targetTranslationCode}", maxLines = 1)
                            }
                            DropdownMenu(expanded = showTargetMenu, onDismissRequest = { showTargetMenu = false }) {
                                NiloraTaxonomy.worldLanguages.forEach { lang ->
                                    DropdownMenuItem(
                                        text = { Text("${lang.nativeName} (${lang.code})") },
                                        onClick = {
                                            onSelectTargetLanguage(lang.code)
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
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Checkbox(checked = autoTranslate, onCheckedChange = { autoTranslate = it })
                            Text("Auto-translate to ${state.targetTranslationCode}", style = MaterialTheme.typography.labelMedium)
                        }
                        FilterChip(
                            selected = state.autoSpeakIncoming,
                            onClick = { onToggleAutoSpeakIncoming(!state.autoSpeakIncoming) },
                            leadingIcon = { Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = null, modifier = Modifier.size(16.dp)) },
                            label = { Text(if (state.autoSpeakIncoming) "Voice: ON" else "Voice: OFF") },
                            modifier = Modifier.testTag("group_auto_speak_chip")
                        )
                    }
                }
            }

            val latestGroupMsg = state.messages.lastOrNull()
            val groupLiveSubtitle = when {
                state.isTranscribingVoice && state.pendingVoiceSttTranscript.isNotBlank() -> LiveSubtitleItem(
                    speakerName = "You (Speaking)",
                    sourceLanguageLabel = state.sourceInputLanguageCode,
                    targetLanguageLabel = state.targetTranslationCode,
                    originalText = state.pendingVoiceSttTranscript,
                    translatedText = "",
                    isPartial = true,
                    confidenceLabel = "MEDIUM"
                )
                latestGroupMsg != null -> LiveSubtitleItem(
                    speakerName = latestGroupMsg.senderName,
                    sourceLanguageLabel = latestGroupMsg.sourceLanguage,
                    targetLanguageLabel = state.targetTranslationCode,
                    originalText = latestGroupMsg.originalText,
                    translatedText = latestGroupMsg.translations[state.targetTranslationCode].orEmpty(),
                    isPartial = false,
                    confidenceLabel = if (latestGroupMsg.translationNote.contains("low", ignoreCase = true)) "LOW" else "HIGH",
                    ambiguityWarning = latestGroupMsg.translationNote
                )
                else -> null
            }

            Box(modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)) {
                LiveSubtitlesOverlayCard(
                    subtitle = groupLiveSubtitle,
                    mode = state.subtitleMode,
                    onModeChange = onSelectSubtitleMode,
                    onSpeakTranslation = {
                        latestGroupMsg?.let { msg ->
                            val textToSpeak = msg.translations[state.targetTranslationCode]?.ifBlank { msg.originalText } ?: msg.originalText
                            val langToSpeak = if (msg.translations[state.targetTranslationCode].isNullOrBlank()) msg.sourceLanguage else state.targetTranslationCode
                            onSpeakText(textToSpeak, langToSpeak)
                        }
                    }
                )
            }

            (state.errorBanner ?: micDeniedNote)?.let { err ->
                Surface(color = ErrorCoralContainer, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = err,
                        color = ErrorCoral,
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                    )
                }
            }

            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth().testTag("group_messages_list"),
                contentPadding = PaddingValues(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (!isMember) {
                    item {
                        UnavailableDataCard(
                            title = "Members-Only Room",
                            explanation = "Join this group to read and send multilingual messages.",
                            icon = Icons.Filled.Lock
                        )
                    }
                } else if (state.messages.isEmpty()) {
                    item {
                        UnavailableDataCard(
                            title = "Start the Multilingual Discussion",
                            explanation = "Every message keeps its original text verbatim and supports translation across Egyptian Arabic, English, Russian, French, and more.",
                            icon = Icons.Filled.Forum
                        )
                    }
                } else {
                    items(state.messages, key = { it.id }) { msg ->
                        val mine = msg.senderId == currentUserId
                        val timeText = msg.createdAt?.toDate()?.let {
                            DateFormat.getTimeInstance(DateFormat.SHORT).format(it)
                        } ?: "Sending"
                        Card(
                            modifier = Modifier.fillMaxWidth().testTag("group_msg_${msg.id}"),
                            colors = CardDefaults.cardColors(
                                containerColor = if (mine) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                            ),
                            border = if (!mine) BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)) else null,
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "${msg.senderName} • ${msg.sourceLanguage} • $timeText",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.secondary
                                    )
                                    Row {
                                        if (!mine) {
                                            IconButton(
                                                onClick = { onReportGroup("GROUP_MESSAGE", "${msg.groupId}/${msg.id}", "Inappropriate group message") },
                                                modifier = Modifier.size(28.dp)
                                            ) {
                                                Icon(Icons.Filled.Flag, contentDescription = "Report message", modifier = Modifier.size(15.dp))
                                            }
                                        }
                                        if (mine || isModOrOwner) {
                                            IconButton(
                                                onClick = { onDeleteMessage(msg.id) },
                                                modifier = Modifier.size(28.dp).testTag("delete_group_msg_${msg.id}")
                                            ) {
                                                Icon(Icons.Filled.DeleteOutline, contentDescription = "Delete message", modifier = Modifier.size(16.dp))
                                            }
                                        }
                                    }
                                }

                                Text(text = msg.originalText, style = MaterialTheme.typography.bodyLarge)

                                val translated = msg.translations[state.targetTranslationCode]
                                if (!translated.isNullOrBlank()) {
                                    HorizontalDivider()
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Translation (${state.targetTranslationCode})",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        IconButton(
                                            onClick = { onSpeakText(translated, state.targetTranslationCode) },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = "Speak translation", modifier = Modifier.size(16.dp))
                                        }
                                    }
                                    Text(text = translated, style = MaterialTheme.typography.bodyMedium)
                                } else if (!msg.sourceLanguage.equals(state.targetTranslationCode, ignoreCase = true)) {
                                    TextButton(
                                        onClick = { onTranslateMessage(msg, state.targetTranslationCode) },
                                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                                    ) {
                                        Icon(Icons.Filled.Translate, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text("Translate to ${state.targetTranslationCode}", style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (isMember) {
                Surface(tonalElevation = 3.dp, modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilledTonalIconButton(
                            onClick = {
                                val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
                                if (granted) onDictateVoice() else micLauncher.launch(Manifest.permission.RECORD_AUDIO)
                            },
                            modifier = Modifier.testTag("group_mic_btn")
                        ) {
                            Icon(
                                imageVector = if (state.isTranscribingVoice) Icons.Filled.GraphicEq else Icons.Filled.Mic,
                                contentDescription = "Speak into Group Live"
                            )
                        }
                        OutlinedTextField(
                            value = draft,
                            onValueChange = { draft = it },
                            placeholder = { Text("Speak or message ${group.name}...") },
                            maxLines = 3,
                            modifier = Modifier.weight(1f).testTag("group_message_input")
                        )
                        IconButton(
                            onClick = {
                                val txt = draft.trim()
                                if (txt.isNotEmpty()) {
                                    onSendMessage(txt, autoTranslate)
                                    draft = ""
                                    onClearVoiceDraft()
                                }
                            },
                            enabled = draft.isNotBlank() && !state.isSending,
                            modifier = Modifier.testTag("group_send_btn")
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send group message")
                        }
                    }
                }
            }
        }
    }

    if (showManagementDialog && group != null) {
        val publicDirectory = (publicProfilesState as? UiState.Success)?.data.orEmpty()
        val inviteCandidates = publicDirectory.filter { it.userId !in group.memberIds && it.userId !in group.bannedIds }
        val pendingRequests = state.joinRequests.filter { it.status == "PENDING" }

        AlertDialog(
            onDismissRequest = { showManagementDialog = false },
            title = { Text("${group.name} • Members & Controls") },
            text = {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().heightIn(max = 380.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (isModOrOwner && pendingRequests.isNotEmpty()) {
                        item { Text("Pending Join Requests (${pendingRequests.size})", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold) }
                        items(pendingRequests, key = { "req_${it.userId}" }) { req ->
                            Card {
                                Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text("${req.userName} (${req.userCountry} • ${req.nativeLanguage})", style = MaterialTheme.typography.bodyMedium)
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Button(
                                            onClick = { onReviewJoinRequest(req, true) },
                                            modifier = Modifier.weight(1f).testTag("approve_group_req_${req.userId}")
                                        ) { Text("Approve") }
                                        OutlinedButton(
                                            onClick = { onReviewJoinRequest(req, false) },
                                            modifier = Modifier.weight(1f)
                                        ) { Text("Decline") }
                                    }
                                }
                            }
                        }
                    }

                    item { Text("Participants (${group.memberIds.size})", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold) }
                    items(group.memberIds, key = { "mem_$it" }) { uid ->
                        val profileMatch = publicDirectory.firstOrNull { it.userId == uid }
                        val roleLabel = when {
                            uid == group.ownerId -> "Owner"
                            uid in group.moderatorIds -> "Moderator"
                            else -> "Member"
                        }
                        val display = profileMatch?.displayName?.ifBlank { uid } ?: if (uid == currentUserId) "You" else uid
                        Card {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(display, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                    Text(roleLabel, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                                }
                                if (isModOrOwner && uid != group.ownerId && uid != currentUserId) {
                                    Row {
                                        if (isOwner && uid !in group.moderatorIds) {
                                            IconButton(onClick = { onPromoteModerator(uid) }) {
                                                Icon(Icons.Filled.AdminPanelSettings, contentDescription = "Promote moderator")
                                            }
                                        }
                                        IconButton(onClick = { onRemoveOrBanMember(uid, false) }) {
                                            Icon(Icons.Filled.PersonRemove, contentDescription = "Remove member")
                                        }
                                        IconButton(onClick = { onRemoveOrBanMember(uid, true) }) {
                                            Icon(Icons.Filled.Block, contentDescription = "Ban member", tint = ErrorCoral)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (isModOrOwner && inviteCandidates.isNotEmpty()) {
                        item { Text("Invite Community Members", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold) }
                        items(inviteCandidates.take(5), key = { "inv_${it.userId}" }) { cand ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("${cand.displayName} (${cand.nativeLanguage})", style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                                TextButton(onClick = { onInviteMember(cand) }) { Text("Invite") }
                            }
                        }
                    }

                    if (!isOwner && isMember) {
                        item {
                            OutlinedButton(
                                onClick = {
                                    showManagementDialog = false
                                    onLeaveGroup()
                                },
                                modifier = Modifier.fillMaxWidth().testTag("leave_group_btn")
                            ) {
                                Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = null)
                                Spacer(Modifier.width(6.dp))
                                Text("Leave Group")
                            }
                        }
                    }

                    item {
                        TextButton(
                            onClick = {
                                showManagementDialog = false
                                onReportGroup("GROUP", group.id, "Group policy violation")
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Report Group Room", color = ErrorCoral)
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showManagementDialog = false }) { Text("Done") } }
        )
    }
}
