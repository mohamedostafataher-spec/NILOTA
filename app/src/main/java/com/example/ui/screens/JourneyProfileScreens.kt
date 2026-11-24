package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.data.model.*
import com.example.niloralive.SubtitleDisplayMode
import com.example.ui.components.BookingStatusChip
import com.example.ui.components.NiloraFooter
import com.example.ui.components.NiloraNotificationCard
import com.example.ui.components.NiloraTopBar
import com.example.ui.components.UnavailableDataCard
import com.example.ui.theme.ErrorCoral

@Composable
fun MyJourneyScreen(
    favoritesState: UiState<List<FavoriteItem>>,
    travelerBookingsState: UiState<List<BookingRecord>>,
    providerBookingsState: UiState<List<BookingRecord>>,
    notesState: UiState<List<TravelNote>>,
    roomsState: UiState<List<ConversationRoom>>,
    onOpenSavedPlaces: () -> Unit,
    onOpenBooking: (String) -> Unit,
    onSaveNote: (title: String, content: String, city: String, date: String) -> Unit,
    onDeleteNote: (String) -> Unit,
    onOpenProfile: () -> Unit,
    onOpenProviderRegistration: () -> Unit
) {
    var noteTitle by remember { mutableStateOf("") }
    var noteContent by remember { mutableStateOf("") }
    var noteCity by remember { mutableStateOf("Cairo") }
    var showAddNote by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("My Journey", style = MaterialTheme.typography.headlineLarge)
                Row {
                    IconButton(onClick = onOpenSavedPlaces, modifier = Modifier.testTag("journey_saved_places_btn")) {
                        Icon(Icons.Filled.Bookmark, contentDescription = "Saved Places")
                    }
                    IconButton(onClick = onOpenProfile, modifier = Modifier.testTag("journey_profile_btn")) {
                        Icon(Icons.Filled.Person, contentDescription = "User Profile")
                    }
                }
            }
        }

        // Saved Places Summary
        item {
            val savedCount = (favoritesState as? UiState.Success)?.data?.size ?: 0
            Card(modifier = Modifier.fillMaxWidth().clickable(onClick = onOpenSavedPlaces)) {
                Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column {
                        Text("Saved Places & Activities", style = MaterialTheme.typography.titleMedium)
                        Text("$savedCount saved items", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Icon(Icons.Filled.ArrowForwardIos, contentDescription = null, modifier = Modifier.size(16.dp))
                }
            }
        }

        // Traveler Booking Requests & Confirmed Reservations
        item { Text("Your Booking Requests & Reservations", style = MaterialTheme.typography.titleLarge) }
        when (travelerBookingsState) {
            is UiState.Loading -> item { LinearProgressIndicator(modifier = Modifier.fillMaxWidth()) }
            is UiState.Error -> item { UnavailableDataCard("Bookings Unavailable", travelerBookingsState.message) }
            is UiState.Success -> {
                val list = travelerBookingsState.data
                if (list.isEmpty()) {
                    item {
                        Text("No booking requests submitted yet.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    items(list) { bk ->
                        Card(modifier = Modifier.fillMaxWidth().clickable { onOpenBooking(bk.id) }) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(bk.activityTitle, style = MaterialTheme.typography.titleMedium)
                                    BookingStatusChip(status = bk.status)
                                }
                                Text("${bk.bookingReference} • ${bk.requestedDate} • ${bk.participantsCount} guests", style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }
            }
        }

        // Incoming Provider Bookings (if user is a provider)
        val incomingProviderBookings = (providerBookingsState as? UiState.Success)?.data.orEmpty()
        if (incomingProviderBookings.isNotEmpty()) {
            item { Text("Incoming Provider Requests", style = MaterialTheme.typography.titleLarge) }
            items(incomingProviderBookings) { bk ->
                Card(modifier = Modifier.fillMaxWidth().clickable { onOpenBooking(bk.id) }) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("${bk.travelerName} — ${bk.activityTitle}", style = MaterialTheme.typography.titleMedium)
                            BookingStatusChip(status = bk.status)
                        }
                        Text("${bk.bookingReference} • ${bk.requestedDate}", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }

        // Personal Travel Notes
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Personal Travel Notes", style = MaterialTheme.typography.titleLarge)
                FilledTonalButton(onClick = { showAddNote = !showAddNote }, modifier = Modifier.testTag("journey_add_note_btn")) {
                    Icon(Icons.Filled.NoteAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Add Note")
                }
            }
        }

        if (showAddNote) {
            item {
                Card {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = noteTitle, onValueChange = { noteTitle = it }, label = { Text("Note Title") }, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(value = noteCity, onValueChange = { noteCity = it }, label = { Text("City") }, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(value = noteContent, onValueChange = { noteContent = it }, label = { Text("Itinerary / Notes") }, minLines = 2, modifier = Modifier.fillMaxWidth())
                        Button(
                            onClick = {
                                if (noteTitle.isNotBlank() && noteContent.isNotBlank()) {
                                    onSaveNote(noteTitle, noteContent, noteCity, "")
                                    noteTitle = ""
                                    noteContent = ""
                                    showAddNote = false
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("Save Travel Note") }
                    }
                }
            }
        }

        when (notesState) {
            is UiState.Loading -> item { LinearProgressIndicator(modifier = Modifier.fillMaxWidth()) }
            is UiState.Error -> item { UnavailableDataCard("Notes Unavailable", notesState.message) }
            is UiState.Success -> {
                if (notesState.data.isEmpty()) {
                    item { Text("No personal travel notes saved yet.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                } else {
                    items(notesState.data) { note ->
                        Card {
                            Row(modifier = Modifier.fillMaxWidth().padding(14.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(note.title, style = MaterialTheme.typography.titleMedium)
                                    if (note.cityName.isNotBlank()) {
                                        Text(note.cityName, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                                    }
                                    Text(note.content, style = MaterialTheme.typography.bodyMedium)
                                }
                                IconButton(onClick = { onDeleteNote(note.id) }) {
                                    Icon(Icons.Filled.DeleteOutline, contentDescription = "Delete note")
                                }
                            }
                        }
                    }
                }
            }
        }

        // Translation Rooms History (only where user created/joined)
        val rooms = (roomsState as? UiState.Success)?.data.orEmpty()
        if (rooms.isNotEmpty()) {
            item { Text("Recent Translation Sessions (${rooms.size})", style = MaterialTheme.typography.titleLarge) }
            items(rooms.take(5)) { room ->
                Card {
                    Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
                        Text("Room ${room.roomCode} (${room.status})", style = MaterialTheme.typography.titleMedium)
                        Text("Transcript retained: ${if (room.retainTranscript) "Yes" else "Temporary"}", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }

        item {
            OutlinedButton(onClick = onOpenProviderRegistration, modifier = Modifier.fillMaxWidth()) {
                Text("Local Connect: Become a Verified Provider")
            }
        }

        item { NiloraFooter() }
    }
}

@Composable
fun SavedPlacesScreen(
    favoritesState: UiState<List<FavoriteItem>>,
    onRemoveFavorite: (FavoriteItem) -> Unit,
    onOpenDestination: (String) -> Unit,
    onOpenActivity: (String) -> Unit,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = { NiloraTopBar(title = "Saved Places", onBackClick = onBack) }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            when (favoritesState) {
                is UiState.Loading -> item { CircularProgressIndicator() }
                is UiState.Error -> item { UnavailableDataCard("Saved Places Unavailable", favoritesState.message) }
                is UiState.Success -> {
                    if (favoritesState.data.isEmpty()) {
                        item {
                            UnavailableDataCard(
                                title = "No Saved Places Yet",
                                explanation = "Bookmark verified destinations or activities to build your travel shortlist.",
                                icon = Icons.Filled.BookmarkBorder
                            )
                        }
                    } else {
                        items(favoritesState.data) { fav ->
                            Card(
                                modifier = Modifier.fillMaxWidth().clickable {
                                    if (fav.targetType == "ACTIVITY") onOpenActivity(fav.targetId) else onOpenDestination(fav.targetId)
                                }
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(fav.title, style = MaterialTheme.typography.titleMedium)
                                        Text("${fav.cityName} • ${fav.category}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    IconButton(onClick = { onRemoveFavorite(fav) }) {
                                        Icon(Icons.Filled.DeleteOutline, contentDescription = "Remove saved place")
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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun UserProfileScreen(
    userProfile: UserProfile?,
    privateInfo: UserPrivateInfo?,
    authEmail: String,
    onSaveProfile: (
        displayName: String,
        accountMode: String,
        langCode: String,
        country: String,
        photoUri: String,
        bio: String,
        privacyConsent: Boolean,
        retainHistory: Boolean,
        phone: String,
        emergencyContact: String,
        city: String,
        nativeLanguage: String,
        spokenLanguages: List<String>,
        learningLanguages: List<String>,
        interests: List<String>,
        profileVisibility: String,
        openToLanguageExchange: Boolean,
        phoneCountryCode: String
    ) -> Unit,
    onSignOut: () -> Unit,
    onDeleteAccount: () -> Unit,
    onBack: () -> Unit
) {
    var displayName by remember(userProfile) { mutableStateOf(userProfile?.displayName ?: "") }
    var accountMode by remember(userProfile) { mutableStateOf(userProfile?.accountMode ?: "GLOBAL_MEMBER") }
    var country by remember(userProfile) { mutableStateOf(userProfile?.country ?: "Egypt") }
    var city by remember(userProfile) { mutableStateOf(userProfile?.city ?: "") }
    var bio by remember(userProfile) { mutableStateOf(userProfile?.bio ?: "") }
    var photoUri by remember(userProfile) { mutableStateOf(userProfile?.photoUri ?: "") }
    var nativeLanguage by remember(userProfile) {
        mutableStateOf(userProfile?.nativeLanguage?.ifBlank { userProfile.preferredLanguage } ?: "en")
    }
    var spokenLanguages by remember(userProfile) {
        mutableStateOf(userProfile?.spokenLanguages?.toSet() ?: setOf(nativeLanguage))
    }
    var learningLanguages by remember(userProfile) {
        mutableStateOf(userProfile?.learningLanguages?.toSet() ?: emptySet())
    }
    var interests by remember(userProfile) {
        mutableStateOf(userProfile?.interests?.toSet() ?: emptySet())
    }
    var profileVisibility by remember(userProfile) {
        mutableStateOf(userProfile?.profileVisibility?.ifBlank { "PUBLIC" } ?: "PUBLIC")
    }
    var openToLanguageExchange by remember(userProfile) {
        mutableStateOf(userProfile?.openToLanguageExchange ?: true)
    }
    var phoneCountryCode by remember(privateInfo, userProfile) {
        mutableStateOf(
            privateInfo?.phoneCountryCode?.ifBlank {
                NiloraTaxonomy.dialInfoForCountry(userProfile?.country ?: "Egypt").dialCode
            } ?: "+20"
        )
    }
    var phone by remember(privateInfo) { mutableStateOf(privateInfo?.phone ?: "") }
    var emergencyContact by remember(privateInfo) { mutableStateOf(privateInfo?.emergencyContact ?: "") }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var countryExpanded by remember { mutableStateOf(false) }
    var dialExpanded by remember { mutableStateOf(false) }
    var nativeLangExpanded by remember { mutableStateOf(false) }
    var phoneError by remember { mutableStateOf<String?>(null) }

    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri: Uri? ->
        if (uri != null) photoUri = uri.toString()
    }

    Scaffold(
        topBar = { NiloraTopBar(title = "Global Profile Settings", subtitle = authEmail, onBackClick = onBack) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                if (photoUri.isNotBlank()) {
                    AsyncImage(
                        model = photoUri,
                        contentDescription = "Profile photo",
                        modifier = Modifier.size(68.dp).clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Surface(modifier = Modifier.size(68.dp), shape = CircleShape, color = MaterialTheme.colorScheme.secondaryContainer) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Filled.Person, contentDescription = "Avatar", modifier = Modifier.size(34.dp))
                        }
                    }
                }
                OutlinedButton(onClick = {
                    photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                }) {
                    Text("Change Photo (Photo Picker)")
                }
            }

            OutlinedTextField(
                value = displayName,
                onValueChange = { displayName = it },
                label = { Text("Display Name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("profile_name_input")
            )

            Text("Account Role", style = MaterialTheme.typography.titleSmall)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("GLOBAL_MEMBER" to "Global Member", "TRAVELER" to "Traveler", "LOCAL_PROVIDER" to "Local Guide / Host").forEach { (key, label) ->
                    FilterChip(
                        selected = accountMode == key,
                        onClick = { accountMode = key },
                        label = { Text(label) }
                    )
                }
            }

            ExposedDropdownMenuBox(expanded = countryExpanded, onExpandedChange = { countryExpanded = !countryExpanded }) {
                OutlinedTextField(
                    value = country,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Country") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = countryExpanded) },
                    modifier = Modifier.menuAnchor().fillMaxWidth()
                )
                ExposedDropdownMenu(expanded = countryExpanded, onDismissRequest = { countryExpanded = false }) {
                    NiloraTaxonomy.countryDialCodes.forEach { info ->
                        DropdownMenuItem(
                            text = { Text("${info.countryName} (${info.dialCode})") },
                            onClick = {
                                country = info.countryName
                                phoneCountryCode = info.dialCode
                                countryExpanded = false
                            }
                        )
                    }
                }
            }

            OutlinedTextField(
                value = city,
                onValueChange = { city = it },
                label = { Text("City (Optional)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("profile_city_input")
            )

            ExposedDropdownMenuBox(expanded = nativeLangExpanded, onExpandedChange = { nativeLangExpanded = !nativeLangExpanded }) {
                OutlinedTextField(
                    value = NiloraTaxonomy.languageDisplayLabel(nativeLanguage),
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Native Language") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = nativeLangExpanded) },
                    modifier = Modifier.menuAnchor().fillMaxWidth()
                )
                ExposedDropdownMenu(expanded = nativeLangExpanded, onDismissRequest = { nativeLangExpanded = false }) {
                    NiloraTaxonomy.worldLanguages.forEach { lang ->
                        DropdownMenuItem(
                            text = { Text("${lang.nativeName} — ${lang.englishName}") },
                            onClick = {
                                nativeLanguage = lang.code
                                spokenLanguages = spokenLanguages + lang.code
                                nativeLangExpanded = false
                            }
                        )
                    }
                }
            }

            Text("Languages I Speak", style = MaterialTheme.typography.titleSmall)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                NiloraTaxonomy.worldLanguages.take(8).forEach { lang ->
                    val checked = lang.code in spokenLanguages
                    FilterChip(
                        selected = checked,
                        onClick = {
                            spokenLanguages = if (checked && spokenLanguages.size > 1) spokenLanguages - lang.code else spokenLanguages + lang.code
                        },
                        label = { Text(lang.code) }
                    )
                }
            }

            Text("Languages I Want to Learn", style = MaterialTheme.typography.titleSmall)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                NiloraTaxonomy.worldLanguages.take(8).forEach { lang ->
                    val checked = lang.code in learningLanguages
                    FilterChip(
                        selected = checked,
                        onClick = {
                            learningLanguages = if (checked) learningLanguages - lang.code else learningLanguages + lang.code
                        },
                        label = { Text(lang.code) }
                    )
                }
            }

            Text("Community Interests", style = MaterialTheme.typography.titleSmall)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                NiloraTaxonomy.communityInterests.forEach { interest ->
                    val checked = interest in interests
                    FilterChip(
                        selected = checked,
                        onClick = { interests = if (checked) interests - interest else interests + interest },
                        label = { Text(interest) }
                    )
                }
            }

            OutlinedTextField(
                value = bio,
                onValueChange = { bio = it },
                label = { Text("Short Biography") },
                minLines = 2,
                modifier = Modifier.fillMaxWidth().testTag("profile_bio_input")
            )

            Text("Discovery & Privacy Visibility", style = MaterialTheme.typography.titleSmall)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("PUBLIC" to "Public", "CONNECTIONS_ONLY" to "Connections Only", "PRIVATE" to "Private").forEach { (key, label) ->
                    FilterChip(
                        selected = profileVisibility == key,
                        onClick = { profileVisibility = key },
                        label = { Text(label) },
                        modifier = Modifier.testTag("visibility_$key")
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Open to Language Exchange", style = MaterialTheme.typography.bodyMedium)
                Switch(checked = openToLanguageExchange, onCheckedChange = { openToLanguageExchange = it })
            }

            // International Phone with country dial code selector (strictly private in users/{uid}/private/info)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                Box(modifier = Modifier.width(120.dp)) {
                    ExposedDropdownMenuBox(expanded = dialExpanded, onExpandedChange = { dialExpanded = !dialExpanded }) {
                        OutlinedTextField(
                            value = phoneCountryCode,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Code") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = dialExpanded) },
                            modifier = Modifier.menuAnchor().fillMaxWidth()
                        )
                        ExposedDropdownMenu(expanded = dialExpanded, onDismissRequest = { dialExpanded = false }) {
                            NiloraTaxonomy.countryDialCodes.forEach { info ->
                                DropdownMenuItem(
                                    text = { Text("${info.iso2} ${info.dialCode}") },
                                    onClick = {
                                        phoneCountryCode = info.dialCode
                                        dialExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
                OutlinedTextField(
                    value = phone,
                    onValueChange = {
                        phone = it
                        phoneError = null
                    },
                    label = { Text("Private Phone Number") },
                    singleLine = true,
                    isError = phoneError != null,
                    supportingText = phoneError?.let { msg -> { Text(msg) } },
                    modifier = Modifier.weight(1f).testTag("profile_phone_input")
                )
            }

            OutlinedTextField(
                value = emergencyContact,
                onValueChange = { emergencyContact = it },
                label = { Text("Emergency Contact (Private)") },
                modifier = Modifier.fillMaxWidth()
            )

            Button(
                onClick = {
                    val cleanNational = phone.trim()
                    if (cleanNational.isNotBlank() && !NiloraTaxonomy.isValidInternationalPhone(phoneCountryCode, cleanNational)) {
                        phoneError = "Enter 7–15 digits for $phoneCountryCode"
                        return@Button
                    }
                    val e164 = if (cleanNational.startsWith("+")) cleanNational
                    else NiloraTaxonomy.formatInternationalPhone(phoneCountryCode, cleanNational)
                    onSaveProfile(
                        displayName,
                        accountMode,
                        userProfile?.preferredLanguage ?: "en",
                        country,
                        photoUri,
                        bio,
                        userProfile?.privacyConsent ?: true,
                        userProfile?.retainTranslationHistory ?: false,
                        e164,
                        emergencyContact,
                        city,
                        nativeLanguage,
                        spokenLanguages.toList(),
                        learningLanguages.toList(),
                        interests.toList(),
                        profileVisibility,
                        openToLanguageExchange,
                        phoneCountryCode
                    )
                },
                modifier = Modifier.fillMaxWidth().height(50.dp).testTag("save_profile_btn")
            ) {
                Text("Save Global Profile")
            }

            OutlinedButton(onClick = onSignOut, modifier = Modifier.fillMaxWidth().testTag("profile_sign_out_btn")) {
                Icon(Icons.Filled.Logout, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Sign Out")
            }

            TextButton(
                onClick = { showDeleteConfirm = true },
                modifier = Modifier.fillMaxWidth().testTag("delete_account_data_btn")
            ) {
                Text("Delete Account Data", color = ErrorCoral)
            }
            NiloraFooter()
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete NILORA Account Data?") },
            text = { Text("This permanently deletes your Firestore profile, discoverable community profile, private contact record, blocked list, saved places, travel notes, and notifications, then signs you out.") },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirm = false
                        onDeleteAccount()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ErrorCoral)
                ) { Text("Delete & Sign Out") }
            },
            dismissButton = { TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") } }
        )
    }
}

@Composable
fun ProfileHubScreen(
    userProfile: UserProfile?,
    authEmail: String,
    isAdmin: Boolean,
    unreadNotificationsCount: Int,
    pendingRequestsCount: Int,
    onEditProfile: () -> Unit,
    onOpenMyJourney: () -> Unit,
    onOpenSavedPlaces: () -> Unit,
    onOpenConnectionRequests: () -> Unit,
    onOpenBlockedUsers: () -> Unit,
    onOpenNotifications: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenAbout: () -> Unit,
    onOpenAdmin: () -> Unit,
    onSignOut: () -> Unit,
    onOpenNiloraMemory: () -> Unit = {},
    onOpenWorldPostcards: () -> Unit = {},
    onOpenCultureSwaps: () -> Unit = {},
    onOpenWorldQuests: () -> Unit = {},
    spokenLanguageCode: String = "en",
    targetTranslationCode: String = "ar-EG",
    subtitleDisplayMode: SubtitleDisplayMode = SubtitleDisplayMode.BOTH,
    preferOriginalAudio: Boolean = false,
    personalizationEnabled: Boolean = true,
    onUpdateLanguageIdentity: (spoken: String, target: String, mode: SubtitleDisplayMode, preferOrig: Boolean) -> Unit = { _, _, _, _ -> },
    onTogglePersonalization: (Boolean) -> Unit = {},
    onOpenLiveHub: () -> Unit = {}
) {
    val quickLangs = listOf("en" to "English", "ar-EG" to "مصري", "ar" to "فصحى", "ru" to "Рус", "fr" to "FR", "es" to "ES", "de" to "DE")

    LazyColumn(
        modifier = Modifier.fillMaxSize().testTag("profile_hub_list"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f))
            ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        val photo = userProfile?.photoUri.orEmpty()
                        if (photo.isNotBlank()) {
                            AsyncImage(
                                model = photo,
                                contentDescription = "My Profile Photo",
                                modifier = Modifier.size(64.dp).clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Surface(modifier = Modifier.size(64.dp), shape = CircleShape, color = MaterialTheme.colorScheme.secondaryContainer) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Filled.Person, contentDescription = "Avatar", modifier = Modifier.size(32.dp))
                                }
                            }
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = userProfile?.displayName?.ifBlank { "NILORA Member" } ?: "NILORA Member",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                            )
                            Text(authEmail, style = MaterialTheme.typography.bodySmall)
                            val loc = listOfNotNull(
                                userProfile?.city?.takeIf { it.isNotBlank() },
                                userProfile?.country?.takeIf { it.isNotBlank() }
                            ).joinToString(", ")
                            if (loc.isNotBlank()) {
                                Text("$loc • Visibility: ${userProfile?.profileVisibility ?: "PUBLIC"}", style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }
                    Button(
                        onClick = onEditProfile,
                        modifier = Modifier.fillMaxWidth().testTag("profile_hub_edit_btn")
                    ) {
                        Icon(Icons.Filled.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Edit Global Profile, Languages & Privacy")
                    }
                }
            }
        }

        // Phase 3: One Language Identity & Transparent Personalization Control Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth().testTag("my_world_language_identity_card"),
                shape = RoundedCornerShape(18.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("One Language Identity", style = MaterialTheme.typography.titleMedium)
                            Text("Synced across Live, Groups, Chats & Adventures", style = MaterialTheme.typography.bodySmall)
                        }
                        TextButton(onClick = onOpenLiveHub) { Text("Open Live") }
                    }
                    Text("I speak:", style = MaterialTheme.typography.labelMedium)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(quickLangs) { (code, label) ->
                            FilterChip(
                                selected = spokenLanguageCode == code,
                                onClick = { onUpdateLanguageIdentity(code, targetTranslationCode, subtitleDisplayMode, preferOriginalAudio) },
                                label = { Text(label) },
                                modifier = Modifier.testTag("identity_spoken_$code")
                            )
                        }
                    }
                    Text("Translate incoming speech/chat to:", style = MaterialTheme.typography.labelMedium)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(quickLangs) { (code, label) ->
                            FilterChip(
                                selected = targetTranslationCode == code,
                                onClick = { onUpdateLanguageIdentity(spokenLanguageCode, code, subtitleDisplayMode, preferOriginalAudio) },
                                label = { Text(label) },
                                modifier = Modifier.testTag("identity_target_$code")
                            )
                        }
                    }
                    Text("Live subtitles mode:", style = MaterialTheme.typography.labelMedium)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(SubtitleDisplayMode.entries) { mode ->
                            FilterChip(
                                selected = subtitleDisplayMode == mode,
                                onClick = { onUpdateLanguageIdentity(spokenLanguageCode, targetTranslationCode, mode, preferOriginalAudio) },
                                label = { Text(mode.label) },
                                modifier = Modifier.testTag("identity_subtitle_${mode.key}")
                            )
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Prefer Original Audio (No Auto-Dub)", style = MaterialTheme.typography.bodyMedium)
                            Text("Tap Speak on any subtitle card anytime.", style = MaterialTheme.typography.bodySmall)
                        }
                        Switch(
                            checked = preferOriginalAudio,
                            onCheckedChange = { onUpdateLanguageIdentity(spokenLanguageCode, targetTranslationCode, subtitleDisplayMode, it) },
                            modifier = Modifier.testTag("identity_prefer_original_switch")
                        )
                    }
                    HorizontalDivider()
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Transparent Personalization", style = MaterialTheme.typography.bodyMedium)
                            Text("Uses only your explicit city, saved places & interests.", style = MaterialTheme.typography.bodySmall)
                        }
                        Switch(
                            checked = personalizationEnabled,
                            onCheckedChange = onTogglePersonalization,
                            modifier = Modifier.testTag("identity_personalization_switch")
                        )
                    }
                }
            }
        }

        val items = listOfNotNull(
            Triple(Icons.Filled.AutoStories, "NILORA Memory Archive & Voice Consent", onOpenNiloraMemory),
            Triple(Icons.Filled.MarkunreadMailbox, "World Postcards", onOpenWorldPostcards),
            Triple(Icons.Filled.SwapHoriz, "Culture Swap Exchanges", onOpenCultureSwaps),
            Triple(Icons.Filled.EmojiEvents, "World Quests", onOpenWorldQuests),
            Triple(Icons.Filled.GroupAdd, "Connection Requests ($pendingRequestsCount pending)", onOpenConnectionRequests),
            Triple(Icons.Filled.Block, "Blocked Users", onOpenBlockedUsers),
            Triple(Icons.Filled.Luggage, "My Journey, Bookings & Notes", onOpenMyJourney),
            Triple(Icons.Filled.Bookmark, "Saved Places & Activities", onOpenSavedPlaces),
            Triple(Icons.Filled.Notifications, "Notifications ($unreadNotificationsCount unread)", onOpenNotifications),
            Triple(Icons.Filled.Settings, "Settings & Language (RTL/LTR)", onOpenSettings),
            Triple(Icons.Filled.Info, "About NILORA & Founder Attribution", onOpenAbout),
            if (isAdmin) Triple(Icons.Filled.AdminPanelSettings, "Admin Dashboard", onOpenAdmin) else null
        )

        items(items) { (icon, title, action) ->
            Card(modifier = Modifier.fillMaxWidth().clickable(onClick = action)) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Text(title, style = MaterialTheme.typography.titleMedium)
                    }
                    Icon(Icons.Filled.ArrowForwardIos, contentDescription = null, modifier = Modifier.size(16.dp))
                }
            }
        }

        item {
            OutlinedButton(onClick = onSignOut, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Filled.Logout, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Sign Out")
            }
        }

        item { NiloraFooter() }
    }
}

@Composable
fun NotificationsScreen(
    notificationsState: UiState<List<AppNotification>>,
    onMarkRead: (String) -> Unit,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = { NiloraTopBar(title = "Notifications", onBackClick = onBack) }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            when (notificationsState) {
                is UiState.Loading -> item { CircularProgressIndicator() }
                is UiState.Error -> item { UnavailableDataCard("Notifications Unavailable", notificationsState.message) }
                is UiState.Success -> {
                    if (notificationsState.data.isEmpty()) {
                        item {
                            UnavailableDataCard(
                                title = "No Notifications",
                                explanation = "Booking updates and verification notices appear here in real time.",
                                icon = Icons.Filled.NotificationsNone
                            )
                        }
                    } else {
                        items(notificationsState.data) { notif ->
                            NiloraNotificationCard(
                                notification = notif,
                                onClick = { if (!notif.readStatus) onMarkRead(notif.id) }
                            )
                        }
                    }
                }
            }
        }
    }
}
