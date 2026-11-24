package com.example.ui.screens

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.*
import com.example.data.repository.GpsCoordinates
import com.example.localization.AppStrings
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.RealityFilterState

@Composable
fun RealityUniverseHubScreen(
    strings: AppStrings,
    currentUserId: String,
    filters: RealityFilterState,
    localGps: GpsCoordinates?,
    gpsMessage: String,
    activitiesState: UiState<List<TeamActivityPost>>,
    blockedIds: Set<String>,
    onSelectCity: (String) -> Unit,
    onSetDistanceKm: (Int?) -> Unit,
    onRequestGps: () -> Unit,
    onSelectDomain: (ActivityDomain?) -> Unit,
    onSelectSubCategory: (ActivitySubCategory?) -> Unit,
    onSelectSkill: (ActivitySkillLevel?) -> Unit,
    onToggleOpenSlotsOnly: () -> Unit,
    onOpenEvents: () -> Unit,
    onOpenMissions: () -> Unit,
    onOpenChallenges: () -> Unit,
    onOpenRealityBridge: () -> Unit,
    onOpenWorldPulseMap: () -> Unit,
    onCreateTeamActivity: (
        domain: ActivityDomain,
        sub: ActivitySubCategory,
        title: String,
        desc: String,
        city: String,
        country: String,
        spot: String,
        dateLabel: String,
        skill: ActivitySkillLevel,
        equip: String,
        safety: String,
        policy: String,
        maxP: Int,
        impactOrg: String
    ) -> Unit,
    onOpenActivityDetail: (String) -> Unit,
    onBack: () -> Unit
) {
    var showCreateDialog by remember { mutableStateOf(false) }

    val gpsPermLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { perms ->
        if (perms[Manifest.permission.ACCESS_COARSE_LOCATION] == true ||
            perms[Manifest.permission.ACCESS_FINE_LOCATION] == true
        ) {
            onRequestGps()
        }
    }

    Scaffold(
        topBar = {
            NiloraTopBar(
                title = strings.realityUniverseTitle,
                subtitle = strings.goPlayTeamUpTitle,
                onBackClick = onBack,
                actions = {
                    IconButton(
                        onClick = onOpenWorldPulseMap,
                        modifier = Modifier.size(48.dp).testTag("reality_hub_nav_map")
                    ) {
                        Icon(Icons.Filled.Map, contentDescription = strings.worldPulseTitle)
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showCreateDialog = true },
                icon = { Icon(Icons.Filled.GroupAdd, contentDescription = null) },
                text = { Text("Team Up") },
                modifier = Modifier.testTag("create_team_activity_btn")
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp, 12.dp, 16.dp, 88.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Phase 2 Modules Navigation Strip
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        FilledTonalButton(
                            onClick = onOpenEvents,
                            modifier = Modifier.defaultMinSize(minHeight = 48.dp).testTag("reality_hub_nav_events")
                        ) {
                            Icon(Icons.Filled.Celebration, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(strings.realityEventsTitle)
                        }
                    }
                    item {
                        FilledTonalButton(
                            onClick = onOpenMissions,
                            modifier = Modifier.defaultMinSize(minHeight = 48.dp).testTag("reality_hub_nav_missions")
                        ) {
                            Icon(Icons.Filled.Flag, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(strings.realWorldMissionsTitle)
                        }
                    }
                    item {
                        FilledTonalButton(
                            onClick = onOpenChallenges,
                            modifier = Modifier.defaultMinSize(minHeight = 48.dp).testTag("reality_hub_nav_challenges")
                        ) {
                            Icon(Icons.Filled.EmojiEvents, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(strings.globalChallengesTitle)
                        }
                    }
                    item {
                        FilledTonalButton(
                            onClick = onOpenRealityBridge,
                            modifier = Modifier.defaultMinSize(minHeight = 48.dp).testTag("reality_hub_nav_bridge")
                        ) {
                            Icon(Icons.Filled.Route, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(strings.realityBridgeTitle)
                        }
                    }
                }
            }

            // Local Life Discovery & Privacy-Safe Opt-In GPS Card
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = NileTurquoiseContainer),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(strings.localLifeTitle, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = NileTurquoiseOnContainer)
                                Text(
                                    text = gpsMessage.ifBlank { "Pick a city manually or check local GPS on-device only (never broadcast)." },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = NileTurquoiseOnContainer
                                )
                            }
                            OutlinedButton(
                                onClick = {
                                    gpsPermLauncher.launch(
                                        arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION)
                                    )
                                },
                                modifier = Modifier.defaultMinSize(minHeight = 48.dp).testTag("local_life_gps_btn")
                            ) {
                                Icon(Icons.Filled.MyLocation, contentDescription = "Detect Local GPS", modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Local GPS")
                            }
                        }

                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            item {
                                FilterChip(selected = filters.selectedCity == "ALL", onClick = { onSelectCity("ALL") }, label = { Text("Global") })
                            }
                            items(GlobalCityHubCatalog.hubs) { hub ->
                                FilterChip(
                                    selected = filters.selectedCity.equals(hub.cityName, true),
                                    onClick = { onSelectCity(hub.cityName) },
                                    label = { Text(hub.cityName) }
                                )
                            }
                        }

                        if (localGps != null) {
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                listOf(null to "Any Distance", 10 to "< 10 km", 25 to "< 25 km", 100 to "< 100 km").forEach { (km, lbl) ->
                                    item {
                                        FilterChip(selected = filters.maxDistanceKm == km, onClick = { onSetDistanceKm(km) }, label = { Text(lbl) })
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Domain Filters: Go Together, Play Sports, Social Games, Adventure, Community Impact
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    item {
                        FilterChip(
                            selected = filters.activityDomain == null,
                            onClick = { onSelectDomain(null) },
                            label = { Text("All Activities") }
                        )
                    }
                    items(ActivityDomain.entries) { dom ->
                        FilterChip(
                            selected = filters.activityDomain == dom,
                            onClick = { onSelectDomain(if (filters.activityDomain == dom) null else dom) },
                            label = { Text(dom.labelEn) },
                            modifier = Modifier.testTag("domain_chip_${dom.key}")
                        )
                    }
                }
            }

            // Subcategory & Skill & Open-Slots Filters
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    item {
                        FilterChip(selected = filters.openSlotsOnly, onClick = onToggleOpenSlotsOnly, label = { Text("Open Slots Only") })
                    }
                    filters.activityDomain?.let { dom ->
                        items(ActivitySubCategory.forDomain(dom)) { sub ->
                            FilterChip(
                                selected = filters.activitySubCategory == sub,
                                onClick = { onSelectSubCategory(if (filters.activitySubCategory == sub) null else sub) },
                                label = { Text(sub.labelEn) }
                            )
                        }
                    }
                    items(ActivitySkillLevel.entries) { lvl ->
                        FilterChip(
                            selected = filters.activitySkillLevel == lvl,
                            onClick = { onSelectSkill(if (filters.activitySkillLevel == lvl) null else lvl) },
                            label = { Text(lvl.labelEn) }
                        )
                    }
                }
            }

            // Live Firestore Team-Up Activities
            when (activitiesState) {
                is UiState.Loading -> item { NiloraLoadingState("Loading Team-Up & Social Play posts...") }
                is UiState.Error -> item { NiloraErrorState("Activities Unavailable", activitiesState.message) }
                is UiState.Success -> {
                    val filtered = activitiesState.data.filter { post ->
                        post.creatorId !in blockedIds &&
                            (filters.selectedCity == "ALL" || post.cityName.equals(filters.selectedCity, true)) &&
                            (filters.activityDomain == null || post.domain == filters.activityDomain.key) &&
                            (filters.activitySubCategory == null || post.subCategory == filters.activitySubCategory.key) &&
                            (filters.activitySkillLevel == null || post.skillLevel == filters.activitySkillLevel.key) &&
                            (!filters.openSlotsOnly || post.participantIds.size < post.maxParticipants &&
                                post.teamStatus in setOf(TeamUpStatus.LOOKING_FOR_PARTICIPANTS.key, TeamUpStatus.GROUP_FORMING.key)) &&
                            (filters.maxDistanceKm == null || localGps == null || post.latitude == null || post.longitude == null ||
                                geoDistanceKm(localGps.latitude, localGps.longitude, post.latitude, post.longitude) <= filters.maxDistanceKm)
                    }
                    if (filtered.isEmpty()) {
                        item {
                            UnavailableDataCard(
                                title = "No Open Team-Ups Here Yet",
                                explanation = "Post a real match (Football, Padel, Tennis), cultural visit (Museum, Beach), treasure hunt, or beach cleanup — NILORA never invents fake participants.",
                                icon = Icons.Filled.SportsSoccer,
                                actionLabel = "Create Team-Up",
                                onActionClick = { showCreateDialog = true }
                            )
                        }
                    } else {
                        items(filtered, key = { it.id }) { post ->
                            TeamActivityCard(post = post, isJoined = currentUserId in post.participantIds) {
                                onOpenActivityDetail(post.id)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        CreateTeamActivityDialog(
            defaultCity = if (filters.selectedCity == "ALL") "Cairo" else filters.selectedCity,
            onDismiss = { showCreateDialog = false },
            onSubmit = { dom, sub, title, desc, city, country, spot, dateLbl, skill, equip, safety, policy, maxP, org ->
                onCreateTeamActivity(dom, sub, title, desc, city, country, spot, dateLbl, skill, equip, safety, policy, maxP, org)
                showCreateDialog = false
            }
        )
    }
}

@Composable
private fun TeamActivityCard(post: TeamActivityPost, isJoined: Boolean, onClick: () -> Unit) {
    val domain = ActivityDomain.fromKey(post.domain)
    val sub = ActivitySubCategory.fromKey(post.subCategory)
    val status = TeamUpStatus.fromKey(post.teamStatus)
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("team_activity_card_${post.id}"),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, if (isJoined) NileTurquoise else MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(color = EgyptianGoldContainer, shape = RoundedCornerShape(999.dp)) {
                    Text(
                        text = "${domain.labelEn} • ${sub.labelEn}",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = EgyptianGoldOnContainer,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
                Text(
                    text = "${post.participantIds.size}/${post.maxParticipants} • ${status.labelEn}",
                    style = MaterialTheme.typography.labelSmall,
                    color = NileTurquoiseDark,
                    fontWeight = FontWeight.Bold
                )
            }
            Text(post.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(
                "${post.cityName} • Public Spot: ${post.publicMeetingSpot} • ${post.scheduledDateLabel}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (domain == ActivityDomain.COMMUNITY_IMPACT) {
                Text(
                    text = if (post.impactVerifiedByAdmin) "Admin-Verified Impact Initiative (${post.impactOrganizationName})"
                    else "Community Initiative (${post.impactOrganizationName.ifBlank { "Pending Admin Review" }})",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (post.impactVerifiedByAdmin) VerifiedEmerald else WarningAmber,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Text(post.description, style = MaterialTheme.typography.bodySmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
fun TeamActivityDetailScreen(
    activity: TeamActivityPost?,
    currentUserId: String,
    messages: List<ActivityGroupMessage>,
    onJoinOrRequest: (TeamActivityPost) -> Unit,
    onLeave: (TeamActivityPost) -> Unit,
    onApproveRequest: (TeamActivityPost, String, Boolean) -> Unit,
    onUpdateStatus: (String, TeamUpStatus) -> Unit,
    onSendMessage: (String, String) -> Unit,
    onTranslateAndSpeak: (String, ActivityGroupMessage) -> Unit,
    onReport: (String, String) -> Unit,
    onBlockOrganizer: (String, String) -> Unit,
    onBack: () -> Unit,
    onOpenLiveTranslator: () -> Unit = {},
    onOpenCrews: () -> Unit = {}
) {
    var chatInput by remember { mutableStateOf("") }
    var showReportDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            NiloraTopBar(
                title = activity?.title ?: "Team-Up Activity",
                subtitle = activity?.let { "${it.cityName} • ${it.publicMeetingSpot}" } ?: "",
                onBackClick = onBack,
                actions = {
                    if (activity != null) {
                        IconButton(onClick = { showReportDialog = true }, modifier = Modifier.size(48.dp)) {
                            Icon(Icons.Filled.Flag, contentDescription = "Report Activity")
                        }
                    }
                }
            )
        }
    ) { padding ->
        if (activity == null) {
            Box(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
                UnavailableDataCard("Activity Not Found", "This activity may have been cancelled or removed.", Icons.Filled.EventBusy, "Back", onBack)
            }
            return@Scaffold
        }
        val isCreator = activity.creatorId == currentUserId
        val isParticipant = currentUserId in activity.participantIds
        val isPending = currentUserId in activity.pendingRequestIds

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            "${ActivityDomain.fromKey(activity.domain).labelEn} • ${TeamUpStatus.fromKey(activity.teamStatus).labelEn}",
                            style = MaterialTheme.typography.labelLarge,
                            color = NileTurquoiseDark,
                            fontWeight = FontWeight.Bold
                        )
                        Text(activity.description, style = MaterialTheme.typography.bodyMedium)
                        HorizontalDivider()
                        Text("Organizer: ${activity.creatorName} • Skill: ${ActivitySkillLevel.fromKey(activity.skillLevel).labelEn}", style = MaterialTheme.typography.bodySmall)
                        Text("When: ${activity.scheduledDateLabel} • Public Venue: ${activity.publicMeetingSpot}", style = MaterialTheme.typography.bodySmall)
                        if (activity.equipmentNote.isNotBlank()) {
                            Text("Equipment: ${activity.equipmentNote}", style = MaterialTheme.typography.bodySmall)
                        }
                        Text("Safety Rules: ${activity.safetyRules}", style = MaterialTheme.typography.bodySmall, color = WarningAmber)
                        if (!isCreator) {
                            TextButton(onClick = { onBlockOrganizer(activity.creatorId, activity.creatorName) }) {
                                Text("Block Organizer", color = ErrorCoral)
                            }
                        }
                    }
                }
            }

            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (!isParticipant) {
                        Button(
                            onClick = { onJoinOrRequest(activity) },
                            enabled = !isPending,
                            modifier = Modifier.weight(1f).defaultMinSize(minHeight = 48.dp).testTag("team_activity_join_btn")
                        ) {
                            Text(
                                when {
                                    isPending -> "Request Pending"
                                    activity.joinPolicy == "REQUEST_APPROVAL" -> "Request to Join"
                                    else -> "Join Team (${activity.participantIds.size}/${activity.maxParticipants})"
                                }
                            )
                        }
                    } else if (!isCreator) {
                        OutlinedButton(
                            onClick = { onLeave(activity) },
                            modifier = Modifier.weight(1f).defaultMinSize(minHeight = 48.dp)
                        ) { Text("Leave Activity") }
                    }
                }
                Spacer(Modifier.height(6.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilledTonalButton(
                        onClick = onOpenLiveTranslator,
                        modifier = Modifier.weight(1f).defaultMinSize(minHeight = 48.dp).testTag("team_open_live_btn")
                    ) {
                        Icon(Icons.Filled.Translate, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Live Subtitles", maxLines = 1)
                    }
                    FilledTonalButton(
                        onClick = onOpenCrews,
                        modifier = Modifier.weight(1f).defaultMinSize(minHeight = 48.dp).testTag("team_open_crews_btn")
                    ) {
                        Icon(Icons.Filled.Diversity3, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("NILORA Crews", maxLines = 1)
                    }
                }
            }

            if (isCreator) {
                item {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(
                            TeamUpStatus.ACTIVITY_CONFIRMED,
                            TeamUpStatus.ACTIVITY_COMPLETED,
                            TeamUpStatus.ACTIVITY_CANCELLED
                        ).forEach { st ->
                            item {
                                FilterChip(
                                    selected = activity.teamStatus == st.key,
                                    onClick = { onUpdateStatus(activity.id, st) },
                                    label = { Text(st.labelEn) }
                                )
                            }
                        }
                    }
                }
                if (activity.pendingRequestIds.isNotEmpty()) {
                    item { Text("Pending Teammate Requests (${activity.pendingRequestIds.size})", style = MaterialTheme.typography.titleMedium) }
                    items(activity.pendingRequestIds) { reqId ->
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Member ${reqId.take(8)}...", style = MaterialTheme.typography.bodyMedium)
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Button(onClick = { onApproveRequest(activity, reqId, true) }) { Text("Accept") }
                                    OutlinedButton(onClick = { onApproveRequest(activity, reqId, false) }) { Text("Decline") }
                                }
                            }
                        }
                    }
                }
            }

            if (isParticipant) {
                item { Text("Team Group Chat (${messages.size})", style = MaterialTheme.typography.titleMedium) }
                items(messages, key = { it.id }) { m ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("${m.senderName} (${m.sourceLanguage})", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                                TextButton(onClick = { onTranslateAndSpeak(activity.id, m) }) { Text("Translate & Speak", style = MaterialTheme.typography.labelSmall) }
                            }
                            Text(m.originalText, style = MaterialTheme.typography.bodyMedium)
                            m.translations.values.firstOrNull()?.let { tr ->
                                Text(tr, style = MaterialTheme.typography.bodySmall, color = NileTurquoiseDark)
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
                            value = chatInput,
                            onValueChange = { chatInput = it },
                            placeholder = { Text("Coordinate public meetup...") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        FilledIconButton(
                            onClick = {
                                if (chatInput.isNotBlank()) {
                                    onSendMessage(activity.id, chatInput)
                                    chatInput = ""
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

    if (showReportDialog && activity != null) {
        var reason by remember { mutableStateOf("Unsafe meetup or spam") }
        AlertDialog(
            onDismissRequest = { showReportDialog = false },
            title = { Text("Report Team Activity") },
            text = { OutlinedTextField(value = reason, onValueChange = { reason = it }, label = { Text("Reason") }) },
            confirmButton = {
                Button(onClick = { onReport(activity.id, reason); showReportDialog = false }) { Text("Report") }
            },
            dismissButton = { TextButton(onClick = { showReportDialog = false }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun CreateTeamActivityDialog(
    defaultCity: String,
    onDismiss: () -> Unit,
    onSubmit: (
        domain: ActivityDomain,
        sub: ActivitySubCategory,
        title: String,
        desc: String,
        city: String,
        country: String,
        spot: String,
        dateLabel: String,
        skill: ActivitySkillLevel,
        equip: String,
        safety: String,
        policy: String,
        maxP: Int,
        impactOrg: String
    ) -> Unit
) {
    var domain by remember { mutableStateOf(ActivityDomain.PLAY_SPORTS) }
    var sub by remember { mutableStateOf(ActivitySubCategory.PADEL) }
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var city by remember { mutableStateOf(defaultCity) }
    var spot by remember { mutableStateOf("") }
    var dateLabel by remember { mutableStateOf("Friday 18:00") }
    var skill by remember { mutableStateOf(ActivitySkillLevel.ALL_LEVELS) }
    var equipment by remember { mutableStateOf("") }
    var policy by remember { mutableStateOf("PUBLIC") }
    var maxParticipantsText by remember { mutableStateOf("6") }
    var impactOrg by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create Go / Play / Team-Up") },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.heightIn(max = 380.dp)) {
                item {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(ActivityDomain.entries) { d ->
                            FilterChip(
                                selected = domain == d,
                                onClick = {
                                    domain = d
                                    sub = ActivitySubCategory.forDomain(d).first()
                                },
                                label = { Text(d.labelEn) }
                            )
                        }
                    }
                }
                item {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(ActivitySubCategory.forDomain(domain)) { s ->
                            FilterChip(selected = sub == s, onClick = { sub = s }, label = { Text(s.labelEn) })
                        }
                    }
                }
                item { OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Activity Title*") }, modifier = Modifier.fillMaxWidth()) }
                item { OutlinedTextField(value = city, onValueChange = { city = it }, label = { Text("City*") }, modifier = Modifier.fillMaxWidth()) }
                item { OutlinedTextField(value = spot, onValueChange = { spot = it }, label = { Text("Public Meeting Spot / Court*") }, modifier = Modifier.fillMaxWidth()) }
                item { OutlinedTextField(value = dateLabel, onValueChange = { dateLabel = it }, label = { Text("Date & Time*") }, modifier = Modifier.fillMaxWidth()) }
                item { OutlinedTextField(value = maxParticipantsText, onValueChange = { maxParticipantsText = it.filter(Char::isDigit) }, label = { Text("Max Participants (2-50)") }, modifier = Modifier.fillMaxWidth()) }
                item { OutlinedTextField(value = equipment, onValueChange = { equipment = it }, label = { Text("Equipment Note (Optional)") }, modifier = Modifier.fillMaxWidth()) }
                if (domain == ActivityDomain.COMMUNITY_IMPACT) {
                    item { OutlinedTextField(value = impactOrg, onValueChange = { impactOrg = it }, label = { Text("Organizer / NGO Name (Optional)") }, modifier = Modifier.fillMaxWidth()) }
                }
                item { OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("Description*") }, minLines = 2, modifier = Modifier.fillMaxWidth()) }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("PUBLIC" to "Public Join", "REQUEST_APPROVAL" to "Approval").forEach { (p, l) ->
                            FilterChip(selected = policy == p, onClick = { policy = p }, label = { Text(l) })
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val hub = GlobalCityHubCatalog.findHub(city)
                    val maxP = (maxParticipantsText.toIntOrNull() ?: 6).coerceIn(2, 50)
                    onSubmit(
                        domain, sub, title, description, city, hub?.countryName ?: "Egypt",
                        spot, dateLabel, skill, equipment,
                        "Meet in a well-lit public venue only. Never share private home addresses.",
                        policy, maxP, impactOrg
                    )
                },
                enabled = title.trim().length >= 2 && spot.trim().length >= 2 && description.trim().length >= 2
            ) { Text("Publish") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
