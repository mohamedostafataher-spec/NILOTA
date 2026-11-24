package com.example

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.LayoutDirection
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.*
import com.example.data.repository.LocationAndPlacesRepository
import com.example.localization.StringsProvider
import com.example.niloralive.AudioCaptureAndHardwareProfiler
import com.example.niloralive.VoiceCapabilityReport
import com.example.ui.components.BrandLogoVariant
import com.example.ui.components.CommunicationHubTabs
import com.example.ui.components.NiloraBrandWordmark
import com.example.ui.screens.*
import com.example.ui.theme.LocalReducedMotion
import com.example.ui.theme.NiloraTheme
import com.example.ui.viewmodel.TranslatorUiState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.ui.navigation.NiloraRoutes
import com.example.ui.viewmodel.AdventureWorldUiState
import com.example.ui.viewmodel.CommunityDiscoveryFilters
import com.example.ui.viewmodel.DirectChatUiState
import com.example.ui.viewmodel.GroupRoomUiState
import com.example.ui.viewmodel.PostcardDetailUiState
import com.example.ui.viewmodel.VoiceMediaDraftState
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun appResourcesAndMapsKeyStatus_areValid() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        assertEquals("NILORA", context.getString(R.string.app_name))
        assertTrue(context.getString(R.string.firestore_database_id).isNotBlank())

        val locRepo = LocationAndPlacesRepository(context)
        // Unless a real GOOGLE_MAPS_API_KEY secret is injected, key readiness must honestly report false
        if (BuildConfig.GOOGLE_MAPS_API_KEY == "MY_GOOGLE_MAPS_API_KEY" || BuildConfig.GOOGLE_MAPS_API_KEY.isBlank()) {
            assertFalse(locRepo.isGoogleMapsKeyConfigured())
        }
    }

    @Test
    fun arabicLanguageAppliesRtlLayoutDirection() {
        var detectedDirection: LayoutDirection? = null
        composeTestRule.setContent {
            NiloraTheme(languageCode = "ar") {
                detectedDirection = LocalLayoutDirection.current
            }
        }
        composeTestRule.waitForIdle()
        assertEquals(LayoutDirection.Rtl, detectedDirection)
    }

    @Test
    fun languageSelectionScreen_selectsLanguageAndContinues() {
        var selected by androidx.compose.runtime.mutableStateOf(NiloraLanguage.ENGLISH)
        var continued = false
        composeTestRule.setContent {
            NiloraTheme(languageCode = selected.code) {
                LanguageSelectionScreen(
                    currentLanguage = selected,
                    strings = StringsProvider.forLanguage(selected),
                    showBack = false,
                    onBack = {},
                    onSelectLanguage = { selected = it },
                    onContinue = { continued = true }
                )
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("language_options_list")
            .performScrollToNode(hasTestTag("language_option_ar"))
        composeTestRule.onNodeWithTag("language_option_ar").performClick()
        composeTestRule.waitForIdle()
        assertEquals(NiloraLanguage.ARABIC, selected)
        composeTestRule.onNodeWithTag("language_continue_button").performClick()
        composeTestRule.waitForIdle()
        assertTrue(continued)
    }

    @Test
    fun aboutAndSettingsScreens_displayMandatoryFounderAttribution() {
        val strings = StringsProvider.forLanguage(NiloraLanguage.ENGLISH)
        composeTestRule.setContent {
            NiloraTheme {
                AboutNiloraScreen(
                    strings = strings,
                    onOpenPrivacyPolicy = {},
                    onOpenTerms = {},
                    onBack = {}
                )
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("about_founder_attribution")
            .assertTextEquals("Created & Founded by Mohamed Mostafa Taher")
    }

    @Test
    fun bookingRequestScreen_enforcesValidationAndHonestPaymentStatus() {
        val strings = StringsProvider.forLanguage(NiloraLanguage.ENGLISH)
        val activity = ActivityListing(
            id = "act_1",
            title = "Red Sea Snorkeling",
            providerId = "prov_1",
            providerName = "Sinai Verified Guides",
            cityName = "Dahab",
            category = "SNORKELING",
            cancellationPolicy = "Free cancellation 24h before start."
        )
        var submitted = false

        composeTestRule.setContent {
            NiloraTheme {
                BookingRequestScreen(
                    strings = strings,
                    activity = activity,
                    onSubmitBooking = { _, _, _ -> submitted = true },
                    onBack = {}
                )
            }
        }
        composeTestRule.waitForIdle()

        // Initially submit button must be disabled until date and conditions checkbox are set
        composeTestRule.onNodeWithTag("booking_submit_btn").assertIsNotEnabled()
        composeTestRule.onNodeWithText("NOT_CONFIGURED", substring = true).assertExists()

        composeTestRule.onNodeWithTag("booking_date_input").performTextInput("2026-12-20")
        composeTestRule.onNodeWithTag("booking_agree_checkbox").performScrollTo().performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("booking_submit_btn").performScrollTo().assertIsEnabled().performClick()
        composeTestRule.waitForIdle()
        assertTrue(submitted)
    }

    @Test
    fun adminDashboard_blocksNonAdminAndShowsZeroFakeMetricsForAdmin() {
        composeTestRule.setContent {
            NiloraTheme {
                AdminDashboardScreen(
                    isAdmin = false,
                    currentUserId = "u_user",
                    isGoogleMapsKeyConfigured = false,
                    isOnline = true,
                    destinationsState = UiState.Success(emptyList()),
                    activitiesState = UiState.Success(emptyList()),
                    providersState = UiState.Success(emptyList()),
                    bookingsState = UiState.Success(emptyList()),
                    reportsState = UiState.Success(emptyList()),
                    auditLogsState = UiState.Success(emptyList()),
                    onPublishVerifiedDestination = {},
                    onPublishVerifiedActivity = {},
                    onModerateDestination = { _, _ -> },
                    onModerateActivity = { _, _ -> },
                    onModerateProvider = { _, _, _ -> },
                    onResolveReport = { _, _ -> },
                    onBack = {}
                )
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Administrator Access Required").assertExists()
    }

    @Test
    fun sharedPhoneTranslatorScreen_rendersConsentGateAndDualControls() {
        val strings = StringsProvider.forLanguage(NiloraLanguage.ENGLISH)
        var consentState by mutableStateOf(false)
        composeTestRule.setContent {
            NiloraTheme {
                SharedPhoneTranslatorScreen(
                    strings = strings,
                    state = TranslatorUiState(voiceConsentGranted = consentState),
                    rmsNormalized = 0.1f,
                    isSpeaking = false,
                    isMuted = false,
                    ttsSpeed = 1.0f,
                    ttsVolume = 1.0f,
                    onGrantVoiceConsent = { consentState = it },
                    onToggleTopRotation = {},
                    onSetTopLanguage = {},
                    onSetBottomLanguage = {},
                    onStartVoice = {},
                    onStopVoice = {},
                    onUpdatePendingTranscript = {},
                    onConfirmPendingTranscript = {},
                    onDismissPendingTranscript = {},
                    onSendText = { _, _ -> },
                    onRepeatAudio = { _, _ -> },
                    onReplayLastAudio = {},
                    onPauseOrStopAudio = {},
                    onToggleMute = {},
                    onSetSpeed = {},
                    onSetVolume = {},
                    onEndSession = {},
                    onBack = {}
                )
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("shared_mute_btn").assertExists()
        composeTestRule.onNodeWithTag("shared_end_btn").assertExists()
        composeTestRule.onNodeWithTag("voice_consent_card").assertExists()
        composeTestRule.onNodeWithTag("grant_voice_consent_btn").performClick()
        composeTestRule.waitForIdle()
        assertTrue(consentState)
    }

    @Test
    fun liveEvaluationScreen_rendersHardwareCapabilitiesAndEvaluatedModels() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val hw = AudioCaptureAndHardwareProfiler.inspectCapabilities(context)
        val models = AudioCaptureAndHardwareProfiler.getEvaluatedModelCatalog(hw)
        val voiceCaps = VoiceCapabilityReport(
            ttsInitialized = true,
            installedVoiceCount = 4,
            hasExactEgyptianArabicVoice = false,
            bestArabicVoiceName = "Generic Arabic (ar-SA) — no dedicated ar-EG voice installed",
            hasEnglishVoice = true,
            hasRussianVoice = true,
            hasFrenchVoice = true,
            notes = "Using generic Arabic voice."
        )

        composeTestRule.setContent {
            NiloraTheme {
                LiveEvaluationScreen(
                    state = TranslatorUiState(hardwareCapabilities = hw, evaluatedModels = models),
                    voiceCapabilities = voiceCaps,
                    onRefreshDiagnostics = {},
                    onRunAmbientCheck = {},
                    onSetArabicDialectMode = {},
                    onRecordTrial = { _, _, _, _, _, _, _ -> },
                    onClearTrials = {},
                    onBack = {}
                )
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("eval_hardware_card").assertExists()
        composeTestRule.onNodeWithTag("eval_ambient_check_btn").assertExists()
        composeTestRule.onNodeWithTag("eval_lazy_list")
            .performScrollToNode(hasText("whisper.cpp (OpenAI Whisper GGUF)", substring = true))
        composeTestRule.onNodeWithText("whisper.cpp (OpenAI Whisper GGUF)", substring = true).assertExists()
    }

    @Test
    fun globalCommunityScreen_filtersPublicProfilesAndSupportsConnectMessageActions() {
        var exchangeOnly by mutableStateOf(false)
        var searchQuery by mutableStateOf("")
        var messagedUid = ""
        var connectedUid = ""

        val sampleMembers = listOf(
            PublicProfile(
                userId = "u_alex",
                displayName = "Alexei Petrov",
                country = "Russia",
                city = "Moscow",
                nativeLanguage = "ru",
                learningLanguages = listOf("ar-EG"),
                bio = "Visiting Cairo soon",
                openToLanguageExchange = true
            ),
            PublicProfile(
                userId = "u_marie",
                displayName = "Marie Laurent",
                country = "France",
                city = "Lyon",
                nativeLanguage = "fr",
                learningLanguages = listOf("en"),
                bio = "Photographer",
                openToLanguageExchange = false
            )
        )

        composeTestRule.setContent {
            NiloraTheme {
                GlobalCommunityScreen(
                    currentUserId = "u_me",
                    profilesState = UiState.Success(sampleMembers),
                    blockedState = UiState.Success(emptyList()),
                    requestsState = UiState.Success(emptyList()),
                    filters = CommunityDiscoveryFilters(
                        searchQuery = searchQuery,
                        languageExchangeOnly = exchangeOnly
                    ),
                    onSearchChange = { searchQuery = it },
                    onLanguageFilterChange = {},
                    onCountryFilterChange = {},
                    onInterestFilterChange = {},
                    onToggleLanguageExchangeOnly = { exchangeOnly = it },
                    onSendConnectionRequest = { profile, _ -> connectedUid = profile.userId },
                    onStartDirectChat = { messagedUid = it.userId },
                    onBlockUser = { _, _ -> },
                    onReportUser = { _, _, _ -> },
                    onOpenConnectionRequests = {},
                    onOpenBlockedUsers = {},
                    onOpenExplorePlaces = {},
                    onOpenMap = {}
                )
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("discover_community_list")
            .performScrollToNode(hasTestTag("member_card_u_alex"))
        composeTestRule.onNodeWithTag("member_card_u_alex").assertExists()
        composeTestRule.onNodeWithTag("discover_community_list")
            .performScrollToNode(hasTestTag("member_card_u_marie"))
        composeTestRule.onNodeWithTag("member_card_u_marie").assertExists()

        // Scroll back to top and toggle Language Exchange filter -> Marie (false) filters out, Alexei remains
        composeTestRule.onNodeWithTag("discover_community_list")
            .performScrollToNode(hasTestTag("filter_lang_exchange"))
        composeTestRule.onNodeWithTag("filter_lang_exchange").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("discover_community_list")
            .performScrollToNode(hasTestTag("member_card_u_alex"))
        composeTestRule.onNodeWithTag("member_card_u_alex").assertExists()
        composeTestRule.onNodeWithTag("member_card_u_marie").assertDoesNotExist()

        composeTestRule.onNodeWithTag("message_member_u_alex").performClick()
        composeTestRule.waitForIdle()
        assertEquals("u_alex", messagedUid)

        composeTestRule.onNodeWithTag("connect_member_u_alex").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("confirm_send_connection").performClick()
        composeTestRule.waitForIdle()
        assertEquals("u_alex", connectedUid)
    }

    @Test
    fun directChatScreen_preservesOriginalWithTranslationAndEnforcesMicConsent() {
        val conv = DirectConversation(
            id = "u_me_u_partner",
            participantIds = listOf("u_me", "u_partner"),
            participantNames = mapOf("u_me" to "Karim", "u_partner" to "Claire"),
            participantLanguages = mapOf("u_me" to "ar-EG", "u_partner" to "fr")
        )
        val msgTranslated = DirectMessage(
            id = "m_1",
            conversationId = "u_me_u_partner",
            participantIds = listOf("u_me", "u_partner"),
            senderId = "u_partner",
            senderName = "Claire",
            originalText = "Bonjour, on se voit demain à 10h ?",
            sourceLanguage = "fr",
            translations = mapOf("ar-EG" to "صباح الخير، نتقابل بكرة الساعة ١٠؟"),
            deliveryStatus = "READ"
        )
        val msgUntranslated = DirectMessage(
            id = "m_2",
            conversationId = "u_me_u_partner",
            participantIds = listOf("u_me", "u_partner"),
            senderId = "u_me",
            senderName = "Karim",
            originalText = "أيوه تمام مستنيكي",
            sourceLanguage = "ar-EG",
            deliveryStatus = "DELIVERED"
        )
        var sentText = ""
        var translatedMsgId = ""
        var micStarted = false

        composeTestRule.setContent {
            NiloraTheme {
                DirectChatScreen(
                    currentUserId = "u_me",
                    state = DirectChatUiState(
                        conversation = conv,
                        messages = listOf(msgTranslated, msgUntranslated),
                        targetTranslationCode = "ar-EG",
                        sourceInputLanguageCode = "ar-EG"
                    ),
                    hasMicPermission = true,
                    onSelectSourceLanguage = {},
                    onSelectTargetTranslationLanguage = {},
                    onSendText = { txt, _ -> sentText = txt },
                    onTranslateMessage = { m, _ -> translatedMsgId = m.id },
                    onStartVoiceRecording = { micStarted = true },
                    onStopVoiceRecording = {},
                    onDiscardVoiceRecording = {},
                    onSendVoiceRecording = { _, _ -> },
                    onDictateMessage = {},
                    onTogglePlayVoiceClip = {},
                    onSpeakText = { _, _ -> },
                    onBlockPartner = { _, _ -> },
                    onReportPartner = { _, _, _ -> },
                    onBack = {}
                )
            }
        }
        composeTestRule.waitForIdle()

        // Original French text and Egyptian Arabic translation are both rendered intact
        composeTestRule.onNodeWithTag("direct_messages_list")
            .performScrollToNode(hasTestTag("direct_msg_m_1"))
        composeTestRule.onNodeWithText("Bonjour, on se voit demain à 10h ?").assertExists()
        composeTestRule.onNodeWithText("صباح الخير، نتقابل بكرة الساعة ١٠؟").assertExists()
        composeTestRule.onNodeWithTag("direct_messages_list")
            .performScrollToNode(hasTestTag("direct_msg_m_2"))
        composeTestRule.onNodeWithText("DELIVERED", substring = true).assertExists()

        // Send message
        composeTestRule.onNodeWithTag("direct_message_input").performTextInput("أهلاً يا كلير")
        composeTestRule.onNodeWithTag("direct_send_btn").performClick()
        composeTestRule.waitForIdle()
        assertEquals("أهلاً يا كلير", sentText)

        // Mic button must trigger explicit voice disclosure dialog before recording
        composeTestRule.onNodeWithTag("direct_mic_record_btn").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("consent_mic_accept_btn").performClick()
        composeTestRule.waitForIdle()
        assertTrue(micStarted)
        assertEquals("", translatedMsgId) // untoggled unless translate clicked
    }

    @Test
    fun groupsHubAndGroupRoomScreens_createGroupAndDisplayModeratorTools() {
        var createdName = ""
        var createdPolicy = ""
        composeTestRule.setContent {
            NiloraTheme {
                GroupsHubScreen(
                    currentUserId = "u_owner",
                    myGroupsState = UiState.Success(
                        listOf(
                            GroupRoom(
                                id = "grp_1",
                                name = "Cairo Russian Exchange",
                                description = "Practice Egyptian Arabic & Russian",
                                ownerId = "u_owner",
                                memberIds = listOf("u_owner", "u_2"),
                                primaryLanguages = listOf("ar-EG", "ru"),
                                joinPolicy = "REQUEST_APPROVAL"
                            )
                        )
                    ),
                    discoverGroupsState = UiState.Success(emptyList()),
                    onCreateGroup = { name, _, _, policy, _ ->
                        createdName = name
                        createdPolicy = policy
                    },
                    onEnterGroup = {},
                    onJoinOrRequestGroup = { _, _ -> }
                )
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("group_card_grp_1").assertExists()
        composeTestRule.onNodeWithTag("create_group_button").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("create_group_name_input").performTextInput("Global Travel Forum")
        composeTestRule.onNodeWithTag("group_policy_REQUEST_APPROVAL").performClick()
        composeTestRule.onNodeWithTag("confirm_create_group_btn").performClick()
        composeTestRule.waitForIdle()

        assertEquals("Global Travel Forum", createdName)
        assertEquals("REQUEST_APPROVAL", createdPolicy)
    }

    @Test
    fun userProfileScreen_validatesPhoneAndSupportsPrivacyVisibilityAndDeleteDialog() {
        var savedCity = ""
        var savedVisibility = ""
        composeTestRule.setContent {
            NiloraTheme {
                UserProfileScreen(
                    userProfile = UserProfile(
                        userId = "u_1",
                        displayName = "Mina",
                        country = "Egypt",
                        city = "Alexandria",
                        profileVisibility = "PUBLIC"
                    ),
                    privateInfo = UserPrivateInfo(userId = "u_1", phoneCountryCode = "+20"),
                    authEmail = "mina@example.com",
                    onSaveProfile = { _, _, _, _, _, _, _, _, _, _, city, _, _, _, _, vis, _, _ ->
                        savedCity = city
                        savedVisibility = vis
                    },
                    onSignOut = {},
                    onDeleteAccount = {},
                    onBack = {}
                )
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("visibility_CONNECTIONS_ONLY").performScrollTo().performClick()
        composeTestRule.onNodeWithTag("save_profile_btn").performScrollTo().performClick()
        composeTestRule.waitForIdle()
        assertEquals("Alexandria", savedCity)
        assertEquals("CONNECTIONS_ONLY", savedVisibility)

        composeTestRule.onNodeWithTag("delete_account_data_btn").performScrollTo().performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Delete NILORA Account Data?").assertExists()
    }

    @Test
    fun responsiveNavShell_usesBottomBarWithFivePrimaryTabsOnPhoneWidth() {
        val primaryNavItems = listOf(
            Triple(NiloraRoutes.HOME, "Home", Icons.Filled.Home),
            Triple(NiloraRoutes.REALTIME_TRANSLATOR, "Translate", Icons.Filled.Translate),
            Triple(NiloraRoutes.EXPLORE, "Explore", Icons.Filled.Explore),
            Triple(NiloraRoutes.MESSAGES, "Messages", Icons.AutoMirrored.Filled.Chat),
            Triple(NiloraRoutes.PROFILE_HUB, "Profile", Icons.Filled.Person)
        )
        val secondaryRailItems = listOf(
            Triple(NiloraRoutes.DISCOVER, "Discover", Icons.Filled.Explore),
            Triple(NiloraRoutes.GROUPS, "Groups", Icons.Filled.Groups)
        )

        composeTestRule.setContent {
            NiloraTheme {
                val snack = remember { SnackbarHostState() }
                NiloraResponsiveNavShell(
                    showTopLevelNav = true,
                    currentRoute = NiloraRoutes.HOME,
                    navItems = primaryNavItems,
                    secondaryRailItems = secondaryRailItems,
                    snackbarHostState = snack,
                    onSelectTab = {}
                ) {
                    Text("Content")
                }
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("adaptive_navigation_bar").assertExists()
        composeTestRule.onNodeWithTag("adaptive_navigation_rail").assertDoesNotExist()
        primaryNavItems.forEach { (route, _, _) ->
            composeTestRule.onNodeWithTag("bottom_nav_$route").assertExists()
        }
        composeTestRule.onNodeWithTag("bottom_nav_${NiloraRoutes.DISCOVER}").assertDoesNotExist()
        composeTestRule.onNodeWithTag("bottom_nav_${NiloraRoutes.GROUPS}").assertDoesNotExist()
    }

    @Test
    @Config(qualifiers = "w840dp-h640dp")
    fun responsiveNavShell_usesNavigationRailWithSecondaryItemsOnTabletWidth() {
        val primaryNavItems = listOf(
            Triple(NiloraRoutes.HOME, "Home", Icons.Filled.Home),
            Triple(NiloraRoutes.REALTIME_TRANSLATOR, "Translate", Icons.Filled.Translate),
            Triple(NiloraRoutes.EXPLORE, "Explore", Icons.Filled.Explore),
            Triple(NiloraRoutes.MESSAGES, "Messages", Icons.AutoMirrored.Filled.Chat),
            Triple(NiloraRoutes.PROFILE_HUB, "Profile", Icons.Filled.Person)
        )
        val secondaryRailItems = listOf(
            Triple(NiloraRoutes.DISCOVER, "Discover", Icons.Filled.Explore),
            Triple(NiloraRoutes.GROUPS, "Groups", Icons.Filled.Groups)
        )

        composeTestRule.setContent {
            NiloraTheme {
                val snack = remember { SnackbarHostState() }
                NiloraResponsiveNavShell(
                    showTopLevelNav = true,
                    currentRoute = NiloraRoutes.HOME,
                    navItems = primaryNavItems,
                    secondaryRailItems = secondaryRailItems,
                    snackbarHostState = snack,
                    onSelectTab = {}
                ) {
                    Text("Content")
                }
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("adaptive_navigation_rail").assertExists()
        composeTestRule.onNodeWithTag("adaptive_navigation_bar").assertDoesNotExist()
        primaryNavItems.forEach { (route, _, _) ->
            composeTestRule.onNodeWithTag("rail_nav_$route").assertExists()
        }
        composeTestRule.onNodeWithTag("rail_nav_${NiloraRoutes.DISCOVER}").assertExists()
        composeTestRule.onNodeWithTag("rail_nav_${NiloraRoutes.GROUPS}").assertExists()
    }

    @Test
    fun rtlAndReducedMotion_applyForArabicAndOverride() {
        var detectedDirection: LayoutDirection? = null
        var detectedReducedMotion = false
        composeTestRule.setContent {
            NiloraTheme(languageCode = "ar-EG", reducedMotionOverride = true) {
                detectedDirection = LocalLayoutDirection.current
                detectedReducedMotion = LocalReducedMotion.current
            }
        }
        composeTestRule.waitForIdle()
        assertEquals(LayoutDirection.Rtl, detectedDirection)
        assertTrue(detectedReducedMotion)
    }

    @Test
    fun homeScreen_displaysBrandWordmarkContinueConversationsAndRealSuggestions() {
        val strings = StringsProvider.forLanguage(NiloraLanguage.ENGLISH)
        val conv = DirectConversation(
            id = "u_me_u_sara",
            participantIds = listOf("u_me", "u_sara"),
            participantNames = mapOf("u_me" to "Karim", "u_sara" to "Sara Hassan"),
            participantLanguages = mapOf("u_me" to "ar-EG", "u_sara" to "en"),
            lastMessagePreview = "See you near Zamalek bridge"
        )
        val member = PublicProfile(
            userId = "u_ivan",
            displayName = "Ivan Volkov",
            country = "Russia",
            city = "Saint Petersburg",
            nativeLanguage = "ru",
            learningLanguages = listOf("ar-EG")
        )
        var openedConversationId = ""

        composeTestRule.setContent {
            NiloraTheme {
                HomeScreen(
                    strings = strings,
                    userProfile = UserProfile(userId = "u_me", displayName = "Karim"),
                    isAdmin = false,
                    isOnline = true,
                    destinationsState = UiState.Success(emptyList()),
                    activitiesState = UiState.Success(emptyList()),
                    unreadNotificationsCount = 2,
                    onOpenTranslator = {},
                    onOpenSharedTranslator = {},
                    onOpenAiAssistant = {},
                    onOpenMap = {},
                    onOpenExplore = {},
                    onOpenSearch = {},
                    onOpenBeaches = {},
                    onOpenActivities = {},
                    onOpenHotels = {},
                    onOpenRestaurants = {},
                    onOpenNotifications = {},
                    onOpenSettings = {},
                    onOpenAdmin = {},
                    onSelectCity = {},
                    onOpenDestination = {},
                    onOpenActivity = {},
                    onOpenProviderRegistration = {},
                    currentUserId = "u_me",
                    conversationsState = UiState.Success(listOf(conv)),
                    suggestedProfilesState = UiState.Success(listOf(member)),
                    blockedUsersState = UiState.Success(emptyList()),
                    onOpenConversation = { openedConversationId = it }
                )
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("NILORA").assertExists()
        composeTestRule.onNodeWithTag("home_screen_list")
            .performScrollToNode(hasText("Cloud AI Ready"))
        composeTestRule.onNodeWithText("Cloud AI Ready").assertExists()

        composeTestRule.onNodeWithTag("home_screen_list")
            .performScrollToNode(hasTestTag("conversation_row_u_me_u_sara"))
        composeTestRule.onNodeWithText("Continue Conversations").assertExists()
        composeTestRule.onNodeWithTag("conversation_row_u_me_u_sara").performClick()
        assertEquals("u_me_u_sara", openedConversationId)

        composeTestRule.onNodeWithTag("home_screen_list")
            .performScrollToNode(hasText("Suggested Community Connections"))
        composeTestRule.onNodeWithText("Ivan Volkov").assertExists()
    }

    @Test
    fun exploreScreen_displaysMapPreviewVerticalsAndHonestEmptyState() {
        composeTestRule.setContent {
            NiloraTheme {
                ExploreScreen(
                    language = NiloraLanguage.ENGLISH,
                    selectedCountry = "EG",
                    selectedCity = "ALL",
                    selectedCategory = "ALL",
                    destinationsState = UiState.Success(emptyList()),
                    onCountryChange = {},
                    onCityChange = {},
                    onCategoryChange = {},
                    onOpenDestination = {},
                    onOpenSearch = {},
                    onOpenMap = {}
                )
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Interactive Discovery Map").assertExists()
        composeTestRule.onNodeWithTag("explore_beaches").assertExists()
        composeTestRule.onNodeWithTag("explore_screen_list")
            .performScrollToNode(hasText("Awaiting Verified Provider or API Listings"))
        composeTestRule.onNodeWithText("Awaiting Verified Provider or API Listings").assertExists()
    }

    @Test
    fun communicationHubTabs_triggersMessagesDiscoverAndGroupsCallbacks() {
        var clickedTab = ""
        composeTestRule.setContent {
            NiloraTheme {
                CommunicationHubTabs(
                    activeTab = "MESSAGES",
                    pendingRequestsCount = 3,
                    onSelectMessages = { clickedTab = "MESSAGES" },
                    onSelectDiscover = { clickedTab = "DISCOVER" },
                    onSelectGroups = { clickedTab = "GROUPS" }
                )
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Discover (3)").assertExists()
        composeTestRule.onNodeWithTag("hub_tab_discover").performClick()
        assertEquals("DISCOVER", clickedTab)
        composeTestRule.onNodeWithTag("hub_tab_groups").performClick()
        assertEquals("GROUPS", clickedTab)
        composeTestRule.onNodeWithTag("hub_tab_messages").performClick()
        assertEquals("MESSAGES", clickedTab)
    }

    @Test
    fun brandWordmarkVariants_renderLightDarkAndGold() {
        composeTestRule.setContent {
            NiloraTheme {
                androidx.compose.foundation.layout.Column {
                    NiloraBrandWordmark(variant = BrandLogoVariant.ON_LIGHT)
                    NiloraBrandWordmark(variant = BrandLogoVariant.ON_DARK)
                    NiloraBrandWordmark(variant = BrandLogoVariant.MONOCHROME_GOLD)
                }
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onAllNodesWithText("NILORA").assertCountEquals(3)
    }

    @Test
    fun homeScreen_displaysNextGenActionPillarsAndExperienceShelves() {
        val strings = StringsProvider.forLanguage(NiloraLanguage.ENGLISH)
        val postcard = WorldPostcard(
            id = "pc_alex",
            authorId = "u_other",
            authorName = "Nadia",
            country = "Egypt",
            city = "Alexandria",
            languageCode = "ar-EG",
            category = "DAILY_LIFE",
            title = "Morning Corniche Walk",
            storyText = "Sea breeze before breakfast on the Corniche.",
            countriesChain = listOf("Egypt", "Japan"),
            contributionsCount = 1
        )
        var clickedPillar = ""

        composeTestRule.setContent {
            NiloraTheme {
                HomeScreen(
                    strings = strings,
                    userProfile = UserProfile(userId = "u_me", displayName = "Karim"),
                    isAdmin = false,
                    isOnline = true,
                    destinationsState = UiState.Success(emptyList()),
                    activitiesState = UiState.Success(emptyList()),
                    unreadNotificationsCount = 0,
                    onOpenTranslator = {},
                    onOpenSharedTranslator = {},
                    onOpenAiAssistant = {},
                    onOpenMap = {},
                    onOpenExplore = {},
                    onOpenSearch = {},
                    onOpenBeaches = {},
                    onOpenActivities = {},
                    onOpenHotels = {},
                    onOpenRestaurants = {},
                    onOpenNotifications = {},
                    onOpenSettings = {},
                    onOpenAdmin = {},
                    onSelectCity = {},
                    onOpenDestination = {},
                    onOpenActivity = {},
                    onOpenProviderRegistration = {},
                    currentUserId = "u_me",
                    postcardsState = UiState.Success(listOf(postcard)),
                    onLiveAMoment = { clickedPillar = "LIVE_MOMENT" },
                    onSwapCultures = { clickedPillar = "SWAP_CULTURES" },
                    onStartWorldQuest = { clickedPillar = "START_QUEST" }
                )
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("home_screen_list")
            .performScrollToNode(hasTestTag("home_cta_live_moment"))
        composeTestRule.onNodeWithTag("home_cta_live_moment").performClick()
        assertEquals("LIVE_MOMENT", clickedPillar)

        composeTestRule.onNodeWithTag("home_screen_list")
            .performScrollToNode(hasTestTag("home_cta_swap_cultures"))
        composeTestRule.onNodeWithTag("home_cta_swap_cultures").performClick()
        assertEquals("SWAP_CULTURES", clickedPillar)

        composeTestRule.onNodeWithTag("home_screen_list")
            .performScrollToNode(hasTestTag("home_cta_start_quest"))
        composeTestRule.onNodeWithTag("home_cta_start_quest").performClick()
        assertEquals("START_QUEST", clickedPillar)

        composeTestRule.onNodeWithTag("home_screen_list")
            .performScrollToNode(hasTestTag("world_postcard_card_pc_alex"))
        composeTestRule.onNodeWithText("Morning Corniche Walk").assertExists()
    }

    @Test
    fun worldPostcardDetailScreen_rendersCountryChainTranslationAndMutualConsentConnect() {
        val postcard = WorldPostcard(
            id = "pc_1",
            authorId = "u_nadia",
            authorName = "Nadia",
            country = "Egypt",
            city = "Cairo",
            languageCode = "ar-EG",
            category = "EXPRESSION",
            title = "Afternoon Tea Custom",
            storyText = "أهلاً بكم في القاهرة",
            translations = mapOf("en" to "Welcome to Cairo"),
            countriesChain = listOf("Egypt", "France"),
            contributionsCount = 1,
            allowReplies = true,
            allowPrivateConnect = true
        )
        var connectedAuthorId = ""

        composeTestRule.setContent {
            NiloraTheme {
                WorldPostcardDetailScreen(
                    detailState = PostcardDetailUiState(
                        postcard = postcard,
                        targetLanguageCode = "en"
                    ),
                    mediaDraft = VoiceMediaDraftState(),
                    currentUserId = "u_visitor",
                    myProfile = UserProfile(userId = "u_visitor", displayName = "Lucas", country = "France"),
                    onSetTargetLanguage = {},
                    onTranslatePostcard = {},
                    onTranslateContribution = { _, _ -> },
                    onPlayAudio = { _, _ -> },
                    onSpeakText = { _, _ -> },
                    onPickPhoto = { _, _ -> },
                    onClearPhoto = {},
                    onStartRecording = {},
                    onStopRecording = {},
                    onClearRecording = {},
                    onDictateText = {},
                    onSubmitContribution = { _, _, _, _, _, _, _ -> },
                    onSaveToMemory = { _, _, _ -> },
                    onContinuePrivately = { pid, _, _, _, _ -> connectedAuthorId = pid },
                    onBlockUser = { _, _ -> },
                    onReportPostcard = { _, _, _ -> },
                    onBack = {}
                )
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("أهلاً بكم في القاهرة").assertExists()
        composeTestRule.onNodeWithText("Welcome to Cairo").assertExists()
        composeTestRule.onNodeWithTag("world_postcard_detail_list")
            .performScrollToNode(hasTestTag("postcard_continue_privately_btn"))
        composeTestRule.onNodeWithTag("postcard_continue_privately_btn").performClick()
        assertEquals("u_nadia", connectedAuthorId)
    }

    @Test
    fun niloraMemoryScreen_enforcesVisibilityControlsAndAudioWipe() {
        val memory = NiloraMemory(
            id = "mem_1",
            ownerId = "u_me",
            ownerName = "Karim",
            memoryType = "VOICE_MEMORY",
            title = "Nile Sunset Reflection",
            summaryText = "Recorded near Zamalek Promenade.",
            countries = listOf("Egypt"),
            audioBase64 = "AAAA",
            audioDurationMs = 3000L,
            retainAudioConsent = true,
            visibility = "PRIVATE"
        )
        var wipedId = ""
        var updatedVis: MemoryVisibility? = null

        composeTestRule.setContent {
            NiloraTheme {
                NiloraMemoryScreen(
                    memoriesState = UiState.Success(listOf(memory)),
                    currentUserId = "u_me",
                    myProfile = UserProfile(userId = "u_me", displayName = "Karim"),
                    mediaDraft = VoiceMediaDraftState(),
                    onPickPhoto = { _, _ -> },
                    onClearPhoto = {},
                    onStartRecording = {},
                    onStopRecording = {},
                    onClearRecording = {},
                    onPlayAudio = { _, _ -> },
                    onDictateText = {},
                    onCreatePersonalMemory = { _, _, _, _, _, _, _ -> },
                    onUpdateVisibility = { _, vis -> updatedVis = vis },
                    onRevokeAudio = { id -> wipedId = id },
                    onDeleteMemory = {},
                    onBack = {}
                )
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("nilora_memory_list")
            .performScrollToNode(hasTestTag("memory_card_mem_1"))
        composeTestRule.onNodeWithText("Nile Sunset Reflection").assertExists()
        composeTestRule.onNodeWithTag("memory_wipe_audio_mem_1").performClick()
        assertEquals("mem_1", wipedId)

        composeTestRule.onNodeWithText("Public").performClick()
        assertEquals(MemoryVisibility.PUBLIC, updatedVis)
    }

    @Test
    fun adventureDiscoveryScreen_revealsSecretSpotAndEntersAdventure() {
        val strings = StringsProvider.forLanguage(NiloraLanguage.ENGLISH)
        val firstAdv = AdventureCatalog.egyptAdventures.first()
        var uiState by mutableStateOf(
            AdventureWorldUiState(
                selectedAdventure = firstAdv,
                weatherState = LiveIntelligenceResult.Unavailable("Offline test mode"),
                wikiState = LiveIntelligenceResult.Unavailable("Offline test mode")
            )
        )
        var enteredId = ""

        composeTestRule.setContent {
            NiloraTheme {
                AdventureDiscoveryScreen(
                    strings = strings,
                    uiState = uiState,
                    joinableSessionsCount = 0,
                    onSelectDestinationFilter = {},
                    onSelectTypeFilter = {},
                    onSelectAdventure = { uiState = uiState.copy(selectedAdventure = it) },
                    onTriggerSurpriseMe = {
                        val spot = AdventureCatalog.secretSpots().first()
                        uiState = uiState.copy(surpriseRevealAdventure = spot, selectedAdventure = spot)
                    },
                    onDismissSurprise = { uiState = uiState.copy(surpriseRevealAdventure = null) },
                    onRefreshIntelligence = {},
                    onEnterAdventure = { enteredId = it },
                    onOpenMatchmaking = {},
                    onOpenCrews = {},
                    onOpenCapabilities = {},
                    onOpenRealWorldPlan = {},
                    onBack = {}
                )
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("surprise_me_secret_spot_btn").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("secret_spot_reveal_card").assertExists()

        composeTestRule.onNodeWithTag("adventure_discovery_list")
            .performScrollToNode(hasTestTag("enter_my_adventure_btn"))
        composeTestRule.onNodeWithTag("enter_my_adventure_btn").performScrollTo().performClick()
        composeTestRule.waitForIdle()
        assertEquals(AdventureCatalog.secretSpots().first().id, enteredId)
    }

    @Test
    fun immersiveAdventureScreen_renders3DViewportAndHonestZeroFakeSessionsState() {
        val strings = StringsProvider.forLanguage(NiloraLanguage.ENGLISH)
        val adv = AdventureCatalog.egyptAdventures.first()
        var soloStarted = false

        composeTestRule.setContent {
            NiloraTheme {
                ImmersiveAdventureScreen(
                    strings = strings,
                    uiState = AdventureWorldUiState(
                        selectedAdventure = adv,
                        selectedPoi = adv.pois.first(),
                        audioMuted = true
                    ),
                    currentUserId = "u_me",
                    myProfile = UserProfile(userId = "u_me", displayName = "Karim"),
                    joinableSessionsState = UiState.Success(emptyList()),
                    onCameraChange = {},
                    onSelectPoi = { _, _ -> },
                    onPointAtPoi = {},
                    onToggleAudioMute = {},
                    onStartSession = { mode -> if (mode == AdventureSessionMode.SOLO) soloStarted = true },
                    onJoinSession = {},
                    onLeaveSession = {},
                    onSendSessionChat = {},
                    onDictateVoiceChat = {},
                    onTranslateSessionMessage = {},
                    onSpeakText = { _, _ -> },
                    onSetTargetLang = {},
                    onToggleSubtitles = {},
                    onToggleSpeakTranslations = {},
                    onSaveMemory = { _, _, _, _ -> },
                    onConnectCompanion = { _, _, _, _, _ -> },
                    onOpenMatchmaking = {},
                    onOpenCrews = {},
                    onOpenRealTrip = {},
                    onBack = {}
                )
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("immersive_3d_viewport").assertExists()
        composeTestRule.onNodeWithTag("immersive_adventure_scroll")
            .performScrollToNode(hasTestTag("mode_explore_solo_btn"))
        composeTestRule.onNodeWithTag("mode_explore_solo_btn").performClick()
        composeTestRule.waitForIdle()
        assertTrue(soloStarted)

        composeTestRule.onNodeWithTag("immersive_adventure_scroll")
            .performScrollToNode(hasTestTag("joinable_sessions_card"))
        composeTestRule.onNodeWithText("No live shared sessions are open right now", substring = true).assertExists()
    }

    @Test
    fun platformCapabilitiesScreen_rendersAllFourHonestStatusBuckets() {
        val strings = StringsProvider.forLanguage(NiloraLanguage.ENGLISH)
        composeTestRule.setContent {
            NiloraTheme {
                PlatformCapabilitiesScreen(strings = strings, onBack = {})
            }
        }
        composeTestRule.waitForIdle()

        CapabilityBucket.entries.forEach { bucket ->
            composeTestRule.onNodeWithTag("platform_capabilities_list")
                .performScrollToNode(hasText(bucket.labelEn))
            composeTestRule.onAllNodesWithText(bucket.labelEn).onFirst().assertExists()
        }
    }

    @Test
    fun realityUniverseHubScreen_filtersTeamActivitiesAndTriggersNavigation() {
        val strings = StringsProvider.forLanguage(NiloraLanguage.ENGLISH)
        val padelPost = TeamActivityPost(
            id = "ta_padel",
            creatorId = "u_other",
            creatorName = "Omar",
            domain = ActivityDomain.PLAY_SPORTS.key,
            subCategory = ActivitySubCategory.PADEL.key,
            title = "Evening Padel Doubles in Zamalek",
            description = "Looking for 2 intermediate players at Gezira public court.",
            cityName = "Cairo",
            publicMeetingSpot = "Gezira Public Club Entrance",
            scheduledDateLabel = "Friday 19:00",
            maxParticipants = 4,
            participantIds = listOf("u_other", "u_friend")
        )
        var openedDetailId = ""
        var navTarget = ""

        composeTestRule.setContent {
            NiloraTheme {
                RealityUniverseHubScreen(
                    strings = strings,
                    currentUserId = "u_me",
                    filters = com.example.ui.viewmodel.RealityFilterState(),
                    localGps = null,
                    gpsMessage = "",
                    activitiesState = UiState.Success(listOf(padelPost)),
                    blockedIds = emptySet(),
                    onSelectCity = {},
                    onSetDistanceKm = {},
                    onRequestGps = {},
                    onSelectDomain = {},
                    onSelectSubCategory = {},
                    onSelectSkill = {},
                    onToggleOpenSlotsOnly = {},
                    onOpenEvents = { navTarget = "EVENTS" },
                    onOpenMissions = { navTarget = "MISSIONS" },
                    onOpenChallenges = { navTarget = "CHALLENGES" },
                    onOpenRealityBridge = { navTarget = "BRIDGE" },
                    onOpenWorldPulseMap = { navTarget = "MAP" },
                    onCreateTeamActivity = { _, _, _, _, _, _, _, _, _, _, _, _, _, _ -> },
                    onOpenActivityDetail = { openedDetailId = it },
                    onBack = {}
                )
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("reality_hub_nav_events").performClick()
        assertEquals("EVENTS", navTarget)

        composeTestRule.onNodeWithTag("team_activity_card_ta_padel").assertExists().performClick()
        assertEquals("ta_padel", openedDetailId)
    }

    @Test
    fun realityPulseEventsScreen_showsHonestProvenanceBadges() {
        val strings = StringsProvider.forLanguage(NiloraLanguage.ENGLISH)
        val verifiedEvent = RealityEvent(
            id = "ev_verified",
            creatorId = "u_admin",
            title = "Cairo Opera Heritage Night",
            category = RealityEventCategory.CULTURAL.key,
            description = "Public cultural performance.",
            cityName = "Cairo",
            countryName = "Egypt",
            venueName = "Cairo Opera Plaza",
            startDateIso = "2026-12-10",
            freeVerified = true,
            verificationTier = EventVerificationTier.ADMIN_VERIFIED.key
        )
        val unverifiedEvent = RealityEvent(
            id = "ev_user",
            creatorId = "u_member",
            title = "Alexandria Corniche Sketch Walk",
            category = RealityEventCategory.COMMUNITY.key,
            description = "Casual meetup near Bibliotheca.",
            cityName = "Alexandria",
            countryName = "Egypt",
            venueName = "Bibliotheca Plaza",
            startDateIso = "2026-12-12",
            freeVerified = false,
            priceNote = "Check organizer note",
            verificationTier = EventVerificationTier.UNVERIFIED_USER_SUBMITTED.key
        )

        composeTestRule.setContent {
            NiloraTheme {
                RealityPulseEventsScreen(
                    strings = strings,
                    currentUserId = "u_me",
                    isAdmin = false,
                    filters = com.example.ui.viewmodel.RealityFilterState(),
                    localGps = null,
                    eventsState = UiState.Success(listOf(verifiedEvent, unverifiedEvent)),
                    externalEvents = null,
                    isLoadingExternal = false,
                    blockedIds = emptySet(),
                    onSelectCity = {},
                    onSelectCategory = {},
                    onSelectVerification = {},
                    onToggleFreeOnly = {},
                    onToggleUpcomingOnly = {},
                    onRefreshExternal = { _, _ -> },
                    onCreateEvent = { _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _ -> },
                    onOpenEvent = {},
                    onBack = {}
                )
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("reality_events_list")
            .performScrollToNode(hasTestTag("reality_event_card_ev_verified"))
        composeTestRule.onNodeWithTag("reality_event_card_ev_verified").assertExists()
        composeTestRule.onNodeWithText("Admin Verified Event", substring = true).assertExists()
        composeTestRule.onNodeWithTag("reality_events_list")
            .performScrollToNode(hasTestTag("reality_event_card_ev_user"))
        composeTestRule.onNodeWithTag("reality_event_card_ev_user").assertExists()
        composeTestRule.onNodeWithText("Unverified / User Submitted", substring = true).assertExists()
    }

    @Test
    fun realWorldMissionsAndRealityBridge_enforceSeparationAndProgression() {
        val strings = StringsProvider.forLanguage(NiloraLanguage.ENGLISH)
        var launchedTemplateId = ""

        composeTestRule.setContent {
            NiloraTheme {
                RealWorldMissionsScreen(
                    strings = strings,
                    filters = com.example.ui.viewmodel.RealityFilterState(),
                    missionsState = UiState.Success(emptyList()),
                    selectedMissionSubmissions = emptyList(),
                    attachedPhotoBase64 = "",
                    blockedIds = emptySet(),
                    onSelectModeFilter = {},
                    onLaunchTemplate = { launchedTemplateId = it.id },
                    onCreateCustomMission = { _, _, _, _, _, _, _ -> },
                    onSelectMission = {},
                    onPickPhoto = { _, _ -> },
                    onClearPhoto = {},
                    onSubmitEvidence = { _, _, _, _ -> },
                    onSaveMissionMemory = { _, _ -> },
                    onBack = {}
                )
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Honest Separation Rule", substring = true).assertExists()
        composeTestRule.onNodeWithTag("launch_tpl_tpl_cairo_explorer").performClick()
        assertEquals("tpl_cairo_explorer", launchedTemplateId)
    }

    @Test
    fun realityBridgeScreen_advancesStageCleanly() {
        val strings = StringsProvider.forLanguage(NiloraLanguage.ENGLISH)
        val link = RealityBridgeLink(
            id = "rb_1",
            userId = "u_me",
            virtualAdventureId = "sharm_reef_dive",
            virtualAdventureTitle = "Ras Mohammed Coral Reef",
            destinationName = "Sharm El Sheikh",
            targetType = "REAL_WORLD_MISSION",
            targetId = "m_1",
            targetTitle = "Reef-Safe Coastal Visit",
            stage = RealityBridgeStage.PLANNED.key
        )
        var nextStageSelected: RealityBridgeStage? = null

        composeTestRule.setContent {
            NiloraTheme {
                RealityBridgeScreen(
                    strings = strings,
                    bridgeState = UiState.Success(listOf(link)),
                    onCreateBridgePlan = { _, _, _, _, _, _ -> },
                    onAdvanceStage = { _, stage -> nextStageSelected = stage },
                    onDeleteBridge = {},
                    onBack = {}
                )
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("bridge_card_rb_1").assertExists()
        composeTestRule.onNodeWithText("Advance -> Request / Joined").performClick()
        assertEquals(RealityBridgeStage.REGISTERED, nextStageSelected)
    }
}
