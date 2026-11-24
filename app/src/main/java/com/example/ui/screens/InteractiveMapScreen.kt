package com.example.ui.screens

import android.Manifest
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.model.*
import com.example.data.repository.GpsCoordinates
import com.example.data.repository.PlacesSearchOutcome
import com.example.ui.components.NiloraTopBar
import com.example.ui.components.UnavailableDataCard
import com.example.ui.theme.DeepNavy
import com.example.ui.theme.Turquoise
import com.example.ui.theme.WarmSand
import kotlin.math.*

data class VerifiedMapMarker(
    val id: String,
    val title: String,
    val subtitle: String,
    val category: String,
    val layer: String = "DESTINATIONS",
    val latitude: Double,
    val longitude: Double,
    val source: String,
    val destinationId: String? = null,
    val eventId: String? = null,
    val activityId: String? = null,
    val missionId: String? = null,
    val challengeId: String? = null,
    val adventureId: String? = null
)

@Composable
fun InteractiveMapScreen(
    destinationsState: UiState<List<Destination>>,
    gpsLocation: GpsCoordinates?,
    gpsStatusMessage: String,
    placesOutcome: PlacesSearchOutcome?,
    isGoogleMapsKeyConfigured: Boolean,
    onRequestGpsLocation: () -> Unit,
    onSearchPlaces: (String) -> Unit,
    onOpenDestination: (String) -> Unit,
    onBack: () -> Unit,
    realityEventsState: UiState<List<RealityEvent>> = UiState.Success(emptyList()),
    teamActivitiesState: UiState<List<TeamActivityPost>> = UiState.Success(emptyList()),
    missionsState: UiState<List<RealityMission>> = UiState.Success(emptyList()),
    challengesState: UiState<List<GlobalChallenge>> = UiState.Success(emptyList()),
    liveSessionsState: UiState<List<LiveAdventureSession>> = UiState.Success(emptyList()),
    onOpenRealityEvent: (String) -> Unit = {},
    onOpenTeamActivity: (String) -> Unit = {},
    onOpenMissions: () -> Unit = {},
    onOpenChallenges: () -> Unit = {},
    onOpenAdventureWorld: (String) -> Unit = {}
) {
    val context = LocalContext.current
    var centerLat by remember { mutableDoubleStateOf(30.0444) } // Cairo viewport default
    var centerLng by remember { mutableDoubleStateOf(31.2357) }
    var zoom by remember { mutableIntStateOf(6) }
    var selectedCategory by remember { mutableStateOf("ALL") }
    var selectedLayer by remember { mutableStateOf("ALL") }
    var searchText by remember { mutableStateOf("") }
    var selectedMarker by remember { mutableStateOf<VerifiedMapMarker?>(null) }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { perms ->
        if (perms[Manifest.permission.ACCESS_COARSE_LOCATION] == true ||
            perms[Manifest.permission.ACCESS_FINE_LOCATION] == true
        ) {
            onRequestGpsLocation()
        }
    }

    // Collect ONLY public venue / destination coordinates (never private user GPS!)
    val verifiedMarkers = remember(
        destinationsState,
        placesOutcome,
        realityEventsState,
        teamActivitiesState,
        missionsState,
        challengesState,
        liveSessionsState,
        selectedCategory,
        selectedLayer
    ) {
        val dbMarkers = (destinationsState as? UiState.Success)?.data.orEmpty()
            .filter { it.verifiedCoordinates && it.latitude != null && it.longitude != null }
            .filter { selectedCategory == "ALL" || it.category == selectedCategory }
            .map {
                VerifiedMapMarker(
                    id = "db_${it.id}",
                    title = it.title,
                    subtitle = "${it.cityName} • ${it.category}",
                    category = it.category,
                    layer = "DESTINATIONS",
                    latitude = it.latitude!!,
                    longitude = it.longitude!!,
                    source = it.sourceName,
                    destinationId = it.id
                )
            }
        val apiMarkers = (placesOutcome as? PlacesSearchOutcome.Success)?.places.orEmpty().map {
            VerifiedMapMarker(
                id = "gp_${it.placeId}",
                title = it.name,
                subtitle = it.address,
                category = "GOOGLE_PLACES",
                layer = "DESTINATIONS",
                latitude = it.latitude,
                longitude = it.longitude,
                source = "Google Places API"
            )
        }
        val eventMarkers = (realityEventsState as? UiState.Success)?.data.orEmpty()
            .filter { it.latitude != null && it.longitude != null && it.status in setOf("UPCOMING", "ACTIVE") }
            .map { ev ->
                VerifiedMapMarker(
                    id = "ev_${ev.id}",
                    title = ev.title,
                    subtitle = "${ev.cityName} • ${ev.venueName} • ${ev.startDateIso}",
                    category = ev.category,
                    layer = "EVENTS",
                    latitude = ev.latitude!!,
                    longitude = ev.longitude!!,
                    source = EventVerificationTier.fromKey(ev.verificationTier).labelEn,
                    eventId = ev.id
                )
            }
        val activityMarkers = (teamActivitiesState as? UiState.Success)?.data.orEmpty()
            .filter { it.latitude != null && it.longitude != null && it.teamStatus != TeamUpStatus.ACTIVITY_CANCELLED.key }
            .map { act ->
                val isImpact = act.domain == ActivityDomain.COMMUNITY_IMPACT.key
                VerifiedMapMarker(
                    id = "act_${act.id}",
                    title = act.title,
                    subtitle = "${act.cityName} • ${act.publicMeetingSpot} (${act.participantIds.size}/${act.maxParticipants})",
                    category = act.domain,
                    layer = if (isImpact) "IMPACT" else "TEAM_UP",
                    latitude = act.latitude!!,
                    longitude = act.longitude!!,
                    source = if (isImpact && act.impactVerifiedByAdmin) "Admin-Verified Impact" else "Public Meetup Venue",
                    activityId = act.id
                )
            }
        val missionMarkers = (missionsState as? UiState.Success)?.data.orEmpty()
            .filter { it.latitude != null && it.longitude != null && it.executionMode == MissionExecutionMode.REAL_WORLD.key }
            .map { m ->
                VerifiedMapMarker(
                    id = "mis_${m.id}",
                    title = m.title,
                    subtitle = "${m.cityName} • ${m.submissionsCount}/${m.targetSteps} logs",
                    category = "MISSION",
                    layer = "MISSIONS",
                    latitude = m.latitude!!,
                    longitude = m.longitude!!,
                    source = "Real-World Mission",
                    missionId = m.id
                )
            }
        val challengeMarkers = (challengesState as? UiState.Success)?.data.orEmpty()
            .filter { it.latitude != null && it.longitude != null }
            .map { ch ->
                VerifiedMapMarker(
                    id = "chal_${ch.id}",
                    title = ch.title,
                    subtitle = "${ch.cityName} • ${ch.entriesCount}/${ch.targetGoal} entries",
                    category = ch.challengeScope,
                    layer = "MISSIONS",
                    latitude = ch.latitude!!,
                    longitude = ch.longitude!!,
                    source = "Global Challenge",
                    challengeId = ch.id
                )
            }
        val worldMarkers = (liveSessionsState as? UiState.Success)?.data.orEmpty()
            .filter { it.status == "ACTIVE" }
            .mapNotNull { sess ->
                val adv = AdventureCatalog.egyptAdventures.firstOrNull { it.id == sess.adventureId } ?: return@mapNotNull null
                VerifiedMapMarker(
                    id = "world_${sess.id}",
                    title = "Live World: ${sess.adventureTitle}",
                    subtitle = "${sess.destinationName} • ${sess.participantIds.size} exploring",
                    category = "LIVE_WORLD",
                    layer = "LIVE_WORLDS",
                    latitude = adv.latitude,
                    longitude = adv.longitude,
                    source = "3D Adventure Destination Anchor",
                    adventureId = adv.id
                )
            }

        val combined = dbMarkers + apiMarkers + eventMarkers + activityMarkers + missionMarkers + challengeMarkers + worldMarkers
        if (selectedLayer == "ALL") combined else combined.filter { it.layer == selectedLayer }
    }

    Scaffold(
        topBar = {
            NiloraTopBar(
                title = "NILORA World Pulse",
                subtitle = "OpenStreetMap Tiles • Public Venue & Destination Coordinates",
                onBackClick = onBack,
                actions = {
                    IconButton(
                        onClick = {
                            locationPermissionLauncher.launch(
                                arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION)
                            )
                        },
                        modifier = Modifier.testTag("map_my_location_button")
                    ) {
                        Icon(Icons.Filled.MyLocation, contentDescription = "Use my GPS location")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Search & City Jump Bar
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = searchText,
                        onValueChange = { searchText = it },
                        placeholder = { Text("Search places on map...") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    Button(
                        onClick = {
                            val cityMatch = NiloraTaxonomy.egyptCities.firstOrNull {
                                it.nameEn.contains(searchText, true) || it.nameAr.contains(searchText, true)
                            }
                            if (cityMatch != null) {
                                centerLat = cityMatch.viewportLat
                                centerLng = cityMatch.viewportLng
                                zoom = 11
                            }
                            if (searchText.isNotBlank()) {
                                onSearchPlaces(searchText)
                            }
                        }
                    ) {
                        Icon(Icons.Filled.Search, contentDescription = "Search")
                    }
                }

                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(NiloraTaxonomy.egyptCities) { city ->
                        AssistChip(
                            onClick = {
                                centerLat = city.viewportLat
                                centerLng = city.viewportLng
                                zoom = 11
                            },
                            label = { Text(city.nameEn) }
                        )
                    }
                }

                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(
                        "ALL" to "All Pulse Layers",
                        "DESTINATIONS" to "Destinations",
                        "EVENTS" to "Events",
                        "TEAM_UP" to "Team-Up",
                        "IMPACT" to "Impact",
                        "MISSIONS" to "Missions",
                        "LIVE_WORLDS" to "Live Worlds"
                    ).forEach { (layerKey, layerLabel) ->
                        item {
                            FilterChip(
                                selected = selectedLayer == layerKey,
                                onClick = { selectedLayer = layerKey },
                                label = { Text(layerLabel) },
                                modifier = Modifier.testTag("map_layer_$layerKey")
                            )
                        }
                    }
                }

                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    item {
                        FilterChip(selected = selectedCategory == "ALL", onClick = { selectedCategory = "ALL" }, label = { Text("All Verified") })
                    }
                    items(listOf("BEACHES", "HISTORICAL", "HOTELS", "RESTAURANTS", "DIVING", "DESERT_SAFARI")) { cat ->
                        FilterChip(selected = selectedCategory == cat, onClick = { selectedCategory = cat }, label = { Text(cat) })
                    }
                }
            }

            // Real Web Mercator Slippy Tile Map Canvas
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clipToBounds()
            ) {
                SlippyTileMapView(
                    centerLat = centerLat,
                    centerLng = centerLng,
                    zoom = zoom,
                    markers = verifiedMarkers,
                    gpsLocation = gpsLocation,
                    onPan = { dLat, dLng ->
                        centerLat = (centerLat + dLat).coerceIn(-80.0, 80.0)
                        centerLng = (centerLng + dLng).coerceIn(-180.0, 180.0)
                    },
                    onSelectMarker = { selectedMarker = it }
                )

                // Zoom Controls
                Column(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilledIconButton(onClick = { if (zoom < 17) zoom++ }) {
                        Icon(Icons.Filled.Add, contentDescription = "Zoom in")
                    }
                    FilledIconButton(onClick = { if (zoom > 3) zoom-- }) {
                        Icon(Icons.Filled.Remove, contentDescription = "Zoom out")
                    }
                }

                // GPS & Attribution Pill
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(12.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
                        Text("Tiles © OpenStreetMap / CARTO", style = MaterialTheme.typography.labelSmall)
                        if (gpsStatusMessage.isNotBlank()) {
                            Text(gpsStatusMessage, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                        }
                    }
                }

                // Empty State Overlay when no verified markers exist
                if (verifiedMarkers.isEmpty() && selectedMarker == null) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(12.dp)
                    ) {
                        UnavailableDataCard(
                            title = "No Verified Markers in This View",
                            explanation = if (isGoogleMapsKeyConfigured) {
                                "Use Search above to query Google Places API, or publish verified destinations with coordinates in Admin Dashboard."
                            } else {
                                "No verified Firestore coordinates match this filter, and GOOGLE_MAPS_API_KEY is not configured in Secrets."
                            },
                            icon = Icons.Filled.LocationSearching
                        )
                    }
                }

                // Selected Marker Detail Card with Distance & Route Planning
                selectedMarker?.let { marker ->
                    val distanceKm = gpsLocation?.let { haversineKm(it.latitude, it.longitude, marker.latitude, marker.longitude) }
                    Card(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .padding(12.dp),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(marker.title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                                IconButton(onClick = { selectedMarker = null }, modifier = Modifier.size(28.dp)) {
                                    Icon(Icons.Filled.Close, contentDescription = "Close card")
                                }
                            }
                            Text(marker.subtitle, style = MaterialTheme.typography.bodyMedium)
                            Text(
                                text = buildString {
                                    append("Source: ${marker.source}")
                                    if (distanceKm != null) append(" • ${String.format("%.1f", distanceKm)} km away")
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.secondary
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = {
                                        val uri = Uri.parse("https://www.google.com/maps/dir/?api=1&destination=${marker.latitude},${marker.longitude}")
                                        context.startActivity(Intent(Intent.ACTION_VIEW, uri))
                                    },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Filled.Directions, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Route")
                                }
                                if (marker.destinationId != null) {
                                    OutlinedButton(
                                        onClick = { onOpenDestination(marker.destinationId) },
                                        modifier = Modifier.weight(1f)
                                    ) { Text("Details") }
                                } else if (marker.eventId != null) {
                                    OutlinedButton(
                                        onClick = { onOpenRealityEvent(marker.eventId) },
                                        modifier = Modifier.weight(1f)
                                    ) { Text("Open Event") }
                                } else if (marker.activityId != null) {
                                    OutlinedButton(
                                        onClick = { onOpenTeamActivity(marker.activityId) },
                                        modifier = Modifier.weight(1f)
                                    ) { Text("Open Team-Up") }
                                } else if (marker.missionId != null) {
                                    OutlinedButton(
                                        onClick = onOpenMissions,
                                        modifier = Modifier.weight(1f)
                                    ) { Text("Open Mission") }
                                } else if (marker.challengeId != null) {
                                    OutlinedButton(
                                        onClick = onOpenChallenges,
                                        modifier = Modifier.weight(1f)
                                    ) { Text("Challenge") }
                                } else if (marker.adventureId != null) {
                                    OutlinedButton(
                                        onClick = { onOpenAdventureWorld(marker.adventureId) },
                                        modifier = Modifier.weight(1f)
                                    ) { Text("Enter World") }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SlippyTileMapView(
    centerLat: Double,
    centerLng: Double,
    zoom: Int,
    markers: List<VerifiedMapMarker>,
    gpsLocation: GpsCoordinates?,
    onPan: (dLat: Double, dLng: Double) -> Unit,
    onSelectMarker: (VerifiedMapMarker) -> Unit
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }
    val tileSizeDp = 256.dp
    val tileSizePx = with(density) { tileSizeDp.toPx() }

    val n = 2.0.pow(zoom.toDouble())
    val centerTileX = (centerLng + 180.0) / 360.0 * n
    val latRad = Math.toRadians(centerLat)
    val centerTileY = (1.0 - ln(tan(latRad) + 1.0 / cos(latRad)) / PI) / 2.0 * n

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFE5EEF5))
            .onSizeChanged { canvasSize = it }
            .pointerInput(zoom) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    val degPerPxLng = 360.0 / (tileSizePx * n)
                    val degPerPxLat = 170.0 / (tileSizePx * n)
                    onPan(dragAmount.y * degPerPxLat, -dragAmount.x * degPerPxLng)
                }
            }
    ) {
        if (canvasSize.width > 0 && canvasSize.height > 0) {
            val halfW = canvasSize.width / 2f
            val halfH = canvasSize.height / 2f
            val baseX = floor(centerTileX).toInt()
            val baseY = floor(centerTileY).toInt()
            val fracX = (centerTileX - baseX) * tileSizePx
            val fracY = (centerTileY - baseY) * tileSizePx
            val maxTile = n.toInt()

            for (dx in -2..2) {
                for (dy in -2..2) {
                    val tx = ((baseX + dx) % maxTile + maxTile) % maxTile
                    val ty = baseY + dy
                    if (ty in 0 until maxTile) {
                        val px = halfW - fracX.toFloat() + dx * tileSizePx
                        val py = halfH - fracY.toFloat() + dy * tileSizePx
                        val tileUrl = "https://a.basemaps.cartocdn.com/rastertiles/voyager/$zoom/$tx/$ty.png"
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(tileUrl)
                                .addHeader("User-Agent", "NILORA-Android-TravelApp/1.0")
                                .crossfade(false)
                                .build(),
                            contentDescription = null,
                            contentScale = ContentScale.FillBounds,
                            modifier = Modifier
                                .offset { IntOffset(px.roundToInt(), py.roundToInt()) }
                                .size(tileSizeDp)
                        )
                    }
                }
            }

            // Plot User GPS Dot
            if (gpsLocation != null) {
                val gx = ((gpsLocation.longitude + 180.0) / 360.0 * n - centerTileX) * tileSizePx + halfW
                val gLatRad = Math.toRadians(gpsLocation.latitude)
                val gy = ((1.0 - ln(tan(gLatRad) + 1.0 / cos(gLatRad)) / PI) / 2.0 * n - centerTileY) * tileSizePx + halfH
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawCircle(
                        color = Turquoise.copy(alpha = 0.25f),
                        radius = 28f,
                        center = Offset(gx.toFloat(), gy.toFloat())
                    )
                    drawCircle(
                        color = Turquoise,
                        radius = 12f,
                        center = Offset(gx.toFloat(), gy.toFloat())
                    )
                }
            }

            // Plot Verified Destination / Place Markers
            markers.forEach { marker ->
                val mx = ((marker.longitude + 180.0) / 360.0 * n - centerTileX) * tileSizePx + halfW
                val mLatRad = Math.toRadians(marker.latitude)
                val my = ((1.0 - ln(tan(mLatRad) + 1.0 / cos(mLatRad)) / PI) / 2.0 * n - centerTileY) * tileSizePx + halfH
                if (mx in -40.0..(canvasSize.width + 40.0) && my in -40.0..(canvasSize.height + 40.0)) {
                    Surface(
                        modifier = Modifier
                            .offset { IntOffset((mx - 20f).roundToInt(), (my - 36f).roundToInt()) }
                            .clickable { onSelectMarker(marker) },
                        shape = CircleShape,
                        color = DeepNavy,
                        shadowElevation = 4.dp
                    ) {
                        Icon(
                            imageVector = Icons.Filled.LocationOn,
                            contentDescription = marker.title,
                            tint = WarmSand,
                            modifier = Modifier.padding(6.dp).size(24.dp)
                        )
                    }
                }
            }
        }
    }
}

private fun haversineKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    val r = 6371.0
    val dLat = Math.toRadians(lat2 - lat1)
    val dLon = Math.toRadians(lon2 - lon1)
    val a = sin(dLat / 2).pow(2) + cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2).pow(2)
    return 2 * r * asin(sqrt(a))
}
