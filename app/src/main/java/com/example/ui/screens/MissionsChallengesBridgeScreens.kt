package com.example.ui.screens

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.*
import com.example.localization.AppStrings
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.RealityFilterState

@Composable
fun RealWorldMissionsScreen(
    strings: AppStrings,
    filters: RealityFilterState,
    missionsState: UiState<List<RealityMission>>,
    selectedMissionSubmissions: List<MissionSubmission>,
    attachedPhotoBase64: String,
    blockedIds: Set<String>,
    onSelectModeFilter: (MissionExecutionMode?) -> Unit,
    onLaunchTemplate: (CuratedRealityMissionTemplate) -> Unit,
    onCreateCustomMission: (MissionExecutionMode, String, String, String, String, String, Int) -> Unit,
    onSelectMission: (RealityMission) -> Unit,
    onPickPhoto: (Context, Uri?) -> Unit,
    onClearPhoto: () -> Unit,
    onSubmitEvidence: (RealityMission, String, String, Boolean) -> Unit,
    onSaveMissionMemory: (RealityMission, String) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var activeMission by remember { mutableStateOf<RealityMission?>(null) }
    var showCreateDialog by remember { mutableStateOf(false) }

    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        onPickPhoto(context, uri)
    }

    Scaffold(
        topBar = {
            NiloraTopBar(
                title = strings.realWorldMissionsTitle,
                subtitle = "Real-World Logs Strictly Separated From Virtual Exploration",
                onBackClick = onBack
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showCreateDialog = true },
                icon = { Icon(Icons.Filled.AddTask, contentDescription = null) },
                text = { Text("New Mission") },
                modifier = Modifier.testTag("create_mission_btn")
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp, 12.dp, 16.dp, 88.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Surface(color = EgyptianGoldContainer, shape = RoundedCornerShape(14.dp)) {
                    Text(
                        text = "Honest Separation Rule: Real-World Missions record field logs & optional Photo Picker evidence. Virtual exploration is always labeled VIRTUAL and never counts as physical presence.",
                        style = MaterialTheme.typography.bodySmall,
                        color = EgyptianGoldOnContainer,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    item {
                        FilterChip(selected = filters.missionModeFilter == null, onClick = { onSelectModeFilter(null) }, label = { Text("All Modes") })
                    }
                    items(MissionExecutionMode.entries) { mode ->
                        FilterChip(
                            selected = filters.missionModeFilter == mode,
                            onClick = { onSelectModeFilter(if (filters.missionModeFilter == mode) null else mode) },
                            label = { Text(mode.labelEn) }
                        )
                    }
                }
            }

            item {
                NiloraSectionHeader(title = "Starter Curated Mission Templates", subtitle = "Tap Launch to start tracking real steps")
            }
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(RealityMissionCatalog.starterTemplates, key = { it.id }) { tpl ->
                        Card(modifier = Modifier.width(260.dp), shape = RoundedCornerShape(16.dp)) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(tpl.cityName, style = MaterialTheme.typography.labelSmall, color = NileTurquoiseDark, fontWeight = FontWeight.Bold)
                                Text(tpl.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, maxLines = 2)
                                Text(tpl.objective, style = MaterialTheme.typography.bodySmall, maxLines = 3, overflow = TextOverflow.Ellipsis)
                                Button(
                                    onClick = { onLaunchTemplate(tpl) },
                                    modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 48.dp).testTag("launch_tpl_${tpl.id}")
                                ) { Text("Launch (${tpl.suggestedSteps} steps)") }
                            }
                        }
                    }
                }
            }

            item {
                NiloraSectionHeader(title = "Active Missions", subtitle = "Tap a mission to submit field evidence")
            }

            when (missionsState) {
                is UiState.Loading -> item { NiloraLoadingState("Loading missions...") }
                is UiState.Error -> item { NiloraErrorState("Missions Unavailable", missionsState.message) }
                is UiState.Success -> {
                    val list = missionsState.data.filter { m ->
                        m.creatorId !in blockedIds &&
                            (filters.missionModeFilter == null || m.executionMode == filters.missionModeFilter.key)
                    }
                    if (list.isEmpty()) {
                        item {
                            UnavailableDataCard("No Missions Launched Yet", "Launch any curated template above or create your own mission.", Icons.Filled.Flag)
                        }
                    } else {
                        items(list, key = { it.id }) { mission ->
                            val mode = MissionExecutionMode.fromKey(mission.executionMode)
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        activeMission = mission
                                        onSelectMission(mission)
                                    }
                                    .testTag("mission_card_${mission.id}"),
                                border = BorderStroke(1.dp, if (activeMission?.id == mission.id) NileTurquoise else MaterialTheme.colorScheme.outlineVariant)
                            ) {
                                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text(mode.labelEn, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = EgyptianGoldDark)
                                        Text("${mission.submissionsCount}/${mission.targetSteps} logs • ${mission.status}", style = MaterialTheme.typography.labelSmall, color = NileTurquoiseDark)
                                    }
                                    Text(mission.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                    Text("${mission.cityName}, ${mission.countryName}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(mission.objective, style = MaterialTheme.typography.bodySmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                }
                            }
                        }
                    }
                }
            }

            activeMission?.let { m ->
                item {
                    MissionEvidenceLoggerCard(
                        mission = m,
                        submissions = selectedMissionSubmissions,
                        attachedPhotoBase64 = attachedPhotoBase64,
                        onLaunchPicker = {
                            photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        },
                        onClearPhoto = onClearPhoto,
                        onSubmitEvidence = onSubmitEvidence,
                        onSaveMemory = onSaveMissionMemory
                    )
                }
            }
        }
    }

    if (showCreateDialog) {
        var mode by remember { mutableStateOf(MissionExecutionMode.REAL_WORLD) }
        var title by remember { mutableStateOf("") }
        var objective by remember { mutableStateOf("") }
        var city by remember { mutableStateOf("Cairo") }
        var steps by remember { mutableStateOf("3") }
        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = { Text("Create Mission") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        MissionExecutionMode.entries.forEach { m ->
                            FilterChip(selected = mode == m, onClick = { mode = m }, label = { Text(m.labelEn) })
                        }
                    }
                    OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Title*") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = city, onValueChange = { city = it }, label = { Text("City*") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = steps, onValueChange = { steps = it.filter(Char::isDigit) }, label = { Text("Target Steps (1-30)") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = objective, onValueChange = { objective = it }, label = { Text("Objective*") }, minLines = 2, modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val hub = GlobalCityHubCatalog.findHub(city)
                        onCreateCustomMission(
                            mode, title, objective, city, hub?.countryName ?: "Egypt",
                            "Public spaces only. Respect environmental & cultural guidelines.",
                            (steps.toIntOrNull() ?: 3).coerceIn(1, 30)
                        )
                        showCreateDialog = false
                    },
                    enabled = title.trim().length >= 2 && objective.trim().length >= 4
                ) { Text("Create") }
            },
            dismissButton = { TextButton(onClick = { showCreateDialog = false }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun MissionEvidenceLoggerCard(
    mission: RealityMission,
    submissions: List<MissionSubmission>,
    attachedPhotoBase64: String,
    onLaunchPicker: () -> Unit,
    onClearPhoto: () -> Unit,
    onSubmitEvidence: (RealityMission, String, String, Boolean) -> Unit,
    onSaveMemory: (RealityMission, String) -> Unit
) {
    var placeLabel by remember(mission.id) { mutableStateOf(mission.cityName) }
    var reflection by remember(mission.id) { mutableStateOf("") }
    var ecoConfirmed by remember(mission.id) { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth().testTag("mission_evidence_card"),
        colors = CardDefaults.cardColors(containerColor = SurfaceIvoryMuted)
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Log Evidence: ${mission.title}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text("Safety & Eco Rules: ${mission.safetyAndEcoRules}", style = MaterialTheme.typography.bodySmall, color = WarningAmber)
            OutlinedTextField(value = placeLabel, onValueChange = { placeLabel = it }, label = { Text("Public Place / Landmark*") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = reflection, onValueChange = { reflection = it }, label = { Text("Field Observation / Reflection*") }, minLines = 2, modifier = Modifier.fillMaxWidth())
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedButton(onClick = onLaunchPicker, modifier = Modifier.defaultMinSize(minHeight = 48.dp)) {
                    Icon(Icons.Filled.PhotoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(if (attachedPhotoBase64.isBlank()) "Attach Photo" else "Photo Ready")
                }
                if (attachedPhotoBase64.isNotBlank()) {
                    TextButton(onClick = onClearPhoto) { Text("Remove") }
                }
            }
            if (attachedPhotoBase64.isNotBlank()) {
                Base64PhotoPreview(photoBase64 = attachedPhotoBase64, contentDescription = "Mission attachment preview")
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = ecoConfirmed, onCheckedChange = { ecoConfirmed = it })
                Text("I followed public safety & eco guidelines.", style = MaterialTheme.typography.bodySmall)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = {
                        onSubmitEvidence(mission, reflection, placeLabel, ecoConfirmed)
                        reflection = ""
                        ecoConfirmed = false
                    },
                    enabled = ecoConfirmed && reflection.isNotBlank() && placeLabel.isNotBlank(),
                    modifier = Modifier.weight(1f).defaultMinSize(minHeight = 48.dp).testTag("submit_mission_evidence_btn")
                ) { Text("Submit Log") }
                OutlinedButton(
                    onClick = { onSaveMemory(mission, reflection) },
                    modifier = Modifier.defaultMinSize(minHeight = 48.dp)
                ) { Text("Save Memory") }
            }
            if (submissions.isNotEmpty()) {
                HorizontalDivider()
                Text("Recorded Logs (${submissions.size})", style = MaterialTheme.typography.labelLarge)
                submissions.take(4).forEach { sub ->
                    Text(
                        "• [${sub.submissionMode}] ${sub.authorName} @ ${sub.placeLabel}: ${sub.reflectionText}",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}

@Composable
fun GlobalChallengesScreen(
    strings: AppStrings,
    filters: RealityFilterState,
    challengesState: UiState<List<GlobalChallenge>>,
    selectedChallengeEntries: List<ChallengeEntry>,
    blockedIds: Set<String>,
    onSelectScope: (ChallengeScope?) -> Unit,
    onCreateChallenge: (ChallengeScope, String, String, String, Int, String, String) -> Unit,
    onSelectChallenge: (GlobalChallenge) -> Unit,
    onSubmitEntry: (GlobalChallenge, String, String, Int) -> Unit,
    onBack: () -> Unit
) {
    var activeChallenge by remember { mutableStateOf<GlobalChallenge?>(null) }
    var showCreateDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            NiloraTopBar(
                title = strings.globalChallengesTitle,
                subtitle = "Transparent Cross-Border Challenges • No Fake Leaderboards",
                onBackClick = onBack
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showCreateDialog = true },
                icon = { Icon(Icons.Filled.EmojiEvents, contentDescription = null) },
                text = { Text("New Challenge") },
                modifier = Modifier.testTag("create_challenge_btn")
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp, 12.dp, 16.dp, 88.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    item {
                        FilterChip(selected = filters.challengeScopeFilter == null, onClick = { onSelectScope(null) }, label = { Text("All Scopes") })
                    }
                    items(ChallengeScope.entries) { sc ->
                        FilterChip(
                            selected = filters.challengeScopeFilter == sc,
                            onClick = { onSelectScope(if (filters.challengeScopeFilter == sc) null else sc) },
                            label = { Text(sc.labelEn) }
                        )
                    }
                }
            }

            when (challengesState) {
                is UiState.Loading -> item { NiloraLoadingState("Loading Global Challenges...") }
                is UiState.Error -> item { NiloraErrorState("Challenges Unavailable", challengesState.message) }
                is UiState.Success -> {
                    val list = challengesState.data.filter { c ->
                        c.creatorId !in blockedIds &&
                            (filters.challengeScopeFilter == null || c.challengeScope == filters.challengeScopeFilter.key)
                    }
                    if (list.isEmpty()) {
                        item {
                            UnavailableDataCard(
                                title = "No Global Challenges Active Yet",
                                explanation = "Launch an Individual, Team, or International challenge — entries always display honest USER_SUBMITTED vs ADMIN_VERIFIED labels.",
                                icon = Icons.Filled.EmojiEvents,
                                actionLabel = "New Challenge",
                                onActionClick = { showCreateDialog = true }
                            )
                        }
                    } else {
                        items(list, key = { it.id }) { ch ->
                            val progress = (ch.entriesCount.toFloat() / ch.targetGoal.coerceAtLeast(1)).coerceIn(0f, 1f)
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        activeChallenge = ch
                                        onSelectChallenge(ch)
                                    },
                                border = BorderStroke(1.dp, if (activeChallenge?.id == ch.id) EgyptianGold else MaterialTheme.colorScheme.outlineVariant)
                            ) {
                                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("${ch.challengeScope} • ${ch.category}", style = MaterialTheme.typography.labelSmall, color = EgyptianGoldDark, fontWeight = FontWeight.Bold)
                                        Text("${ch.entriesCount}/${ch.targetGoal} entries", style = MaterialTheme.typography.labelSmall, color = NileTurquoiseDark)
                                    }
                                    Text(ch.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                    Text(ch.description, style = MaterialTheme.typography.bodySmall)
                                    LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth())
                                    Text("Countries: ${ch.participantCountries.joinToString()}", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    }
                }
            }

            activeChallenge?.let { ch ->
                item {
                    var teamName by remember(ch.id) { mutableStateOf("") }
                    var summary by remember(ch.id) { mutableStateOf("") }
                    Card(colors = CardDefaults.cardColors(containerColor = SurfaceIvoryMuted)) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Contribute to: ${ch.title}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            OutlinedTextField(value = teamName, onValueChange = { teamName = it }, label = { Text("Team Name (Optional)") }, modifier = Modifier.fillMaxWidth())
                            OutlinedTextField(value = summary, onValueChange = { summary = it }, label = { Text("Contribution Summary*") }, minLines = 2, modifier = Modifier.fillMaxWidth())
                            Button(
                                onClick = {
                                    onSubmitEntry(ch, teamName, summary, 1)
                                    summary = ""
                                },
                                enabled = summary.isNotBlank(),
                                modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 48.dp)
                            ) { Text("Submit Entry (+1)") }
                            if (selectedChallengeEntries.isNotEmpty()) {
                                HorizontalDivider()
                                selectedChallengeEntries.take(6).forEach { e ->
                                    Text(
                                        "• [${e.verificationLabel}] ${e.authorName} (${e.countryName}): ${e.summaryText}",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        var scope by remember { mutableStateOf(ChallengeScope.INTERNATIONAL) }
        var title by remember { mutableStateOf("") }
        var desc by remember { mutableStateOf("") }
        var city by remember { mutableStateOf("Cairo") }
        var goal by remember { mutableStateOf("20") }
        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = { Text("Launch Global Challenge") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        ChallengeScope.entries.forEach { s ->
                            FilterChip(selected = scope == s, onClick = { scope = s }, label = { Text(s.labelEn) })
                        }
                    }
                    OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Challenge Title*") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = city, onValueChange = { city = it }, label = { Text("Primary City*") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = goal, onValueChange = { goal = it.filter(Char::isDigit) }, label = { Text("Target Entries Goal") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = desc, onValueChange = { desc = it }, label = { Text("Description*") }, minLines = 2, modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val hub = GlobalCityHubCatalog.findHub(city)
                        onCreateChallenge(scope, "CULTURAL_EXCHANGE", title, desc, (goal.toIntOrNull() ?: 20).coerceIn(1, 500), city, hub?.countryName ?: "Egypt")
                        showCreateDialog = false
                    },
                    enabled = title.trim().length >= 2 && desc.trim().length >= 4
                ) { Text("Launch") }
            },
            dismissButton = { TextButton(onClick = { showCreateDialog = false }) { Text("Cancel") } }
        )
    }
}

@Composable
fun RealityBridgeScreen(
    strings: AppStrings,
    bridgeState: UiState<List<RealityBridgeLink>>,
    onCreateBridgePlan: (AdventureExperience, String, String, String, RealityBridgeStage, String) -> Unit,
    onAdvanceStage: (RealityBridgeLink, RealityBridgeStage) -> Unit,
    onDeleteBridge: (String) -> Unit,
    onBack: () -> Unit
) {
    var showAddDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            NiloraTopBar(
                title = strings.realityBridgeTitle,
                subtitle = "Virtual Exploration -> Planned -> Registered -> Confirmed -> Memory",
                onBackClick = onBack
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAddDialog = true },
                icon = { Icon(Icons.Filled.AddLink, contentDescription = null) },
                text = { Text("Bridge Adventure") },
                modifier = Modifier.testTag("add_reality_bridge_btn")
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp, 12.dp, 16.dp, 88.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            when (bridgeState) {
                is UiState.Loading -> item { NiloraLoadingState("Loading Reality Bridge links...") }
                is UiState.Error -> item { NiloraErrorState("Reality Bridge Unavailable", bridgeState.message) }
                is UiState.Success -> {
                    val links = bridgeState.data
                    if (links.isEmpty()) {
                        item {
                            UnavailableDataCard(
                                title = "No Virtual-to-Real Plans Saved Yet",
                                explanation = "Turn any 3D virtual adventure into a concrete Real Event, Team-Up, Mission, or Verified Trip plan.",
                                icon = Icons.Filled.Route,
                                actionLabel = "Bridge Adventure",
                                onActionClick = { showAddDialog = true }
                            )
                        }
                    } else {
                        items(links, key = { it.id }) { link ->
                            val currentStage = RealityBridgeStage.fromKey(link.stage)
                            val nextStage = RealityBridgeStage.entries.firstOrNull { it.stepIndex == currentStage.stepIndex + 1 }
                            Card(modifier = Modifier.fillMaxWidth().testTag("bridge_card_${link.id}")) {
                                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text(link.virtualAdventureTitle, style = MaterialTheme.typography.labelLarge, color = EgyptianGoldDark, fontWeight = FontWeight.Bold)
                                        IconButton(onClick = { onDeleteBridge(link.id) }, modifier = Modifier.size(32.dp)) {
                                            Icon(Icons.Filled.DeleteOutline, contentDescription = "Remove plan")
                                        }
                                    }
                                    Text("${link.targetTitle} (${link.destinationName})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                    if (link.notes.isNotBlank()) {
                                        Text(link.notes, style = MaterialTheme.typography.bodySmall)
                                    }
                                    LinearProgressIndicator(
                                        progress = { (currentStage.stepIndex + 1) / 5f },
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    Text("Stage: ${currentStage.labelEn}", style = MaterialTheme.typography.labelMedium, color = NileTurquoiseDark)
                                    if (nextStage != null) {
                                        Button(
                                            onClick = { onAdvanceStage(link, nextStage) },
                                            modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 48.dp)
                                        ) { Text("Advance -> ${nextStage.labelEn}") }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        var selectedAdv by remember { mutableStateOf(AdventureCatalog.egyptAdventures.first()) }
        var targetType by remember { mutableStateOf("REAL_WORLD_MISSION") }
        var targetTitle by remember { mutableStateOf("") }
        var notes by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Bridge Virtual World to Real Life") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(AdventureCatalog.egyptAdventures.take(5)) { adv ->
                            FilterChip(selected = selectedAdv.id == adv.id, onClick = { selectedAdv = adv }, label = { Text(adv.destinationName) })
                        }
                    }
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("REAL_WORLD_MISSION", "REALITY_EVENT", "TEAM_ACTIVITY", "BOOKING_ACTIVITY").forEach { t ->
                            item { FilterChip(selected = targetType == t, onClick = { targetType = t }, label = { Text(t.replace('_', ' ')) }) }
                        }
                    }
                    OutlinedTextField(value = targetTitle, onValueChange = { targetTitle = it }, label = { Text("Real-World Plan Title*") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = notes, onValueChange = { notes = it }, label = { Text("Plan Notes") }, modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onCreateBridgePlan(selectedAdv, targetType, "plan_${System.currentTimeMillis()}", targetTitle, RealityBridgeStage.PLANNED, notes)
                        showAddDialog = false
                    },
                    enabled = targetTitle.trim().length >= 2
                ) { Text("Save Bridge") }
            },
            dismissButton = { TextButton(onClick = { showAddDialog = false }) { Text("Cancel") } }
        )
    }
}
