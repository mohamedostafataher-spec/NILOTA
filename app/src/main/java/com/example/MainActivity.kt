package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.ui.theme.LocalReducedMotion
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.data.model.*
import com.example.data.repository.*
import com.example.firebase.attemptAutoSignIn
import com.example.firebase.configureAppCheck
import com.example.firebase.signOut
import com.example.localization.StringsProvider
import com.example.niloralive.AndroidSpeechRecognitionEngine
import com.example.niloralive.AndroidVoiceGenerationEngine
import com.example.niloralive.EgyptianTranslationEngine
import com.example.niloralive.SubtitleDisplayMode
import com.example.niloralive.VoiceMessageAudioRecorder
import com.example.ui.navigation.NiloraRoutes
import com.example.ui.screens.*
import com.example.ui.theme.NiloraTheme
import com.example.ui.viewmodel.*
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // Initialize Firebase App Check BEFORE any Firebase AI Logic / ViewModel calls
        configureAppCheck(this, intent)
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val appContext = applicationContext
        val prefsRepo = UserPreferencesRepository(appContext)
        val credentialManager = CredentialManager.create(this)

        setContent {
            val prefs by prefsRepo.preferencesFlow.collectAsStateWithLifecycle(initialValue = LocalAppPreferences())
            val language = NiloraLanguage.fromCode(prefs.languageCode)
            val strings = remember(language) { StringsProvider.forLanguage(language) }
            val darkTheme = when (prefs.darkThemeOverride) {
                "DARK" -> true
                "LIGHT" -> false
                else -> isSystemInDarkTheme()
            }

            var currentUser by remember { mutableStateOf(Firebase.auth.currentUser) }
            val scope = rememberCoroutineScope()

            DisposableEffect(Unit) {
                val listener = FirebaseAuth.AuthStateListener { auth ->
                    currentUser = auth.currentUser
                }
                Firebase.auth.addAuthStateListener(listener)
                onDispose { Firebase.auth.removeAuthStateListener(listener) }
            }

            LaunchedEffect(Unit) {
                if (Firebase.auth.currentUser == null) {
                    attemptAutoSignIn(
                        context = this@MainActivity,
                        credentialManager = credentialManager,
                        onAuthSuccess = { currentUser = Firebase.auth.currentUser },
                        onUnauthenticated = {},
                        scope = scope
                    )
                }
            }

            NiloraTheme(
                darkTheme = darkTheme,
                languageCode = language.code
            ) {
                val user = currentUser
                if (user == null) {
                    UnauthenticatedFlow(
                        prefs = prefs,
                        language = language,
                        strings = strings,
                        onSelectLanguage = { lang -> scope.launch { prefsRepo.setLanguage(lang.code) } },
                        onTogglePersonalization = { enabled -> scope.launch { prefsRepo.setPersonalizationEnabled(enabled) } },
                        onOnboardingDone = { scope.launch { prefsRepo.setOnboardingDone(true) } },
                        onAuthSuccess = { currentUser = Firebase.auth.currentUser }
                    )
                } else {
                    val uid = user.uid
                    key(uid) {
                        val firestoreRepo = remember(uid) { FirestoreDataRepository(appContext) }
                        val communityRepo = remember(uid) { CommunityAndMessagingRepository(appContext) }
                        val experienceRepo = remember(uid) { ExperienceConnectionRepository(appContext) }
                        val adventureRepo = remember(uid) { AdventureWorldRepository(appContext) }
                        val intelRepo = remember(uid) { DestinationIntelligenceRepository() }
                        val realityRepo = remember(uid) { RealityUniverseRepository(appContext) }
                        val eventsIntelRepo = remember(uid) { RealityEventsIntelligenceRepository() }
                        val locationRepo = remember(uid) { LocationAndPlacesRepository(appContext) }
                        val aiRepo = remember(uid) { TranslationAiRepository() }
                        val speechEngine = remember(uid) { AndroidSpeechRecognitionEngine(appContext) }
                        val translationEngine = remember(uid) { EgyptianTranslationEngine() }
                        val voiceEngine = remember(uid) { AndroidVoiceGenerationEngine(appContext) }
                        val voiceNoteRecorder = remember(uid) { VoiceMessageAudioRecorder(appContext) }

                        val mainVm: MainViewModel = viewModel(
                            key = "main_$uid",
                            factory = object : ViewModelProvider.Factory {
                                @Suppress("UNCHECKED_CAST")
                                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                                    MainViewModel(firestoreRepo, locationRepo, uid) as T
                            }
                        )
                        val translatorVm: TranslatorViewModel = viewModel(
                            key = "translator_$uid",
                            factory = object : ViewModelProvider.Factory {
                                @Suppress("UNCHECKED_CAST")
                                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                                    TranslatorViewModel(
                                        firestoreRepo = firestoreRepo,
                                        aiRepo = aiRepo,
                                        speechEngine = speechEngine,
                                        translationEngine = translationEngine,
                                        voiceEngine = voiceEngine,
                                        locationRepo = locationRepo,
                                        prefsRepo = prefsRepo,
                                        appContext = appContext,
                                        currentUserId = uid
                                    ) as T
                            }
                        )
                        val communityVm: CommunityMessagingViewModel = viewModel(
                            key = "community_$uid",
                            factory = object : ViewModelProvider.Factory {
                                @Suppress("UNCHECKED_CAST")
                                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                                    CommunityMessagingViewModel(
                                        communityRepo = communityRepo,
                                        firestoreRepo = firestoreRepo,
                                        locationRepo = locationRepo,
                                        translationEngine = translationEngine,
                                        sttEngine = speechEngine,
                                        ttsEngine = voiceEngine,
                                        voiceRecorder = voiceNoteRecorder,
                                        currentUserId = uid
                                    ) as T
                            }
                        )
                        val experienceVm: ExperienceConnectionViewModel = viewModel(
                            key = "experience_$uid",
                            factory = object : ViewModelProvider.Factory {
                                @Suppress("UNCHECKED_CAST")
                                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                                    ExperienceConnectionViewModel(
                                        experienceRepo = experienceRepo,
                                        communityRepo = communityRepo,
                                        firestoreRepo = firestoreRepo,
                                        translationEngine = translationEngine,
                                        sttEngine = speechEngine,
                                        ttsEngine = voiceEngine,
                                        voiceRecorder = voiceNoteRecorder,
                                        currentUserId = uid
                                    ) as T
                            }
                        )
                        val adventureVm: AdventureWorldViewModel = viewModel(
                            key = "adventure_$uid",
                            factory = object : ViewModelProvider.Factory {
                                @Suppress("UNCHECKED_CAST")
                                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                                    AdventureWorldViewModel(
                                        adventureRepo = adventureRepo,
                                        intelRepo = intelRepo,
                                        experienceRepo = experienceRepo,
                                        communityRepo = communityRepo,
                                        firestoreRepo = firestoreRepo,
                                        translationEngine = translationEngine,
                                        sttEngine = speechEngine,
                                        ttsEngine = voiceEngine,
                                        currentUserId = uid
                                    ) as T
                            }
                        )
                        val realityVm: RealityUniverseViewModel = viewModel(
                            key = "reality_$uid",
                            factory = object : ViewModelProvider.Factory {
                                @Suppress("UNCHECKED_CAST")
                                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                                    RealityUniverseViewModel(
                                        realityRepo = realityRepo,
                                        eventsIntelRepo = eventsIntelRepo,
                                        experienceRepo = experienceRepo,
                                        communityRepo = communityRepo,
                                        firestoreRepo = firestoreRepo,
                                        locationRepo = locationRepo,
                                        translationEngine = translationEngine,
                                        ttsEngine = voiceEngine,
                                        currentUserId = uid
                                    ) as T
                            }
                        )

                        AuthenticatedAppShell(
                            userEmail = user.email.orEmpty(),
                            userDisplayName = user.displayName.orEmpty(),
                            prefs = prefs,
                            prefsRepo = prefsRepo,
                            language = language,
                            strings = strings,
                            mainVm = mainVm,
                            translatorVm = translatorVm,
                            communityVm = communityVm,
                            experienceVm = experienceVm,
                            adventureVm = adventureVm,
                            realityVm = realityVm,
                            onSignOut = {
                                signOut(
                                    credentialManager = credentialManager,
                                    scope = scope,
                                    onSignOutComplete = { currentUser = null }
                                )
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun UnauthenticatedFlow(
    prefs: LocalAppPreferences,
    language: NiloraLanguage,
    strings: com.example.localization.AppStrings,
    onSelectLanguage: (NiloraLanguage) -> Unit,
    onTogglePersonalization: (Boolean) -> Unit = {},
    onOnboardingDone: () -> Unit,
    onAuthSuccess: () -> Unit
) {
    val navController = rememberNavController()
    var splashCompleted by remember { mutableStateOf(false) }

    NavHost(
        navController = navController,
        startDestination = NiloraRoutes.SPLASH
    ) {
        composable(NiloraRoutes.SPLASH) {
            SplashScreen(strings = strings) {
                if (!splashCompleted) {
                    splashCompleted = true
                    val next = if (!prefs.onboardingDone) NiloraRoutes.LANGUAGE_SELECTION else NiloraRoutes.LOGIN
                    navController.navigate(next) {
                        popUpTo(NiloraRoutes.SPLASH) { inclusive = true }
                    }
                }
            }
        }
        composable(NiloraRoutes.LANGUAGE_SELECTION) {
            LanguageSelectionScreen(
                currentLanguage = language,
                strings = strings,
                showBack = prefs.onboardingDone,
                onBack = { navController.popBackStack() },
                onSelectLanguage = onSelectLanguage,
                onContinue = {
                    if (!prefs.onboardingDone) {
                        navController.navigate(NiloraRoutes.ONBOARDING)
                    } else {
                        navController.popBackStack()
                    }
                }
            )
        }
        composable(NiloraRoutes.ONBOARDING) {
            OnboardingScreen(
                strings = strings,
                personalizationEnabled = prefs.personalizationEnabled,
                onTogglePersonalization = onTogglePersonalization,
                onFinish = {
                    onOnboardingDone()
                    navController.navigate(NiloraRoutes.LOGIN) {
                        popUpTo(NiloraRoutes.LANGUAGE_SELECTION) { inclusive = true }
                    }
                }
            )
        }
        composable(NiloraRoutes.LOGIN) {
            LoginScreen(
                strings = strings,
                onAuthSuccess = onAuthSuccess,
                onOpenLanguageSelection = { navController.navigate(NiloraRoutes.LANGUAGE_SELECTION) },
                onOpenPrivacyPolicy = { navController.navigate(NiloraRoutes.PRIVACY_POLICY) },
                onOpenTerms = { navController.navigate(NiloraRoutes.TERMS) },
                onOpenAbout = { navController.navigate(NiloraRoutes.ABOUT_NILORA) }
            )
        }
        composable(NiloraRoutes.PRIVACY_POLICY) {
            PrivacyPolicyScreen(strings = strings, onBack = { navController.popBackStack() })
        }
        composable(NiloraRoutes.TERMS) {
            TermsAndConditionsScreen(strings = strings, onBack = { navController.popBackStack() })
        }
        composable(NiloraRoutes.ABOUT_NILORA) {
            AboutNiloraScreen(
                strings = strings,
                onOpenPrivacyPolicy = { navController.navigate(NiloraRoutes.PRIVACY_POLICY) },
                onOpenTerms = { navController.navigate(NiloraRoutes.TERMS) },
                onBack = { navController.popBackStack() },
                onOpenCapabilities = { navController.navigate(NiloraRoutes.PLATFORM_CAPABILITIES) }
            )
        }
        composable(NiloraRoutes.PLATFORM_CAPABILITIES) {
            PlatformCapabilitiesScreen(
                strings = strings,
                onBack = { navController.popBackStack() }
            )
        }
    }
}

@Composable
private fun AuthenticatedAppShell(
    userEmail: String,
    userDisplayName: String,
    prefs: LocalAppPreferences,
    prefsRepo: UserPreferencesRepository,
    language: NiloraLanguage,
    strings: com.example.localization.AppStrings,
    mainVm: MainViewModel,
    translatorVm: TranslatorViewModel,
    communityVm: CommunityMessagingViewModel,
    experienceVm: ExperienceConnectionViewModel,
    adventureVm: AdventureWorldViewModel,
    realityVm: RealityUniverseViewModel,
    onSignOut: () -> Unit
) {
    val navController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val uiMeta by mainVm.uiMeta.collectAsStateWithLifecycle()
    val destinationsState by mainVm.destinationsState.collectAsStateWithLifecycle()
    val activitiesState by mainVm.activitiesState.collectAsStateWithLifecycle()
    val providersState by mainVm.providersState.collectAsStateWithLifecycle()
    val myProviderState by mainVm.myProviderProfileState.collectAsStateWithLifecycle()
    val travelerBookingsState by mainVm.travelerBookingsState.collectAsStateWithLifecycle()
    val providerBookingsState by mainVm.providerBookingsState.collectAsStateWithLifecycle()
    val favoritesState by mainVm.favoritesState.collectAsStateWithLifecycle()
    val notesState by mainVm.travelNotesState.collectAsStateWithLifecycle()
    val notificationsState by mainVm.notificationsState.collectAsStateWithLifecycle()
    val myRoomsState by mainVm.myRoomsState.collectAsStateWithLifecycle()

    val translatorState by translatorVm.state.collectAsStateWithLifecycle()
    val rmsNormalized by translatorVm.rmsNormalized.collectAsStateWithLifecycle()
    val isSpeaking by translatorVm.isSpeaking.collectAsStateWithLifecycle()
    val isMuted by translatorVm.isMuted.collectAsStateWithLifecycle()
    val ttsSpeed by translatorVm.ttsSpeed.collectAsStateWithLifecycle()
    val ttsVolume by translatorVm.ttsVolume.collectAsStateWithLifecycle()
    val voiceCapabilities by translatorVm.voiceCapabilities.collectAsStateWithLifecycle()
    val availableTtsVoices by translatorVm.availableTtsVoices.collectAsStateWithLifecycle()

    val communityFilters by communityVm.filters.collectAsStateWithLifecycle()
    val publicProfilesState by communityVm.publicProfilesState.collectAsStateWithLifecycle()
    val blockedUsersState by communityVm.blockedUsersState.collectAsStateWithLifecycle()
    val connectionRequestsState by communityVm.connectionRequestsState.collectAsStateWithLifecycle()
    val directConversationsState by communityVm.directConversationsState.collectAsStateWithLifecycle()
    val directChatState by communityVm.directChat.collectAsStateWithLifecycle()
    val discoverableGroupsState by communityVm.discoverableGroupsState.collectAsStateWithLifecycle()
    val myGroupsState by communityVm.myGroupsState.collectAsStateWithLifecycle()
    val groupRoomState by communityVm.groupRoom.collectAsStateWithLifecycle()
    val communityActionStatus by communityVm.actionStatus.collectAsStateWithLifecycle()

    val postcardsState by experienceVm.postcardsState.collectAsStateWithLifecycle()
    val cultureSwapsState by experienceVm.cultureSwapsState.collectAsStateWithLifecycle()
    val worldQuestsState by experienceVm.worldQuestsState.collectAsStateWithLifecycle()
    val memoriesState by experienceVm.memoriesState.collectAsStateWithLifecycle()
    val mediaDraft by experienceVm.mediaDraft.collectAsStateWithLifecycle()
    val postcardDetailState by experienceVm.postcardDetail.collectAsStateWithLifecycle()
    val swapDetailState by experienceVm.swapDetail.collectAsStateWithLifecycle()
    val questDetailState by experienceVm.questDetail.collectAsStateWithLifecycle()
    val experienceActionStatus by experienceVm.actionStatus.collectAsStateWithLifecycle()

    val worldUi by adventureVm.worldUi.collectAsStateWithLifecycle()
    val joinableSessionsState by adventureVm.joinableSessionsState.collectAsStateWithLifecycle()
    val matchProfilesState by adventureVm.matchProfilesState.collectAsStateWithLifecycle()
    val myCrewsState by adventureVm.myCrewsState.collectAsStateWithLifecycle()
    val openCrewsState by adventureVm.openCrewsState.collectAsStateWithLifecycle()
    val selectedCrew by adventureVm.selectedCrew.collectAsStateWithLifecycle()
    val crewMessages by adventureVm.crewMessages.collectAsStateWithLifecycle()
    val adventureActionStatus by adventureVm.actionStatus.collectAsStateWithLifecycle()

    val realityFilters by realityVm.filters.collectAsStateWithLifecycle()
    val localGps by realityVm.localGps.collectAsStateWithLifecycle()
    val gpsMessage by realityVm.gpsMessage.collectAsStateWithLifecycle()
    val externalEvents by realityVm.externalEvents.collectAsStateWithLifecycle()
    val isLoadingExternalEvents by realityVm.isLoadingExternalEvents.collectAsStateWithLifecycle()
    val missionPhotoBase64 by realityVm.missionPhotoBase64.collectAsStateWithLifecycle()
    val realityActionStatus by realityVm.actionStatus.collectAsStateWithLifecycle()
    val eventsState by realityVm.eventsState.collectAsStateWithLifecycle()
    val teamActivitiesState by realityVm.teamActivitiesState.collectAsStateWithLifecycle()
    val missionsState by realityVm.missionsState.collectAsStateWithLifecycle()
    val challengesState by realityVm.challengesState.collectAsStateWithLifecycle()
    val bridgeLinksState by realityVm.bridgeLinksState.collectAsStateWithLifecycle()
    val eventMessages by realityVm.eventMessages.collectAsStateWithLifecycle()
    val activityMessages by realityVm.activityMessages.collectAsStateWithLifecycle()
    val missionSubmissions by realityVm.missionSubmissions.collectAsStateWithLifecycle()
    val challengeEntries by realityVm.challengeEntries.collectAsStateWithLifecycle()

    LaunchedEffect(uiMeta.actionMessage) {
        val msg = uiMeta.actionMessage
        if (!msg.isNullOrBlank()) {
            snackbarHostState.showSnackbar(msg)
            mainVm.clearActionMessage()
        }
    }

    LaunchedEffect(communityActionStatus) {
        val msg = communityActionStatus.first
        if (!msg.isNullOrBlank()) {
            snackbarHostState.showSnackbar(msg)
            communityVm.clearStatus()
        }
    }

    LaunchedEffect(experienceActionStatus) {
        val msg = experienceActionStatus.first
        if (!msg.isNullOrBlank()) {
            snackbarHostState.showSnackbar(msg)
            experienceVm.clearStatus()
        }
    }

    LaunchedEffect(adventureActionStatus) {
        val msg = adventureActionStatus.first
        if (!msg.isNullOrBlank()) {
            snackbarHostState.showSnackbar(msg)
            adventureVm.clearStatus()
        }
    }

    LaunchedEffect(realityActionStatus) {
        val msg = realityActionStatus.first
        if (!msg.isNullOrBlank()) {
            snackbarHostState.showSnackbar(msg)
            realityVm.clearStatus()
        }
    }

    LaunchedEffect(
        prefs.spokenLanguageCode,
        prefs.targetTranslationLanguageCode,
        prefs.subtitleDisplayMode,
        prefs.preferOriginalAudio,
        prefs.preferEgyptianColloquial
    ) {
        val subtitleMode = SubtitleDisplayMode.fromKey(prefs.subtitleDisplayMode)
        communityVm.applyLanguageIdentityDefaults(
            spokenCode = prefs.spokenLanguageCode,
            targetCode = prefs.targetTranslationLanguageCode,
            subtitleMode = subtitleMode,
            preferOriginalAudio = prefs.preferOriginalAudio
        )
        experienceVm.applyLanguageIdentityDefaults(prefs.targetTranslationLanguageCode)
        adventureVm.applyLanguageIdentityDefaults(
            spokenCode = prefs.spokenLanguageCode,
            targetCode = prefs.targetTranslationLanguageCode,
            subtitleMode = subtitleMode
        )
        realityVm.applyLanguageIdentityDefaults(
            spokenCode = prefs.spokenLanguageCode,
            targetCode = prefs.targetTranslationLanguageCode,
            preferEgyptian = prefs.preferEgyptianColloquial
        )
    }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val topLevelRoutes = setOf(
        NiloraRoutes.HOME,
        NiloraRoutes.TOGETHER_HUB,
        NiloraRoutes.EVENTS_HUB,
        NiloraRoutes.REALTIME_TRANSLATOR,
        NiloraRoutes.PROFILE_HUB,
        NiloraRoutes.EXPLORE,
        NiloraRoutes.MESSAGES,
        NiloraRoutes.DISCOVER,
        NiloraRoutes.GROUPS,
        NiloraRoutes.WORLD_POSTCARDS,
        NiloraRoutes.CULTURE_SWAPS,
        NiloraRoutes.WORLD_QUESTS,
        NiloraRoutes.NILORA_MEMORY,
        NiloraRoutes.ADVENTURE_DISCOVERY,
        NiloraRoutes.NILORA_CREWS,
        NiloraRoutes.REALITY_HUB
    )
    val showTopLevelNav = currentRoute in topLevelRoutes

    val unreadCount = (notificationsState as? UiState.Success)?.data?.count { !it.readStatus } ?: 0
    val pendingReqCount = (connectionRequestsState as? UiState.Success)?.data?.count {
        it.receiverId == mainVm.currentUserId && it.status == "PENDING"
    } ?: 0
    val liveAdventureCount = (joinableSessionsState as? UiState.Success)?.data?.count { it.status == "ACTIVE" } ?: 0
    val destList = (destinationsState as? UiState.Success)?.data.orEmpty()
    val actList = (activitiesState as? UiState.Success)?.data.orEmpty()
    val provList = (providersState as? UiState.Success)?.data.orEmpty()
    val favList = (favoritesState as? UiState.Success)?.data.orEmpty()
    val blockedIds = (blockedUsersState as? UiState.Success)?.data?.map { it.blockedUserId }?.toSet().orEmpty()
    val eventsList = (eventsState as? UiState.Success)?.data.orEmpty()
    val teamList = (teamActivitiesState as? UiState.Success)?.data.orEmpty()
    val missionsList = (missionsState as? UiState.Success)?.data.orEmpty()
    val challengesList = (challengesState as? UiState.Success)?.data.orEmpty()
    val bridgeList = (bridgeLinksState as? UiState.Success)?.data.orEmpty()

    // Phase 3: Unified 5-Pillar Architecture for bottom NavigationBar
    val primaryNavItems = listOf(
        Triple(NiloraRoutes.HOME, strings.navExplore, Icons.Filled.Explore),
        Triple(NiloraRoutes.TOGETHER_HUB, strings.navTogether, Icons.Filled.Groups),
        Triple(NiloraRoutes.EVENTS_HUB, strings.navEvents, Icons.Filled.Celebration),
        Triple(NiloraRoutes.REALTIME_TRANSLATOR, strings.navLive, Icons.Filled.Translate),
        Triple(NiloraRoutes.PROFILE_HUB, strings.navMyWorld, Icons.Filled.Person)
    )
    // Secondary shortcuts in Tablet/Desktop NavigationRail
    val secondaryRailItems = listOf(
        Triple(NiloraRoutes.EXPLORE, "Destinations", Icons.Filled.TravelExplore),
        Triple(NiloraRoutes.ADVENTURE_DISCOVERY, "3D Worlds", Icons.Filled.Waves),
        Triple(NiloraRoutes.REALITY_HUB, "Go & Play", Icons.Filled.SportsSoccer),
        Triple(NiloraRoutes.NILORA_CREWS, "Crews", Icons.Filled.Diversity3),
        Triple(NiloraRoutes.GROUPS, "Group Live", Icons.Filled.Forum),
        Triple(NiloraRoutes.MESSAGES, "1:1 Chats", Icons.AutoMirrored.Filled.Chat),
        Triple(NiloraRoutes.NILORA_MEMORY, "Memory", Icons.Filled.AutoStories)
    )

    fun navigateTopTab(route: String) {
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    val reduceMotion = LocalReducedMotion.current

    NiloraResponsiveNavShell(
        showTopLevelNav = showTopLevelNav,
        currentRoute = currentRoute,
        navItems = primaryNavItems,
        secondaryRailItems = secondaryRailItems,
        snackbarHostState = snackbarHostState,
        onSelectTab = ::navigateTopTab
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = NiloraRoutes.HOME,
            modifier = Modifier.padding(innerPadding),
            enterTransition = { if (reduceMotion) EnterTransition.None else fadeIn(tween(200)) },
            exitTransition = { if (reduceMotion) ExitTransition.None else fadeOut(tween(170)) },
            popEnterTransition = { if (reduceMotion) EnterTransition.None else fadeIn(tween(200)) },
            popExitTransition = { if (reduceMotion) ExitTransition.None else fadeOut(tween(170)) }
        ) {
            composable(NiloraRoutes.HOME) {
                HomeScreen(
                    strings = strings,
                    userProfile = uiMeta.userProfile,
                    isAdmin = uiMeta.isAdmin,
                    isOnline = mainVm.isOnline,
                    destinationsState = destinationsState,
                    activitiesState = activitiesState,
                    unreadNotificationsCount = unreadCount,
                    onOpenTranslator = { navigateTopTab(NiloraRoutes.REALTIME_TRANSLATOR) },
                    onOpenSharedTranslator = { navController.navigate(NiloraRoutes.SHARED_TRANSLATOR) },
                    onOpenAiAssistant = { navController.navigate(NiloraRoutes.AI_ASSISTANT) },
                    onOpenMap = { navController.navigate(NiloraRoutes.INTERACTIVE_MAP) },
                    onOpenExplore = { navigateTopTab(NiloraRoutes.EXPLORE) },
                    onOpenSearch = { navController.navigate(NiloraRoutes.SEARCH_RESULTS) },
                    onOpenBeaches = { navController.navigate(NiloraRoutes.BEACHES) },
                    onOpenActivities = { navController.navigate(NiloraRoutes.ACTIVITIES) },
                    onOpenHotels = { navController.navigate(NiloraRoutes.HOTELS) },
                    onOpenRestaurants = { navController.navigate(NiloraRoutes.RESTAURANTS) },
                    onOpenNotifications = { navController.navigate(NiloraRoutes.NOTIFICATIONS) },
                    onOpenSettings = { navController.navigate(NiloraRoutes.SETTINGS) },
                    onOpenAdmin = { navController.navigate(NiloraRoutes.ADMIN_DASHBOARD) },
                    onSelectCity = { cityName ->
                        mainVm.setCityFilter(cityName)
                        navigateTopTab(NiloraRoutes.EXPLORE)
                    },
                    onOpenDestination = { id -> navController.navigate(NiloraRoutes.destinationDetails(id)) },
                    onOpenActivity = { id -> navController.navigate(NiloraRoutes.activityDetails(id)) },
                    onOpenProviderRegistration = { navController.navigate(NiloraRoutes.PROVIDER_REGISTRATION) },
                    onOpenMessages = { navigateTopTab(NiloraRoutes.MESSAGES) },
                    onOpenDiscoverCommunity = { navigateTopTab(NiloraRoutes.DISCOVER) },
                    onOpenGroups = { navigateTopTab(NiloraRoutes.GROUPS) },
                    currentUserId = mainVm.currentUserId,
                    conversationsState = directConversationsState,
                    suggestedProfilesState = publicProfilesState,
                    blockedUsersState = blockedUsersState,
                    onOpenConversation = { cid -> navController.navigate(NiloraRoutes.directChat(cid)) },
                    onOpenSuggestedMember = { partner ->
                        communityVm.startDirectChatWith(partner) { cid ->
                            navController.navigate(NiloraRoutes.directChat(cid))
                        }
                    },
                    postcardsState = postcardsState,
                    cultureSwapsState = cultureSwapsState,
                    worldQuestsState = worldQuestsState,
                    memoriesState = memoriesState,
                    onLiveAMoment = {
                        experienceVm.resetMediaDraft()
                        navController.navigate(NiloraRoutes.POSTCARD_CREATE)
                    },
                    onBrowsePostcards = { navController.navigate(NiloraRoutes.WORLD_POSTCARDS) },
                    onOpenPostcard = { id -> navController.navigate(NiloraRoutes.postcardDetail(id)) },
                    onSwapCultures = { navController.navigate(NiloraRoutes.CULTURE_SWAPS) },
                    onOpenCultureSwap = { id -> navController.navigate(NiloraRoutes.cultureSwapDetail(id)) },
                    onStartWorldQuest = { navController.navigate(NiloraRoutes.WORLD_QUESTS) },
                    onOpenWorldQuest = { id -> navController.navigate(NiloraRoutes.worldQuestDetail(id)) },
                    onOpenMemories = { navController.navigate(NiloraRoutes.NILORA_MEMORY) },
                    liveAdventureSessionsCount = liveAdventureCount,
                    onOpenAdventureGateway = { navController.navigate(NiloraRoutes.ADVENTURE_DISCOVERY) },
                    onEnterFeaturedAdventure = {
                        val adv = worldUi.selectedAdventure
                        navController.navigate(NiloraRoutes.immersiveAdventure(adv.id))
                    },
                    onOpenAdventureMatchmaking = { navController.navigate(NiloraRoutes.ADVENTURE_MATCHMAKING) },
                    onOpenCrews = { navController.navigate(NiloraRoutes.NILORA_CREWS) },
                    realityEventsCount = eventsList.size,
                    teamActivitiesCount = teamList.size,
                    onOpenRealityHub = { navigateTopTab(NiloraRoutes.REALITY_HUB) },
                    onOpenRealityEvents = { navController.navigate(NiloraRoutes.REALITY_EVENTS) },
                    onOpenMissions = { navController.navigate(NiloraRoutes.REAL_WORLD_MISSIONS) },
                    onOpenChallenges = { navController.navigate(NiloraRoutes.GLOBAL_CHALLENGES) },
                    onOpenRealityBridge = { navController.navigate(NiloraRoutes.REALITY_BRIDGE) },
                    personalizationEnabled = prefs.personalizationEnabled,
                    savedDestinationIds = favList.map { it.targetId }.toSet(),
                    onTogglePersonalization = { enabled -> scope.launch { prefsRepo.setPersonalizationEnabled(enabled) } },
                    onOpenUnifiedSearch = { navController.navigate(NiloraRoutes.UNIFIED_SEARCH) },
                    onOpenAdventureById = { advId -> navController.navigate(NiloraRoutes.immersiveAdventure(advId)) }
                )
            }

            composable(NiloraRoutes.TOGETHER_HUB) {
                TogetherHubScreen(
                    strings = strings,
                    currentUserId = mainVm.currentUserId,
                    userProfile = uiMeta.userProfile,
                    personalizationEnabled = prefs.personalizationEnabled,
                    pendingRequestsCount = pendingReqCount,
                    teamActivitiesState = teamActivitiesState,
                    openCrewsState = openCrewsState,
                    publicProfilesState = publicProfilesState,
                    postcardsState = postcardsState,
                    cultureSwapsState = cultureSwapsState,
                    blockedIds = blockedIds,
                    onTogglePersonalization = { enabled -> scope.launch { prefsRepo.setPersonalizationEnabled(enabled) } },
                    onOpenUnifiedSearch = { navController.navigate(NiloraRoutes.UNIFIED_SEARCH) },
                    onOpenTeamUpSports = { navController.navigate(NiloraRoutes.REALITY_HUB) },
                    onOpenTeamActivityDetail = { id -> navController.navigate(NiloraRoutes.teamActivityDetail(id)) },
                    onOpenPeopleDiscovery = { navController.navigate(NiloraRoutes.DISCOVER) },
                    onOpenCrews = { navController.navigate(NiloraRoutes.NILORA_CREWS) },
                    onOpenCrewDetail = { cid -> navController.navigate(NiloraRoutes.crewDetail(cid)) },
                    onOpenMatchmaking = { navController.navigate(NiloraRoutes.ADVENTURE_MATCHMAKING) },
                    onOpenGroups = { navController.navigate(NiloraRoutes.GROUPS) },
                    onOpenDirectMessages = { navController.navigate(NiloraRoutes.MESSAGES) },
                    onOpenWorldPostcards = { navController.navigate(NiloraRoutes.WORLD_POSTCARDS) },
                    onOpenPostcardDetail = { id -> navController.navigate(NiloraRoutes.postcardDetail(id)) },
                    onCreatePostcard = {
                        experienceVm.resetMediaDraft()
                        navController.navigate(NiloraRoutes.POSTCARD_CREATE)
                    },
                    onOpenCultureSwaps = { navController.navigate(NiloraRoutes.CULTURE_SWAPS) },
                    onOpenCultureSwapDetail = { id -> navController.navigate(NiloraRoutes.cultureSwapDetail(id)) },
                    onConnectPerson = { partner ->
                        communityVm.startDirectChatWith(partner) { cid ->
                            navController.navigate(NiloraRoutes.directChat(cid))
                        }
                    }
                )
            }

            composable(NiloraRoutes.EVENTS_HUB) {
                EventsHubScreen(
                    strings = strings,
                    userProfile = uiMeta.userProfile,
                    personalizationEnabled = prefs.personalizationEnabled,
                    eventsState = eventsState,
                    externalEvents = externalEvents,
                    missionsState = missionsState,
                    challengesState = challengesState,
                    bridgeLinksState = bridgeLinksState,
                    blockedIds = blockedIds,
                    onTogglePersonalization = { enabled -> scope.launch { prefsRepo.setPersonalizationEnabled(enabled) } },
                    onOpenUnifiedSearch = { navController.navigate(NiloraRoutes.UNIFIED_SEARCH) },
                    onOpenRealityEvents = { navController.navigate(NiloraRoutes.REALITY_EVENTS) },
                    onOpenEventDetail = { id -> navController.navigate(NiloraRoutes.realityEventDetail(id)) },
                    onOpenMissions = { navController.navigate(NiloraRoutes.REAL_WORLD_MISSIONS) },
                    onOpenChallenges = { navController.navigate(NiloraRoutes.GLOBAL_CHALLENGES) },
                    onOpenRealityBridge = { navController.navigate(NiloraRoutes.REALITY_BRIDGE) },
                    onOpenWorldPulseMap = { navController.navigate(NiloraRoutes.INTERACTIVE_MAP) },
                    onRefreshExternalEvents = {
                        val hub = GlobalCityHubCatalog.findHub(realityFilters.selectedCity)
                        realityVm.refreshExternalEventFeed(hub?.cityName ?: "Cairo", hub?.countryName ?: "Egypt")
                    }
                )
            }

            composable(NiloraRoutes.UNIFIED_SEARCH) {
                UnifiedSearchScreen(
                    strings = strings,
                    query = uiMeta.searchQuery,
                    currentUserId = mainVm.currentUserId,
                    onQueryChange = mainVm::setSearchQuery,
                    destinationsState = destinationsState,
                    eventsState = eventsState,
                    teamActivitiesState = teamActivitiesState,
                    discoverableGroupsState = discoverableGroupsState,
                    myGroupsState = myGroupsState,
                    openCrewsState = openCrewsState,
                    myCrewsState = myCrewsState,
                    publicProfilesState = publicProfilesState,
                    blockedUsersState = blockedUsersState,
                    providersState = providersState,
                    placesOutcome = uiMeta.placesOutcome,
                    isSearchingPlaces = uiMeta.isSearchingPlaces,
                    isGoogleMapsKeyConfigured = mainVm.isGoogleMapsKeyConfigured,
                    onSearchGooglePlaces = mainVm::searchLivePlaces,
                    onSelectCity = { cityName ->
                        mainVm.setCityFilter(cityName)
                        navController.navigate(NiloraRoutes.EXPLORE)
                    },
                    onOpenDestination = { id -> navController.navigate(NiloraRoutes.destinationDetails(id)) },
                    onOpenAdventure = { id -> navController.navigate(NiloraRoutes.immersiveAdventure(id)) },
                    onOpenEvent = { id -> navController.navigate(NiloraRoutes.realityEventDetail(id)) },
                    onOpenTeamActivity = { id -> navController.navigate(NiloraRoutes.teamActivityDetail(id)) },
                    onOpenGroup = { gid -> navController.navigate(NiloraRoutes.groupRoom(gid)) },
                    onOpenCrew = { cid -> navController.navigate(NiloraRoutes.crewDetail(cid)) },
                    onOpenPerson = { partner ->
                        communityVm.startDirectChatWith(partner) { cid ->
                            navController.navigate(NiloraRoutes.directChat(cid))
                        }
                    },
                    onOpenProvider = { id -> navController.navigate(NiloraRoutes.providerProfile(id)) },
                    onBack = { navController.popBackStack() }
                )
            }

            composable(NiloraRoutes.EXPLORE) {
                ExploreScreen(
                    language = language,
                    selectedCountry = uiMeta.selectedCountryFilter,
                    selectedCity = uiMeta.selectedCityFilter,
                    selectedCategory = uiMeta.selectedCategoryFilter,
                    destinationsState = destinationsState,
                    onCountryChange = mainVm::setCountryFilter,
                    onCityChange = mainVm::setCityFilter,
                    onCategoryChange = mainVm::setCategoryFilter,
                    onOpenDestination = { id -> navController.navigate(NiloraRoutes.destinationDetails(id)) },
                    onOpenSearch = { navController.navigate(NiloraRoutes.UNIFIED_SEARCH) },
                    onOpenMap = { navController.navigate(NiloraRoutes.INTERACTIVE_MAP) },
                    onOpenBeaches = { navController.navigate(NiloraRoutes.BEACHES) },
                    onOpenActivities = { navController.navigate(NiloraRoutes.ACTIVITIES) },
                    onOpenHotels = { navController.navigate(NiloraRoutes.HOTELS) },
                    onOpenRestaurants = { navController.navigate(NiloraRoutes.RESTAURANTS) },
                    onOpenSavedPlaces = { navController.navigate(NiloraRoutes.SAVED_PLACES) },
                    onOpenRealityHub = { navigateTopTab(NiloraRoutes.REALITY_HUB) },
                    onOpenRealityEvents = { navController.navigate(NiloraRoutes.REALITY_EVENTS) }
                )
            }

            composable(NiloraRoutes.SEARCH_RESULTS) {
                SearchResultsScreen(
                    query = uiMeta.searchQuery,
                    onQueryChange = mainVm::setSearchQuery,
                    destinationsState = destinationsState,
                    activitiesState = activitiesState,
                    providersState = providersState,
                    placesOutcome = uiMeta.placesOutcome,
                    isSearchingPlaces = uiMeta.isSearchingPlaces,
                    isGoogleMapsKeyConfigured = mainVm.isGoogleMapsKeyConfigured,
                    onSearchGooglePlaces = mainVm::searchLivePlaces,
                    onOpenDestination = { id -> navController.navigate(NiloraRoutes.destinationDetails(id)) },
                    onOpenActivity = { id -> navController.navigate(NiloraRoutes.activityDetails(id)) },
                    onOpenProvider = { id -> navController.navigate(NiloraRoutes.providerProfile(id)) },
                    onBack = { navController.popBackStack() }
                )
            }

            composable(NiloraRoutes.DESTINATION_DETAILS) { backStackEntry ->
                val destId = backStackEntry.arguments?.getString("destinationId").orEmpty()
                val dest = destList.firstOrNull { it.id == destId }
                val isSaved = favList.any { it.targetId == destId }
                DestinationDetailsScreen(
                    destination = dest,
                    isSaved = isSaved,
                    onToggleSave = {
                        if (dest != null) {
                            mainVm.toggleFavorite(dest.id, "DESTINATION", dest.title, dest.cityName, dest.category, isSaved)
                        }
                    },
                    onReport = { reason ->
                        if (dest != null) mainVm.submitReport("DESTINATION", dest.id, reason, "")
                    },
                    onOpenMap = { navController.navigate(NiloraRoutes.INTERACTIVE_MAP) },
                    onBack = { navController.popBackStack() },
                    onOpenLiveTranslator = { navigateTopTab(NiloraRoutes.REALTIME_TRANSLATOR) },
                    onOpenNearbyEvents = { city ->
                        realityVm.selectCityHub(city)
                        navController.navigate(NiloraRoutes.REALITY_EVENTS)
                    }
                )
            }

            composable(NiloraRoutes.BEACHES) {
                BeachesScreen(
                    destinationsState = destinationsState,
                    activitiesState = activitiesState,
                    onOpenDestination = { id -> navController.navigate(NiloraRoutes.destinationDetails(id)) },
                    onOpenActivity = { id -> navController.navigate(NiloraRoutes.activityDetails(id)) },
                    onBack = { navController.popBackStack() }
                )
            }

            composable(NiloraRoutes.ACTIVITIES) {
                ActivitiesScreen(
                    activitiesState = activitiesState,
                    onOpenActivity = { id -> navController.navigate(NiloraRoutes.activityDetails(id)) },
                    onOpenProviderRegistration = { navController.navigate(NiloraRoutes.PROVIDER_REGISTRATION) },
                    onBack = { navController.popBackStack() }
                )
            }

            composable(NiloraRoutes.HOTELS) {
                HotelsScreen(
                    destinationsState = destinationsState,
                    onOpenDestination = { id -> navController.navigate(NiloraRoutes.destinationDetails(id)) },
                    onOpenSearch = { navController.navigate(NiloraRoutes.SEARCH_RESULTS) },
                    onBack = { navController.popBackStack() }
                )
            }

            composable(NiloraRoutes.RESTAURANTS) {
                RestaurantsScreen(
                    destinationsState = destinationsState,
                    onOpenDestination = { id -> navController.navigate(NiloraRoutes.destinationDetails(id)) },
                    onOpenSearch = { navController.navigate(NiloraRoutes.SEARCH_RESULTS) },
                    onBack = { navController.popBackStack() }
                )
            }

            composable(NiloraRoutes.INTERACTIVE_MAP) {
                InteractiveMapScreen(
                    destinationsState = destinationsState,
                    gpsLocation = uiMeta.gpsLocation,
                    gpsStatusMessage = uiMeta.gpsStatusMessage,
                    placesOutcome = uiMeta.placesOutcome,
                    isGoogleMapsKeyConfigured = mainVm.isGoogleMapsKeyConfigured,
                    onRequestGpsLocation = mainVm::refreshGpsLocation,
                    onSearchPlaces = mainVm::searchLivePlaces,
                    onOpenDestination = { id -> navController.navigate(NiloraRoutes.destinationDetails(id)) },
                    onBack = { navController.popBackStack() },
                    realityEventsState = eventsState,
                    teamActivitiesState = teamActivitiesState,
                    missionsState = missionsState,
                    challengesState = challengesState,
                    liveSessionsState = joinableSessionsState,
                    onOpenRealityEvent = { id -> navController.navigate(NiloraRoutes.realityEventDetail(id)) },
                    onOpenTeamActivity = { id -> navController.navigate(NiloraRoutes.teamActivityDetail(id)) },
                    onOpenMissions = { navController.navigate(NiloraRoutes.REAL_WORLD_MISSIONS) },
                    onOpenChallenges = { navController.navigate(NiloraRoutes.GLOBAL_CHALLENGES) },
                    onOpenAdventureWorld = { id -> navController.navigate(NiloraRoutes.immersiveAdventure(id)) }
                )
            }

            composable(NiloraRoutes.REALTIME_TRANSLATOR) {
                RealTimeTranslatorScreen(
                    strings = strings,
                    state = translatorState,
                    voiceCapabilities = voiceCapabilities,
                    myRoomsState = myRoomsState,
                    isOnline = mainVm.isOnline,
                    onGrantVoiceConsent = translatorVm::grantVoiceConsent,
                    onSetArabicDialectMode = translatorVm::setArabicDialectMode,
                    onSetAutoTranslateAfterSpeech = translatorVm::setAutoTranslateAfterSpeech,
                    onOpenCreateRoom = { navController.navigate(NiloraRoutes.CREATE_ROOM) },
                    onOpenJoinRoom = { navController.navigate(NiloraRoutes.JOIN_ROOM) },
                    onOpenSharedMode = { navController.navigate(NiloraRoutes.SHARED_TRANSLATOR) },
                    onOpenLiveEvaluation = { navController.navigate(NiloraRoutes.LIVE_EVALUATION) },
                    onResumeRoom = { code ->
                        translatorVm.attachToRoom(code)
                        navController.navigate(NiloraRoutes.CREATE_ROOM)
                    },
                    onOpenGroupsLive = { navController.navigate(NiloraRoutes.GROUPS) },
                    onOpenDirectChats = { navController.navigate(NiloraRoutes.MESSAGES) },
                    onOpenAiAssistant = { navController.navigate(NiloraRoutes.AI_ASSISTANT) },
                    onEditLanguageIdentity = { navigateTopTab(NiloraRoutes.PROFILE_HUB) },
                    interfaceLanguageCode = language.code,
                    availableVoices = availableTtsVoices,
                    onSetTranslationDomain = translatorVm::setTranslationDomain,
                    onToggleAutoDetectSource = translatorVm::setAutoDetectSource,
                    onTogglePreferOriginalAudio = translatorVm::setPreferOriginalAudio,
                    onSelectTtsVoice = translatorVm::selectTtsVoice
                )
            }

            composable(NiloraRoutes.LIVE_EVALUATION) {
                LiveEvaluationScreen(
                    state = translatorState,
                    voiceCapabilities = voiceCapabilities,
                    onRefreshDiagnostics = translatorVm::refreshHardwareAndModelDiagnostics,
                    onRunAmbientCheck = translatorVm::runAmbientMicrophoneCheck,
                    onSetArabicDialectMode = translatorVm::setArabicDialectMode,
                    onRecordTrial = { sLang, tLang, scenario, refTr, recTr, refTrans, notes ->
                        translatorVm.recordEvaluationTrial(
                            sourceLang = sLang,
                            targetLang = tLang,
                            scenarioLabel = scenario,
                            referenceTranscript = refTr,
                            recognizedTranscript = recTr,
                            referenceTranslation = refTrans,
                            humanNotes = notes
                        )
                    },
                    onClearTrials = translatorVm::clearEvaluationTrials,
                    onBack = { navController.popBackStack() }
                )
            }

            composable(NiloraRoutes.CREATE_ROOM) {
                val speakerName = uiMeta.userProfile?.displayName?.ifBlank { userDisplayName } ?: userDisplayName.ifBlank { "Host" }
                CreateConversationRoomScreen(
                    userName = speakerName,
                    defaultLanguage = language,
                    state = translatorState,
                    rmsNormalized = rmsNormalized,
                    isSpeaking = isSpeaking,
                    isMuted = isMuted,
                    ttsSpeed = ttsSpeed,
                    ttsVolume = ttsVolume,
                    onGrantVoiceConsent = translatorVm::grantVoiceConsent,
                    onCreateRoom = { lang, retain -> translatorVm.createRoom(speakerName, lang, retain) {} },
                    onSelectMyLanguage = translatorVm::setMyRoomLanguage,
                    onStartVoice = { translatorVm.startVoiceCaptureForRoom(speakerName) },
                    onStopVoice = translatorVm::stopVoiceCapture,
                    onUpdatePendingTranscript = translatorVm::updatePendingTranscript,
                    onConfirmPendingTranscript = { translatorVm.submitEditedPendingTranscript(speakerName) },
                    onDismissPendingTranscript = translatorVm::dismissPendingCorrection,
                    onSendText = { txt -> translatorVm.sendRoomUtterance(speakerName, txt) },
                    onRepeatAudio = translatorVm::speakTranslatedAudio,
                    onReplayLastAudio = translatorVm::replayLastAudio,
                    onPauseOrStopAudio = translatorVm::pauseOrStopAudio,
                    onToggleMute = { translatorVm.voiceEngine.toggleMute() },
                    onSetSpeed = translatorVm::setTtsSpeed,
                    onSetVolume = translatorVm::setTtsVolume,
                    onLeaveRoom = {
                        translatorVm.leaveRoomSessionLocally()
                        navController.popBackStack()
                    },
                    onEndRoom = translatorVm::endActiveRoom,
                    onBack = { navController.popBackStack() },
                    onSetSubtitleMode = translatorVm::setSubtitleDisplayMode,
                    onSetTranslationDomain = translatorVm::setTranslationDomain,
                    onToggleAutoDetectSource = translatorVm::setAutoDetectSource,
                    onTogglePreferOriginalAudio = translatorVm::setPreferOriginalAudio,
                    availableVoices = availableTtsVoices,
                    onSelectVoice = translatorVm::selectTtsVoice
                )
            }

            composable(NiloraRoutes.JOIN_ROOM) {
                val speakerName = uiMeta.userProfile?.displayName?.ifBlank { userDisplayName } ?: userDisplayName.ifBlank { "Guest" }
                JoinConversationRoomScreen(
                    state = translatorState,
                    rmsNormalized = rmsNormalized,
                    isSpeaking = isSpeaking,
                    currentUserId = mainVm.currentUserId,
                    isMuted = isMuted,
                    ttsSpeed = ttsSpeed,
                    ttsVolume = ttsVolume,
                    onGrantVoiceConsent = translatorVm::grantVoiceConsent,
                    onJoinRoom = { code, lang -> translatorVm.joinRoom(code, speakerName, lang) {} },
                    onSelectMyLanguage = translatorVm::setMyRoomLanguage,
                    onStartVoice = { translatorVm.startVoiceCaptureForRoom(speakerName) },
                    onStopVoice = translatorVm::stopVoiceCapture,
                    onUpdatePendingTranscript = translatorVm::updatePendingTranscript,
                    onConfirmPendingTranscript = { translatorVm.submitEditedPendingTranscript(speakerName) },
                    onDismissPendingTranscript = translatorVm::dismissPendingCorrection,
                    onSendText = { txt -> translatorVm.sendRoomUtterance(speakerName, txt) },
                    onRepeatAudio = translatorVm::speakTranslatedAudio,
                    onReplayLastAudio = translatorVm::replayLastAudio,
                    onPauseOrStopAudio = translatorVm::pauseOrStopAudio,
                    onToggleMute = { translatorVm.voiceEngine.toggleMute() },
                    onSetSpeed = translatorVm::setTtsSpeed,
                    onSetVolume = translatorVm::setTtsVolume,
                    onLeaveRoom = {
                        translatorVm.leaveRoomSessionLocally()
                        navController.popBackStack()
                    },
                    onEndRoom = translatorVm::endActiveRoom,
                    onBack = { navController.popBackStack() },
                    onSetSubtitleMode = translatorVm::setSubtitleDisplayMode,
                    onSetTranslationDomain = translatorVm::setTranslationDomain,
                    onToggleAutoDetectSource = translatorVm::setAutoDetectSource,
                    onTogglePreferOriginalAudio = translatorVm::setPreferOriginalAudio,
                    availableVoices = availableTtsVoices,
                    onSelectVoice = translatorVm::selectTtsVoice
                )
            }

            composable(NiloraRoutes.SHARED_TRANSLATOR) {
                SharedPhoneTranslatorScreen(
                    strings = strings,
                    state = translatorState,
                    rmsNormalized = rmsNormalized,
                    isSpeaking = isSpeaking,
                    isMuted = isMuted,
                    ttsSpeed = ttsSpeed,
                    ttsVolume = ttsVolume,
                    onGrantVoiceConsent = translatorVm::grantVoiceConsent,
                    onToggleTopRotation = translatorVm::toggleTopPanelRotation,
                    onSetTopLanguage = translatorVm::setTopLanguage,
                    onSetBottomLanguage = translatorVm::setBottomLanguage,
                    onStartVoice = translatorVm::startVoiceCaptureShared,
                    onStopVoice = translatorVm::stopVoiceCapture,
                    onUpdatePendingTranscript = translatorVm::updatePendingTranscript,
                    onConfirmPendingTranscript = { translatorVm.submitEditedPendingTranscript() },
                    onDismissPendingTranscript = translatorVm::dismissPendingCorrection,
                    onSendText = { side, txt -> translatorVm.translateSharedUtterance(side, txt) },
                    onRepeatAudio = translatorVm::speakTranslatedAudio,
                    onReplayLastAudio = translatorVm::replayLastAudio,
                    onPauseOrStopAudio = translatorVm::pauseOrStopAudio,
                    onToggleMute = { translatorVm.voiceEngine.toggleMute() },
                    onSetSpeed = translatorVm::setTtsSpeed,
                    onSetVolume = translatorVm::setTtsVolume,
                    onEndSession = {
                        translatorVm.clearSharedSession()
                        navController.popBackStack()
                    },
                    onBack = { navController.popBackStack() },
                    onSetSubtitleMode = translatorVm::setSubtitleDisplayMode,
                    onSetTranslationDomain = translatorVm::setTranslationDomain,
                    onToggleAutoDetectSource = translatorVm::setAutoDetectSource,
                    onTogglePreferOriginalAudio = translatorVm::setPreferOriginalAudio,
                    availableVoices = availableTtsVoices,
                    onSelectVoice = translatorVm::selectTtsVoice
                )
            }

            composable(NiloraRoutes.AI_ASSISTANT) {
                AiTravelAssistantScreen(
                    language = language,
                    state = translatorState,
                    verifiedCountSummary = "${destList.size} verified destinations • ${actList.size} activities • ${eventsList.size} reality events • ${teamList.size} team-ups",
                    onAskQuestion = { q ->
                        translatorVm.askAssistant(
                            query = q,
                            language = language,
                            verifiedDestinations = destList,
                            verifiedActivities = actList,
                            verifiedProviders = provList,
                            realityEvents = eventsList,
                            teamActivities = teamList,
                            missions = missionsList,
                            challenges = challengesList,
                            bridgeLinks = bridgeList
                        )
                    }
                )
            }

            composable(NiloraRoutes.ACTIVITY_DETAILS) { backStackEntry ->
                val actId = backStackEntry.arguments?.getString("activityId").orEmpty()
                val activity = actList.firstOrNull { it.id == actId }
                val isSaved = favList.any { it.targetId == actId }
                ActivityDetailsScreen(
                    activity = activity,
                    isSaved = isSaved,
                    onToggleSave = {
                        if (activity != null) {
                            mainVm.toggleFavorite(activity.id, "ACTIVITY", activity.title, activity.cityName, activity.category, isSaved)
                        }
                    },
                    onOpenProvider = { pid -> navController.navigate(NiloraRoutes.providerProfile(pid)) },
                    onBookRequest = { aid -> navController.navigate(NiloraRoutes.bookingRequest(aid)) },
                    onBack = { navController.popBackStack() }
                )
            }

            composable(NiloraRoutes.BOOKING_REQUEST) { backStackEntry ->
                val actId = backStackEntry.arguments?.getString("activityId").orEmpty()
                val activity = actList.firstOrNull { it.id == actId }
                BookingRequestScreen(
                    strings = strings,
                    activity = activity,
                    onSubmitBooking = { date, participants, notes ->
                        if (activity != null) {
                            mainVm.createBookingRequest(activity, date, participants, notes) { newId ->
                                navController.navigate(NiloraRoutes.bookingDetails(newId)) {
                                    popUpTo(NiloraRoutes.ACTIVITY_DETAILS)
                                }
                            }
                        }
                    },
                    onBack = { navController.popBackStack() }
                )
            }

            composable(NiloraRoutes.BOOKING_DETAILS) { backStackEntry ->
                val bookingId = backStackEntry.arguments?.getString("bookingId").orEmpty()
                val tList = (travelerBookingsState as? UiState.Success)?.data.orEmpty()
                val pList = (providerBookingsState as? UiState.Success)?.data.orEmpty()
                val booking = (tList + pList).firstOrNull { it.id == bookingId }
                BookingDetailsScreen(
                    booking = booking,
                    currentUserId = mainVm.currentUserId,
                    onUpdateStatus = { newStatus, reason ->
                        mainVm.updateBookingStatus(bookingId, newStatus, reason)
                    },
                    onBack = { navController.popBackStack() }
                )
            }

            composable(NiloraRoutes.MY_JOURNEY) {
                MyJourneyScreen(
                    favoritesState = favoritesState,
                    travelerBookingsState = travelerBookingsState,
                    providerBookingsState = providerBookingsState,
                    notesState = notesState,
                    roomsState = myRoomsState,
                    onOpenSavedPlaces = { navController.navigate(NiloraRoutes.SAVED_PLACES) },
                    onOpenBooking = { bid -> navController.navigate(NiloraRoutes.bookingDetails(bid)) },
                    onSaveNote = mainVm::saveTravelNote,
                    onDeleteNote = mainVm::deleteTravelNote,
                    onOpenProfile = { navController.navigate(NiloraRoutes.USER_PROFILE) },
                    onOpenProviderRegistration = { navController.navigate(NiloraRoutes.PROVIDER_REGISTRATION) }
                )
            }

            composable(NiloraRoutes.SAVED_PLACES) {
                SavedPlacesScreen(
                    favoritesState = favoritesState,
                    onRemoveFavorite = { fav ->
                        mainVm.toggleFavorite(fav.targetId, fav.targetType, fav.title, fav.cityName, fav.category, true)
                    },
                    onOpenDestination = { id -> navController.navigate(NiloraRoutes.destinationDetails(id)) },
                    onOpenActivity = { id -> navController.navigate(NiloraRoutes.activityDetails(id)) },
                    onBack = { navController.popBackStack() }
                )
            }

            composable(NiloraRoutes.USER_PROFILE) {
                UserProfileScreen(
                    userProfile = uiMeta.userProfile,
                    privateInfo = uiMeta.privateInfo,
                    authEmail = userEmail,
                    onSaveProfile = { name, mode, langCode, country, photo, bio, consent, retain, phone, emergency, city, nativeLang, spoken, learning, interests, visibility, exchange, dialCode ->
                        mainVm.saveProfile(
                            displayName = name,
                            accountMode = mode,
                            preferredLanguage = langCode,
                            country = country,
                            photoUri = photo,
                            bio = bio,
                            privacyConsent = consent,
                            retainHistory = retain,
                            phone = phone,
                            emergencyContact = emergency,
                            city = city,
                            nativeLanguage = nativeLang,
                            spokenLanguages = spoken,
                            learningLanguages = learning,
                            interests = interests,
                            profileVisibility = visibility,
                            openToLanguageExchange = exchange,
                            phoneCountryCode = dialCode
                        )
                    },
                    onSignOut = onSignOut,
                    onDeleteAccount = {
                        mainVm.deleteAccountData { onSignOut() }
                    },
                    onBack = { navController.popBackStack() }
                )
            }

            composable(NiloraRoutes.PROFILE_HUB) {
                ProfileHubScreen(
                    userProfile = uiMeta.userProfile,
                    authEmail = userEmail,
                    isAdmin = uiMeta.isAdmin,
                    unreadNotificationsCount = unreadCount,
                    pendingRequestsCount = pendingReqCount,
                    onEditProfile = { navController.navigate(NiloraRoutes.USER_PROFILE) },
                    onOpenMyJourney = { navController.navigate(NiloraRoutes.MY_JOURNEY) },
                    onOpenSavedPlaces = { navController.navigate(NiloraRoutes.SAVED_PLACES) },
                    onOpenConnectionRequests = { navController.navigate(NiloraRoutes.CONNECTION_REQUESTS) },
                    onOpenBlockedUsers = { navController.navigate(NiloraRoutes.BLOCKED_USERS) },
                    onOpenNotifications = { navController.navigate(NiloraRoutes.NOTIFICATIONS) },
                    onOpenSettings = { navController.navigate(NiloraRoutes.SETTINGS) },
                    onOpenAbout = { navController.navigate(NiloraRoutes.ABOUT_NILORA) },
                    onOpenAdmin = { navController.navigate(NiloraRoutes.ADMIN_DASHBOARD) },
                    onSignOut = onSignOut,
                    onOpenNiloraMemory = { navController.navigate(NiloraRoutes.NILORA_MEMORY) },
                    onOpenWorldPostcards = { navController.navigate(NiloraRoutes.WORLD_POSTCARDS) },
                    onOpenCultureSwaps = { navController.navigate(NiloraRoutes.CULTURE_SWAPS) },
                    onOpenWorldQuests = { navController.navigate(NiloraRoutes.WORLD_QUESTS) },
                    spokenLanguageCode = prefs.spokenLanguageCode,
                    targetTranslationCode = prefs.targetTranslationLanguageCode,
                    subtitleDisplayMode = SubtitleDisplayMode.fromKey(prefs.subtitleDisplayMode),
                    preferOriginalAudio = prefs.preferOriginalAudio,
                    personalizationEnabled = prefs.personalizationEnabled,
                    onUpdateLanguageIdentity = { spoken, target, subMode, preferOrig ->
                        scope.launch {
                            prefsRepo.setLanguageIdentity(
                                interfaceLanguageCode = prefs.languageCode,
                                spokenLanguageCode = spoken,
                                targetTranslationLanguageCode = target,
                                preferEgyptianColloquial = (target == "ar-EG" || spoken == "ar-EG")
                            )
                            prefsRepo.setSubtitleDisplayMode(subMode.key)
                            prefsRepo.setPreferOriginalAudio(preferOrig)
                        }
                    },
                    onTogglePersonalization = { enabled -> scope.launch { prefsRepo.setPersonalizationEnabled(enabled) } },
                    onOpenLiveHub = { navigateTopTab(NiloraRoutes.REALTIME_TRANSLATOR) }
                )
            }

            composable(NiloraRoutes.MESSAGES) {
                GlobalMessagesInboxScreen(
                    currentUserId = mainVm.currentUserId,
                    conversationsState = directConversationsState,
                    blockedState = blockedUsersState,
                    requestsState = connectionRequestsState,
                    onOpenConversation = { cid ->
                        navController.navigate(NiloraRoutes.directChat(cid))
                    },
                    onOpenDiscover = { navigateTopTab(NiloraRoutes.DISCOVER) },
                    onOpenRequests = { navController.navigate(NiloraRoutes.CONNECTION_REQUESTS) },
                    onOpenVoiceTranslator = { navigateTopTab(NiloraRoutes.REALTIME_TRANSLATOR) },
                    onOpenGroupsHub = { navigateTopTab(NiloraRoutes.GROUPS) }
                )
            }

            composable(NiloraRoutes.DIRECT_CHAT) { backStackEntry ->
                val cid = backStackEntry.arguments?.getString("conversationId").orEmpty()
                LaunchedEffect(cid) {
                    if (cid.isNotBlank()) communityVm.openDirectConversation(cid)
                }
                DirectChatScreen(
                    currentUserId = mainVm.currentUserId,
                    state = directChatState,
                    hasMicPermission = communityVm.hasMicPermission(),
                    onSelectSourceLanguage = communityVm::setDirectSourceLanguage,
                    onSelectTargetTranslationLanguage = communityVm::setDirectTargetTranslationLanguage,
                    onSendText = communityVm::sendDirectTextMessage,
                    onTranslateMessage = communityVm::translateDirectMessageOnDemand,
                    onStartVoiceRecording = communityVm::startVoiceNoteRecording,
                    onStopVoiceRecording = communityVm::stopVoiceNoteRecording,
                    onDiscardVoiceRecording = communityVm::discardRecordedVoiceClip,
                    onSendVoiceRecording = communityVm::sendRecordedVoiceNote,
                    onDictateMessage = communityVm::dictateMessageIntoComposer,
                    onTogglePlayVoiceClip = communityVm::toggleVoiceClipPlayback,
                    onSpeakText = communityVm::speakMessageText,
                    onBlockPartner = { uid, name ->
                        communityVm.blockUser(uid, name)
                        navController.popBackStack()
                    },
                    onReportPartner = { tid, reason, details ->
                        communityVm.reportContent("MESSAGE", tid, reason, details)
                    },
                    onBack = { navController.popBackStack() }
                )
            }

            composable(NiloraRoutes.DISCOVER) {
                GlobalCommunityScreen(
                    currentUserId = mainVm.currentUserId,
                    profilesState = publicProfilesState,
                    blockedState = blockedUsersState,
                    requestsState = connectionRequestsState,
                    filters = communityFilters,
                    onSearchChange = communityVm::updateSearchQuery,
                    onLanguageFilterChange = communityVm::updateLanguageFilter,
                    onCountryFilterChange = communityVm::updateCountryFilter,
                    onInterestFilterChange = communityVm::updateInterestFilter,
                    onToggleLanguageExchangeOnly = communityVm::toggleLanguageExchangeFilter,
                    onSendConnectionRequest = communityVm::sendConnectionRequest,
                    onStartDirectChat = { partner ->
                        communityVm.startDirectChatWith(partner) { cid ->
                            navController.navigate(NiloraRoutes.directChat(cid))
                        }
                    },
                    onBlockUser = { uid, name -> communityVm.blockUser(uid, name) },
                    onReportUser = { uid, reason, details -> communityVm.reportContent("USER", uid, reason, details) },
                    onOpenConnectionRequests = { navController.navigate(NiloraRoutes.CONNECTION_REQUESTS) },
                    onOpenBlockedUsers = { navController.navigate(NiloraRoutes.BLOCKED_USERS) },
                    onOpenExplorePlaces = { navigateTopTab(NiloraRoutes.EXPLORE) },
                    onOpenMap = { navController.navigate(NiloraRoutes.INTERACTIVE_MAP) },
                    onOpenMessagesHub = { navigateTopTab(NiloraRoutes.MESSAGES) },
                    onOpenGroupsHub = { navigateTopTab(NiloraRoutes.GROUPS) }
                )
            }

            composable(NiloraRoutes.CONNECTION_REQUESTS) {
                ConnectionRequestsScreen(
                    currentUserId = mainVm.currentUserId,
                    requestsState = connectionRequestsState,
                    onRespond = { req, status ->
                        communityVm.respondToConnectionRequest(req, status) { convId ->
                            navController.navigate(NiloraRoutes.directChat(convId))
                        }
                    },
                    onBack = { navController.popBackStack() }
                )
            }

            composable(NiloraRoutes.BLOCKED_USERS) {
                BlockedUsersScreen(
                    blockedState = blockedUsersState,
                    onUnblock = communityVm::unblockUser,
                    onBack = { navController.popBackStack() }
                )
            }

            composable(NiloraRoutes.GROUPS) {
                GroupsHubScreen(
                    currentUserId = mainVm.currentUserId,
                    myGroupsState = myGroupsState,
                    discoverGroupsState = discoverableGroupsState,
                    onCreateGroup = { name, desc, langs, policy, autoTr ->
                        communityVm.createGroup(name, desc, langs, policy, autoTr) { gid ->
                            navController.navigate(NiloraRoutes.groupRoom(gid))
                        }
                    },
                    onEnterGroup = { gid -> navController.navigate(NiloraRoutes.groupRoom(gid)) },
                    onJoinOrRequestGroup = { grp, msg ->
                        communityVm.joinOrRequestGroup(grp, msg) { gid ->
                            navController.navigate(NiloraRoutes.groupRoom(gid))
                        }
                    },
                    pendingRequestsCount = pendingReqCount,
                    onOpenMessagesHub = { navigateTopTab(NiloraRoutes.MESSAGES) },
                    onOpenDiscoverHub = { navigateTopTab(NiloraRoutes.DISCOVER) }
                )
            }

            composable(NiloraRoutes.GROUP_ROOM) { backStackEntry ->
                val gid = backStackEntry.arguments?.getString("groupId").orEmpty()
                LaunchedEffect(gid) {
                    if (gid.isNotBlank()) communityVm.openGroupRoom(gid)
                }
                GroupRoomScreen(
                    currentUserId = mainVm.currentUserId,
                    state = groupRoomState,
                    publicProfilesState = publicProfilesState,
                    onSelectSourceLanguage = communityVm::setGroupSourceLanguage,
                    onSelectTargetLanguage = communityVm::setGroupTargetLanguage,
                    onSendMessage = communityVm::sendGroupMessage,
                    onTranslateMessage = communityVm::translateGroupMessageOnDemand,
                    onSpeakText = communityVm::speakMessageText,
                    onReviewJoinRequest = communityVm::reviewGroupJoinRequest,
                    onInviteMember = communityVm::inviteMemberToCurrentGroup,
                    onPromoteModerator = communityVm::promoteGroupModerator,
                    onRemoveOrBanMember = communityVm::removeOrBanGroupMember,
                    onDeleteMessage = communityVm::deleteGroupMessage,
                    onLeaveGroup = {
                        communityVm.leaveCurrentGroup { navController.popBackStack() }
                    },
                    onReportGroup = { type, id, reason ->
                        communityVm.reportContent(type, id, reason, "")
                    },
                    onBack = { navController.popBackStack() },
                    onSelectSubtitleMode = { mode ->
                        communityVm.setGroupSubtitleMode(mode)
                        scope.launch { prefsRepo.setSubtitleDisplayMode(mode.key) }
                    },
                    onToggleAutoSpeakIncoming = communityVm::toggleGroupAutoSpeak,
                    onDictateVoice = communityVm::dictateMessageIntoGroup,
                    onClearVoiceDraft = communityVm::clearGroupVoiceDraft
                )
            }

            composable(NiloraRoutes.REGISTRATION) {
                RegistrationScreen(
                    existingProfile = uiMeta.userProfile,
                    defaultLanguage = language,
                    initialEmail = userEmail,
                    initialName = userDisplayName,
                    onSaveRegistration = { name, mode, langCode, country, bio, consent, retain, phone, city, nativeLang, exchange, dialCode ->
                        scope.launch { prefsRepo.setLanguage(langCode) }
                        mainVm.saveProfile(
                            displayName = name,
                            accountMode = mode,
                            preferredLanguage = langCode,
                            country = country,
                            photoUri = "",
                            bio = bio,
                            privacyConsent = consent,
                            retainHistory = retain,
                            phone = phone,
                            emergencyContact = "",
                            city = city,
                            nativeLanguage = nativeLang,
                            spokenLanguages = listOf(nativeLang),
                            learningLanguages = emptyList(),
                            interests = emptyList(),
                            profileVisibility = "PUBLIC",
                            openToLanguageExchange = exchange,
                            phoneCountryCode = dialCode
                        ) {
                            navController.popBackStack()
                        }
                    },
                    onOpenPrivacyPolicy = { navController.navigate(NiloraRoutes.PRIVACY_POLICY) },
                    onBack = { navController.popBackStack() }
                )
            }

            composable(NiloraRoutes.PROVIDER_PROFILE) { backStackEntry ->
                val pid = backStackEntry.arguments?.getString("providerId").orEmpty()
                val provider = provList.firstOrNull { it.id == pid || it.userId == pid }
                val pActs = actList.filter { it.providerId == pid }
                ProviderProfileScreen(
                    provider = provider,
                    activities = pActs,
                    onOpenActivity = { id -> navController.navigate(NiloraRoutes.activityDetails(id)) },
                    onReportProvider = { reason -> mainVm.submitReport("PROVIDER", pid, reason, "") },
                    onBack = { navController.popBackStack() }
                )
            }

            composable(NiloraRoutes.PROVIDER_REGISTRATION) {
                val existing = (myProviderState as? UiState.Success)?.data
                ProviderRegistrationScreen(
                    existingApplication = existing,
                    defaultName = uiMeta.userProfile?.displayName?.ifBlank { userDisplayName } ?: userDisplayName,
                    currentUserId = mainVm.currentUserId,
                    onSubmitApplication = mainVm::submitProviderApplication,
                    onBack = { navController.popBackStack() }
                )
            }

            composable(NiloraRoutes.NOTIFICATIONS) {
                NotificationsScreen(
                    notificationsState = notificationsState,
                    onMarkRead = mainVm::markNotificationRead,
                    onBack = { navController.popBackStack() }
                )
            }

            composable(NiloraRoutes.SETTINGS) {
                SettingsScreen(
                    strings = strings,
                    currentLanguage = language,
                    darkThemeOverride = prefs.darkThemeOverride,
                    retainTranscripts = prefs.retainTranslationTranscripts,
                    isAdmin = uiMeta.isAdmin,
                    onSelectLanguage = { lang -> scope.launch { prefsRepo.setLanguage(lang.code) } },
                    onSelectThemeMode = { mode -> scope.launch { prefsRepo.setDarkThemeOverride(mode) } },
                    onToggleRetainTranscripts = { retain -> scope.launch { prefsRepo.setRetainTranscripts(retain) } },
                    onOpenProfile = { navController.navigate(NiloraRoutes.USER_PROFILE) },
                    onOpenProviderRegistration = { navController.navigate(NiloraRoutes.PROVIDER_REGISTRATION) },
                    onOpenPrivacyPolicy = { navController.navigate(NiloraRoutes.PRIVACY_POLICY) },
                    onOpenTerms = { navController.navigate(NiloraRoutes.TERMS) },
                    onOpenAbout = { navController.navigate(NiloraRoutes.ABOUT_NILORA) },
                    onOpenAdminDashboard = { navController.navigate(NiloraRoutes.ADMIN_DASHBOARD) },
                    onSignOut = onSignOut,
                    onBack = { navController.popBackStack() },
                    onOpenCapabilities = { navController.navigate(NiloraRoutes.PLATFORM_CAPABILITIES) }
                )
            }

            composable(NiloraRoutes.LANGUAGE_SELECTION) {
                LanguageSelectionScreen(
                    currentLanguage = language,
                    strings = strings,
                    showBack = true,
                    onBack = { navController.popBackStack() },
                    onSelectLanguage = { lang -> scope.launch { prefsRepo.setLanguage(lang.code) } },
                    onContinue = { navController.popBackStack() }
                )
            }

            composable(NiloraRoutes.PRIVACY_POLICY) {
                PrivacyPolicyScreen(strings = strings, onBack = { navController.popBackStack() })
            }

            composable(NiloraRoutes.TERMS) {
                TermsAndConditionsScreen(strings = strings, onBack = { navController.popBackStack() })
            }

            composable(NiloraRoutes.ABOUT_NILORA) {
                AboutNiloraScreen(
                    strings = strings,
                    onOpenPrivacyPolicy = { navController.navigate(NiloraRoutes.PRIVACY_POLICY) },
                    onOpenTerms = { navController.navigate(NiloraRoutes.TERMS) },
                    onBack = { navController.popBackStack() },
                    onOpenCapabilities = { navController.navigate(NiloraRoutes.PLATFORM_CAPABILITIES) }
                )
            }

            composable(NiloraRoutes.ADMIN_DASHBOARD) {
                val adminDests by if (uiMeta.isAdmin) mainVm.adminDestinationsFlow.collectAsStateWithLifecycle() else remember { mutableStateOf(UiState.Success(emptyList())) }
                val adminActs by if (uiMeta.isAdmin) mainVm.adminActivitiesFlow.collectAsStateWithLifecycle() else remember { mutableStateOf(UiState.Success(emptyList())) }
                val adminProvs by if (uiMeta.isAdmin) mainVm.adminProvidersFlow.collectAsStateWithLifecycle() else remember { mutableStateOf(UiState.Success(emptyList())) }
                val adminBooks by if (uiMeta.isAdmin) mainVm.adminBookingsFlow.collectAsStateWithLifecycle() else remember { mutableStateOf(UiState.Success(emptyList())) }
                val adminReports by if (uiMeta.isAdmin) mainVm.adminReportsFlow.collectAsStateWithLifecycle() else remember { mutableStateOf(UiState.Success(emptyList())) }
                val adminLogs by if (uiMeta.isAdmin) mainVm.auditLogsFlow.collectAsStateWithLifecycle() else remember { mutableStateOf(UiState.Success(emptyList())) }

                AdminDashboardScreen(
                    isAdmin = uiMeta.isAdmin,
                    currentUserId = mainVm.currentUserId,
                    isGoogleMapsKeyConfigured = mainVm.isGoogleMapsKeyConfigured,
                    isOnline = mainVm.isOnline,
                    destinationsState = adminDests,
                    activitiesState = adminActs,
                    providersState = adminProvs,
                    bookingsState = adminBooks,
                    reportsState = adminReports,
                    auditLogsState = adminLogs,
                    onPublishVerifiedDestination = { d -> mainVm.submitDestination(d, asAdminApproved = true) },
                    onPublishVerifiedActivity = { a -> mainVm.submitActivity(a, asAdminApproved = true) },
                    onModerateDestination = mainVm::moderateDestination,
                    onModerateActivity = mainVm::moderateActivity,
                    onModerateProvider = mainVm::moderateProvider,
                    onResolveReport = mainVm::resolveReport,
                    onBack = { navController.popBackStack() },
                    realityEventsState = eventsState,
                    teamActivitiesState = teamActivitiesState,
                    onModerateRealityEvent = realityVm::moderateRealityEventVerification,
                    onModerateImpactActivity = realityVm::moderateImpactVerification
                )
            }

            // --- NEXT-GEN HUMAN CONNECTION ROUTES ---

            composable(NiloraRoutes.WORLD_POSTCARDS) {
                WorldPostcardsDiscoveryScreen(
                    postcardsState = postcardsState,
                    currentUserId = mainVm.currentUserId,
                    onCreatePostcard = {
                        experienceVm.resetMediaDraft()
                        navController.navigate(NiloraRoutes.POSTCARD_CREATE)
                    },
                    onOpenPostcard = { id -> navController.navigate(NiloraRoutes.postcardDetail(id)) },
                    onBack = { navController.popBackStack() }
                )
            }

            composable(NiloraRoutes.POSTCARD_CREATE) {
                CreateWorldPostcardScreen(
                    myProfile = uiMeta.userProfile,
                    mediaDraft = mediaDraft,
                    onPickPhoto = experienceVm::attachPhotoFromPicker,
                    onClearPhoto = experienceVm::clearPhotoAttachment,
                    onStartRecording = experienceVm::startVoiceRecording,
                    onStopRecording = experienceVm::stopVoiceRecording,
                    onClearRecording = experienceVm::clearRecordedClip,
                    onPlayPreviewClip = { b64 -> experienceVm.toggleAudioPlayback("draft_preview", b64) },
                    onDictateText = experienceVm::dictateIntoDraft,
                    onSubmitPostcard = { category, title, storyText, expr, question, country, city, langCode, allowReplies, allowConnect, vis ->
                        experienceVm.createWorldPostcard(
                            category = category,
                            title = title,
                            storyText = storyText,
                            localExpression = expr,
                            questionPrompt = question,
                            country = country,
                            city = city,
                            languageCode = langCode,
                            allowReplies = allowReplies,
                            allowPrivateConnect = allowConnect,
                            visibility = vis
                        ) { newId ->
                            navController.navigate(NiloraRoutes.postcardDetail(newId)) {
                                popUpTo(NiloraRoutes.POSTCARD_CREATE) { inclusive = true }
                            }
                        }
                    },
                    onBack = { navController.popBackStack() }
                )
            }

            composable(NiloraRoutes.POSTCARD_DETAIL) { backStackEntry ->
                val postcardId = backStackEntry.arguments?.getString("postcardId").orEmpty()
                LaunchedEffect(postcardId) {
                    if (postcardId.isNotBlank()) experienceVm.openPostcardDetail(postcardId)
                }
                WorldPostcardDetailScreen(
                    detailState = postcardDetailState,
                    mediaDraft = mediaDraft,
                    currentUserId = mainVm.currentUserId,
                    myProfile = uiMeta.userProfile,
                    onSetTargetLanguage = experienceVm::setPostcardTargetLanguage,
                    onTranslatePostcard = { card -> experienceVm.translatePostcard(card) },
                    onTranslateContribution = { pid, contrib -> experienceVm.translatePostcardContribution(pid, contrib) },
                    onPlayAudio = experienceVm::toggleAudioPlayback,
                    onSpeakText = experienceVm::speakText,
                    onPickPhoto = experienceVm::attachPhotoFromPicker,
                    onClearPhoto = experienceVm::clearPhotoAttachment,
                    onStartRecording = experienceVm::startVoiceRecording,
                    onStopRecording = experienceVm::stopVoiceRecording,
                    onClearRecording = experienceVm::clearRecordedClip,
                    onDictateText = experienceVm::dictateIntoDraft,
                    onSubmitContribution = { card, text, expr, country, city, langCode, allowConn ->
                        experienceVm.addPostcardContribution(card, text, expr, country, city, langCode, allowConn)
                    },
                    onSaveToMemory = { card, contribs, vis ->
                        val summary = buildString {
                            append(card.storyText)
                            if (contribs.isNotEmpty()) {
                                append("\n\nReplies (${contribs.size}): ")
                                append(contribs.take(3).joinToString(" | ") { "${it.country}: ${it.responseText}" })
                            }
                        }
                        experienceVm.saveExperienceMemory(
                            memoryType = MemoryType.WORLD_POSTCARD,
                            sourceId = card.id,
                            title = card.title,
                            summaryText = summary,
                            originalLanguage = card.languageCode,
                            translations = card.translations,
                            countries = card.countriesChain,
                            partnerIds = card.participantIds.filter { it != mainVm.currentUserId },
                            partnerNames = contribs.map { it.authorName }.distinct(),
                            placeLabel = listOfNotNull(card.city.takeIf { it.isNotBlank() }, card.country).joinToString(", "),
                            photoBase64 = card.photoBase64,
                            audioBase64 = "",
                            audioDurationMs = 0L,
                            retainAudioConsent = false,
                            visibility = vis
                        )
                    },
                    onContinuePrivately = { pid, pname, pcountry, plang, title ->
                        experienceVm.continuePrivatelyWith(pid, pname, pcountry, plang, title) { cid ->
                            navController.navigate(NiloraRoutes.directChat(cid))
                        }
                    },
                    onBlockUser = { uidToBlock, name ->
                        experienceVm.blockParticipant(uidToBlock, name)
                        navController.popBackStack()
                    },
                    onReportPostcard = { targetType, targetId, reason ->
                        experienceVm.reportExperienceContent(targetType, targetId, reason)
                    },
                    onBack = { navController.popBackStack() }
                )
            }

            composable(NiloraRoutes.CULTURE_SWAPS) {
                CultureSwapsListScreen(
                    swapsState = cultureSwapsState,
                    currentUserId = mainVm.currentUserId,
                    onCreateSwap = {
                        experienceVm.resetMediaDraft()
                        navController.navigate(NiloraRoutes.CULTURE_SWAP_CREATE)
                    },
                    onOpenSwap = { id -> navController.navigate(NiloraRoutes.cultureSwapDetail(id)) },
                    onBack = { navController.popBackStack() }
                )
            }

            composable(NiloraRoutes.CULTURE_SWAP_CREATE) {
                CreateCultureSwapScreen(
                    myProfile = uiMeta.userProfile,
                    mediaDraft = mediaDraft,
                    onPickPhoto = experienceVm::attachPhotoFromPicker,
                    onClearPhoto = experienceVm::clearPhotoAttachment,
                    onStartRecording = experienceVm::startVoiceRecording,
                    onStopRecording = experienceVm::stopVoiceRecording,
                    onClearRecording = experienceVm::clearRecordedClip,
                    onPlayPreviewClip = { b64 -> experienceVm.toggleAudioPlayback("draft_preview", b64) },
                    onDictateText = experienceVm::dictateIntoDraft,
                    onSubmitSwap = { cat, prompt, mode, maxP, resp, expr, langCode, allowConn ->
                        experienceVm.createCultureSwap(
                            promptCategory = cat,
                            promptText = prompt,
                            participationMode = mode,
                            maxParticipants = maxP,
                            initialResponseText = resp,
                            expressionSample = expr,
                            languageCode = langCode,
                            allowPrivateConnect = allowConn
                        ) { newId ->
                            navController.navigate(NiloraRoutes.cultureSwapDetail(newId)) {
                                popUpTo(NiloraRoutes.CULTURE_SWAP_CREATE) { inclusive = true }
                            }
                        }
                    },
                    onBack = { navController.popBackStack() }
                )
            }

            composable(NiloraRoutes.CULTURE_SWAP_DETAIL) { backStackEntry ->
                val swapId = backStackEntry.arguments?.getString("swapId").orEmpty()
                LaunchedEffect(swapId) {
                    if (swapId.isNotBlank()) experienceVm.openCultureSwapDetail(swapId)
                }
                CultureSwapDetailScreen(
                    detailState = swapDetailState,
                    mediaDraft = mediaDraft,
                    currentUserId = mainVm.currentUserId,
                    myProfile = uiMeta.userProfile,
                    onSetTargetLanguage = experienceVm::setSwapTargetLanguage,
                    onTranslateEntry = { sid, entry -> experienceVm.translateSwapEntry(sid, entry) },
                    onPlayAudio = experienceVm::toggleAudioPlayback,
                    onSpeakText = experienceVm::speakText,
                    onPickPhoto = experienceVm::attachPhotoFromPicker,
                    onClearPhoto = experienceVm::clearPhotoAttachment,
                    onStartRecording = experienceVm::startVoiceRecording,
                    onStopRecording = experienceVm::stopVoiceRecording,
                    onClearRecording = experienceVm::clearRecordedClip,
                    onDictateText = experienceVm::dictateIntoDraft,
                    onSubmitEntry = { swap, content, expr, country, langCode ->
                        experienceVm.addCultureSwapEntry(swap, content, expr, country, langCode)
                    },
                    onSaveToMemory = { swap, entries, vis ->
                        val summary = buildString {
                            append(swap.promptText)
                            if (entries.isNotEmpty()) {
                                append("\n\nExchange: ")
                                append(entries.joinToString(" | ") { "${it.authorName} (${it.country}): ${it.contentText}" })
                            }
                        }
                        experienceVm.saveExperienceMemory(
                            memoryType = MemoryType.CULTURE_SWAP,
                            sourceId = swap.id,
                            title = SwapPromptCategory.fromKey(swap.promptCategory).titleEn,
                            summaryText = summary,
                            originalLanguage = swap.creatorLanguage,
                            translations = emptyMap(),
                            countries = swap.participantCountries.values.distinct(),
                            partnerIds = swap.participantIds.filter { it != mainVm.currentUserId },
                            partnerNames = swap.participantNames.filterKeys { it != mainVm.currentUserId }.values.toList(),
                            placeLabel = swap.participantCountries.values.distinct().joinToString(" <-> "),
                            photoBase64 = entries.firstOrNull { it.photoBase64.isNotBlank() }?.photoBase64.orEmpty(),
                            audioBase64 = "",
                            audioDurationMs = 0L,
                            retainAudioConsent = false,
                            visibility = vis
                        )
                    },
                    onContinuePrivately = { pid, pname, pcountry, plang, title ->
                        experienceVm.continuePrivatelyWith(pid, pname, pcountry, plang, title) { cid ->
                            navController.navigate(NiloraRoutes.directChat(cid))
                        }
                    },
                    onBlockUser = { uidToBlock, name ->
                        experienceVm.blockParticipant(uidToBlock, name)
                        navController.popBackStack()
                    },
                    onReportSwap = { id, reason ->
                        experienceVm.reportExperienceContent("CULTURE_SWAP", id, reason)
                    },
                    onBack = { navController.popBackStack() }
                )
            }

            composable(NiloraRoutes.WORLD_QUESTS) {
                WorldQuestsListScreen(
                    questsState = worldQuestsState,
                    currentUserId = mainVm.currentUserId,
                    onCreateQuest = { navController.navigate(NiloraRoutes.WORLD_QUEST_CREATE) },
                    onOpenQuest = { id -> navController.navigate(NiloraRoutes.worldQuestDetail(id)) },
                    onBack = { navController.popBackStack() }
                )
            }

            composable(NiloraRoutes.WORLD_QUEST_CREATE) {
                CreateWorldQuestScreen(
                    onSubmitQuest = { mType, title, objective, goal ->
                        experienceVm.createWorldQuest(mType, title, objective, goal) { newId ->
                            navController.navigate(NiloraRoutes.worldQuestDetail(newId)) {
                                popUpTo(NiloraRoutes.WORLD_QUEST_CREATE) { inclusive = true }
                            }
                        }
                    },
                    onBack = { navController.popBackStack() }
                )
            }

            composable(NiloraRoutes.WORLD_QUEST_DETAIL) { backStackEntry ->
                val questId = backStackEntry.arguments?.getString("questId").orEmpty()
                LaunchedEffect(questId) {
                    if (questId.isNotBlank()) experienceVm.openWorldQuestDetail(questId)
                }
                WorldQuestDetailScreen(
                    detailState = questDetailState,
                    mediaDraft = mediaDraft,
                    currentUserId = mainVm.currentUserId,
                    myProfile = uiMeta.userProfile,
                    onSetTargetLanguage = experienceVm::setQuestTargetLanguage,
                    onTranslateContribution = { qid, c -> experienceVm.translateQuestContribution(qid, c) },
                    onPlayAudio = experienceVm::toggleAudioPlayback,
                    onSpeakText = experienceVm::speakText,
                    onPickPhoto = experienceVm::attachPhotoFromPicker,
                    onClearPhoto = experienceVm::clearPhotoAttachment,
                    onStartRecording = experienceVm::startVoiceRecording,
                    onStopRecording = experienceVm::stopVoiceRecording,
                    onClearRecording = experienceVm::clearRecordedClip,
                    onDictateText = experienceVm::dictateIntoDraft,
                    onSubmitContribution = { quest, text, country, langCode ->
                        experienceVm.addWorldQuestContribution(quest, text, country, langCode)
                    },
                    onSaveSharedMemory = { quest, contribs, vis ->
                        val summary = buildString {
                            append("${quest.objective} (${quest.contributionsCount}/${quest.targetGoal} contributions)")
                            if (contribs.isNotEmpty()) {
                                append("\n\n")
                                append(contribs.take(4).joinToString(" | ") { "${it.country}: ${it.contentText}" })
                            }
                        }
                        experienceVm.saveExperienceMemory(
                            memoryType = MemoryType.WORLD_QUEST,
                            sourceId = quest.id,
                            title = quest.title,
                            summaryText = summary,
                            originalLanguage = contribs.firstOrNull()?.languageCode ?: "en",
                            translations = emptyMap(),
                            countries = quest.participantCountries,
                            partnerIds = quest.participantIds.filter { it != mainVm.currentUserId },
                            partnerNames = contribs.map { it.authorName }.distinct(),
                            placeLabel = quest.participantCountries.joinToString(", "),
                            photoBase64 = contribs.firstOrNull { it.photoBase64.isNotBlank() }?.photoBase64.orEmpty(),
                            audioBase64 = "",
                            audioDurationMs = 0L,
                            retainAudioConsent = false,
                            visibility = vis
                        )
                    },
                    onBlockUser = { uidToBlock, name ->
                        experienceVm.blockParticipant(uidToBlock, name)
                        navController.popBackStack()
                    },
                    onReportQuest = { id, reason ->
                        experienceVm.reportExperienceContent("WORLD_QUEST", id, reason)
                    },
                    onBack = { navController.popBackStack() }
                )
            }

            composable(NiloraRoutes.NILORA_MEMORY) {
                NiloraMemoryScreen(
                    memoriesState = memoriesState,
                    currentUserId = mainVm.currentUserId,
                    myProfile = uiMeta.userProfile,
                    mediaDraft = mediaDraft,
                    onPickPhoto = experienceVm::attachPhotoFromPicker,
                    onClearPhoto = experienceVm::clearPhotoAttachment,
                    onStartRecording = experienceVm::startVoiceRecording,
                    onStopRecording = experienceVm::stopVoiceRecording,
                    onClearRecording = experienceVm::clearRecordedClip,
                    onPlayAudio = experienceVm::toggleAudioPlayback,
                    onDictateText = experienceVm::dictateIntoDraft,
                    onCreatePersonalMemory = { title, summary, country, placeLabel, langCode, audioConsent, vis ->
                        experienceVm.createPersonalMemory(
                            title = title,
                            summaryText = summary,
                            country = country,
                            placeLabel = placeLabel,
                            languageCode = langCode,
                            retainAudioConsent = audioConsent,
                            visibility = vis
                        )
                    },
                    onUpdateVisibility = experienceVm::updateMemoryVisibility,
                    onRevokeAudio = experienceVm::revokeMemoryAudio,
                    onDeleteMemory = experienceVm::deleteMemory,
                    onBack = { navController.popBackStack() }
                )
            }

            // --- GLOBAL IMMERSIVE TRAVEL & SOCIAL ADVENTURE PLATFORM ROUTES ---

            composable(NiloraRoutes.ADVENTURE_DISCOVERY) {
                AdventureDiscoveryScreen(
                    strings = strings,
                    uiState = worldUi,
                    joinableSessionsCount = liveAdventureCount,
                    onSelectDestinationFilter = adventureVm::setDestinationFilter,
                    onSelectTypeFilter = adventureVm::setTypeFilter,
                    onSelectAdventure = adventureVm::selectAdventure,
                    onTriggerSurpriseMe = adventureVm::triggerSurpriseMe,
                    onDismissSurprise = adventureVm::clearSurpriseBanner,
                    onRefreshIntelligence = { adventureVm.refreshDestinationIntelligence() },
                    onEnterAdventure = { advId -> navController.navigate(NiloraRoutes.immersiveAdventure(advId)) },
                    onOpenMatchmaking = { navController.navigate(NiloraRoutes.ADVENTURE_MATCHMAKING) },
                    onOpenCrews = { navController.navigate(NiloraRoutes.NILORA_CREWS) },
                    onOpenCapabilities = { navController.navigate(NiloraRoutes.PLATFORM_CAPABILITIES) },
                    onOpenRealWorldPlan = { cityName ->
                        mainVm.setCityFilter(cityName)
                        navigateTopTab(NiloraRoutes.EXPLORE)
                    },
                    onBack = { navController.popBackStack() }
                )
            }

            composable(NiloraRoutes.IMMERSIVE_ADVENTURE) { backStackEntry ->
                val adventureId = backStackEntry.arguments?.getString("adventureId").orEmpty()
                LaunchedEffect(adventureId) {
                    if (adventureId.isNotBlank() && adventureId != worldUi.selectedAdventure.id) {
                        adventureVm.selectAdventure(AdventureCatalog.findById(adventureId))
                    }
                }
                ImmersiveAdventureScreen(
                    strings = strings,
                    uiState = worldUi,
                    currentUserId = mainVm.currentUserId,
                    myProfile = uiMeta.userProfile,
                    joinableSessionsState = joinableSessionsState,
                    onCameraChange = adventureVm::updateCamera,
                    onSelectPoi = adventureVm::discoverPoi,
                    onPointAtPoi = adventureVm::pointAtPoi,
                    onToggleAudioMute = adventureVm::toggleAudioMuted,
                    onStartSession = { mode -> adventureVm.startAdventureSession(mode) },
                    onJoinSession = { sess -> adventureVm.joinLiveSession(sess) },
                    onLeaveSession = adventureVm::leaveActiveSession,
                    onSendSessionChat = adventureVm::sendSessionTextMessage,
                    onDictateVoiceChat = adventureVm::dictateAndSendSessionVoice,
                    onTranslateSessionMessage = adventureVm::translateSessionMessage,
                    onSpeakText = adventureVm::speakMessage,
                    onSetTargetLang = adventureVm::setTargetTranslationLanguage,
                    onToggleSubtitles = adventureVm::toggleSubtitles,
                    onToggleSpeakTranslations = adventureVm::toggleSpeakTranslations,
                    onSaveMemory = adventureVm::saveAdventureToMemory,
                    onConnectCompanion = { pid, pname, pcountry, plang, title ->
                        adventureVm.sendCompanionConnectionRequest(pid, pname, pcountry, plang, title) { cid ->
                            navController.navigate(NiloraRoutes.directChat(cid))
                        }
                    },
                    onOpenMatchmaking = { navController.navigate(NiloraRoutes.ADVENTURE_MATCHMAKING) },
                    onOpenCrews = { navController.navigate(NiloraRoutes.NILORA_CREWS) },
                    onOpenRealTrip = { cityName ->
                        mainVm.setCityFilter(cityName)
                        navigateTopTab(NiloraRoutes.EXPLORE)
                    },
                    onBack = { navController.popBackStack() },
                    onBridgeToReality = { adv ->
                        realityVm.saveRealityBridgeItem(
                            adventure = adv,
                            targetType = "REAL_WORLD_MISSION",
                            targetId = "bridge_${adv.id}",
                            targetTitle = "Explore ${adv.destinationName} in person",
                            stage = RealityBridgeStage.PLANNED,
                            notes = "Bridged from 3D adventure"
                        )
                        navController.navigate(NiloraRoutes.REALITY_BRIDGE)
                    },
                    onOpenLiveTranslator = { navigateTopTab(NiloraRoutes.REALTIME_TRANSLATOR) },
                    onOpenCityEvents = { city ->
                        realityVm.selectCityHub(city)
                        navController.navigate(NiloraRoutes.REALITY_EVENTS)
                    }
                )
            }

            composable(NiloraRoutes.ADVENTURE_MATCHMAKING) {
                AdventureMatchmakingScreen(
                    strings = strings,
                    currentUserId = mainVm.currentUserId,
                    myProfile = uiMeta.userProfile,
                    selectedAdventure = worldUi.selectedAdventure,
                    matchProfilesState = matchProfilesState,
                    blockedUsersState = blockedUsersState,
                    onPublishAvailability = adventureVm::publishMatchAvailability,
                    onConnectWithMatch = { partner ->
                        adventureVm.sendCompanionConnectionRequest(
                            partnerId = partner.userId,
                            partnerName = partner.displayName,
                            partnerCountry = partner.country,
                            partnerLanguage = partner.preferredLanguage,
                            adventureTitle = partner.targetDestination
                        ) { cid ->
                            navController.navigate(NiloraRoutes.directChat(cid))
                        }
                    },
                    onBack = { navController.popBackStack() }
                )
            }

            composable(NiloraRoutes.NILORA_CREWS) {
                NiloraCrewsScreen(
                    strings = strings,
                    currentUserId = mainVm.currentUserId,
                    myProfile = uiMeta.userProfile,
                    myCrewsState = myCrewsState,
                    openCrewsState = openCrewsState,
                    onCreateCrew = { name, desc, dest, interests, advId, policy, voice ->
                        adventureVm.createCrew(
                            name = name,
                            description = desc,
                            focusDestination = dest,
                            sharedInterests = interests,
                            plannedAdventureId = advId,
                            historyTitle = null,
                            joinPolicy = policy,
                            voiceEnabled = voice
                        ) { crewId ->
                            navController.navigate(NiloraRoutes.crewDetail(crewId))
                        }
                    },
                    onJoinCrew = { crew ->
                        adventureVm.joinCrew(crew) {
                            navController.navigate(NiloraRoutes.crewDetail(crew.id))
                        }
                    },
                    onOpenCrewDetail = { crewId -> navController.navigate(NiloraRoutes.crewDetail(crewId)) },
                    onBack = { navController.popBackStack() }
                )
            }

            composable(NiloraRoutes.CREW_DETAIL) { backStackEntry ->
                val crewId = backStackEntry.arguments?.getString("crewId").orEmpty()
                LaunchedEffect(crewId) {
                    if (crewId.isNotBlank()) adventureVm.openCrewDetail(crewId)
                }
                val myLang = uiMeta.userProfile?.nativeLanguage?.ifBlank { "en" } ?: "en"
                CrewDetailScreen(
                    crew = selectedCrew,
                    messages = crewMessages,
                    myLanguageCode = myLang,
                    onSendMessage = { txt -> if (crewId.isNotBlank()) adventureVm.sendCrewMessage(crewId, txt) },
                    onTranslateMessage = { m, target -> if (crewId.isNotBlank()) adventureVm.translateCrewMessage(crewId, m, target) },
                    onPlanAdventure = adventureVm::planCrewAdventure,
                    onLaunchCrewAdventure = { adv, cid ->
                        adventureVm.selectAdventure(adv)
                        adventureVm.startAdventureSession(
                            mode = AdventureSessionMode.CREW_SESSION,
                            crewId = cid
                        ) {
                            navController.navigate(NiloraRoutes.immersiveAdventure(adv.id))
                        }
                    },
                    onBack = { navController.popBackStack() },
                    onOpenLiveTranslator = { navigateTopTab(NiloraRoutes.REALTIME_TRANSLATOR) },
                    onOpenRealityBridge = { navController.navigate(NiloraRoutes.REALITY_BRIDGE) }
                )
            }

            // --- PHASE 2: REALITY CONNECTED UNIVERSE, GLOBAL ACTIVITIES & SOCIAL PLAY ---

            composable(NiloraRoutes.REALITY_HUB) {
                RealityUniverseHubScreen(
                    strings = strings,
                    currentUserId = mainVm.currentUserId,
                    filters = realityFilters,
                    localGps = localGps,
                    gpsMessage = gpsMessage,
                    activitiesState = teamActivitiesState,
                    blockedIds = blockedIds,
                    onSelectCity = realityVm::selectCityHub,
                    onSetDistanceKm = realityVm::setMaxDistanceKm,
                    onRequestGps = realityVm::requestLocalGpsForDiscovery,
                    onSelectDomain = realityVm::setActivityDomainFilter,
                    onSelectSubCategory = realityVm::setActivitySubCategoryFilter,
                    onSelectSkill = realityVm::setActivitySkillFilter,
                    onToggleOpenSlotsOnly = realityVm::toggleOpenSlotsOnly,
                    onOpenEvents = { navController.navigate(NiloraRoutes.REALITY_EVENTS) },
                    onOpenMissions = { navController.navigate(NiloraRoutes.REAL_WORLD_MISSIONS) },
                    onOpenChallenges = { navController.navigate(NiloraRoutes.GLOBAL_CHALLENGES) },
                    onOpenRealityBridge = { navController.navigate(NiloraRoutes.REALITY_BRIDGE) },
                    onOpenWorldPulseMap = { navController.navigate(NiloraRoutes.INTERACTIVE_MAP) },
                    onCreateTeamActivity = { domain, sub, title, desc, city, country, spot, dateLbl, skill, equip, safety, policy, maxP, impactOrg ->
                        realityVm.createTeamActivity(
                            domain = domain,
                            subCategory = sub,
                            title = title,
                            description = desc,
                            cityName = city,
                            countryName = country,
                            publicMeetingSpot = spot,
                            scheduledDateLabel = dateLbl,
                            skillLevel = skill,
                            equipmentNote = equip,
                            safetyRules = safety,
                            joinPolicy = policy,
                            maxParticipants = maxP,
                            impactOrganizationName = impactOrg
                        ) { newId ->
                            navController.navigate(NiloraRoutes.teamActivityDetail(newId))
                        }
                    },
                    onOpenActivityDetail = { id -> navController.navigate(NiloraRoutes.teamActivityDetail(id)) },
                    onBack = { navController.popBackStack() }
                )
            }

            composable(NiloraRoutes.REALITY_EVENTS) {
                RealityPulseEventsScreen(
                    strings = strings,
                    currentUserId = mainVm.currentUserId,
                    isAdmin = uiMeta.isAdmin,
                    filters = realityFilters,
                    localGps = localGps,
                    eventsState = eventsState,
                    externalEvents = externalEvents,
                    isLoadingExternal = isLoadingExternalEvents,
                    blockedIds = blockedIds,
                    onSelectCity = realityVm::selectCityHub,
                    onSelectCategory = realityVm::setEventCategoryFilter,
                    onSelectVerification = realityVm::setEventVerificationFilter,
                    onToggleFreeOnly = realityVm::toggleFreeVerifiedOnly,
                    onToggleUpcomingOnly = realityVm::toggleUpcomingOnly,
                    onRefreshExternal = realityVm::refreshExternalEventFeed,
                    onCreateEvent = { title, cat, desc, city, country, code, venue, dateIso, timeWindow, freeVerified, priceNote, organizer, orgType, url, conditions, safety, maxP, policy ->
                        realityVm.createRealityEvent(
                            title = title,
                            category = cat,
                            description = desc,
                            cityName = city,
                            countryName = country,
                            countryCode = code,
                            venueName = venue,
                            startDateIso = dateIso,
                            timeWindowLabel = timeWindow,
                            freeVerified = freeVerified,
                            priceNote = priceNote,
                            organizerName = organizer,
                            organizerType = orgType,
                            officialUrl = url,
                            conditionsNote = conditions,
                            safetyNote = safety,
                            maxParticipants = maxP,
                            joinPolicy = policy,
                            isAdmin = uiMeta.isAdmin
                        ) { newId ->
                            navController.navigate(NiloraRoutes.realityEventDetail(newId))
                        }
                    },
                    onOpenEvent = { id -> navController.navigate(NiloraRoutes.realityEventDetail(id)) },
                    onBack = { navController.popBackStack() }
                )
            }

            composable(NiloraRoutes.REALITY_EVENT_DETAIL) { backStackEntry ->
                val eventId = backStackEntry.arguments?.getString("eventId").orEmpty()
                val event = eventsList.firstOrNull { it.id == eventId }
                val isParticipant = event != null && mainVm.currentUserId in event.participantIds
                LaunchedEffect(eventId, isParticipant) {
                    if (eventId.isNotBlank()) realityVm.openEventDetail(eventId, isParticipant)
                }
                RealityEventDetailScreen(
                    event = event,
                    currentUserId = mainVm.currentUserId,
                    messages = eventMessages,
                    onJoinOrRequest = realityVm::joinOrRequestEvent,
                    onLeave = realityVm::leaveEvent,
                    onApproveRequest = realityVm::approveEventRequest,
                    onUpdateStatus = realityVm::updateEventStatus,
                    onSendMessage = realityVm::sendEventMessage,
                    onTranslateAndSpeak = realityVm::translateAndSpeakEventMessage,
                    onReport = { id, reason -> realityVm.reportRealityContent("REALITY_EVENT", id, reason) },
                    onBack = { navController.popBackStack() },
                    onOpenLiveTranslator = { navigateTopTab(NiloraRoutes.REALTIME_TRANSLATOR) },
                    onOpenMap = { navController.navigate(NiloraRoutes.INTERACTIVE_MAP) }
                )
            }

            composable(NiloraRoutes.TEAM_ACTIVITY_DETAIL) { backStackEntry ->
                val activityId = backStackEntry.arguments?.getString("activityId").orEmpty()
                val activity = teamList.firstOrNull { it.id == activityId }
                val isParticipant = activity != null && mainVm.currentUserId in activity.participantIds
                LaunchedEffect(activityId, isParticipant) {
                    if (activityId.isNotBlank()) realityVm.openActivityDetail(activityId, isParticipant)
                }
                TeamActivityDetailScreen(
                    activity = activity,
                    currentUserId = mainVm.currentUserId,
                    messages = activityMessages,
                    onJoinOrRequest = realityVm::joinOrRequestActivity,
                    onLeave = realityVm::leaveActivity,
                    onApproveRequest = realityVm::approveActivityRequest,
                    onUpdateStatus = realityVm::updateActivityStatus,
                    onSendMessage = realityVm::sendActivityMessage,
                    onTranslateAndSpeak = realityVm::translateAndSpeakActivityMessage,
                    onReport = { id, reason -> realityVm.reportRealityContent("TEAM_ACTIVITY", id, reason) },
                    onBlockOrganizer = { uidToBlock, name ->
                        realityVm.blockParticipant(uidToBlock, name)
                        navController.popBackStack()
                    },
                    onBack = { navController.popBackStack() },
                    onOpenLiveTranslator = { navigateTopTab(NiloraRoutes.REALTIME_TRANSLATOR) },
                    onOpenCrews = { navController.navigate(NiloraRoutes.NILORA_CREWS) }
                )
            }

            composable(NiloraRoutes.REAL_WORLD_MISSIONS) {
                RealWorldMissionsScreen(
                    strings = strings,
                    filters = realityFilters,
                    missionsState = missionsState,
                    selectedMissionSubmissions = missionSubmissions,
                    attachedPhotoBase64 = missionPhotoBase64,
                    blockedIds = blockedIds,
                    onSelectModeFilter = realityVm::setMissionModeFilter,
                    onLaunchTemplate = { tpl -> realityVm.launchMissionFromTemplate(tpl) },
                    onCreateCustomMission = { mode, title, obj, city, country, safety, steps ->
                        realityVm.createCustomMission(mode, title, obj, city, country, safety, steps)
                    },
                    onSelectMission = { mission -> realityVm.openMissionDetail(mission.id) },
                    onPickPhoto = realityVm::attachMissionPhoto,
                    onClearPhoto = realityVm::clearMissionPhoto,
                    onSubmitEvidence = realityVm::submitMissionEvidence,
                    onSaveMissionMemory = realityVm::saveMissionToNiloraMemory,
                    onBack = { navController.popBackStack() }
                )
            }

            composable(NiloraRoutes.GLOBAL_CHALLENGES) {
                GlobalChallengesScreen(
                    strings = strings,
                    filters = realityFilters,
                    challengesState = challengesState,
                    selectedChallengeEntries = challengeEntries,
                    blockedIds = blockedIds,
                    onSelectScope = realityVm::setChallengeScopeFilter,
                    onCreateChallenge = { scope, cat, title, desc, goal, city, country ->
                        realityVm.createChallenge(scope, cat, title, desc, goal, city, country)
                    },
                    onSelectChallenge = { ch -> realityVm.openChallengeDetail(ch.id) },
                    onSubmitEntry = realityVm::submitChallengeEntry,
                    onBack = { navController.popBackStack() }
                )
            }

            composable(NiloraRoutes.REALITY_BRIDGE) {
                RealityBridgeScreen(
                    strings = strings,
                    bridgeState = bridgeLinksState,
                    onCreateBridgePlan = realityVm::saveRealityBridgeItem,
                    onAdvanceStage = realityVm::advanceBridgeStage,
                    onDeleteBridge = realityVm::removeBridgeItem,
                    onBack = { navController.popBackStack() }
                )
            }

            composable(NiloraRoutes.PLATFORM_CAPABILITIES) {
                PlatformCapabilitiesScreen(
                    strings = strings,
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}

@Composable
fun NiloraResponsiveNavShell(
    showTopLevelNav: Boolean,
    currentRoute: String?,
    navItems: List<Triple<String, String, androidx.compose.ui.graphics.vector.ImageVector>>,
    secondaryRailItems: List<Triple<String, String, androidx.compose.ui.graphics.vector.ImageVector>> = emptyList(),
    snackbarHostState: SnackbarHostState,
    onSelectTab: (String) -> Unit,
    content: @Composable (PaddingValues) -> Unit
) {
    val activeExploreTab = currentRoute in setOf(NiloraRoutes.HOME, NiloraRoutes.EXPLORE, NiloraRoutes.ADVENTURE_DISCOVERY)
    val activeTogetherTab = currentRoute in setOf(NiloraRoutes.TOGETHER_HUB, NiloraRoutes.DISCOVER, NiloraRoutes.NILORA_CREWS, NiloraRoutes.WORLD_POSTCARDS, NiloraRoutes.CULTURE_SWAPS)
    val activeEventsTab = currentRoute in setOf(NiloraRoutes.EVENTS_HUB, NiloraRoutes.REALITY_HUB, NiloraRoutes.WORLD_QUESTS)
    val activeLiveTab = currentRoute in setOf(NiloraRoutes.REALTIME_TRANSLATOR, NiloraRoutes.MESSAGES, NiloraRoutes.GROUPS)
    val activeMyWorldTab = currentRoute in setOf(NiloraRoutes.PROFILE_HUB, NiloraRoutes.NILORA_MEMORY)

    fun isRouteActive(route: String): Boolean = when (route) {
        NiloraRoutes.HOME -> activeExploreTab
        NiloraRoutes.TOGETHER_HUB -> activeTogetherTab
        NiloraRoutes.EVENTS_HUB -> activeEventsTab
        NiloraRoutes.REALTIME_TRANSLATOR -> activeLiveTab
        NiloraRoutes.PROFILE_HUB -> activeMyWorldTab
        else -> currentRoute == route
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useRail = maxWidth >= 600.dp
        if (useRail && showTopLevelNav) {
            Row(modifier = Modifier.fillMaxSize()) {
                NavigationRail(
                    modifier = Modifier.testTag("adaptive_navigation_rail")
                ) {
                    Column(
                        modifier = Modifier.verticalScroll(rememberScrollState()),
                        horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally
                    ) {
                        Spacer(Modifier.height(12.dp))
                        navItems.forEach { (route, label, icon) ->
                            NavigationRailItem(
                                selected = isRouteActive(route),
                                onClick = { onSelectTab(route) },
                                icon = { Icon(icon, contentDescription = label) },
                                label = { Text(label, maxLines = 1) },
                                modifier = Modifier.testTag("rail_nav_$route")
                            )
                        }
                        if (secondaryRailItems.isNotEmpty()) {
                            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp, horizontal = 12.dp))
                            secondaryRailItems.forEach { (route, label, icon) ->
                                NavigationRailItem(
                                    selected = currentRoute == route,
                                    onClick = { onSelectTab(route) },
                                    icon = { Icon(icon, contentDescription = label) },
                                    label = { Text(label, maxLines = 1) },
                                    modifier = Modifier.testTag("rail_nav_$route")
                                )
                            }
                        }
                    }
                }
                Scaffold(
                    modifier = Modifier.weight(1f),
                    snackbarHost = { SnackbarHost(snackbarHostState) },
                    contentWindowInsets = WindowInsets.safeDrawing,
                    content = content
                )
            }
        } else {
            Scaffold(
                snackbarHost = { SnackbarHost(snackbarHostState) },
                contentWindowInsets = WindowInsets.safeDrawing,
                bottomBar = {
                    if (showTopLevelNav) {
                        NavigationBar(modifier = Modifier.testTag("adaptive_navigation_bar")) {
                            navItems.forEach { (route, label, icon) ->
                                NavigationBarItem(
                                    selected = isRouteActive(route),
                                    onClick = { onSelectTab(route) },
                                    icon = { Icon(icon, contentDescription = label) },
                                    label = {
                                        Text(
                                            text = label,
                                            maxLines = 1,
                                            softWrap = false,
                                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                        )
                                    },
                                    modifier = Modifier.testTag("bottom_nav_$route")
                                )
                            }
                        }
                    }
                },
                content = content
            )
        }
    }
}
