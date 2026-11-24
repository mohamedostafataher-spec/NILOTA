package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.NiloraLanguage
import com.example.data.model.NiloraTaxonomy
import androidx.compose.foundation.background
import com.example.localization.AppStrings
import com.example.ui.components.BrandLogoVariant
import com.example.ui.components.NiloraBrandWordmark
import com.example.ui.components.NiloraFooter
import com.example.ui.components.NiloraGeometricDivider
import com.example.ui.components.NiloraTopBar
import com.example.ui.theme.*

@Composable
fun SettingsScreen(
    strings: AppStrings,
    currentLanguage: NiloraLanguage,
    darkThemeOverride: String,
    retainTranscripts: Boolean,
    isAdmin: Boolean,
    onSelectLanguage: (NiloraLanguage) -> Unit,
    onSelectThemeMode: (String) -> Unit,
    onToggleRetainTranscripts: (Boolean) -> Unit,
    onOpenProfile: () -> Unit,
    onOpenProviderRegistration: () -> Unit,
    onOpenPrivacyPolicy: () -> Unit,
    onOpenTerms: () -> Unit,
    onOpenAbout: () -> Unit,
    onOpenAdminDashboard: () -> Unit,
    onSignOut: () -> Unit,
    onBack: () -> Unit,
    onOpenCapabilities: () -> Unit = {}
) {
    Scaffold(
        topBar = { NiloraTopBar(title = strings.settingsTitle, onBackClick = onBack) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Language & Direction (RTL / LTR)", style = MaterialTheme.typography.titleMedium)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(NiloraLanguage.entries) { lang ->
                    FilterChip(
                        selected = currentLanguage == lang,
                        onClick = { onSelectLanguage(lang) },
                        label = { Text("${lang.nativeName} (${if (lang.isRtl) "RTL" else "LTR"})") },
                        modifier = Modifier.testTag("settings_lang_${lang.code}")
                    )
                }
            }

            Text("Appearance", style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                listOf("SYSTEM" to "System", "LIGHT" to "Light", "DARK" to "Dark").forEach { (mode, label) ->
                    FilterChip(
                        selected = darkThemeOverride == mode,
                        onClick = { onSelectThemeMode(mode) },
                        label = { Text(label) },
                        modifier = Modifier.weight(1f).testTag("settings_theme_${mode.lowercase()}")
                    )
                }
            }

            Card {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Retain Translation Transcripts", style = MaterialTheme.typography.titleMedium)
                            Text(
                                "Audio recordings are never stored. Enable only if you wish to keep translated room text after sessions end.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = retainTranscripts,
                            onCheckedChange = onToggleRetainTranscripts,
                            modifier = Modifier.testTag("settings_retain_transcripts_switch")
                        )
                    }
                    HorizontalDivider()
                    val reducedMotion = LocalReducedMotion.current
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(Icons.Filled.AccessibilityNew, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                        Column {
                            Text("Accessibility & Motion", style = MaterialTheme.typography.titleSmall)
                            Text(
                                text = if (reducedMotion) "Reduced motion active (Android animation scale = 0). Ambient animations paused."
                                else "Standard subtle transitions active. Automatically adapts to OS Reduced Motion.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            Text("Account & Operations", style = MaterialTheme.typography.titleMedium)
            Card {
                Column {
                    SettingsRowItem("User Profile & Emergency Contact", Icons.Filled.Person, onClick = onOpenProfile)
                    HorizontalDivider()
                    SettingsRowItem("Local Connect Provider Application", Icons.Filled.VerifiedUser, onClick = onOpenProviderRegistration)
                    if (isAdmin) {
                        HorizontalDivider()
                        SettingsRowItem("Administrator Dashboard", Icons.Filled.AdminPanelSettings, onClick = onOpenAdminDashboard)
                    }
                }
            }

            Text("Legal, Privacy & Founder Attribution", style = MaterialTheme.typography.titleMedium)
            Card {
                Column {
                    SettingsRowItem(strings.privacyPolicyTitle, Icons.Filled.PrivacyTip, onClick = onOpenPrivacyPolicy)
                    HorizontalDivider()
                    SettingsRowItem(strings.termsTitle, Icons.Filled.Gavel, onClick = onOpenTerms)
                    HorizontalDivider()
                    SettingsRowItem(strings.aboutTitle, Icons.Filled.Info, onClick = onOpenAbout)
                    HorizontalDivider()
                    SettingsRowItem(strings.capabilitiesTitle, Icons.Filled.VerifiedUser, onClick = onOpenCapabilities)
                }
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = TurquoiseContainer),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("App Information", style = MaterialTheme.typography.labelLarge, color = TurquoiseOnContainer)
                    Text("NILORA Global Travel & Communication Platform", style = MaterialTheme.typography.titleMedium, color = TurquoiseOnContainer)
                    Text(
                        NiloraTaxonomy.FOUNDER_ATTRIBUTION,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = TurquoiseOnContainer,
                        modifier = Modifier.testTag("settings_founder_attribution")
                    )
                }
            }

            OutlinedButton(
                onClick = onSignOut,
                modifier = Modifier.fillMaxWidth().height(50.dp).testTag("settings_sign_out_btn")
            ) {
                Icon(Icons.Filled.Logout, contentDescription = null, tint = ErrorCoral)
                Spacer(Modifier.width(8.dp))
                Text("Sign Out", color = ErrorCoral)
            }

            NiloraFooter()
        }
    }
}

@Composable
private fun SettingsRowItem(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
            Text(title, style = MaterialTheme.typography.bodyLarge)
        }
        Icon(Icons.Filled.ArrowForwardIos, contentDescription = null, modifier = Modifier.size(14.dp))
    }
}

@Composable
fun PrivacyPolicyScreen(
    strings: AppStrings,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = { NiloraTopBar(title = strings.privacyPolicyTitle, subtitle = "GDPR-Aligned Operational Draft", onBackClick = onBack) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Card(colors = CardDefaults.cardColors(containerColor = WarningAmberContainer)) {
                Text(
                    text = "Notice: This Privacy Policy is a structured operational draft prepared for formal legal counsel review before commercial app store publication.",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(14.dp)
                )
            }

            PolicySection(
                "1. Platform Identity & Controller",
                "NILORA (${NiloraTaxonomy.FOUNDER_ATTRIBUTION}) processes only data strictly necessary to deliver Google Sign-In authentication, verified travel discovery, transparent booking requests, and real-time voice translation."
            )
            PolicySection(
                "2. Voice & Translation Privacy",
                "Microphone audio is captured strictly after explicit user action and Android runtime permission grant. Raw audio recordings are NEVER stored on NILORA servers. Two-phone conversation room transcripts are deleted automatically when sessions end unless participants explicitly opt into transcript retention."
            )
            PolicySection(
                "3. Location & Maps Data",
                "Device GPS location (ACCESS_COARSE_LOCATION / ACCESS_FINE_LOCATION) is used only when requested on the Interactive Map to calculate real distances or center viewports. GPS coordinates are never sold or shared for advertising."
            )
            PolicySection(
                "4. Media & Permissions Minimization",
                "NILORA uses Android's zero-permission Photo Picker for profile photos and provider credential attachments and never requests broad storage permissions."
            )
            PolicySection(
                "5. Your Rights (Access, Correction & Deletion)",
                "You can view, update, or permanently delete your account profile, private contact record, saved places, travel notes, and notifications directly from the User Profile screen at any time."
            )

            NiloraFooter()
        }
    }
}

@Composable
fun TermsAndConditionsScreen(
    strings: AppStrings,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = { NiloraTopBar(title = strings.termsTitle, subtitle = "Legal Review Draft", onBackClick = onBack) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Card(colors = CardDefaults.cardColors(containerColor = WarningAmberContainer)) {
                Text(
                    text = "Notice: These Terms & Conditions are a structured product draft requiring formal legal counsel review prior to commercial release.",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(14.dp)
                )
            }

            PolicySection(
                "1. Acceptance & Platform Scope",
                "NILORA (${NiloraTaxonomy.FOUNDER_ATTRIBUTION}) provides verified destination discovery, AI-assisted travel guidance, real-time multilingual translation, and booking request workflows connecting travelers and verified local providers."
            )
            PolicySection(
                "2. Booking Request vs. Confirmed Reservation",
                "Submitting a booking request in NILORA creates a REQUEST_SUBMITTED / AWAITING_PROVIDER_CONFIRMATION record only. A reservation is NEVER confirmed until the verified service provider explicitly accepts it."
            )
            PolicySection(
                "3. Payments & Financial Liability",
                "NILORA never stores raw payment card numbers. Where external payment gateways are not yet configured (paymentStatus = NOT_CONFIGURED), financial settlement occurs directly only after confirmed service terms."
            )
            PolicySection(
                "4. Translation & Safety Accuracy Disclaimer",
                "Real-time voice translation and AI travel guidance assist cross-cultural communication. For critical medical, legal, marine diving, or emergency instructions, always verify with qualified local personnel."
            )
            PolicySection(
                "5. Data Integrity & Community Reporting",
                "Users and providers may not submit fabricated venues, prices, or misleading credentials. Reported content undergoes administrator review."
            )

            NiloraFooter()
        }
    }
}

@Composable
fun AboutNiloraScreen(
    strings: AppStrings,
    onOpenPrivacyPolicy: () -> Unit,
    onOpenTerms: () -> Unit,
    onBack: () -> Unit,
    onOpenCapabilities: () -> Unit = {}
) {
    Scaffold(
        topBar = { NiloraTopBar(title = strings.aboutTitle, onBackClick = onBack) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Surface(
                color = MidnightNavy,
                shape = RoundedCornerShape(22.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    NiloraBrandWordmark(
                        variant = BrandLogoVariant.ON_DARK,
                        showTagline = true,
                        symbolSize = 48.dp
                    )
                    Text(
                        text = NiloraTaxonomy.BRAND_VISION,
                        style = MaterialTheme.typography.labelLarge,
                        color = WarmSand
                    )
                    NiloraGeometricDivider()
                    Text(
                        text = "Native Android App (Kotlin + Jetpack Compose + Material 3). Inspired by Nile landscapes, Red Sea turquoise, Mediterranean light, and Egyptian geometric craft.",
                        style = MaterialTheme.typography.bodySmall,
                        color = SoftIvory.copy(alpha = 0.86f)
                    )
                }
            }

            Surface(
                color = SoftIvory,
                shape = RoundedCornerShape(18.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, OutlineIvory),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Brand Identity Variations", style = MaterialTheme.typography.labelLarge, color = MidnightNavy)
                    NiloraBrandWordmark(variant = BrandLogoVariant.ON_LIGHT, showTagline = false, symbolSize = 32.dp)
                    Surface(color = MidnightNavyDark, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
                        Box(modifier = Modifier.padding(10.dp)) {
                            NiloraBrandWordmark(variant = BrandLogoVariant.MONOCHROME_GOLD, showTagline = false, symbolSize = 30.dp)
                        }
                    }
                }
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = TurquoiseContainer),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Founder & Creator Attribution", style = MaterialTheme.typography.labelLarge, color = TurquoiseOnContainer)
                    Text(
                        text = NiloraTaxonomy.FOUNDER_ATTRIBUTION,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = TurquoiseOnContainer,
                        modifier = Modifier.testTag("about_founder_attribution")
                    )
                    Text(
                        "NILORA is designed from day one as a global multi-country, multi-currency, multi-language travel and communication platform, launching with dedicated coverage across 12 Egyptian cities and coastal regions.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TurquoiseOnContainer
                    )
                }
            }

            PolicySection(
                "Zero-Fabrication Architecture",
                "Every destination, hotel, restaurant, beach, activity price, and provider profile shown in NILORA comes strictly from administrator-verified Firestore records or authorized external APIs. Empty categories display honest unavailable states instead of invented placeholders."
            )
            PolicySection(
                "Real-Time Voice Translation Engine",
                "Supports live bilingual conversations in Arabic (including Egyptian dialect), English, Russian, and French across two-phone Firestore rooms or split-screen 180° face-to-face mode."
            )

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(onClick = onOpenPrivacyPolicy, modifier = Modifier.weight(1f)) {
                    Text(strings.privacyPolicyTitle)
                }
                OutlinedButton(onClick = onOpenTerms, modifier = Modifier.weight(1f)) {
                    Text(strings.termsTitle)
                }
            }

            FilledTonalButton(
                onClick = onOpenCapabilities,
                modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 48.dp).testTag("about_capabilities_btn")
            ) {
                Icon(Icons.Filled.VerifiedUser, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(strings.capabilitiesTitle)
            }

            NiloraFooter()
        }
    }
}

@Composable
private fun PolicySection(title: String, body: String) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        Text(body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
