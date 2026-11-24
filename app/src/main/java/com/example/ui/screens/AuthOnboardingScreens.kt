package com.example.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import com.example.R
import com.example.data.model.NiloraLanguage
import com.example.data.model.NiloraTaxonomy
import com.example.data.model.UserProfile
import com.example.firebase.onGoogleSignInClicked
import com.example.localization.AppStrings
import com.example.ui.components.BrandLogoVariant
import com.example.ui.components.NiloraBrandSymbol
import com.example.ui.components.NiloraBrandWordmark
import com.example.ui.components.NiloraFooter
import com.example.ui.components.NiloraGeometricDivider
import com.example.ui.components.NiloraTopBar
import com.example.ui.theme.*
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    strings: AppStrings,
    onFinished: () -> Unit
) {
    LaunchedEffect(Unit) {
        delay(1200)
        onFinished()
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(listOf(DeepNavyDark, DeepNavy, DeepNavyLight))
            )
            .systemBarsPadding(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            NiloraBrandSymbol(
                size = 112.dp,
                variant = BrandLogoVariant.ON_DARK
            )
            Spacer(Modifier.height(22.dp))
            Text(
                text = "NILORA",
                style = MaterialTheme.typography.displayLarge,
                color = SoftIvory
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "GLOBAL SOUL — EGYPTIAN ORIGIN",
                style = MaterialTheme.typography.labelMedium,
                color = NileTurquoise,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = strings.brandVision,
                style = MaterialTheme.typography.labelLarge,
                color = WarmSand,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(28.dp))
            CircularProgressIndicator(
                color = NileTurquoise,
                strokeWidth = 3.dp,
                modifier = Modifier.size(32.dp)
            )
            Spacer(Modifier.height(36.dp))
            NiloraGeometricDivider()
            Spacer(Modifier.height(12.dp))
            Text(
                text = NiloraTaxonomy.FOUNDER_ATTRIBUTION,
                style = MaterialTheme.typography.labelSmall,
                color = SoftIvory.copy(alpha = 0.82f),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun LanguageSelectionScreen(
    currentLanguage: NiloraLanguage,
    strings: AppStrings,
    showBack: Boolean,
    onBack: () -> Unit,
    onSelectLanguage: (NiloraLanguage) -> Unit,
    onContinue: () -> Unit
) {
    Scaffold(
        topBar = {
            if (showBack) {
                NiloraTopBar(title = strings.chooseLanguageTitle, onBackClick = onBack)
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .testTag("language_options_list"),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Column {
                        if (!showBack) {
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = "NILORA",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.secondary
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(
                                text = strings.chooseLanguageTitle,
                                style = MaterialTheme.typography.headlineLarge
                            )
                            Spacer(Modifier.height(8.dp))
                        }
                        Text(
                            text = "Arabic supports full Right-to-Left (RTL) layout. English, French, and Russian use Left-to-Right (LTR).",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(8.dp))
                    }
                }
                items(NiloraLanguage.entries) { lang ->
                    val selected = lang == currentLanguage
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("language_option_${lang.code}")
                            .clickable { onSelectLanguage(lang) },
                        colors = CardDefaults.cardColors(
                            containerColor = if (selected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface
                        ),
                        border = CardDefaults.outlinedCardBorder().let {
                            androidx.compose.foundation.BorderStroke(
                                width = if (selected) 2.dp else 1.dp,
                                color = if (selected) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.outline
                            )
                        },
                        shape = RoundedCornerShape(18.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(18.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = lang.nativeName,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${lang.displayName} • ${if (lang.isRtl) "RTL" else "LTR"}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (selected) {
                                Icon(
                                    Icons.Filled.CheckCircle,
                                    contentDescription = "Selected",
                                    tint = MaterialTheme.colorScheme.secondary
                                )
                            }
                        }
                    }
                }
                item {
                    NiloraFooter()
                }
            }

            Button(
                onClick = onContinue,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("language_continue_button")
            ) {
                Text(strings.onboardingNext)
            }
        }
    }
}

@Composable
fun OnboardingScreen(
    strings: AppStrings,
    personalizationEnabled: Boolean = true,
    onTogglePersonalization: (Boolean) -> Unit = {},
    onSaveOptionalInterests: (city: String, interests: List<String>) -> Unit = { _, _ -> },
    onFinish: () -> Unit
) {
    var page by remember { mutableIntStateOf(0) }
    var optionalCity by remember { mutableStateOf("") }
    var selectedInterests by remember { mutableStateOf(setOf<String>()) }
    val interestOptions = listOf(
        "Egyptian Heritage", "Red Sea & Diving", "Local Food", "Football & Sports",
        "Cultural Swaps", "Photography", "Desert & Nature"
    )

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                NiloraBrandWordmark(
                    variant = BrandLogoVariant.ON_LIGHT,
                    showTagline = false,
                    symbolSize = 34.dp
                )
                TextButton(
                    onClick = onFinish,
                    modifier = Modifier.testTag("onboarding_skip_button")
                ) {
                    Text(strings.onboardingSkip)
                }
            }

            Spacer(Modifier.height(16.dp))

            if (page == 0) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.img_translator_art),
                        contentDescription = "NILORA Universal Live Translation",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(210.dp)
                            .clip(RoundedCornerShape(24.dp)),
                        contentScale = ContentScale.Crop
                    )
                    Text(
                        text = "Speak & Understand Worldwide",
                        style = MaterialTheme.typography.headlineMedium,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "Natural Egyptian Arabic (ar-EG) and global live translation across Face-to-Face, Two-Phone Rooms, Group Live chats, and 1:1 conversations.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(Icons.Filled.Mic, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Text("Microphone: Requested only when you tap Speak.", style = MaterialTheme.typography.bodySmall)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(Icons.Filled.LocationOn, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Text("Location: Optional — used only when filtering nearby events or map.", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            } else {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.img_hero_egypt),
                        contentDescription = "NILORA Explore & Together",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp)
                            .clip(RoundedCornerShape(20.dp)),
                        contentScale = ContentScale.Crop
                    )
                    Text(
                        text = "Personalize Your World (Optional)",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Pick your city or interests if you want tailored picks — or tap Skip anytime.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = optionalCity,
                        onValueChange = { optionalCity = it },
                        label = { Text("Home or Destination City (Optional)") },
                        placeholder = { Text("e.g., Cairo, Hurghada, London") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("onboarding_city_input")
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Interest-Based Discovery", style = MaterialTheme.typography.titleSmall)
                            Text("Can be turned off anytime in My World.", style = MaterialTheme.typography.bodySmall)
                        }
                        Switch(
                            checked = personalizationEnabled,
                            onCheckedChange = onTogglePersonalization,
                            modifier = Modifier.testTag("onboarding_personalization_switch")
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            interestOptions.chunked(2).forEach { pair ->
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    pair.forEach { tag ->
                                        FilterChip(
                                            selected = tag in selectedInterests,
                                            onClick = {
                                                selectedInterests = if (tag in selectedInterests) {
                                                    selectedInterests - tag
                                                } else selectedInterests + tag
                                            },
                                            label = { Text(tag) }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(18.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                listOf(0, 1).forEach { idx ->
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 4.dp)
                            .height(8.dp)
                            .width(if (idx == page) 28.dp else 8.dp)
                            .clip(CircleShape)
                            .background(
                                if (idx == page) MaterialTheme.colorScheme.secondary
                                else MaterialTheme.colorScheme.outline
                            )
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    onClick = {
                        if (page == 0) {
                            page = 1
                        } else {
                            if (optionalCity.isNotBlank() || selectedInterests.isNotEmpty()) {
                                onSaveOptionalInterests(optionalCity.trim(), selectedInterests.toList())
                            }
                            onFinish()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("onboarding_next_button")
                ) {
                    Text(if (page == 0) strings.onboardingNext else strings.onboardingStart)
                }
                NiloraFooter()
            }
        }
    }
}

@Composable
fun LoginScreen(
    strings: AppStrings,
    onAuthSuccess: () -> Unit,
    onOpenLanguageSelection: () -> Unit,
    onOpenPrivacyPolicy: () -> Unit,
    onOpenTerms: () -> Unit,
    onOpenAbout: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    NiloraBrandWordmark(
                        variant = BrandLogoVariant.ON_LIGHT,
                        showTagline = true,
                        symbolSize = 40.dp
                    )
                    OutlinedButton(
                        onClick = onOpenLanguageSelection,
                        modifier = Modifier.testTag("login_language_button")
                    ) {
                        Icon(Icons.Filled.Language, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Lang")
                    }
                }

                Spacer(Modifier.height(20.dp))
                Image(
                    painter = painterResource(id = R.drawable.img_hero_egypt),
                    contentDescription = "NILORA Egypt & Global Travel",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(190.dp)
                        .clip(RoundedCornerShape(24.dp)),
                    contentScale = ContentScale.Crop
                )

                Spacer(Modifier.height(24.dp))
                Text(
                    text = strings.signInTitle,
                    style = MaterialTheme.typography.headlineLarge
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = strings.signInSubtitle,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(Modifier.height(20.dp))
                if (errorMessage != null) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = ErrorCoralContainer),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Filled.ErrorOutline, contentDescription = null, tint = ErrorCoral)
                            Text(errorMessage.orEmpty(), color = ErrorCoral, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                    Spacer(Modifier.height(14.dp))
                }

                Button(
                    onClick = {
                        isLoading = true
                        errorMessage = null
                        onGoogleSignInClicked(
                            context = context,
                            credentialManager = credentialManager,
                            onAuthSuccess = {
                                isLoading = false
                                onAuthSuccess()
                            },
                            onAuthError = { msg ->
                                isLoading = false
                                errorMessage = msg
                            },
                            scope = scope,
                            onAuthCancelled = {
                                isLoading = false
                            }
                        )
                    },
                    enabled = !isLoading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .testTag("google_sign_in_button")
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(Icons.Filled.AccountCircle, contentDescription = null)
                        Spacer(Modifier.width(10.dp))
                        Text(strings.signInGoogleButton, style = MaterialTheme.typography.titleMedium)
                    }
                }

                Spacer(Modifier.height(12.dp))
                Text(
                    text = "New to NILORA? Signing in with Google automatically creates your secure account for global community, messaging, and travel. Account recovery & password reset are managed directly through your Google Account.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    TextButton(onClick = onOpenPrivacyPolicy) { Text(strings.privacyPolicyTitle) }
                    TextButton(onClick = onOpenTerms) { Text(strings.termsTitle) }
                    TextButton(onClick = onOpenAbout) { Text(strings.aboutTitle) }
                }
            }

            Spacer(Modifier.height(16.dp))
            NiloraFooter()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun RegistrationScreen(
    existingProfile: UserProfile?,
    defaultLanguage: NiloraLanguage,
    initialEmail: String,
    initialName: String,
    onSaveRegistration: (
        displayName: String,
        accountMode: String,
        languageCode: String,
        country: String,
        bio: String,
        privacyConsent: Boolean,
        retainHistory: Boolean,
        phone: String,
        city: String,
        nativeLanguage: String,
        openToLanguageExchange: Boolean,
        phoneCountryCode: String
    ) -> Unit,
    onOpenPrivacyPolicy: () -> Unit,
    onBack: (() -> Unit)? = null
) {
    var displayName by remember(existingProfile) {
        mutableStateOf(existingProfile?.displayName?.ifBlank { initialName } ?: initialName)
    }
    var accountMode by remember(existingProfile) {
        mutableStateOf(existingProfile?.accountMode ?: "GLOBAL_MEMBER")
    }
    var selectedLang by remember(existingProfile) {
        mutableStateOf(NiloraLanguage.fromCode(existingProfile?.preferredLanguage ?: defaultLanguage.code))
    }
    var nativeLangCode by remember(existingProfile) {
        mutableStateOf(existingProfile?.nativeLanguage?.ifBlank { defaultLanguage.code } ?: defaultLanguage.code)
    }
    var selectedCountry by remember(existingProfile) {
        mutableStateOf(existingProfile?.country?.ifBlank { "Egypt" } ?: "Egypt")
    }
    var city by remember(existingProfile) { mutableStateOf(existingProfile?.city ?: "") }
    var dialCode by remember(existingProfile) {
        mutableStateOf(NiloraTaxonomy.dialInfoForCountry(existingProfile?.country ?: "Egypt").dialCode)
    }
    var bio by remember(existingProfile) { mutableStateOf(existingProfile?.bio ?: "") }
    var phone by remember { mutableStateOf("") }
    var openToExchange by remember(existingProfile) { mutableStateOf(existingProfile?.openToLanguageExchange ?: true) }
    var privacyConsent by remember(existingProfile) { mutableStateOf(existingProfile?.privacyConsent ?: true) }
    var retainHistory by remember(existingProfile) { mutableStateOf(existingProfile?.retainTranslationHistory ?: false) }
    var countryExpanded by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            NiloraTopBar(
                title = "NILORA Account Setup",
                subtitle = initialEmail.ifBlank { "Set up your global communication profile" },
                onBackClick = onBack
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "Personalize your global NILORA profile",
                style = MaterialTheme.typography.headlineMedium
            )
            OutlinedTextField(
                value = displayName,
                onValueChange = { displayName = it },
                label = { Text("Full Name or Display Name") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("registration_name_input")
            )

            Text("Account Experience", style = MaterialTheme.typography.titleMedium)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                FilterChip(
                    selected = accountMode == "GLOBAL_MEMBER",
                    onClick = { accountMode = "GLOBAL_MEMBER" },
                    label = { Text("Global Member") },
                    leadingIcon = { Icon(Icons.Filled.Public, contentDescription = null, modifier = Modifier.size(18.dp)) }
                )
                FilterChip(
                    selected = accountMode == "TRAVELER",
                    onClick = { accountMode = "TRAVELER" },
                    label = { Text("Traveler") },
                    leadingIcon = { Icon(Icons.Filled.FlightTakeoff, contentDescription = null, modifier = Modifier.size(18.dp)) }
                )
                FilterChip(
                    selected = accountMode == "LOCAL_PROVIDER",
                    onClick = { accountMode = "LOCAL_PROVIDER" },
                    label = { Text("Local Guide / Host") },
                    leadingIcon = { Icon(Icons.Filled.Badge, contentDescription = null, modifier = Modifier.size(18.dp)) }
                )
            }

            ExposedDropdownMenuBox(
                expanded = countryExpanded,
                onExpandedChange = { countryExpanded = !countryExpanded }
            ) {
                OutlinedTextField(
                    value = "$selectedCountry ($dialCode)",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Country & Calling Code") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = countryExpanded) },
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth()
                )
                ExposedDropdownMenu(expanded = countryExpanded, onDismissRequest = { countryExpanded = false }) {
                    NiloraTaxonomy.countryDialCodes.forEach { info ->
                        DropdownMenuItem(
                            text = { Text("${info.countryName} (${info.dialCode})") },
                            onClick = {
                                selectedCountry = info.countryName
                                dialCode = info.dialCode
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
                modifier = Modifier.fillMaxWidth()
            )

            Text("App Interface Language", style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                NiloraLanguage.entries.forEach { lang ->
                    FilterChip(
                        selected = selectedLang == lang,
                        onClick = {
                            selectedLang = lang
                            nativeLangCode = if (lang == NiloraLanguage.ARABIC) "ar-EG" else lang.code
                        },
                        label = { Text(lang.code.uppercase()) }
                    )
                }
            }

            OutlinedTextField(
                value = phone,
                onValueChange = { phone = it },
                label = { Text("Private Phone ($dialCode, Optional)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = bio,
                onValueChange = { bio = it },
                label = { Text("Languages, cultural interests, or bio (Optional)") },
                minLines = 2,
                modifier = Modifier.fillMaxWidth()
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = openToExchange, onCheckedChange = { openToExchange = it })
                Text(
                    text = "Open to language & cultural exchange with verified NILORA members",
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = retainHistory, onCheckedChange = { retainHistory = it })
                Text(
                    text = "Allow optional saving of translation transcripts (audio recordings are never stored by default)",
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = privacyConsent, onCheckedChange = { privacyConsent = it })
                Column {
                    Text(
                        text = "I accept the NILORA Privacy Policy & Terms (Draft for legal review)",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    TextButton(onClick = onOpenPrivacyPolicy, contentPadding = PaddingValues(0.dp)) {
                        Text("Read Privacy Policy")
                    }
                }
            }

            Button(
                onClick = {
                    val formattedPhone = if (phone.isNotBlank()) NiloraTaxonomy.formatInternationalPhone(dialCode, phone) else ""
                    onSaveRegistration(
                        displayName.ifBlank { "NILORA Member" },
                        accountMode,
                        selectedLang.code,
                        selectedCountry,
                        bio,
                        privacyConsent,
                        retainHistory,
                        formattedPhone,
                        city,
                        nativeLangCode,
                        openToExchange,
                        dialCode
                    )
                },
                enabled = privacyConsent && displayName.isNotBlank(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("registration_submit_button")
            ) {
                Text("Save & Enter NILORA")
            }

            NiloraFooter()
        }
    }
}
