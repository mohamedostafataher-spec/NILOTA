package com.example.ui.screens

import android.net.Uri
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.*
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.QuestDetailUiState
import com.example.ui.viewmodel.VoiceMediaDraftState

@Composable
fun WorldQuestsListScreen(
    questsState: UiState<List<WorldQuest>>,
    currentUserId: String,
    onCreateQuest: () -> Unit,
    onOpenQuest: (String) -> Unit,
    onBack: () -> Unit
) {
    var statusFilter by remember { mutableStateOf("ALL") }

    Scaffold(
        topBar = {
            NiloraTopBar(
                title = "World Quests",
                subtitle = "Shared cross-cultural missions",
                onBackClick = onBack,
                actions = {
                    IconButton(
                        onClick = onCreateQuest,
                        modifier = Modifier.testTag("world_quests_create_top_btn")
                    ) {
                        Icon(Icons.Filled.AddCircle, contentDescription = "Start a World Quest")
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onCreateQuest,
                icon = { Icon(Icons.Filled.EmojiEvents, contentDescription = null) },
                text = { Text("START A WORLD QUEST") },
                containerColor = EgyptianGoldDark,
                contentColor = SoftIvory,
                modifier = Modifier.testTag("world_quests_fab")
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .testTag("world_quests_list"),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("ALL" to "All Quests", "ACTIVE" to "Active", "COMPLETED" to "Completed").forEach { (key, label) ->
                        FilterChip(
                            selected = statusFilter == key,
                            onClick = { statusFilter = key },
                            label = { Text(label) }
                        )
                    }
                }
            }

            when (questsState) {
                is UiState.Loading -> item { NiloraLoadingState("Loading World Quests...") }
                is UiState.Error -> item { NiloraErrorState("World Quests Unavailable", questsState.message) }
                is UiState.Success -> {
                    val filtered = questsState.data.filter { q ->
                        statusFilter == "ALL" || q.status == statusFilter
                    }
                    if (filtered.isEmpty()) {
                        item {
                            UnavailableDataCard(
                                title = "No World Quests Available Yet",
                                explanation = "Launch a collaborative mission like '5 Cultural Similarities' or '3 Everyday Expressions'. Progress advances only from genuine member contributions.",
                                icon = Icons.Filled.Flag,
                                actionLabel = "Start a World Quest",
                                onActionClick = onCreateQuest
                            )
                        }
                    } else {
                        items(filtered, key = { it.id }) { quest ->
                            WorldQuestCard(
                                quest = quest,
                                isJoined = currentUserId in quest.participantIds,
                                onClick = { onOpenQuest(quest.id) }
                            )
                        }
                    }
                }
            }
            item { Spacer(Modifier.height(64.dp)) }
        }
    }
}

@Composable
fun WorldQuestCard(
    quest: WorldQuest,
    isJoined: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("world_quest_card_${quest.id}")
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(
            1.dp,
            if (quest.status == "COMPLETED") VerifiedEmerald.copy(alpha = 0.55f)
            else EgyptianGold.copy(alpha = 0.45f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = if (quest.status == "COMPLETED") VerifiedEmeraldContainer else EgyptianGoldContainer,
                    shape = RoundedCornerShape(999.dp)
                ) {
                    Text(
                        text = if (quest.status == "COMPLETED") "COMPLETED" else QuestMissionType.fromKey(quest.missionType).titleEn,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (quest.status == "COMPLETED") VerifiedEmerald else EgyptianGoldOnContainer,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
                Text(
                    text = "${quest.contributionsCount} / ${quest.targetGoal} contributions",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Text(quest.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(quest.objective, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

            LinearProgressIndicator(
                progress = { quest.progressFraction() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp),
                color = if (quest.status == "COMPLETED") VerifiedEmerald else NileTurquoiseDark
            )

            CountryChainRow(countries = quest.participantCountries)

            Text(
                text = "Hosted by ${quest.creatorName} • ${quest.participantIds.size} participant(s)" +
                    if (isJoined) " • You joined" else "",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun CreateWorldQuestScreen(
    onSubmitQuest: (
        missionType: QuestMissionType,
        title: String,
        objective: String,
        targetGoal: Long
    ) -> Unit,
    onBack: () -> Unit
) {
    var missionType by remember { mutableStateOf(QuestMissionType.THREE_EXPRESSIONS) }
    var title by remember { mutableStateOf(QuestMissionType.THREE_EXPRESSIONS.titleEn) }
    var objective by remember { mutableStateOf(QuestMissionType.THREE_EXPRESSIONS.defaultObjectiveEn) }
    var goalText by remember { mutableStateOf(QuestMissionType.THREE_EXPRESSIONS.suggestedGoal.toString()) }

    val parsedGoal = goalText.toLongOrNull()?.coerceIn(2L, 50L) ?: 3L

    Scaffold(
        topBar = {
            NiloraTopBar(
                title = "Start a World Quest",
                subtitle = "Collaborative cross-border goal",
                onBackClick = onBack
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .testTag("create_quest_form"),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text("Select Quest Blueprint", style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(6.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(QuestMissionType.entries) { type ->
                        FilterChip(
                            selected = missionType == type,
                            onClick = {
                                missionType = type
                                title = type.titleEn
                                objective = type.defaultObjectiveEn
                                goalText = type.suggestedGoal.toString()
                            },
                            label = { Text(type.titleEn) }
                        )
                    }
                }
            }

            item {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Quest Title") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("create_quest_title_input")
                )
            }

            item {
                OutlinedTextField(
                    value = objective,
                    onValueChange = { objective = it },
                    label = { Text("Shared Objective") },
                    minLines = 3,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("create_quest_objective_input")
                )
            }

            item {
                OutlinedTextField(
                    value = goalText,
                    onValueChange = { ch -> goalText = ch.filter { it.isDigit() }.take(2) },
                    label = { Text("Required Genuine Contributions (2 - 50)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "A World Quest only transitions to COMPLETED once real members submit $parsedGoal authentic contributions.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            item {
                Button(
                    onClick = { onSubmitQuest(missionType, title, objective, parsedGoal) },
                    enabled = title.trim().length >= 3 && objective.trim().length >= 6,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("submit_world_quest_btn")
                ) {
                    Icon(Icons.Filled.EmojiEvents, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Launch World Quest")
                }
            }
        }
    }
}

@Composable
fun WorldQuestDetailScreen(
    detailState: QuestDetailUiState,
    mediaDraft: VoiceMediaDraftState,
    currentUserId: String,
    myProfile: UserProfile?,
    onSetTargetLanguage: (String) -> Unit,
    onTranslateContribution: (String, WorldQuestContribution) -> Unit,
    onPlayAudio: (String, String) -> Unit,
    onSpeakText: (String, String) -> Unit,
    onPickPhoto: (android.content.Context, Uri?) -> Unit,
    onClearPhoto: () -> Unit,
    onStartRecording: () -> Unit,
    onStopRecording: () -> Unit,
    onClearRecording: () -> Unit,
    onDictateText: (String) -> Unit,
    onSubmitContribution: (WorldQuest, String, String, String) -> Unit,
    onSaveSharedMemory: (WorldQuest, List<WorldQuestContribution>, MemoryVisibility) -> Unit,
    onBlockUser: (userId: String, userName: String) -> Unit,
    onReportQuest: (targetId: String, reason: String) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val quest = detailState.quest
    var contribText by remember { mutableStateOf("") }
    var contribLangCode by remember(myProfile) {
        mutableStateOf(myProfile?.nativeLanguage?.ifBlank { myProfile.preferredLanguage } ?: "en")
    }
    var showMemoryDialog by remember { mutableStateOf(false) }

    LaunchedEffect(mediaDraft.dictatedText) {
        if (mediaDraft.dictatedText.isNotBlank()) {
            contribText = if (contribText.isBlank()) mediaDraft.dictatedText else "$contribText ${mediaDraft.dictatedText}"
        }
    }

    Scaffold(
        topBar = {
            NiloraTopBar(
                title = quest?.title?.ifBlank { "World Quest" } ?: "World Quest",
                subtitle = quest?.let { "${it.contributionsCount}/${it.targetGoal} • ${it.status}" },
                onBackClick = onBack,
                actions = {
                    if (quest != null) {
                        IconButton(
                            onClick = { showMemoryDialog = true },
                            modifier = Modifier.testTag("quest_save_memory_btn")
                        ) {
                            Icon(Icons.Filled.Bookmarks, contentDescription = "Save Quest to NILORA Memory")
                        }
                    }
                }
            )
        }
    ) { padding ->
        if (quest == null) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                NiloraLoadingState("Loading World Quest...")
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .testTag("world_quest_detail_list"),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (detailState.errorBanner != null) {
                item { NiloraErrorState("Notice", detailState.errorBanner) }
            }

            item {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (quest.isGoalMet()) VerifiedEmeraldContainer.copy(alpha = 0.55f)
                        else EgyptianGoldContainer.copy(alpha = 0.55f)
                    )
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
                            Text(
                                text = if (quest.isGoalMet()) "Goal Reached — Shared Memory Ready!" else "Quest In Progress",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (quest.isGoalMet()) VerifiedEmerald else EgyptianGoldOnContainer
                            )
                            if (quest.creatorId != currentUserId) {
                                Row {
                                    IconButton(onClick = { onReportQuest(quest.id, "Inappropriate World Quest") }) {
                                        Icon(Icons.Filled.Flag, contentDescription = "Report quest")
                                    }
                                    IconButton(onClick = { onBlockUser(quest.creatorId, quest.creatorName) }) {
                                        Icon(Icons.Filled.Block, contentDescription = "Block creator")
                                    }
                                }
                            }
                        }

                        Text(quest.objective, style = MaterialTheme.typography.bodyMedium)

                        LinearProgressIndicator(
                            progress = { quest.progressFraction() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(10.dp),
                            color = if (quest.isGoalMet()) VerifiedEmerald else NileTurquoiseDark
                        )
                        Text(
                            text = "${quest.contributionsCount} of ${quest.targetGoal} real contributions • ${quest.participantCountries.size} countries",
                            style = MaterialTheme.typography.labelMedium
                        )
                        CountryChainRow(countries = quest.participantCountries)

                        if (quest.isGoalMet()) {
                            Button(
                                onClick = { showMemoryDialog = true },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("quest_create_shared_memory_btn")
                            ) {
                                Icon(Icons.Filled.AutoAwesome, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text("Save Completed Quest as Shared Memory")
                            }
                        }
                    }
                }
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Translate Contributions Into:", style = MaterialTheme.typography.labelMedium)
                    TargetLanguageQuickSelector(
                        selectedCode = detailState.targetLanguageCode,
                        onSelectCode = onSetTargetLanguage
                    )
                }
            }

            items(detailState.contributions, key = { it.id }) { contrib ->
                Card(shape = RoundedCornerShape(14.dp)) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "${contrib.authorName} • ${contrib.country}",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        if (contrib.photoBase64.isNotBlank()) {
                            Base64PhotoPreview(photoBase64 = contrib.photoBase64, contentDescription = "Quest contribution photo")
                        }
                        if (contrib.audioBase64.isNotBlank()) {
                            VoiceAudioPlayerChip(
                                clipId = "qc_${contrib.id}",
                                durationMs = contrib.audioDurationMs,
                                isPlaying = mediaDraft.playingAudioId == "qc_${contrib.id}",
                                onTogglePlay = { onPlayAudio("qc_${contrib.id}", contrib.audioBase64) }
                            )
                        }
                        InlineTranslationBox(
                            originalText = contrib.contentText,
                            sourceLanguageCode = contrib.languageCode,
                            translations = contrib.translations,
                            translationNote = contrib.translationNote,
                            targetLanguageCode = detailState.targetLanguageCode,
                            isTranslating = detailState.isTranslating,
                            onTranslateClick = { onTranslateContribution(quest.id, contrib) },
                            onSpeakOriginal = { onSpeakText(contrib.contentText, contrib.languageCode) },
                            onSpeakTranslation = { t -> onSpeakText(t, detailState.targetLanguageCode) },
                            testTagPrefix = "quest_contrib_${contrib.id}"
                        )
                    }
                }
            }

            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f))
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text("Contribute to This Quest", style = MaterialTheme.typography.titleMedium)
                        OutlinedTextField(
                            value = contribText,
                            onValueChange = { contribText = it },
                            label = { Text("Add your real cultural contribution") },
                            minLines = 2,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("quest_contrib_input")
                        )
                        TargetLanguageQuickSelector(selectedCode = contribLangCode, onSelectCode = { contribLangCode = it })
                        ExperienceMediaAttachmentBar(
                            draft = mediaDraft,
                            languageCode = contribLangCode,
                            onPickPhotoUri = { uri -> onPickPhoto(context, uri) },
                            onClearPhoto = onClearPhoto,
                            onStartRecording = onStartRecording,
                            onStopRecording = onStopRecording,
                            onClearRecording = onClearRecording,
                            onPlayPreviewClip = { b64 -> onPlayAudio("draft_preview", b64) },
                            onDictateText = { onDictateText(contribLangCode) }
                        )
                        Button(
                            onClick = {
                                onSubmitContribution(
                                    quest,
                                    contribText,
                                    myProfile?.country?.ifBlank { "Egypt" } ?: "Egypt",
                                    contribLangCode
                                )
                                contribText = ""
                            },
                            enabled = contribText.isNotBlank() && !detailState.isSubmitting,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("quest_submit_contrib_btn")
                        ) {
                            Text(if (detailState.isSubmitting) "Contributing..." else "Submit Quest Contribution")
                        }
                    }
                }
            }
        }
    }

    if (showMemoryDialog && quest != null) {
        var selectedVisibility by remember { mutableStateOf(MemoryVisibility.SHARED_WITH_PARTICIPANTS) }
        AlertDialog(
            onDismissRequest = { showMemoryDialog = false },
            title = { Text("Save Quest to NILORA Memory") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    MemoryVisibility.entries.forEach { vis ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedVisibility = vis },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(selected = selectedVisibility == vis, onClick = { selectedVisibility = vis })
                            Text(vis.labelEn, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    showMemoryDialog = false
                    onSaveSharedMemory(quest, detailState.contributions, selectedVisibility)
                }) { Text("Save Memory") }
            },
            dismissButton = { TextButton(onClick = { showMemoryDialog = false }) { Text("Cancel") } }
        )
    }
}
