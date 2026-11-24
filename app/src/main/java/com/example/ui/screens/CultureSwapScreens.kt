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
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.*
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.SwapDetailUiState
import com.example.ui.viewmodel.VoiceMediaDraftState

@Composable
fun CultureSwapsListScreen(
    swapsState: UiState<List<CultureSwap>>,
    currentUserId: String,
    onCreateSwap: () -> Unit,
    onOpenSwap: (String) -> Unit,
    onBack: () -> Unit
) {
    var modeFilter by remember { mutableStateOf("ALL") }

    Scaffold(
        topBar = {
            NiloraTopBar(
                title = "Culture Swap",
                subtitle = "Structured cross-cultural exchanges",
                onBackClick = onBack,
                actions = {
                    IconButton(
                        onClick = onCreateSwap,
                        modifier = Modifier.testTag("culture_swaps_create_top_btn")
                    ) {
                        Icon(Icons.Filled.AddCircle, contentDescription = "Start Culture Swap")
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onCreateSwap,
                icon = { Icon(Icons.Filled.SwapHoriz, contentDescription = null) },
                text = { Text("SWAP CULTURES") },
                containerColor = NileTurquoiseDark,
                contentColor = SoftIvory,
                modifier = Modifier.testTag("culture_swaps_fab")
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .testTag("culture_swaps_list"),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("ALL" to "All Swaps", "TWO_PERSON" to "1-on-1 Swap", "GROUP" to "Group Swap").forEach { (key, label) ->
                        FilterChip(
                            selected = modeFilter == key,
                            onClick = { modeFilter = key },
                            label = { Text(label) }
                        )
                    }
                }
            }

            when (swapsState) {
                is UiState.Loading -> item { NiloraLoadingState("Loading open Culture Swaps...") }
                is UiState.Error -> item { NiloraErrorState("Culture Swaps Unavailable", swapsState.message) }
                is UiState.Success -> {
                    val filtered = swapsState.data.filter { s ->
                        modeFilter == "ALL" || s.participationMode == modeFilter
                    }
                    if (filtered.isEmpty()) {
                        item {
                            UnavailableDataCard(
                                title = "No Open Culture Swaps Yet",
                                explanation = "Start a guided exchange: teach a local expression, swap morning routines, share a home dish, or describe a quiet neighborhood corner.",
                                icon = Icons.Filled.SwapHoriz,
                                actionLabel = "Start a Culture Swap",
                                onActionClick = onCreateSwap
                            )
                        }
                    } else {
                        items(filtered, key = { it.id }) { swap ->
                            CultureSwapCard(
                                swap = swap,
                                currentUserId = currentUserId,
                                onClick = { onOpenSwap(swap.id) }
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
fun CultureSwapCard(
    swap: CultureSwap,
    currentUserId: String,
    onClick: () -> Unit
) {
    val isParticipant = currentUserId in swap.participantIds
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("culture_swap_card_${swap.id}")
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, NileTurquoise.copy(alpha = 0.4f))
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
                Surface(color = NileTurquoiseContainer, shape = RoundedCornerShape(999.dp)) {
                    Text(
                        text = SwapPromptCategory.fromKey(swap.promptCategory).titleEn,
                        style = MaterialTheme.typography.labelSmall,
                        color = NileTurquoiseOnContainer,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
                Text(
                    text = "${swap.status} • ${swap.participantIds.size}/${swap.maxParticipants}",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (swap.status == "EXCHANGED") VerifiedEmerald else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Text(
                text = swap.promptText,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
            CountryChainRow(countries = swap.participantCountries.values.distinct())
            Text(
                text = "Started by ${swap.creatorName} (${swap.creatorCountry}) • ${swap.entriesCount} turns" +
                    if (isParticipant) " • Joined" else "",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun CreateCultureSwapScreen(
    myProfile: UserProfile?,
    mediaDraft: VoiceMediaDraftState,
    onPickPhoto: (android.content.Context, Uri?) -> Unit,
    onClearPhoto: () -> Unit,
    onStartRecording: () -> Unit,
    onStopRecording: () -> Unit,
    onClearRecording: () -> Unit,
    onPlayPreviewClip: (String) -> Unit,
    onDictateText: (String) -> Unit,
    onSubmitSwap: (
        category: SwapPromptCategory,
        promptText: String,
        participationMode: String,
        maxParticipants: Long,
        initialResponseText: String,
        expressionSample: String,
        languageCode: String,
        allowPrivateConnect: Boolean
    ) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var category by remember { mutableStateOf(SwapPromptCategory.TEACH_PHRASE) }
    var promptText by remember { mutableStateOf(SwapPromptCategory.TEACH_PHRASE.defaultPromptEn) }
    var participationMode by remember { mutableStateOf("TWO_PERSON") }
    var initialResponse by remember { mutableStateOf("") }
    var expressionSample by remember { mutableStateOf("") }
    var languageCode by remember(myProfile) {
        mutableStateOf(myProfile?.nativeLanguage?.ifBlank { myProfile.preferredLanguage } ?: "en")
    }
    var allowPrivateConnect by remember { mutableStateOf(true) }

    LaunchedEffect(mediaDraft.dictatedText) {
        if (mediaDraft.dictatedText.isNotBlank()) {
            initialResponse = if (initialResponse.isBlank()) mediaDraft.dictatedText else "$initialResponse ${mediaDraft.dictatedText}"
        }
    }

    Scaffold(
        topBar = {
            NiloraTopBar(
                title = "Start a Culture Swap",
                subtitle = "Exchange voices, phrases, or daily traditions",
                onBackClick = onBack
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .testTag("create_swap_form"),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text("Choose an Exchange Prompt", style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(6.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(SwapPromptCategory.entries) { cat ->
                        FilterChip(
                            selected = category == cat,
                            onClick = {
                                category = cat
                                promptText = cat.defaultPromptEn
                            },
                            label = { Text(cat.titleEn) }
                        )
                    }
                }
            }

            item {
                OutlinedTextField(
                    value = promptText,
                    onValueChange = { promptText = it },
                    label = { Text("Swap Prompt Question") },
                    minLines = 2,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("create_swap_prompt_input")
                )
            }

            item {
                Text("Participation Format", style = MaterialTheme.typography.titleSmall)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = participationMode == "TWO_PERSON",
                        onClick = { participationMode = "TWO_PERSON" },
                        label = { Text("Two-Person Swap (1:1)") }
                    )
                    FilterChip(
                        selected = participationMode == "GROUP",
                        onClick = { participationMode = "GROUP" },
                        label = { Text("Group Swap (up to 8)") }
                    )
                }
            }

            item {
                OutlinedTextField(
                    value = initialResponse,
                    onValueChange = { initialResponse = it },
                    label = { Text("Your Opening Side of the Swap") },
                    placeholder = { Text("Share your own phrase, morning custom, or photo first...") },
                    minLines = 3,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("create_swap_response_input")
                )
            }

            item {
                OutlinedTextField(
                    value = expressionSample,
                    onValueChange = { expressionSample = it },
                    label = { Text("Featured Phrase or Dish Name (Optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item {
                Text("Your Language", style = MaterialTheme.typography.labelMedium)
                TargetLanguageQuickSelector(selectedCode = languageCode, onSelectCode = { languageCode = it })
                Spacer(Modifier.height(8.dp))
                ExperienceMediaAttachmentBar(
                    draft = mediaDraft,
                    languageCode = languageCode,
                    onPickPhotoUri = { uri -> onPickPhoto(context, uri) },
                    onClearPhoto = onClearPhoto,
                    onStartRecording = onStartRecording,
                    onStopRecording = onStopRecording,
                    onClearRecording = onClearRecording,
                    onPlayPreviewClip = onPlayPreviewClip,
                    onDictateText = { onDictateText(languageCode) }
                )
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Allow partners to Continue Privately after swap", style = MaterialTheme.typography.bodyMedium)
                    Switch(checked = allowPrivateConnect, onCheckedChange = { allowPrivateConnect = it })
                }
            }

            item {
                Button(
                    onClick = {
                        onSubmitSwap(
                            category,
                            promptText,
                            participationMode,
                            if (participationMode == "GROUP") 8L else 2L,
                            initialResponse,
                            expressionSample,
                            languageCode,
                            allowPrivateConnect
                        )
                    },
                    enabled = promptText.trim().length >= 3 && initialResponse.trim().length >= 2,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("submit_culture_swap_btn")
                ) {
                    Icon(Icons.Filled.SwapHoriz, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Open Culture Swap")
                }
            }
        }
    }
}

@Composable
fun CultureSwapDetailScreen(
    detailState: SwapDetailUiState,
    mediaDraft: VoiceMediaDraftState,
    currentUserId: String,
    myProfile: UserProfile?,
    onSetTargetLanguage: (String) -> Unit,
    onTranslateEntry: (String, CultureSwapEntry) -> Unit,
    onPlayAudio: (String, String) -> Unit,
    onSpeakText: (String, String) -> Unit,
    onPickPhoto: (android.content.Context, Uri?) -> Unit,
    onClearPhoto: () -> Unit,
    onStartRecording: () -> Unit,
    onStopRecording: () -> Unit,
    onClearRecording: () -> Unit,
    onDictateText: (String) -> Unit,
    onSubmitEntry: (CultureSwap, String, String, String, String) -> Unit,
    onSaveToMemory: (CultureSwap, List<CultureSwapEntry>, MemoryVisibility) -> Unit,
    onContinuePrivately: (partnerId: String, partnerName: String, partnerCountry: String, partnerLanguage: String, title: String) -> Unit,
    onBlockUser: (userId: String, userName: String) -> Unit,
    onReportSwap: (targetId: String, reason: String) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val swap = detailState.swap
    var myTurnText by remember { mutableStateOf("") }
    var myPhrase by remember { mutableStateOf("") }
    var myLangCode by remember(myProfile) {
        mutableStateOf(myProfile?.nativeLanguage?.ifBlank { myProfile.preferredLanguage } ?: "en")
    }
    var showMemoryDialog by remember { mutableStateOf(false) }

    LaunchedEffect(mediaDraft.dictatedText) {
        if (mediaDraft.dictatedText.isNotBlank()) {
            myTurnText = if (myTurnText.isBlank()) mediaDraft.dictatedText else "$myTurnText ${mediaDraft.dictatedText}"
        }
    }

    Scaffold(
        topBar = {
            NiloraTopBar(
                title = swap?.let { SwapPromptCategory.fromKey(it.promptCategory).titleEn } ?: "Culture Swap",
                subtitle = swap?.let { "${it.status} • ${it.entriesCount} exchanges" },
                onBackClick = onBack,
                actions = {
                    if (swap != null) {
                        IconButton(
                            onClick = { showMemoryDialog = true },
                            modifier = Modifier.testTag("swap_save_memory_btn")
                        ) {
                            Icon(Icons.Filled.Bookmarks, contentDescription = "Save Culture Swap to Memory")
                        }
                    }
                }
            )
        }
    ) { padding ->
        if (swap == null) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                NiloraLoadingState("Loading Culture Swap...")
            }
            return@Scaffold
        }

        val canContribute = currentUserId in swap.participantIds || swap.participantIds.size < swap.maxParticipants

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .testTag("culture_swap_detail_list"),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (detailState.errorBanner != null) {
                item { NiloraErrorState("Notice", detailState.errorBanner) }
            }

            item {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = NileTurquoiseContainer.copy(alpha = 0.45f))
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
                            Text(
                                text = "${swap.participationMode.replace("_", " ")} • ${swap.participantIds.size}/${swap.maxParticipants} members",
                                style = MaterialTheme.typography.labelMedium,
                                color = NileTurquoiseDark,
                                fontWeight = FontWeight.Bold
                            )
                            if (swap.creatorId != currentUserId) {
                                Row {
                                    IconButton(onClick = { onReportSwap(swap.id, "Inappropriate Culture Swap") }) {
                                        Icon(Icons.Filled.Flag, contentDescription = "Report Culture Swap")
                                    }
                                    IconButton(onClick = { onBlockUser(swap.creatorId, swap.creatorName) }) {
                                        Icon(Icons.Filled.Block, contentDescription = "Block creator")
                                    }
                                }
                            }
                        }
                        Text(swap.promptText, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        CountryChainRow(countries = swap.participantCountries.values.distinct())
                    }
                }
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Translate Turns Into:", style = MaterialTheme.typography.labelMedium)
                    TargetLanguageQuickSelector(
                        selectedCode = detailState.targetLanguageCode,
                        onSelectCode = onSetTargetLanguage
                    )
                }
            }

            items(detailState.entries, key = { it.id }) { entry ->
                Card(shape = RoundedCornerShape(14.dp)) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${entry.authorName} (${entry.country})",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            if (entry.authorId != currentUserId && swap.allowPrivateConnect) {
                                TextButton(
                                    onClick = {
                                        onContinuePrivately(
                                            entry.authorId,
                                            entry.authorName,
                                            entry.country,
                                            entry.languageCode,
                                            SwapPromptCategory.fromKey(swap.promptCategory).titleEn
                                        )
                                    },
                                    modifier = Modifier.testTag("swap_continue_privately_${entry.id}")
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Continue Privately")
                                }
                            }
                        }

                        if (entry.photoBase64.isNotBlank()) {
                            Base64PhotoPreview(photoBase64 = entry.photoBase64, contentDescription = "Swap photo")
                        }
                        if (entry.audioBase64.isNotBlank()) {
                            VoiceAudioPlayerChip(
                                clipId = "sw_${entry.id}",
                                durationMs = entry.audioDurationMs,
                                isPlaying = mediaDraft.playingAudioId == "sw_${entry.id}",
                                onTogglePlay = { onPlayAudio("sw_${entry.id}", entry.audioBase64) }
                            )
                        }

                        InlineTranslationBox(
                            originalText = entry.contentText,
                            sourceLanguageCode = entry.languageCode,
                            translations = entry.translations,
                            translationNote = entry.translationNote,
                            targetLanguageCode = detailState.targetLanguageCode,
                            isTranslating = detailState.isTranslating,
                            onTranslateClick = { onTranslateEntry(swap.id, entry) },
                            onSpeakOriginal = { onSpeakText(entry.contentText, entry.languageCode) },
                            onSpeakTranslation = { t -> onSpeakText(t, detailState.targetLanguageCode) },
                            testTagPrefix = "swap_entry_${entry.id}"
                        )

                        if (entry.expressionSample.isNotBlank()) {
                            Text(
                                text = "Shared phrase: \"${entry.expressionSample}\"",
                                style = MaterialTheme.typography.labelLarge,
                                color = NileTurquoiseDark
                            )
                        }
                    }
                }
            }

            if (canContribute) {
                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f))
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text("Share Your Side of the Swap", style = MaterialTheme.typography.titleMedium)
                            OutlinedTextField(
                                value = myTurnText,
                                onValueChange = { myTurnText = it },
                                label = { Text("Your cultural response") },
                                minLines = 2,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("swap_turn_input")
                            )
                            OutlinedTextField(
                                value = myPhrase,
                                onValueChange = { myPhrase = it },
                                label = { Text("Local expression or title (Optional)") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            TargetLanguageQuickSelector(selectedCode = myLangCode, onSelectCode = { myLangCode = it })
                            ExperienceMediaAttachmentBar(
                                draft = mediaDraft,
                                languageCode = myLangCode,
                                onPickPhotoUri = { uri -> onPickPhoto(context, uri) },
                                onClearPhoto = onClearPhoto,
                                onStartRecording = onStartRecording,
                                onStopRecording = onStopRecording,
                                onClearRecording = onClearRecording,
                                onPlayPreviewClip = { b64 -> onPlayAudio("draft_preview", b64) },
                                onDictateText = { onDictateText(myLangCode) }
                            )
                            Button(
                                onClick = {
                                    onSubmitEntry(
                                        swap,
                                        myTurnText,
                                        myPhrase,
                                        myProfile?.country?.ifBlank { "Egypt" } ?: "Egypt",
                                        myLangCode
                                    )
                                    myTurnText = ""
                                    myPhrase = ""
                                },
                                enabled = myTurnText.isNotBlank() && !detailState.isSubmitting,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("swap_submit_turn_btn")
                            ) {
                                Text(if (detailState.isSubmitting) "Submitting..." else "Submit My Swap")
                            }
                        }
                    }
                }
            }
        }
    }

    if (showMemoryDialog && swap != null) {
        var selectedVisibility by remember { mutableStateOf(MemoryVisibility.SHARED_WITH_PARTICIPANTS) }
        AlertDialog(
            onDismissRequest = { showMemoryDialog = false },
            title = { Text("Save Culture Swap to NILORA Memory") },
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
                    onSaveToMemory(swap, detailState.entries, selectedVisibility)
                }) { Text("Save Memory") }
            },
            dismissButton = { TextButton(onClick = { showMemoryDialog = false }) { Text("Cancel") } }
        )
    }
}
