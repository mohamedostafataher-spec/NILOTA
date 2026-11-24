package com.example.ui.screens

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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.*
import com.example.localization.AppStrings
import com.example.ui.components.*
import com.example.ui.theme.*

@Composable
fun TogetherHubScreen(
    strings: AppStrings,
    currentUserId: String,
    userProfile: UserProfile?,
    personalizationEnabled: Boolean,
    pendingRequestsCount: Int,
    teamActivitiesState: UiState<List<TeamActivityPost>>,
    openCrewsState: UiState<List<NiloraCrew>>,
    publicProfilesState: UiState<List<PublicProfile>>,
    postcardsState: UiState<List<WorldPostcard>>,
    cultureSwapsState: UiState<List<CultureSwap>>,
    blockedIds: Set<String>,
    onTogglePersonalization: (Boolean) -> Unit,
    onOpenUnifiedSearch: () -> Unit,
    onOpenTeamUpSports: () -> Unit,
    onOpenTeamActivityDetail: (String) -> Unit,
    onOpenPeopleDiscovery: () -> Unit,
    onOpenCrews: () -> Unit,
    onOpenCrewDetail: (String) -> Unit,
    onOpenMatchmaking: () -> Unit,
    onOpenGroups: () -> Unit,
    onOpenDirectMessages: () -> Unit,
    onOpenWorldPostcards: () -> Unit,
    onOpenPostcardDetail: (String) -> Unit,
    onCreatePostcard: () -> Unit,
    onOpenCultureSwaps: () -> Unit,
    onOpenCultureSwapDetail: (String) -> Unit,
    onConnectPerson: (PublicProfile) -> Unit
) {
    val rawActivities = (teamActivitiesState as? UiState.Success)?.data.orEmpty()
        .filter { it.creatorId !in blockedIds }
    val rankedActivities = remember(rawActivities, userProfile, personalizationEnabled) {
        PersonalizationEngine.rankTeamActivities(rawActivities, userProfile, personalizationEnabled)
    }
    val rawCrews = (openCrewsState as? UiState.Success)?.data.orEmpty()
    val rankedCrews = remember(rawCrews, userProfile, personalizationEnabled) {
        PersonalizationEngine.rankCrews(rawCrews, userProfile, personalizationEnabled)
    }
    val people = (publicProfilesState as? UiState.Success)?.data.orEmpty()
        .filter { it.userId != currentUserId && it.userId !in blockedIds && it.profileVisibility == "PUBLIC" }
    val postcards = (postcardsState as? UiState.Success)?.data.orEmpty().filter { it.authorId !in blockedIds }
    val swaps = (cultureSwapsState as? UiState.Success)?.data.orEmpty().filter { it.creatorId !in blockedIds }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("together_hub_list"),
        contentPadding = PaddingValues(16.dp, 14.dp, 16.dp, 28.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = strings.navTogether,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Meet real people through sports, crews, multilingual groups, and cultural swaps.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                UnifiedSearchEntryBar(
                    placeholder = strings.unifiedSearchHint,
                    onClick = onOpenUnifiedSearch,
                    testTag = "together_unified_search_btn"
                )
                PersonalizationTogglePill(
                    enabled = personalizationEnabled,
                    onToggle = onTogglePersonalization,
                    testTag = "together_personalization_chip"
                )
            }
        }

        // 4 Core Social & Play Gateways
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    PillarQuickGatewayCard(
                        title = "Go, Play & Sports",
                        subtitle = "${rawActivities.size} live team-ups",
                        icon = Icons.Filled.SportsSoccer,
                        containerColor = MidnightNavy,
                        contentColor = SoftIvory,
                        accentColor = NileTurquoise,
                        testTag = "together_card_sports",
                        onClick = onOpenTeamUpSports,
                        modifier = Modifier.weight(1f)
                    )
                    PillarQuickGatewayCard(
                        title = "People Discovery",
                        subtitle = if (pendingRequestsCount > 0) "$pendingRequestsCount requests" else "${people.size} public",
                        icon = Icons.Filled.PersonSearch,
                        containerColor = NileTurquoiseContainer,
                        contentColor = NileTurquoiseOnContainer,
                        accentColor = NileTurquoiseDark,
                        testTag = "together_card_people",
                        onClick = onOpenPeopleDiscovery,
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    PillarQuickGatewayCard(
                        title = "NILORA Crews",
                        subtitle = "${rawCrews.size} open crews",
                        icon = Icons.Filled.Diversity3,
                        containerColor = EgyptianGoldContainer,
                        contentColor = EgyptianGoldOnContainer,
                        accentColor = EgyptianGoldDark,
                        testTag = "together_card_crews",
                        onClick = onOpenCrews,
                        modifier = Modifier.weight(1f)
                    )
                    PillarQuickGatewayCard(
                        title = "Group Live & Chats",
                        subtitle = "Multilingual rooms",
                        icon = Icons.Filled.Groups,
                        containerColor = WarmSandContainer,
                        contentColor = DeepNavyDark,
                        accentColor = MidnightNavy,
                        testTag = "together_card_groups",
                        onClick = onOpenGroups,
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = onOpenDirectMessages,
                        modifier = Modifier.weight(1f).defaultMinSize(minHeight = 48.dp).testTag("together_btn_dms")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("1:1 Chats", maxLines = 1)
                    }
                    OutlinedButton(
                        onClick = onOpenMatchmaking,
                        modifier = Modifier.weight(1f).defaultMinSize(minHeight = 48.dp).testTag("together_btn_match")
                    ) {
                        Icon(Icons.Filled.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Matchmaking", maxLines = 1)
                    }
                }
            }
        }

        // Go / Play / Team-Up Preview
        item {
            NiloraSectionHeader(
                title = "Go, Play & Sports Team-Ups",
                subtitle = if (personalizationEnabled) "Ranked by your city & interests" else "Chronological open meetups",
                actionLabel = "All Sports & Play",
                onActionClick = onOpenTeamUpSports
            )
        }
        if (rankedActivities.isEmpty()) {
            item {
                UnavailableDataCard(
                    title = "No Active Team-Ups Yet",
                    explanation = "Organize a public football match, padel game, morning run, language coffee, or photography walk.",
                    icon = Icons.Filled.SportsSoccer,
                    actionLabel = "Open Go & Play Hub",
                    onActionClick = onOpenTeamUpSports
                )
            }
        } else {
            items(rankedActivities.take(3), key = { it.item.id }) { ranked ->
                val post = ranked.item
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("together_team_card_${post.id}")
                        .clickable { onOpenTeamActivityDetail(post.id) },
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, NileTurquoise.copy(alpha = 0.35f))
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "${ActivityDomain.fromKey(post.domain).labelEn} • ${post.cityName}",
                                style = MaterialTheme.typography.labelSmall,
                                color = NileTurquoiseDark,
                                fontWeight = FontWeight.Bold
                            )
                            if (personalizationEnabled && ranked.matchReason.isNotBlank()) {
                                Surface(color = EgyptianGoldContainer, shape = RoundedCornerShape(999.dp)) {
                                    Text(
                                        ranked.matchReason,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = EgyptianGoldOnContainer,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                        Text(post.title, style = MaterialTheme.typography.titleMedium)
                        Text(
                            "${post.scheduledDateLabel} • ${post.publicMeetingSpot} • ${post.participantIds.size}/${post.maxParticipants} joined",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Crews & People Strip
        if (rankedCrews.isNotEmpty()) {
            item {
                NiloraSectionHeader(
                    title = "Active NILORA Crews",
                    subtitle = "Small ongoing adventure & culture teams",
                    actionLabel = "Explore Crews",
                    onActionClick = onOpenCrews
                )
            }
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(rankedCrews.take(5), key = { it.item.id }) { ranked ->
                        val crew = ranked.item
                        Card(
                            modifier = Modifier
                                .width(230.dp)
                                .clickable { onOpenCrewDetail(crew.id) },
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, EgyptianGold.copy(alpha = 0.4f))
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(crew.focusDestination, style = MaterialTheme.typography.labelSmall, color = EgyptianGoldDark, fontWeight = FontWeight.Bold)
                                Text(crew.name, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text("${crew.memberIds.size} members", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
        }

        if (people.isNotEmpty()) {
            item {
                NiloraSectionHeader(
                    title = "People Open to Connect",
                    subtitle = "Respects profile visibility & block settings",
                    actionLabel = "See All",
                    onActionClick = onOpenPeopleDiscovery
                )
            }
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(people.take(6), key = { it.userId }) { person ->
                        Card(
                            modifier = Modifier
                                .width(210.dp)
                                .clickable { onConnectPerson(person) },
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(person.displayName, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text("${person.city.ifBlank { person.country }} • ${person.nativeLanguage}", style = MaterialTheme.typography.labelSmall)
                                Text(
                                    person.interests.take(2).joinToString().ifBlank { "Cultural exchange" },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }

        // Cross-Border Stories & Swaps
        item {
            NiloraSectionHeader(
                title = "World Postcards & Culture Swaps",
                subtitle = "${postcards.size} postcards • ${swaps.size} exchanges",
                actionLabel = "Postcards",
                onActionClick = onOpenWorldPostcards
            )
        }
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onCreatePostcard,
                    modifier = Modifier.weight(1f).defaultMinSize(minHeight = 48.dp)
                ) {
                    Icon(Icons.Filled.AddComment, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Live a Moment", maxLines = 1)
                }
                FilledTonalButton(
                    onClick = onOpenCultureSwaps,
                    modifier = Modifier.weight(1f).defaultMinSize(minHeight = 48.dp)
                ) {
                    Icon(Icons.Filled.SwapHoriz, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Culture Swaps", maxLines = 1)
                }
            }
        }
        items(postcards.take(2), key = { "tog_pc_${it.id}" }) { card ->
            WorldPostcardCard(
                postcard = card,
                isMine = card.authorId == currentUserId,
                onClick = { onOpenPostcardDetail(card.id) }
            )
        }
        items(swaps.take(1), key = { "tog_sw_${it.id}" }) { swap ->
            CultureSwapCard(
                swap = swap,
                currentUserId = currentUserId,
                onClick = { onOpenCultureSwapDetail(swap.id) }
            )
        }
    }
}

@Composable
fun EventsHubScreen(
    strings: AppStrings,
    userProfile: UserProfile?,
    personalizationEnabled: Boolean,
    eventsState: UiState<List<RealityEvent>>,
    externalEvents: LiveIntelligenceResult<List<ExternalOpenSourceEventNote>>?,
    missionsState: UiState<List<RealityMission>>,
    challengesState: UiState<List<GlobalChallenge>>,
    bridgeLinksState: UiState<List<RealityBridgeLink>>,
    blockedIds: Set<String>,
    onTogglePersonalization: (Boolean) -> Unit,
    onOpenUnifiedSearch: () -> Unit,
    onOpenRealityEvents: () -> Unit,
    onOpenEventDetail: (String) -> Unit,
    onOpenMissions: () -> Unit,
    onOpenChallenges: () -> Unit,
    onOpenRealityBridge: () -> Unit,
    onOpenWorldPulseMap: () -> Unit,
    onRefreshExternalEvents: () -> Unit
) {
    val rawEvents = (eventsState as? UiState.Success)?.data.orEmpty()
        .filter { it.creatorId !in blockedIds }
    val rankedEvents = remember(rawEvents, userProfile, personalizationEnabled) {
        PersonalizationEngine.rankEvents(rawEvents, userProfile, personalizationEnabled)
    }
    val missions = (missionsState as? UiState.Success)?.data.orEmpty()
    val challenges = (challengesState as? UiState.Success)?.data.orEmpty()
    val bridges = (bridgeLinksState as? UiState.Success)?.data.orEmpty()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("events_hub_list"),
        contentPadding = PaddingValues(16.dp, 14.dp, 16.dp, 28.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = strings.navEvents,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Real-world festivals, verified events, outdoor missions, global challenges, and reality bridge plans.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                UnifiedSearchEntryBar(
                    placeholder = strings.unifiedSearchHint,
                    onClick = onOpenUnifiedSearch,
                    testTag = "events_unified_search_btn"
                )
                PersonalizationTogglePill(
                    enabled = personalizationEnabled,
                    onToggle = onTogglePersonalization,
                    testTag = "events_personalization_chip"
                )
            }
        }

        // 4 Reality & Action Pillars
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    PillarQuickGatewayCard(
                        title = "Reality Events",
                        subtitle = "${rawEvents.size} active listings",
                        icon = Icons.Filled.Celebration,
                        containerColor = MidnightNavy,
                        contentColor = SoftIvory,
                        accentColor = SunsetCoral,
                        testTag = "events_card_reality_events",
                        onClick = onOpenRealityEvents,
                        modifier = Modifier.weight(1f)
                    )
                    PillarQuickGatewayCard(
                        title = "Real Missions",
                        subtitle = "${missions.size} open missions",
                        icon = Icons.Filled.Flag,
                        containerColor = NileTurquoiseContainer,
                        contentColor = NileTurquoiseOnContainer,
                        accentColor = NileTurquoiseDark,
                        testTag = "events_card_missions",
                        onClick = onOpenMissions,
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    PillarQuickGatewayCard(
                        title = "Global Challenges",
                        subtitle = "${challenges.size} active",
                        icon = Icons.Filled.EmojiEvents,
                        containerColor = EgyptianGoldContainer,
                        contentColor = EgyptianGoldOnContainer,
                        accentColor = EgyptianGoldDark,
                        testTag = "events_card_challenges",
                        onClick = onOpenChallenges,
                        modifier = Modifier.weight(1f)
                    )
                    PillarQuickGatewayCard(
                        title = "Reality Bridge",
                        subtitle = "${bridges.size} trip plans",
                        icon = Icons.Filled.Route,
                        containerColor = WarmSandContainer,
                        contentColor = DeepNavyDark,
                        accentColor = MidnightNavy,
                        testTag = "events_card_bridge",
                        onClick = onOpenRealityBridge,
                        modifier = Modifier.weight(1f)
                    )
                }
                OutlinedButton(
                    onClick = onOpenWorldPulseMap,
                    modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 48.dp).testTag("events_world_map_btn")
                ) {
                    Icon(Icons.Filled.Map, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Open World Pulse Live Map")
                }
            }
        }

        // Upcoming Reality Events
        item {
            NiloraSectionHeader(
                title = "Verified & Community Reality Events",
                subtitle = "Honest provenance tags on every listing",
                actionLabel = "All Events",
                onActionClick = onOpenRealityEvents
            )
        }

        if (rankedEvents.isEmpty()) {
            item {
                UnavailableDataCard(
                    title = "No Reality Events Listed Yet",
                    explanation = "Explore open-source Wikimedia cultural notes or publish a verified public meetup in your city.",
                    icon = Icons.Filled.Celebration,
                    actionLabel = "Open Events & Publish",
                    onActionClick = onOpenRealityEvents
                )
            }
        } else {
            items(rankedEvents.take(4), key = { it.item.id }) { ranked ->
                val ev = ranked.item
                val tier = EventVerificationTier.fromKey(ev.verificationTier)
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("events_hub_item_${ev.id}")
                        .clickable { onOpenEventDetail(ev.id) },
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, EgyptianGold.copy(alpha = 0.35f))
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "${ev.cityName} • ${tier.labelEn}",
                                style = MaterialTheme.typography.labelSmall,
                                color = EgyptianGoldDark,
                                fontWeight = FontWeight.Bold
                            )
                            if (personalizationEnabled && ranked.matchReason.isNotBlank()) {
                                Surface(color = NileTurquoiseContainer, shape = RoundedCornerShape(999.dp)) {
                                    Text(
                                        ranked.matchReason,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = NileTurquoiseOnContainer,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                        Text(ev.title, style = MaterialTheme.typography.titleMedium)
                        Text(
                            "${ev.venueName} • ${ev.startDateIso} ${ev.timeWindowLabel}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Open-Source Cultural Events Preview
        item {
            NiloraSectionHeader(
                title = "Open-Source Cultural Events Feed",
                subtitle = "Wikimedia / Wikipedia public reference feed",
                actionLabel = "Refresh",
                onActionClick = onRefreshExternalEvents
            )
        }
        when (externalEvents) {
            is LiveIntelligenceResult.Available -> {
                items(externalEvents.data.take(3), key = { it.id }) { note ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(onClick = onOpenRealityEvents),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("${note.cityName} • ${note.attribution}", style = MaterialTheme.typography.labelSmall, color = NileTurquoiseDark)
                            Text(note.title, style = MaterialTheme.typography.titleSmall)
                            Text(note.summary, style = MaterialTheme.typography.bodySmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
            else -> {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth().clickable(onClick = onRefreshExternalEvents),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Filled.Public, contentDescription = null, tint = NileTurquoiseDark)
                            Text(
                                "Tap Refresh or open Reality Events to load Wikimedia open-source cultural events for your city.",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun UnifiedSearchEntryBar(
    placeholder: String,
    onClick: () -> Unit,
    testTag: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 50.dp)
            .testTag(testTag)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(Icons.Filled.Search, contentDescription = "Global Search", tint = MaterialTheme.colorScheme.primary)
            Text(
                text = placeholder,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun PersonalizationTogglePill(
    enabled: Boolean,
    onToggle: (Boolean) -> Unit,
    testTag: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        FilterChip(
            selected = enabled,
            onClick = { onToggle(!enabled) },
            leadingIcon = {
                Icon(
                    imageVector = if (enabled) Icons.Filled.AutoAwesome else Icons.Filled.FilterListOff,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
            },
            label = {
                Text(
                    if (enabled) "Personalized by city & interests" else "Personalization paused (All)",
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            },
            modifier = Modifier.testTag(testTag)
        )
    }
}

@Composable
private fun PillarQuickGatewayCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    containerColor: Color,
    contentColor: Color,
    accentColor: Color,
    testTag: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .defaultMinSize(minHeight = 96.dp)
            .testTag(testTag)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        color = containerColor
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(icon, contentDescription = title, tint = accentColor, modifier = Modifier.size(24.dp))
            Text(title, style = MaterialTheme.typography.titleSmall, color = contentColor, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(subtitle, style = MaterialTheme.typography.labelSmall, color = contentColor.copy(alpha = 0.85f), maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}
