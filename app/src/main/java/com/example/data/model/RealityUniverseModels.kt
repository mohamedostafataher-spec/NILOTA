package com.example.data.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp
import kotlin.math.*

enum class RealityEventCategory(
    val key: String,
    val labelEn: String,
    val labelAr: String,
    val labelFr: String,
    val labelRu: String
) {
    CULTURAL("CULTURAL", "Cultural", "ثقافي", "Culturel", "Культура"),
    FESTIVAL("FESTIVAL", "Festival", "مهرجان", "Festival", "Фестиваль"),
    MUSIC("MUSIC", "Music", "موسيقى", "Musique", "Музыка"),
    SPORTS("SPORTS", "Sports", "رياضة", "Sport", "Спорт"),
    TOURNAMENT("TOURNAMENT", "Tournament", "بطولة", "Tournoi", "Турнир"),
    COMMUNITY("COMMUNITY", "Community", "مجتمعي", "Communauté", "Сообщество"),
    EXHIBITION("EXHIBITION", "Exhibition", "معرض", "Exposition", "Выставка"),
    CONFERENCE("CONFERENCE", "Conference", "مؤتمر", "Conférence", "Конференция"),
    TOURISM("TOURISM", "Tourism", "سياحة", "Tourisme", "Туризм"),
    OUTDOOR("OUTDOOR", "Outdoor", "هواء طلق", "Plein air", "На воздухе"),
    EDUCATIONAL("EDUCATIONAL", "Educational", "تعليمي", "Éducatif", "Обучение"),
    SEASONAL("SEASONAL", "Seasonal", "موسمي", "Saisonnier", "Сезонное");

    fun label(lang: NiloraLanguage): String = when (lang) {
        NiloraLanguage.ARABIC -> labelAr
        NiloraLanguage.FRENCH -> labelFr
        NiloraLanguage.RUSSIAN -> labelRu
        NiloraLanguage.ENGLISH -> labelEn
    }

    companion object {
        fun fromKey(key: String): RealityEventCategory =
            entries.firstOrNull { it.key.equals(key, ignoreCase = true) } ?: CULTURAL
    }
}

enum class EventVerificationTier(
    val key: String,
    val labelEn: String,
    val labelAr: String
) {
    ADMIN_VERIFIED("ADMIN_VERIFIED", "Admin Verified Event", "فعالية موثقة"),
    UNVERIFIED_USER_SUBMITTED("UNVERIFIED_USER_SUBMITTED", "Unverified / User Submitted", "مقدمة من مستخدم (غير موثقة)"),
    EXTERNAL_OPEN_SOURCE("EXTERNAL_OPEN_SOURCE", "Open-Source Reference (Confirm Official Schedule)", "مصدر عام مفتوح (تأكد من الموعد الرسمي)");

    companion object {
        fun fromKey(key: String): EventVerificationTier =
            entries.firstOrNull { it.key.equals(key, ignoreCase = true) } ?: UNVERIFIED_USER_SUBMITTED
    }
}

enum class EventOrganizerType(val key: String, val labelEn: String) {
    OFFICIAL_INSTITUTION("OFFICIAL_INSTITUTION", "Official Institution"),
    CULTURAL_CENTER("CULTURAL_CENTER", "Cultural Center"),
    LOCAL_COMMUNITY("LOCAL_COMMUNITY", "Local Community"),
    SPORTS_CLUB("SPORTS_CLUB", "Sports Club"),
    USER_ORGANIZER("USER_ORGANIZER", "Community Member"),
    EXTERNAL_OPEN_SOURCE("EXTERNAL_OPEN_SOURCE", "Open-Source Feed");

    companion object {
        fun fromKey(key: String): EventOrganizerType =
            entries.firstOrNull { it.key.equals(key, ignoreCase = true) } ?: USER_ORGANIZER
    }
}

enum class ActivityDomain(
    val key: String,
    val labelEn: String,
    val labelAr: String
) {
    GO_TOGETHER("GO_TOGETHER", "Go Together", "اذهبوا معاً"),
    PLAY_SPORTS("PLAY_SPORTS", "Play Sports", "العبوا رياضة"),
    SOCIAL_GAME("SOCIAL_GAME", "Social Games", "ألعاب جماعية"),
    ADVENTURE_OUTDOOR("ADVENTURE_OUTDOOR", "Adventure & Nature", "مغامرة وطبيعة"),
    COMMUNITY_IMPACT("COMMUNITY_IMPACT", "Community Impact", "أثر مجتمعي");

    companion object {
        fun fromKey(key: String): ActivityDomain =
            entries.firstOrNull { it.key.equals(key, ignoreCase = true) } ?: GO_TOGETHER
    }
}

enum class ActivitySubCategory(
    val key: String,
    val domain: ActivityDomain,
    val labelEn: String,
    val labelAr: String
) {
    // Go Together
    BEACH("BEACH", ActivityDomain.GO_TOGETHER, "Beach Outing", "نزهة شاطئية"),
    MUSEUM("MUSEUM", ActivityDomain.GO_TOGETHER, "Museum Visit", "زيارة متحف"),
    CITY_EXPLORATION("CITY_EXPLORATION", ActivityDomain.GO_TOGETHER, "City Exploration", "استكشاف المدينة"),
    RESTAURANT("RESTAURANT", ActivityDomain.GO_TOGETHER, "Local Food Table", "طعام محلي"),
    FESTIVAL_BUDDY("FESTIVAL_BUDDY", ActivityDomain.GO_TOGETHER, "Festival Companion", "رفيق مهرجان"),
    // Play Sports
    FOOTBALL("FOOTBALL", ActivityDomain.PLAY_SPORTS, "Football", "كرة قدم"),
    PADEL("PADEL", ActivityDomain.PLAY_SPORTS, "Padel", "بادل"),
    BASKETBALL("BASKETBALL", ActivityDomain.PLAY_SPORTS, "Basketball", "كرة سلة"),
    TENNIS("TENNIS", ActivityDomain.PLAY_SPORTS, "Tennis", "تنس"),
    VOLLEYBALL("VOLLEYBALL", ActivityDomain.PLAY_SPORTS, "Volleyball", "كرة طائرة"),
    RUNNING("RUNNING", ActivityDomain.PLAY_SPORTS, "Running", "جري"),
    CYCLING("CYCLING", ActivityDomain.PLAY_SPORTS, "Cycling", "دراجات"),
    SWIMMING("SWIMMING", ActivityDomain.PLAY_SPORTS, "Swimming", "سباحة"),
    // Social Games
    TREASURE_HUNT("TREASURE_HUNT", ActivityDomain.SOCIAL_GAME, "City Treasure Hunt", "البحث عن الكنز"),
    SCAVENGER_HUNT("SCAVENGER_HUNT", ActivityDomain.SOCIAL_GAME, "Scavenger Hunt", "مهمة استكشافية"),
    CULTURAL_QUIZ("CULTURAL_QUIZ", ActivityDomain.SOCIAL_GAME, "Cultural Quiz", "تحدي ثقافي"),
    PHOTO_CHALLENGE("PHOTO_CHALLENGE", ActivityDomain.SOCIAL_GAME, "Photo Challenge", "تحدي تصوير"),
    TEAM_COMPETITION("TEAM_COMPETITION", ActivityDomain.SOCIAL_GAME, "Friendly Team Game", "تحدي فرق"),
    // Adventure & Outdoor
    DIVING("DIVING", ActivityDomain.ADVENTURE_OUTDOOR, "Diving Group", "غوص"),
    SNORKELING("SNORKELING", ActivityDomain.ADVENTURE_OUTDOOR, "Snorkeling", "سنوركلينج"),
    BOAT_TRIP("BOAT_TRIP", ActivityDomain.ADVENTURE_OUTDOOR, "Boat Trip", "رحلة بحرية"),
    HIKING("HIKING", ActivityDomain.ADVENTURE_OUTDOOR, "Hiking", "تسلق ومشي"),
    CAMPING("CAMPING", ActivityDomain.ADVENTURE_OUTDOOR, "Desert Camping", "تخييم"),
    // Community Impact
    BEACH_CLEANUP("BEACH_CLEANUP", ActivityDomain.COMMUNITY_IMPACT, "Beach Cleanup", "تنظيف الشاطئ"),
    TREE_PLANTING("TREE_PLANTING", ActivityDomain.COMMUNITY_IMPACT, "Tree Planting", "زراعة أشجار"),
    VOLUNTEERING("VOLUNTEERING", ActivityDomain.COMMUNITY_IMPACT, "Volunteering", "تطوع مجتمعي"),
    EDUCATIONAL("EDUCATIONAL", ActivityDomain.COMMUNITY_IMPACT, "Educational Initiative", "مبادرة تعليمية"),
    OTHER("OTHER", ActivityDomain.GO_TOGETHER, "Other Activity", "نشاط آخر");

    companion object {
        fun forDomain(domain: ActivityDomain): List<ActivitySubCategory> =
            entries.filter { it.domain == domain }.ifEmpty { listOf(OTHER) }

        fun fromKey(key: String): ActivitySubCategory =
            entries.firstOrNull { it.key.equals(key, ignoreCase = true) } ?: OTHER
    }
}

enum class ActivitySkillLevel(val key: String, val labelEn: String) {
    ALL_LEVELS("ALL_LEVELS", "All Levels"),
    BEGINNER("BEGINNER", "Beginner"),
    INTERMEDIATE("INTERMEDIATE", "Intermediate"),
    ADVANCED("ADVANCED", "Advanced");

    companion object {
        fun fromKey(key: String): ActivitySkillLevel =
            entries.firstOrNull { it.key.equals(key, ignoreCase = true) } ?: ALL_LEVELS
    }
}

enum class TeamUpStatus(val key: String, val labelEn: String, val labelAr: String) {
    LOOKING_FOR_PARTICIPANTS("LOOKING_FOR_PARTICIPANTS", "Looking for Participants", "مطلوب مشاركون"),
    GROUP_FORMING("GROUP_FORMING", "Group Forming", "جاري تكوين الفريق"),
    GROUP_FULL("GROUP_FULL", "Group Full", "المجموعة مكتملة"),
    ACTIVITY_CONFIRMED("ACTIVITY_CONFIRMED", "Activity Confirmed", "النشاط مؤكد"),
    ACTIVITY_COMPLETED("ACTIVITY_COMPLETED", "Completed", "مكتمل"),
    ACTIVITY_CANCELLED("ACTIVITY_CANCELLED", "Cancelled", "ملغى");

    companion object {
        fun fromKey(key: String): TeamUpStatus =
            entries.firstOrNull { it.key.equals(key, ignoreCase = true) } ?: LOOKING_FOR_PARTICIPANTS
    }
}

enum class MissionExecutionMode(val key: String, val labelEn: String, val labelAr: String) {
    REAL_WORLD("REAL_WORLD", "Real-World Mission", "مهمة واقعية ميدانية"),
    VIRTUAL("VIRTUAL", "Virtual Study Mode", "وضع استكشاف افتراضي");

    companion object {
        fun fromKey(key: String): MissionExecutionMode =
            entries.firstOrNull { it.key.equals(key, ignoreCase = true) } ?: REAL_WORLD
    }
}

enum class ChallengeScope(val key: String, val labelEn: String) {
    INDIVIDUAL("INDIVIDUAL", "Individual"),
    TEAM("TEAM", "Team"),
    INTERNATIONAL("INTERNATIONAL", "International");

    companion object {
        fun fromKey(key: String): ChallengeScope =
            entries.firstOrNull { it.key.equals(key, ignoreCase = true) } ?: TEAM
    }
}

enum class RealityBridgeStage(val key: String, val labelEn: String, val labelAr: String, val stepIndex: Int) {
    VIRTUAL("VIRTUAL", "Virtual Exploration", "استكشاف افتراضي", 0),
    PLANNED("PLANNED", "Real Plan Saved", "خطة واقعية", 1),
    REGISTERED("REGISTERED", "Request / Joined", "تم التسجيل", 2),
    CONFIRMED("CONFIRMED", "Confirmed Plan", "مؤكد", 3),
    COMPLETED("COMPLETED", "Completed -> Memory", "مكتمل في الذاكرة", 4);

    companion object {
        fun fromKey(key: String): RealityBridgeStage =
            entries.firstOrNull { it.key.equals(key, ignoreCase = true) } ?: PLANNED
    }
}

data class LocalCityHub(
    val cityName: String,
    val countryName: String,
    val countryCode: String,
    val latitude: Double,
    val longitude: Double
)

object GlobalCityHubCatalog {
    val hubs: List<LocalCityHub> = listOf(
        LocalCityHub("Cairo", "Egypt", "EG", 30.0444, 31.2357),
        LocalCityHub("Alexandria", "Egypt", "EG", 31.2001, 29.9187),
        LocalCityHub("Sharm El Sheikh", "Egypt", "EG", 27.9158, 34.3299),
        LocalCityHub("Hurghada", "Egypt", "EG", 27.2579, 33.8116),
        LocalCityHub("Dahab", "Egypt", "EG", 28.5091, 34.5136),
        LocalCityHub("Luxor", "Egypt", "EG", 25.6872, 32.6396),
        LocalCityHub("Aswan", "Egypt", "EG", 24.0889, 32.8998),
        LocalCityHub("Dubai", "UAE", "AE", 25.2048, 55.2708),
        LocalCityHub("Paris", "France", "FR", 48.8566, 2.3522),
        LocalCityHub("London", "United Kingdom", "GB", 51.5072, -0.1276),
        LocalCityHub("Moscow", "Russia", "RU", 55.7558, 37.6173),
        LocalCityHub("Istanbul", "Turkey", "TR", 41.0082, 28.9784),
        LocalCityHub("Barcelona", "Spain", "ES", 41.3874, 2.1686),
        LocalCityHub("Tokyo", "Japan", "JP", 35.6762, 139.6503)
    )

    fun findHub(cityName: String): LocalCityHub? =
        hubs.firstOrNull { it.cityName.equals(cityName, ignoreCase = true) }
}

data class CuratedRealityMissionTemplate(
    val id: String,
    val templateType: String,
    val executionMode: MissionExecutionMode,
    val title: String,
    val objective: String,
    val cityName: String,
    val countryName: String,
    val latitude: Double,
    val longitude: Double,
    val safetyAndEcoRules: String,
    val suggestedSteps: Int
)

object RealityMissionCatalog {
    val starterTemplates: List<CuratedRealityMissionTemplate> = listOf(
        CuratedRealityMissionTemplate(
            id = "tpl_cairo_explorer",
            templateType = "CAIRO_EXPLORER",
            executionMode = MissionExecutionMode.REAL_WORLD,
            title = "Cairo Explorer — Living Heritage Walk",
            objective = "Visit 3 public historic landmarks or cultural squares in Cairo, exchange a greeting in Arabic, and record respectful field observations.",
            cityName = "Cairo",
            countryName = "Egypt",
            latitude = 30.0444,
            longitude = 31.2357,
            safetyAndEcoRules = "Stay in well-lit public squares during daylight, respect active worship spaces, and carry zero single-use litter.",
            suggestedSteps = 3
        ),
        CuratedRealityMissionTemplate(
            id = "tpl_red_sea_explorer",
            templateType = "RED_SEA_EXPLORER",
            executionMode = MissionExecutionMode.REAL_WORLD,
            title = "Red Sea Explorer — Reef Care & Coastal Log",
            objective = "Document shoreline marine safety prep, reef-safe sunscreen usage, and coastal cleanliness along Hurghada, Dahab, or Sharm El Sheikh.",
            cityName = "Sharm El Sheikh",
            countryName = "Egypt",
            latitude = 27.9158,
            longitude = 34.3299,
            safetyAndEcoRules = "Never touch or stand on coral reefs, swim with a buddy in marked safe zones, and pack out all waste.",
            suggestedSteps = 3
        ),
        CuratedRealityMissionTemplate(
            id = "tpl_city_discovery",
            templateType = "CITY_DISCOVERY",
            executionMode = MissionExecutionMode.REAL_WORLD,
            title = "City Discovery — Local Neighborhood Stories",
            objective = "Discover an artisan workshop, public waterfront promenade, or community library in your current city and log what makes it welcoming.",
            cityName = "Alexandria",
            countryName = "Egypt",
            latitude = 31.2001,
            longitude = 29.9187,
            safetyAndEcoRules = "Public venues only. Ask permission before photographing people or artisan stalls.",
            suggestedSteps = 2
        ),
        CuratedRealityMissionTemplate(
            id = "tpl_culture_connect",
            templateType = "CULTURE_CONNECT",
            executionMode = MissionExecutionMode.REAL_WORLD,
            title = "Culture Connect — Cross-Language Real Meetup",
            objective = "Meet in a public cafe, museum courtyard, or cultural hub with someone speaking another language and log 2 shared phrases learned in person.",
            cityName = "Cairo",
            countryName = "Egypt",
            latitude = 30.0478,
            longitude = 31.2336,
            safetyAndEcoRules = "Meet exclusively in busy public spaces and respect personal privacy boundaries.",
            suggestedSteps = 2
        ),
        CuratedRealityMissionTemplate(
            id = "tpl_team_challenge",
            templateType = "TEAM_CHALLENGE",
            executionMode = MissionExecutionMode.REAL_WORLD,
            title = "Team Challenge — Collaborative Scavenger & Photo Quest",
            objective = "Form a small 2–6 person team to complete 4 public architectural/cultural photo clues safely on foot.",
            cityName = "Luxor",
            countryName = "Egypt",
            latitude = 25.6872,
            longitude = 32.6396,
            safetyAndEcoRules = "Stay hydrated, obey traffic crossings and monument rules, and keep group members accounted for.",
            suggestedSteps = 4
        )
    )
}

data class ExternalOpenSourceEventNote(
    val id: String,
    val title: String,
    val summary: String,
    val cityName: String,
    val countryName: String,
    val sourceUrl: String,
    val attribution: String = "Wikipedia / Wikimedia Open-Source API (CC BY-SA 4.0)",
    val verificationTier: String = EventVerificationTier.EXTERNAL_OPEN_SOURCE.key
)

@Keep
@IgnoreExtraProperties
data class RealityEvent(
    @DocumentId val id: String = "",
    val creatorId: String = "",
    val creatorName: String = "",
    val title: String = "",
    val category: String = RealityEventCategory.CULTURAL.key,
    val description: String = "",
    val countryCode: String = "EG",
    val countryName: String = "Egypt",
    val cityName: String = "Cairo",
    val venueName: String = "",
    val latitude: Double? = null,
    val longitude: Double? = null,
    val hasVerifiedCoordinates: Boolean = false,
    val startDateIso: String = "",
    val endDateIso: String = "",
    val timeWindowLabel: String = "",
    val freeVerified: Boolean = false,
    val priceNote: String = "Check official organizer site",
    val organizerName: String = "",
    val organizerType: String = EventOrganizerType.USER_ORGANIZER.key,
    val officialUrl: String = "",
    val conditionsNote: String = "",
    val safetyNote: String = "Public venue meetup — verify local schedule before attending.",
    val ageMinimum: Int = 18,
    val verificationTier: String = EventVerificationTier.UNVERIFIED_USER_SUBMITTED.key,
    val lastVerifiedLabel: String = "",
    val status: String = "UPCOMING", // UPCOMING | ACTIVE | CANCELLED | COMPLETED
    val maxParticipants: Int = 25,
    val participantIds: List<String> = emptyList(),
    val participantNames: Map<String, String> = emptyMap(),
    val pendingRequestIds: List<String> = emptyList(),
    val joinPolicy: String = "OPEN", // OPEN | REQUEST_APPROVAL
    val linkedAdventureId: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class RealityEventMessage(
    @DocumentId val id: String = "",
    val eventId: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val originalText: String = "",
    val sourceLanguage: String = "en",
    val translations: Map<String, String> = emptyMap(),
    @ServerTimestamp val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class TeamActivityPost(
    @DocumentId val id: String = "",
    val creatorId: String = "",
    val creatorName: String = "",
    val domain: String = ActivityDomain.GO_TOGETHER.key,
    val subCategory: String = ActivitySubCategory.CITY_EXPLORATION.key,
    val title: String = "",
    val description: String = "",
    val countryName: String = "Egypt",
    val cityName: String = "Cairo",
    val publicMeetingSpot: String = "",
    val latitude: Double? = null,
    val longitude: Double? = null,
    val scheduledDateLabel: String = "",
    val skillLevel: String = ActivitySkillLevel.ALL_LEVELS.key,
    val equipmentNote: String = "",
    val safetyRules: String = "Meet in a well-lit public venue only. Never share private home addresses.",
    val ageMinimum: Int = 18,
    val joinPolicy: String = "PUBLIC", // PUBLIC | REQUEST_APPROVAL | INVITE_ONLY
    val teamStatus: String = TeamUpStatus.LOOKING_FOR_PARTICIPANTS.key,
    val maxParticipants: Int = 6,
    val participantIds: List<String> = emptyList(),
    val participantNames: Map<String, String> = emptyMap(),
    val pendingRequestIds: List<String> = emptyList(),
    val invitedUserIds: List<String> = emptyList(),
    val impactOrganizationName: String = "",
    val impactVerifiedByAdmin: Boolean = false,
    val linkedAdventureId: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class ActivityGroupMessage(
    @DocumentId val id: String = "",
    val activityId: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val originalText: String = "",
    val sourceLanguage: String = "en",
    val translations: Map<String, String> = emptyMap(),
    @ServerTimestamp val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class RealityMission(
    @DocumentId val id: String = "",
    val creatorId: String = "",
    val creatorName: String = "",
    val templateType: String = "CITY_DISCOVERY",
    val executionMode: String = MissionExecutionMode.REAL_WORLD.key,
    val title: String = "",
    val objective: String = "",
    val cityName: String = "Cairo",
    val countryName: String = "Egypt",
    val latitude: Double? = null,
    val longitude: Double? = null,
    val safetyAndEcoRules: String = "",
    val targetSteps: Int = 3,
    val participantIds: List<String> = emptyList(),
    val submissionsCount: Int = 0,
    val status: String = "ACTIVE", // ACTIVE | COMPLETED
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class MissionSubmission(
    @DocumentId val id: String = "",
    val missionId: String = "",
    val authorId: String = "",
    val authorName: String = "",
    val submissionMode: String = "REAL_WORLD_LOG", // REAL_WORLD_LOG | VIRTUAL_PROGRESS
    val reflectionText: String = "",
    val placeLabel: String = "",
    val photoBase64: String = "",
    val ecoSafetyConfirmed: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class GlobalChallenge(
    @DocumentId val id: String = "",
    val creatorId: String = "",
    val creatorName: String = "",
    val challengeScope: String = ChallengeScope.INTERNATIONAL.key,
    val category: String = "COMMUNITY_IMPACT",
    val title: String = "",
    val description: String = "",
    val targetGoal: Int = 20,
    val participantIds: List<String> = emptyList(),
    val participantCountries: List<String> = emptyList(),
    val entriesCount: Int = 0,
    val cityName: String = "Cairo",
    val countryName: String = "Egypt",
    val latitude: Double? = null,
    val longitude: Double? = null,
    val status: String = "ACTIVE", // ACTIVE | COMPLETED
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class ChallengeEntry(
    @DocumentId val id: String = "",
    val challengeId: String = "",
    val authorId: String = "",
    val authorName: String = "",
    val countryName: String = "Egypt",
    val teamName: String = "",
    val summaryText: String = "",
    val pointsClaimed: Int = 1,
    val verificationLabel: String = "USER_SUBMITTED", // USER_SUBMITTED | PARTICIPANT_CONFIRMED | ADMIN_VERIFIED
    @ServerTimestamp val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class RealityBridgeLink(
    @DocumentId val id: String = "",
    val userId: String = "",
    val virtualAdventureId: String = "",
    val virtualAdventureTitle: String = "",
    val destinationName: String = "",
    val targetType: String = "REALITY_EVENT", // REALITY_EVENT | TEAM_ACTIVITY | BOOKING_ACTIVITY | REAL_WORLD_MISSION | NILORA_MEMORY
    val targetId: String = "",
    val targetTitle: String = "",
    val stage: String = RealityBridgeStage.PLANNED.key,
    val notes: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

fun geoDistanceKm(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
    val earthRadiusKm = 6371.0
    val dLat = Math.toRadians(lat2 - lat1)
    val dLon = Math.toRadians(lng2 - lng1)
    val a = sin(dLat / 2).pow(2) +
        cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2).pow(2)
    return 2.0 * earthRadiusKm * asin(sqrt(a))
}
