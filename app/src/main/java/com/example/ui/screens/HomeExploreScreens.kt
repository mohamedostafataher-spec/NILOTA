package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.R
import com.example.data.model.*
import com.example.data.repository.PlacesSearchOutcome
import com.example.localization.AppStrings
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.CircleShape
import com.example.ui.components.*
import com.example.ui.theme.*

@Composable
fun HomeScreen(
    strings: AppStrings,
    userProfile: UserProfile?,
    isAdmin: Boolean,
    isOnline: Boolean,
    destinationsState: UiState<List<Destination>>,
    activitiesState: UiState<List<ActivityListing>>,
    unreadNotificationsCount: Int,
    onOpenTranslator: () -> Unit,
    onOpenSharedTranslator: () -> Unit,
    onOpenAiAssistant: () -> Unit,
    onOpenMap: () -> Unit,
    onOpenExplore: () -> Unit,
    onOpenSearch: () -> Unit,
    onOpenBeaches: () -> Unit,
    onOpenActivities: () -> Unit,
    onOpenHotels: () -> Unit,
    onOpenRestaurants: () -> Unit,
    onOpenNotifications: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenAdmin: () -> Unit,
    onSelectCity: (String) -> Unit,
    onOpenDestination: (String) -> Unit,
    onOpenActivity: (String) -> Unit,
    onOpenProviderRegistration: () -> Unit,
    onOpenMessages: () -> Unit = {},
    onOpenDiscoverCommunity: () -> Unit = {},
    onOpenGroups: () -> Unit = {},
    currentUserId: String = "",
    conversationsState: UiState<List<DirectConversation>> = UiState.Success(emptyList()),
    suggestedProfilesState: UiState<List<PublicProfile>> = UiState.Success(emptyList()),
    blockedUsersState: UiState<List<BlockedUserRecord>> = UiState.Success(emptyList()),
    onOpenConversation: (String) -> Unit = {},
    onOpenSuggestedMember: (PublicProfile) -> Unit = {},
    postcardsState: UiState<List<WorldPostcard>> = UiState.Success(emptyList()),
    cultureSwapsState: UiState<List<CultureSwap>> = UiState.Success(emptyList()),
    worldQuestsState: UiState<List<WorldQuest>> = UiState.Success(emptyList()),
    memoriesState: UiState<List<NiloraMemory>> = UiState.Success(emptyList()),
    onLiveAMoment: () -> Unit = {},
    onBrowsePostcards: () -> Unit = {},
    onOpenPostcard: (String) -> Unit = {},
    onSwapCultures: () -> Unit = {},
    onOpenCultureSwap: (String) -> Unit = {},
    onStartWorldQuest: () -> Unit = {},
    onOpenWorldQuest: (String) -> Unit = {},
    onOpenMemories: () -> Unit = {},
    liveAdventureSessionsCount: Int = 0,
    onOpenAdventureGateway: () -> Unit = {},
    onEnterFeaturedAdventure: () -> Unit = {},
    onOpenAdventureMatchmaking: () -> Unit = {},
    onOpenCrews: () -> Unit = {},
    realityEventsCount: Int = 0,
    teamActivitiesCount: Int = 0,
    onOpenRealityHub: () -> Unit = {},
    onOpenRealityEvents: () -> Unit = {},
    onOpenMissions: () -> Unit = {},
    onOpenChallenges: () -> Unit = {},
    onOpenRealityBridge: () -> Unit = {},
    personalizationEnabled: Boolean = true,
    savedDestinationIds: Set<String> = emptySet(),
    onTogglePersonalization: (Boolean) -> Unit = {},
    onOpenUnifiedSearch: () -> Unit = onOpenSearch,
    onOpenAdventureById: (String) -> Unit = {}
) {
    val isDark = isSystemInDarkTheme()
    val blockedIds = remember(blockedUsersState) {
        (blockedUsersState as? UiState.Success)?.data?.map { it.blockedUserId }?.toSet().orEmpty()
    }
    val rankedDestinations = remember(destinationsState, userProfile, savedDestinationIds, personalizationEnabled) {
        val list = (destinationsState as? UiState.Success)?.data.orEmpty()
        PersonalizationEngine.rankDestinations(list, userProfile, savedDestinationIds, personalizationEnabled)
    }
    val rankedAdventures = remember(userProfile, personalizationEnabled) {
        PersonalizationEngine.rankAdventures(AdventureCatalog.egyptAdventures, userProfile, personalizationEnabled)
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("home_screen_list"),
        contentPadding = PaddingValues(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        item {
            OfflineStatusBanner(isOnline = isOnline)
        }

        // Brand Wordmark Header & Utility Icons
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    NiloraBrandWordmark(
                        variant = if (isDark) BrandLogoVariant.ON_DARK else BrandLogoVariant.ON_LIGHT,
                        showTagline = true,
                        symbolSize = 36.dp
                    )
                    Text(
                        text = "Welcome, ${userProfile?.displayName?.ifBlank { "NILORA Member" } ?: "NILORA Member"}",
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (isAdmin) {
                        IconButton(
                            onClick = onOpenAdmin,
                            modifier = Modifier.testTag("home_admin_button")
                        ) {
                            Icon(Icons.Filled.AdminPanelSettings, contentDescription = "Admin Dashboard")
                        }
                    }
                    IconButton(
                        onClick = onOpenSearch,
                        modifier = Modifier.testTag("home_search_button")
                    ) {
                        Icon(Icons.Filled.Search, contentDescription = "Search")
                    }
                    BadgedBox(
                        badge = {
                            if (unreadNotificationsCount > 0) {
                                Badge { Text("$unreadNotificationsCount") }
                            }
                        }
                    ) {
                        IconButton(
                            onClick = onOpenNotifications,
                            modifier = Modifier.testTag("home_notifications_button")
                        ) {
                            Icon(Icons.Filled.Notifications, contentDescription = "Notifications")
                        }
                    }
                    IconButton(
                        onClick = onOpenSettings,
                        modifier = Modifier.testTag("home_settings_button")
                    ) {
                        Icon(Icons.Filled.Settings, contentDescription = "Settings")
                    }
                }
            }
        }

        // Phase 3: Unified Search & Transparent Personalization Bar
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                UnifiedSearchEntryBar(
                    placeholder = strings.unifiedSearchHint,
                    onClick = onOpenUnifiedSearch,
                    testTag = "home_unified_search_bar"
                )
                PersonalizationTogglePill(
                    enabled = personalizationEnabled,
                    onToggle = onTogglePersonalization,
                    testTag = "home_personalization_toggle"
                )
                if (rankedDestinations.isNotEmpty() || rankedAdventures.isNotEmpty()) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(rankedDestinations.take(3), key = { "rec_dest_${it.item.id}" }) { ranked ->
                            val d = ranked.item
                            AssistChip(
                                onClick = { onOpenDestination(d.id) },
                                label = {
                                    Text(
                                        if (personalizationEnabled && ranked.matchReason.isNotBlank()) {
                                            "${d.title} • ${ranked.matchReason}"
                                        } else d.title,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                },
                                leadingIcon = {
                                    Icon(Icons.Filled.Place, contentDescription = null, modifier = Modifier.size(16.dp))
                                }
                            )
                        }
                        items(rankedAdventures.take(2), key = { "rec_adv_${it.item.id}" }) { ranked ->
                            val adv = ranked.item
                            AssistChip(
                                onClick = { onOpenAdventureById(adv.id) },
                                label = { Text("${adv.destinationName} 3D", maxLines = 1) },
                                leadingIcon = {
                                    Icon(Icons.Filled.Explore, contentDescription = null, modifier = Modifier.size(16.dp))
                                }
                            )
                        }
                    }
                }
            }
        }

        // Cinematic Enter-a-World Gateway + Human Connection Hero
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(268.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .clickable(onClick = onOpenAdventureGateway)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.img_hero_egypt),
                        contentDescription = strings.egyptSpotlightTitle,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    listOf(
                                        MidnightNavy.copy(alpha = 0.22f),
                                        DeepNavyDark.copy(alpha = 0.94f)
                                    )
                                )
                            )
                    )
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                color = WarmSand,
                                shape = RoundedCornerShape(999.dp)
                            ) {
                                Text(
                                    text = "THE WORLD IS OPEN • GLOBAL SOUL",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = DeepNavyDark,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                            Surface(
                                color = NileTurquoise.copy(alpha = 0.92f),
                                shape = RoundedCornerShape(999.dp)
                            ) {
                                Text(
                                    text = "$liveAdventureSessionsCount live sessions",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = SoftIvory,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                        Text(
                            text = strings.worldGatewayHeadline,
                            style = MaterialTheme.typography.titleLarge,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = strings.experienceHeroTagline,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.90f),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = onEnterFeaturedAdventure,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = EgyptianGold,
                                    contentColor = MidnightNavy
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .defaultMinSize(minHeight = 48.dp)
                                    .testTag("home_enter_adventure_btn")
                            ) {
                                Icon(Icons.Filled.Explore, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    strings.enterMyAdventureCta,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            FilledTonalButton(
                                onClick = onOpenAdventureGateway,
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = SoftIvory.copy(alpha = 0.18f),
                                    contentColor = SoftIvory
                                ),
                                modifier = Modifier
                                    .defaultMinSize(minHeight = 48.dp)
                                    .testTag("home_adventure_worlds_btn")
                            ) {
                                Text("Worlds", maxLines = 1)
                            }
                        }
                    }
                }

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        AssistChip(
                            onClick = onOpenAdventureGateway,
                            label = { Text(strings.surpriseMeCta) },
                            leadingIcon = { Icon(Icons.Filled.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp)) },
                            modifier = Modifier.testTag("home_chip_secret_spots")
                        )
                    }
                    item {
                        AssistChip(
                            onClick = onOpenAdventureMatchmaking,
                            label = { Text(strings.findAdventurePeopleLabel) },
                            leadingIcon = { Icon(Icons.Filled.PersonSearch, contentDescription = null, modifier = Modifier.size(16.dp)) },
                            modifier = Modifier.testTag("home_chip_find_people")
                        )
                    }
                    item {
                        AssistChip(
                            onClick = onOpenCrews,
                            label = { Text(strings.crewsTitle) },
                            leadingIcon = { Icon(Icons.Filled.Groups, contentDescription = null, modifier = Modifier.size(16.dp)) },
                            modifier = Modifier.testTag("home_chip_crews")
                        )
                    }
                    item {
                        AssistChip(
                            onClick = onBrowsePostcards,
                            label = { Text(strings.worldPostcardTitle) },
                            leadingIcon = { Icon(Icons.Filled.MarkunreadMailbox, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        )
                    }
                }

                // Phase 2 Reality Connected Universe Gateway Card
                EditorialActionPillarCard(
                    eyebrow = "Reality Connected Universe • $realityEventsCount Events • $teamActivitiesCount Team-Ups",
                    title = strings.realityUniverseTitle,
                    subtitle = "Join real sports & social games, verified local events, Real-World Missions, Global Challenges & Reality Bridge.",
                    icon = Icons.Filled.Public,
                    containerColor = MidnightNavyLight,
                    contentColor = SoftIvory,
                    accentColor = NileTurquoise,
                    testTag = "home_card_reality_universe",
                    onClick = onOpenRealityHub
                )

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        AssistChip(
                            onClick = onOpenRealityEvents,
                            label = { Text(strings.realityEventsTitle) },
                            leadingIcon = { Icon(Icons.Filled.Celebration, contentDescription = null, modifier = Modifier.size(16.dp)) },
                            modifier = Modifier.testTag("home_chip_reality_events")
                        )
                    }
                    item {
                        AssistChip(
                            onClick = onOpenMissions,
                            label = { Text(strings.realWorldMissionsTitle) },
                            leadingIcon = { Icon(Icons.Filled.Flag, contentDescription = null, modifier = Modifier.size(16.dp)) },
                            modifier = Modifier.testTag("home_chip_missions")
                        )
                    }
                    item {
                        AssistChip(
                            onClick = onOpenChallenges,
                            label = { Text(strings.globalChallengesTitle) },
                            leadingIcon = { Icon(Icons.Filled.EmojiEvents, contentDescription = null, modifier = Modifier.size(16.dp)) },
                            modifier = Modifier.testTag("home_chip_challenges")
                        )
                    }
                    item {
                        AssistChip(
                            onClick = onOpenRealityBridge,
                            label = { Text(strings.realityBridgeTitle) },
                            leadingIcon = { Icon(Icons.Filled.Route, contentDescription = null, modifier = Modifier.size(16.dp)) },
                            modifier = Modifier.testTag("home_chip_reality_bridge")
                        )
                    }
                }
            }
        }

        // 3 Primary Human Connection Pillars
        item {
            Column(
                modifier = Modifier.padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                EditorialActionPillarCard(
                    eyebrow = "Signature Experience • World Postcard",
                    title = strings.liveMomentCta,
                    subtitle = "Send a living story, local phrase, photo, or voice note across borders.",
                    icon = Icons.Filled.MarkunreadMailbox,
                    containerColor = MidnightNavy,
                    contentColor = SoftIvory,
                    accentColor = EgyptianGold,
                    testTag = "home_cta_live_moment",
                    onClick = onLiveAMoment
                )
                EditorialActionPillarCard(
                    eyebrow = "Guided Cultural Exchange",
                    title = strings.swapCulturesCta,
                    subtitle = "Trade an expression, morning ritual, recipe, or song with someone worldwide.",
                    icon = Icons.Filled.SwapHoriz,
                    containerColor = NileTurquoiseContainer,
                    contentColor = NileTurquoiseOnContainer,
                    accentColor = NileTurquoiseDark,
                    testTag = "home_cta_swap_cultures",
                    onClick = onSwapCultures
                )
                EditorialActionPillarCard(
                    eyebrow = "Collaborative Cross-Border Goal",
                    title = strings.startQuestCta,
                    subtitle = "Join real people to document similarities, phrases, or traditions together.",
                    icon = Icons.Filled.EmojiEvents,
                    containerColor = EgyptianGoldContainer,
                    contentColor = EgyptianGoldOnContainer,
                    accentColor = EgyptianGoldDark,
                    testTag = "home_cta_start_quest",
                    onClick = onStartWorldQuest
                )
            }
        }

        // The Connection Journey Progression
        item {
            Column(
                modifier = Modifier.padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "How Connection Grows on NILORA",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                ConnectionJourneyStrip()
            }
        }

        // Living World Postcards Curated Shelf
        item {
            Column(
                modifier = Modifier.padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                NiloraSectionHeader(
                    title = strings.worldPostcardTitle,
                    subtitle = "Living cross-border stories & voice moments",
                    actionLabel = "See All",
                    onActionClick = onBrowsePostcards
                )
                when (postcardsState) {
                    is UiState.Loading -> NiloraLoadingState("Loading World Postcards...")
                    is UiState.Error -> NiloraErrorState("World Postcards Unavailable", postcardsState.message)
                    is UiState.Success -> {
                        val cards = postcardsState.data.filter { it.authorId !in blockedIds }
                        if (cards.isEmpty()) {
                            UnavailableDataCard(
                                title = "Send the First World Postcard",
                                explanation = "Share a daily story, cultural question, local expression, or voice clip — travelers and people at home can answer in their own languages.",
                                icon = Icons.Filled.MarkunreadMailbox,
                                actionLabel = strings.liveMomentCta,
                                onActionClick = onLiveAMoment
                            )
                        } else {
                            cards.take(3).forEach { card ->
                                WorldPostcardCard(
                                    postcard = card,
                                    isMine = card.authorId == currentUserId,
                                    onClick = { onOpenPostcard(card.id) }
                                )
                            }
                        }
                    }
                }
            }
        }

        // Active Culture Swaps & World Quests Shelf
        item {
            Column(
                modifier = Modifier.padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                NiloraSectionHeader(
                    title = "Culture Swaps & World Quests",
                    subtitle = "Participate in real exchanges and collaborative missions",
                    actionLabel = "Explore Swaps",
                    onActionClick = onSwapCultures
                )
                val swaps = (cultureSwapsState as? UiState.Success)?.data
                    ?.filter { it.creatorId !in blockedIds }.orEmpty()
                val quests = (worldQuestsState as? UiState.Success)?.data
                    ?.filter { it.creatorId !in blockedIds }.orEmpty()

                if (cultureSwapsState is UiState.Loading && worldQuestsState is UiState.Loading) {
                    NiloraLoadingState("Loading active swaps and quests...")
                } else if (swaps.isEmpty() && quests.isEmpty()) {
                    UnavailableDataCard(
                        title = "Start an Exchange or Quest",
                        explanation = "Open a Culture Swap to exchange local expressions or launch a World Quest with real shared progress.",
                        icon = Icons.Filled.Public,
                        actionLabel = strings.swapCulturesCta,
                        onActionClick = onSwapCultures
                    )
                } else {
                    swaps.take(2).forEach { swap ->
                        CultureSwapCard(
                            swap = swap,
                            currentUserId = currentUserId,
                            onClick = { onOpenCultureSwap(swap.id) }
                        )
                    }
                    quests.take(2).forEach { quest ->
                        WorldQuestCard(
                            quest = quest,
                            isJoined = currentUserId in quest.participantIds,
                            onClick = { onOpenWorldQuest(quest.id) }
                        )
                    }
                }
            }
        }

        // NILORA Memory Shelf
        item {
            Column(
                modifier = Modifier.padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                NiloraSectionHeader(
                    title = strings.niloraMemoryTitle,
                    subtitle = "Private-by-default moments & shared memories",
                    actionLabel = "Open Archive",
                    onActionClick = onOpenMemories
                )
                when (memoriesState) {
                    is UiState.Loading -> NiloraLoadingState("Checking saved memories...")
                    is UiState.Error -> NiloraErrorState("Memories Unavailable", memoriesState.message)
                    is UiState.Success -> {
                        val list = memoriesState.data
                        if (list.isEmpty()) {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("home_memory_empty_card")
                                    .clickable(onClick = onOpenMemories),
                                shape = RoundedCornerShape(16.dp),
                                border = BorderStroke(1.dp, EgyptianGold.copy(alpha = 0.35f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Filled.AutoStories, contentDescription = null, tint = EgyptianGoldDark)
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("Keep Meaningful Moments Safe", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                        Text(
                                            "Save World Postcards, Swaps, or personal voice memories with full visibility & audio-retention control.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    TextButton(onClick = onOpenMemories) { Text("Open") }
                                }
                            }
                        } else {
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                items(list.take(5), key = { it.id }) { mem ->
                                    Card(
                                        modifier = Modifier
                                            .width(220.dp)
                                            .clickable(onClick = onOpenMemories),
                                        shape = RoundedCornerShape(16.dp),
                                        border = BorderStroke(1.dp, EgyptianGold.copy(alpha = 0.4f))
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(14.dp),
                                            verticalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Text(
                                                text = MemoryType.fromKey(mem.memoryType).labelEn,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = EgyptianGoldDark,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = mem.title,
                                                style = MaterialTheme.typography.titleSmall,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = mem.summaryText,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = MemoryVisibility.fromKey(mem.visibility).labelEn,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = NileTurquoiseDark
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // NILORA LIVE Translation & Global Communication Tools
        item {
            Column(
                modifier = Modifier.padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("NILORA LIVE Voice & Chat", style = MaterialTheme.typography.titleLarge)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        NiloraAnimatedStatusPulse(
                            isActive = isOnline,
                            activeColor = NileTurquoise,
                            idleColor = ErrorCoral,
                            sizeDp = 8
                        )
                        Text(
                            text = if (isOnline) "Cloud AI Ready" else "Offline",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isOnline) NileTurquoiseDark else ErrorCoral
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    QuickActionCard(
                        title = strings.realTimeTranslatorTitle,
                        subtitle = "2-Phone Rooms",
                        icon = Icons.Filled.Translate,
                        containerColor = MidnightNavy,
                        contentColor = SoftIvory,
                        testTag = "home_card_translator",
                        onClick = onOpenTranslator,
                        modifier = Modifier.weight(1f)
                    )
                    QuickActionCard(
                        title = strings.onePhoneModeTitle,
                        subtitle = "Face-to-Face",
                        icon = Icons.Filled.RecordVoiceOver,
                        containerColor = NileTurquoiseContainer,
                        contentColor = NileTurquoiseOnContainer,
                        testTag = "home_card_shared_translator",
                        onClick = onOpenSharedTranslator,
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    QuickActionCard(
                        title = strings.navMessages,
                        subtitle = "1:1 & Voice Notes",
                        icon = Icons.AutoMirrored.Filled.Chat,
                        containerColor = NileTurquoiseContainer,
                        contentColor = NileTurquoiseOnContainer,
                        testTag = "home_card_messages",
                        onClick = onOpenMessages,
                        modifier = Modifier.weight(1f)
                    )
                    QuickActionCard(
                        title = strings.navDiscover,
                        subtitle = "Global Community",
                        icon = Icons.Filled.Public,
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        testTag = "home_card_discover",
                        onClick = onOpenDiscoverCommunity,
                        modifier = Modifier.weight(1f)
                    )
                    QuickActionCard(
                        title = strings.navGroups,
                        subtitle = "Language Rooms",
                        icon = Icons.Filled.Groups,
                        containerColor = EgyptianGoldContainer,
                        contentColor = EgyptianGoldOnContainer,
                        testTag = "home_card_groups",
                        onClick = onOpenGroups,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Continue Private Conversations (strictly real Firestore direct conversations)
        item {
            Column(
                modifier = Modifier.padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                NiloraSectionHeader(
                    title = "Continue Conversations",
                    actionLabel = "All Chats",
                    onActionClick = onOpenMessages
                )
                when (conversationsState) {
                    is UiState.Loading -> NiloraLoadingState("Loading conversations...")
                    is UiState.Error -> NiloraErrorState("Conversations Unavailable", conversationsState.message)
                    is UiState.Success -> {
                        val activeChats = conversationsState.data.filter { conv ->
                            conv.partnerId(currentUserId) !in blockedIds
                        }
                        if (activeChats.isEmpty()) {
                            UnavailableDataCard(
                                title = "No Active Conversations Yet",
                                explanation = "Connect from a World Postcard, Culture Swap, or Global Community when both sides opt in.",
                                icon = Icons.AutoMirrored.Filled.Chat,
                                actionLabel = "Discover People",
                                onActionClick = onOpenDiscoverCommunity
                            )
                        } else {
                            activeChats.take(2).forEach { conv ->
                                NiloraConversationPreviewCard(
                                    conversation = conv,
                                    currentUserId = currentUserId,
                                    onClick = { onOpenConversation(conv.id) }
                                )
                            }
                        }
                    }
                }
            }
        }

        // Suggested Community Connections (strictly real discoverable public_profiles)
        item {
            Column(
                modifier = Modifier.padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                NiloraSectionHeader(
                    title = "Suggested Community Connections",
                    subtitle = "Members who voluntarily opted into Public visibility",
                    actionLabel = "Discover",
                    onActionClick = onOpenDiscoverCommunity
                )
                when (suggestedProfilesState) {
                    is UiState.Loading -> NiloraLoadingState("Checking public community...")
                    is UiState.Error -> NiloraErrorState("Community Unavailable", suggestedProfilesState.message)
                    is UiState.Success -> {
                        val realSuggestions = suggestedProfilesState.data.filter { p ->
                            p.userId != currentUserId && p.userId !in blockedIds
                        }
                        if (realSuggestions.isEmpty()) {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(NiloraRadius.md),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "No other public profiles available yet. Browse Community when members opt in.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.weight(1f)
                                    )
                                    TextButton(onClick = onOpenDiscoverCommunity) { Text("Explore") }
                                }
                            }
                        } else {
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                items(realSuggestions.take(6), key = { it.userId }) { member ->
                                    Card(
                                        modifier = Modifier
                                            .width(210.dp)
                                            .clickable { onOpenSuggestedMember(member) },
                                        shape = RoundedCornerShape(NiloraRadius.md),
                                        border = BorderStroke(1.dp, EgyptianGold.copy(alpha = 0.35f))
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(14.dp),
                                            verticalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Text(member.displayName, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                            Text(
                                                text = listOfNotNull(member.city.takeIf { it.isNotBlank() }, member.country).joinToString(", "),
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 1
                                            )
                                            Text(
                                                text = "Native: ${member.nativeLanguage}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.secondary
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Trust & Safe Public Meetup Reminder
        item {
            Box(modifier = Modifier.padding(horizontal = 20.dp)) {
                SafeMeetupGuidanceCard()
            }
        }

        // Connected Travel Discovery Capability (Distinct Section)
        item {
            Column(
                modifier = Modifier.padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                NiloraSectionHeader(
                    title = "Travel Discovery & Verified Places",
                    subtitle = "Connected travel module — explore when planning a trip",
                    actionLabel = "Explore Map",
                    onActionClick = onOpenMap
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    QuickActionCard(
                        title = "Interactive Map",
                        subtitle = "Real Tiles & GPS",
                        icon = Icons.Filled.Map,
                        containerColor = EgyptianGoldContainer,
                        contentColor = EgyptianGoldOnContainer,
                        testTag = "home_card_map",
                        onClick = onOpenMap,
                        modifier = Modifier.weight(1f)
                    )
                    QuickActionCard(
                        title = "AI Travel Guide",
                        subtitle = "Gemini Grounded",
                        icon = Icons.Filled.AutoAwesome,
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.onSurface,
                        testTag = "home_card_ai_guide",
                        onClick = onOpenAiAssistant,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Quick Tourism Verticals (Beaches, Activities, Hotels, Restaurants)
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Discover by Vertical",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(horizontal = 20.dp)
                )
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item {
                        VerticalPillCard("Beaches & Red Sea", Icons.Filled.BeachAccess, "home_vertical_beaches", onOpenBeaches)
                    }
                    item {
                        VerticalPillCard("Trips & Activities", Icons.Filled.Kayaking, "home_vertical_activities", onOpenActivities)
                    }
                    item {
                        VerticalPillCard("Hotels & Resorts", Icons.Filled.Hotel, "home_vertical_hotels", onOpenHotels)
                    }
                    item {
                        VerticalPillCard("Food & Nightlife", Icons.Filled.Restaurant, "home_vertical_restaurants", onOpenRestaurants)
                    }
                    item {
                        VerticalPillCard("Become a Provider", Icons.Filled.VerifiedUser, "home_vertical_provider", onOpenProviderRegistration)
                    }
                }
            }
        }

        // Egypt 12-City Destination Taxonomy
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Egypt Destinations", style = MaterialTheme.typography.titleLarge)
                    TextButton(onClick = onOpenExplore) { Text("View All") }
                }
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(NiloraTaxonomy.egyptCities) { city ->
                        ElevatedCard(
                            modifier = Modifier
                                .width(154.dp)
                                .clickable { onSelectCity(city.nameEn) },
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(city.nameEn, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(city.nameAr, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.secondary)
                                Spacer(Modifier.height(4.dp))
                                Text(city.region, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                }
            }
        }

        // Verified Destinations from Firestore (ZERO fake listings!)
        item {
            Column(
                modifier = Modifier.padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Verified Destinations", style = MaterialTheme.typography.titleLarge)
                when (destinationsState) {
                    is UiState.Loading -> LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    is UiState.Error -> UnavailableDataCard(
                        title = strings.unavailableTitle,
                        explanation = destinationsState.message
                    )
                    is UiState.Success -> {
                        val list = destinationsState.data
                        if (list.isEmpty()) {
                            UnavailableDataCard(
                                title = "No Verified Destinations Published Yet",
                                explanation = "NILORA strictly displays only administrator-verified destinations and authorized API places. Add verified records via the Admin Dashboard or search connected Google Places.",
                                icon = Icons.Filled.VerifiedUser,
                                actionLabel = if (isAdmin) "Open Admin Dashboard" else "Search Live Places",
                                onActionClick = if (isAdmin) onOpenAdmin else onOpenSearch
                            )
                        } else {
                            list.take(5).forEach { dest ->
                                DestinationSummaryCard(dest = dest, onClick = { onOpenDestination(dest.id) })
                            }
                        }
                    }
                }
            }
        }

        // Verified Activities from Firestore
        item {
            Column(
                modifier = Modifier.padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Verified Experiences & Trips", style = MaterialTheme.typography.titleLarge)
                when (activitiesState) {
                    is UiState.Loading -> LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    is UiState.Error -> UnavailableDataCard(title = strings.unavailableTitle, explanation = activitiesState.message)
                    is UiState.Success -> {
                        val list = activitiesState.data
                        if (list.isEmpty()) {
                            UnavailableDataCard(
                                title = "No Verified Activities Published Yet",
                                explanation = "Verified local providers can submit boat trips, diving, snorkeling, and desert safaris after admin approval.",
                                icon = Icons.Filled.Explore,
                                actionLabel = "Apply as Local Provider",
                                onActionClick = onOpenProviderRegistration
                            )
                        } else {
                            list.take(5).forEach { act ->
                                ActivitySummaryCard(activity = act, onClick = { onOpenActivity(act.id) })
                            }
                        }
                    }
                }
            }
        }

        item {
            Box(modifier = Modifier.padding(horizontal = 20.dp)) {
                NiloraFooter()
            }
        }
    }
}

@Composable
private fun QuickActionCard(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    containerColor: Color,
    contentColor: Color,
    testTag: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .height(106.dp)
            .testTag(testTag)
            .clickable(onClick = onClick),
        color = containerColor,
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Icon(icon, contentDescription = title, tint = contentColor, modifier = Modifier.size(26.dp))
            Column {
                Text(title, style = MaterialTheme.typography.titleMedium, color = contentColor, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(subtitle, style = MaterialTheme.typography.labelSmall, color = contentColor.copy(alpha = 0.8f))
            }
        }
    }
}

@Composable
private fun VerticalPillCard(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    testTag: String,
    onClick: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier
            .testTag(testTag)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(icon, contentDescription = label, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(20.dp))
            Text(label, style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
fun DestinationSummaryCard(dest: Destination, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            if (dest.photoUrl.isNotBlank()) {
                AsyncImage(
                    model = dest.photoUrl,
                    contentDescription = dest.title,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                        .clip(RoundedCornerShape(12.dp)),
                    contentScale = ContentScale.Crop
                )
            }
            VerifiedSourceBadge(
                sourceName = dest.sourceName,
                lastVerifiedAt = dest.lastVerifiedAt,
                verificationStatus = dest.verificationStatus
            )
            Text(dest.title, style = MaterialTheme.typography.titleLarge)
            Text("${dest.cityName} • ${dest.category}", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.secondary)
            Text(dest.description, style = MaterialTheme.typography.bodyMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
fun ActivitySummaryCard(activity: ActivityListing, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(activity.cityName, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.secondary)
                Surface(color = TurquoiseContainer, shape = RoundedCornerShape(999.dp)) {
                    Text(
                        text = if (activity.priceOnRequest || activity.priceAmount == null) "Price on request" else "${activity.priceAmount} ${activity.currency}",
                        style = MaterialTheme.typography.labelSmall,
                        color = TurquoiseOnContainer,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }
            Text(activity.title, style = MaterialTheme.typography.titleLarge)
            Text("Provider: ${activity.providerName} • ${activity.durationLabel}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("Availability: ${activity.availabilityStatus}", style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
fun ExploreScreen(
    language: NiloraLanguage,
    selectedCountry: String,
    selectedCity: String,
    selectedCategory: String,
    destinationsState: UiState<List<Destination>>,
    onCountryChange: (String) -> Unit,
    onCityChange: (String) -> Unit,
    onCategoryChange: (String) -> Unit,
    onOpenDestination: (String) -> Unit,
    onOpenSearch: () -> Unit,
    onOpenMap: () -> Unit,
    onOpenBeaches: () -> Unit = {},
    onOpenActivities: () -> Unit = {},
    onOpenHotels: () -> Unit = {},
    onOpenRestaurants: () -> Unit = {},
    onOpenSavedPlaces: () -> Unit = {},
    onOpenRealityHub: () -> Unit = {},
    onOpenRealityEvents: () -> Unit = {}
) {
    val verifiedCount = (destinationsState as? UiState.Success)?.data?.count { it.verifiedCoordinates } ?: 0

    Column(modifier = Modifier.fillMaxSize()) {
        NiloraTopBar(
            title = "NILORA EXPLORE",
            subtitle = "Global discovery • Egypt inaugural coverage",
            actions = {
                IconButton(onClick = onOpenSavedPlaces) { Icon(Icons.Filled.Bookmark, contentDescription = "Saved Places") }
                IconButton(onClick = onOpenSearch) { Icon(Icons.Filled.Search, contentDescription = "Search") }
                IconButton(onClick = onOpenMap) { Icon(Icons.Filled.Map, contentDescription = "Map") }
            }
        )
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .testTag("explore_screen_list"),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                NiloraMapPreviewPanel(
                    verifiedLocationsCount = verifiedCount,
                    onOpenMap = onOpenMap
                )
            }

            item {
                Text("Explore Verticals", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(6.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item { VerticalPillCard("Beaches & Sea", Icons.Filled.BeachAccess, "explore_beaches", onOpenBeaches) }
                    item { VerticalPillCard("Diving & Activities", Icons.Filled.Kayaking, "explore_activities", onOpenActivities) }
                    item { VerticalPillCard("Go / Play / Team-Up", Icons.Filled.SportsSoccer, "explore_reality_hub", onOpenRealityHub) }
                    item { VerticalPillCard("Reality Events", Icons.Filled.Celebration, "explore_reality_events", onOpenRealityEvents) }
                    item { VerticalPillCard("Hotels & Resorts", Icons.Filled.Hotel, "explore_hotels", onOpenHotels) }
                    item { VerticalPillCard("Restaurants & Cafes", Icons.Filled.Restaurant, "explore_restaurants", onOpenRestaurants) }
                    item { VerticalPillCard("Saved Places", Icons.Filled.Bookmark, "explore_saved", onOpenSavedPlaces) }
                }
            }

            item {
                Text("Countries", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(6.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(NiloraTaxonomy.supportedCountries) { (name, code) ->
                        FilterChip(
                            selected = selectedCountry == code,
                            onClick = { onCountryChange(code) },
                            label = { Text(name) }
                        )
                    }
                }
            }

            if (selectedCountry == "EG") {
                item {
                    Text("Egypt Cities & Regions (12 Destinations)", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(6.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        item {
                            FilterChip(
                                selected = selectedCity == "ALL",
                                onClick = { onCityChange("ALL") },
                                label = { Text("All Egypt") }
                            )
                        }
                        items(NiloraTaxonomy.egyptCities) { city ->
                            FilterChip(
                                selected = selectedCity.equals(city.nameEn, ignoreCase = true),
                                onClick = { onCityChange(city.nameEn) },
                                label = { Text(if (language == NiloraLanguage.ARABIC) city.nameAr else city.nameEn) }
                            )
                        }
                    }
                }
            }

            item {
                Text("All 17 Tourism Categories", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(6.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        FilterChip(
                            selected = selectedCategory == "ALL",
                            onClick = { onCategoryChange("ALL") },
                            label = { Text("All") }
                        )
                    }
                    items(TourismCategory.entries) { cat ->
                        FilterChip(
                            selected = selectedCategory == cat.key,
                            onClick = { onCategoryChange(cat.key) },
                            label = { Text(cat.label(language)) }
                        )
                    }
                }
            }

            item {
                when (destinationsState) {
                    is UiState.Loading -> NiloraLoadingState("Loading verified destinations...")
                    is UiState.Error -> UnavailableDataCard("Unavailable", destinationsState.message)
                    is UiState.Success -> {
                        val filtered = destinationsState.data.filter { dest ->
                            (selectedCountry == "ALL" || dest.countryCode == selectedCountry) &&
                                (selectedCity == "ALL" || dest.cityName.equals(selectedCity, ignoreCase = true)) &&
                                (selectedCategory == "ALL" || dest.category.equals(selectedCategory, ignoreCase = true))
                        }
                        if (filtered.isEmpty()) {
                            UnavailableDataCard(
                                title = "Awaiting Verified Provider or API Listings",
                                explanation = "No verified destinations match $selectedCity / $selectedCategory yet. NILORA never invents ratings, prices, or coordinates.",
                                icon = Icons.Filled.TravelExplore,
                                actionLabel = "Search Live Places API",
                                onActionClick = onOpenSearch
                            )
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                filtered.forEach { d ->
                                    DestinationSummaryCard(dest = d, onClick = { onOpenDestination(d.id) })
                                }
                            }
                        }
                    }
                }
            }

            item { NiloraFooter() }
        }
    }
}

@Composable
fun SearchResultsScreen(
    query: String,
    onQueryChange: (String) -> Unit,
    destinationsState: UiState<List<Destination>>,
    activitiesState: UiState<List<ActivityListing>>,
    providersState: UiState<List<ProviderProfile>>,
    placesOutcome: PlacesSearchOutcome?,
    isSearchingPlaces: Boolean,
    isGoogleMapsKeyConfigured: Boolean,
    onSearchGooglePlaces: (String) -> Unit,
    onOpenDestination: (String) -> Unit,
    onOpenActivity: (String) -> Unit,
    onOpenProvider: (String) -> Unit,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = { NiloraTopBar(title = "Search NILORA", onBackClick = onBack) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = onQueryChange,
                label = { Text("Search cities, categories, verified places, guides...") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                trailingIcon = {
                    if (query.isNotBlank()) {
                        IconButton(onClick = { onQueryChange("") }) {
                            Icon(Icons.Filled.Clear, contentDescription = "Clear")
                        }
                    }
                },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("search_query_input")
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = { onSearchGooglePlaces(query.ifBlank { "Egypt tourism" }) },
                    enabled = !isSearchingPlaces,
                    modifier = Modifier.testTag("search_live_places_button")
                ) {
                    Text(if (isSearchingPlaces) "Querying..." else "Query Google Places API")
                }
                Text(
                    text = if (isGoogleMapsKeyConfigured) "Key configured" else "Key missing in Secrets",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (placesOutcome != null) {
                    item {
                        when (placesOutcome) {
                            is PlacesSearchOutcome.MissingCredentials -> UnavailableDataCard(
                                title = "Google Places Integration Awaiting Credentials",
                                explanation = "Add GOOGLE_MAPS_API_KEY in AI Studio Secrets to retrieve live Google Places listings. Local verified Firestore results appear below.",
                                icon = Icons.Filled.Key
                            )
                            is PlacesSearchOutcome.Error -> UnavailableDataCard(
                                title = "Google Places Unavailable",
                                explanation = placesOutcome.message,
                                actionLabel = "Retry",
                                onActionClick = { onSearchGooglePlaces(query.ifBlank { "Egypt tourism" }) }
                            )
                            is PlacesSearchOutcome.Success -> {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text("Live Results (${placesOutcome.source})", style = MaterialTheme.typography.titleMedium)
                                    if (placesOutcome.places.isEmpty()) {
                                        Text("No Google Places matches returned for '$query'.", style = MaterialTheme.typography.bodyMedium)
                                    } else {
                                        placesOutcome.places.forEach { p ->
                                            Card(modifier = Modifier.fillMaxWidth()) {
                                                Column(modifier = Modifier.padding(14.dp)) {
                                                    Text(p.name, style = MaterialTheme.typography.titleMedium)
                                                    Text(p.address, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                    Text("Coords: ${p.latitude}, ${p.longitude}", style = MaterialTheme.typography.labelSmall)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                val q = query.trim()
                val destMatches = (destinationsState as? UiState.Success)?.data.orEmpty().filter {
                    q.isBlank() || it.title.contains(q, true) || it.cityName.contains(q, true) || it.category.contains(q, true)
                }
                val actMatches = (activitiesState as? UiState.Success)?.data.orEmpty().filter {
                    q.isBlank() || it.title.contains(q, true) || it.cityName.contains(q, true) || it.category.contains(q, true)
                }
                val provMatches = (providersState as? UiState.Success)?.data.orEmpty().filter {
                    q.isBlank() || it.fullName.contains(q, true) || it.citiesCovered.any { c -> c.contains(q, true) }
                }

                if (destMatches.isEmpty() && actMatches.isEmpty() && provMatches.isEmpty()) {
                    item {
                        UnavailableDataCard(
                            title = "No Verified Database Matches",
                            explanation = "No approved destinations, activities, or local providers in Firestore match '${q.ifBlank { "your search" }}'.",
                            icon = Icons.Filled.SearchOff
                        )
                    }
                } else {
                    items(destMatches) { d ->
                        DestinationSummaryCard(dest = d, onClick = { onOpenDestination(d.id) })
                    }
                    items(actMatches) { a ->
                        ActivitySummaryCard(activity = a, onClick = { onOpenActivity(a.id) })
                    }
                    items(provMatches) { p ->
                        Card(modifier = Modifier.fillMaxWidth().clickable { onOpenProvider(p.id) }) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text("${p.fullName} (Verified ${p.serviceType})", style = MaterialTheme.typography.titleMedium)
                                Text("Cities: ${p.citiesCovered.joinToString()}", style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DestinationDetailsScreen(
    destination: Destination?,
    isSaved: Boolean,
    onToggleSave: () -> Unit,
    onReport: (reason: String) -> Unit,
    onOpenMap: () -> Unit,
    onBack: () -> Unit,
    onOpenLiveTranslator: () -> Unit = {},
    onOpenNearbyEvents: (String) -> Unit = {}
) {
    val context = LocalContext.current
    var showReportDialog by remember { mutableStateOf(false) }
    var reportReason by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            NiloraTopBar(
                title = destination?.title ?: "Destination Details",
                onBackClick = onBack,
                actions = {
                    if (destination != null) {
                        IconButton(onClick = onToggleSave, modifier = Modifier.testTag("destination_save_button")) {
                            Icon(
                                imageVector = if (isSaved) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder,
                                contentDescription = "Save destination"
                            )
                        }
                        IconButton(onClick = { showReportDialog = true }) {
                            Icon(Icons.Filled.Flag, contentDescription = "Report listing")
                        }
                    }
                }
            )
        }
    ) { padding ->
        if (destination == null) {
            Box(modifier = Modifier.fillMaxSize().padding(padding).padding(20.dp)) {
                UnavailableDataCard(
                    title = "Destination Unavailable",
                    explanation = "This verified destination could not be found or has been removed.",
                    actionLabel = "Go Back",
                    onActionClick = onBack
                )
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                if (destination.photoUrl.isNotBlank()) {
                    AsyncImage(
                        model = destination.photoUrl,
                        contentDescription = destination.title,
                        modifier = Modifier.fillMaxWidth().height(210.dp).clip(RoundedCornerShape(20.dp)),
                        contentScale = ContentScale.Crop
                    )
                }
                VerifiedSourceBadge(
                    sourceName = destination.sourceName,
                    lastVerifiedAt = destination.lastVerifiedAt,
                    verificationStatus = destination.verificationStatus
                )
                Text(destination.title, style = MaterialTheme.typography.headlineLarge)
                Text("${destination.cityName} (${destination.countryCode}) • ${destination.category}", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.secondary)
                Text(destination.description, style = MaterialTheme.typography.bodyLarge)

                if (destination.safetyWarnings.isNotBlank()) {
                    Card(colors = CardDefaults.cardColors(containerColor = WarningAmberContainer)) {
                        Row(modifier = Modifier.padding(14.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Icon(Icons.Filled.Warning, contentDescription = null, tint = Color(0xFF92400E))
                            Column {
                                Text("Safety Notice", style = MaterialTheme.typography.titleMedium, color = Color(0xFF92400E))
                                Text(destination.safetyWarnings, style = MaterialTheme.typography.bodyMedium, color = Color(0xFF78350F))
                            }
                        }
                    }
                }

                InfoRow("Operating Hours", destination.operatingHours.ifBlank { "Not verified / unavailable" })
                InfoRow("Verified Pricing", destination.priceInfo.ifBlank { "Not verified / unavailable" })
                InfoRow("Accessibility", destination.accessibilityInfo.ifBlank { "Not verified" })
                InfoRow("Contact", destination.contactInfo.ifBlank { "Unavailable" })
                InfoRow("Official Website", destination.officialWebsite.ifBlank { "Unavailable" })
                InfoRow(
                    "Coordinates",
                    if (destination.verifiedCoordinates && destination.latitude != null && destination.longitude != null) {
                        "${destination.latitude}, ${destination.longitude}"
                    } else {
                        "Verified coordinates unavailable"
                    }
                )
                if (destination.facilities.isNotEmpty()) {
                    Text("Facilities: ${destination.facilities.joinToString()}", style = MaterialTheme.typography.bodyMedium)
                }

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    Button(
                        onClick = {
                            if (destination.verifiedCoordinates && destination.latitude != null && destination.longitude != null) {
                                val uri = Uri.parse("https://www.google.com/maps/dir/?api=1&destination=${destination.latitude},${destination.longitude}")
                                context.startActivity(Intent(Intent.ACTION_VIEW, uri))
                            } else {
                                onOpenMap()
                            }
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Filled.Directions, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("Directions / Map")
                    }
                    OutlinedButton(onClick = onToggleSave, modifier = Modifier.weight(1f)) {
                        Text(if (isSaved) "Saved" else "Save Place")
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    FilledTonalButton(
                        onClick = onOpenLiveTranslator,
                        modifier = Modifier.weight(1f).defaultMinSize(minHeight = 48.dp).testTag("dest_open_live_btn")
                    ) {
                        Icon(Icons.Filled.Translate, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Live Translate", maxLines = 1)
                    }
                    FilledTonalButton(
                        onClick = { onOpenNearbyEvents(destination.cityName) },
                        modifier = Modifier.weight(1f).defaultMinSize(minHeight = 48.dp).testTag("dest_open_events_btn")
                    ) {
                        Icon(Icons.Filled.Celebration, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("City Events", maxLines = 1)
                    }
                }
                NiloraFooter()
            }
        }
    }

    if (showReportDialog) {
        AlertDialog(
            onDismissRequest = { showReportDialog = false },
            title = { Text("Report Incorrect Information") },
            text = {
                OutlinedTextField(
                    value = reportReason,
                    onValueChange = { reportReason = it },
                    label = { Text("Describe issue") }
                )
            },
            confirmButton = {
                Button(onClick = {
                    if (reportReason.isNotBlank()) onReport(reportReason)
                    showReportDialog = false
                }) { Text("Submit") }
            },
            dismissButton = { TextButton(onClick = { showReportDialog = false }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyLarge)
        HorizontalDivider(modifier = Modifier.padding(top = 6.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
    }
}
