package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.*
import com.example.ui.components.BookingStatusChip
import com.example.ui.components.NiloraFooter
import com.example.ui.components.NiloraTopBar
import com.example.ui.components.UnavailableDataCard
import com.example.ui.components.VerifiedSourceBadge
import com.example.ui.theme.ErrorCoral
import com.example.ui.theme.TurquoiseContainer
import com.example.ui.theme.TurquoiseOnContainer
import com.example.ui.theme.VerifiedEmeraldContainer

@Composable
fun AdminDashboardScreen(
    isAdmin: Boolean,
    currentUserId: String,
    isGoogleMapsKeyConfigured: Boolean,
    isOnline: Boolean,
    destinationsState: UiState<List<Destination>>,
    activitiesState: UiState<List<ActivityListing>>,
    providersState: UiState<List<ProviderProfile>>,
    bookingsState: UiState<List<BookingRecord>>,
    reportsState: UiState<List<ContentReport>>,
    auditLogsState: UiState<List<AuditLog>>,
    onPublishVerifiedDestination: (Destination) -> Unit,
    onPublishVerifiedActivity: (ActivityListing) -> Unit,
    onModerateDestination: (id: String, status: String) -> Unit,
    onModerateActivity: (id: String, status: String) -> Unit,
    onModerateProvider: (id: String, status: String, reason: String) -> Unit,
    onResolveReport: (id: String, status: String) -> Unit,
    onBack: () -> Unit,
    realityEventsState: UiState<List<RealityEvent>> = UiState.Success(emptyList()),
    teamActivitiesState: UiState<List<TeamActivityPost>> = UiState.Success(emptyList()),
    onModerateRealityEvent: (id: String, tier: EventVerificationTier) -> Unit = { _, _ -> },
    onModerateImpactActivity: (id: String, verified: Boolean) -> Unit = { _, _ -> }
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Overview", "Publish Verified", "Moderation", "Integrations & Logs")

    Scaffold(
        topBar = {
            NiloraTopBar(
                title = "NILORA Admin Dashboard",
                subtitle = "Role-gated verification & governance",
                onBackClick = onBack
            )
        }
    ) { padding ->
        if (!isAdmin) {
            Box(modifier = Modifier.fillMaxSize().padding(padding).padding(20.dp)) {
                UnavailableDataCard(
                    title = "Administrator Access Required",
                    explanation = "Only verified NILORA administrators (${NiloraTaxonomy.FOUNDER_ATTRIBUTION}) can moderate listings, approve providers, or view audit logs.",
                    icon = Icons.Filled.Lock,
                    actionLabel = "Go Back",
                    onActionClick = onBack
                )
            }
            return@Scaffold
        }

        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            ScrollableTabRow(selectedTabIndex = selectedTab) {
                tabs.forEachIndexed { idx, label ->
                    Tab(
                        selected = selectedTab == idx,
                        onClick = { selectedTab = idx },
                        text = { Text(label) },
                        modifier = Modifier.testTag("admin_tab_$idx")
                    )
                }
            }

            when (selectedTab) {
                0 -> AdminOverviewTab(destinationsState, activitiesState, providersState, bookingsState, reportsState)
                1 -> AdminPublishTab(currentUserId, onPublishVerifiedDestination, onPublishVerifiedActivity)
                2 -> AdminModerationTab(
                    destinationsState = destinationsState,
                    activitiesState = activitiesState,
                    providersState = providersState,
                    bookingsState = bookingsState,
                    reportsState = reportsState,
                    realityEventsState = realityEventsState,
                    teamActivitiesState = teamActivitiesState,
                    onModerateDestination = onModerateDestination,
                    onModerateActivity = onModerateActivity,
                    onModerateProvider = onModerateProvider,
                    onResolveReport = onResolveReport,
                    onModerateRealityEvent = onModerateRealityEvent,
                    onModerateImpactActivity = onModerateImpactActivity
                )
                3 -> AdminIntegrationsLogsTab(isOnline, isGoogleMapsKeyConfigured, auditLogsState)
            }
        }
    }
}

@Composable
private fun AdminOverviewTab(
    destinationsState: UiState<List<Destination>>,
    activitiesState: UiState<List<ActivityListing>>,
    providersState: UiState<List<ProviderProfile>>,
    bookingsState: UiState<List<BookingRecord>>,
    reportsState: UiState<List<ContentReport>>
) {
    val dests = (destinationsState as? UiState.Success)?.data.orEmpty()
    val acts = (activitiesState as? UiState.Success)?.data.orEmpty()
    val provs = (providersState as? UiState.Success)?.data.orEmpty()
    val books = (bookingsState as? UiState.Success)?.data.orEmpty()
    val reps = (reportsState as? UiState.Success)?.data.orEmpty()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                "Real-Time Platform Metrics (Live Firestore Counts Only)",
                style = MaterialTheme.typography.titleMedium
            )
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                AdminMetricCard("Approved Destinations", dests.count { it.verificationStatus == "APPROVED" }.toString(), Modifier.weight(1f))
                AdminMetricCard("Pending Destinations", dests.count { it.verificationStatus == "PENDING" }.toString(), Modifier.weight(1f))
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                AdminMetricCard("Approved Providers", provs.count { it.verificationStatus == "APPROVED" }.toString(), Modifier.weight(1f))
                AdminMetricCard("Pending Providers", provs.count { it.verificationStatus == "PENDING" }.toString(), Modifier.weight(1f))
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                AdminMetricCard("Activities", acts.size.toString(), Modifier.weight(1f))
                AdminMetricCard("Total Bookings", books.size.toString(), Modifier.weight(1f))
            }
        }
        item {
            AdminMetricCard("Open Moderation Reports", reps.count { it.status == "OPEN" }.toString(), Modifier.fillMaxWidth())
        }
        item { NiloraFooter() }
    }
}

@Composable
private fun AdminMetricCard(title: String, value: String, modifier: Modifier = Modifier) {
    Card(modifier = modifier, colors = CardDefaults.cardColors(containerColor = TurquoiseContainer)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(value, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = TurquoiseOnContainer)
            Text(title, style = MaterialTheme.typography.labelLarge, color = TurquoiseOnContainer)
        }
    }
}

@Composable
private fun AdminPublishTab(
    currentUserId: String,
    onPublishVerifiedDestination: (Destination) -> Unit,
    onPublishVerifiedActivity: (ActivityListing) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var city by remember { mutableStateOf("Cairo") }
    var category by remember { mutableStateOf("HISTORICAL") }
    var description by remember { mutableStateOf("") }
    var sourceName by remember { mutableStateOf("Ministry of Tourism & Antiquities / Official Record") }
    var sourceUrl by remember { mutableStateOf("") }
    var latInput by remember { mutableStateOf("") }
    var lngInput by remember { mutableStateOf("") }
    var operatingHours by remember { mutableStateOf("") }
    var priceInfo by remember { mutableStateOf("") }
    var safetyWarnings by remember { mutableStateOf("") }

    var actTitle by remember { mutableStateOf("") }
    var actCity by remember { mutableStateOf("Sharm El Sheikh") }
    var actCategory by remember { mutableStateOf("DIVING") }
    var actDuration by remember { mutableStateOf("Half Day (4 Hours)") }
    var actPriceInput by remember { mutableStateOf("") }
    var actCurrency by remember { mutableStateOf("EGP") }
    var actDesc by remember { mutableStateOf("") }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("Publish Verified Destination", style = MaterialTheme.typography.titleLarge)
            Text("Only publish factual, sourced destination details.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        item { OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Destination Title") }, modifier = Modifier.fillMaxWidth()) }
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(NiloraTaxonomy.egyptCities) { c ->
                    FilterChip(selected = city == c.nameEn, onClick = { city = c.nameEn }, label = { Text(c.nameEn) })
                }
            }
        }
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(TourismCategory.entries) { cat ->
                    FilterChip(selected = category == cat.key, onClick = { category = cat.key }, label = { Text(cat.labelEn) })
                }
            }
        }
        item { OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("Verified Description") }, minLines = 2, modifier = Modifier.fillMaxWidth()) }
        item { OutlinedTextField(value = sourceName, onValueChange = { sourceName = it }, label = { Text("Verification Source Name (Required)") }, modifier = Modifier.fillMaxWidth()) }
        item { OutlinedTextField(value = sourceUrl, onValueChange = { sourceUrl = it }, label = { Text("Source Reference URL (Optional)") }, modifier = Modifier.fillMaxWidth()) }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = latInput, onValueChange = { latInput = it }, label = { Text("Latitude (Optional)") }, modifier = Modifier.weight(1f))
                OutlinedTextField(value = lngInput, onValueChange = { lngInput = it }, label = { Text("Longitude (Optional)") }, modifier = Modifier.weight(1f))
            }
        }
        item { OutlinedTextField(value = operatingHours, onValueChange = { operatingHours = it }, label = { Text("Operating Hours") }, modifier = Modifier.fillMaxWidth()) }
        item { OutlinedTextField(value = priceInfo, onValueChange = { priceInfo = it }, label = { Text("Official Ticket / Price Info") }, modifier = Modifier.fillMaxWidth()) }
        item { OutlinedTextField(value = safetyWarnings, onValueChange = { safetyWarnings = it }, label = { Text("Safety Warnings (Optional)") }, modifier = Modifier.fillMaxWidth()) }
        item {
            Button(
                onClick = {
                    val lat = latInput.toDoubleOrNull()
                    val lng = lngInput.toDoubleOrNull()
                    onPublishVerifiedDestination(
                        Destination(
                            title = title,
                            countryCode = "EG",
                            cityName = city,
                            category = category,
                            description = description,
                            latitude = lat,
                            longitude = lng,
                            verifiedCoordinates = (lat != null && lng != null),
                            operatingHours = operatingHours,
                            priceInfo = priceInfo,
                            safetyWarnings = safetyWarnings,
                            sourceName = sourceName,
                            sourceUrl = sourceUrl,
                            verificationStatus = "APPROVED",
                            submittedBy = currentUserId
                        )
                    )
                    title = ""
                    description = ""
                    latInput = ""
                    lngInput = ""
                },
                enabled = title.length >= 2 && description.isNotBlank() && sourceName.isNotBlank(),
                modifier = Modifier.fillMaxWidth().testTag("admin_publish_dest_btn")
            ) {
                Text("Publish Verified Destination")
            }
        }

        item { HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp)) }

        item { Text("Publish Verified Activity Listing", style = MaterialTheme.typography.titleLarge) }
        item { OutlinedTextField(value = actTitle, onValueChange = { actTitle = it }, label = { Text("Activity Title") }, modifier = Modifier.fillMaxWidth()) }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = actCity, onValueChange = { actCity = it }, label = { Text("City") }, modifier = Modifier.weight(1f))
                OutlinedTextField(value = actCategory, onValueChange = { actCategory = it }, label = { Text("Category") }, modifier = Modifier.weight(1f))
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = actDuration, onValueChange = { actDuration = it }, label = { Text("Duration") }, modifier = Modifier.weight(1f))
                OutlinedTextField(value = actPriceInput, onValueChange = { actPriceInput = it }, label = { Text("Price (Blank = On Request)") }, modifier = Modifier.weight(1f))
                OutlinedTextField(value = actCurrency, onValueChange = { actCurrency = it }, label = { Text("Currency") }, modifier = Modifier.width(90.dp))
            }
        }
        item { OutlinedTextField(value = actDesc, onValueChange = { actDesc = it }, label = { Text("Activity Description") }, minLines = 2, modifier = Modifier.fillMaxWidth()) }
        item {
            Button(
                onClick = {
                    val price = actPriceInput.toDoubleOrNull()
                    onPublishVerifiedActivity(
                        ActivityListing(
                            title = actTitle,
                            providerId = currentUserId,
                            providerName = "NILORA Verified Operations",
                            countryCode = "EG",
                            cityName = actCity,
                            category = actCategory,
                            description = actDesc,
                            durationLabel = actDuration,
                            priceAmount = price,
                            currency = actCurrency,
                            priceOnRequest = price == null,
                            availabilityStatus = "ON_REQUEST",
                            cancellationPolicy = "Free cancellation up to 24 hours prior.",
                            sourceName = "NILORA Admin Verified",
                            verificationStatus = "APPROVED"
                        )
                    )
                    actTitle = ""
                    actDesc = ""
                    actPriceInput = ""
                },
                enabled = actTitle.length >= 2 && actDesc.isNotBlank(),
                modifier = Modifier.fillMaxWidth().testTag("admin_publish_act_btn")
            ) {
                Text("Publish Verified Activity")
            }
        }
        item { NiloraFooter() }
    }
}

@Composable
private fun AdminModerationTab(
    destinationsState: UiState<List<Destination>>,
    activitiesState: UiState<List<ActivityListing>>,
    providersState: UiState<List<ProviderProfile>>,
    bookingsState: UiState<List<BookingRecord>>,
    reportsState: UiState<List<ContentReport>>,
    realityEventsState: UiState<List<RealityEvent>>,
    teamActivitiesState: UiState<List<TeamActivityPost>>,
    onModerateDestination: (String, String) -> Unit,
    onModerateActivity: (String, String) -> Unit,
    onModerateProvider: (String, String, String) -> Unit,
    onResolveReport: (String, String) -> Unit,
    onModerateRealityEvent: (String, EventVerificationTier) -> Unit,
    onModerateImpactActivity: (String, Boolean) -> Unit
) {
    val provs = (providersState as? UiState.Success)?.data.orEmpty()
    val dests = (destinationsState as? UiState.Success)?.data.orEmpty()
    val acts = (activitiesState as? UiState.Success)?.data.orEmpty()
    val books = (bookingsState as? UiState.Success)?.data.orEmpty()
    val reps = (reportsState as? UiState.Success)?.data.orEmpty()
    val revents = (realityEventsState as? UiState.Success)?.data.orEmpty()
    val impactPosts = (teamActivitiesState as? UiState.Success)?.data.orEmpty()
        .filter { it.domain == ActivityDomain.COMMUNITY_IMPACT.key }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { Text("Local Providers (${provs.size})", style = MaterialTheme.typography.titleLarge) }
        if (provs.isEmpty()) {
            item { Text("No provider applications submitted yet.", style = MaterialTheme.typography.bodyMedium) }
        } else {
            items(provs) { p ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        VerifiedSourceBadge(p.serviceType, p.licenseNumber, p.verificationStatus)
                        Text(p.fullName, style = MaterialTheme.typography.titleMedium)
                        Text("Cities: ${p.citiesCovered.joinToString()} • Docs: ${p.documentSummary}", style = MaterialTheme.typography.bodyMedium)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = { onModerateProvider(p.id, "APPROVED", "") }) { Text("Approve") }
                            OutlinedButton(onClick = { onModerateProvider(p.id, "REJECTED", "Credential verification incomplete") }) { Text("Reject") }
                        }
                    }
                }
            }
        }

        item { Text("Destinations Queue (${dests.size})", style = MaterialTheme.typography.titleLarge) }
        items(dests) { d ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    VerifiedSourceBadge(d.sourceName, d.lastVerifiedAt, d.verificationStatus)
                    Text("${d.title} (${d.cityName})", style = MaterialTheme.typography.titleMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { onModerateDestination(d.id, "APPROVED") }) { Text("Approve") }
                        OutlinedButton(onClick = { onModerateDestination(d.id, "REJECTED") }) { Text("Reject") }
                    }
                }
            }
        }

        item { Text("Activities Queue (${acts.size})", style = MaterialTheme.typography.titleLarge) }
        items(acts) { a ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    VerifiedSourceBadge(a.sourceName, "", a.verificationStatus)
                    Text("${a.title} (${a.providerName})", style = MaterialTheme.typography.titleMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { onModerateActivity(a.id, "APPROVED") }) { Text("Approve") }
                        OutlinedButton(onClick = { onModerateActivity(a.id, "REJECTED") }) { Text("Reject") }
                    }
                }
            }
        }

        item { Text("Bookings Oversight (${books.size})", style = MaterialTheme.typography.titleLarge) }
        items(books) { b ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(b.bookingReference, style = MaterialTheme.typography.titleMedium)
                        BookingStatusChip(b.status)
                    }
                    Text("${b.travelerName} → ${b.providerName} • ${b.activityTitle}", style = MaterialTheme.typography.bodyMedium)
                }
            }
        }

        item { Text("Reality Events Moderation (${revents.size})", style = MaterialTheme.typography.titleLarge) }
        items(revents) { ev ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("${ev.title} (${ev.cityName})", style = MaterialTheme.typography.titleMedium)
                    Text("Tier: ${ev.verificationTier} • Organizer: ${ev.organizerName}", style = MaterialTheme.typography.bodySmall)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { onModerateRealityEvent(ev.id, EventVerificationTier.ADMIN_VERIFIED) }) { Text("Admin Verify") }
                        OutlinedButton(onClick = { onModerateRealityEvent(ev.id, EventVerificationTier.UNVERIFIED_USER_SUBMITTED) }) { Text("Unverified") }
                    }
                }
            }
        }

        item { Text("Community Impact Verification (${impactPosts.size})", style = MaterialTheme.typography.titleLarge) }
        items(impactPosts) { imp ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("${imp.title} (${imp.cityName})", style = MaterialTheme.typography.titleMedium)
                    Text("Verified: ${imp.impactVerifiedByAdmin} • Org: ${imp.impactOrganizationName.ifBlank { "Community" }}", style = MaterialTheme.typography.bodySmall)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { onModerateImpactActivity(imp.id, true) }) { Text("Verify Impact") }
                        OutlinedButton(onClick = { onModerateImpactActivity(imp.id, false) }) { Text("Clear Verify") }
                    }
                }
            }
        }

        item { Text("Content Reports (${reps.size})", style = MaterialTheme.typography.titleLarge) }
        items(reps) { r ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("${r.targetType} • ${r.status}", style = MaterialTheme.typography.labelLarge, color = ErrorCoral)
                    Text(r.reason, style = MaterialTheme.typography.titleMedium)
                    if (r.status != "RESOLVED") {
                        Button(onClick = { onResolveReport(r.id, "RESOLVED") }) { Text("Mark Resolved") }
                    }
                }
            }
        }
    }
}

@Composable
private fun AdminIntegrationsLogsTab(
    isOnline: Boolean,
    isGoogleMapsKeyConfigured: Boolean,
    auditLogsState: UiState<List<AuditLog>>
) {
    val logs = (auditLogsState as? UiState.Success)?.data.orEmpty()
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { Text("External Services & Integration Readiness", style = MaterialTheme.typography.titleLarge) }
        item {
            IntegrationStatusRow("Firebase Authentication (Google Sign-In)", "CONFIGURED", true)
            Spacer(Modifier.height(6.dp))
            IntegrationStatusRow("Cloud Firestore & Security Rules", "CONFIGURED", true)
            Spacer(Modifier.height(6.dp))
            IntegrationStatusRow("Firebase AI Logic (Gemini 3.8 Flash + App Check)", if (isOnline) "ONLINE" else "OFFLINE", isOnline)
            Spacer(Modifier.height(6.dp))
            IntegrationStatusRow("Android SpeechRecognizer & Text-to-Speech", "DEVICE API READY", true)
            Spacer(Modifier.height(6.dp))
            IntegrationStatusRow("OpenStreetMap / CARTO Map Tiles & Fused GPS", if (isOnline) "ONLINE" else "OFFLINE", isOnline)
            Spacer(Modifier.height(6.dp))
            IntegrationStatusRow(
                "Google Places API (GOOGLE_MAPS_API_KEY in Secrets)",
                if (isGoogleMapsKeyConfigured) "CONFIGURED" else "AWAITING_CREDENTIALS",
                isGoogleMapsKeyConfigured
            )
            Spacer(Modifier.height(6.dp))
            IntegrationStatusRow("Payment Provider Gateway (Stripe / Paymob)", "NOT_CONFIGURED", false)
        }

        item { Text("Security & Moderation Audit Logs (${logs.size})", style = MaterialTheme.typography.titleLarge) }
        if (logs.isEmpty()) {
            item { Text("No audit events recorded yet.", style = MaterialTheme.typography.bodyMedium) }
        } else {
            items(logs) { log ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("${log.action} • ${log.severity}", style = MaterialTheme.typography.labelLarge)
                        Text(log.summary, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }
}

@Composable
private fun IntegrationStatusRow(name: String, status: String, healthy: Boolean) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (healthy) VerifiedEmeraldContainer else MaterialTheme.colorScheme.surfaceVariant
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(name, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
            Text(status, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
        }
    }
}
