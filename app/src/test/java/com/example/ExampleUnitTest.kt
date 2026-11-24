package com.example

import com.example.adventure3d.*
import com.example.data.model.*
import com.example.localization.StringsProvider
import com.example.niloralive.ArabicDialectMode
import com.example.niloralive.SpeechEvaluationHarness
import com.example.niloralive.TranslationRequest
import com.example.niloralive.TranslationValidator
import com.example.ui.navigation.NiloraRoutes
import org.junit.Assert.*
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun languages_supportExpectedCodesAndRtlDirection() {
        assertEquals(4, NiloraLanguage.entries.size)
        assertTrue(NiloraLanguage.ARABIC.isRtl)
        assertFalse(NiloraLanguage.ENGLISH.isRtl)
        assertFalse(NiloraLanguage.FRENCH.isRtl)
        assertFalse(NiloraLanguage.RUSSIAN.isRtl)
        assertEquals(NiloraLanguage.ARABIC, NiloraLanguage.fromCode("ar"))
        assertEquals(NiloraLanguage.ENGLISH, NiloraLanguage.fromCode("unknown"))
    }

    @Test
    fun founderAttribution_isPreservedVerbatimAcrossAllLanguages() {
        val expected = "Created & Founded by Mohamed Mostafa Taher"
        assertEquals(expected, NiloraTaxonomy.FOUNDER_ATTRIBUTION)
        NiloraLanguage.entries.forEach { lang ->
            val strings = StringsProvider.forLanguage(lang)
            assertEquals(expected, strings.founderLine)
        }
    }

    @Test
    fun egyptTaxonomyAndTourismCategories_areComplete() {
        assertEquals(12, NiloraTaxonomy.egyptCities.size)
        assertTrue(NiloraTaxonomy.egyptCities.any { it.nameEn == "Cairo" })
        assertTrue(NiloraTaxonomy.egyptCities.any { it.nameEn == "Sharm El Sheikh" })
        assertTrue(NiloraTaxonomy.egyptCities.any { it.nameEn == "Siwa" })
        assertTrue(NiloraTaxonomy.egyptCities.any { it.nameEn == "North Coast" })

        assertEquals(17, TourismCategory.entries.size)
        TourismCategory.entries.forEach { cat ->
            NiloraLanguage.entries.forEach { lang ->
                assertTrue(cat.label(lang).isNotBlank())
            }
        }
    }

    @Test
    fun navigationRoutes_formatArgumentsCorrectly() {
        assertEquals("destination_details/dest_1", NiloraRoutes.destinationDetails("dest_1"))
        assertEquals("activity_details/act_2", NiloraRoutes.activityDetails("act_2"))
        assertEquals("booking_request/act_2", NiloraRoutes.bookingRequest("act_2"))
        assertEquals("booking_details/bk_9", NiloraRoutes.bookingDetails("bk_9"))
        assertEquals("provider_profile/prov_3", NiloraRoutes.providerProfile("prov_3"))
    }

    @Test
    fun userProfile_toCreateMapSanitizesBlankNameAndPreservesPrivacyFlags() {
        val profile = UserProfile(
            userId = "u_123",
            displayName = "",
            accountMode = "TRAVELER",
            preferredLanguage = "ar",
            country = "",
            privacyConsent = true,
            retainTranslationHistory = false
        )
        val map = profile.toCreateMap()
        assertEquals("u_123", map["userId"])
        assertEquals("NILORA Member", map["displayName"])
        assertEquals("Egypt", map["country"])
        assertEquals("ar", map["preferredLanguage"])
        assertEquals("PUBLIC", map["profileVisibility"])
        assertEquals(true, map["openToLanguageExchange"])
        assertEquals(false, map["retainTranslationHistory"])
    }

    @Test
    fun translationValidator_detectsMissingNumbersEchoAndLowConfidence() {
        val req = TranslationRequest(
            sourceText = "The boat leaves at 10 and costs 500 pounds",
            sourceLanguage = NiloraLanguage.ENGLISH,
            targetLanguage = NiloraLanguage.ARABIC,
            arabicDialectMode = ArabicDialectMode.EGYPTIAN_COLLOQUIAL
        )
        // Missing 500 should trigger numeric warning
        val warningsMissingNum = TranslationValidator.validate(req, "المركب هيتحرك الساعة 10", "HIGH")
        assertTrue(warningsMissingNum.any { it.contains("500") })

        // Arabic-Indic digits should satisfy numeric preservation
        val warningsArabicIndic = TranslationValidator.validate(req, "المركب هيتحرك الساعة ١٠ والتكلفة ٥٠٠ جنيه", "HIGH")
        assertTrue(warningsArabicIndic.isEmpty())

        // Echoed input across different languages should trigger verification warning
        val warningsEcho = TranslationValidator.validate(req, req.sourceText, "LOW")
        assertTrue(warningsEcho.any { it.contains("matches source text") })
        assertTrue(warningsEcho.any { it.contains("Low confidence") })
    }

    @Test
    fun speechEvaluationHarness_computesExactDeterministicWer() {
        // Blank reference never fabricates a WER score
        assertNull(SpeechEvaluationHarness.computeWordErrorRate("   ", "hello world"))
        // Exact match (case & punctuation insensitive)
        assertEquals(0.0, SpeechEvaluationHarness.computeWordErrorRate("Where is the beach?", "where is the beach")!!, 0.0001)
        // 1 substitution out of 4 reference words => 0.25 WER
        assertEquals(0.25, SpeechEvaluationHarness.computeWordErrorRate("Where is the beach", "Where is a beach")!!, 0.0001)
    }

    @Test
    fun internationalPhoneFormatterAndValidator_handleE164AndRejectInvalidNumbers() {
        assertEquals("+201012345678", NiloraTaxonomy.formatInternationalPhone("+20", "01012345678"))
        assertEquals("+79161234567", NiloraTaxonomy.formatInternationalPhone("+7", "916-123-45-67"))
        assertEquals("+33612345678", NiloraTaxonomy.formatInternationalPhone("+33", "06 12 34 56 78"))
        assertEquals("+14155550199", NiloraTaxonomy.formatInternationalPhone("+1", "(415) 555-0199"))

        assertTrue(NiloraTaxonomy.isValidInternationalPhone("+20", "01012345678"))
        assertTrue(NiloraTaxonomy.isValidInternationalPhone("+7", "9161234567"))
        // Optional phone number is valid when blank
        assertTrue(NiloraTaxonomy.isValidInternationalPhone("+20", ""))
        // Too few digits or invalid formatting must be rejected
        assertFalse(NiloraTaxonomy.isValidInternationalPhone("+20", "123"))
        assertFalse(NiloraTaxonomy.isValidInternationalPhone("+20", "123456789012345678"))
    }

    @Test
    fun worldLanguagesCatalog_isExtensibleAndIdentifiesRtlCorrectly() {
        assertTrue(NiloraTaxonomy.worldLanguages.size >= 12)
        val coreCodes = NiloraTaxonomy.worldLanguages.filter { it.isCoreLiveSupported }.map { it.code }.toSet()
        assertTrue(coreCodes.containsAll(setOf("ar-EG", "en", "ru", "fr", "ar-MSA")))

        assertTrue(NiloraTaxonomy.isRtlLanguageCode("ar-EG"))
        assertTrue(NiloraTaxonomy.isRtlLanguageCode("ar-MSA"))
        assertTrue(NiloraTaxonomy.isRtlLanguageCode("ar"))
        assertFalse(NiloraTaxonomy.isRtlLanguageCode("en"))
        assertFalse(NiloraTaxonomy.isRtlLanguageCode("ru"))
        assertFalse(NiloraTaxonomy.isRtlLanguageCode("fr"))
        assertFalse(NiloraTaxonomy.isRtlLanguageCode("ja"))

        assertTrue(NiloraTaxonomy.languageDisplayLabel("ar-EG").contains("Egyptian Arabic"))
    }

    @Test
    fun directConversationHelpers_andPhase2Routes_resolveCorrectly() {
        val conv = DirectConversation(
            id = "u_a_u_b",
            participantIds = listOf("u_a", "u_b"),
            participantNames = mapOf("u_a" to "Nour", "u_b" to "Elena"),
            participantLanguages = mapOf("u_a" to "ar-EG", "u_b" to "ru")
        )
        assertEquals("u_b", conv.partnerId("u_a"))
        assertEquals("Elena", conv.partnerName("u_a"))
        assertEquals("ru", conv.partnerLanguage("u_a"))

        assertEquals("u_a", conv.partnerId("u_b"))
        assertEquals("Nour", conv.partnerName("u_b"))
        assertEquals("ar-EG", conv.partnerLanguage("u_b"))

        assertEquals("direct_chat/u_a_u_b", NiloraRoutes.directChat("u_a_u_b"))
        assertEquals("group_room/grp_42", NiloraRoutes.groupRoom("grp_42"))
    }

    @Test
    fun stringsProvider_providesNonBlankPhase2NavigationLabelsForAllLanguages() {
        NiloraLanguage.entries.forEach { lang ->
            val s = StringsProvider.forLanguage(lang)
            assertTrue(s.navHome.isNotBlank())
            assertTrue(s.navTranslator.isNotBlank())
            assertTrue(s.navMessages.isNotBlank())
            assertTrue(s.navDiscover.isNotBlank())
            assertTrue(s.navGroups.isNotBlank())
            assertTrue(s.navProfile.isNotBlank())
            assertTrue(s.communityDiscoveryTitle.isNotBlank())
            assertTrue(s.globalMessagesTitle.isNotBlank())
            assertTrue(s.groupsTitle.isNotBlank())
            assertTrue(s.liveMomentCta.isNotBlank())
            assertTrue(s.swapCulturesCta.isNotBlank())
            assertTrue(s.startQuestCta.isNotBlank())
            assertTrue(s.niloraMemoryTitle.isNotBlank())
        }
    }

    @Test
    fun worldQuest_progressAndGoalGate_neverFabricatesCompletion() {
        val activeQuest = WorldQuest(
            id = "q_1",
            title = "3 Everyday Expressions",
            targetGoal = 3L,
            contributionsCount = 2L,
            status = "ACTIVE"
        )
        assertFalse(activeQuest.isGoalMet())
        assertEquals(2f / 3f, activeQuest.progressFraction(), 0.001f)

        val completedQuest = activeQuest.copy(contributionsCount = 3L, status = "COMPLETED")
        assertTrue(completedQuest.isGoalMet())
        assertEquals(1.0f, completedQuest.progressFraction(), 0.001f)

        val zeroGoalQuest = WorldQuest(targetGoal = 0L, contributionsCount = 1L)
        assertEquals(0f, zeroGoalQuest.progressFraction(), 0.001f)
    }

    @Test
    fun nextGenRoutes_promptBlueprints_andSafeMeetupPolicy_areValid() {
        assertEquals("world_postcard/pc_7", NiloraRoutes.postcardDetail("pc_7"))
        assertEquals("culture_swap/sw_9", NiloraRoutes.cultureSwapDetail("sw_9"))
        assertEquals("world_quest/q_3", NiloraRoutes.worldQuestDetail("q_3"))

        assertEquals(6, PostcardCategory.entries.size)
        assertEquals(6, SwapPromptCategory.entries.size)
        assertEquals(6, QuestMissionType.entries.size)
        assertEquals(3, MemoryVisibility.entries.size)

        SwapPromptCategory.entries.forEach { cat ->
            assertTrue(cat.titleEn.isNotBlank())
            assertTrue(cat.defaultPromptEn.isNotBlank())
        }
        assertTrue(SafeMeetupPolicy.guidelines.any { it.contains("public", ignoreCase = true) })
        assertTrue(SafeMeetupPolicy.guidelines.any { it.contains("home address", ignoreCase = true) })
    }

    @Test
    fun adventureCatalog_andSurpriseSecretSpotSelection_respectConditions() {
        assertTrue(AdventureCatalog.egyptAdventures.size >= 8)
        val secretSpots = AdventureCatalog.secretSpots()
        assertTrue(secretSpots.isNotEmpty())
        AdventureCatalog.egyptAdventures.forEach { adv ->
            assertTrue(adv.latitude in 20.0..33.0)
            assertTrue(adv.longitude in 24.0..37.0)
            assertTrue(adv.pois.isNotEmpty())
            assertTrue(adv.geoAttribution.contains("Artistic 3D spatial reconstruction"))
        }

        // High wind speed avoids coastal marine secret spots when non-coastal spots exist
        val highWindSpot = AdventureCatalog.pickSurpriseSecretSpot(windSpeedKmh = 45.0)
        assertFalse(highWindSpot.isCoastalMarine)
    }

    @Test
    fun projectionMath3D_projectsFrontPointsRejectsBehindAndBuildsScenePerBiome() {
        val cam = Camera3D(x = 0f, y = -4f, z = 4f, yawRad = 0f, pitchRad = 0f)
        val front = ProjectionMath3D.projectPoint(Vec3(0f, -4f, -12f), cam, 1080f, 1920f)
        assertNotNull(front)
        assertEquals(540f, front!!.screenX, 1f)
        assertTrue(front.depthZ > 0f)

        val behind = ProjectionMath3D.projectPoint(Vec3(0f, -4f, 10f), cam, 1080f, 1920f)
        assertNull(behind)

        val moved = cam.move(forwardDelta = 2f, strafeDelta = 1f, verticalDelta = 0.5f)
        assertTrue(moved.z < cam.z)
        assertTrue(moved.x > cam.x)

        AdventureBiome.entries.forEach { biome ->
            val adv = AdventureCatalog.egyptAdventures.firstOrNull { it.biome == biome }
                ?: AdventureCatalog.egyptAdventures.first().copy(biome = biome)
            val scene = ProceduralWorldGenerator.buildScene(adv)
            assertTrue(scene.formations.isNotEmpty())
            assertTrue(scene.creatures.isNotEmpty())
        }
    }

    @Test
    fun adventureMatchmaker_ranksCompatibilityAndCapabilitiesCoverAllFourBuckets() {
        val seeker = AdventureMatchProfile(
            userId = "u_seeker",
            targetAdventureId = "sharm_reef_dive",
            targetDestination = "Sharm El Sheikh",
            interests = listOf("Coral Reef", "Marine Biology"),
            preferredLanguage = "ar-EG",
            spokenLanguages = listOf("ar-EG", "en"),
            experienceLevel = AdventureLevel.BEGINNER.key,
            preferredDurationMinutes = 30,
            availableTimeSlot = "Evenings",
            voicePreferred = true
        )
        val highMatch = seeker.copy(userId = "u_high")
        val lowMatch = AdventureMatchProfile(
            userId = "u_low",
            targetAdventureId = "luxor_karnak_sunset",
            targetDestination = "Luxor",
            interests = listOf("Archaeology"),
            preferredLanguage = "ru",
            spokenLanguages = listOf("ru"),
            experienceLevel = AdventureLevel.EXPLORER.key,
            preferredDurationMinutes = 90,
            availableTimeSlot = "Mornings",
            voicePreferred = false
        )
        val highRes = AdventureMatchmaker.computeCompatibility(seeker, highMatch)
        val lowRes = AdventureMatchmaker.computeCompatibility(seeker, lowMatch)
        assertTrue(highRes.scorePercent > lowRes.scorePercent)
        assertTrue(highRes.scorePercent >= 90)
        assertTrue(lowRes.matchReasons.any { it.contains("NILORA LIVE Bridgeable") })

        val buckets = PlatformCapabilityRegistry.items.map { it.bucket }.toSet()
        assertEquals(CapabilityBucket.entries.toSet(), buckets)
    }

    @Test
    fun adventureRoutes_andStringsProvider_coverAllLanguages() {
        assertEquals("immersive_adventure/sharm_reef_dive", NiloraRoutes.immersiveAdventure("sharm_reef_dive"))
        assertEquals("nilora_crew/crew_7", NiloraRoutes.crewDetail("crew_7"))
        NiloraLanguage.entries.forEach { lang ->
            val s = StringsProvider.forLanguage(lang)
            assertTrue(s.worldGatewayHeadline.isNotBlank())
            assertTrue(s.enterMyAdventureCta.isNotBlank())
            assertTrue(s.surpriseMeCta.isNotBlank())
            assertTrue(s.crewsTitle.isNotBlank())
            assertTrue(s.capabilitiesTitle.isNotBlank())
        }
    }

    @Test
    fun realityEvents_verificationTiersCategoriesAndGeoDistance_behaveAccurately() {
        assertEquals(3, EventVerificationTier.entries.size)
        assertEquals(EventVerificationTier.UNVERIFIED_USER_SUBMITTED, EventVerificationTier.fromKey("UNKNOWN_TIER"))
        assertEquals(12, RealityEventCategory.entries.size)
        RealityEventCategory.entries.forEach { cat ->
            NiloraLanguage.entries.forEach { lang ->
                assertTrue(cat.label(lang).isNotBlank())
            }
        }
        // Cairo to Alexandria ~175-185 km, Cairo to itself == 0 km
        val cairo = GlobalCityHubCatalog.findHub("Cairo")!!
        val alex = GlobalCityHubCatalog.findHub("Alexandria")!!
        assertEquals(0.0, geoDistanceKm(cairo.latitude, cairo.longitude, cairo.latitude, cairo.longitude), 0.001)
        val cairoAlexKm = geoDistanceKm(cairo.latitude, cairo.longitude, alex.latitude, alex.longitude)
        assertTrue(cairoAlexKm in 160.0..210.0)
        assertTrue(GlobalCityHubCatalog.hubs.size >= 14)
    }

    @Test
    fun teamActivities_domainsSubCategoriesStatusesAndMissions_areDistinctAndHonest() {
        ActivityDomain.entries.forEach { domain ->
            val subs = ActivitySubCategory.forDomain(domain)
            assertTrue(subs.isNotEmpty())
            assertTrue(subs.all { it.domain == domain })
        }
        assertTrue(ActivitySubCategory.forDomain(ActivityDomain.PLAY_SPORTS).contains(ActivitySubCategory.PADEL))
        assertTrue(ActivitySubCategory.forDomain(ActivityDomain.COMMUNITY_IMPACT).contains(ActivitySubCategory.BEACH_CLEANUP))
        assertEquals(6, TeamUpStatus.entries.size)

        // Real-world vs Virtual mission execution modes remain explicitly separated
        assertEquals(2, MissionExecutionMode.entries.size)
        assertEquals(MissionExecutionMode.REAL_WORLD, MissionExecutionMode.fromKey("REAL_WORLD"))
        assertEquals(MissionExecutionMode.VIRTUAL, MissionExecutionMode.fromKey("VIRTUAL"))
        assertEquals(5, RealityMissionCatalog.starterTemplates.size)
        RealityMissionCatalog.starterTemplates.forEach { tpl ->
            assertEquals(MissionExecutionMode.REAL_WORLD, tpl.executionMode)
            assertTrue(tpl.safetyAndEcoRules.isNotBlank())
        }
    }

    @Test
    fun realityBridgeStages_challengeProvenance_routesAndPhase2Strings_areComplete() {
        val stages = RealityBridgeStage.entries.sortedBy { it.stepIndex }
        assertEquals(listOf(0, 1, 2, 3, 4), stages.map { it.stepIndex })
        assertEquals(RealityBridgeStage.VIRTUAL, stages.first())
        assertEquals(RealityBridgeStage.COMPLETED, stages.last())

        val defaultEntry = ChallengeEntry()
        assertEquals("USER_SUBMITTED", defaultEntry.verificationLabel)

        assertEquals("reality_event/ev_99", NiloraRoutes.realityEventDetail("ev_99"))
        assertEquals("team_activity/ta_42", NiloraRoutes.teamActivityDetail("ta_42"))

        NiloraLanguage.entries.forEach { lang ->
            val s = StringsProvider.forLanguage(lang)
            assertTrue(s.realityUniverseTitle.isNotBlank())
            assertTrue(s.realityEventsTitle.isNotBlank())
            assertTrue(s.goPlayTeamUpTitle.isNotBlank())
            assertTrue(s.realWorldMissionsTitle.isNotBlank())
            assertTrue(s.globalChallengesTitle.isNotBlank())
            assertTrue(s.realityBridgeTitle.isNotBlank())
            assertTrue(s.worldPulseTitle.isNotBlank())
        }

        // PlatformCapabilityRegistry includes Phase 2 items in Implemented & Partial buckets
        assertTrue(PlatformCapabilityRegistry.items.any { it.feature.contains("Reality Connected Universe", ignoreCase = true) })
        assertTrue(PlatformCapabilityRegistry.items.any { it.feature.contains("Open-Source Cultural Events", ignoreCase = true) })
    }
}
