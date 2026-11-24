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
import com.example.data.repository.PlacesSearchOutcome
import com.example.localization.AppStrings
import com.example.ui.components.NiloraSearchFilterBar
import com.example.ui.components.NiloraTopBar
import com.example.ui.components.UnavailableDataCard
import com.example.ui.theme.*

enum class UnifiedSearchScope(val key: String, val label: String) {
    ALL("ALL", "All"),
    PLACES("PLACES", "Countries & Cities"),
    DESTINATIONS("DESTINATIONS", "Destinations"),
    ADVENTURES("ADVENTURES", "3D Worlds"),
    EVENTS("EVENTS", "Events"),
    SPORTS("SPORTS", "Sports & Play"),
    GROUPS("GROUPS", "Groups"),
    CREWS("CREWS", "Crews"),
    PEOPLE("PEOPLE", "People"),
    PROVIDERS("PROVIDERS", "Guides")
}

@Composable
fun UnifiedSearchScreen(
    strings: AppStrings,
    query: String,
    currentUserId: String,
    onQueryChange: (String) -> Unit,
    destinationsState: UiState<List<Destination>>,
    eventsState: UiState<List<RealityEvent>>,
    teamActivitiesState: UiState<List<TeamActivityPost>>,
    discoverableGroupsState: UiState<List<GroupRoom>>,
    myGroupsState: UiState<List<GroupRoom>>,
    openCrewsState: UiState<List<NiloraCrew>>,
    myCrewsState: UiState<List<NiloraCrew>>,
    publicProfilesState: UiState<List<PublicProfile>>,
    blockedUsersState: UiState<List<BlockedUserRecord>>,
    providersState: UiState<List<ProviderProfile>>,
    placesOutcome: PlacesSearchOutcome?,
    isSearchingPlaces: Boolean,
    isGoogleMapsKeyConfigured: Boolean,
    onSearchGooglePlaces: (String) -> Unit,
    onSelectCity: (String) -> Unit,
    onOpenDestination: (String) -> Unit,
    onOpenAdventure: (String) -> Unit,
    onOpenEvent: (String) -> Unit,
    onOpenTeamActivity: (String) -> Unit,
    onOpenGroup: (String) -> Unit,
    onOpenCrew: (String) -> Unit,
    onOpenPerson: (PublicProfile) -> Unit,
    onOpenProvider: (String) -> Unit,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)
    var selectedScope by remember { mutableStateOf(UnifiedSearchScope.ALL) }
    val q = query.trim().lowercase()

    val blockedIds = remember(blockedUsersState) {
        (blockedUsersState as? UiState.Success)?.data?.map { it.blockedUserId }?.toSet().orEmpty()
    }

    val matchingCities = remember(q) {
        GlobalCityHubCatalog.hubs.filter { hub ->
            q.isBlank() ||
                hub.cityName.lowercase().contains(q) ||
                hub.countryName.lowercase().contains(q) ||
                hub.countryCode.lowercase().contains(q)
        }
    }

    val matchingCountries = remember(q) {
        if (q.isBlank()) emptyList()
        else NiloraTaxonomy.countryDialCodes.filter {
            it.countryName.lowercase().contains(q) || it.iso2.lowercase().contains(q)
        }.take(6)
    }

    val matchingDestinations = remember(destinationsState, q) {
        val list = (destinationsState as? UiState.Success)?.data.orEmpty()
        list.filter {
            q.isBlank() ||
                it.title.lowercase().contains(q) ||
                it.cityName.lowercase().contains(q) ||
                it.category.lowercase().contains(q) ||
                it.description.lowercase().contains(q)
        }
    }

    val matchingAdventures = remember(q) {
        AdventureCatalog.egyptAdventures.filter {
            q.isBlank() ||
                it.title.lowercase().contains(q) ||
                it.destinationName.lowercase().contains(q) ||
                it.regionLabel.lowercase().contains(q) ||
                it.subtitle.lowercase().contains(q)
        }
    }

    val matchingEvents = remember(eventsState, blockedIds, q) {
        val list = (eventsState as? UiState.Success)?.data.orEmpty()
            .filter { it.creatorId !in blockedIds }
        list.filter {
            q.isBlank() ||
                it.title.lowercase().contains(q) ||
                it.cityName.lowercase().contains(q) ||
                it.category.lowercase().contains(q) ||
                it.venueName.lowercase().contains(q)
        }
    }

    val matchingSports = remember(teamActivitiesState, blockedIds, q) {
        val list = (teamActivitiesState as? UiState.Success)?.data.orEmpty()
            .filter { it.creatorId !in blockedIds }
        list.filter {
            q.isBlank() ||
                it.title.lowercase().contains(q) ||
                it.cityName.lowercase().contains(q) ||
                it.domain.lowercase().contains(q) ||
                it.subCategory.lowercase().contains(q) ||
                it.publicMeetingSpot.lowercase().contains(q)
        }
    }

    val matchingGroups = remember(discoverableGroupsState, myGroupsState, q) {
        val all = ((myGroupsState as? UiState.Success)?.data.orEmpty() +
            (discoverableGroupsState as? UiState.Success)?.data.orEmpty())
            .distinctBy { it.id }
        all.filter {
            q.isBlank() ||
                it.name.lowercase().contains(q) ||
                it.description.lowercase().contains(q) ||
                it.primaryLanguages.any { l -> l.lowercase().contains(q) }
        }
    }

    val matchingCrews = remember(openCrewsState, myCrewsState, q) {
        val all = ((myCrewsState as? UiState.Success)?.data.orEmpty() +
            (openCrewsState as? UiState.Success)?.data.orEmpty())
            .distinctBy { it.id }
        all.filter {
            q.isBlank() ||
                it.name.lowercase().contains(q) ||
                it.focusDestination.lowercase().contains(q) ||
                it.sharedInterests.any { i -> i.lowercase().contains(q) }
        }
    }

    val matchingPeople = remember(publicProfilesState, blockedIds, currentUserId, q) {
        val list = (publicProfilesState as? UiState.Success)?.data.orEmpty()
            .filter {
                it.userId != currentUserId &&
                    it.userId !in blockedIds &&
                    it.profileVisibility == "PUBLIC"
            }
        list.filter {
            q.isBlank() ||
                it.displayName.lowercase().contains(q) ||
                it.city.lowercase().contains(q) ||
                it.country.lowercase().contains(q) ||
                it.interests.any { i -> i.lowercase().contains(q) } ||
                it.nativeLanguage.lowercase().contains(q)
        }
    }

    val matchingProviders = remember(providersState, q) {
        val list = (providersState as? UiState.Success)?.data.orEmpty()
        list.filter {
            q.isBlank() ||
                it.fullName.lowercase().contains(q) ||
                it.citiesCovered.any { c -> c.lowercase().contains(q) } ||
                it.bio.lowercase().contains(q)
        }
    }

    fun showScope(scope: UnifiedSearchScope): Boolean =
        selectedScope == UnifiedSearchScope.ALL || selectedScope == scope

    val hasAnyResult =
        (showScope(UnifiedSearchScope.PLACES) && (matchingCities.isNotEmpty() || matchingCountries.isNotEmpty())) ||
            (showScope(UnifiedSearchScope.DESTINATIONS) && matchingDestinations.isNotEmpty()) ||
            (showScope(UnifiedSearchScope.ADVENTURES) && matchingAdventures.isNotEmpty()) ||
            (showScope(UnifiedSearchScope.EVENTS) && matchingEvents.isNotEmpty()) ||
            (showScope(UnifiedSearchScope.SPORTS) && matchingSports.isNotEmpty()) ||
            (showScope(UnifiedSearchScope.GROUPS) && matchingGroups.isNotEmpty()) ||
            (showScope(UnifiedSearchScope.CREWS) && matchingCrews.isNotEmpty()) ||
            (showScope(UnifiedSearchScope.PEOPLE) && matchingPeople.isNotEmpty()) ||
            (showScope(UnifiedSearchScope.PROVIDERS) && matchingProviders.isNotEmpty())

    Scaffold(
        topBar = {
            NiloraTopBar(
                title = "Global Search",
                subtitle = "Countries, Destinations, Events, Sports, Groups, Crews & People",
                onBackClick = onBack
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .testTag("unified_search_list"),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                NiloraSearchFilterBar(
                    query = query,
                    placeholder = strings.unifiedSearchHint,
                    onQueryChange = onQueryChange,
                    testTag = "unified_search_input"
                )
            }

            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(UnifiedSearchScope.entries) { scope ->
                        FilterChip(
                            selected = selectedScope == scope,
                            onClick = { selectedScope = scope },
                            label = { Text(scope.label) },
                            modifier = Modifier.testTag("unified_scope_${scope.key}")
                        )
                    }
                }
            }

            if (isGoogleMapsKeyConfigured && q.length >= 2) {
                item {
                    OutlinedButton(
                        onClick = { onSearchGooglePlaces(query) },
                        modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 48.dp).testTag("unified_google_places_btn")
                    ) {
                        Icon(Icons.Filled.TravelExplore, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(if (isSearchingPlaces) "Searching Live Places..." else "Search Live Google Places for \"$query\"")
                    }
                }
            }

            if (placesOutcome is PlacesSearchOutcome.Success && placesOutcome.places.isNotEmpty()) {
                item { Text("Live Google Places (${placesOutcome.places.size})", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold) }
                items(placesOutcome.places.take(4)) { place ->
                    UnifiedSearchResultCard(
                        badge = "LIVE GOOGLE PLACE",
                        title = place.name,
                        subtitle = place.address,
                        icon = Icons.Filled.Place,
                        accentColor = NileTurquoiseDark,
                        testTag = "unified_place_${place.placeId}",
                        onClick = { onSelectCity(place.name) }
                    )
                }
            }

            if (showScope(UnifiedSearchScope.PLACES) && (matchingCities.isNotEmpty() || matchingCountries.isNotEmpty())) {
                item {
                    Text(
                        "Cities & Countries (${matchingCities.size + matchingCountries.size})",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
                items(matchingCities.take(if (selectedScope == UnifiedSearchScope.PLACES) 15 else 4), key = { "city_${it.cityName}" }) { hub ->
                    UnifiedSearchResultCard(
                        badge = hub.countryName.uppercase(),
                        title = hub.cityName,
                        subtitle = "${hub.countryName} (${hub.countryCode}) • City Hub",
                        icon = Icons.Filled.LocationCity,
                        accentColor = NileTurquoiseDark,
                        testTag = "unified_city_${hub.cityName}",
                        onClick = { onSelectCity(hub.cityName) }
                    )
                }
                items(matchingCountries, key = { "country_${it.iso2}" }) { country ->
                    UnifiedSearchResultCard(
                        badge = "COUNTRY • ${country.iso2}",
                        title = country.countryName,
                        subtitle = "Dial ${country.dialCode} • Explore hubs & people",
                        icon = Icons.Filled.Public,
                        accentColor = EgyptianGoldDark,
                        testTag = "unified_country_${country.iso2}",
                        onClick = { onSelectCity(country.countryName) }
                    )
                }
            }

            if (showScope(UnifiedSearchScope.DESTINATIONS) && matchingDestinations.isNotEmpty()) {
                item {
                    Text("Verified Destinations (${matchingDestinations.size})", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                }
                items(matchingDestinations.take(if (selectedScope == UnifiedSearchScope.DESTINATIONS) 20 else 4), key = { "dest_${it.id}" }) { d ->
                    UnifiedSearchResultCard(
                        badge = "${d.cityName} • ${d.category}",
                        title = d.title,
                        subtitle = d.description,
                        icon = Icons.Filled.Verified,
                        accentColor = MidnightNavy,
                        testTag = "unified_dest_${d.id}",
                        onClick = { onOpenDestination(d.id) }
                    )
                }
            }

            if (showScope(UnifiedSearchScope.ADVENTURES) && matchingAdventures.isNotEmpty()) {
                item {
                    Text("3D Immersive Adventures (${matchingAdventures.size})", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                }
                items(matchingAdventures.take(if (selectedScope == UnifiedSearchScope.ADVENTURES) 20 else 4), key = { "adv_${it.id}" }) { adv ->
                    UnifiedSearchResultCard(
                        badge = "${adv.destinationName} • ${adv.adventureType.labelEn}",
                        title = adv.title,
                        subtitle = adv.subtitle,
                        icon = Icons.Filled.Explore,
                        accentColor = EgyptianGoldDark,
                        testTag = "unified_adv_${adv.id}",
                        onClick = { onOpenAdventure(adv.id) }
                    )
                }
            }

            if (showScope(UnifiedSearchScope.EVENTS) && matchingEvents.isNotEmpty()) {
                item {
                    Text("Reality Events (${matchingEvents.size})", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                }
                items(matchingEvents.take(if (selectedScope == UnifiedSearchScope.EVENTS) 20 else 4), key = { "ev_${it.id}" }) { ev ->
                    UnifiedSearchResultCard(
                        badge = "${ev.cityName} • ${EventVerificationTier.fromKey(ev.verificationTier).labelEn}",
                        title = ev.title,
                        subtitle = "${ev.venueName} • ${ev.startDateIso}",
                        icon = Icons.Filled.Celebration,
                        accentColor = SunsetCoral,
                        testTag = "unified_event_${ev.id}",
                        onClick = { onOpenEvent(ev.id) }
                    )
                }
            }

            if (showScope(UnifiedSearchScope.SPORTS) && matchingSports.isNotEmpty()) {
                item {
                    Text("Go, Play & Sports Team-Ups (${matchingSports.size})", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                }
                items(matchingSports.take(if (selectedScope == UnifiedSearchScope.SPORTS) 20 else 4), key = { "sport_${it.id}" }) { act ->
                    UnifiedSearchResultCard(
                        badge = "${act.cityName} • ${ActivityDomain.fromKey(act.domain).labelEn}",
                        title = act.title,
                        subtitle = "${act.publicMeetingSpot} • ${act.participantIds.size}/${act.maxParticipants} joined",
                        icon = Icons.Filled.SportsSoccer,
                        accentColor = NileTurquoiseDark,
                        testTag = "unified_sport_${act.id}",
                        onClick = { onOpenTeamActivity(act.id) }
                    )
                }
            }

            if (showScope(UnifiedSearchScope.GROUPS) && matchingGroups.isNotEmpty()) {
                item {
                    Text("Multilingual Groups (${matchingGroups.size})", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                }
                items(matchingGroups.take(if (selectedScope == UnifiedSearchScope.GROUPS) 20 else 4), key = { "grp_${it.id}" }) { grp ->
                    UnifiedSearchResultCard(
                        badge = "GROUP • ${grp.primaryLanguages.joinToString()}",
                        title = grp.name,
                        subtitle = grp.description,
                        icon = Icons.Filled.Groups,
                        accentColor = MidnightNavy,
                        testTag = "unified_group_${grp.id}",
                        onClick = { onOpenGroup(grp.id) }
                    )
                }
            }

            if (showScope(UnifiedSearchScope.CREWS) && matchingCrews.isNotEmpty()) {
                item {
                    Text("NILORA Crews (${matchingCrews.size})", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                }
                items(matchingCrews.take(if (selectedScope == UnifiedSearchScope.CREWS) 20 else 4), key = { "crew_${it.id}" }) { crew ->
                    UnifiedSearchResultCard(
                        badge = "CREW • ${crew.focusDestination}",
                        title = crew.name,
                        subtitle = "${crew.memberIds.size} members • ${crew.sharedInterests.joinToString()}",
                        icon = Icons.Filled.Diversity3,
                        accentColor = EgyptianGoldDark,
                        testTag = "unified_crew_${crew.id}",
                        onClick = { onOpenCrew(crew.id) }
                    )
                }
            }

            if (showScope(UnifiedSearchScope.PEOPLE) && matchingPeople.isNotEmpty()) {
                item {
                    Text("Public People (${matchingPeople.size})", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                }
                items(matchingPeople.take(if (selectedScope == UnifiedSearchScope.PEOPLE) 20 else 4), key = { "person_${it.userId}" }) { p ->
                    UnifiedSearchResultCard(
                        badge = "${p.city.ifBlank { p.country }} • Speaks ${p.nativeLanguage}",
                        title = p.displayName,
                        subtitle = p.bio.ifBlank { p.interests.joinToString().ifBlank { "Open to connect & exchange culture" } },
                        icon = Icons.Filled.Person,
                        accentColor = NileTurquoiseDark,
                        testTag = "unified_person_${p.userId}",
                        onClick = { onOpenPerson(p) }
                    )
                }
            }

            if (showScope(UnifiedSearchScope.PROVIDERS) && matchingProviders.isNotEmpty()) {
                item {
                    Text("Verified Guides & Providers (${matchingProviders.size})", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                }
                items(matchingProviders.take(if (selectedScope == UnifiedSearchScope.PROVIDERS) 20 else 4), key = { "prov_${it.id}" }) { prov ->
                    UnifiedSearchResultCard(
                        badge = "${prov.citiesCovered.joinToString().ifBlank { "Egypt" }} • ${prov.verificationStatus}",
                        title = prov.fullName,
                        subtitle = prov.bio,
                        icon = Icons.Filled.Badge,
                        accentColor = EgyptianGoldDark,
                        testTag = "unified_provider_${prov.id}",
                        onClick = { onOpenProvider(prov.id) }
                    )
                }
            }

            if (!hasAnyResult) {
                item {
                    UnavailableDataCard(
                        title = "No Real Matches Found",
                        explanation = "Try another city, activity, language, or switch filter scope above. NILORA never fabricates search results.",
                        icon = Icons.Filled.SearchOff
                    )
                }
            }
        }
    }
}

@Composable
private fun UnifiedSearchResultCard(
    badge: String,
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accentColor: androidx.compose.ui.graphics.Color,
    testTag: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(testTag)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.28f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                color = accentColor.copy(alpha = 0.14f),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.size(44.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = title, tint = accentColor)
                }
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(badge, style = MaterialTheme.typography.labelSmall, color = accentColor, fontWeight = FontWeight.Bold)
                Text(title, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (subtitle.isNotBlank()) {
                    Text(
                        subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Icon(Icons.Filled.ChevronRight, contentDescription = null)
        }
    }
}
