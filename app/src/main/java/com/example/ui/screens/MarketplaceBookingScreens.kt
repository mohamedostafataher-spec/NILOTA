package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.model.*
import com.example.localization.AppStrings
import com.example.ui.components.BookingStatusChip
import com.example.ui.components.NiloraFooter
import com.example.ui.components.NiloraTopBar
import com.example.ui.components.UnavailableDataCard
import com.example.ui.components.VerifiedSourceBadge

@Composable
fun ActivityDetailsScreen(
    activity: ActivityListing?,
    isSaved: Boolean,
    onToggleSave: () -> Unit,
    onOpenProvider: (String) -> Unit,
    onBookRequest: (String) -> Unit,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            NiloraTopBar(
                title = activity?.title ?: "Activity Details",
                onBackClick = onBack,
                actions = {
                    if (activity != null) {
                        IconButton(onClick = onToggleSave) {
                            Icon(if (isSaved) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder, contentDescription = "Save")
                        }
                    }
                }
            )
        }
    ) { padding ->
        if (activity == null) {
            Box(modifier = Modifier.fillMaxSize().padding(padding).padding(20.dp)) {
                UnavailableDataCard("Activity Unavailable", "This verified activity could not be loaded.", actionLabel = "Back", onActionClick = onBack)
            }
        } else {
            Column(
                modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                VerifiedSourceBadge(
                    sourceName = activity.sourceName,
                    lastVerifiedAt = "",
                    verificationStatus = activity.verificationStatus
                )
                Text(activity.title, style = MaterialTheme.typography.headlineLarge)
                Text("${activity.cityName} • ${activity.category} • ${activity.durationLabel}", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.secondary)
                Text(activity.description, style = MaterialTheme.typography.bodyLarge)

                Text(
                    text = "Price: " + if (activity.priceOnRequest || activity.priceAmount == null) "Price on request" else "${activity.priceAmount} ${activity.currency}",
                    style = MaterialTheme.typography.titleMedium
                )
                Text("Availability: ${activity.availabilityStatus}", style = MaterialTheme.typography.bodyMedium)
                Text("Cancellation Policy: ${activity.cancellationPolicy}", style = MaterialTheme.typography.bodyMedium)
                if (activity.safetyRequirements.isNotBlank()) {
                    Text("Safety Requirements: ${activity.safetyRequirements}", style = MaterialTheme.typography.bodyMedium)
                }
                if (activity.includedServices.isNotEmpty()) {
                    Text("Included: ${activity.includedServices.joinToString()}", style = MaterialTheme.typography.bodyMedium)
                }
                if (activity.exclusions.isNotEmpty()) {
                    Text("Exclusions: ${activity.exclusions.joinToString()}", style = MaterialTheme.typography.bodyMedium)
                }

                OutlinedButton(
                    onClick = { onOpenProvider(activity.providerId) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Filled.Badge, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("View Provider: ${activity.providerName}")
                }

                Button(
                    onClick = { onBookRequest(activity.id) },
                    enabled = activity.availabilityStatus != "UNAVAILABLE",
                    modifier = Modifier.fillMaxWidth().height(52.dp).testTag("activity_book_request_btn")
                ) {
                    Text(if (activity.availabilityStatus == "UNAVAILABLE") "Currently Unavailable" else "Submit Booking Request")
                }
                NiloraFooter()
            }
        }
    }
}

@Composable
fun BookingRequestScreen(
    strings: AppStrings,
    activity: ActivityListing?,
    onSubmitBooking: (requestedDate: String, participants: Int, notes: String) -> Unit,
    onBack: () -> Unit
) {
    var requestedDate by remember { mutableStateOf("") }
    var participants by remember { mutableIntStateOf(1) }
    var notes by remember { mutableStateOf("") }
    var agreedConditions by remember { mutableStateOf(false) }

    Scaffold(
        topBar = { NiloraTopBar(title = "Booking Request", onBackClick = onBack) }
    ) { padding ->
        if (activity == null) {
            Box(modifier = Modifier.fillMaxSize().padding(padding).padding(20.dp)) {
                UnavailableDataCard("Activity Not Found", "Cannot create a booking request without a verified activity.")
            }
        } else {
            Column(
                modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(activity.title, style = MaterialTheme.typography.headlineMedium)
                Text("Provider: ${activity.providerName} • ${activity.cityName}", style = MaterialTheme.typography.bodyLarge)

                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Booking & Payment Integrity", style = MaterialTheme.typography.titleMedium)
                        Text(strings.requestSubmittedNote, style = MaterialTheme.typography.bodyMedium)
                        Text(
                            "Payment Gateway: Not configured yet (paymentStatus = NOT_CONFIGURED). No card details are collected or stored.",
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }

                OutlinedTextField(
                    value = requestedDate,
                    onValueChange = { requestedDate = it },
                    label = { Text("Preferred Date (YYYY-MM-DD)") },
                    placeholder = { Text("2026-12-15") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("booking_date_input")
                )

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Participants: $participants", style = MaterialTheme.typography.titleMedium)
                    OutlinedButton(onClick = { if (participants > 1) participants-- }) { Text("-") }
                    OutlinedButton(onClick = { if (participants < 50) participants++ }) { Text("+") }
                }

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Special requests / language preference") },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Cancellation Conditions: ${activity.cancellationPolicy}", style = MaterialTheme.typography.bodyMedium)

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { agreedConditions = !agreedConditions }
                ) {
                    Checkbox(
                        checked = agreedConditions,
                        onCheckedChange = { agreedConditions = it },
                        modifier = Modifier.testTag("booking_agree_checkbox")
                    )
                    Text("I reviewed the conditions and understand confirmation depends on provider approval.", style = MaterialTheme.typography.bodyMedium)
                }

                Button(
                    onClick = { onSubmitBooking(requestedDate, participants, notes) },
                    enabled = agreedConditions && requestedDate.isNotBlank(),
                    modifier = Modifier.fillMaxWidth().height(52.dp).testTag("booking_submit_btn")
                ) {
                    Text("Submit Booking Request")
                }
            }
        }
    }
}

@Composable
fun BookingDetailsScreen(
    booking: BookingRecord?,
    currentUserId: String,
    onUpdateStatus: (status: String, reason: String) -> Unit,
    onBack: () -> Unit
) {
    var cancelReason by remember { mutableStateOf("") }
    Scaffold(
        topBar = { NiloraTopBar(title = "Booking Details", subtitle = booking?.bookingReference, onBackClick = onBack) }
    ) { padding ->
        if (booking == null) {
            Box(modifier = Modifier.fillMaxSize().padding(padding).padding(20.dp)) {
                UnavailableDataCard("Booking Unavailable", "Booking record could not be loaded.")
            }
        } else {
            val isTraveler = booking.travelerId == currentUserId
            val isProvider = booking.providerId == currentUserId
            val isTerminal = booking.status in setOf("CANCELLED", "REJECTED")

            Column(
                modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                BookingStatusChip(status = booking.status)
                Text(booking.activityTitle, style = MaterialTheme.typography.headlineMedium)
                Text("Reference: ${booking.bookingReference}", style = MaterialTheme.typography.titleMedium)
                Text("Traveler: ${booking.travelerName}", style = MaterialTheme.typography.bodyLarge)
                Text("Provider: ${booking.providerName}", style = MaterialTheme.typography.bodyLarge)
                Text("Requested Date: ${booking.requestedDate}", style = MaterialTheme.typography.bodyLarge)
                Text("Participants: ${booking.participantsCount}", style = MaterialTheme.typography.bodyLarge)
                Text("Payment Status: ${booking.paymentStatus}", style = MaterialTheme.typography.bodyMedium)
                if (booking.notes.isNotBlank()) {
                    Text("Notes: ${booking.notes}", style = MaterialTheme.typography.bodyMedium)
                }
                if (booking.cancellationReason.isNotBlank()) {
                    Text("Status Note: ${booking.cancellationReason}", style = MaterialTheme.typography.bodyMedium)
                }

                if (!isTerminal) {
                    OutlinedTextField(
                        value = cancelReason,
                        onValueChange = { cancelReason = it },
                        label = { Text("Reason / Provider Note (Optional)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (isProvider) {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                            Button(
                                onClick = { onUpdateStatus("CONFIRMED", cancelReason) },
                                modifier = Modifier.weight(1f).testTag("booking_provider_confirm_btn")
                            ) {
                                Text("Confirm Booking")
                            }
                            OutlinedButton(
                                onClick = { onUpdateStatus("REJECTED", cancelReason) },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Reject")
                            }
                        }
                    }
                    if (isTraveler) {
                        OutlinedButton(
                            onClick = { onUpdateStatus("CANCELLED", cancelReason) },
                            modifier = Modifier.fillMaxWidth().testTag("booking_traveler_cancel_btn")
                        ) {
                            Text("Cancel Booking Request")
                        }
                    }
                }
                NiloraFooter()
            }
        }
    }
}

@Composable
fun ProviderProfileScreen(
    provider: ProviderProfile?,
    activities: List<ActivityListing>,
    onOpenActivity: (String) -> Unit,
    onReportProvider: (String) -> Unit,
    onBack: () -> Unit
) {
    var reportText by remember { mutableStateOf("") }
    var showReport by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            NiloraTopBar(
                title = provider?.fullName ?: "Provider Profile",
                onBackClick = onBack,
                actions = {
                    if (provider != null) {
                        IconButton(onClick = { showReport = true }) {
                            Icon(Icons.Filled.Flag, contentDescription = "Report provider")
                        }
                    }
                }
            )
        }
    ) { padding ->
        if (provider == null) {
            Box(modifier = Modifier.fillMaxSize().padding(padding).padding(20.dp)) {
                UnavailableDataCard("Provider Unavailable", "This local professional profile is not available.")
            }
        } else {
            Column(
                modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                VerifiedSourceBadge(
                    sourceName = "NILORA Credential Check",
                    lastVerifiedAt = provider.licenseNumber.ifBlank { "Reviewed" },
                    verificationStatus = provider.verificationStatus
                )
                Text(provider.fullName, style = MaterialTheme.typography.headlineLarge)
                Text("Role: ${provider.serviceType}", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.secondary)
                Text("Languages: ${provider.languages.joinToString()}", style = MaterialTheme.typography.bodyLarge)
                Text("Cities: ${provider.citiesCovered.joinToString()}", style = MaterialTheme.typography.bodyLarge)
                Text(provider.bio, style = MaterialTheme.typography.bodyLarge)
                if (provider.rejectionReason.isNotBlank()) {
                    Text("Review Note: ${provider.rejectionReason}", style = MaterialTheme.typography.bodyMedium)
                }

                HorizontalDivider()
                Text("Provider Activities (${activities.size})", style = MaterialTheme.typography.titleLarge)
                if (activities.isEmpty()) {
                    Text("No published activities from this provider yet.", style = MaterialTheme.typography.bodyMedium)
                } else {
                    activities.forEach { act ->
                        ActivitySummaryCard(act) { onOpenActivity(act.id) }
                    }
                }
                NiloraFooter()
            }
        }
    }

    if (showReport) {
        AlertDialog(
            onDismissRequest = { showReport = false },
            title = { Text("Report Provider") },
            text = { OutlinedTextField(value = reportText, onValueChange = { reportText = it }, label = { Text("Reason") }) },
            confirmButton = {
                Button(onClick = {
                    if (reportText.isNotBlank()) onReportProvider(reportText)
                    showReport = false
                }) { Text("Submit") }
            },
            dismissButton = { TextButton(onClick = { showReport = false }) { Text("Cancel") } }
        )
    }
}

@Composable
fun ProviderRegistrationScreen(
    existingApplication: ProviderProfile?,
    defaultName: String,
    currentUserId: String,
    onSubmitApplication: (ProviderProfile) -> Unit,
    onBack: () -> Unit
) {
    var fullName by remember(existingApplication) { mutableStateOf(existingApplication?.fullName?.ifBlank { defaultName } ?: defaultName) }
    var serviceType by remember(existingApplication) { mutableStateOf(existingApplication?.serviceType ?: "TOUR_GUIDE") }
    var languagesInput by remember(existingApplication) { mutableStateOf(existingApplication?.languages?.joinToString(", ") ?: "en, ar") }
    var citiesInput by remember(existingApplication) { mutableStateOf(existingApplication?.citiesCovered?.joinToString(", ") ?: "Cairo, Giza") }
    var bio by remember(existingApplication) { mutableStateOf(existingApplication?.bio ?: "") }
    var licenseNumber by remember(existingApplication) { mutableStateOf(existingApplication?.licenseNumber ?: "") }
    var docSummary by remember(existingApplication) { mutableStateOf(existingApplication?.documentSummary ?: "") }
    var docUri by remember(existingApplication) { mutableStateOf(existingApplication?.documentRefUri ?: "") }

    // Zero-permission Android Photo Picker per Play Policy
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) docUri = uri.toString()
    }

    Scaffold(
        topBar = { NiloraTopBar(title = "Local Connect Application", subtitle = "Provider Verification Workflow", onBackClick = onBack) }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (existingApplication != null) {
                VerifiedSourceBadge(
                    sourceName = "Current Application",
                    lastVerifiedAt = existingApplication.rejectionReason,
                    verificationStatus = existingApplication.verificationStatus
                )
            }

            Text(
                "Only administrator-approved providers appear as verified in Local Connect.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            OutlinedTextField(value = fullName, onValueChange = { fullName = it }, label = { Text("Full Legal / Professional Name") }, modifier = Modifier.fillMaxWidth())

            Text("Service Category", style = MaterialTheme.typography.titleMedium)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(listOf("TOUR_GUIDE", "TRANSLATOR", "TRAVEL_ASSISTANT", "TOUR_OPERATOR", "HOSPITALITY")) { type ->
                    FilterChip(selected = serviceType == type, onClick = { serviceType = type }, label = { Text(type) })
                }
            }

            OutlinedTextField(value = languagesInput, onValueChange = { languagesInput = it }, label = { Text("Spoken Languages (comma-separated)") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = citiesInput, onValueChange = { citiesInput = it }, label = { Text("Cities Covered (comma-separated)") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = licenseNumber, onValueChange = { licenseNumber = it }, label = { Text("License / Permit Number (if applicable)") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = docSummary, onValueChange = { docSummary = it }, label = { Text("Credential Document Description") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = bio, onValueChange = { bio = it }, label = { Text("Professional Bio & Experience") }, minLines = 3, modifier = Modifier.fillMaxWidth())

            OutlinedButton(
                onClick = {
                    photoPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Filled.UploadFile, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(if (docUri.isBlank()) "Attach Credential Photo (Photo Picker)" else "Credential Attached")
            }

            Button(
                onClick = {
                    onSubmitApplication(
                        ProviderProfile(
                            userId = currentUserId,
                            fullName = fullName,
                            serviceType = serviceType,
                            languages = languagesInput.split(",").map { it.trim() }.filter { it.isNotBlank() },
                            citiesCovered = citiesInput.split(",").map { it.trim() }.filter { it.isNotBlank() },
                            bio = bio,
                            licenseNumber = licenseNumber,
                            documentSummary = docSummary,
                            documentRefUri = docUri
                        )
                    )
                },
                enabled = fullName.isNotBlank() && bio.length >= 5 && docSummary.length >= 3,
                modifier = Modifier.fillMaxWidth().height(52.dp).testTag("provider_application_submit_btn")
            ) {
                Text("Submit for Admin Verification")
            }

            NiloraFooter()
        }
    }
}
