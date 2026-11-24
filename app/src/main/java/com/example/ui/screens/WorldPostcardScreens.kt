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
import com.example.ui.viewmodel.PostcardDetailUiState
import com.example.ui.viewmodel.VoiceMediaDraftState

@Composable
fun WorldPostcardsDiscoveryScreen(
    postcardsState: UiState<List<WorldPostcard>>,
    currentUserId: String,
    onCreatePostcard: () -> Unit,
    onOpenPostcard: (String) -> Unit,
    onBack: () -> Unit
) {
    var selectedCategory by remember { mutableStateOf("ALL") }

    Scaffold(
        topBar = {
            NiloraTopBar(
                title = "World Postcards",
                subtitle = "Living stories across borders",
                onBackClick = onBack,
                actions = {
                    IconButton(
                        onClick = onCreatePostcard,
                        modifier = Modifier.testTag("world_postcards_create_top_btn")
                    ) {
                        Icon(Icons.Filled.AddCircle, contentDescription = "Create World Postcard")
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onCreatePostcard,
                icon = { Icon(Icons.Filled.PostAdd, contentDescription = null) },
                text = { Text("LIVE A MOMENT") },
                containerColor = MidnightNavy,
                contentColor = SoftIvory,
                modifier = Modifier.testTag("world_postcards_fab")
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .testTag("world_postcards_list"),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        FilterChip(
                            selected = selectedCategory == "ALL",
                            onClick = { selectedCategory = "ALL" },
                            label = { Text("All Moments") }
                        )
                    }
                    items(PostcardCategory.entries) { cat ->
                        FilterChip(
                            selected = selectedCategory == cat.key,
                            onClick = { selectedCategory = cat.key },
                            label = { Text(cat.labelEn) }
                        )
                    }
                }
            }

            when (postcardsState) {
                is UiState.Loading -> item { NiloraLoadingState("Loading living World Postcards...") }
                is UiState.Error -> item { NiloraErrorState("World Postcards Unavailable", postcardsState.message) }
                is UiState.Success -> {
                    val filtered = postcardsState.data.filter { card ->
                        selectedCategory == "ALL" || card.category == selectedCategory
                    }
                    if (filtered.isEmpty()) {
                        item {
                            UnavailableDataCard(
                                title = "No World Postcards Here Yet",
                                explanation = "Share a real cultural story, local expression, photo, or question from your city. People around the world can contribute in their own languages.",
                                icon = Icons.Filled.MarkunreadMailbox,
                                actionLabel = "Create World Postcard",
                                onActionClick = onCreatePostcard
                            )
                        }
                    } else {
                        items(filtered, key = { it.id }) { card ->
                            WorldPostcardCard(
                                postcard = card,
                                isMine = card.authorId == currentUserId,
                                onClick = { onOpenPostcard(card.id) }
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
fun WorldPostcardCard(
    postcard: WorldPostcard,
    isMine: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("world_postcard_card_${postcard.id}")
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, EgyptianGold.copy(alpha = 0.38f))
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
                Surface(
                    color = EgyptianGoldContainer,
                    shape = RoundedCornerShape(999.dp)
                ) {
                    Text(
                        text = PostcardCategory.fromKey(postcard.category).labelEn,
                        style = MaterialTheme.typography.labelSmall,
                        color = EgyptianGoldOnContainer,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
                Text(
                    text = listOfNotNull(
                        postcard.city.takeIf { it.isNotBlank() },
                        postcard.country
                    ).joinToString(", ") + if (isMine) " • You" else "",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (postcard.photoBase64.isNotBlank()) {
                Base64PhotoPreview(
                    photoBase64 = postcard.photoBase64,
                    contentDescription = postcard.title
                )
            }

            Text(
                text = postcard.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = postcard.storyText,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 4,
                overflow = TextOverflow.Ellipsis
            )

            if (postcard.localExpression.isNotBlank()) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        text = "Local expression: \"${postcard.localExpression}\"",
                        style = MaterialTheme.typography.bodySmall,
                        color = NileTurquoiseDark,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }

            CountryChainRow(countries = postcard.countriesChain)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "By ${postcard.authorName} • ${postcard.contributionsCount} replies • ${postcard.languageCode}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (postcard.audioBase64.isNotBlank()) {
                    Icon(
                        imageVector = Icons.Filled.GraphicEq,
                        contentDescription = "Includes voice clip",
                        tint = NileTurquoiseDark,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun CreateWorldPostcardScreen(
    myProfile: UserProfile?,
    mediaDraft: VoiceMediaDraftState,
    onPickPhoto: (android.content.Context, Uri?) -> Unit,
    onClearPhoto: () -> Unit,
    onStartRecording: () -> Unit,
    onStopRecording: () -> Unit,
    onClearRecording: () -> Unit,
    onPlayPreviewClip: (String) -> Unit,
    onDictateText: (String) -> Unit,
    onSubmitPostcard: (
        category: PostcardCategory,
        title: String,
        storyText: String,
        localExpression: String,
        questionPrompt: String,
        country: String,
        city: String,
        languageCode: String,
        allowReplies: Boolean,
        allowPrivateConnect: Boolean,
        visibility: String
    ) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var category by remember { mutableStateOf(PostcardCategory.STORY) }
    var title by remember { mutableStateOf("") }
    var storyText by remember { mutableStateOf("") }
    var localExpression by remember { mutableStateOf("") }
    var questionPrompt by remember { mutableStateOf("") }
    var country by remember(myProfile) { mutableStateOf(myProfile?.country?.ifBlank { "Egypt" } ?: "Egypt") }
    var city by remember(myProfile) { mutableStateOf(myProfile?.city.orEmpty()) }
    var languageCode by remember(myProfile) {
        mutableStateOf(myProfile?.nativeLanguage?.ifBlank { myProfile.preferredLanguage } ?: "en")
    }
    var allowReplies by remember { mutableStateOf(true) }
    var allowPrivateConnect by remember { mutableStateOf(true) }
    var visibility by remember { mutableStateOf("PUBLIC") }

    LaunchedEffect(mediaDraft.dictatedText) {
        if (mediaDraft.dictatedText.isNotBlank()) {
            storyText = if (storyText.isBlank()) mediaDraft.dictatedText else "$storyText ${mediaDraft.dictatedText}"
        }
    }

    Scaffold(
        topBar = {
            NiloraTopBar(
                title = "Create World Postcard",
                subtitle = "Live a Moment across cultures",
                onBackClick = onBack
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .testTag("create_postcard_form"),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text("Experience Category", style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(6.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(PostcardCategory.entries) { cat ->
                        FilterChip(
                            selected = category == cat,
                            onClick = { category = cat },
                            label = { Text(cat.labelEn) }
                        )
                    }
                }
            }

            item {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Postcard Title") },
                    placeholder = { Text("e.g., Afternoon mint tea in Alexandria") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("create_postcard_title_input")
                )
            }

            item {
                OutlinedTextField(
                    value = storyText,
                    onValueChange = { storyText = it },
                    label = { Text("Story, cultural detail, or daily reflection") },
                    minLines = 3,
                    maxLines = 6,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("create_postcard_story_input")
                )
            }

            item {
                OutlinedTextField(
                    value = localExpression,
                    onValueChange = { localExpression = it },
                    label = { Text("Local Expression (Optional)") },
                    placeholder = { Text("e.g., صباح الفل / Bon appétit") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item {
                OutlinedTextField(
                    value = questionPrompt,
                    onValueChange = { questionPrompt = it },
                    label = { Text("Invite the world with a question (Optional)") },
                    placeholder = { Text("What does this moment look like where you live?") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = country,
                        onValueChange = { country = it },
                        label = { Text("Country") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = city,
                        onValueChange = { city = it },
                        label = { Text("City (No GPS/street)") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text("Postcard Language", style = MaterialTheme.typography.labelMedium)
                TargetLanguageQuickSelector(
                    selectedCode = languageCode,
                    onSelectCode = { languageCode = it }
                )
            }

            item {
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
                Card(shape = RoundedCornerShape(14.dp)) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("Privacy & Connection Controls", style = MaterialTheme.typography.titleSmall)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("PUBLIC" to "Public", "PRIVATE" to "Private Draft").forEach { (visKey, visLabel) ->
                                FilterChip(
                                    selected = visibility == visKey,
                                    onClick = { visibility = visKey },
                                    label = { Text(visLabel) }
                                )
                            }
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Allow multi-country replies", style = MaterialTheme.typography.bodyMedium)
                            Switch(checked = allowReplies, onCheckedChange = { allowReplies = it })
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Allow participants to Continue Privately", style = MaterialTheme.typography.bodyMedium)
                            Switch(checked = allowPrivateConnect, onCheckedChange = { allowPrivateConnect = it })
                        }
                    }
                }
            }

            item {
                Button(
                    onClick = {
                        onSubmitPostcard(
                            category,
                            title,
                            storyText,
                            localExpression,
                            questionPrompt,
                            country,
                            city,
                            languageCode,
                            allowReplies,
                            allowPrivateConnect,
                            visibility
                        )
                    },
                    enabled = title.trim().length >= 2 && storyText.trim().length >= 3,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("submit_world_postcard_btn")
                ) {
                    Icon(Icons.Filled.Send, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Publish World Postcard")
                }
            }
        }
    }
}

@Composable
fun WorldPostcardDetailScreen(
    detailState: PostcardDetailUiState,
    mediaDraft: VoiceMediaDraftState,
    currentUserId: String,
    myProfile: UserProfile?,
    onSetTargetLanguage: (String) -> Unit,
    onTranslatePostcard: (WorldPostcard) -> Unit,
    onTranslateContribution: (String, PostcardContribution) -> Unit,
    onPlayAudio: (String, String) -> Unit,
    onSpeakText: (String, String) -> Unit,
    onPickPhoto: (android.content.Context, Uri?) -> Unit,
    onClearPhoto: () -> Unit,
    onStartRecording: () -> Unit,
    onStopRecording: () -> Unit,
    onClearRecording: () -> Unit,
    onDictateText: (String) -> Unit,
    onSubmitContribution: (WorldPostcard, String, String, String, String, String, Boolean) -> Unit,
    onSaveToMemory: (WorldPostcard, List<PostcardContribution>, MemoryVisibility) -> Unit,
    onContinuePrivately: (partnerId: String, partnerName: String, partnerCountry: String, partnerLanguage: String, title: String) -> Unit,
    onBlockUser: (userId: String, userName: String) -> Unit,
    onReportPostcard: (targetType: String, targetId: String, reason: String) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val postcard = detailState.postcard
    var replyText by remember { mutableStateOf("") }
    var replyExpression by remember { mutableStateOf("") }
    var replyAllowConnect by remember { mutableStateOf(true) }
    var replyLangCode by remember(myProfile) {
        mutableStateOf(myProfile?.nativeLanguage?.ifBlank { myProfile.preferredLanguage } ?: "en")
    }
    var showMemoryDialog by remember { mutableStateOf(false) }

    LaunchedEffect(mediaDraft.dictatedText) {
        if (mediaDraft.dictatedText.isNotBlank()) {
            replyText = if (replyText.isBlank()) mediaDraft.dictatedText else "$replyText ${mediaDraft.dictatedText}"
        }
    }

    Scaffold(
        topBar = {
            NiloraTopBar(
                title = postcard?.title?.ifBlank { "World Postcard" } ?: "World Postcard",
                subtitle = postcard?.let { "${it.countriesChain.size} countries • ${it.contributionsCount} replies" },
                onBackClick = onBack,
                actions = {
                    if (postcard != null) {
                        IconButton(
                            onClick = { showMemoryDialog = true },
                            modifier = Modifier.testTag("postcard_save_memory_btn")
                        ) {
                            Icon(Icons.Filled.Bookmarks, contentDescription = "Save to NILORA Memory")
                        }
                    }
                }
            )
        }
    ) { padding ->
        if (postcard == null) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                NiloraLoadingState("Loading World Postcard...")
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .testTag("world_postcard_detail_list"),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            if (detailState.errorBanner != null) {
                item {
                    NiloraErrorState("Notice", detailState.errorBanner)
                }
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Translate Story Into:", style = MaterialTheme.typography.labelMedium)
                    TargetLanguageQuickSelector(
                        selectedCode = detailState.targetLanguageCode,
                        onSelectCode = onSetTargetLanguage
                    )
                }
            }

            item {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    border = BorderStroke(1.dp, EgyptianGold.copy(alpha = 0.45f))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(postcard.authorName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text(
                                    text = listOfNotNull(postcard.city.takeIf { it.isNotBlank() }, postcard.country).joinToString(", "),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (postcard.authorId != currentUserId) {
                                Row {
                                    IconButton(onClick = { onReportPostcard("POSTCARD", postcard.id, "Inappropriate or unsafe content") }) {
                                        Icon(Icons.Filled.Flag, contentDescription = "Report postcard")
                                    }
                                    IconButton(onClick = { onBlockUser(postcard.authorId, postcard.authorName) }) {
                                        Icon(Icons.Filled.Block, contentDescription = "Block author")
                                    }
                                }
                            }
                        }

                        if (postcard.photoBase64.isNotBlank()) {
                            Base64PhotoPreview(photoBase64 = postcard.photoBase64, contentDescription = postcard.title)
                        }

                        if (postcard.audioBase64.isNotBlank()) {
                            VoiceAudioPlayerChip(
                                clipId = "pc_${postcard.id}",
                                durationMs = postcard.audioDurationMs,
                                isPlaying = mediaDraft.playingAudioId == "pc_${postcard.id}",
                                onTogglePlay = { onPlayAudio("pc_${postcard.id}", postcard.audioBase64) }
                            )
                        }

                        InlineTranslationBox(
                            originalText = postcard.storyText,
                            sourceLanguageCode = postcard.languageCode,
                            translations = postcard.translations,
                            translationNote = postcard.translationNote,
                            targetLanguageCode = detailState.targetLanguageCode,
                            isTranslating = detailState.isTranslating,
                            onTranslateClick = { onTranslatePostcard(postcard) },
                            onSpeakOriginal = { onSpeakText(postcard.storyText, postcard.languageCode) },
                            onSpeakTranslation = { translated -> onSpeakText(translated, detailState.targetLanguageCode) },
                            testTagPrefix = "postcard_main"
                        )

                        if (postcard.localExpression.isNotBlank()) {
                            Text(
                                text = "Local expression: ${postcard.localExpression}",
                                style = MaterialTheme.typography.labelLarge,
                                color = NileTurquoiseDark
                            )
                        }
                        if (postcard.questionPrompt.isNotBlank()) {
                            Surface(color = EgyptianGoldContainer.copy(alpha = 0.7f), shape = RoundedCornerShape(10.dp)) {
                                Text(
                                    text = "Question: ${postcard.questionPrompt}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = EgyptianGoldOnContainer,
                                    modifier = Modifier.padding(10.dp)
                                )
                            }
                        }

                        CountryChainRow(countries = postcard.countriesChain)

                        if (postcard.authorId != currentUserId && postcard.allowPrivateConnect) {
                            OutlinedButton(
                                onClick = {
                                    onContinuePrivately(
                                        postcard.authorId,
                                        postcard.authorName,
                                        postcard.country,
                                        postcard.languageCode,
                                        postcard.title
                                    )
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("postcard_continue_privately_btn")
                            ) {
                                Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Continue Privately with ${postcard.authorName}")
                            }
                        }
                    }
                }
            }

            item {
                Text(
                    text = "Living Story Timeline (${detailState.contributions.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            if (detailState.contributions.isEmpty()) {
                item {
                    UnavailableDataCard(
                        title = "Be the Next Country in This Chain",
                        explanation = "Add your voice, local expression, or photo from your city to continue this World Postcard.",
                        icon = Icons.Filled.Public
                    )
                }
            } else {
                items(detailState.contributions, key = { it.id }) { contrib ->
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
                                    text = "${contrib.authorName} • ${listOfNotNull(contrib.city.takeIf { it.isNotBlank() }, contrib.country).joinToString(", ")}",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                                if (contrib.authorId != currentUserId && contrib.allowPrivateConnect) {
                                    TextButton(
                                        onClick = {
                                            onContinuePrivately(
                                                contrib.authorId,
                                                contrib.authorName,
                                                contrib.country,
                                                contrib.languageCode,
                                                postcard.title
                                            )
                                        },
                                        modifier = Modifier.testTag("contrib_connect_${contrib.id}")
                                    ) {
                                        Text("Connect")
                                    }
                                }
                            }

                            if (contrib.photoBase64.isNotBlank()) {
                                Base64PhotoPreview(photoBase64 = contrib.photoBase64, contentDescription = "Reply photo")
                            }
                            if (contrib.audioBase64.isNotBlank()) {
                                VoiceAudioPlayerChip(
                                    clipId = "pcc_${contrib.id}",
                                    durationMs = contrib.audioDurationMs,
                                    isPlaying = mediaDraft.playingAudioId == "pcc_${contrib.id}",
                                    onTogglePlay = { onPlayAudio("pcc_${contrib.id}", contrib.audioBase64) }
                                )
                            }

                            InlineTranslationBox(
                                originalText = contrib.responseText,
                                sourceLanguageCode = contrib.languageCode,
                                translations = contrib.translations,
                                translationNote = contrib.translationNote,
                                targetLanguageCode = detailState.targetLanguageCode,
                                isTranslating = detailState.isTranslating,
                                onTranslateClick = { onTranslateContribution(postcard.id, contrib) },
                                onSpeakOriginal = { onSpeakText(contrib.responseText, contrib.languageCode) },
                                onSpeakTranslation = { t -> onSpeakText(t, detailState.targetLanguageCode) },
                                testTagPrefix = "contrib_${contrib.id}"
                            )

                            if (contrib.localExpression.isNotBlank()) {
                                Text(
                                    text = "Expression shared: \"${contrib.localExpression}\"",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = NileTurquoiseDark
                                )
                            }
                        }
                    }
                }
            }

            if (postcard.allowReplies) {
                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f))
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text("Contribute Your Perspective", style = MaterialTheme.typography.titleMedium)
                            OutlinedTextField(
                                value = replyText,
                                onValueChange = { replyText = it },
                                label = { Text("Share your response in your own language") },
                                minLines = 2,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("postcard_reply_input")
                            )
                            OutlinedTextField(
                                value = replyExpression,
                                onValueChange = { replyExpression = it },
                                label = { Text("Local phrase from your home (Optional)") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            TargetLanguageQuickSelector(
                                selectedCode = replyLangCode,
                                onSelectCode = { replyLangCode = it }
                            )
                            ExperienceMediaAttachmentBar(
                                draft = mediaDraft,
                                languageCode = replyLangCode,
                                onPickPhotoUri = { uri -> onPickPhoto(context, uri) },
                                onClearPhoto = onClearPhoto,
                                onStartRecording = onStartRecording,
                                onStopRecording = onStopRecording,
                                onClearRecording = onClearRecording,
                                onPlayPreviewClip = { b64 -> onPlayAudio("draft_preview", b64) },
                                onDictateText = { onDictateText(replyLangCode) }
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Allow private chat invitation", style = MaterialTheme.typography.bodySmall)
                                Switch(checked = replyAllowConnect, onCheckedChange = { replyAllowConnect = it })
                            }
                            Button(
                                onClick = {
                                    onSubmitContribution(
                                        postcard,
                                        replyText,
                                        replyExpression,
                                        myProfile?.country?.ifBlank { "Egypt" } ?: "Egypt",
                                        myProfile?.city.orEmpty(),
                                        replyLangCode,
                                        replyAllowConnect
                                    )
                                    replyText = ""
                                    replyExpression = ""
                                },
                                enabled = replyText.isNotBlank() && !detailState.isSubmitting,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("postcard_submit_reply_btn")
                            ) {
                                Text(if (detailState.isSubmitting) "Sending..." else "Add to World Postcard")
                            }
                        }
                    }
                }
            }
        }
    }

    if (showMemoryDialog && postcard != null) {
        var selectedVisibility by remember { mutableStateOf(MemoryVisibility.PRIVATE) }
        AlertDialog(
            onDismissRequest = { showMemoryDialog = false },
            title = { Text("Save to NILORA Memory") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Choose who can view this saved experience:")
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
                    onSaveToMemory(postcard, detailState.contributions, selectedVisibility)
                }) { Text("Save Memory") }
            },
            dismissButton = {
                TextButton(onClick = { showMemoryDialog = false }) { Text("Cancel") }
            }
        )
    }
}
