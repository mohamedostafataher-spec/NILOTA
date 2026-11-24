package com.example.data.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp
import kotlin.math.abs

enum class AdventureBiome(val key: String, val labelEn: String, val depthLabelEn: String) {
    RED_SEA_REEF("RED_SEA_REEF", "Red Sea Coral World", "Underwater 3D Environment"),
    MEDITERRANEAN_COAST("MEDITERRANEAN_COAST", "Mediterranean & Islands", "Coastal Lagoon 3D Preview"),
    DESERT_OASIS("DESERT_OASIS", "Sinai & Western Oasis", "Dunes & Spring 3D Preview"),
    NILE_HERITAGE("NILE_HERITAGE", "Nile & Ancient Heritage", "River & Temple 3D Preview");

    companion object {
        fun fromKey(key: String?): AdventureBiome =
            entries.firstOrNull { it.key == key } ?: RED_SEA_REEF
    }
}

enum class AdventureType(val key: String, val labelEn: String) {
    SCUBA_DIVING("SCUBA_DIVING", "Scuba Diving"),
    SNORKELING("SNORKELING", "Snorkeling"),
    CORAL_REEF("CORAL_REEF", "Coral Reef Exploration"),
    SWIMMING("SWIMMING", "Swimming"),
    BOAT_TRIP("BOAT_TRIP", "Boat Trip"),
    DESERT_EXPLORATION("DESERT_EXPLORATION", "Desert Exploration"),
    MOUNTAIN_ADVENTURE("MOUNTAIN_ADVENTURE", "Mountain Adventure"),
    SUNSET_EXPERIENCE("SUNSET_EXPERIENCE", "Sunset Experience"),
    CULTURAL_EXPLORATION("CULTURAL_EXPLORATION", "Cultural Exploration"),
    HISTORICAL_EXPLORATION("HISTORICAL_EXPLORATION", "Historical Exploration"),
    LOCAL_FOOD("LOCAL_FOOD", "Local Food Experience"),
    RELAXATION("RELAXATION", "Relaxation & Wellness");

    companion object {
        fun fromKey(key: String?): AdventureType =
            entries.firstOrNull { it.key == key } ?: CORAL_REEF
    }
}

enum class AdventureSessionMode(val key: String, val labelEn: String, val subtitleEn: String) {
    SOLO("SOLO", "Explore Solo", "Enter the immersive 3D environment at your own pace"),
    JOIN_OTHERS("JOIN_OTHERS", "Join Others", "Explore live alongside real people inside the same world"),
    CREW_SESSION("CREW_SESSION", "Create a Crew", "Host a shared session and turn companions into a crew");

    companion object {
        fun fromKey(key: String?): AdventureSessionMode =
            entries.firstOrNull { it.key == key } ?: SOLO
    }
}

enum class AdventureLevel(val key: String, val labelEn: String) {
    BEGINNER("BEGINNER", "Beginner Friendly"),
    INTERMEDIATE("INTERMEDIATE", "Intermediate Explorer"),
    EXPLORER("EXPLORER", "Seasoned Adventurer");

    companion object {
        fun fromKey(key: String?): AdventureLevel =
            entries.firstOrNull { it.key == key } ?: BEGINNER
    }
}

data class AdventurePoi(
    val id: String,
    val name: String,
    val category: String,
    val posX: Float,
    val posY: Float,
    val posZ: Float,
    val depthMeters: Int,
    val factText: String,
    val conservationTip: String
)

data class AdventureExperience(
    val id: String,
    val title: String,
    val subtitle: String,
    val destinationName: String,
    val countryCode: String,
    val regionLabel: String,
    val latitude: Double,
    val longitude: Double,
    val wikipediaSlug: String,
    val biome: AdventureBiome,
    val adventureType: AdventureType,
    val isCoastalMarine: Boolean,
    val isSecretSpot: Boolean,
    val recommendedTimeOfDay: String,
    val ecoConservationNote: String,
    val safetyNotice: String,
    val description: String,
    val pois: List<AdventurePoi>,
    val geoAttribution: String = "WGS84 Geographic Coordinates (OpenStreetMap / EEAA Reference). Artistic 3D spatial reconstruction — not an exact millimeter replica."
)

object AdventureCatalog {
    val egyptAdventures: List<AdventureExperience> = listOf(
        AdventureExperience(
            id = "sharm_reef_dive",
            title = "Red Sea — Reef Exploration",
            subtitle = "Shark & Yolanda Reef Marine Sanctuary",
            destinationName = "Sharm El Sheikh",
            countryCode = "EG",
            regionLabel = "South Sinai Peninsula",
            latitude = 27.9158,
            longitude = 34.3299,
            wikipediaSlug = "Sharm_El_Sheikh",
            biome = AdventureBiome.RED_SEA_REEF,
            adventureType = AdventureType.CORAL_REEF,
            isCoastalMarine = true,
            isSecretSpot = false,
            recommendedTimeOfDay = "Morning (08:30–11:30)",
            ecoConservationNote = "Never touch or stand on Red Sea corals; use reef-safe mineral sunscreen and maintain neutral buoyancy.",
            safetyNotice = "Real dives at Shark & Yolanda Reef carry strong offshore currents; scuba certification & local licensed guide required.",
            description = "Descend into sunlit Red Sea coral walls teeming with anthias, Napoleon wrasse, sea fans, and turquoise caustics before planning your trip with real companions.",
            pois = listOf(
                AdventurePoi(
                    id = "poi_shark_wall",
                    name = "Coral Crest Drop-off",
                    category = "Coral Shelf",
                    posX = 0f,
                    posY = -4.2f,
                    posZ = -12f,
                    depthMeters = 6,
                    factText = "Sunlit table and staghorn corals thrive in clear shallow Red Sea currents rich in plankton.",
                    conservationTip = "Keep fins at least 2 meters above branching coral heads."
                ),
                AdventurePoi(
                    id = "poi_anemone_garden",
                    name = "Anemone Sanctuary",
                    category = "Marine Life",
                    posX = -7.5f,
                    posY = -6.0f,
                    posZ = -18f,
                    depthMeters = 11,
                    factText = "Red Sea two-banded anemonefish shelter inside luminous host anemones along protected ledges.",
                    conservationTip = "Observe calmly without chasing or flashing camera strobes closely."
                ),
                AdventurePoi(
                    id = "poi_pelagic_blue",
                    name = "Sapphire Current Edge",
                    category = "Open Reef",
                    posX = 8.2f,
                    posY = -8.5f,
                    posZ = -24f,
                    depthMeters = 18,
                    factText = "Where the Sinai shelf drops sharply into the Gulf of Aqaba, warm surface water meets deep upwelling.",
                    conservationTip = "Check surface winds via live Open-Meteo marine telemetry before boat departures."
                )
            )
        ),
        AdventureExperience(
            id = "ras_mohamed_mangrove",
            title = "Ras Mohamed — Mangrove & Reef Gate",
            subtitle = "Hidden Marine Reserve Channel",
            destinationName = "Ras Mohamed",
            countryCode = "EG",
            regionLabel = "Southern Tip of Sinai",
            latitude = 27.7222,
            longitude = 34.2500,
            wikipediaSlug = "Ras_Muhammad_National_Park",
            biome = AdventureBiome.RED_SEA_REEF,
            adventureType = AdventureType.SNORKELING,
            isCoastalMarine = true,
            isSecretSpot = true,
            recommendedTimeOfDay = "Early Morning",
            ecoConservationNote = "Protected Egyptian National Park (EEAA). No single-use plastics, shell collection, or anchoring outside moorings.",
            safetyNotice = "Observe reserve gate hours and tide levels along the mangrove channel.",
            description = "Explore Egypt's southernmost Sinai sanctuary where salt-tolerant mangroves nurture juvenile reef fish right beside crystal turquoise lagoons.",
            pois = listOf(
                AdventurePoi(
                    id = "poi_ras_wall",
                    name = "Anemone City Terrace",
                    category = "Snorkel Ledge",
                    posX = 3.5f,
                    posY = -3.8f,
                    posZ = -14f,
                    depthMeters = 5,
                    factText = "Gentle shallow shelves make Ras Mohamed a rare site where snorkelers view thriving hard corals safely.",
                    conservationTip = "Use designated entry points so shore seagrass beds remain undisturbed."
                ),
                AdventurePoi(
                    id = "poi_mangrove_lagoon",
                    name = "Tidal Mangrove Nursery",
                    category = "Coastal Ecology",
                    posX = -6.0f,
                    posY = -2.2f,
                    posZ = -10f,
                    depthMeters = 2,
                    factText = "Avicennia marina mangroves here are among the northernmost mangrove forests on Earth.",
                    conservationTip = "Stay on marked wooden trails; never break prop roots."
                )
            )
        ),
        AdventureExperience(
            id = "dahab_blue_hole",
            title = "Dahab & Blue Hole Sanctuary",
            subtitle = "Gulf of Aqaba Rim & Canyon Light",
            destinationName = "Blue Hole, Dahab",
            countryCode = "EG",
            regionLabel = "Gulf of Aqaba Coast",
            latitude = 28.5724,
            longitude = 34.5371,
            wikipediaSlug = "Blue_Hole_(Red_Sea)",
            biome = AdventureBiome.RED_SEA_REEF,
            adventureType = AdventureType.SCUBA_DIVING,
            isCoastalMarine = true,
            isSecretSpot = false,
            recommendedTimeOfDay = "Mid-Morning Sunlight",
            ecoConservationNote = "Enter through the Saddle reef rim only; protect fragile coralline ledges along the shore.",
            safetyNotice = "The Blue Hole deep arch exceeds recreational limits (~56m). Recreational snorkelers & divers must remain safely near the upper Saddle (<18m).",
            description = "Float above the Gulf of Aqaba's sapphire sinkhole rim surrounded by Sinai desert mountains and shimmering coral chimneys.",
            pois = listOf(
                AdventurePoi(
                    id = "poi_saddle_entry",
                    name = "The Saddle Rim",
                    category = "Snorkel & Entry",
                    posX = -2.5f,
                    posY = -4.5f,
                    posZ = -13f,
                    depthMeters = 7,
                    factText = "A natural coral saddle opens between the coastal lagoon and the deep Gulf drop-off.",
                    conservationTip = "Check northerlies wind conditions before entering from the shore platform."
                ),
                AdventurePoi(
                    id = "poi_bells_wall",
                    name = "Sunlit Coral Chimney",
                    category = "Vertical Wall",
                    posX = 6.5f,
                    posY = -7.2f,
                    posZ = -20f,
                    depthMeters = 14,
                    factText = "Sunlight pierces perpendicular limestone walls covered in vibrant soft corals and glassfish.",
                    conservationTip = "Follow depth limits strictly and dive only with certified local buddies."
                )
            )
        ),
        AdventureExperience(
            id = "dahab_lagoon_sunset",
            title = "Dahab — Coast & Bedouin Tea",
            subtitle = "Coastal Lagoon, Free-Diving & Sunset Culture",
            destinationName = "Dahab",
            countryCode = "EG",
            regionLabel = "South Sinai",
            latitude = 28.5091,
            longitude = 34.5136,
            wikipediaSlug = "Dahab",
            biome = AdventureBiome.RED_SEA_REEF,
            adventureType = AdventureType.LOCAL_FOOD,
            isCoastalMarine = true,
            isSecretSpot = false,
            recommendedTimeOfDay = "Sunset (16:30–18:30)",
            ecoConservationNote = "Support local Bedouin & coastal artisans sustainably without shoreline litter.",
            safetyNotice = "Afternoon thermal winds accelerate briskly across Dahab Lagoon; verify wind speed before paddling.",
            description = "Experience Dahab's laid-back shore reefs paired with Bedouin mint tea, mountain silhouettes across the Gulf of Aqaba, and slow cultural conversations.",
            pois = listOf(
                AdventurePoi(
                    id = "poi_eel_garden",
                    name = "Lighthouse Shore Garden",
                    category = "Shore Reef",
                    posX = 1.8f,
                    posY = -3.5f,
                    posZ = -11f,
                    depthMeters = 4,
                    factText = "Sand slopes near Dahab Lighthouse host colonies of swaying garden eels and bright parrotfish.",
                    conservationTip = "Avoid kicking near sandy burrows."
                )
            )
        ),
        AdventureExperience(
            id = "sataya_dolphin_lagoon",
            title = "Sataya Reef Lagoon Sanctuary",
            subtitle = "Fury Shoals Crescent Lagoon (Secret Spot)",
            destinationName = "Marsa Alam / Fury Shoals",
            countryCode = "EG",
            regionLabel = "Southern Red Sea",
            latitude = 24.1667,
            longitude = 35.6981,
            wikipediaSlug = "Marsa_Alam",
            biome = AdventureBiome.RED_SEA_REEF,
            adventureType = AdventureType.BOAT_TRIP,
            isCoastalMarine = true,
            isSecretSpot = true,
            recommendedTimeOfDay = "Morning (07:30–10:30)",
            ecoConservationNote = "Strict wildlife etiquette: never block resting marine pods, never chase wildlife, zero loud splashing.",
            safetyNotice = "Remote southern offshore reef reachable only by licensed day or liveaboard vessel in calm seas.",
            description = "A calm crescent coral lagoon in southern Egypt where tranquil turquoise waters reward respectful travelers with silence and pristine reef gardens.",
            pois = listOf(
                AdventurePoi(
                    id = "poi_sataya_crest",
                    name = "Crescent Coral Wall",
                    category = "Protected Lagoon",
                    posX = -3.0f,
                    posY = -4.0f,
                    posZ = -15f,
                    depthMeters = 6,
                    factText = "Horseshoe reefs naturally shelter the lagoon interior from open Red Sea swells.",
                    conservationTip = "Give all marine wildlife generous space to pass freely."
                )
            )
        ),
        AdventureExperience(
            id = "giftun_red_sea_islands",
            title = "Giftun Island & Coral Shallows",
            subtitle = "Red Sea Marine Park Islands",
            destinationName = "Hurghada / Giftun Island",
            countryCode = "EG",
            regionLabel = "Red Sea Governorate",
            latitude = 27.2167,
            longitude = 33.9667,
            wikipediaSlug = "Giftun_Islands",
            biome = AdventureBiome.MEDITERRANEAN_COAST,
            adventureType = AdventureType.SWIMMING,
            isCoastalMarine = true,
            isSecretSpot = false,
            recommendedTimeOfDay = "Late Morning",
            ecoConservationNote = "Marine park conservation fee supports reef mooring buoys across the Giftun archipelago.",
            safetyNotice = "Watch sun exposure on open white-sand spits and obey boat-channel swim zones.",
            description = "Glide between luminous sandbars and bright offshore coral patches around Giftun Island with real-time sea weather updates.",
            pois = listOf(
                AdventurePoi(
                    id = "poi_giftun_sandbar",
                    name = "Turquoise Sand Spit",
                    category = "Island Lagoon",
                    posX = 4.0f,
                    posY = -2.5f,
                    posZ = -12f,
                    depthMeters = 3,
                    factText = "Fine biogenic coral sand creates bright aquamarine gradients visible from miles out at sea.",
                    conservationTip = "Pack back every item brought ashore."
                )
            )
        ),
        AdventureExperience(
            id = "luxor_west_bank",
            title = "Luxor — Sunrise Nile & Sandstone",
            subtitle = "Theban Heritage & Felucca Horizon",
            destinationName = "Luxor",
            countryCode = "EG",
            regionLabel = "Upper Egypt",
            latitude = 25.6872,
            longitude = 32.6396,
            wikipediaSlug = "Luxor",
            biome = AdventureBiome.NILE_HERITAGE,
            adventureType = AdventureType.HISTORICAL_EXPLORATION,
            isCoastalMarine = false,
            isSecretSpot = false,
            recommendedTimeOfDay = "Sunrise (06:00–08:30)",
            ecoConservationNote = "Never touch ancient carved reliefs or painted limestone chambers.",
            safetyNotice = "Summer temperatures exceed 40°C by midday; plan walks early using live temperature data.",
            description = "Stand amid golden sandstone colonnades and tranquil Nile reflections while practicing Arabic, French, Russian, or English greetings with fellow explorers.",
            pois = listOf(
                AdventurePoi(
                    id = "poi_luxor_axis",
                    name = "Sandstone Pylon Axis",
                    category = "Heritage Monument",
                    posX = 0f,
                    posY = -1.2f,
                    posZ = -14f,
                    depthMeters = 0,
                    factText = "Ancient Theban temples aligned key processional avenues with Nile crossings and solstice light.",
                    conservationTip = "Respect monument photography and preservation guidelines."
                )
            )
        ),
        AdventureExperience(
            id = "aswan_nubian_nile",
            title = "Aswan — Granite Cataracts Sunset",
            subtitle = "Nubian Riverbanks & Sailboat Calm",
            destinationName = "Aswan",
            countryCode = "EG",
            regionLabel = "Southern Nile",
            latitude = 24.0889,
            longitude = 32.8998,
            wikipediaSlug = "Aswan",
            biome = AdventureBiome.NILE_HERITAGE,
            adventureType = AdventureType.SUNSET_EXPERIENCE,
            isCoastalMarine = false,
            isSecretSpot = false,
            recommendedTimeOfDay = "Golden Hour (17:00)",
            ecoConservationNote = "Keep First Cataract granite islands clean and support Nubian family guesthouses.",
            safetyNotice = "Strong river currents near cataract rocks; swim only where locals confirm safe calm eddies.",
            description = "Drift past smooth pink-granite boulders and warm Nubian river villages as golden desert sun meets the Nile.",
            pois = listOf(
                AdventurePoi(
                    id = "poi_elephantine_rock",
                    name = "First Cataract Granite Bend",
                    category = "River Landmark",
                    posX = -4.2f,
                    posY = -1.0f,
                    posZ = -13f,
                    depthMeters = 0,
                    factText = "Smooth black-glazed granite boulders formed Egypt's historic southern Nile gateway for millennia.",
                    conservationTip = "Respect local island privacy and cultural photography etiquette."
                )
            )
        ),
        AdventureExperience(
            id = "cairo_historic_citadel",
            title = "Cairo — Citadel Light & Old Streets",
            subtitle = "Living Craft, Coffeehouses & Skyline",
            destinationName = "Cairo",
            countryCode = "EG",
            regionLabel = "Nile Delta & Capital",
            latitude = 30.0444,
            longitude = 31.2357,
            wikipediaSlug = "Cairo",
            biome = AdventureBiome.NILE_HERITAGE,
            adventureType = AdventureType.CULTURAL_EXPLORATION,
            isCoastalMarine = false,
            isSecretSpot = false,
            recommendedTimeOfDay = "Late Afternoon & Evening",
            ecoConservationNote = "Respect active prayer spaces, artisans' workshops, and historic residential alleys.",
            safetyNotice = "Bustling pedestrian alleys near Khan El Khalili — keep valuables secure and use public transit landmarks.",
            description = "Meet Cairenes and travelers in an atmospheric architectural twilight setting — exchange Egyptian colloquial phrases before meeting in safe public spaces.",
            pois = listOf(
                AdventurePoi(
                    id = "poi_muizz_lantern",
                    name = "Historic Mamluk Dome Terrace",
                    category = "Architecture",
                    posX = 3.0f,
                    posY = -0.8f,
                    posZ = -15f,
                    depthMeters = 0,
                    factText = "Cairo's historic core preserves over a thousand years of stone domes, mashrabiya woodcraft, and caravanserais.",
                    conservationTip = "Support authentic handmade crafts and ask before photographing artisans."
                )
            )
        ),
        AdventureExperience(
            id = "siwa_salt_lakes",
            title = "Siwa Oasis — Salt Lakes & Dunes",
            subtitle = "Cleopatra's Spring & Desert Stars (Secret Spot)",
            destinationName = "Siwa Oasis",
            countryCode = "EG",
            regionLabel = "Western Desert",
            latitude = 29.2032,
            longitude = 25.5195,
            wikipediaSlug = "Siwa_Oasis",
            biome = AdventureBiome.DESERT_OASIS,
            adventureType = AdventureType.DESERT_EXPLORATION,
            isCoastalMarine = false,
            isSecretSpot = true,
            recommendedTimeOfDay = "Sunrise or Sunset",
            ecoConservationNote = "Siwa's hypersaline lakes and palm groves are fragile desert ecosystems — rinse salt cleanly off-site.",
            safetyNotice = "Remote Western Desert road travel requires daylight planning, abundant water, and licensed local desert guides.",
            description = "Experience tranquil emerald salt-crust pools bordered by golden dunes and Amazigh-Egyptian heritage far from busy highways.",
            pois = listOf(
                AdventurePoi(
                    id = "poi_salt_basin",
                    name = "Emerald Salt Mirror",
                    category = "Natural Spring",
                    posX = 0f,
                    posY = -1.6f,
                    posZ = -13f,
                    depthMeters = 0,
                    factText = "High mineral salinity allows effortless floating beneath clear Saharan skies.",
                    conservationTip = "Keep freshwater irrigation channels free from soaps or chemicals."
                )
            )
        )
    )

    fun findById(id: String): AdventureExperience =
        egyptAdventures.firstOrNull { it.id == id } ?: egyptAdventures.first()

    fun secretSpots(): List<AdventureExperience> =
        egyptAdventures.filter { it.isSecretSpot }

    fun pickSurpriseSecretSpot(
        preferredType: AdventureType? = null,
        windSpeedKmh: Double? = null,
        excludeId: String? = null
    ): AdventureExperience {
        val candidates = secretSpots().filter { spot ->
            spot.id != excludeId &&
                (windSpeedKmh == null || !spot.isCoastalMarine || windSpeedKmh < 32.0)
        }.ifEmpty { secretSpots() }

        return preferredType?.let { wanted ->
            candidates.firstOrNull { it.adventureType == wanted }
        } ?: candidates.first()
    }
}

data class LiveWeatherSnapshot(
    val temperatureC: Double,
    val windSpeedKmh: Double,
    val weatherCode: Int,
    val weatherLabel: String,
    val seaSurfaceTempC: Double? = null,
    val waveHeightMeters: Double? = null,
    val sourceAttribution: String = "Open-Meteo Weather & Marine API (CC BY 4.0)"
)

data class WikipediaSummaryResult(
    val title: String,
    val extract: String,
    val pageUrl: String,
    val attribution: String = "Wikipedia REST API (CC BY-SA 4.0)"
)

sealed interface LiveIntelligenceResult<out T> {
    data class Available<T>(val data: T) : LiveIntelligenceResult<T>
    data class Unavailable(val reason: String) : LiveIntelligenceResult<Nothing>
}

@Keep
@IgnoreExtraProperties
data class LiveAdventureSession(
    @DocumentId val id: String = "",
    val hostId: String = "",
    val hostName: String = "",
    val adventureId: String = "",
    val adventureTitle: String = "",
    val destinationName: String = "",
    val mode: String = AdventureSessionMode.JOIN_OTHERS.key, // SOLO | JOIN_OTHERS | CREW_SESSION
    val status: String = "ACTIVE", // ACTIVE | ENDED
    val maxParticipants: Int = 6,
    val participantIds: List<String> = emptyList(),
    val participantNames: Map<String, String> = emptyMap(),
    val participantCountries: Map<String, String> = emptyMap(),
    val participantLanguages: Map<String, String> = emptyMap(),
    val voicePreferred: Boolean = true,
    val experienceLevel: String = AdventureLevel.BEGINNER.key,
    val durationMinutes: Int = 30,
    val availabilitySlot: String = "Now",
    val crewId: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class SessionParticipantPresence(
    @DocumentId val id: String = "",
    val sessionId: String = "",
    val userId: String = "",
    val displayName: String = "",
    val country: String = "",
    val languageCode: String = "en",
    val posX: Double = 0.0,
    val posY: Double = -4.0,
    val posZ: Double = 2.0,
    val yawDeg: Double = 0.0,
    val pointedPoiId: String = "",
    val pointedX: Double = 0.0,
    val pointedY: Double = 0.0,
    val pointedZ: Double = 0.0,
    val voiceMuted: Boolean = false,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class SessionChatMessage(
    @DocumentId val id: String = "",
    val sessionId: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val originalText: String = "",
    val sourceLanguage: String = "en",
    val translations: Map<String, String> = emptyMap(),
    val translationNote: String = "",
    val discoveryPoiId: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class AdventureMatchProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "",
    val country: String = "Egypt",
    val targetAdventureId: String = "sharm_reef_dive",
    val targetDestination: String = "Sharm El Sheikh",
    val interests: List<String> = emptyList(),
    val preferredLanguage: String = "en",
    val spokenLanguages: List<String> = emptyList(),
    val experienceLevel: String = AdventureLevel.BEGINNER.key,
    val preferredDurationMinutes: Int = 30,
    val availableTimeSlot: String = "Evenings",
    val voicePreferred: Boolean = true,
    val isAvailable: Boolean = true,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

data class AdventureMatchResult(
    val profile: AdventureMatchProfile,
    val scorePercent: Int,
    val matchReasons: List<String>
)

@Keep
@IgnoreExtraProperties
data class NiloraCrew(
    @DocumentId val id: String = "",
    val name: String = "",
    val description: String = "",
    val leaderId: String = "",
    val memberIds: List<String> = emptyList(),
    val memberNames: Map<String, String> = emptyMap(),
    val memberCountries: Map<String, String> = emptyMap(),
    val sharedInterests: List<String> = emptyList(),
    val focusDestination: String = "Sharm El Sheikh",
    val plannedAdventureIds: List<String> = emptyList(),
    val adventureHistoryTitles: List<String> = emptyList(),
    val joinPolicy: String = "OPEN", // OPEN | INVITE_ONLY
    val voiceChannelEnabled: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class CrewMessage(
    @DocumentId val id: String = "",
    val crewId: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val originalText: String = "",
    val sourceLanguage: String = "en",
    val translations: Map<String, String> = emptyMap(),
    @ServerTimestamp val createdAt: Timestamp? = null
)

object AdventureMatchmaker {
    fun computeCompatibility(
        seeker: AdventureMatchProfile,
        candidate: AdventureMatchProfile
    ): AdventureMatchResult {
        var score = 0
        val reasons = mutableListOf<String>()

        if (seeker.targetAdventureId == candidate.targetAdventureId) {
            score += 30
            reasons.add("Same Adventure")
        } else if (seeker.targetDestination.equals(candidate.targetDestination, ignoreCase = true)) {
            score += 18
            reasons.add("Same Destination")
        }

        val sharedInterests = seeker.interests.map { it.lowercase() }
            .intersect(candidate.interests.map { it.lowercase() }.toSet())
        if (sharedInterests.isNotEmpty()) {
            score += minOf(20, sharedInterests.size * 7)
            reasons.add("${sharedInterests.size} Shared Interest(s)")
        }

        val seekerLangs = (seeker.spokenLanguages + seeker.preferredLanguage).map { it.lowercase() }.toSet()
        val candLangs = (candidate.spokenLanguages + candidate.preferredLanguage).map { it.lowercase() }.toSet()
        if (seekerLangs.intersect(candLangs).isNotEmpty()) {
            score += 15
            reasons.add("Common Language")
        } else {
            score += 8
            reasons.add("NILORA LIVE Bridgeable")
        }

        if (seeker.experienceLevel == candidate.experienceLevel) {
            score += 12
            reasons.add(AdventureLevel.fromKey(candidate.experienceLevel).labelEn)
        }
        if (abs(seeker.preferredDurationMinutes - candidate.preferredDurationMinutes) <= 15) {
            score += 10
            reasons.add("~${candidate.preferredDurationMinutes}m Session")
        }
        if (seeker.voicePreferred == candidate.voicePreferred) {
            score += 8
            reasons.add(if (candidate.voicePreferred) "Voice Chat On" else "Quiet / Text Mode")
        }
        if (seeker.availableTimeSlot.equals(candidate.availableTimeSlot, ignoreCase = true)) {
            score += 5
            reasons.add("Same Time Window")
        }

        return AdventureMatchResult(
            profile = candidate,
            scorePercent = score.coerceIn(10, 100),
            matchReasons = reasons.ifEmpty { listOf("Open to Global Explorers") }
        )
    }
}

enum class CapabilityBucket(val labelEn: String) {
    IMPLEMENTED_AND_TESTED("Implemented & Tested"),
    DEVICE_OR_NETWORK_DEPENDENT("Implemented (Device / Network Dependent)"),
    REQUIRES_EXTERNAL_CONFIG("Requires External Service Configuration"),
    NOT_YET_IMPLEMENTED("Not Yet Implemented")
}

data class PlatformCapabilityItem(
    val feature: String,
    val bucket: CapabilityBucket,
    val details: String
)

object PlatformCapabilityRegistry {
    val items: List<PlatformCapabilityItem> = listOf(
        PlatformCapabilityItem(
            feature = "Interactive 3D Red Sea & Destination Engine",
            bucket = CapabilityBucket.IMPLEMENTED_AND_TESTED,
            details = "Native Kotlin perspective-projected 3D canvas with touch look-around, movement, POIs, depth shading, animated marine life, and spatial participant markers."
        ),
        PlatformCapabilityItem(
            feature = "Zero-Fake Multiplayer Presence & Matchmaking",
            bucket = CapabilityBucket.IMPLEMENTED_AND_TESTED,
            details = "Real Firestore-backed Live Adventure Sessions, 3D participant coordinates/pointing beacons, availability matchmaking, and persistent NILORA Crews verified with emulator security rules."
        ),
        PlatformCapabilityItem(
            feature = "NILORA LIVE 4-Language Translation (ar-EG, en, ru, fr)",
            bucket = CapabilityBucket.DEVICE_OR_NETWORK_DEPENDENT,
            details = "Uses Android SpeechRecognizer + TextToSpeech and Firebase AI Logic Gemini models; accuracy varies for background noise and colloquial nuance, and requires internet + app check."
        ),
        PlatformCapabilityItem(
            feature = "Open-Meteo Marine/Weather & Wikipedia Intelligence",
            bucket = CapabilityBucket.DEVICE_OR_NETWORK_DEPENDENT,
            details = "Fetches real live sea temperature, waves, wind, and encyclopedic extracts with CC BY / CC BY-SA attribution when online; never invents offline metrics."
        ),
        PlatformCapabilityItem(
            feature = "Real-Time Multi-Party Streaming Voice Calls (WebRTC SFU)",
            bucket = CapabilityBucket.REQUIRES_EXTERNAL_CONFIG,
            details = "Live adventure sessions sync voice dictation, TTS playback, and translated messages; continuous full-duplex multi-party VoIP requires a WebRTC SFU/TURN server."
        ),
        PlatformCapabilityItem(
            feature = "Reality Connected Universe (Events, Team-Up, Missions & Bridge)",
            bucket = CapabilityBucket.IMPLEMENTED_AND_TESTED,
            details = "Firestore-backed Reality Events, Go/Play/Social Games/Team-Up & Community Impact posts, Real-World Missions (strictly isolated from Virtual exploration), Global Challenges, and Reality Bridge timeline verified by emulator security rules."
        ),
        PlatformCapabilityItem(
            feature = "Open-Source Cultural Events Intelligence (Wikimedia API)",
            bucket = CapabilityBucket.DEVICE_OR_NETWORK_DEPENDENT,
            details = "Queries live Wikipedia public search API (CC BY-SA 4.0) tagged EXTERNAL_OPEN_SOURCE; requires internet and never fabricates offline schedules or ticket prices."
        ),
        PlatformCapabilityItem(
            feature = "Official Commercial Event Ticketing APIs & Live Crowd Telemetry",
            bucket = CapabilityBucket.NOT_YET_IMPLEMENTED,
            details = "Events link to official organizer URLs when provided and clearly distinguish ADMIN_VERIFIED vs UNVERIFIED_USER_SUBMITTED; automated third-party ticketing & live attendance sensors are not connected."
        ),
        PlatformCapabilityItem(
            feature = "Commercial Travel Payment Checkout & Photogrammetry Twins",
            bucket = CapabilityBucket.NOT_YET_IMPLEMENTED,
            details = "Bookings submit requests (NOT_CONFIGURED payment) and 3D worlds are artistic reconstructions anchored to real coordinates rather than millimeter photogrammetric scans."
        )
    )
}
