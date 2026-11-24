package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.data.model.*
import com.example.ui.components.CommunicationHubTabs
import com.example.ui.components.NiloraFooter
import com.example.ui.components.NiloraSearchFilterBar
import com.example.ui.components.NiloraTopBar
import com.example.ui.components.UnavailableDataCard
import com.example.ui.theme.*
import com.example.ui.viewmodel.CommunityDiscoveryFilters

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun GlobalCommunityScreen(
    currentUserId: String,
    profilesState: UiState<List<PublicProfile>>,
    blockedState: UiState<List<BlockedUserRecord>>,
    requestsState: UiState<List<ConnectionRequest>>,
    filters: CommunityDiscoveryFilters,
    onSearchChange: (String) -> Unit,
    onLanguageFilterChange: (String) -> Unit,
    onCountryFilterChange: (String) -> Unit,
    onInterestFilterChange: (String) -> Unit,
    onToggleLanguageExchangeOnly: (Boolean) -> Unit,
    onSendConnectionRequest: (PublicProfile, String) -> Unit,
    onStartDirectChat: (PublicProfile) -> Unit,
    onBlockUser: (String, String) -> Unit,
    onReportUser: (String, String, String) -> Unit,
    onOpenConnectionRequests: () -> Unit,
    onOpenBlockedUsers: () -> Unit,
    onOpenExplorePlaces: () -> Unit,
    onOpenMap: () -> Unit,
    onOpenMessagesHub: () -> Unit = {},
    onOpenGroupsHub: () -> Unit = {}
) {
    val blockedIds = remember(blockedState) {
        (blockedState as? UiState.Success)?.data?.map { it.blockedUserId }?.toSet().orEmpty()
    }
    val requests = remember(requestsState) {
        (requestsState as? UiState.Success)?.data.orEmpty()
    }
    val pendingIncomingCount = requests.count { it.receiverId == currentUserId && it.status == "PENDING" }

    var connectTarget by remember { mutableStateOf<PublicProfile?>(null) }
    var connectIntro by remember { mutableStateOf("") }
    var reportTarget by remember { mutableStateOf<PublicProfile?>(null) }
    var reportReason by remember { mutableStateOf("Harassment or spam") }
    var reportDetails by remember { mutableStateOf("") }
    var countryDropdownOpen by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("discover_community_list"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Global Community",
                            style = MaterialTheme.typography.headlineLarge
                        )
                        Text(
                            text = "Connect worldwide for language exchange, culture, and travel",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    BadgedBox(
                        badge = {
                            if (pendingIncomingCount > 0) {
                                Badge { Text(pendingIncomingCount.toString()) }
                            }
                        }
                    ) {
                        FilledTonalIconButton(
                            onClick = onOpenConnectionRequests,
                            modifier = Modifier.testTag("open_connection_requests_btn")
                        ) {
                            Icon(Icons.Filled.GroupAdd, contentDescription = "Connection Requests")
                        }
                    }
                }

                CommunicationHubTabs(
                    activeTab = "DISCOVER",
                    pendingRequestsCount = pendingIncomingCount,
                    onSelectMessages = onOpenMessagesHub,
                    onSelectDiscover = {},
                    onSelectGroups = onOpenGroupsHub
                )

                // Quick switching bar keeping Phase 1 Places/Map immediately accessible
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    FilledTonalButton(
                        onClick = onOpenExplorePlaces,
                        modifier = Modifier.weight(1f).testTag("discover_places_shortcut")
                    ) {
                        Icon(Icons.Filled.Explore, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Places", maxLines = 1)
                    }
                    OutlinedButton(
                        onClick = onOpenMap,
                        modifier = Modifier.weight(1f).testTag("discover_map_shortcut")
                    ) {
                        Icon(Icons.Filled.Map, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Map", maxLines = 1)
                    }
                    OutlinedButton(
                        onClick = onOpenBlockedUsers,
                        modifier = Modifier.weight(1f).testTag("discover_blocked_shortcut")
                    ) {
                        Icon(Icons.Filled.Block, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Blocked", maxLines = 1)
                    }
                }
            }
        }

        item {
            NiloraSearchFilterBar(
                query = filters.searchQuery,
                placeholder = "Search members by name, city, bio, or language",
                onQueryChange = onSearchChange,
                testTag = "community_search_input"
            )
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        FilterChip(
                            selected = filters.languageExchangeOnly,
                            onClick = { onToggleLanguageExchangeOnly(!filters.languageExchangeOnly) },
                            label = { Text("Language Exchange") },
                            leadingIcon = { Icon(Icons.Filled.Translate, contentDescription = null, modifier = Modifier.size(16.dp)) },
                            modifier = Modifier.testTag("filter_lang_exchange")
                        )
                    }
                    item {
                        ExposedDropdownMenuBox(
                            expanded = countryDropdownOpen,
                            onExpandedChange = { countryDropdownOpen = !countryDropdownOpen }
                        ) {
                            FilterChip(
                                selected = filters.countryFilter != "ALL",
                                onClick = { countryDropdownOpen = true },
                                label = { Text(if (filters.countryFilter == "ALL") "Country: All" else filters.countryFilter) },
                                trailingIcon = { Icon(Icons.Filled.ArrowDropDown, contentDescription = null) },
                                modifier = Modifier.menuAnchor()
                            )
                            ExposedDropdownMenu(
                                expanded = countryDropdownOpen,
                                onDismissRequest = { countryDropdownOpen = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("All Countries") },
                                    onClick = {
                                        onCountryFilterChange("ALL")
                                        countryDropdownOpen = false
                                    }
                                )
                                NiloraTaxonomy.countryDialCodes.forEach { info ->
                                    DropdownMenuItem(
                                        text = { Text("${info.countryName} (${info.iso2})") },
                                        onClick = {
                                            onCountryFilterChange(info.countryName)
                                            countryDropdownOpen = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                    item {
                        FilterChip(
                            selected = filters.languageFilter == "ALL",
                            onClick = { onLanguageFilterChange("ALL") },
                            label = { Text("All Languages") }
                        )
                    }
                    items(NiloraTaxonomy.worldLanguages.take(6)) { lang ->
                        FilterChip(
                            selected = filters.languageFilter == lang.code,
                            onClick = {
                                onLanguageFilterChange(if (filters.languageFilter == lang.code) "ALL" else lang.code)
                            },
                            label = { Text("${lang.englishName} (${lang.code})") },
                            modifier = Modifier.testTag("filter_lang_${lang.code}")
                        )
                    }
                }

                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        FilterChip(
                            selected = filters.interestFilter == "ALL",
                            onClick = { onInterestFilterChange("ALL") },
                            label = { Text("All Interests") }
                        )
                    }
                    items(NiloraTaxonomy.communityInterests) { interest ->
                        FilterChip(
                            selected = filters.interestFilter == interest,
                            onClick = {
                                onInterestFilterChange(if (filters.interestFilter == interest) "ALL" else interest)
                            },
                            label = { Text(interest) }
                        )
                    }
                }
            }
        }

        item {
            Surface(
                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.PrivacyTip, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                    Text(
                        text = "Only members who voluntarily choose Public visibility appear here. Exact GPS, email, and phone remain private.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
        }

        when (profilesState) {
            is UiState.Loading -> {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
            }
            is UiState.Error -> {
                item {
                    UnavailableDataCard(
                        title = "Community Directory Unavailable",
                        explanation = profilesState.message
                    )
                }
            }
            is UiState.Success -> {
                val filtered = profilesState.data.filter { profile ->
                    profile.userId != currentUserId &&
                        profile.userId !in blockedIds &&
                        (!filters.languageExchangeOnly || profile.openToLanguageExchange) &&
                        (filters.countryFilter == "ALL" || profile.country.equals(filters.countryFilter, ignoreCase = true)) &&
                        (filters.languageFilter == "ALL" ||
                            profile.nativeLanguage.equals(filters.languageFilter, ignoreCase = true) ||
                            profile.spokenLanguages.any { it.equals(filters.languageFilter, ignoreCase = true) } ||
                            profile.learningLanguages.any { it.equals(filters.languageFilter, ignoreCase = true) }) &&
                        (filters.interestFilter == "ALL" ||
                            profile.interests.any { it.equals(filters.interestFilter, ignoreCase = true) }) &&
                        (filters.searchQuery.isBlank() ||
                            profile.displayName.contains(filters.searchQuery, ignoreCase = true) ||
                            profile.country.contains(filters.searchQuery, ignoreCase = true) ||
                            profile.city.contains(filters.searchQuery, ignoreCase = true) ||
                            profile.bio.contains(filters.searchQuery, ignoreCase = true))
                }

                if (filtered.isEmpty()) {
                    item {
                        UnavailableDataCard(
                            title = "No Discoverable Members Match",
                            explanation = "No public NILORA profiles currently match these filters. Real member accounts appear as users set their profile visibility to Public.",
                            icon = Icons.Filled.PeopleOutline
                        )
                    }
                } else {
                    items(filtered, key = { it.userId }) { member ->
                        val relation = requests.firstOrNull {
                            it.senderId == member.userId || it.receiverId == member.userId
                        }
                        MemberDiscoveryCard(
                            member = member,
                            connectionStatus = relation?.status,
                            onMessageClick = { onStartDirectChat(member) },
                            onConnectClick = {
                                connectTarget = member
                                connectIntro = ""
                            },
                            onBlockClick = { onBlockUser(member.userId, member.displayName) },
                            onReportClick = { reportTarget = member }
                        )
                    }
                }
            }
        }

        item { NiloraFooter() }
    }

    connectTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { connectTarget = null },
            title = { Text("Connect with ${target.displayName}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Send an optional introduction message. You can practice ${NiloraTaxonomy.languageDisplayLabel(target.nativeLanguage)} or explore shared interests.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    OutlinedTextField(
                        value = connectIntro,
                        onValueChange = { connectIntro = it },
                        label = { Text("Introduction (Optional)") },
                        minLines = 2,
                        modifier = Modifier.fillMaxWidth().testTag("connect_intro_input")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onSendConnectionRequest(target, connectIntro)
                        connectTarget = null
                    },
                    modifier = Modifier.testTag("confirm_send_connection")
                ) { Text("Send Request") }
            },
            dismissButton = {
                TextButton(onClick = { connectTarget = null }) { Text("Cancel") }
            }
        )
    }

    reportTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { reportTarget = null },
            title = { Text("Report ${target.displayName}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = reportReason,
                        onValueChange = { reportReason = it },
                        label = { Text("Reason") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = reportDetails,
                        onValueChange = { reportDetails = it },
                        label = { Text("Additional details (Optional)") },
                        minLines = 2,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onReportUser(target.userId, reportReason, reportDetails)
                        reportTarget = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ErrorCoral)
                ) { Text("Submit Report") }
            },
            dismissButton = {
                TextButton(onClick = { reportTarget = null }) { Text("Cancel") }
            }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MemberDiscoveryCard(
    member: PublicProfile,
    connectionStatus: String?,
    onMessageClick: () -> Unit,
    onConnectClick: () -> Unit,
    onBlockClick: () -> Unit,
    onReportClick: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("member_card_${member.userId}"),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (member.photoUri.isNotBlank()) {
                        AsyncImage(
                            model = member.photoUri,
                            contentDescription = "${member.displayName} avatar",
                            modifier = Modifier.size(52.dp).clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Surface(
                            modifier = Modifier.size(52.dp),
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = member.displayName.take(2).uppercase().ifBlank { "NL" },
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Column {
                        Text(member.displayName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        val loc = listOfNotNull(
                            member.city.takeIf { it.isNotBlank() },
                            member.country.takeIf { it.isNotBlank() }
                        ).joinToString(", ")
                        Text(loc.ifBlank { "Global" }, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                Box {
                    IconButton(
                        onClick = { menuExpanded = true },
                        modifier = Modifier.testTag("member_menu_${member.userId}")
                    ) {
                        Icon(Icons.Filled.MoreVert, contentDescription = "Member actions")
                    }
                    DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                        DropdownMenuItem(
                            text = { Text("Block User") },
                            leadingIcon = { Icon(Icons.Filled.Block, contentDescription = null) },
                            onClick = {
                                menuExpanded = false
                                onBlockClick()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Report Profile") },
                            leadingIcon = { Icon(Icons.Filled.Flag, contentDescription = null) },
                            onClick = {
                                menuExpanded = false
                                onReportClick()
                            }
                        )
                    }
                }
            }

            if (member.bio.isNotBlank()) {
                Text(member.bio, style = MaterialTheme.typography.bodyMedium)
            }

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                AssistChip(
                    onClick = {},
                    label = { Text("Native: ${member.nativeLanguage}") }
                )
                if (member.learningLanguages.isNotEmpty()) {
                    AssistChip(
                        onClick = {},
                        label = { Text("Learning: ${member.learningLanguages.joinToString()}") }
                    )
                }
                if (member.openToLanguageExchange) {
                    Surface(
                        color = VerifiedEmeraldContainer,
                        shape = RoundedCornerShape(999.dp)
                    ) {
                        Text(
                            text = "Language Exchange",
                            style = MaterialTheme.typography.labelSmall,
                            color = TurquoiseOnContainer,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }
                member.interests.take(3).forEach { interest ->
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(999.dp)
                    ) {
                        Text(
                            text = interest,
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                Button(
                    onClick = onMessageClick,
                    modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("message_member_${member.userId}")
                ) {
                    Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Message")
                }

                when (connectionStatus) {
                    "ACCEPTED" -> {
                        OutlinedButton(
                            onClick = {},
                            enabled = false,
                            modifier = Modifier.weight(1f).heightIn(min = 48.dp)
                        ) { Text("Connected") }
                    }
                    "PENDING" -> {
                        OutlinedButton(
                            onClick = {},
                            enabled = false,
                            modifier = Modifier.weight(1f).heightIn(min = 48.dp)
                        ) { Text("Pending") }
                    }
                    else -> {
                        FilledTonalButton(
                            onClick = onConnectClick,
                            modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("connect_member_${member.userId}")
                        ) {
                            Icon(Icons.Filled.PersonAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Connect")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ConnectionRequestsScreen(
    currentUserId: String,
    requestsState: UiState<List<ConnectionRequest>>,
    onRespond: (ConnectionRequest, String) -> Unit,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = { NiloraTopBar(title = "Connection Requests", onBackClick = onBack) }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            when (requestsState) {
                is UiState.Loading -> item { CircularProgressIndicator() }
                is UiState.Error -> item { UnavailableDataCard("Requests Unavailable", requestsState.message) }
                is UiState.Success -> {
                    val incoming = requestsState.data.filter { it.receiverId == currentUserId && it.status == "PENDING" }
                    val sent = requestsState.data.filter { it.senderId == currentUserId && it.status == "PENDING" }
                    val connected = requestsState.data.filter { it.status == "ACCEPTED" }

                    if (incoming.isEmpty() && sent.isEmpty() && connected.isEmpty()) {
                        item {
                            UnavailableDataCard(
                                title = "No Connection Activity Yet",
                                explanation = "Send or receive connection requests from the Global Community tab.",
                                icon = Icons.Filled.PeopleOutline
                            )
                        }
                    }

                    if (incoming.isNotEmpty()) {
                        item { Text("Incoming Requests (${incoming.size})", style = MaterialTheme.typography.titleLarge) }
                        items(incoming, key = { it.id }) { req ->
                            Card(modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text("${req.senderName} • ${req.senderCountry}", style = MaterialTheme.typography.titleMedium)
                                    Text("Native language: ${req.senderNativeLanguage}", style = MaterialTheme.typography.labelMedium)
                                    if (req.introMessage.isNotBlank()) {
                                        Text("\"${req.introMessage}\"", style = MaterialTheme.typography.bodyMedium)
                                    }
                                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                                        Button(
                                            onClick = { onRespond(req, "ACCEPTED") },
                                            modifier = Modifier.weight(1f).testTag("accept_request_${req.id}")
                                        ) { Text("Accept") }
                                        OutlinedButton(
                                            onClick = { onRespond(req, "DECLINED") },
                                            modifier = Modifier.weight(1f).testTag("decline_request_${req.id}")
                                        ) { Text("Decline") }
                                    }
                                }
                            }
                        }
                    }

                    if (sent.isNotEmpty()) {
                        item { Text("Sent Requests (${sent.size})", style = MaterialTheme.typography.titleLarge) }
                        items(sent, key = { it.id }) { req ->
                            Card(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(req.receiverName, style = MaterialTheme.typography.titleMedium)
                                        Text("${req.receiverCountry} • Awaiting response", style = MaterialTheme.typography.bodySmall)
                                    }
                                    OutlinedButton(onClick = { onRespond(req, "CANCELLED") }) {
                                        Text("Cancel")
                                    }
                                }
                            }
                        }
                    }

                    if (connected.isNotEmpty()) {
                        item { Text("Connections (${connected.size})", style = MaterialTheme.typography.titleLarge) }
                        items(connected, key = { it.id }) { req ->
                            val otherName = if (req.senderId == currentUserId) req.receiverName else req.senderName
                            val otherCountry = if (req.senderId == currentUserId) req.receiverCountry else req.senderCountry
                            Card(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(otherName, style = MaterialTheme.typography.titleMedium)
                                        Text(otherCountry, style = MaterialTheme.typography.bodySmall)
                                    }
                                    Icon(Icons.Filled.CheckCircle, contentDescription = "Connected", tint = VerifiedEmerald)
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
fun BlockedUsersScreen(
    blockedState: UiState<List<BlockedUserRecord>>,
    onUnblock: (String) -> Unit,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = { NiloraTopBar(title = "Blocked Users", onBackClick = onBack) }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            when (blockedState) {
                is UiState.Loading -> item { CircularProgressIndicator() }
                is UiState.Error -> item { UnavailableDataCard("Blocked List Unavailable", blockedState.message) }
                is UiState.Success -> {
                    if (blockedState.data.isEmpty()) {
                        item {
                            UnavailableDataCard(
                                title = "No Blocked Users",
                                explanation = "Blocked users are prevented server-side from messaging you, viewing your discoverable profile, or sending connection requests.",
                                icon = Icons.Filled.VerifiedUser
                            )
                        }
                    } else {
                        items(blockedState.data, key = { it.blockedUserId }) { blk ->
                            Card(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(blk.blockedUserName.ifBlank { blk.blockedUserId }, style = MaterialTheme.typography.titleMedium)
                                        if (blk.reason.isNotBlank()) {
                                            Text(blk.reason, style = MaterialTheme.typography.bodySmall)
                                        }
                                    }
                                    OutlinedButton(
                                        onClick = { onUnblock(blk.blockedUserId) },
                                        modifier = Modifier.testTag("unblock_${blk.blockedUserId}")
                                    ) { Text("Unblock") }
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
