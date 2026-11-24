package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.automirrored.filled.Send
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
import com.example.data.repository.GpsCoordinates
import com.example.localization.AppStrings
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.RealityFilterState

@Composable
fun RealityPulseEventsScreen(
    strings: AppStrings,
    currentUserId: String,
    isAdmin: Boolean,
    filters: RealityFilterState,
    localGps: GpsCoordinates?,
    eventsState: UiState<List<RealityEvent>>,
    externalEvents: LiveIntelligenceResult<List<ExternalOpenSourceEventNote>>?,
    isLoadingExternal: Boolean,
    blockedIds: Set<String>,
    onSelectCity: (String) -> Unit,
    onSelectCategory: (RealityEventCategory?) -> Unit,
    onSelectVerification: (EventVerificationTier?) -> Unit,
    onToggleFreeOnly: () -> Unit,
    onToggleUpcomingOnly: () -> Unit,
    onRefreshExternal: (String, String) -> Unit,
    onCreateEvent: (
        title: String,
        cat: RealityEventCategory,
        desc: String,
        city: String,
        country: String,
        code: String,
        venue: String,
        dateIso: String,
        timeWindow: String,
        freeVerified: Boolean,
        priceNote: String,
        organizer: String,
        orgType: EventOrganizerType,
        url: String,
        conditions: String,
        safety: String,
        maxP: Int,
        joinPolicy: String
    ) -> Unit,
    onOpenEvent: (String) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var showCreateDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            NiloraTopBar(
                title = strings.realityEventsTitle,
                subtitle = "Verified, User-Submitted & Open-Source Provenance",
                onBackClick = onBack,
                actions = {
                    IconButton(
                        onClick = {
                            val hub = GlobalCityHubCatalog.findHub(filters.selectedCity)
                            onRefreshExternal(hub?.cityName ?: "Cairo", hub?.countryName ?: "Egypt")
                        },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(Icons.Filled.Refresh, contentDescription = "Refresh Open-Source Events")
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showCreateDialog = true },
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text("Publish Event") },
                modifier = Modifier.testTag("create_reality_event_btn")
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .testTag("reality_events_list"),
            contentPadding = PaddingValues(16.dp, 12.dp, 16.dp, 88.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Surface(color = WarningAmberContainer, shape = RoundedCornerShape(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.VerifiedUser, contentDescription = null, tint = WarningAmber)
                        Text(
                            text = "Honest Provenance: NILORA marks Admin-Verified events, Unverified User-Submitted events, and Open-Source Wikimedia references separately and never fabricates ticket prices or crowd counts.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MidnightNavy
                        )
                    }
                }
            }

            // City Hub Filter
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    item {
                        FilterChip(
                            selected = filters.selectedCity == "ALL",
                            onClick = { onSelectCity("ALL") },
                            label = { Text("All Cities") }
                        )
                    }
                    items(GlobalCityHubCatalog.hubs) { hub ->
                        FilterChip(
                            selected = filters.selectedCity.equals(hub.cityName, ignoreCase = true),
                            onClick = { onSelectCity(hub.cityName) },
                            label = { Text(hub.cityName) }
                        )
                    }
                }
            }

            // Category & Provenance Filters
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    item {
                        FilterChip(
                            selected = filters.freeVerifiedOnly,
                            onClick = onToggleFreeOnly,
                            label = { Text("Free Verified") },
                            modifier = Modifier.testTag("events_filter_free")
                        )
                    }
                    item {
                        FilterChip(
                            selected = filters.upcomingOrActiveOnly,
                            onClick = onToggleUpcomingOnly,
                            label = { Text("Upcoming / Active") }
                        )
                    }
                    item {
                        FilterChip(
                            selected = filters.eventVerificationFilter == EventVerificationTier.ADMIN_VERIFIED,
                            onClick = {
                                onSelectVerification(
                                    if (filters.eventVerificationFilter == EventVerificationTier.ADMIN_VERIFIED) null
                                    else EventVerificationTier.ADMIN_VERIFIED
                                )
                            },
                            label = { Text("Admin Verified Only") }
                        )
                    }
                    items(RealityEventCategory.entries.take(8)) { cat ->
                        FilterChip(
                            selected = filters.eventCategory == cat,
                            onClick = { onSelectCategory(if (filters.eventCategory == cat) null else cat) },
                            label = { Text(cat.labelEn) }
                        )
                    }
                }
            }

            // Live Firestore Events
            when (eventsState) {
                is UiState.Loading -> item { NiloraLoadingState("Loading Reality Events...") }
                is UiState.Error -> item { NiloraErrorState("Events Unavailable", eventsState.message) }
                is UiState.Success -> {
                    val filtered = eventsState.data.filter { ev ->
                        ev.creatorId !in blockedIds &&
                            (filters.selectedCity == "ALL" || ev.cityName.equals(filters.selectedCity, true)) &&
                            (filters.eventCategory == null || ev.category == filters.eventCategory.key) &&
                            (filters.eventVerificationFilter == null || ev.verificationTier == filters.eventVerificationFilter.key) &&
                            (!filters.freeVerifiedOnly || ev.freeVerified) &&
                            (!filters.upcomingOrActiveOnly || ev.status in setOf("UPCOMING", "ACTIVE")) &&
                            (filters.maxDistanceKm == null || localGps == null || ev.latitude == null || ev.longitude == null ||
                                geoDistanceKm(localGps.latitude, localGps.longitude, ev.latitude, ev.longitude) <= filters.maxDistanceKm)
                    }
                    if (filtered.isEmpty()) {
                        item {
                            UnavailableDataCard(
                                title = "No Matching Firestore Events Yet",
                                explanation = "Publish a real community, cultural, sports, or festival listing with venue, schedule, and official organizer details.",
                                icon = Icons.Filled.EventAvailable,
                                actionLabel = "Publish Event",
                                onActionClick = { showCreateDialog = true }
                            )
                        }
                    } else {
                        items(filtered, key = { it.id }) { ev ->
                            RealityEventCard(event = ev, isMine = currentUserId in ev.participantIds) {
                                onOpenEvent(ev.id)
                            }
                        }
                    }
                }
            }

            // External Open-Source Wikimedia Cultural References
            item {
                Spacer(Modifier.height(4.dp))
                NiloraSectionHeader(
                    title = "Open-Source Cultural & Festival Reference",
                    subtitle = "External Wikipedia API (CC BY-SA 4.0) — confirm official live schedule"
                )
            }
            if (isLoadingExternal) {
                item { NiloraLoadingState("Fetching open-source cultural references...") }
            } else {
                when (externalEvents) {
                    is LiveIntelligenceResult.Available -> {
                        items(externalEvents.data, key = { it.id }) { note ->
                            Card(
                                modifier = Modifier.fillMaxWidth().testTag("external_event_card_${note.id}"),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                            ) {
                                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Surface(color = SurfaceIvoryMuted, shape = RoundedCornerShape(999.dp)) {
                                        Text(
                                            text = "EXTERNAL_OPEN_SOURCE • ${note.cityName}",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                        )
                                    }
                                    Text(note.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                    Text(note.summary, style = MaterialTheme.typography.bodySmall, maxLines = 3, overflow = TextOverflow.Ellipsis)
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(note.attribution, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
                                        TextButton(
                                            onClick = {
                                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(note.sourceUrl)))
                                            }
                                        ) { Text("Source") }
                                    }
                                }
                            }
                        }
                    }
                    is LiveIntelligenceResult.Unavailable -> {
                        item { UnavailableDataCard("Open-Source Reference Unavailable", externalEvents.reason, Icons.Filled.CloudOff) }
                    }
                    null -> {}
                }
            }
        }
    }

    if (showCreateDialog) {
        CreateRealityEventDialog(
            defaultCity = if (filters.selectedCity == "ALL") "Cairo" else filters.selectedCity,
            onDismiss = { showCreateDialog = false },
            onSubmit = { title, cat, desc, city, country, code, venue, dateIso, timeWindow, freeVerified, priceNote, organizer, orgType, url, conditions, safety, maxP, policy ->
                onCreateEvent(title, cat, desc, city, country, code, venue, dateIso, timeWindow, freeVerified, priceNote, organizer, orgType, url, conditions, safety, maxP, policy)
                showCreateDialog = false
            }
        )
    }
}

@Composable
private fun RealityEventCard(event: RealityEvent, isMine: Boolean, onClick: () -> Unit) {
    val tier = EventVerificationTier.fromKey(event.verificationTier)
    val badgeBg = when (tier) {
        EventVerificationTier.ADMIN_VERIFIED -> VerifiedEmeraldContainer
        EventVerificationTier.UNVERIFIED_USER_SUBMITTED -> WarningAmberContainer
        EventVerificationTier.EXTERNAL_OPEN_SOURCE -> SurfaceIvoryMuted
    }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("reality_event_card_${event.id}"),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, if (isMine) NileTurquoise else MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(color = badgeBg, shape = RoundedCornerShape(999.dp)) {
                    Text(
                        text = tier.labelEn,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MidnightNavy,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
                Text(
                    text = "${event.participantIds.size}/${event.maxParticipants} joined",
                    style = MaterialTheme.typography.labelSmall,
                    color = NileTurquoiseDark,
                    fontWeight = FontWeight.Bold
                )
            }
            Text(event.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(
                "${RealityEventCategory.fromKey(event.category).labelEn} • ${event.cityName}, ${event.countryName} • ${event.venueName}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                "${event.startDateIso}${if (event.timeWindowLabel.isNotBlank()) " (${event.timeWindowLabel})" else ""} • ${if (event.freeVerified) "Free Verified" else event.priceNote}",
                style = MaterialTheme.typography.labelMedium,
                color = EgyptianGoldDark
            )
            Text(event.description, style = MaterialTheme.typography.bodySmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
fun RealityEventDetailScreen(
    event: RealityEvent?,
    currentUserId: String,
    messages: List<RealityEventMessage>,
    onJoinOrRequest: (RealityEvent) -> Unit,
    onLeave: (RealityEvent) -> Unit,
    onApproveRequest: (RealityEvent, String, Boolean) -> Unit,
    onUpdateStatus: (String, String) -> Unit,
    onSendMessage: (String, String) -> Unit,
    onTranslateAndSpeak: (String, RealityEventMessage) -> Unit,
    onReport: (String, String) -> Unit,
    onBack: () -> Unit,
    onOpenLiveTranslator: () -> Unit = {},
    onOpenMap: () -> Unit = {}
) {
    val context = LocalContext.current
    var chatInput by remember { mutableStateOf("") }
    var showReportDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            NiloraTopBar(
                title = event?.title ?: "Reality Event",
                subtitle = event?.let { "${it.cityName} • ${it.venueName}" } ?: "",
                onBackClick = onBack,
                actions = {
                    if (event != null) {
                        IconButton(onClick = { showReportDialog = true }, modifier = Modifier.size(48.dp)) {
                            Icon(Icons.Filled.Flag, contentDescription = "Report Event")
                        }
                    }
                }
            )
        }
    ) { padding ->
        if (event == null) {
            Box(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
                UnavailableDataCard("Event Not Found", "This event may have been removed.", Icons.Filled.EventBusy, "Back", onBack)
            }
            return@Scaffold
        }
        val isCreator = event.creatorId == currentUserId
        val isParticipant = currentUserId in event.participantIds
        val isPending = currentUserId in event.pendingRequestIds
        val tier = EventVerificationTier.fromKey(event.verificationTier)

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(tier.labelEn, style = MaterialTheme.typography.labelLarge, color = EgyptianGoldDark, fontWeight = FontWeight.Bold)
                        Text(event.description, style = MaterialTheme.typography.bodyMedium)
                        HorizontalDivider()
                        Text("Organizer: ${event.organizerName} (${event.organizerType})", style = MaterialTheme.typography.bodySmall)
                        Text("Schedule: ${event.startDateIso} ${event.timeWindowLabel}", style = MaterialTheme.typography.bodySmall)
                        Text("Entry: ${if (event.freeVerified) "Free Verified" else event.priceNote}", style = MaterialTheme.typography.bodySmall)
                        if (event.conditionsNote.isNotBlank()) {
                            Text("Conditions: ${event.conditionsNote}", style = MaterialTheme.typography.bodySmall)
                        }
                        Text("Safety: ${event.safetyNote}", style = MaterialTheme.typography.bodySmall, color = WarningAmber)
                        if (event.officialUrl.startsWith("http")) {
                            TextButton(
                                onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(event.officialUrl))) }
                            ) {
                                Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Official Organizer Link")
                            }
                        }
                    }
                }
            }

            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (!isParticipant) {
                        Button(
                            onClick = { onJoinOrRequest(event) },
                            enabled = !isPending,
                            modifier = Modifier.weight(1f).defaultMinSize(minHeight = 48.dp).testTag("event_join_btn")
                        ) {
                            Text(
                                when {
                                    isPending -> "Approval Pending"
                                    event.joinPolicy == "REQUEST_APPROVAL" -> "Request to Join"
                                    else -> "Join Event (${event.participantIds.size}/${event.maxParticipants})"
                                }
                            )
                        }
                    } else if (!isCreator) {
                        OutlinedButton(
                            onClick = { onLeave(event) },
                            modifier = Modifier.weight(1f).defaultMinSize(minHeight = 48.dp)
                        ) { Text("Leave Event") }
                    } else {
                        OutlinedButton(
                            onClick = { onUpdateStatus(event.id, if (event.status == "CANCELLED") "UPCOMING" else "CANCELLED") },
                            modifier = Modifier.weight(1f).defaultMinSize(minHeight = 48.dp)
                        ) { Text(if (event.status == "CANCELLED") "Reopen Event" else "Cancel Event") }
                    }
                }
                Spacer(Modifier.height(6.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilledTonalButton(
                        onClick = onOpenLiveTranslator,
                        modifier = Modifier.weight(1f).defaultMinSize(minHeight = 48.dp).testTag("event_open_live_btn")
                    ) {
                        Icon(Icons.Filled.Translate, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Live Subtitles", maxLines = 1)
                    }
                    FilledTonalButton(
                        onClick = onOpenMap,
                        modifier = Modifier.weight(1f).defaultMinSize(minHeight = 48.dp).testTag("event_open_map_btn")
                    ) {
                        Icon(Icons.Filled.Map, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Pulse Map", maxLines = 1)
                    }
                }
            }

            if (isCreator && event.pendingRequestIds.isNotEmpty()) {
                item { Text("Pending Requests (${event.pendingRequestIds.size})", style = MaterialTheme.typography.titleMedium) }
                items(event.pendingRequestIds) { reqId ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("User ${reqId.take(8)}...", style = MaterialTheme.typography.bodyMedium)
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Button(onClick = { onApproveRequest(event, reqId, true) }) { Text("Approve") }
                                OutlinedButton(onClick = { onApproveRequest(event, reqId, false) }) { Text("Decline") }
                            }
                        }
                    }
                }
            }

            if (isParticipant) {
                item {
                    Text("Event Participant Group Chat (${messages.size})", style = MaterialTheme.typography.titleMedium)
                }
                items(messages, key = { it.id }) { m ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("${m.senderName} (${m.sourceLanguage})", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                                TextButton(onClick = { onTranslateAndSpeak(event.id, m) }) {
                                    Text("Translate & Speak", style = MaterialTheme.typography.labelSmall)
                                }
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
                            placeholder = { Text("Coordinate arrival at public venue...") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        FilledIconButton(
                            onClick = {
                                if (chatInput.isNotBlank()) {
                                    onSendMessage(event.id, chatInput)
                                    chatInput = ""
                                }
                            },
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send message")
                        }
                    }
                }
            }
        }
    }

    if (showReportDialog && event != null) {
        var reason by remember { mutableStateOf("Misleading schedule or unsafe venue") }
        AlertDialog(
            onDismissRequest = { showReportDialog = false },
            title = { Text("Report Event") },
            text = {
                OutlinedTextField(value = reason, onValueChange = { reason = it }, label = { Text("Reason") }, modifier = Modifier.fillMaxWidth())
            },
            confirmButton = {
                Button(onClick = { onReport(event.id, reason); showReportDialog = false }) { Text("Submit Report") }
            },
            dismissButton = { TextButton(onClick = { showReportDialog = false }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun CreateRealityEventDialog(
    defaultCity: String,
    onDismiss: () -> Unit,
    onSubmit: (
        title: String,
        cat: RealityEventCategory,
        desc: String,
        city: String,
        country: String,
        code: String,
        venue: String,
        dateIso: String,
        timeWindow: String,
        freeVerified: Boolean,
        priceNote: String,
        organizer: String,
        orgType: EventOrganizerType,
        url: String,
        conditions: String,
        safety: String,
        maxP: Int,
        joinPolicy: String
    ) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var category by remember { mutableStateOf(RealityEventCategory.CULTURAL) }
    var description by remember { mutableStateOf("") }
    var city by remember { mutableStateOf(defaultCity) }
    var venue by remember { mutableStateOf("") }
    var dateIso by remember { mutableStateOf("2026-12-01") }
    var timeWindow by remember { mutableStateOf("18:00 - 20:00") }
    var freeVerified by remember { mutableStateOf(true) }
    var priceNote by remember { mutableStateOf("") }
    var organizer by remember { mutableStateOf("") }
    var officialUrl by remember { mutableStateOf("") }
    var joinPolicy by remember { mutableStateOf("OPEN") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Publish Reality Event") },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.heightIn(max = 380.dp)) {
                item {
                    Text(
                        "Published as Unverified / User Submitted until verified by a NILORA Administrator.",
                        style = MaterialTheme.typography.labelSmall,
                        color = WarningAmber
                    )
                }
                item { OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Event Title*") }, modifier = Modifier.fillMaxWidth()) }
                item {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(RealityEventCategory.entries) { c ->
                            FilterChip(selected = category == c, onClick = { category = c }, label = { Text(c.labelEn) })
                        }
                    }
                }
                item { OutlinedTextField(value = city, onValueChange = { city = it }, label = { Text("City*") }, modifier = Modifier.fillMaxWidth()) }
                item { OutlinedTextField(value = venue, onValueChange = { venue = it }, label = { Text("Public Venue Name*") }, modifier = Modifier.fillMaxWidth()) }
                item { OutlinedTextField(value = dateIso, onValueChange = { dateIso = it }, label = { Text("Start Date (YYYY-MM-DD)*") }, modifier = Modifier.fillMaxWidth()) }
                item { OutlinedTextField(value = timeWindow, onValueChange = { timeWindow = it }, label = { Text("Time Window") }, modifier = Modifier.fillMaxWidth()) }
                item { OutlinedTextField(value = organizer, onValueChange = { organizer = it }, label = { Text("Organizer Name*") }, modifier = Modifier.fillMaxWidth()) }
                item { OutlinedTextField(value = officialUrl, onValueChange = { officialUrl = it }, label = { Text("Official URL (Optional)") }, modifier = Modifier.fillMaxWidth()) }
                item { OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("Factual Description*") }, minLines = 2, modifier = Modifier.fillMaxWidth()) }
                item {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Checkbox(checked = freeVerified, onCheckedChange = { freeVerified = it })
                        Text("Free entry confirmed", style = MaterialTheme.typography.bodySmall)
                    }
                }
                if (!freeVerified) {
                    item { OutlinedTextField(value = priceNote, onValueChange = { priceNote = it }, label = { Text("Official Price / Ticket Note") }, modifier = Modifier.fillMaxWidth()) }
                }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(selected = joinPolicy == "OPEN", onClick = { joinPolicy = "OPEN" }, label = { Text("Open Join") })
                        FilterChip(selected = joinPolicy == "REQUEST_APPROVAL", onClick = { joinPolicy = "REQUEST_APPROVAL" }, label = { Text("Approval Required") })
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val hub = GlobalCityHubCatalog.findHub(city)
                    onSubmit(
                        title, category, description, city,
                        hub?.countryName ?: "Egypt", hub?.countryCode ?: "EG",
                        venue, dateIso, timeWindow, freeVerified,
                        if (freeVerified) "Free entry" else priceNote.ifBlank { "Check official site" },
                        organizer.ifBlank { "Community Organizer" }, EventOrganizerType.USER_ORGANIZER,
                        officialUrl, "", "Public venue meetup — verify local schedule.", 30, joinPolicy
                    )
                },
                enabled = title.trim().length >= 2 && venue.trim().isNotEmpty() && description.trim().length >= 2
            ) { Text("Publish") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
