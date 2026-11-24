package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
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
import com.example.data.model.*
import com.example.localization.AppStrings
import com.example.ui.components.*
import com.example.ui.theme.*

@Composable
fun NiloraCrewsScreen(
    strings: AppStrings,
    currentUserId: String,
    myProfile: UserProfile?,
    myCrewsState: UiState<List<NiloraCrew>>,
    openCrewsState: UiState<List<NiloraCrew>>,
    onCreateCrew: (
        name: String,
        description: String,
        focusDestination: String,
        interests: List<String>,
        plannedAdventureId: String?,
        joinPolicy: String,
        voiceEnabled: Boolean
    ) -> Unit,
    onJoinCrew: (NiloraCrew) -> Unit,
    onOpenCrewDetail: (String) -> Unit,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)

    var showCreateDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            NiloraTopBar(
                title = strings.crewsTitle,
                subtitle = "Persistent Adventure Friendships",
                onBackClick = onBack,
                actions = {
                    IconButton(
                        onClick = { showCreateDialog = true },
                        modifier = Modifier.testTag("create_crew_dialog_btn")
                    ) {
                        Icon(Icons.Filled.GroupAdd, contentDescription = strings.createCrewLabel)
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .testTag("nilora_crews_list"),
            contentPadding = PaddingValues(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = NileTurquoiseContainer),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Connections That Continue Beyond One Experience", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = NileTurquoiseOnContainer)
                        Text(
                            "Form a crew after sharing a Red Sea reef exploration or cultural swap — plan future adventures, keep shared memories, and chat across languages.",
                            style = MaterialTheme.typography.bodySmall,
                            color = NileTurquoiseOnContainer
                        )
                    }
                }
            }

            item { Text("My Crews", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
            when (myCrewsState) {
                is UiState.Loading -> item { NiloraLoadingState("Loading your crews...") }
                is UiState.Error -> item { NiloraErrorState("Crews Unavailable", myCrewsState.message) }
                is UiState.Success -> {
                    if (myCrewsState.data.isEmpty()) {
                        item {
                            UnavailableDataCard(
                                title = "No Crews Joined Yet",
                                explanation = "Create your own adventure crew or join an open crew below.",
                                icon = Icons.Filled.Groups,
                                actionLabel = strings.createCrewLabel,
                                onActionClick = { showCreateDialog = true }
                            )
                        }
                    } else {
                        items(myCrewsState.data, key = { "mine_${it.id}" }) { crew ->
                            CrewCard(
                                crew = crew,
                                isMember = true,
                                onJoin = {},
                                onOpen = { onOpenCrewDetail(crew.id) }
                            )
                        }
                    }
                }
            }

            item { Text("Open Crews Looking for Explorers", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
            when (openCrewsState) {
                is UiState.Loading -> item { NiloraLoadingState("Discovering open crews...") }
                is UiState.Error -> item { NiloraErrorState("Open Crews Unavailable", openCrewsState.message) }
                is UiState.Success -> {
                    val others = openCrewsState.data.filter { currentUserId !in it.memberIds }
                    if (others.isEmpty()) {
                        item {
                            Text(
                                "No other open crews published yet.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        items(others, key = { "open_${it.id}" }) { crew ->
                            CrewCard(
                                crew = crew,
                                isMember = false,
                                onJoin = { onJoinCrew(crew) },
                                onOpen = { onOpenCrewDetail(crew.id) }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        var name by remember { mutableStateOf("") }
        var desc by remember { mutableStateOf("") }
        var dest by remember { mutableStateOf("Sharm El Sheikh") }
        var joinPolicy by remember { mutableStateOf("OPEN") }
        var voiceEnabled by remember { mutableStateOf(true) }

        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = { Text(strings.createCrewLabel) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Crew Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("crew_name_input")
                    )
                    OutlinedTextField(
                        value = desc,
                        onValueChange = { desc = it },
                        label = { Text("Shared Adventure Goal") },
                        minLines = 2,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = dest,
                        onValueChange = { dest = it },
                        label = { Text("Focus Destination") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(selected = joinPolicy == "OPEN", onClick = { joinPolicy = "OPEN" }, label = { Text("Open Crew") })
                        FilterChip(selected = joinPolicy == "INVITE_ONLY", onClick = { joinPolicy = "INVITE_ONLY" }, label = { Text("Invite Only") })
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onCreateCrew(
                            name,
                            desc,
                            dest,
                            myProfile?.interests.orEmpty().ifEmpty { listOf("Adventure") },
                            AdventureCatalog.egyptAdventures.firstOrNull { it.destinationName.contains(dest, ignoreCase = true) }?.id,
                            joinPolicy,
                            voiceEnabled
                        )
                        showCreateDialog = false
                    },
                    enabled = name.trim().length >= 2,
                    modifier = Modifier.testTag("submit_create_crew_btn")
                ) { Text("Create") }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun CrewCard(
    crew: NiloraCrew,
    isMember: Boolean,
    onJoin: () -> Unit,
    onOpen: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = isMember, onClick = onOpen)
            .testTag("crew_card_${crew.id}"),
        border = BorderStroke(1.dp, if (isMember) NileTurquoise else MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(crew.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Surface(color = EgyptianGoldContainer, shape = RoundedCornerShape(999.dp)) {
                    Text(
                        "${crew.memberIds.size} members",
                        style = MaterialTheme.typography.labelSmall,
                        color = EgyptianGoldOnContainer,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
            Text("Focus: ${crew.focusDestination} • Countries: ${crew.memberCountries.values.distinct().joinToString(", ")}", style = MaterialTheme.typography.bodySmall, color = NileTurquoiseDark)
            if (crew.description.isNotBlank()) {
                Text(crew.description, style = MaterialTheme.typography.bodySmall)
            }
            if (isMember) {
                OutlinedButton(
                    onClick = onOpen,
                    modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 48.dp)
                ) { Text("Open Crew Hub") }
            } else {
                Button(
                    onClick = onJoin,
                    modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 48.dp)
                ) { Text("Join Crew") }
            }
        }
    }
}

@Composable
fun CrewDetailScreen(
    crew: NiloraCrew?,
    messages: List<CrewMessage>,
    myLanguageCode: String,
    onSendMessage: (String) -> Unit,
    onTranslateMessage: (CrewMessage, String) -> Unit,
    onPlanAdventure: (NiloraCrew, AdventureExperience) -> Unit,
    onLaunchCrewAdventure: (AdventureExperience, String) -> Unit,
    onBack: () -> Unit,
    onOpenLiveTranslator: () -> Unit = {},
    onOpenRealityBridge: () -> Unit = {}
) {
    BackHandler(onBack = onBack)
    var msgInput by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            NiloraTopBar(
                title = crew?.name ?: "NILORA Crew",
                subtitle = crew?.focusDestination ?: "",
                onBackClick = onBack
            )
        }
    ) { padding ->
        if (crew == null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                NiloraLoadingState("Loading crew workspace...")
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("Crew Members (${crew.memberIds.size})", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Text(
                                crew.memberNames.entries.joinToString(" • ") { (id, n) ->
                                    "$n (${crew.memberCountries[id] ?: "World"})"
                                },
                                style = MaterialTheme.typography.bodySmall
                            )
                            if (crew.adventureHistoryTitles.isNotEmpty()) {
                                HorizontalDivider()
                                Text("Shared Adventure History", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                                crew.adventureHistoryTitles.takeLast(4).forEach { h ->
                                    Text("• $h", style = MaterialTheme.typography.bodySmall)
                                }
                            }
                            HorizontalDivider()
                            Text("Launch Next Crew Adventure", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                items(AdventureCatalog.egyptAdventures.take(5)) { adv ->
                                    FilledTonalButton(
                                        onClick = {
                                            onPlanAdventure(crew, adv)
                                            onLaunchCrewAdventure(adv, crew.id)
                                        },
                                        modifier = Modifier.defaultMinSize(minHeight = 48.dp)
                                    ) {
                                        Text(adv.destinationName)
                                    }
                                }
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(
                                    onClick = onOpenLiveTranslator,
                                    modifier = Modifier.weight(1f).defaultMinSize(minHeight = 48.dp).testTag("crew_open_live_btn")
                                ) { Text("Live Subtitles", maxLines = 1) }
                                OutlinedButton(
                                    onClick = onOpenRealityBridge,
                                    modifier = Modifier.weight(1f).defaultMinSize(minHeight = 48.dp).testTag("crew_open_bridge_btn")
                                ) { Text("Reality Bridge", maxLines = 1) }
                            }
                        }
                    }
                }

                item {
                    Text("Crew Multilingual Conversation", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                }

                if (messages.isEmpty()) {
                    item {
                        Text("Start planning your next shared adventure or trip with your crew.", style = MaterialTheme.typography.bodySmall)
                    }
                } else {
                    items(messages, key = { it.id }) { m ->
                        val translated = m.translations[myLanguageCode]
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("${m.senderName} (${m.sourceLanguage})", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                    TextButton(onClick = { onTranslateMessage(m, myLanguageCode) }) { Text("Translate") }
                                }
                                Text(m.originalText, style = MaterialTheme.typography.bodyMedium)
                                if (!translated.isNullOrBlank()) {
                                    Text(translated, style = MaterialTheme.typography.bodySmall, color = NileTurquoiseDark)
                                }
                            }
                        }
                    }
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = msgInput,
                            onValueChange = { msgInput = it },
                            placeholder = { Text("Message crew...") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = {
                                if (msgInput.isNotBlank()) {
                                    onSendMessage(msgInput)
                                    msgInput = ""
                                }
                            },
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PlatformCapabilitiesScreen(
    strings: AppStrings,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)

    Scaffold(
        topBar = {
            NiloraTopBar(
                title = strings.capabilitiesTitle,
                subtitle = "Honest Implementation Status",
                onBackClick = onBack
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .testTag("platform_capabilities_list"),
            contentPadding = PaddingValues(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MidnightNavy),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Zero-Pretend Engineering Standard", style = MaterialTheme.typography.titleMedium, color = SoftIvory, fontWeight = FontWeight.Bold)
                        Text(
                            "NILORA clearly separates implemented & tested features from hardware/network dependencies and services awaiting configuration.",
                            style = MaterialTheme.typography.bodySmall,
                            color = SoftIvory.copy(alpha = 0.85f)
                        )
                    }
                }
            }

            items(PlatformCapabilityRegistry.items) { cap ->
                val badgeColor = when (cap.bucket) {
                    CapabilityBucket.IMPLEMENTED_AND_TESTED -> NileTurquoiseContainer
                    CapabilityBucket.DEVICE_OR_NETWORK_DEPENDENT -> EgyptianGoldContainer
                    CapabilityBucket.REQUIRES_EXTERNAL_CONFIG -> WarningAmberContainer
                    CapabilityBucket.NOT_YET_IMPLEMENTED -> SunsetCoralContainer
                }
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Surface(color = badgeColor, shape = RoundedCornerShape(999.dp)) {
                            Text(
                                cap.bucket.labelEn,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                        Text(cap.feature, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Text(cap.details, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            item { NiloraFooter() }
        }
    }
}
