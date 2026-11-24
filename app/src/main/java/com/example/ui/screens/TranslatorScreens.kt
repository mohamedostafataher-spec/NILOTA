package com.example.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.R
import com.example.data.model.*
import com.example.localization.AppStrings
import com.example.niloralive.ArabicDialectMode
import com.example.niloralive.PipelineStage
import com.example.niloralive.SubtitleDisplayMode
import com.example.niloralive.TranslationContextDomain
import com.example.niloralive.TtsVoiceOption
import com.example.niloralive.VoiceCapabilityReport
import com.example.ui.components.LiveSubtitleItem
import com.example.ui.components.LiveSubtitlesOverlayCard
import com.example.ui.components.NiloraFooter
import com.example.ui.components.NiloraTopBar
import com.example.ui.components.OneLanguageIdentitySummaryBanner
import com.example.ui.components.TranslationDomainAndAudioBar
import com.example.ui.components.UnavailableDataCard
import com.example.ui.theme.*
import com.example.ui.viewmodel.SharedTranslationMessage
import com.example.ui.viewmodel.TranslatorUiState

@Composable
fun RealTimeTranslatorScreen(
    strings: AppStrings,
    state: TranslatorUiState,
    voiceCapabilities: VoiceCapabilityReport,
    myRoomsState: UiState<List<ConversationRoom>>,
    isOnline: Boolean,
    onGrantVoiceConsent: (Boolean) -> Unit,
    onSetArabicDialectMode: (ArabicDialectMode) -> Unit,
    onSetAutoTranslateAfterSpeech: (Boolean) -> Unit,
    onOpenCreateRoom: () -> Unit,
    onOpenJoinRoom: () -> Unit,
    onOpenSharedMode: () -> Unit,
    onOpenLiveEvaluation: () -> Unit,
    onResumeRoom: (String) -> Unit,
    onOpenGroupsLive: () -> Unit = {},
    onOpenDirectChats: () -> Unit = {},
    onOpenAiAssistant: () -> Unit = {},
    onEditLanguageIdentity: () -> Unit = {},
    interfaceLanguageCode: String = "en",
    availableVoices: List<TtsVoiceOption> = emptyList(),
    onSetTranslationDomain: (TranslationContextDomain) -> Unit = {},
    onToggleAutoDetectSource: (Boolean) -> Unit = {},
    onTogglePreferOriginalAudio: (Boolean) -> Unit = {},
    onSelectTtsVoice: (String) -> Unit = {}
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().testTag("live_hub_list"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text(strings.realTimeTranslatorTitle, style = MaterialTheme.typography.headlineLarge)
            Text(
                "NILORA LIVE • Egyptian Arabic (ar-EG) • English • Russian • French",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.secondary
            )
        }

        if (!isOnline) {
            item {
                Card(colors = CardDefaults.cardColors(containerColor = ErrorCoralContainer)) {
                    Row(modifier = Modifier.padding(14.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Filled.WifiOff, contentDescription = null, tint = ErrorCoral)
                        Text("Network disconnected. AI translation and two-phone rooms require internet access.", color = ErrorCoral)
                    }
                }
            }
        }

        item {
            Image(
                painter = painterResource(id = R.drawable.img_translator_art),
                contentDescription = "NILORA Voice Translation",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(155.dp)
                    .clip(RoundedCornerShape(22.dp)),
                contentScale = ContentScale.Crop
            )
        }

        item {
            OneLanguageIdentitySummaryBanner(
                interfaceLangCode = interfaceLanguageCode,
                spokenLangCode = state.spokenLanguageCode,
                targetTranslationCode = state.targetTranslationLanguageCode,
                subtitleMode = state.subtitleDisplayMode,
                onEditLanguageIdentity = onEditLanguageIdentity
            )
        }

        // Explicit Audio/Text Transmission Consent Disclosure
        item {
            VoiceConsentCard(
                consentGranted = state.voiceConsentGranted,
                onGrantConsent = onGrantVoiceConsent
            )
        }

        // Dialect, Context Domain, Pipeline & Voice Capability Settings
        item {
            Card(shape = RoundedCornerShape(18.dp)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Egyptian Arabic Dialect, Context & Voice Controls", style = MaterialTheme.typography.titleMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        FilterChip(
                            selected = state.arabicDialectMode == ArabicDialectMode.EGYPTIAN_COLLOQUIAL,
                            onClick = { onSetArabicDialectMode(ArabicDialectMode.EGYPTIAN_COLLOQUIAL) },
                            label = { Text("Egyptian Colloquial (ar-EG)") },
                            modifier = Modifier.weight(1f).testTag("dialect_egyptian_chip")
                        )
                        FilterChip(
                            selected = state.arabicDialectMode == ArabicDialectMode.MODERN_STANDARD,
                            onClick = { onSetArabicDialectMode(ArabicDialectMode.MODERN_STANDARD) },
                            label = { Text("Modern Standard (MSA)") },
                            modifier = Modifier.weight(1f).testTag("dialect_msa_chip")
                        )
                    }
                    TranslationDomainAndAudioBar(
                        selectedDomain = state.translationDomain,
                        autoDetectSource = state.autoDetectSource,
                        preferOriginalAudio = state.preferOriginalAudio,
                        availableVoices = availableVoices,
                        selectedVoiceName = state.selectedTtsVoiceName,
                        onSelectDomain = onSetTranslationDomain,
                        onToggleAutoDetect = onToggleAutoDetectSource,
                        onTogglePreferOriginalAudio = onTogglePreferOriginalAudio,
                        onSelectVoice = onSelectTtsVoice
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Auto-translate immediately after speech", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                            Text(
                                "Turn off to always review and edit recognized speech before translating.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = state.autoTranslateAfterSpeech,
                            onCheckedChange = onSetAutoTranslateAfterSpeech,
                            modifier = Modifier.testTag("translator_auto_translate_switch")
                        )
                    }
                    Text(
                        "TTS Arabic Voice: ${voiceCapabilities.bestArabicVoiceName}",
                        style = MaterialTheme.typography.labelMedium,
                        color = if (voiceCapabilities.hasExactEgyptianArabicVoice) VerifiedEmerald else MaterialTheme.colorScheme.secondary
                    )
                }
            }
        }

        // Mode 1: Two Different Phones
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = DeepNavy),
                shape = RoundedCornerShape(22.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Filled.PhonelinkSetup, contentDescription = null, tint = Turquoise)
                        Text(strings.twoPhoneModeTitle, style = MaterialTheme.typography.titleLarge, color = Color.White)
                    }
                    Text(
                        "Create a private Cloud Firestore conversation room or join with a code. Speech recognition runs on-device, translates into Egyptian Arabic / target language, and speaks locally via Android TTS.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.85f)
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                        Button(
                            onClick = onOpenCreateRoom,
                            colors = ButtonDefaults.buttonColors(containerColor = Turquoise, contentColor = DeepNavyDark),
                            modifier = Modifier.weight(1f).testTag("translator_create_room_btn")
                        ) {
                            Text(strings.createRoomLabel)
                        }
                        OutlinedButton(
                            onClick = onOpenJoinRoom,
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                            modifier = Modifier.weight(1f).testTag("translator_join_room_btn")
                        ) {
                            Text(strings.joinRoomLabel)
                        }
                    }
                }
            }
        }

        // Mode 2: One Phone Shared
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = TurquoiseContainer),
                shape = RoundedCornerShape(22.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("translator_shared_mode_card")
                    .clickable(onClick = onOpenSharedMode)
            ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Filled.RecordVoiceOver, contentDescription = null, tint = TurquoiseOnContainer)
                        Text(strings.onePhoneModeTitle, style = MaterialTheme.typography.titleLarge, color = TurquoiseOnContainer)
                    }
                    Text(
                        "Split-screen face-to-face mode: top panel rotates 180° for the person opposite you, with live subtitles, independent language selectors, and playback controls.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TurquoiseOnContainer.copy(alpha = 0.88f)
                    )
                    Button(
                        onClick = onOpenSharedMode,
                        modifier = Modifier.testTag("translator_open_shared_btn")
                    ) {
                        Text("Launch Face-to-Face Mode")
                    }
                }
            }
        }

        // Mode 3: Group Live (Multi-Speaker) + Direct Chats & AI Guide
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = WarmSandContainer),
                shape = RoundedCornerShape(22.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("translator_group_live_card")
                    .clickable(onClick = onOpenGroupsLive)
            ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Filled.Groups, contentDescription = null, tint = WarmSandOnContainer)
                        Text("Group Live (Multi-Speaker)", style = MaterialTheme.typography.titleLarge, color = WarmSandOnContainer)
                    }
                    Text(
                        "Speak or write in your language inside shared groups, events, or crews. Each participant views live subtitles and audio in their own preferred language.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = WarmSandOnContainer.copy(alpha = 0.9f)
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = onOpenGroupsLive,
                            modifier = Modifier.testTag("translator_open_group_live_btn")
                        ) {
                            Text("Open Group Live")
                        }
                        OutlinedButton(
                            onClick = onOpenDirectChats,
                            modifier = Modifier.testTag("translator_open_direct_chats_btn")
                        ) {
                            Text("1:1 Chats")
                        }
                        OutlinedButton(
                            onClick = onOpenAiAssistant,
                            modifier = Modifier.testTag("translator_open_ai_guide_btn")
                        ) {
                            Text("AI Guide")
                        }
                    }
                }
            }
        }

        // Diagnostics & Speech Evaluation Lab
        item {
            OutlinedCard(
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("translator_eval_lab_card")
                    .clickable(onClick = onOpenLiveEvaluation)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Analytics, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Column {
                            Text("Speech Diagnostics & WER Evaluation Lab", style = MaterialTheme.typography.titleMedium)
                            Text(
                                "Inspect device mic/TTS capabilities, evaluated open-source models (whisper.cpp, NLLB-200), ambient noise floor, and WER.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Icon(Icons.Filled.ArrowForwardIos, contentDescription = null, modifier = Modifier.size(16.dp))
                }
            }
        }

        // Recent / Active Two-Phone Rooms
        item {
            Text("Your Conversation Rooms", style = MaterialTheme.typography.titleLarge)
        }
        when (myRoomsState) {
            is UiState.Loading -> item { LinearProgressIndicator(modifier = Modifier.fillMaxWidth()) }
            is UiState.Error -> item { UnavailableDataCard("Rooms Unavailable", myRoomsState.message) }
            is UiState.Success -> {
                val activeRooms = myRoomsState.data.filter { it.status != "ENDED" }
                if (activeRooms.isEmpty()) {
                    item {
                        Text(
                            "No active two-phone rooms right now.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    items(activeRooms) { room ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onResumeRoom(room.roomCode) }
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("Room ${room.roomCode}", style = MaterialTheme.typography.titleMedium)
                                    Text("Status: ${room.status} • ${room.hostLanguage.uppercase()} ↔ ${room.guestLanguage.uppercase()}", style = MaterialTheme.typography.bodyMedium)
                                }
                                Icon(Icons.Filled.ArrowForwardIos, contentDescription = "Open room", modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }
        }

        item { NiloraFooter() }
    }
}

@Composable
private fun VoiceConsentCard(
    consentGranted: Boolean,
    onGrantConsent: (Boolean) -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (consentGranted) TurquoiseContainer else WarningAmberContainer
        ),
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier.fillMaxWidth().testTag("voice_consent_card")
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = if (consentGranted) Icons.Filled.VerifiedUser else Icons.Filled.PrivacyTip,
                    contentDescription = null,
                    tint = if (consentGranted) TurquoiseOnContainer else DeepNavyDark
                )
                Text(
                    text = if (consentGranted) "Voice & Translation Consent Active" else "Consent Required Before Capturing Speech",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (consentGranted) TurquoiseOnContainer else DeepNavyDark
                )
            }
            Text(
                "Microphone audio is processed by your device's Android SpeechRecognizer. Recognized text is sent to Firebase AI Logic (Gemini) only after your consent. Raw audio recordings are never stored or uploaded to Firebase.",
                style = MaterialTheme.typography.bodySmall,
                color = if (consentGranted) TurquoiseOnContainer else DeepNavyDark
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (!consentGranted) {
                    Button(
                        onClick = { onGrantConsent(true) },
                        modifier = Modifier.testTag("grant_voice_consent_btn")
                    ) {
                        Text("Allow Speech & Translation")
                    }
                } else {
                    TextButton(
                        onClick = { onGrantConsent(false) },
                        modifier = Modifier.testTag("revoke_voice_consent_btn")
                    ) {
                        Text("Revoke Consent")
                    }
                }
            }
        }
    }
}

@Composable
private fun PipelineAndVadStatusBar(
    stage: PipelineStage,
    rmsNormalized: Float,
    partialTranscript: String,
    pipelineError: String?
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            AssistChip(
                onClick = {},
                label = { Text("Pipeline: ${stage.label}") },
                leadingIcon = {
                    val icon = when (stage) {
                        PipelineStage.LISTENING, PipelineStage.SPEECH_DETECTED, PipelineStage.RECOGNIZING -> Icons.Filled.Mic
                        PipelineStage.AWAITING_CORRECTION -> Icons.Filled.Edit
                        PipelineStage.TRANSLATING, PipelineStage.VALIDATING -> Icons.Filled.Translate
                        PipelineStage.GENERATING_VOICE, PipelineStage.PLAYING -> Icons.Filled.VolumeUp
                        PipelineStage.ERROR -> Icons.Filled.ErrorOutline
                        else -> Icons.Filled.CheckCircleOutline
                    }
                    Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp))
                },
                modifier = Modifier.testTag("pipeline_stage_chip")
            )
            if (stage == PipelineStage.LISTENING || stage == PipelineStage.SPEECH_DETECTED || stage == PipelineStage.RECOGNIZING) {
                Text("VAD ${(rmsNormalized * 100).toInt()}%", style = MaterialTheme.typography.labelSmall)
            }
        }
        if (stage == PipelineStage.LISTENING || stage == PipelineStage.SPEECH_DETECTED || stage == PipelineStage.RECOGNIZING) {
            LinearProgressIndicator(
                progress = { rmsNormalized.coerceIn(0.05f, 1f) },
                modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(4.dp))
            )
        } else if (stage == PipelineStage.TRANSLATING || stage == PipelineStage.VALIDATING || stage == PipelineStage.GENERATING_VOICE) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }
        if (partialTranscript.isNotBlank()) {
            Text(
                "Recognizing (unstable preview): $partialTranscript",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary
            )
        }
        if (!pipelineError.isNullOrBlank()) {
            Text(
                pipelineError,
                style = MaterialTheme.typography.bodySmall,
                color = ErrorCoral,
                modifier = Modifier.testTag("pipeline_error_text")
            )
        }
    }
}

@Composable
private fun TranscriptCorrectionPanel(
    pendingText: String,
    onTextChange: (String) -> Unit,
    onConfirmTranslate: () -> Unit,
    onDismiss: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth().testTag("transcript_correction_panel")
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Review or Correct Recognized Speech Before Translation", style = MaterialTheme.typography.labelLarge)
            OutlinedTextField(
                value = pendingText,
                onValueChange = onTextChange,
                label = { Text("Recognized Transcript") },
                modifier = Modifier.fillMaxWidth().testTag("transcript_correction_input")
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                    Text("Cancel")
                }
                Button(
                    onClick = onConfirmTranslate,
                    enabled = pendingText.isNotBlank(),
                    modifier = Modifier.weight(1f).testTag("transcript_correction_confirm_btn")
                ) {
                    Text("Translate")
                }
            }
        }
    }
}

@Composable
private fun AudioPlaybackControlsRow(
    isSpeaking: Boolean,
    isMuted: Boolean,
    ttsSpeed: Float,
    ttsVolume: Float,
    onPauseOrStop: () -> Unit,
    onReplayLast: () -> Unit,
    onToggleMute: () -> Unit,
    onSetSpeed: (Float) -> Unit,
    onSetVolume: (Float) -> Unit
) {
    var showSliders by remember { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                FilledTonalButton(
                    onClick = onReplayLast,
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier.testTag("audio_replay_btn")
                ) {
                    Icon(Icons.Filled.Replay, contentDescription = "Replay", modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Replay")
                }
                if (isSpeaking) {
                    FilledTonalButton(
                        onClick = onPauseOrStop,
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.testTag("audio_stop_btn")
                    ) {
                        Icon(Icons.Filled.Stop, contentDescription = "Stop audio", modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Stop")
                    }
                }
            }
            Row {
                IconButton(onClick = onToggleMute) {
                    Icon(if (isMuted) Icons.Filled.VolumeOff else Icons.Filled.VolumeUp, contentDescription = "Mute")
                }
                IconButton(onClick = { showSliders = !showSliders }) {
                    Icon(Icons.Filled.Tune, contentDescription = "Speed and volume")
                }
            }
        }
        if (showSliders) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Speed ${String.format("%.1fx", ttsSpeed)}", style = MaterialTheme.typography.labelSmall, modifier = Modifier.width(68.dp))
                Slider(value = ttsSpeed, onValueChange = onSetSpeed, valueRange = 0.6f..1.6f, modifier = Modifier.weight(1f))
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Vol ${(ttsVolume * 100).toInt()}%", style = MaterialTheme.typography.labelSmall, modifier = Modifier.width(68.dp))
                Slider(value = ttsVolume, onValueChange = onSetVolume, valueRange = 0f..1f, modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
fun CreateConversationRoomScreen(
    userName: String,
    defaultLanguage: NiloraLanguage,
    state: TranslatorUiState,
    rmsNormalized: Float,
    isSpeaking: Boolean,
    isMuted: Boolean,
    ttsSpeed: Float,
    ttsVolume: Float,
    onGrantVoiceConsent: (Boolean) -> Unit,
    onCreateRoom: (NiloraLanguage, Boolean) -> Unit,
    onSelectMyLanguage: (NiloraLanguage) -> Unit,
    onStartVoice: () -> Unit,
    onStopVoice: () -> Unit,
    onUpdatePendingTranscript: (String) -> Unit,
    onConfirmPendingTranscript: () -> Unit,
    onDismissPendingTranscript: () -> Unit,
    onSendText: (String) -> Unit,
    onRepeatAudio: (String, NiloraLanguage) -> Unit,
    onReplayLastAudio: () -> Unit,
    onPauseOrStopAudio: () -> Unit,
    onToggleMute: () -> Unit,
    onSetSpeed: (Float) -> Unit,
    onSetVolume: (Float) -> Unit,
    onLeaveRoom: () -> Unit,
    onEndRoom: () -> Unit,
    onBack: () -> Unit,
    onSetSubtitleMode: (SubtitleDisplayMode) -> Unit = {},
    onSetTranslationDomain: (TranslationContextDomain) -> Unit = {},
    onToggleAutoDetectSource: (Boolean) -> Unit = {},
    onTogglePreferOriginalAudio: (Boolean) -> Unit = {},
    availableVoices: List<TtsVoiceOption> = emptyList(),
    onSelectVoice: (String) -> Unit = {}
) {
    val context = LocalContext.current
    var selectedLang by remember { mutableStateOf(defaultLanguage) }
    var retainTranscript by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            NiloraTopBar(
                title = if (state.activeRoom != null) "Room ${state.activeRoom.roomCode}" else "Create Conversation Room",
                subtitle = state.activeRoom?.let { "${state.roomConnectionStatus} • ${it.status}" },
                onBackClick = onBack,
                actions = {
                    if (state.activeRoom != null) {
                        IconButton(onClick = onToggleMute) {
                            Icon(if (isMuted) Icons.Filled.VolumeOff else Icons.Filled.VolumeUp, contentDescription = "Mute")
                        }
                        IconButton(
                            onClick = {
                                val inviteLink = "https://nilora.app/join/${state.activeRoom.roomCode}"
                                val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, "Join my NILORA Voice Translation Room.\nCode: ${state.activeRoom.roomCode}\nLink: $inviteLink")
                                }
                                context.startActivity(Intent.createChooser(sendIntent, "Share Room Invite"))
                            }
                        ) {
                            Icon(Icons.Filled.Share, contentDescription = "Share invite")
                        }
                    }
                }
            )
        }
    ) { padding ->
        if (state.activeRoom == null) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                VoiceConsentCard(consentGranted = state.voiceConsentGranted, onGrantConsent = onGrantVoiceConsent)
                Text("Select Your Spoken Language", style = MaterialTheme.typography.titleLarge)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(NiloraLanguage.entries) { lang ->
                        FilterChip(
                            selected = selectedLang == lang,
                            onClick = { selectedLang = lang },
                            label = { Text(lang.nativeName) }
                        )
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = retainTranscript, onCheckedChange = { retainTranscript = it })
                    Text("Retain translated text transcript after session ends (audio is never saved)", style = MaterialTheme.typography.bodyMedium)
                }
                if (state.roomError != null) {
                    Text(state.roomError, color = ErrorCoral, style = MaterialTheme.typography.bodyMedium)
                }
                Button(
                    onClick = { onCreateRoom(selectedLang, retainTranscript) },
                    enabled = !state.isConnectingRoom,
                    modifier = Modifier.fillMaxWidth().height(52.dp).testTag("create_room_confirm_btn")
                ) {
                    Text(if (state.isConnectingRoom) "Creating Room..." else "Generate Secure Room Code")
                }
            }
        } else {
            ActiveTwoPhoneRoomView(
                room = state.activeRoom,
                messages = state.roomMessages,
                currentUserId = state.activeRoom.hostId,
                state = state,
                rmsNormalized = rmsNormalized,
                isSpeaking = isSpeaking,
                isMuted = isMuted,
                ttsSpeed = ttsSpeed,
                ttsVolume = ttsVolume,
                onGrantVoiceConsent = onGrantVoiceConsent,
                onSelectLanguage = onSelectMyLanguage,
                onStartVoice = onStartVoice,
                onStopVoice = onStopVoice,
                onUpdatePendingTranscript = onUpdatePendingTranscript,
                onConfirmPendingTranscript = onConfirmPendingTranscript,
                onDismissPendingTranscript = onDismissPendingTranscript,
                onSendText = onSendText,
                onRepeatAudio = onRepeatAudio,
                onReplayLastAudio = onReplayLastAudio,
                onPauseOrStopAudio = onPauseOrStopAudio,
                onToggleMute = onToggleMute,
                onSetSpeed = onSetSpeed,
                onSetVolume = onSetVolume,
                onLeaveRoom = onLeaveRoom,
                onEndRoom = onEndRoom,
                onSetSubtitleMode = onSetSubtitleMode,
                onSetTranslationDomain = onSetTranslationDomain,
                onToggleAutoDetectSource = onToggleAutoDetectSource,
                onTogglePreferOriginalAudio = onTogglePreferOriginalAudio,
                availableVoices = availableVoices,
                onSelectVoice = onSelectVoice,
                modifier = Modifier.padding(padding)
            )
        }
    }
}

@Composable
fun JoinConversationRoomScreen(
    state: TranslatorUiState,
    rmsNormalized: Float,
    isSpeaking: Boolean,
    currentUserId: String,
    isMuted: Boolean,
    ttsSpeed: Float,
    ttsVolume: Float,
    onGrantVoiceConsent: (Boolean) -> Unit,
    onJoinRoom: (String, NiloraLanguage) -> Unit,
    onSelectMyLanguage: (NiloraLanguage) -> Unit,
    onStartVoice: () -> Unit,
    onStopVoice: () -> Unit,
    onUpdatePendingTranscript: (String) -> Unit,
    onConfirmPendingTranscript: () -> Unit,
    onDismissPendingTranscript: () -> Unit,
    onSendText: (String) -> Unit,
    onRepeatAudio: (String, NiloraLanguage) -> Unit,
    onReplayLastAudio: () -> Unit,
    onPauseOrStopAudio: () -> Unit,
    onToggleMute: () -> Unit,
    onSetSpeed: (Float) -> Unit,
    onSetVolume: (Float) -> Unit,
    onLeaveRoom: () -> Unit,
    onEndRoom: () -> Unit,
    onBack: () -> Unit,
    onSetSubtitleMode: (SubtitleDisplayMode) -> Unit = {},
    onSetTranslationDomain: (TranslationContextDomain) -> Unit = {},
    onToggleAutoDetectSource: (Boolean) -> Unit = {},
    onTogglePreferOriginalAudio: (Boolean) -> Unit = {},
    availableVoices: List<TtsVoiceOption> = emptyList(),
    onSelectVoice: (String) -> Unit = {}
) {
    var codeInput by remember { mutableStateOf("") }
    var selectedLang by remember { mutableStateOf(NiloraLanguage.ARABIC) }

    Scaffold(
        topBar = {
            NiloraTopBar(
                title = if (state.activeRoom != null) "Room ${state.activeRoom.roomCode}" else "Join Conversation Room",
                subtitle = state.activeRoom?.let { "${state.roomConnectionStatus} • ${it.status}" },
                onBackClick = onBack,
                actions = {
                    if (state.activeRoom != null) {
                        IconButton(onClick = onToggleMute) {
                            Icon(if (isMuted) Icons.Filled.VolumeOff else Icons.Filled.VolumeUp, contentDescription = "Mute")
                        }
                    }
                }
            )
        }
    ) { padding ->
        if (state.activeRoom == null) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                VoiceConsentCard(consentGranted = state.voiceConsentGranted, onGrantConsent = onGrantVoiceConsent)
                OutlinedTextField(
                    value = codeInput,
                    onValueChange = { input ->
                        // Accept either raw code or full invite URL like https://nilora.app/join/NLR48291
                        codeInput = input.substringAfterLast("/join/", input).uppercase().trim()
                    },
                    label = { Text("Enter Room Code or Invite Link (e.g. NLR48291)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("join_room_code_input")
                )
                Text("Select Your Spoken Language", style = MaterialTheme.typography.titleMedium)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(NiloraLanguage.entries) { lang ->
                        FilterChip(
                            selected = selectedLang == lang,
                            onClick = { selectedLang = lang },
                            label = { Text(lang.nativeName) }
                        )
                    }
                }
                if (state.roomError != null) {
                    Text(state.roomError, color = ErrorCoral)
                }
                Button(
                    onClick = { onJoinRoom(codeInput, selectedLang) },
                    enabled = codeInput.isNotBlank() && !state.isConnectingRoom,
                    modifier = Modifier.fillMaxWidth().height(52.dp).testTag("join_room_submit_btn")
                ) {
                    Text(if (state.isConnectingRoom) "Joining..." else "Join Live Room")
                }
            }
        } else {
            ActiveTwoPhoneRoomView(
                room = state.activeRoom,
                messages = state.roomMessages,
                currentUserId = currentUserId,
                state = state,
                rmsNormalized = rmsNormalized,
                isSpeaking = isSpeaking,
                isMuted = isMuted,
                ttsSpeed = ttsSpeed,
                ttsVolume = ttsVolume,
                onGrantVoiceConsent = onGrantVoiceConsent,
                onSelectLanguage = onSelectMyLanguage,
                onStartVoice = onStartVoice,
                onStopVoice = onStopVoice,
                onUpdatePendingTranscript = onUpdatePendingTranscript,
                onConfirmPendingTranscript = onConfirmPendingTranscript,
                onDismissPendingTranscript = onDismissPendingTranscript,
                onSendText = onSendText,
                onRepeatAudio = onRepeatAudio,
                onReplayLastAudio = onReplayLastAudio,
                onPauseOrStopAudio = onPauseOrStopAudio,
                onToggleMute = onToggleMute,
                onSetSpeed = onSetSpeed,
                onSetVolume = onSetVolume,
                onLeaveRoom = onLeaveRoom,
                onEndRoom = onEndRoom,
                onSetSubtitleMode = onSetSubtitleMode,
                onSetTranslationDomain = onSetTranslationDomain,
                onToggleAutoDetectSource = onToggleAutoDetectSource,
                onTogglePreferOriginalAudio = onTogglePreferOriginalAudio,
                availableVoices = availableVoices,
                onSelectVoice = onSelectVoice,
                modifier = Modifier.padding(padding)
            )
        }
    }
}

@Composable
private fun ActiveTwoPhoneRoomView(
    room: ConversationRoom,
    messages: List<ConversationMessage>,
    currentUserId: String,
    state: TranslatorUiState,
    rmsNormalized: Float,
    isSpeaking: Boolean,
    isMuted: Boolean,
    ttsSpeed: Float,
    ttsVolume: Float,
    onGrantVoiceConsent: (Boolean) -> Unit,
    onSelectLanguage: (NiloraLanguage) -> Unit,
    onStartVoice: () -> Unit,
    onStopVoice: () -> Unit,
    onUpdatePendingTranscript: (String) -> Unit,
    onConfirmPendingTranscript: () -> Unit,
    onDismissPendingTranscript: () -> Unit,
    onSendText: (String) -> Unit,
    onRepeatAudio: (String, NiloraLanguage) -> Unit,
    onReplayLastAudio: () -> Unit,
    onPauseOrStopAudio: () -> Unit,
    onToggleMute: () -> Unit,
    onSetSpeed: (Float) -> Unit,
    onSetVolume: (Float) -> Unit,
    onLeaveRoom: () -> Unit,
    onEndRoom: () -> Unit,
    onSetSubtitleMode: (SubtitleDisplayMode) -> Unit = {},
    onSetTranslationDomain: (TranslationContextDomain) -> Unit = {},
    onToggleAutoDetectSource: (Boolean) -> Unit = {},
    onTogglePreferOriginalAudio: (Boolean) -> Unit = {},
    availableVoices: List<TtsVoiceOption> = emptyList(),
    onSelectVoice: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var micDeniedMsg by remember { mutableStateOf<String?>(null) }
    var textDraft by remember { mutableStateOf("") }

    val micLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            micDeniedMsg = null
            onStartVoice()
        } else {
            micDeniedMsg = "Microphone permission denied. You can still type phrases below to translate."
        }
    }

    val isCapturing = state.pipelineStage == PipelineStage.LISTENING ||
        state.pipelineStage == PipelineStage.SPEECH_DETECTED ||
        state.pipelineStage == PipelineStage.RECOGNIZING

    Column(modifier = modifier.fillMaxSize()) {
        // Connection & Participant Status Header
        Surface(color = MaterialTheme.colorScheme.secondaryContainer, modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (room.status == "WAITING") {
                                "Waiting for partner • Code ${room.roomCode}"
                            } else {
                                "Connected: ${room.hostName} & ${room.guestName.ifBlank { "Partner" }}"
                            },
                            style = MaterialTheme.typography.labelLarge,
                            color = TurquoiseOnContainer
                        )
                        Text(
                            text = "Invite: https://nilora.app/join/${room.roomCode} • ${state.roomConnectionStatus}",
                            style = MaterialTheme.typography.labelSmall,
                            color = TurquoiseOnContainer.copy(alpha = 0.8f)
                        )
                    }
                    Row {
                        TextButton(onClick = onLeaveRoom) {
                            Text("Leave")
                        }
                        if (room.hostId == currentUserId) {
                            TextButton(onClick = onEndRoom) {
                                Text("End Room", color = ErrorCoral)
                            }
                        }
                    }
                }
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(NiloraLanguage.entries) { lang ->
                        FilterChip(
                            selected = state.myRoomLanguage == lang,
                            onClick = { onSelectLanguage(lang) },
                            label = { Text(lang.code.uppercase()) }
                        )
                    }
                }
            }
        }

        if (!state.voiceConsentGranted) {
            Box(modifier = Modifier.padding(12.dp)) {
                VoiceConsentCard(consentGranted = false, onGrantConsent = onGrantVoiceConsent)
            }
        }
        if (micDeniedMsg != null) {
            Text(micDeniedMsg.orEmpty(), color = ErrorCoral, modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp), style = MaterialTheme.typography.bodySmall)
        }
        if (state.roomError != null) {
            Text(state.roomError, color = ErrorCoral, modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp), style = MaterialTheme.typography.bodySmall)
        }

        val activeSubtitle = when {
            state.partialTranscript.isNotBlank() -> LiveSubtitleItem(
                speakerName = "You (Speaking)",
                sourceLanguageLabel = state.myRoomLanguage.code.uppercase(),
                targetLanguageLabel = "...",
                originalText = state.partialTranscript,
                translatedText = "",
                isPartial = true,
                confidenceLabel = "MEDIUM"
            )
            messages.isNotEmpty() -> {
                val latest = messages.last()
                LiveSubtitleItem(
                    speakerName = latest.senderName,
                    sourceLanguageLabel = latest.sourceLanguage.uppercase(),
                    targetLanguageLabel = latest.targetLanguage.uppercase(),
                    originalText = latest.originalText,
                    translatedText = latest.translatedText,
                    isPartial = false,
                    confidenceLabel = if (latest.confidenceNote.contains("low", ignoreCase = true)) "LOW" else "HIGH",
                    ambiguityWarning = latest.confidenceNote
                )
            }
            else -> null
        }

        Box(modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)) {
            LiveSubtitlesOverlayCard(
                subtitle = activeSubtitle,
                mode = state.subtitleDisplayMode,
                onModeChange = onSetSubtitleMode,
                onSpeakTranslation = {
                    messages.lastOrNull()?.let { latest ->
                        onRepeatAudio(latest.translatedText, NiloraLanguage.fromCode(latest.targetLanguage))
                    }
                }
            )
        }

        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (messages.isEmpty()) {
                item {
                    UnavailableDataCard(
                        title = "Ready for Real-Time Conversation",
                        explanation = "Transport uses Cloud Firestore real-time listeners (translated text + local TTS synthesis; raw P2P WebRTC audio media is not bundled). Speak or type below.",
                        icon = Icons.Filled.RecordVoiceOver
                    )
                }
            } else {
                items(messages) { msg ->
                    val isMine = msg.senderId == currentUserId
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (isMine) DeepNavy else MaterialTheme.colorScheme.surfaceVariant
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = "${msg.senderName} (${msg.sourceLanguage.uppercase()} → ${msg.targetLanguage.uppercase()})",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isMine) WarmSand else MaterialTheme.colorScheme.secondary
                            )
                            Text(
                                text = msg.originalText,
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (isMine) Color.White.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = msg.translatedText,
                                style = MaterialTheme.typography.titleMedium,
                                color = if (isMine) Color.White else MaterialTheme.colorScheme.onSurface
                            )
                            if (msg.confidenceNote.isNotBlank()) {
                                Text(msg.confidenceNote, style = MaterialTheme.typography.labelSmall, color = WarningAmber)
                            }
                            TextButton(
                                onClick = { onRepeatAudio(msg.translatedText, NiloraLanguage.fromCode(msg.targetLanguage)) },
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Icon(Icons.Filled.VolumeUp, contentDescription = "Repeat", modifier = Modifier.size(16.dp), tint = if (isMine) Turquoise else MaterialTheme.colorScheme.primary)
                                Spacer(Modifier.width(4.dp))
                                Text("Play Audio", color = if (isMine) Turquoise else MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            }
        }

        // Input, Transcript Review, Pipeline Status & Audio Controls
        Surface(tonalElevation = 4.dp) {
            Column(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                TranslationDomainAndAudioBar(
                    selectedDomain = state.translationDomain,
                    autoDetectSource = state.autoDetectSource,
                    preferOriginalAudio = state.preferOriginalAudio,
                    availableVoices = availableVoices,
                    selectedVoiceName = state.selectedTtsVoiceName,
                    onSelectDomain = onSetTranslationDomain,
                    onToggleAutoDetect = onToggleAutoDetectSource,
                    onTogglePreferOriginalAudio = onTogglePreferOriginalAudio,
                    onSelectVoice = onSelectVoice
                )
                PipelineAndVadStatusBar(
                    stage = state.pipelineStage,
                    rmsNormalized = rmsNormalized,
                    partialTranscript = state.partialTranscript,
                    pipelineError = state.pipelineError
                )
                if (state.pipelineStage == PipelineStage.AWAITING_CORRECTION) {
                    TranscriptCorrectionPanel(
                        pendingText = state.pendingEditableTranscript,
                        onTextChange = onUpdatePendingTranscript,
                        onConfirmTranslate = onConfirmPendingTranscript,
                        onDismiss = onDismissPendingTranscript
                    )
                }
                AudioPlaybackControlsRow(
                    isSpeaking = isSpeaking,
                    isMuted = isMuted,
                    ttsSpeed = ttsSpeed,
                    ttsVolume = ttsVolume,
                    onPauseOrStop = onPauseOrStopAudio,
                    onReplayLast = onReplayLastAudio,
                    onToggleMute = onToggleMute,
                    onSetSpeed = onSetSpeed,
                    onSetVolume = onSetVolume
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = textDraft,
                        onValueChange = { textDraft = it },
                        placeholder = { Text("Type or speak in ${state.myRoomLanguage.displayName}...") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = {
                            if (textDraft.isNotBlank()) {
                                onSendText(textDraft)
                                textDraft = ""
                            }
                        },
                        enabled = textDraft.isNotBlank() && state.voiceConsentGranted && state.pipelineStage != PipelineStage.TRANSLATING
                    ) {
                        Icon(Icons.Filled.Send, contentDescription = "Send")
                    }
                    FilledIconButton(
                        onClick = {
                            if (isCapturing) {
                                onStopVoice()
                            } else {
                                val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
                                if (granted) onStartVoice() else micLauncher.launch(Manifest.permission.RECORD_AUDIO)
                            }
                        },
                        enabled = state.voiceConsentGranted,
                        modifier = Modifier.testTag("room_mic_button")
                    ) {
                        Icon(if (isCapturing) Icons.Filled.Stop else Icons.Filled.Mic, contentDescription = if (isCapturing) "Stop listening" else "Speak")
                    }
                }
            }
        }
    }
}

@Composable
fun SharedPhoneTranslatorScreen(
    strings: AppStrings,
    state: TranslatorUiState,
    rmsNormalized: Float,
    isSpeaking: Boolean,
    isMuted: Boolean,
    ttsSpeed: Float,
    ttsVolume: Float,
    onGrantVoiceConsent: (Boolean) -> Unit,
    onToggleTopRotation: () -> Unit,
    onSetTopLanguage: (NiloraLanguage) -> Unit,
    onSetBottomLanguage: (NiloraLanguage) -> Unit,
    onStartVoice: (String) -> Unit,
    onStopVoice: () -> Unit,
    onUpdatePendingTranscript: (String) -> Unit,
    onConfirmPendingTranscript: () -> Unit,
    onDismissPendingTranscript: () -> Unit,
    onSendText: (String, String) -> Unit,
    onRepeatAudio: (String, NiloraLanguage) -> Unit,
    onReplayLastAudio: () -> Unit,
    onPauseOrStopAudio: () -> Unit,
    onToggleMute: () -> Unit,
    onSetSpeed: (Float) -> Unit,
    onSetVolume: (Float) -> Unit,
    onEndSession: () -> Unit,
    onBack: () -> Unit,
    onSetSubtitleMode: (SubtitleDisplayMode) -> Unit = {},
    onSetTranslationDomain: (TranslationContextDomain) -> Unit = {},
    onToggleAutoDetectSource: (Boolean) -> Unit = {},
    onTogglePreferOriginalAudio: (Boolean) -> Unit = {},
    availableVoices: List<TtsVoiceOption> = emptyList(),
    onSelectVoice: (String) -> Unit = {}
) {
    val context = LocalContext.current
    var pendingSpeakerSide by remember { mutableStateOf("BOTTOM") }
    var micDeniedMsg by remember { mutableStateOf<String?>(null) }
    var topDraft by remember { mutableStateOf("") }
    var bottomDraft by remember { mutableStateOf("") }

    val micLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            micDeniedMsg = null
            onStartVoice(pendingSpeakerSide)
        } else {
            micDeniedMsg = "Microphone permission denied. Type below or grant microphone permission in Settings."
        }
    }

    val isCapturing = state.pipelineStage == PipelineStage.LISTENING ||
        state.pipelineStage == PipelineStage.SPEECH_DETECTED ||
        state.pipelineStage == PipelineStage.RECOGNIZING

    fun triggerVoice(side: String) {
        if (isCapturing && state.activeSharedSpeaker == side) {
            onStopVoice()
            return
        }
        pendingSpeakerSide = side
        val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        if (granted) onStartVoice(side) else micLauncher.launch(Manifest.permission.RECORD_AUDIO)
    }

    val latestMsg: SharedTranslationMessage? = state.sharedMessages.lastOrNull()

    Scaffold(
        topBar = {
            NiloraTopBar(
                title = strings.onePhoneModeTitle,
                onBackClick = onBack,
                actions = {
                    IconButton(onClick = onToggleTopRotation, modifier = Modifier.testTag("shared_rotate_btn")) {
                        Icon(Icons.Filled.ScreenRotation, contentDescription = "Toggle 180 degree rotation")
                    }
                    IconButton(onClick = onToggleMute, modifier = Modifier.testTag("shared_mute_btn")) {
                        Icon(if (isMuted) Icons.Filled.VolumeOff else Icons.Filled.VolumeUp, contentDescription = strings.muteLabel)
                    }
                    TextButton(onClick = onEndSession, modifier = Modifier.testTag("shared_end_btn")) {
                        Text(strings.endSessionLabel, color = ErrorCoral)
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (!state.voiceConsentGranted) {
                VoiceConsentCard(consentGranted = false, onGrantConsent = onGrantVoiceConsent)
            }

            // TOP HALF: Rotatable 180 degrees for person standing opposite the phone
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .graphicsLayer { rotationZ = if (state.rotateTopPanel) 180f else 0f },
                color = DeepNavy,
                shape = RoundedCornerShape(22.dp)
            ) {
                SharedSpeakerPane(
                    label = "Partner Side",
                    selectedLang = state.topLanguage,
                    onSelectLang = onSetTopLanguage,
                    latestMessage = latestMsg,
                    isForTopSide = true,
                    draft = topDraft,
                    onDraftChange = { topDraft = it },
                    isListening = isCapturing && state.activeSharedSpeaker == "TOP",
                    isTranslating = state.pipelineStage == PipelineStage.TRANSLATING,
                    consentGranted = state.voiceConsentGranted,
                    onSpeakClick = { triggerVoice("TOP") },
                    onSendClick = {
                        if (topDraft.isNotBlank()) {
                            onSendText("TOP", topDraft)
                            topDraft = ""
                        }
                    },
                    onRepeatClick = {
                        latestMsg?.let { onRepeatAudio(it.translatedText, it.targetLang) }
                    },
                    darkPane = true
                )
            }

            val activeSubtitle = when {
                state.partialTranscript.isNotBlank() -> LiveSubtitleItem(
                    speakerName = if (state.activeSharedSpeaker == "TOP") "Partner Side" else "Your Side",
                    sourceLanguageLabel = (if (state.activeSharedSpeaker == "TOP") state.topLanguage else state.bottomLanguage).code.uppercase(),
                    targetLanguageLabel = (if (state.activeSharedSpeaker == "TOP") state.bottomLanguage else state.topLanguage).code.uppercase(),
                    originalText = state.partialTranscript,
                    translatedText = "",
                    isPartial = true,
                    confidenceLabel = "MEDIUM"
                )
                latestMsg != null -> LiveSubtitleItem(
                    speakerName = if (latestMsg.speakerSide == "TOP") "Partner Side" else "Your Side",
                    sourceLanguageLabel = latestMsg.sourceLang.code.uppercase(),
                    targetLanguageLabel = latestMsg.targetLang.code.uppercase(),
                    originalText = latestMsg.originalText,
                    translatedText = latestMsg.translatedText,
                    isPartial = false,
                    confidenceLabel = latestMsg.confidence,
                    ambiguityWarning = latestMsg.warning
                )
                else -> null
            }

            LiveSubtitlesOverlayCard(
                subtitle = activeSubtitle,
                mode = state.subtitleDisplayMode,
                onModeChange = onSetSubtitleMode,
                onSpeakTranslation = {
                    latestMsg?.let { onRepeatAudio(it.translatedText, it.targetLang) }
                }
            )

            // Center Pipeline Status, Editable Correction & Playback Controls
            PipelineAndVadStatusBar(
                stage = state.pipelineStage,
                rmsNormalized = rmsNormalized,
                partialTranscript = state.partialTranscript,
                pipelineError = state.pipelineError ?: micDeniedMsg
            )

            if (state.pipelineStage == PipelineStage.AWAITING_CORRECTION) {
                TranscriptCorrectionPanel(
                    pendingText = state.pendingEditableTranscript,
                    onTextChange = onUpdatePendingTranscript,
                    onConfirmTranslate = onConfirmPendingTranscript,
                    onDismiss = onDismissPendingTranscript
                )
            }

            TranslationDomainAndAudioBar(
                selectedDomain = state.translationDomain,
                autoDetectSource = state.autoDetectSource,
                preferOriginalAudio = state.preferOriginalAudio,
                availableVoices = availableVoices,
                selectedVoiceName = state.selectedTtsVoiceName,
                onSelectDomain = onSetTranslationDomain,
                onToggleAutoDetect = onToggleAutoDetectSource,
                onTogglePreferOriginalAudio = onTogglePreferOriginalAudio,
                onSelectVoice = onSelectVoice
            )

            AudioPlaybackControlsRow(
                isSpeaking = isSpeaking,
                isMuted = isMuted,
                ttsSpeed = ttsSpeed,
                ttsVolume = ttsVolume,
                onPauseOrStop = onPauseOrStopAudio,
                onReplayLast = onReplayLastAudio,
                onToggleMute = onToggleMute,
                onSetSpeed = onSetSpeed,
                onSetVolume = onSetVolume
            )

            // BOTTOM HALF: Upright for phone holder
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                color = TurquoiseContainer,
                shape = RoundedCornerShape(22.dp)
            ) {
                SharedSpeakerPane(
                    label = "Your Side",
                    selectedLang = state.bottomLanguage,
                    onSelectLang = onSetBottomLanguage,
                    latestMessage = latestMsg,
                    isForTopSide = false,
                    draft = bottomDraft,
                    onDraftChange = { bottomDraft = it },
                    isListening = isCapturing && state.activeSharedSpeaker == "BOTTOM",
                    isTranslating = state.pipelineStage == PipelineStage.TRANSLATING,
                    consentGranted = state.voiceConsentGranted,
                    onSpeakClick = { triggerVoice("BOTTOM") },
                    onSendClick = {
                        if (bottomDraft.isNotBlank()) {
                            onSendText("BOTTOM", bottomDraft)
                            bottomDraft = ""
                        }
                    },
                    onRepeatClick = {
                        latestMsg?.let { onRepeatAudio(it.translatedText, it.targetLang) }
                    },
                    darkPane = false
                )
            }
        }
    }
}

@Composable
private fun SharedSpeakerPane(
    label: String,
    selectedLang: NiloraLanguage,
    onSelectLang: (NiloraLanguage) -> Unit,
    latestMessage: SharedTranslationMessage?,
    isForTopSide: Boolean,
    draft: String,
    onDraftChange: (String) -> Unit,
    isListening: Boolean,
    isTranslating: Boolean,
    consentGranted: Boolean,
    onSpeakClick: () -> Unit,
    onSendClick: () -> Unit,
    onRepeatClick: () -> Unit,
    darkPane: Boolean
) {
    val fg = if (darkPane) Color.White else TurquoiseOnContainer
    Column(
        modifier = Modifier.fillMaxSize().padding(14.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("$label • ${selectedLang.nativeName}", style = MaterialTheme.typography.labelLarge, color = fg)
                if (latestMessage != null) {
                    IconButton(onClick = onRepeatClick, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Filled.VolumeUp, contentDescription = "Repeat translation", tint = fg)
                    }
                }
            }
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(NiloraLanguage.entries) { lang ->
                    FilterChip(
                        selected = selectedLang == lang,
                        onClick = { onSelectLang(lang) },
                        label = { Text(lang.code.uppercase()) }
                    )
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            if (isListening) {
                Text("Listening...", style = MaterialTheme.typography.titleMedium, color = if (darkPane) Turquoise else DeepNavy)
            } else if (latestMessage != null) {
                val showTranslated = (isForTopSide && latestMessage.speakerSide == "BOTTOM") || (!isForTopSide && latestMessage.speakerSide == "TOP")
                val mainText = if (showTranslated) latestMessage.translatedText else latestMessage.originalText
                Text(mainText, style = MaterialTheme.typography.titleLarge, color = fg, maxLines = 3)
                Text(
                    "Dialect: ${latestMessage.detectedDialect} • Confidence: ${latestMessage.confidence} • ${latestMessage.translationLatencyMs}ms",
                    style = MaterialTheme.typography.labelSmall,
                    color = fg.copy(alpha = 0.72f)
                )
                if (latestMessage.warning.isNotBlank()) {
                    Text(latestMessage.warning, style = MaterialTheme.typography.labelSmall, color = WarningAmber)
                }
            } else {
                Text("Tap Mic or type below to translate", style = MaterialTheme.typography.bodyMedium, color = fg.copy(alpha = 0.75f))
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = draft,
                onValueChange = onDraftChange,
                placeholder = { Text("Type...", color = fg.copy(alpha = 0.6f)) },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = fg,
                    unfocusedTextColor = fg,
                    focusedBorderColor = fg,
                    unfocusedBorderColor = fg.copy(alpha = 0.5f)
                ),
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onSendClick, enabled = draft.isNotBlank() && consentGranted && !isTranslating) {
                Icon(Icons.Filled.Send, contentDescription = "Translate text", tint = fg)
            }
            FilledIconButton(
                onClick = onSpeakClick,
                enabled = consentGranted,
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = if (darkPane) Turquoise else DeepNavy,
                    contentColor = if (darkPane) DeepNavyDark else Color.White
                )
            ) {
                Icon(if (isListening) Icons.Filled.Stop else Icons.Filled.Mic, contentDescription = if (isListening) "Stop" else "Speak")
            }
        }
    }
}
