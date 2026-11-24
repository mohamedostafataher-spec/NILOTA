package com.example.ui.screens

import android.net.Uri
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.*
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.VoiceMediaDraftState
import java.text.DateFormat

@Composable
fun NiloraMemoryScreen(
    memoriesState: UiState<List<NiloraMemory>>,
    currentUserId: String,
    myProfile: UserProfile?,
    mediaDraft: VoiceMediaDraftState,
    onPickPhoto: (android.content.Context, Uri?) -> Unit,
    onClearPhoto: () -> Unit,
    onStartRecording: () -> Unit,
    onStopRecording: () -> Unit,
    onClearRecording: () -> Unit,
    onPlayAudio: (String, String) -> Unit,
    onDictateText: (String) -> Unit,
    onCreatePersonalMemory: (
        title: String,
        summaryText: String,
        country: String,
        placeLabel: String,
        languageCode: String,
        retainAudioConsent: Boolean,
        visibility: MemoryVisibility
    ) -> Unit,
    onUpdateVisibility: (memoryId: String, visibility: MemoryVisibility) -> Unit,
    onRevokeAudio: (memoryId: String) -> Unit,
    onDeleteMemory: (memoryId: String) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var typeFilter by remember { mutableStateOf("ALL") }
    var showCreateDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            NiloraTopBar(
                title = "NILORA Memory",
                subtitle = "Your cultural moments, voices & friendships",
                onBackClick = onBack,
                actions = {
                    IconButton(
                        onClick = { showCreateDialog = true },
                        modifier = Modifier.testTag("nilora_memory_add_btn")
                    ) {
                        Icon(Icons.Filled.AddPhotoAlternate, contentDescription = "Save New Personal Memory")
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showCreateDialog = true },
                icon = { Icon(Icons.Filled.AutoStories, contentDescription = null) },
                text = { Text("Capture Memory") },
                containerColor = MidnightNavy,
                contentColor = SoftIvory,
                modifier = Modifier.testTag("nilora_memory_fab")
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .testTag("nilora_memory_list"),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text(
                        text = "Every NILORA Memory is Private by default unless you choose Shared with Participants or Public. Stored audio clips require your explicit consent and can be permanently wiped anytime.",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        FilterChip(
                            selected = typeFilter == "ALL",
                            onClick = { typeFilter = "ALL" },
                            label = { Text("All Memories") }
                        )
                    }
                    items(MemoryType.entries) { mType ->
                        FilterChip(
                            selected = typeFilter == mType.key,
                            onClick = { typeFilter = mType.key },
                            label = { Text(mType.labelEn) }
                        )
                    }
                }
            }

            when (memoriesState) {
                is UiState.Loading -> item { NiloraLoadingState("Loading your NILORA Memories...") }
                is UiState.Error -> item { NiloraErrorState("NILORA Memory Unavailable", memoriesState.message) }
                is UiState.Success -> {
                    val filtered = memoriesState.data.filter { mem ->
                        typeFilter == "ALL" || mem.memoryType == typeFilter
                    }
                    if (filtered.isEmpty()) {
                        item {
                            UnavailableDataCard(
                                title = "No Saved Memories Here Yet",
                                explanation = "Save a meaningful World Postcard, Culture Swap, completed World Quest, or capture a personal voice & story memory directly.",
                                icon = Icons.Filled.AutoStories,
                                actionLabel = "Capture First Memory",
                                onActionClick = { showCreateDialog = true }
                            )
                        }
                    } else {
                        items(filtered, key = { it.id }) { mem ->
                            NiloraMemoryCard(
                                memory = mem,
                                isOwner = mem.ownerId == currentUserId,
                                isPlayingAudio = mediaDraft.playingAudioId == "mem_${mem.id}",
                                onTogglePlayAudio = { onPlayAudio("mem_${mem.id}", mem.audioBase64) },
                                onChangeVisibility = { newVis -> onUpdateVisibility(mem.id, newVis) },
                                onRevokeAudio = { onRevokeAudio(mem.id) },
                                onDelete = { onDeleteMemory(mem.id) }
                            )
                        }
                    }
                }
            }
            item { Spacer(Modifier.height(64.dp)) }
        }
    }

    if (showCreateDialog) {
        var title by remember { mutableStateOf("") }
        var summary by remember { mutableStateOf("") }
        var placeLabel by remember { mutableStateOf(myProfile?.city.orEmpty()) }
        var langCode by remember(myProfile) {
            mutableStateOf(myProfile?.nativeLanguage?.ifBlank { myProfile.preferredLanguage } ?: "en")
        }
        var retainAudioConsent by remember { mutableStateOf(false) }
        var visibility by remember { mutableStateOf(MemoryVisibility.PRIVATE) }

        LaunchedEffect(mediaDraft.dictatedText) {
            if (mediaDraft.dictatedText.isNotBlank()) {
                summary = if (summary.isBlank()) mediaDraft.dictatedText else "$summary ${mediaDraft.dictatedText}"
            }
        }

        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = { Text("Capture Personal Memory") },
            text = {
                LazyColumn(
                    modifier = Modifier.heightIn(max = 460.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item {
                        OutlinedTextField(
                            value = title,
                            onValueChange = { title = it },
                            label = { Text("Memory Title") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("create_memory_title_input")
                        )
                    }
                    item {
                        OutlinedTextField(
                            value = summary,
                            onValueChange = { summary = it },
                            label = { Text("What happened or what did you learn?") },
                            minLines = 2,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("create_memory_summary_input")
                        )
                    }
                    item {
                        OutlinedTextField(
                            value = placeLabel,
                            onValueChange = { placeLabel = it },
                            label = { Text("City / Cultural Context (Optional)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    item {
                        ExperienceMediaAttachmentBar(
                            draft = mediaDraft,
                            languageCode = langCode,
                            onPickPhotoUri = { uri -> onPickPhoto(context, uri) },
                            onClearPhoto = onClearPhoto,
                            onStartRecording = onStartRecording,
                            onStopRecording = onStopRecording,
                            onClearRecording = onClearRecording,
                            onPlayPreviewClip = { b64 -> onPlayAudio("draft_preview", b64) },
                            onDictateText = { onDictateText(langCode) }
                        )
                    }
                    if (mediaDraft.recordedClip != null) {
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Consent to retain voice audio",
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.weight(1f)
                                )
                                Switch(
                                    checked = retainAudioConsent,
                                    onCheckedChange = { retainAudioConsent = it },
                                    modifier = Modifier.testTag("memory_audio_consent_switch")
                                )
                            }
                        }
                    }
                    item {
                        Text("Visibility", style = MaterialTheme.typography.labelMedium)
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            MemoryVisibility.entries.forEach { vis ->
                                FilterChip(
                                    selected = visibility == vis,
                                    onClick = { visibility = vis },
                                    label = {
                                        Text(
                                            when (vis) {
                                                MemoryVisibility.PRIVATE -> "Private"
                                                MemoryVisibility.SHARED_WITH_PARTICIPANTS -> "Shared"
                                                MemoryVisibility.PUBLIC -> "Public"
                                            }
                                        )
                                    }
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onCreatePersonalMemory(
                            title,
                            summary,
                            myProfile?.country?.ifBlank { "Egypt" } ?: "Egypt",
                            placeLabel,
                            langCode,
                            retainAudioConsent,
                            visibility
                        )
                        showCreateDialog = false
                    },
                    enabled = title.trim().length >= 2 && summary.trim().length >= 3,
                    modifier = Modifier.testTag("save_personal_memory_btn")
                ) { Text("Save Memory") }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
fun NiloraMemoryCard(
    memory: NiloraMemory,
    isOwner: Boolean,
    isPlayingAudio: Boolean,
    onTogglePlayAudio: () -> Unit,
    onChangeVisibility: (MemoryVisibility) -> Unit,
    onRevokeAudio: () -> Unit,
    onDelete: () -> Unit
) {
    val vis = MemoryVisibility.fromKey(memory.visibility)
    val dateLabel = remember(memory.createdAt) {
        memory.createdAt?.toDate()?.let { DateFormat.getDateInstance(DateFormat.MEDIUM).format(it) } ?: "Today"
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("memory_card_${memory.id}"),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, EgyptianGold.copy(alpha = 0.42f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(color = EgyptianGoldContainer, shape = RoundedCornerShape(999.dp)) {
                    Text(
                        text = "${MemoryType.fromKey(memory.memoryType).labelEn} • $dateLabel",
                        style = MaterialTheme.typography.labelSmall,
                        color = EgyptianGoldOnContainer,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
                Text(
                    text = vis.labelEn,
                    style = MaterialTheme.typography.labelSmall,
                    color = NileTurquoiseDark,
                    fontWeight = FontWeight.SemiBold
                )
            }

            if (memory.photoBase64.isNotBlank()) {
                Base64PhotoPreview(photoBase64 = memory.photoBase64, contentDescription = memory.title)
            }

            Text(memory.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(memory.summaryText, style = MaterialTheme.typography.bodyMedium)

            if (memory.placeLabel.isNotBlank()) {
                Text(
                    text = "Context: ${memory.placeLabel}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (memory.partnerNames.isNotEmpty()) {
                Text(
                    text = "With: ${memory.partnerNames.joinToString(", ")}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary
                )
            }

            CountryChainRow(countries = memory.countries)

            if (memory.audioBase64.isNotBlank()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    VoiceAudioPlayerChip(
                        clipId = "mem_${memory.id}",
                        durationMs = memory.audioDurationMs,
                        isPlaying = isPlayingAudio,
                        onTogglePlay = onTogglePlayAudio
                    )
                    if (isOwner) {
                        TextButton(
                            onClick = onRevokeAudio,
                            modifier = Modifier.testTag("memory_wipe_audio_${memory.id}")
                        ) {
                            Text("Wipe Audio", color = ErrorCoral)
                        }
                    }
                }
            }

            if (isOwner) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        MemoryVisibility.entries.forEach { option ->
                            FilterChip(
                                selected = vis == option,
                                onClick = { onChangeVisibility(option) },
                                label = {
                                    Text(
                                        when (option) {
                                            MemoryVisibility.PRIVATE -> "Private"
                                            MemoryVisibility.SHARED_WITH_PARTICIPANTS -> "Shared"
                                            MemoryVisibility.PUBLIC -> "Public"
                                        }
                                    )
                                }
                            )
                        }
                    }
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.testTag("memory_delete_${memory.id}")
                    ) {
                        Icon(Icons.Filled.DeleteOutline, contentDescription = "Delete memory", tint = ErrorCoral)
                    }
                }
            }
        }
    }
}
