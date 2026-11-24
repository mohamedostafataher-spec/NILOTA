# NILORA — Global Travel, Discovery, Booking & Real-Time Voice Translation Super App

**Vision:** `ONE APP. ONE WORLD. NO LANGUAGE BARRIERS.`  
**Founder & Creator Attribution:** **Created & Founded by Mohamed Mostafa Taher**

---

## 1. Platform Overview & Integrity Guarantees
NILORA is a mobile-first Android platform built with **Kotlin, Jetpack Compose, Material Design 3, Firebase Auth, Cloud Firestore, and Firebase AI Logic (Gemini 3.8 Flash + App Check)**.

- **Zero Fabricated Data:** NILORA never generates synthetic hotels, beaches, restaurants, prices, coordinates, reviews, fake booking confirmations, or fake users. Empty categories show honest unavailable states until populated by verified administrators or authorized external APIs.
- **Booking Request vs. Confirmation Integrity:** Travelers submit `REQUEST_SUBMITTED` / `AWAITING_PROVIDER_CONFIRMATION` requests. Only the assigned service provider (or verified administrator) can transition a booking to `CONFIRMED` or `REJECTED`. Payment status defaults honestly to `NOT_CONFIGURED` until a real merchant gateway is connected; raw card details are never stored.
- **Global Extensible Architecture with Egypt as Inaugural Destination:** Includes 12 Egyptian city/regional viewports (`Cairo`, `Giza`, `Alexandria`, `Sharm El Sheikh`, `Hurghada`, `Dahab`, `Marsa Alam`, `Luxor`, `Aswan`, `El Gouna`, `Siwa`, `North Coast`), all 17 tourism categories, and multi-country/multi-currency taxonomy support.
- **Multilingual & RTL/LTR Support:** UI localization (`en`, `ar` RTL, `fr`, `ru`) with bundled `Cairo` (Arabic), `Playfair Display` (Display), and `Plus Jakarta Sans` (Body) `.ttf` fonts, honoring `LocalReducedMotion` accessibility settings, plus an extensible 12-language `WorldLanguage` catalog prioritizing **Egyptian Arabic (`ar-EG`)**, **Modern Standard Arabic (`ar-MSA`)**, **English (`en`)**, **Russian (`ru`)**, and **French (`fr`)**.
- **Phase 2 Global Community, 1:1 Messaging & Groups:** Voluntarily discoverable `public_profiles`, server-side user blocking, connection requests, 1:1 conversations preserving `originalText` verbatim alongside NILORA LIVE translations, AAC `MediaRecorder` voice clips, Speech-to-Text dictation, and moderated multilingual groups (`OPEN` / `REQUEST_APPROVAL` / `INVITE_ONLY`).
- **Phase 3 Complete Brand Identity (`GLOBAL SOUL — EGYPTIAN ORIGIN`) & Adaptive UX Integration:** Midnight Navy (`#101D32`), Nile Turquoise (`#19B6B0`), Warm Sand (`#E9D9BD`), Soft Ivory (`#FAF8F2`), Egyptian Gold (`#C6A15B`), and Coral (`#EF806C`) palette; original custom `NiloraBrandSymbol` / `NiloraBrandWordmark` (`ON_LIGHT`, `ON_DARK`, `MONOCHROME_GOLD`); uncluttered **5-tab mobile `NavigationBar`** (`Home`, `Translate`, `Explore`, `Messages`, `Profile`) + **Tablet/Desktop `NavigationRail`** with secondary `Discover` & `Groups` tabs and unified `CommunicationHubTabs`. Delivered natively for **Android** (not iOS or Web/PWA).

---

## 2. Project Directory Structure

```text
├── .env.example                                  # Optional Google Maps API key placeholder
├── docs/
│   ├── NILORA_LIVE_PHASE01.md                    # NILORA LIVE engine specs, model matrix & testing guide
│   ├── NILORA_PHASE2_RELEASE_READINESS.md        # Honest Phase 2 readiness & verification classification
│   └── NILORA_PHASE3_ARCHITECTURE_AND_QA.md      # Phase 3 brand system, nav map, costs & 4-bucket QA report
├── firebase-blueprint.json                       # Canonical 22-entity Firestore schema blueprint
├── firestore.rules                               # Zero-trust RBAC/ABAC security & block rules
├── firestore.test.js                             # Emulator-backed Firestore rules test suite (10 tests)
├── firebase.json                                 # Local Firebase emulator configuration
├── metadata.json                                 # AI Studio platform identity
└── app/src/main/java/com/example/
    ├── MainActivity.kt                           # App Check init, Google Auth gate, responsive 6-tab NavHost
    ├── niloralive/                               # Modular NILORA LIVE Phase 01 + Phase 2 translation/audio engine
    │   ├── LiveEngineContracts.kt                # PipelineStage, DialectMode, swappable engine interfaces
    │   ├── AudioCaptureAndHardwareProfiler.kt    # Real AudioRecord PCM check, NS/AEC & model catalog
    │   ├── VoiceMessageAudioRecorder.kt          # Real AAC MediaRecorder capture & MediaPlayer clip playback
    │   ├── AndroidSpeechRecognitionEngine.kt     # SpeechRecognizer STT + live RMS VAD energy
    │   ├── EgyptianTranslationEngine.kt          # Gemini 3.8 Flash ar-EG + world language translation engine
    │   ├── AndroidVoiceGenerationEngine.kt       # TTS voice inspector, queue control, speed/volume/replay
    │   └── SpeechEvaluationHarness.kt            # Deterministic Levenshtein WER & Markdown report export
    ├── data/model/NiloraModels.kt                # Domain models, WorldLanguage/DialCodes, Phase 1+2 taxonomies
    ├── data/repository/
    │   ├── FirestoreDataRepository.kt            # Auth-scoped Firestore CRUD, profile & public_profiles sync
    │   ├── CommunityAndMessagingRepository.kt    # Global community, blocks, connections, 1:1 chat & groups
    │   ├── TranslationAiRepository.kt            # Grounded AI Travel Assistant repository
    │   ├── LocationAndPlacesRepository.kt        # FusedLocation GPS + Retrofit Google Places API client
    │   └── UserPreferencesRepository.kt          # DataStore local preferences (language, theme, consent, TTS)
    ├── firebase/
    │   ├── AppCheckHelper.kt                     # Debug/Recaptcha Firebase App Check setup
    │   ├── FirestoreErrorHandler.kt              # Diagnostic structured security/permission logger
    │   └── GoogleAuthHelper.kt                   # Credential Manager Google Sign-In & auto sign-in
    ├── localization/AppStrings.kt                # EN, AR, FR, RU localization strings (incl. 5 primary + rail labels)
    └── ui/
        ├── theme/{Color.kt, Type.kt, Theme.kt}   # Midnight Navy + Nile Turquoise + Egyptian Gold + Soft Ivory + LocalReducedMotion
        ├── navigation/NiloraRoutes.kt            # Complete Phase 1–3 route definitions
        ├── components/
        │   ├── NiloraBrandIdentity.kt            # NiloraBrandSymbol, NiloraBrandWordmark (Light/Dark/Gold), NiloraGeometricDivider
        │   └── NiloraComponents.kt               # CommunicationHubTabs, MapPreviewPanel, ConversationPreviewCard, VoiceMessagePlayer, Footer
        ├── viewmodel/{MainViewModel.kt, TranslatorViewModel.kt, CommunityMessagingViewModel.kt}
        └── screens/
            ├── AuthOnboardingScreens.kt          # Branded Splash, Onboarding, Language, Google Login, Global Registration
            ├── HomeExploreScreens.kt             # Branded Home (Live AI status, real chats/suggestions), Explore (Map preview & verticals)
            ├── CategoryListingScreens.kt         # Beaches, Activities, Hotels, Restaurants
            ├── InteractiveMapScreen.kt           # OpenStreetMap/CARTO slippy map, GPS & Routes
            ├── TranslatorScreens.kt              # NILORA LIVE Hub, Create Room, Join Room, Shared 180° Mode
            ├── LiveEvaluationScreen.kt           # Hardware Audit, Model Matrix & WER Lab
            ├── AiAssistantScreen.kt              # Streaming Gemini Grounded Assistant
            ├── GlobalMessagingScreens.kt         # Global Messages Inbox & 1:1 Direct Chat (Voice/STT/Translate)
            ├── CommunityDiscoverScreens.kt       # Global Community Discovery, Connection Requests & Blocked Users
            ├── GroupConversationsScreens.kt      # Multilingual Groups Hub, Room & Owner/Moderator Controls
            ├── MarketplaceBookingScreens.kt      # Activity, Booking, Provider flows
            ├── JourneyProfileScreens.kt          # My Journey, Saved, Global Profile Settings, Profile Hub, Alerts
            ├── LegalSettingsAboutScreens.kt      # Settings, Privacy Policy, Terms, About
            └── AdminDashboardScreen.kt           # Role-gated Verification, Publishing & Audit Logs
```

---

## 3. Environment Variables & Integrations

1. **Firebase Auth + Cloud Firestore + Firebase AI Logic + App Check:** Configured via `app/google-services.json` and `app/src/main/res/values/firebase_applet_config.xml`. Uses `gemini-3.8-flash` for structured JSON translation and streaming AI travel guidance without embedding a Gemini API key in client code.
2. **Optional Google Places API (`GOOGLE_MAPS_API_KEY`):** Configure in the **Secrets panel in AI Studio** (`.env.example` provides the `GOOGLE_MAPS_API_KEY=MY_GOOGLE_MAPS_API_KEY` template). Without a live key, the app honestly displays `AWAITING_CREDENTIALS` while OpenStreetMap/CARTO map tiles, FusedLocation GPS, and verified Firestore markers continue operating normally.
3. **Administrator Bootstrap:** Initial admin access is granted via `mohamedostafataher@gmail.com` (verified email claim checked in `firestore.rules` + `/admin_users/{uid}`).

---

## 4. Verification & Testing Commands

- **Firestore Security Rules Emulator Suite (Node — 10 tests):**
  ```bash
  FIRESTORE_EMULATOR_HOST="127.0.0.1:8085" node --test firestore.test.js
  ```
- **JVM Unit & Robolectric Compose UI Tests:**
  ```bash
  gradle :app:testDebugUnitTest
  ```
- **Android Debug Compilation:**
  ```bash
  gradle :app:compileDebugKotlin
  ```

---

## 5. Local Build & CI

**Requirements:** JDK 17 (Temurin), Android SDK with Platform 36, and Gradle 9.3.1 (the version pinned in `gradle/wrapper/gradle-wrapper.properties`; install it from gradle.org or via SDKMAN until the `gradlew` script is added back to the repo).

```bash
# Unit + Robolectric UI tests
gradle :app:testDebugUnitTest

# Debug APK (output: app/build/outputs/apk/debug/)
gradle :app:assembleDebug
```

`app/google-services.json` is intentionally not committed; the Google Services Gradle plugin is configured with `missingGoogleServicesStrategy = WARN`, so local and CI builds succeed without it. Firebase-dependent features stay inert until a real `google-services.json` is placed in `app/`.

**Continuous Integration:** `.github/workflows/android-ci.yml` runs on every push to `main` and every pull request: it sets up JDK 17 and the Android SDK, runs `gradle testDebugUnitTest`, builds `gradle assembleDebug`, and uploads the debug APK as a workflow artifact.
