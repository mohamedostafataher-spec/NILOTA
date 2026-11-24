package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.data.model.*
import com.example.ui.components.NiloraFooter
import com.example.ui.components.NiloraTopBar
import com.example.ui.components.UnavailableDataCard
import com.example.ui.theme.WarningAmberContainer

@Composable
fun BeachesScreen(
    destinationsState: UiState<List<Destination>>,
    activitiesState: UiState<List<ActivityListing>>,
    onOpenDestination: (String) -> Unit,
    onOpenActivity: (String) -> Unit,
    onBack: () -> Unit
) {
    var selectedCity by remember { mutableStateOf("ALL") }
    val beachCategories = setOf("BEACHES", "SWIMMING", "DIVING", "SNORKELING", "BOAT_TRIPS")

    Scaffold(
        topBar = { NiloraTopBar(title = "Beaches & Marine Life", subtitle = "Swimming, Snorkeling, Diving & Boats", onBackClick = onBack) }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Card(colors = CardDefaults.cardColors(containerColor = WarningAmberContainer)) {
                    Row(modifier = Modifier.padding(14.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Icon(Icons.Filled.Waves, contentDescription = null, tint = Color(0xFF92400E))
                        Text(
                            text = "Water Safety Notice: Always verify local currents, flag warnings, and certified supervision before swimming or diving. NILORA never marks a beach as safe without verified safety records.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF78350F)
                        )
                    }
                }
            }
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        FilterChip(selected = selectedCity == "ALL", onClick = { selectedCity = "ALL" }, label = { Text("All Coasts") })
                    }
                    items(listOf("Sharm El Sheikh", "Hurghada", "Dahab", "Marsa Alam", "El Gouna", "Alexandria", "North Coast")) { city ->
                        FilterChip(selected = selectedCity == city, onClick = { selectedCity = city }, label = { Text(city) })
                    }
                }
            }

            val dests = (destinationsState as? UiState.Success)?.data.orEmpty().filter {
                it.category in beachCategories && (selectedCity == "ALL" || it.cityName.equals(selectedCity, true))
            }
            val acts = (activitiesState as? UiState.Success)?.data.orEmpty().filter {
                it.category in beachCategories && (selectedCity == "ALL" || it.cityName.equals(selectedCity, true))
            }

            if (dests.isEmpty() && acts.isEmpty()) {
                item {
                    UnavailableDataCard(
                        title = "No Verified Beaches or Marine Trips Listed",
                        explanation = "No administrator-verified beaches, diving centers, or boat excursions are currently stored for $selectedCity.",
                        icon = Icons.Filled.BeachAccess
                    )
                }
            } else {
                items(dests) { d -> DestinationSummaryCard(d) { onOpenDestination(d.id) } }
                items(acts) { a -> ActivitySummaryCard(a) { onOpenActivity(a.id) } }
            }
            item { NiloraFooter() }
        }
    }
}

@Composable
fun ActivitiesScreen(
    activitiesState: UiState<List<ActivityListing>>,
    onOpenActivity: (String) -> Unit,
    onOpenProviderRegistration: () -> Unit,
    onBack: () -> Unit
) {
    var selectedCity by remember { mutableStateOf("ALL") }
    Scaffold(
        topBar = { NiloraTopBar(title = "Trips & Activities", subtitle = "Verified Tours, Safari, Diving & Culture", onBackClick = onBack) }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item { FilterChip(selected = selectedCity == "ALL", onClick = { selectedCity = "ALL" }, label = { Text("All Cities") }) }
                    items(NiloraTaxonomy.egyptCities) { city ->
                        FilterChip(selected = selectedCity == city.nameEn, onClick = { selectedCity = city.nameEn }, label = { Text(city.nameEn) })
                    }
                }
            }
            when (activitiesState) {
                is UiState.Loading -> item { CircularProgressIndicator() }
                is UiState.Error -> item { UnavailableDataCard("Activities Unavailable", activitiesState.message) }
                is UiState.Success -> {
                    val filtered = activitiesState.data.filter { selectedCity == "ALL" || it.cityName.equals(selectedCity, true) }
                    if (filtered.isEmpty()) {
                        item {
                            UnavailableDataCard(
                                title = "No Verified Activities Available",
                                explanation = "Only verified providers approved by NILORA administrators can publish real activities and trips.",
                                icon = Icons.Filled.Kayaking,
                                actionLabel = "Apply as Provider",
                                onActionClick = onOpenProviderRegistration
                            )
                        }
                    } else {
                        items(filtered) { act -> ActivitySummaryCard(act) { onOpenActivity(act.id) } }
                    }
                }
            }
            item { NiloraFooter() }
        }
    }
}

@Composable
fun HotelsScreen(
    destinationsState: UiState<List<Destination>>,
    onOpenDestination: (String) -> Unit,
    onOpenSearch: () -> Unit,
    onBack: () -> Unit
) {
    val accomCategories = setOf("HOTELS", "RESORTS")
    Scaffold(
        topBar = { NiloraTopBar(title = "Hotels & Accommodation", subtitle = "Hotels, Resorts, Apartments & Hostels", onBackClick = onBack) }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Card {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text("Live Room Inventory Notice", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "NILORA does not simulate live room inventory. Only verified properties or connected booking partners show live pricing and availability.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            val list = (destinationsState as? UiState.Success)?.data.orEmpty().filter { it.category in accomCategories }
            if (list.isEmpty()) {
                item {
                    UnavailableDataCard(
                        title = "No Verified Hotels Connected Yet",
                        explanation = "Connect an authorized accommodation partner API or publish verified hotel records in Admin Dashboard.",
                        icon = Icons.Filled.Hotel,
                        actionLabel = "Search Google Places",
                        onActionClick = onOpenSearch
                    )
                }
            } else {
                items(list) { d -> DestinationSummaryCard(d) { onOpenDestination(d.id) } }
            }
            item { NiloraFooter() }
        }
    }
}

@Composable
fun RestaurantsScreen(
    destinationsState: UiState<List<Destination>>,
    onOpenDestination: (String) -> Unit,
    onOpenSearch: () -> Unit,
    onBack: () -> Unit
) {
    val foodCategories = setOf("RESTAURANTS", "CAFES", "NIGHTLIFE")
    Scaffold(
        topBar = { NiloraTopBar(title = "Restaurants & Entertainment", subtitle = "Cafes, Local Food & Nightlife", onBackClick = onBack) }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            val list = (destinationsState as? UiState.Success)?.data.orEmpty().filter { it.category in foodCategories }
            if (list.isEmpty()) {
                item {
                    UnavailableDataCard(
                        title = "No Verified Dining Listings Yet",
                        explanation = "Verified restaurants, cafes, and entertainment venues appear once approved or retrieved via Google Places API.",
                        icon = Icons.Filled.Restaurant,
                        actionLabel = "Search Google Places",
                        onActionClick = onOpenSearch
                    )
                }
            } else {
                items(list) { d -> DestinationSummaryCard(d) { onOpenDestination(d.id) } }
            }
            item { NiloraFooter() }
        }
    }
}
