package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.adventure3d.AmbientOceanAudioEngine
import com.example.adventure3d.Camera3D
import com.example.adventure3d.Immersive3DWorldCanvas
import com.example.data.model.*
import com.example.localization.AppStrings
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.AdventureWorldUiState

@Composable
fun ImmersiveAdventureScreen(
    strings: AppStrings,
    uiState: AdventureWorldUiState,
    currentUserId: String,
    myProfile: UserProfile?,
    joinableSessionsState: UiState<List<LiveAdventureSession>>,
    onCameraChange: (Camera3D) -> Unit,
    onSelectPoi: (AdventurePoi, () -> Unit) -> Unit,
    onPointAtPoi: (AdventurePoi) -> Unit,
    onToggleAudioMute: () -> Unit,
    onStartSession: (AdventureSessionMode) -> Unit,
    onJoinSession: (LiveAdventureSession) -> Unit,
    onLeaveSession: () -> Unit,
    onSendSessionChat: (String) -> Unit,
    onDictateVoiceChat: () -> Unit,
    onTranslateSessionMessage: (SessionChatMessage) -> Unit,
    onSpeakText: (String, String) -> Unit,
    onSetTargetLang: (String) -> Unit,
    onToggleSubtitles: () -> Unit,
    onToggleSpeakTranslations: () -> Unit,
    onSaveMemory: (AdventureExperience, String, List<SessionParticipantPresence>, MemoryVisibility) -> Unit,
    onConnectCompanion: (String, String, String, String, String) -> Unit,
    onOpenMatchmaking: () -> Unit,
    onOpenCrews: () -> Unit,
    onOpenRealTrip: (String) -> Unit,
    onBack: () -> Unit,
    onBridgeToReality: (AdventureExperience) -> Unit = {},
    onOpenLiveTranslator: () -> Unit = {},
    onOpenCityEvents: (String) -> Unit = {}
) {
    val adv = uiState.selectedAdventure
    val active = uiState.activeSession
    val audioScope = rememberCoroutineScope()
    val audioEngine = remember(adv.id) { AmbientOceanAudioEngine() }

    LaunchedEffect(adv.biome, uiState.audioMuted) {
        if (!uiState.audioMuted) {
            audioEngine.start(adv.biome, audioScope)
        } else {
            audioEngine.stop()
        }
    }
    DisposableEffect(adv.id) {
        onDispose { audioEngine.stop() }
    }

    BackHandler {
        if (active != null) onLeaveSession()
        onBack()
    }

    var chatInput by remember { mutableStateOf("") }
    var showSaveDialog by remember { mutableStateOf(false) }

    val matchingOpenSessions = remember(joinableSessionsState, adv.id, active?.id) {
        (joinableSessionsState as? UiState.Success)?.data
            ?.filter { it.status == "ACTIVE" && it.id != active?.id }
            .orEmpty()
    }

    Scaffold(
        topBar = {
            NiloraTopBar(
                title = adv.title,
                subtitle = if (active != null) "Live Session (${active.participantIds.size}/${active.maxParticipants})" else "${adv.destinationName} • Interactive 3D",
                onBackClick = {
                    if (active != null) onLeaveSession()
                    onBack()
                },
                actions = {
                    if (active != null) {
                        TextButton(
                            onClick = onLeaveSession,
                            modifier = Modifier.testTag("leave_adventure_session_btn")
                        ) {
                            Text("Leave", color = ErrorCoral, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .testTag("immersive_adventure_scroll"),
            contentPadding = PaddingValues(14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Interactive 3D Adventure Viewport
            item {
                Immersive3DWorldCanvas(
                    adventure = adv,
                    camera = uiState.camera,
                    currentUserId = currentUserId,
                    participants = uiState.sessionParticipants,
                    discoveredPoiIds = uiState.discoveredPoiIds,
                    selectedPoi = uiState.selectedPoi,
                    audioMuted = uiState.audioMuted,
                    onCameraChange = onCameraChange,
                    onSelectPoi = { poi -> onSelectPoi(poi) { audioEngine.playDiscoveryChime() } },
                    onPointAtPoi = onPointAtPoi,
                    onToggleAudio = onToggleAudioMute,
                    modifier = Modifier.height(330.dp)
                )
            }

            // Selected 3D Discovery Point Fact Card
            uiState.selectedPoi?.let { poi ->
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth().testTag("selected_poi_card"),
                        border = BorderStroke(1.dp, EgyptianGold)
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${poi.name} (${poi.category})",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = if (poi.depthMeters > 0) "~${poi.depthMeters}m depth" else "Landmark",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = NileTurquoiseDark
                                )
                            }
                            Text(poi.factText, style = MaterialTheme.typography.bodySmall)
                            Text(
                                text = "Eco Tip: ${poi.conservationTip}",
                                style = MaterialTheme.typography.labelSmall,
                                color = NileTurquoiseDark
                            )
                        }
                    }
                }
            }

            // Mode Selection Row (EXPLORE SOLO | JOIN OTHERS | CREATE A CREW | FIND PEOPLE)
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Live Worlds & Exploration Modes", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { onStartSession(AdventureSessionMode.SOLO) },
                            modifier = Modifier.weight(1f).defaultMinSize(minHeight = 48.dp).testTag("mode_explore_solo_btn")
                        ) { Text(strings.exploreSoloLabel, maxLines = 1) }

                        Button(
                            onClick = { onStartSession(AdventureSessionMode.JOIN_OTHERS) },
                            colors = ButtonDefaults.buttonColors(containerColor = MidnightNavy, contentColor = SoftIvory),
                            modifier = Modifier.weight(1f).defaultMinSize(minHeight = 48.dp).testTag("mode_open_joinable_btn")
                        ) { Text(strings.joinOthersLabel, maxLines = 1) }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilledTonalButton(
                            onClick = onOpenCrews,
                            modifier = Modifier.weight(1f).defaultMinSize(minHeight = 48.dp).testTag("mode_create_crew_btn")
                        ) { Text(strings.createCrewLabel, maxLines = 1) }

                        FilledTonalButton(
                            onClick = onOpenMatchmaking,
                            modifier = Modifier.weight(1f).defaultMinSize(minHeight = 48.dp).testTag("mode_find_people_btn")
                        ) { Text("Find People", maxLines = 1) }
                    }
                }
            }

            // Active Session Live Presence & NILORA LIVE Multilingual Communication
            if (active != null) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth().testTag("live_session_panel"),
                        border = BorderStroke(1.5.dp, NileTurquoise)
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = "Live Session Companions (${uiState.sessionParticipants.size})",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            val realOthers = uiState.sessionParticipants.filter { it.userId != currentUserId }
                            if (realOthers.isEmpty()) {
                                Text(
                                    text = "You are currently exploring with 0 other real participants inside this session. Other NILORA users can join via 'Join Others'.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            } else {
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    items(realOthers, key = { it.userId }) { p ->
                                        AssistChip(
                                            onClick = {
                                                onConnectCompanion(p.userId, p.displayName, p.country, p.languageCode, adv.title)
                                            },
                                            label = { Text("${p.displayName} (${p.country}) • Connect") },
                                            leadingIcon = {
                                                Icon(Icons.Filled.PersonAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                                            }
                                        )
                                    }
                                }
                            }

                            HorizontalDivider()

                            // NILORA LIVE 4-Language Session Settings
                            Text("NILORA LIVE Session Communication", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                items(NiloraTaxonomy.worldLanguages.take(5)) { lang ->
                                    FilterChip(
                                        selected = uiState.targetTranslationLanguage == lang.code,
                                        onClick = { onSetTargetLang(lang.code) },
                                        label = { Text(lang.nativeName) },
                                        modifier = Modifier.testTag("session_lang_${lang.code}")
                                    )
                                }
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                FilterChip(
                                    selected = uiState.showSubtitles,
                                    onClick = onToggleSubtitles,
                                    label = { Text("Subtitles") }
                                )
                                FilterChip(
                                    selected = uiState.speakTranslationsAloud,
                                    onClick = onToggleSpeakTranslations,
                                    label = { Text("Speak Aloud") }
                                )
                            }

                            // Session Message Feed
                            if (uiState.sessionMessages.isEmpty()) {
                                Text(
                                    text = "Speak or type below to share discoveries in ${NiloraTaxonomy.languageDisplayLabel(myProfile?.nativeLanguage ?: "en")} with live translation.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            } else {
                                uiState.sessionMessages.takeLast(8).forEach { msg ->
                                    val translated = msg.translations[uiState.targetTranslationLanguage]
                                    Surface(
                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(10.dp),
                                            verticalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text(
                                                    "${msg.senderName} (${msg.sourceLanguage})",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                TextButton(
                                                    onClick = { onTranslateSessionMessage(msg) },
                                                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp)
                                                ) {
                                                    Text("Translate & Speak", style = MaterialTheme.typography.labelSmall)
                                                }
                                            }
                                            Text(msg.originalText, style = MaterialTheme.typography.bodyMedium)
                                            if (uiState.showSubtitles && !translated.isNullOrBlank()) {
                                                Text(
                                                    text = "${strings.translationLabel}: $translated",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = NileTurquoiseDark,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            if (uiState.isTranslatingChat) {
                                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                IconButton(
                                    onClick = onDictateVoiceChat,
                                    modifier = Modifier.size(48.dp).testTag("session_voice_speak_btn")
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Mic,
                                        contentDescription = strings.speakNowLabel,
                                        tint = if (uiState.isListeningSpeech) SunsetCoral else NileTurquoiseDark
                                    )
                                }
                                OutlinedTextField(
                                    value = chatInput,
                                    onValueChange = { chatInput = it },
                                    placeholder = { Text("Speak or message companions...") },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f).testTag("session_chat_input")
                                )
                                IconButton(
                                    onClick = {
                                        if (chatInput.isNotBlank()) {
                                            onSendSessionChat(chatInput)
                                            chatInput = ""
                                        }
                                    },
                                    modifier = Modifier.size(48.dp).testTag("session_chat_send_btn")
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send")
                                }
                            }
                            Text(
                                text = "Honest Note: Live speech uses device SpeechRecognizer + Gemini AI translation; dialect nuance and ambient noise may require repeating. Continuous multi-party WebRTC VoIP audio awaits SFU setup.",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                // Show real joinable sessions when browsing before opening one
                item {
                    Card(modifier = Modifier.fillMaxWidth().testTag("joinable_sessions_card")) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text("Real Open Sessions Right Now (${matchingOpenSessions.size})", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            if (matchingOpenSessions.isEmpty()) {
                                Text(
                                    text = "No live shared sessions are open right now (NILORA never shows fake active rooms). Press 'Explore Solo' or 'Join Others' above to host a real session.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            } else {
                                matchingOpenSessions.forEach { sess ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text("${sess.adventureTitle} — Host: ${sess.hostName}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                            Text("${sess.destinationName} • ${sess.participantIds.size}/${sess.maxParticipants} explorers", style = MaterialTheme.typography.labelSmall)
                                        }
                                        Button(
                                            onClick = { onJoinSession(sess) },
                                            modifier = Modifier.defaultMinSize(minHeight = 48.dp)
                                        ) { Text("Join") }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Post-Adventure Continuation Actions
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = WarmSandContainer),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text("Turn Virtual Adventure Into Real Connection", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = { showSaveDialog = true },
                                modifier = Modifier.weight(1f).defaultMinSize(minHeight = 48.dp).testTag("save_adventure_memory_btn")
                            ) {
                                Text("Save Memory", maxLines = 1)
                            }
                            OutlinedButton(
                                onClick = { onOpenRealTrip(adv.destinationName) },
                                modifier = Modifier.weight(1f).defaultMinSize(minHeight = 48.dp)
                            ) {
                                Text("Real Trip Plan", maxLines = 1)
                            }
                        }
                        FilledTonalButton(
                            onClick = { onBridgeToReality(adv) },
                            modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 48.dp).testTag("bridge_adventure_btn")
                        ) {
                            Icon(Icons.Filled.Route, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Bridge to Reality Plan -> Event / Team-Up / Mission")
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = onOpenLiveTranslator,
                                modifier = Modifier.weight(1f).defaultMinSize(minHeight = 48.dp).testTag("adventure_open_live_btn")
                            ) {
                                Icon(Icons.Filled.Translate, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Live Hub", maxLines = 1)
                            }
                            OutlinedButton(
                                onClick = { onOpenCityEvents(adv.destinationName) },
                                modifier = Modifier.weight(1f).defaultMinSize(minHeight = 48.dp).testTag("adventure_open_events_btn")
                            ) {
                                Icon(Icons.Filled.Celebration, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("City Events", maxLines = 1)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showSaveDialog) {
        var note by remember { mutableStateOf("") }
        var visibility by remember { mutableStateOf(MemoryVisibility.PRIVATE) }
        AlertDialog(
            onDismissRequest = { showSaveDialog = false },
            title = { Text("Save Adventure Memory") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = note,
                        onValueChange = { note = it },
                        label = { Text("Reflection from ${adv.destinationName}") },
                        minLines = 2,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        MemoryVisibility.entries.forEach { vis ->
                            FilterChip(
                                selected = visibility == vis,
                                onClick = { visibility = vis },
                                label = { Text(vis.name.lowercase().replaceFirstChar { it.uppercase() }.take(7)) }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onSaveMemory(adv, note, uiState.sessionParticipants, visibility)
                        showSaveDialog = false
                    }
                ) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { showSaveDialog = false }) { Text("Cancel") }
            }
        )
    }
}
