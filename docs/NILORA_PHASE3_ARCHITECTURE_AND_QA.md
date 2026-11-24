# NILORA — Phase 3 Architecture, Brand Identity & Honest QA Report

**Tagline / Creative Direction:** `GLOBAL SOUL — EGYPTIAN ORIGIN`  
**Mandatory Creator & Founder Attribution:** `Created & Founded by Mohamed Mostafa Taher`  
**Delivered Target Platform:** **Native Android Application (Kotlin, Jetpack Compose, Material Design 3)**  
*(Explicit Platform Scope Disclosure: This deliverable targets Android phones and tablets via Compose + Adaptive Window Widths. Web/PWA and native iOS binaries are **NOT** implemented in this repository.)*

---

## 1. Visual Identity System & Design Tokens

Implemented centrally in `app/src/main/java/com/example/ui/theme/` and `app/src/main/java/com/example/ui/components/NiloraBrandIdentity.kt`:

### A. Brand Color Palette & Accessibility Pairing (`Color.kt` + `Theme.kt`)
| Role | Token Name | Hex | Accessibility Notes |
|---|---|---|---|
| Primary Navy | `MidnightNavy` (`MidnightNavyLight` / `MidnightNavyDark`) | `#101D32` (`#1B2F50` / `#081120`) | Primary light-theme chrome, headers, 2-phone room dark cards |
| Accent Turquoise | `NileTurquoise` / `NileTurquoiseDark` | `#19B6B0` / `#0D7D78` | `NileTurquoiseDark` enforces WCAG AA contrast on ivory/white surfaces |
| Warm Sand | `WarmSand` / `WarmSandSoft` | `#E9D9BD` / `#F3EAD8` | Editorial callouts, desert-inspired badges, dark-card secondary ink |
| Soft Ivory | `SoftIvory` / `SurfaceIvoryMuted` | `#FAF8F2` / `#F1ECE1` | Primary light-mode background (`NiloraLightColorScheme.background`) |
| Egyptian Gold | `EgyptianGold` / `EgyptianGoldDark` | `#C6A15B` / `#846425` | `EgyptianGoldDark` used for tertiary light-mode text; subtle 1dp borders |
| Coral Accent | `CoralAccent` / `CoralAccessible` | `#EF806C` / `#B83924` | `CoralAccessible` used for light-mode errors & destructive actions |

- Light & Dark themes (`NiloraLightColorScheme`, `NiloraDarkColorScheme`) automatically honor system dark mode and user override (`LIGHT`, `DARK`, `SYSTEM`) persisted in DataStore.
- Contrast-checked semantic status chips (`VerifiedEmerald` `#0F9D75`, `WarningAmber` `#B45309`).

### B. Typography & Multilingual Support (`Type.kt` + `res/font/*.ttf`)
- **Arabic (`ar` / `ar-EG` / `ar-MSA`):** Bundled `Cairo` typeface (`cairo.ttf`), full automatic `LayoutDirection.Rtl` whenever `languageCode.startsWith("ar", ignoreCase = true)`.
- **Latin & Cyrillic Display (`display*`, `headline*`):** Bundled `Playfair Display` (`playfair_display.ttf`).
- **Latin & Cyrillic Body & UI (`title*`, `body*`, `label*`):** Bundled `Plus Jakarta Sans` (`plus_jakarta_sans.ttf`) supporting English, French, and Russian.
- **Scale Rules:** 4dp spacing grid (`NiloraSpacing`), rounded corner tokens (`NiloraRadius`: 10dp, 16dp, 20dp, 28dp), and `sp` font scaling.

### C. Original Abstract Geometric Identity (`NiloraBrandIdentity.kt`)
- `NiloraBrandSymbol`: Custom Jetpack Compose `Canvas` geometric Nile diamond keystone arch + dual acoustic/Nile horizon waves + compass apex/nadir nodes (deliberately avoids cliché globes/planes/speech bubbles).
- `NiloraBrandWordmark`: Paired symbol + `NILORA` tracked wordmark + `GLOBAL SOUL • EGYPTIAN ORIGIN` subtitle across three explicit variants:
  - `BrandLogoVariant.ON_LIGHT`
  - `BrandLogoVariant.ON_DARK`
  - `BrandLogoVariant.MONOCHROME_GOLD`
- `NiloraGeometricDivider`: Horizon rule centered with a Nile turquoise diamond node.
- **Adaptive Launcher Icon (`img_nilora_icon_fg.png` + `#0D1F35` background):** Generated custom adaptive launcher icon across all density buckets (`mdpi`–`xxxhdpi` + `mipmap-anydpi-v26`).

### D. Motion & Accessibility (`LocalReducedMotion`)
- `rememberSystemReducedMotion()` reads `Settings.Global.ANIMATOR_DURATION_SCALE` and exposes `LocalReducedMotion`.
- `MainActivity` `NavHost` transitions drop to `EnterTransition.None` / `ExitTransition.None` when reduced motion is active, and `NiloraAnimatedStatusPulse` holds steady opacity instead of pulsing indefinitely.

---

## 2. Integrated Navigation Architecture

### Mobile (`maxWidth < 600.dp`) — Exact 5 Primary Destinations
`NiloraResponsiveNavShell` renders a Material 3 `NavigationBar` with **5 uncluttered top-level tabs**:
1. **Home** (`NiloraRoutes.HOME`) — Branded dashboard, live Cloud AI readiness badge, contextual shortcuts (`Map`, `AI Travel Guide`, `Messages`, `Discover`, `Groups`), real `Continue Conversations`, real `Suggested Community Connections`, Egypt destinations, verified places & activities.
2. **Translate** (`NiloraRoutes.REALTIME_TRANSLATOR`) — NILORA LIVE voice translation hub (Two-Phone Rooms, 180° Face-to-Face Split Screen, Dialect mode, Consent Gate, Speech Diagnostics & WER Lab).
3. **Explore** (`NiloraRoutes.EXPLORE`) — Interactive map preview panel, vertical quick pills (`Beaches`, `Diving & Activities`, `Hotels & Resorts`, `Restaurants`, `Saved Places`), country/city/17-category filters, verified listings.
4. **Messages** (`NiloraRoutes.MESSAGES`) — Direct entry into the Communication Hub (`CommunicationHubTabs` links **Chats**, **Discover**, and **Groups** cleanly without crowding mobile navigation).
5. **Profile** (`NiloraRoutes.PROFILE_HUB`) — Travel Journey & Saved Places, Connection Requests badge, Blocked Users, Notifications, Language/Theme Settings, About NILORA, Admin Dashboard (when authorized).

### Tablet / Large Foldables / ChromeOS (`maxWidth >= 600.dp`)
`NiloraResponsiveNavShell` automatically switches to a persistent `NavigationRail` displaying all 5 primary destinations plus a visual divider and secondary destinations (**Discover** and **Groups**).

---

## 3. Reusable Component Library (`NiloraComponents.kt` + `NiloraBrandIdentity.kt`)
- Navigation & Headers: `NiloraResponsiveNavShell`, `NiloraTopBar`, `NiloraFooter`, `NiloraSectionHeader`, `CommunicationHubTabs`
- Brand Identity: `NiloraBrandSymbol`, `NiloraBrandWordmark`, `NiloraGeometricDivider`
- Messaging & Community: `NiloraSearchFilterBar`, `NiloraConversationPreviewCard`, `NiloraVoiceMessagePlayer`, `NiloraTranslationPanelCard`, `NiloraNotificationCard`
- Discovery & Booking: `NiloraMapPreviewPanel`, `VerifiedSourceBadge`, `BookingStatusChip`
- Honest State & Feedback: `NiloraLoadingState`, `NiloraErrorState`, `NiloraConfirmDialog`, `UnavailableDataCard`, `OfflineStatusBanner`, `NiloraAnimatedStatusPulse`

---

## 4. Remaining Dependencies & External Cost Disclosure

**No paid upgrades or metered external subscriptions were auto-activated.**

| Dependency / Integration | Current Status | Cost / Billing Behavior | Notes |
|---|---|---|---|
| **Firebase Auth & Cloud Firestore** | Active (`ai-studio-android-nilora-aa0ab52d-c288-4861-bd88-9974aa66a40b`) | Firebase Spark/Blaze usage based on project quota | Security rules (`firestore.rules`) deployed & verified |
| **Firebase AI Logic (Gemini 3.8 Flash + App Check)** | Active | Governed by project's Firebase AI Logic / Gemini API quota | Used only after explicit user consent in NILORA LIVE / Chat / Assistant |
| **OpenStreetMap / CARTO Map Tiles** | Active (Public OSM/CARTO tile URLs in `InteractiveMapScreen`) | Free community tile usage (respect OSM tile usage policy for high-volume commercial scale) | Real GPS via `FusedLocationProviderClient`; no API key required |
| **Google Places API (`GOOGLE_MAPS_API_KEY`)** | **Optional / User-Managed** | Subject to Google Maps Platform billing if enabled | Configured in `.env.example` / Secrets; app cleanly displays `AWAITING_CREDENTIALS` when absent |
| **Merchant Payment Processor** | **`NOT_CONFIGURED`** | None | Booking requests submit to providers with explicit `NOT_CONFIGURED` payment status; no fake checkouts or card capture |

---

## 5. Honest Verification & QA Status Report

### A. Implemented and Tested (Verified via Automated Suites)
- **Firestore Security Rules Emulator Suite (`firestore.test.js` — 10 tests):** Zero-trust auth isolation, admin/provider role gates, booking transition guards, `blocks` messaging/request prevention, immutable message content rules, group owner/moderator membership controls.
- **JVM Unit & Robolectric Compose UI Tests (`app/src/test/java/com/example/`):**
  - Adaptive navigation shell: 5-tab `NavigationBar` on mobile width vs `NavigationRail` (primary + secondary items) on `w840dp-h640dp` tablet width.
  - RTL direction (`ar-EG` -> `LayoutDirection.Rtl`) & reduced-motion accessibility (`LocalReducedMotion`).
  - Brand wordmark variants (`ON_LIGHT`, `ON_DARK`, `MONOCHROME_GOLD`) and founder attribution (`Created & Founded by Mohamed Mostafa Taher`).
  - `HomeScreen` (real `Continue Conversations`, real `Suggested Community Connections`, live Cloud AI badge), `ExploreScreen` (map preview, verticals, honest empty states), `CommunicationHubTabs` switching.
  - NILORA LIVE consent gate, shared 180° screen controls, evaluation lab (`AudioCaptureAndHardwareProfiler`, `SpeechEvaluationHarness`), booking validation, admin RBAC, community discovery filter, 1:1 original+translated chat fidelity, and group creation/moderation.

### B. Implemented but Not Yet Tested on Physical Hardware Emulator
*(Environment has no ADB/live Android Emulator attached for interactive instrumented device runs.)*
- Live hardware `AudioRecord` / `SpeechRecognizer` acoustic accuracy across noisy real-world Egyptian environments (`ar-EG` WER trials).
- End-to-end live `GoogleAuthHelper` Credential Manager sheet on physical Play Services devices and push/re-install lifecycle.
- AAC `MediaRecorder` / `MediaPlayer` physical speaker/microphone routing on physical Android handsets.

### C. Awaiting External Configuration
- `GOOGLE_MAPS_API_KEY` in AI Studio Secrets (for live Google Places HTTP text-search queries alongside verified Firestore pins).
- Merchant payment gateway integration (currently explicitly surfaced as `NOT_CONFIGURED`).

### D. Not Implemented (Out of Current Scope)
- Native iOS (SwiftUI) and Web/PWA browser builds (this repository strictly delivers Native Android Kotlin + Jetpack Compose).
- Real-time P2P raw WebRTC audio/video streaming in Two-Phone Translator Rooms (rooms intentionally sync real-time Firestore transcripts/translations + on-device TTS playback and clearly state this in-UI).
- Dedicated pre-trained offline on-device `whisper.cpp` / `NLLB-200` model binaries (cataloged & evaluated in the Evaluation Lab UI; not bundled inside the APK to keep binary size lightweight).
