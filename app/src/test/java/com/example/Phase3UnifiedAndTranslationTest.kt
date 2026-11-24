package com.example

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Celebration
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.example.data.model.*
import com.example.localization.StringsProvider
import com.example.niloralive.*
import com.example.ui.components.LiveSubtitleItem
import com.example.ui.components.LiveSubtitlesOverlayCard
import com.example.ui.navigation.NiloraRoutes
import com.example.ui.screens.*
import com.example.ui.theme.NiloraTheme
import com.example.ui.viewmodel.GroupRoomUiState
import com.example.ui.viewmodel.TranslatorUiState
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class Phase3UnifiedAndTranslationTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun personalizationEngine_ranksByCityAndInterestsWhenEnabledAndPreservesOrderWhenDisabled() {
        val cairoDest = Destination(id = "d_cairo", cityName = "Cairo", title = "Egyptian Museum", category = "Heritage", description = "Pharaonic museum", verifiedCoordinates = true)
        val dahabDest = Destination(id = "d_dahab", cityName = "Dahab", title = "Blue Hole Diving", category = "Diving", description = "Red Sea diving reef", verifiedCoordinates = true)
        val profile = UserProfile(userId = "u1", city = "Dahab", interests = listOf("Diving"))

        val enabledRanked = PersonalizationEngine.rankDestinations(
            destinations = listOf(cairoDest, dahabDest),
            profile = profile,
            savedIds = emptySet(),
            enabled = true
        )
        assertEquals("d_dahab", enabledRanked.first().item.id)
        assertTrue(enabledRanked.first().score > enabledRanked.last().score)
        assertTrue(enabledRanked.first().matchReason.contains("Near Dahab"))

        val pausedRanked = PersonalizationEngine.rankDestinations(
            destinations = listOf(cairoDest, dahabDest),
            profile = profile,
            savedIds = emptySet(),
            enabled = false
        )
        assertEquals(listOf("d_cairo", "d_dahab"), pausedRanked.map { it.item.id })
        assertEquals("Personalization paused", pausedRanked.first().matchReason)
    }

    @Test
    fun egyptianArabicBenchmarkSuite_computesDeterministicWerAndValidatesNumbersAndCodeSwitching() {
        assertTrue(EgyptianArabicBenchmarkSuite.cases.size >= 5)
        val codeSwitchCase = EgyptianArabicBenchmarkSuite.cases.first { it.isCodeSwitched }
        val wer = SpeechEvaluationHarness.computeWordErrorRate(
            reference = codeSwitchCase.referenceTranscript,
            hypothesis = codeSwitchCase.sampleRecognizedHypothesis
        )
        assertNotNull(wer)
        assertTrue(wer!! > 0.0 && wer < 0.35)

        val request = TranslationRequest(
            sourceText = "بكام التذكرة الساعة 10",
            sourceLanguage = NiloraLanguage.ARABIC,
            targetLanguage = NiloraLanguage.ENGLISH,
            domain = TranslationContextDomain.TRAVEL,
            autoDetectSource = true
        )
        val okWarnings = TranslationValidator.validate(request, "How much is the ticket at 10?", "HIGH")
        assertTrue(okWarnings.isEmpty())

        val arabicNumerals = TranslationValidator.validate(
            request = TranslationRequest("Meet at 8", NiloraLanguage.ENGLISH, NiloraLanguage.ARABIC),
            rawTranslatedText = "هنتقابل الساعة ٨",
            confidenceLabel = "HIGH"
        )
        assertTrue(arabicNumerals.isEmpty())

        val missingNumWarnings = TranslationValidator.validate(request, "How much is the ticket?", "LOW")
        assertEquals(2, missingNumWarnings.size)
    }

    @Test
    fun subtitleDisplayMode_andLiveSubtitlesOverlay_renderModesAndLowConfidenceWarning() {
        assertEquals(SubtitleDisplayMode.BOTH, SubtitleDisplayMode.fromKey("unknown"))
        assertEquals(SubtitleDisplayMode.TRANSLATED_ONLY, SubtitleDisplayMode.fromKey("TRANSLATED_ONLY"))

        val subtitle = LiveSubtitleItem(
            speakerName = "Nadia",
            sourceLanguageLabel = "ar-EG",
            targetLanguageLabel = "en",
            originalText = "عايزين نحجز مركب في الأقصر",
            translatedText = "We want to book a boat in Luxor",
            isPartial = false,
            confidenceLabel = "LOW",
            ambiguityWarning = "Boat size or felucca nuance"
        )

        composeTestRule.setContent {
            NiloraTheme {
                var mode by remember { mutableStateOf(SubtitleDisplayMode.BOTH) }
                LiveSubtitlesOverlayCard(
                    subtitle = subtitle,
                    mode = mode,
                    onModeChange = { mode = it },
                    onSpeakTranslation = {}
                )
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("live_subtitles_overlay_card").assertExists()
        composeTestRule.onNodeWithTag("subtitle_status_pill").assertTextEquals("Final")
        composeTestRule.onNodeWithTag("subtitle_original_text").assertExists()
        composeTestRule.onNodeWithTag("subtitle_translated_text").assertExists()
        composeTestRule.onNodeWithTag("subtitle_low_confidence_warning").assertExists()

        composeTestRule.onNodeWithTag("subtitle_mode_TRANSLATED_ONLY").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("subtitle_original_text").assertDoesNotExist()
        composeTestRule.onNodeWithTag("subtitle_translated_text").assertExists()

        composeTestRule.onNodeWithTag("subtitle_mode_ORIGINAL_ONLY").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("subtitle_original_text").assertExists()
        composeTestRule.onNodeWithTag("subtitle_translated_text").assertDoesNotExist()

        composeTestRule.onNodeWithTag("subtitle_mode_NONE").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("subtitle_hidden_hint").assertExists()
    }

    @Test
    fun groupRoomScreen_exposesMultiSpeakerSubtitlesMicAndAutoSpeak() {
        val group = GroupRoom(
            id = "grp_1",
            name = "Cairo & Moscow Travellers",
            description = "Live multilingual chat",
            ownerId = "u_me",
            memberIds = listOf("u_me", "u_anna"),
            primaryLanguages = listOf("ar-EG", "ru", "en"),
            autoTranslateDefault = true
        )
        val msg = GroupMessage(
            id = "m1",
            groupId = "grp_1",
            senderId = "u_anna",
            senderName = "Anna",
            sourceLanguage = "ru",
            originalText = "Встретимся у входа в 6 вечера",
            translations = mapOf("ar-EG" to "هنتقابل عند البوابة الساعة 6 المغرب")
        )
        var dictated = false
        var sentText = ""

        composeTestRule.setContent {
            NiloraTheme {
                GroupRoomScreen(
                    currentUserId = "u_me",
                    state = GroupRoomUiState(
                        group = group,
                        messages = listOf(msg),
                        sourceInputLanguageCode = "ar-EG",
                        targetTranslationCode = "ar-EG",
                        subtitleMode = SubtitleDisplayMode.BOTH,
                        autoSpeakIncoming = true
                    ),
                    publicProfilesState = UiState.Success(emptyList()),
                    onSelectSourceLanguage = {},
                    onSelectTargetLanguage = {},
                    onSendMessage = { text, _ -> sentText = text },
                    onTranslateMessage = { _, _ -> },
                    onSpeakText = { _, _ -> },
                    onReviewJoinRequest = { _, _ -> },
                    onInviteMember = {},
                    onPromoteModerator = {},
                    onRemoveOrBanMember = { _, _ -> },
                    onDeleteMessage = {},
                    onLeaveGroup = {},
                    onReportGroup = { _, _, _ -> },
                    onBack = {},
                    onDictateVoice = { dictated = true }
                )
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("live_subtitles_overlay_card").assertExists()
        composeTestRule.onNodeWithTag("group_auto_speak_chip").assertExists()
        composeTestRule.onNodeWithTag("group_mic_btn").assertExists()

        composeTestRule.onNodeWithTag("group_msg_input").performTextInput("تمام جاي")
        composeTestRule.onNodeWithTag("group_send_btn").performClick()
        composeTestRule.waitForIdle()
        assertEquals("تمام جاي", sentText)
        assertFalse(dictated)
    }

    @Test
    fun unifiedSearchScreen_filtersRealDataAndExcludesBlockedOrPrivateProfiles() {
        val strings = StringsProvider.forLanguage(NiloraLanguage.ENGLISH)
        val publicPerson = PublicProfile(userId = "u_pub", displayName = "Youssef", city = "Cairo", country = "Egypt", nativeLanguage = "ar-EG", profileVisibility = "PUBLIC")
        val privatePerson = PublicProfile(userId = "u_priv", displayName = "Hidden Cairo", city = "Cairo", country = "Egypt", nativeLanguage = "en", profileVisibility = "PRIVATE")
        val blockedPerson = PublicProfile(userId = "u_blk", displayName = "Blocked Cairo", city = "Cairo", country = "Egypt", nativeLanguage = "en", profileVisibility = "PUBLIC")

        composeTestRule.setContent {
            NiloraTheme {
                var query by remember { mutableStateOf("Cairo") }
                UnifiedSearchScreen(
                    strings = strings,
                    query = query,
                    currentUserId = "u_me",
                    onQueryChange = { query = it },
                    destinationsState = UiState.Success(listOf(Destination(id = "d1", title = "Cairo Citadel", cityName = "Cairo", category = "Heritage"))),
                    eventsState = UiState.Success(listOf(RealityEvent(id = "e1", title = "Cairo Jazz Night", cityName = "Cairo", venueName = "Zamalek"))),
                    teamActivitiesState = UiState.Success(listOf(TeamActivityPost(id = "t1", title = "Cairo Padel Game", cityName = "Cairo", publicMeetingSpot = "Gezira Club"))),
                    discoverableGroupsState = UiState.Success(listOf(GroupRoom(id = "g1", name = "Cairo Locals"))),
                    myGroupsState = UiState.Success(emptyList()),
                    openCrewsState = UiState.Success(listOf(NiloraCrew(id = "c1", name = "Cairo Explorers", focusDestination = "Cairo"))),
                    myCrewsState = UiState.Success(emptyList()),
                    publicProfilesState = UiState.Success(listOf(publicPerson, privatePerson, blockedPerson)),
                    blockedUsersState = UiState.Success(listOf(BlockedUserRecord(blockedUserId = "u_blk", blockedUserName = "Blocked Cairo"))),
                    providersState = UiState.Success(emptyList()),
                    placesOutcome = null,
                    isSearchingPlaces = false,
                    isGoogleMapsKeyConfigured = false,
                    onSearchGooglePlaces = {},
                    onSelectCity = {},
                    onOpenDestination = {},
                    onOpenAdventure = {},
                    onOpenEvent = {},
                    onOpenTeamActivity = {},
                    onOpenGroup = {},
                    onOpenCrew = {},
                    onOpenPerson = {},
                    onOpenProvider = {},
                    onBack = {}
                )
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("unified_search_list").performScrollToNode(hasTestTag("unified_person_u_pub"))
        composeTestRule.onNodeWithTag("unified_person_u_pub").assertExists()
        composeTestRule.onNodeWithTag("unified_person_u_priv").assertDoesNotExist()
        composeTestRule.onNodeWithTag("unified_person_u_blk").assertDoesNotExist()
    }

    @Test
    fun phase3FivePillarNavAndUnifiedHubs_renderExploreTogetherEventsLiveMyWorld() {
        val strings = StringsProvider.forLanguage(NiloraLanguage.ENGLISH)
        val fiveHubs = listOf(
            Triple(NiloraRoutes.HOME, strings.navExplore, Icons.Filled.Explore),
            Triple(NiloraRoutes.TOGETHER_HUB, strings.navTogether, Icons.Filled.Groups),
            Triple(NiloraRoutes.EVENTS_HUB, strings.navEvents, Icons.Filled.Celebration),
            Triple(NiloraRoutes.REALTIME_TRANSLATOR, strings.navLive, Icons.Filled.Translate),
            Triple(NiloraRoutes.PROFILE_HUB, strings.navMyWorld, Icons.Filled.Person)
        )
        composeTestRule.setContent {
            NiloraTheme {
                val snack = remember { SnackbarHostState() }
                NiloraResponsiveNavShell(
                    showTopLevelNav = true,
                    currentRoute = NiloraRoutes.TOGETHER_HUB,
                    navItems = fiveHubs,
                    snackbarHostState = snack,
                    onSelectTab = {}
                ) {
                    Text("Hub")
                }
            }
        }
        composeTestRule.waitForIdle()
        fiveHubs.forEach { (route, _, _) ->
            composeTestRule.onNodeWithTag("bottom_nav_$route").assertExists()
        }
    }

    @Test
    fun liveHub_displaysThreeModesAndLanguageIdentityBanner() {
        val strings = StringsProvider.forLanguage(NiloraLanguage.ENGLISH)
        val caps = VoiceCapabilityReport(true, 3, true, "ar-EG", true, true, true, "OK")
        var openedGroupLive = false

        composeTestRule.setContent {
            NiloraTheme {
                RealTimeTranslatorScreen(
                    strings = strings,
                    state = TranslatorUiState(),
                    voiceCapabilities = caps,
                    myRoomsState = UiState.Success(emptyList()),
                    isOnline = true,
                    onGrantVoiceConsent = {},
                    onSetArabicDialectMode = {},
                    onSetAutoTranslateAfterSpeech = {},
                    onOpenCreateRoom = {},
                    onOpenJoinRoom = {},
                    onOpenSharedMode = {},
                    onOpenLiveEvaluation = {},
                    onResumeRoom = {},
                    onOpenGroupsLive = { openedGroupLive = true }
                )
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("one_language_identity_banner").assertExists()
        composeTestRule.onNodeWithTag("live_hub_list").performScrollToNode(hasTestTag("live_mode_group_btn"))
        composeTestRule.onNodeWithTag("live_mode_group_btn").performClick()
        assertTrue(openedGroupLive)
    }

    @Test
    fun streamlinedOnboarding_allowsImmediateSkipAndPersonalizationToggle() {
        val strings = StringsProvider.forLanguage(NiloraLanguage.ENGLISH)
        var finished = false
        var pers = true
        composeTestRule.setContent {
            NiloraTheme {
                OnboardingScreen(
                    strings = strings,
                    personalizationEnabled = pers,
                    onTogglePersonalization = { pers = it },
                    onFinish = { finished = true }
                )
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("onboarding_next_button").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("onboarding_personalization_switch").assertExists()
        composeTestRule.onNodeWithTag("onboarding_skip_button").performClick()
        assertTrue(finished)
    }
}
