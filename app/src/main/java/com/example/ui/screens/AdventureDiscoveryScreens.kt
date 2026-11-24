package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.*
import com.example.localization.AppStrings
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.AdventureWorldUiState

@Composable
fun AdventureDiscoveryScreen(
    strings: AppStrings,
    uiState: AdventureWorldUiState,
    joinableSessionsCount: Int,
    onSelectDestinationFilter: (String) -> Unit,
    onSelectTypeFilter: (AdventureType?) -> Unit,
    onSelectAdventure: (AdventureExperience) -> Unit,
    onTriggerSurpriseMe: () -> Unit,
    onDismissSurprise: () -> Unit,
    onRefreshIntelligence: () -> Unit,
    onEnterAdventure: (String) -> Unit,
    onOpenMatchmaking: () -> Unit,
    onOpenCrews: () -> Unit,
    onOpenCapabilities: () -> Unit,
    onOpenRealWorldPlan: (String) -> Unit,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)

    val destinationNames = remember {
        listOf("ALL") + AdventureCatalog.egyptAdventures.map { it.destinationName }.distinct()
    }
    val filteredAdventures = remember(uiState.selectedDestinationFilter, uiState.selectedTypeFilter) {
        AdventureCatalog.egyptAdventures.filter { adv ->
            val destMatch = uiState.selectedDestinationFilter == "ALL" ||
                adv.destinationName == uiState.selectedDestinationFilter
            val typeMatch = uiState.selectedTypeFilter == null || adv.adventureType == uiState.selectedTypeFilter
            destMatch && typeMatch
        }
    }
    val selected = uiState.selectedAdventure

    Scaffold(
        topBar = {
            NiloraTopBar(
                title = "NILORA Adventure Engine",
                subtitle = "Egypt Launch • Global Architecture",
                onBackClick = onBack,
                actions = {
                    IconButton(
                        onClick = onOpenCapabilities,
                        modifier = Modifier.testTag("adv_capabilities_btn")
                    ) {
                        Icon(Icons.Filled.VerifiedUser, contentDescription = strings.capabilitiesTitle)
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .testTag("adventure_discovery_list"),
            contentPadding = PaddingValues(18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Cinematic Gateway Card + Surprise Me Secret Spot
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(MidnightNavy, DeepNavyDark, NileTurquoiseDark)
                            )
                        )
                        .padding(18.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(color = EgyptianGold, shape = RoundedCornerShape(999.dp)) {
                                Text(
                                    text = "LIVE WORLDS • $joinableSessionsCount ACTIVE SESSIONS",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = DeepNavyDark,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                            FilledTonalButton(
                                onClick = onTriggerSurpriseMe,
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = SunsetCoral,
                                    contentColor = Color.White
                                ),
                                modifier = Modifier
                                    .defaultMinSize(minHeight = 48.dp)
                                    .testTag("surprise_me_secret_spot_btn")
                            ) {
                                Icon(Icons.Filled.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(strings.surpriseMeCta, fontWeight = FontWeight.Bold)
                            }
                        }

                        Text(
                            text = strings.worldGatewayHeadline,
                            style = MaterialTheme.typography.titleLarge,
                            color = SoftIvory,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = strings.worldGatewaySubheadline,
                            style = MaterialTheme.typography.bodySmall,
                            color = SoftIvory.copy(alpha = 0.85f)
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = onOpenMatchmaking,
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = SoftIvory),
                                border = BorderStroke(1.dp, NileTurquoise),
                                modifier = Modifier.weight(1f).defaultMinSize(minHeight = 48.dp).testTag("open_matchmaking_btn")
                            ) {
                                Icon(Icons.Filled.PersonSearch, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Find People", maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                            OutlinedButton(
                                onClick = onOpenCrews,
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = SoftIvory),
                                border = BorderStroke(1.dp, EgyptianGold),
                                modifier = Modifier.weight(1f).defaultMinSize(minHeight = 48.dp).testTag("open_crews_btn")
                            ) {
                                Icon(Icons.Filled.Groups, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("NILORA Crews", maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                }
            }

            // Secret Spot Reveal Banner
            item {
                AnimatedVisibility(visible = uiState.surpriseRevealAdventure != null) {
                    val reveal = uiState.surpriseRevealAdventure
                    if (reveal != null) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = EgyptianGoldContainer),
                            border = BorderStroke(1.5.dp, EgyptianGoldDark),
                            modifier = Modifier.fillMaxWidth().testTag("secret_spot_reveal_card")
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "${strings.secretSpotsTitle}: ${reveal.destinationName}",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = EgyptianGoldOnContainer,
                                        fontWeight = FontWeight.Bold
                                    )
                                    TextButton(onClick = onDismissSurprise) { Text("Hide") }
                                }
                                Text(reveal.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text(reveal.ecoConservationNote, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }

            // Destination & Adventure Type Filters
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("1. Choose Real Destination", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(destinationNames) { dest ->
                            FilterChip(
                                selected = uiState.selectedDestinationFilter == dest,
                                onClick = { onSelectDestinationFilter(dest) },
                                label = { Text(if (dest == "ALL") "All Egypt Destinations" else dest) },
                                modifier = Modifier.testTag("adv_dest_filter_${dest.replace(" ", "_").lowercase()}")
                            )
                        }
                    }
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        item {
                            FilterChip(
                                selected = uiState.selectedTypeFilter == null,
                                onClick = { onSelectTypeFilter(null) },
                                label = { Text("All Adventures") }
                            )
                        }
                        items(AdventureType.entries) { t ->
                            FilterChip(
                                selected = uiState.selectedTypeFilter == t,
                                onClick = { onSelectTypeFilter(t) },
                                label = { Text(t.labelEn) }
                            )
                        }
                    }
                }
            }

            // Selected Adventure Geographic Intelligence & "ENTER MY ADVENTURE" Spotlight
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().testTag("selected_adventure_spotlight"),
                    shape = RoundedCornerShape(22.dp),
                    border = BorderStroke(1.5.dp, NileTurquoise)
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "${selected.destinationName} • ${selected.regionLabel}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = NileTurquoiseDark,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = selected.title,
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            if (selected.isSecretSpot) {
                                Surface(color = SunsetCoralContainer, shape = RoundedCornerShape(999.dp)) {
                                    Text(
                                        "Secret Spot",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = SunsetCoral,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }

                        Text(selected.description, style = MaterialTheme.typography.bodyMedium)

                        // Verified Coordinates & Geographic Context
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "GPS: ${selected.latitude}° N, ${selected.longitude}° E",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    TextButton(onClick = onRefreshIntelligence) {
                                        Text("Refresh Live Data")
                                    }
                                }

                                // Real Open-Meteo Marine / Weather Telemetry
                                if (uiState.isLoadingWeather) {
                                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                                } else {
                                    when (val w = uiState.weatherState) {
                                        is LiveIntelligenceResult.Available -> {
                                            val data = w.data
                                            val seaInfo = if (data.seaSurfaceTempC != null) {
                                                " • Sea ${data.seaSurfaceTempC}°C • Waves ${data.waveHeightMeters ?: "--"}m"
                                            } else ""
                                            Text(
                                                text = "Live Weather: ${data.temperatureC}°C • Wind ${data.windSpeedKmh} km/h (${data.weatherLabel})$seaInfo",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = NileTurquoiseDark,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                            Text(
                                                text = "Source: ${data.sourceAttribution}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        is LiveIntelligenceResult.Unavailable -> {
                                            Text(
                                                text = w.reason,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        null -> Unit
                                    }
                                }

                                // Wikipedia Encyclopedic Context (CC BY-SA 4.0)
                                when (val wiki = uiState.wikiState) {
                                    is LiveIntelligenceResult.Available -> {
                                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                                        Text(
                                            text = wiki.data.extract,
                                            style = MaterialTheme.typography.bodySmall,
                                            maxLines = 4,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "Source: ${wiki.data.attribution}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    is LiveIntelligenceResult.Unavailable -> {
                                        Text(
                                            text = wiki.reason,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    null -> Unit
                                }
                            }
                        }

                        // Real Safety & Ecological Conservation Notice
                        Surface(
                            color = WarningAmberContainer,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("Safety & Ecology Notice", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                                Text(selected.safetyNotice, style = MaterialTheme.typography.bodySmall)
                                Text(selected.ecoConservationNote, style = MaterialTheme.typography.bodySmall)
                            }
                        }

                        Text(
                            text = selected.geoAttribution,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Button(
                            onClick = { onEnterAdventure(selected.id) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MidnightNavy,
                                contentColor = SoftIvory
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(54.dp)
                                .testTag("enter_my_adventure_btn")
                        ) {
                            Icon(Icons.Filled.Explore, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text(strings.enterMyAdventureCta, fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = { onOpenRealWorldPlan(selected.destinationName) },
                            modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 48.dp)
                        ) {
                            Text("View Verified Real-World Places in ${selected.destinationName}")
                        }
                    }
                }
            }

            // Catalog of Available Adventures
            item {
                Text(
                    text = "Available Adventures (${filteredAdventures.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            items(filteredAdventures, key = { it.id }) { adv ->
                val isSelected = adv.id == selected.id
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelectAdventure(adv) }
                        .testTag("adventure_catalog_card_${adv.id}"),
                    border = if (isSelected) BorderStroke(2.dp, NileTurquoise) else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Surface(
                            color = if (adv.biome == AdventureBiome.RED_SEA_REEF) NileTurquoiseContainer else EgyptianGoldContainer,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.size(48.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = when (adv.biome) {
                                        AdventureBiome.RED_SEA_REEF, AdventureBiome.MEDITERRANEAN_COAST -> Icons.Filled.ScubaDiving
                                        AdventureBiome.DESERT_OASIS -> Icons.Filled.Landscape
                                        AdventureBiome.NILE_HERITAGE -> Icons.Filled.AccountBalance
                                    },
                                    contentDescription = null,
                                    tint = MidnightNavy
                                )
                            }
                        }
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text("${adv.destinationName} • ${adv.adventureType.labelEn}", style = MaterialTheme.typography.labelSmall, color = NileTurquoiseDark)
                            Text(adv.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Text("${adv.pois.size} 3D Discovery Points • ${adv.recommendedTimeOfDay}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Icon(Icons.Filled.ChevronRight, contentDescription = "Select")
                    }
                }
            }
        }
    }
}

@Composable
fun AdventureMatchmakingScreen(
    strings: AppStrings,
    currentUserId: String,
    myProfile: UserProfile?,
    selectedAdventure: AdventureExperience,
    matchProfilesState: UiState<List<AdventureMatchProfile>>,
    blockedUsersState: UiState<List<BlockedUserRecord>>,
    onPublishAvailability: (
        adventure: AdventureExperience,
        interests: List<String>,
        preferredLang: String,
        spokenLangs: List<String>,
        level: AdventureLevel,
        durationMinutes: Int,
        slot: String,
        voicePreferred: Boolean,
        isAvailable: Boolean
    ) -> Unit,
    onConnectWithMatch: (AdventureMatchProfile) -> Unit,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)

    var chosenAdv by remember(selectedAdventure.id) { mutableStateOf(selectedAdventure) }
    var level by remember { mutableStateOf(AdventureLevel.BEGINNER) }
    var durationMin by remember { mutableIntStateOf(30) }
    var timeSlot by remember { mutableStateOf("Evenings & Weekends") }
    var voicePref by remember { mutableStateOf(true) }
    var broadcastLive by remember { mutableStateOf(true) }

    val myMatchProfile = remember(myProfile, chosenAdv, level, durationMin, timeSlot, voicePref) {
        AdventureMatchProfile(
            userId = currentUserId,
            displayName = myProfile?.displayName ?: "NILORA Explorer",
            country = myProfile?.country ?: "Egypt",
            targetAdventureId = chosenAdv.id,
            targetDestination = chosenAdv.destinationName,
            interests = myProfile?.interests.orEmpty().ifEmpty { listOf(chosenAdv.adventureType.labelEn) },
            preferredLanguage = myProfile?.preferredLanguage ?: "en",
            spokenLanguages = myProfile?.spokenLanguages.orEmpty().ifEmpty { listOf(myProfile?.preferredLanguage ?: "en") },
            experienceLevel = level.key,
            preferredDurationMinutes = durationMin,
            availableTimeSlot = timeSlot,
            voicePreferred = voicePref
        )
    }

    val blockedIds = remember(blockedUsersState) {
        (blockedUsersState as? UiState.Success)?.data?.map { it.blockedUserId }?.toSet().orEmpty()
    }

    Scaffold(
        topBar = {
            NiloraTopBar(
                title = strings.findAdventurePeopleLabel,
                subtitle = "Real Explorers Only • Zero Fake Profiles",
                onBackClick = onBack
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .testTag("adventure_matchmaking_list"),
            contentPadding = PaddingValues(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    border = BorderStroke(1.dp, NileTurquoise)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text("Your Adventure Match Preferences", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(AdventureCatalog.egyptAdventures.take(6)) { adv ->
                                FilterChip(
                                    selected = chosenAdv.id == adv.id,
                                    onClick = { chosenAdv = adv },
                                    label = { Text(adv.destinationName) }
                                )
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            AdventureLevel.entries.forEach { lvl ->
                                FilterChip(
                                    selected = level == lvl,
                                    onClick = { level = lvl },
                                    label = { Text(lvl.labelEn.split(" ").first()) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf(15, 30, 60).forEach { mins ->
                                FilterChip(
                                    selected = durationMin == mins,
                                    onClick = { durationMin = mins },
                                    label = { Text("${mins}m") }
                                )
                            }
                            FilterChip(
                                selected = voicePref,
                                onClick = { voicePref = !voicePref },
                                label = { Text(if (voicePref) "Voice On" else "Text Only") }
                            )
                        }
                        OutlinedTextField(
                            value = timeSlot,
                            onValueChange = { timeSlot = it },
                            label = { Text("Availability Window") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Visible to matching explorers", style = MaterialTheme.typography.bodyMedium)
                            Switch(checked = broadcastLive, onCheckedChange = { broadcastLive = it })
                        }
                        Button(
                            onClick = {
                                onPublishAvailability(
                                    chosenAdv,
                                    myMatchProfile.interests,
                                    myMatchProfile.preferredLanguage,
                                    myMatchProfile.spokenLanguages,
                                    level,
                                    durationMin,
                                    timeSlot,
                                    voicePref,
                                    broadcastLive
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .defaultMinSize(minHeight = 48.dp)
                                .testTag("publish_match_profile_btn")
                        ) {
                            Text("Update Live Match Profile")
                        }
                    }
                }
            }

            item {
                Text("Real Compatible Explorers", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }

            when (matchProfilesState) {
                is UiState.Loading -> item { NiloraLoadingState("Scanning real available explorers...") }
                is UiState.Error -> item { NiloraErrorState("Matchmaking Unavailable", matchProfilesState.message) }
                is UiState.Success -> {
                    val ranked = matchProfilesState.data
                        .filter { it.userId != currentUserId && it.userId !in blockedIds }
                        .map { AdventureMatchmaker.computeCompatibility(myMatchProfile, it) }
                        .sortedByDescending { it.scorePercent }

                    if (ranked.isEmpty()) {
                        item {
                            UnavailableDataCard(
                                title = "No Real Explorers Matched Right Now",
                                explanation = "NILORA never displays fake people or simulated matches. Publish your match profile above or start an open session so others can join you.",
                                icon = Icons.Filled.PersonSearch
                            )
                        }
                    } else {
                        items(ranked, key = { it.profile.userId }) { match ->
                            val cand = match.profile
                            Card(modifier = Modifier.fillMaxWidth().testTag("match_result_${cand.userId}")) {
                                Column(
                                    modifier = Modifier.padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(cand.displayName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                            Text("${cand.country} • ${cand.targetDestination}", style = MaterialTheme.typography.bodySmall, color = NileTurquoiseDark)
                                        }
                                        Surface(color = EgyptianGoldContainer, shape = RoundedCornerShape(999.dp)) {
                                            Text(
                                                "${match.scorePercent}% Match",
                                                style = MaterialTheme.typography.labelMedium,
                                                color = EgyptianGoldOnContainer,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                            )
                                        }
                                    }
                                    Text(
                                        text = match.matchReasons.joinToString(" • "),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Button(
                                        onClick = { onConnectWithMatch(cand) },
                                        modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 48.dp)
                                    ) {
                                        Text("Connect with Mutual Consent")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
