package com.example.data.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

enum class NiloraLanguage(
    val code: String,
    val displayName: String,
    val nativeName: String,
    val bcp47Tag: String,
    val isRtl: Boolean
) {
    ENGLISH("en", "English", "English", "en-US", false),
    ARABIC("ar", "Arabic (incl. Egyptian)", "العربية (المصرية)", "ar-EG", true),
    FRENCH("fr", "French", "Français", "fr-FR", false),
    RUSSIAN("ru", "Russian", "Русский", "ru-RU", false);

    companion object {
        fun fromCode(code: String?): NiloraLanguage =
            entries.firstOrNull { it.code == code } ?: ENGLISH
    }
}

enum class TourismCategory(val key: String, val labelEn: String, val labelAr: String, val labelFr: String, val labelRu: String) {
    BEACHES("BEACHES", "Beaches", "الشواطئ", "Plages", "Пляжи"),
    SWIMMING("SWIMMING", "Swimming", "السباحة", "Natation", "Плавание"),
    DIVING("DIVING", "Diving", "الغوص", "Plongée", "Дайвинг"),
    SNORKELING("SNORKELING", "Snorkeling", "السنوركلينج", "Snorkeling", "Снорклинг"),
    BOAT_TRIPS("BOAT_TRIPS", "Boat Trips", "رحلات بحرية", "Excursions en bateau", "Морские прогулки"),
    HISTORICAL("HISTORICAL", "Historical Attractions", "معالم تاريخية", "Sites historiques", "Исторические места"),
    MUSEUMS("MUSEUMS", "Museums", "المتاحف", "Musées", "Музеи"),
    RESTAURANTS("RESTAURANTS", "Restaurants", "مطاعم", "Restaurants", "Рестораны"),
    CAFES("CAFES", "Cafes", "مقاهي", "Cafés", "Кафе"),
    HOTELS("HOTELS", "Hotels", "فنادق", "Hôtels", "Отели"),
    RESORTS("RESORTS", "Resorts", "منتجعات", "Complexes hôteliers", "Курорты"),
    NIGHTLIFE("NIGHTLIFE", "Nightlife", "الحياة الليلية", "Vie nocturne", "Ночная жизнь"),
    DESERT_SAFARI("DESERT_SAFARI", "Desert Safari", "سفاري الصحراء", "Safari dans le désert", "Сафари по пустыне"),
    ADVENTURE("ADVENTURE", "Adventure Activities", "أنشطة المغامرات", "Aventure", "Приключения"),
    FAMILY("FAMILY", "Family Activities", "أنشطة عائلية", "Activités familiales", "Семейный отдых"),
    SHOPPING("SHOPPING", "Shopping", "التسوق", "Shopping", "Шопинг"),
    LOCAL_EXPERIENCES("LOCAL_EXPERIENCES", "Local Experiences", "تجارب محلية", "Expériences locales", "Местный опыт");

    fun label(lang: NiloraLanguage): String = when (lang) {
        NiloraLanguage.ARABIC -> labelAr
        NiloraLanguage.FRENCH -> labelFr
        NiloraLanguage.RUSSIAN -> labelRu
        NiloraLanguage.ENGLISH -> labelEn
    }

    companion object {
        fun fromKey(key: String?): TourismCategory =
            entries.firstOrNull { it.key == key || it.labelEn.equals(key, ignoreCase = true) } ?: HISTORICAL
    }
}

data class EgyptCityTaxonomy(
    val id: String,
    val nameEn: String,
    val nameAr: String,
    val region: String,
    val viewportLat: Double,
    val viewportLng: Double
)

object NiloraTaxonomy {
    const val FOUNDER_ATTRIBUTION = "Created & Founded by Mohamed Mostafa Taher"
    const val BRAND_VISION = "ONE APP. ONE WORLD. NO LANGUAGE BARRIERS."

    val supportedCountries = listOf(
        "Egypt" to "EG",
        "United Arab Emirates" to "AE",
        "Saudi Arabia" to "SA",
        "France" to "FR",
        "United Kingdom" to "GB",
        "United States" to "US",
        "Russia" to "RU",
        "Italy" to "IT",
        "Spain" to "ES",
        "Greece" to "GR",
        "Turkey" to "TR",
        "Japan" to "JP"
    )

    // Initial Egypt city/destination taxonomy (viewport centers for map navigation; never used as fake business listings)
    val egyptCities = listOf(
        EgyptCityTaxonomy("cairo", "Cairo", "القاهرة", "Nile Delta & Capital", 30.0444, 31.2357),
        EgyptCityTaxonomy("giza", "Giza", "الجيزة", "Greater Cairo Plateau", 29.9792, 31.1342),
        EgyptCityTaxonomy("alexandria", "Alexandria", "الإسكندرية", "Mediterranean Coast", 31.2001, 29.9187),
        EgyptCityTaxonomy("sharm_el_sheikh", "Sharm El Sheikh", "شرم الشيخ", "South Sinai / Red Sea", 27.9158, 34.3299),
        EgyptCityTaxonomy("hurghada", "Hurghada", "الغردقة", "Red Sea Governorate", 27.2579, 33.8116),
        EgyptCityTaxonomy("dahab", "Dahab", "دهب", "South Sinai Coast", 28.5091, 34.5136),
        EgyptCityTaxonomy("marsa_alam", "Marsa Alam", "مرسى علم", "Southern Red Sea", 25.0676, 34.8789),
        EgyptCityTaxonomy("luxor", "Luxor", "الأقصر", "Upper Egypt", 25.6872, 32.6396),
        EgyptCityTaxonomy("aswan", "Aswan", "أسوان", "Southern Nile", 24.0889, 32.8998),
        EgyptCityTaxonomy("el_gouna", "El Gouna", "الجونة", "Red Sea Lagoon", 27.3941, 33.6782),
        EgyptCityTaxonomy("siwa", "Siwa", "سيوة", "Western Desert Oasis", 29.2032, 25.5195),
        EgyptCityTaxonomy("north_coast", "North Coast", "الساحل الشمالي", "Mediterranean Sahel", 30.9756, 28.4972)
    )

    val worldLanguages = listOf(
        WorldLanguage("ar-EG", "Egyptian Arabic", "العربية المصرية", isRtl = true, isCoreLiveSupported = true),
        WorldLanguage("en", "English", "English", isRtl = false, isCoreLiveSupported = true),
        WorldLanguage("ru", "Russian", "Русский", isRtl = false, isCoreLiveSupported = true),
        WorldLanguage("fr", "French", "Français", isRtl = false, isCoreLiveSupported = true),
        WorldLanguage("ar-MSA", "Modern Standard Arabic", "العربية الفصحى", isRtl = true, isCoreLiveSupported = true),
        WorldLanguage("es", "Spanish", "Español", isRtl = false, isCoreLiveSupported = false),
        WorldLanguage("de", "German", "Deutsch", isRtl = false, isCoreLiveSupported = false),
        WorldLanguage("it", "Italian", "Italiano", isRtl = false, isCoreLiveSupported = false),
        WorldLanguage("tr", "Turkish", "Türkçe", isRtl = false, isCoreLiveSupported = false),
        WorldLanguage("zh", "Mandarin Chinese", "中文", isRtl = false, isCoreLiveSupported = false),
        WorldLanguage("ja", "Japanese", "日本語", isRtl = false, isCoreLiveSupported = false),
        WorldLanguage("pt", "Portuguese", "Português", isRtl = false, isCoreLiveSupported = false)
    )

    val countryDialCodes = listOf(
        CountryDialInfo("Egypt", "EG", "+20"),
        CountryDialInfo("United Arab Emirates", "AE", "+971"),
        CountryDialInfo("Saudi Arabia", "SA", "+966"),
        CountryDialInfo("France", "FR", "+33"),
        CountryDialInfo("Russia", "RU", "+7"),
        CountryDialInfo("United Kingdom", "GB", "+44"),
        CountryDialInfo("United States", "US", "+1"),
        CountryDialInfo("Canada", "CA", "+1"),
        CountryDialInfo("Germany", "DE", "+49"),
        CountryDialInfo("Italy", "IT", "+39"),
        CountryDialInfo("Spain", "ES", "+34"),
        CountryDialInfo("Greece", "GR", "+30"),
        CountryDialInfo("Turkey", "TR", "+90"),
        CountryDialInfo("Japan", "JP", "+81"),
        CountryDialInfo("Brazil", "BR", "+55"),
        CountryDialInfo("India", "IN", "+91"),
        CountryDialInfo("China", "CN", "+86"),
        CountryDialInfo("Morocco", "MA", "+212"),
        CountryDialInfo("Jordan", "JO", "+962")
    )

    val communityInterests = listOf(
        "Language Exchange",
        "Culture & History",
        "Local Food",
        "Photography",
        "Remote Work",
        "Travel & Coast",
        "Tech & Design",
        "Music & Art",
        "University & Study"
    )

    fun dialInfoForCountry(countryName: String): CountryDialInfo =
        countryDialCodes.firstOrNull { it.countryName.equals(countryName.trim(), ignoreCase = true) }
            ?: countryDialCodes.first()

    fun formatInternationalPhone(dialCode: String, nationalNumber: String): String {
        val cleanCode = dialCode.trim().let { if (it.startsWith("+")) it else "+$it" }
        val digits = nationalNumber.filter { it.isDigit() }.trimStart('0')
        return if (digits.isBlank()) "" else "$cleanCode$digits"
    }

    fun isValidInternationalPhone(dialCode: String, nationalNumber: String): Boolean {
        val formatted = formatInternationalPhone(dialCode, nationalNumber)
        if (formatted.isBlank()) return true // phone is optional
        return formatted.matches(Regex("^\\+[1-9][0-9]{6,14}$"))
    }

    fun languageDisplayLabel(code: String): String =
        worldLanguages.firstOrNull { it.code.equals(code, ignoreCase = true) }
            ?.let { "${it.nativeName} (${it.englishName})" }
            ?: code

    fun isRtlLanguageCode(code: String): Boolean =
        code.startsWith("ar", ignoreCase = true) ||
            worldLanguages.any { it.code.equals(code, ignoreCase = true) && it.isRtl }
}

data class WorldLanguage(
    val code: String,
    val englishName: String,
    val nativeName: String,
    val isRtl: Boolean,
    val isCoreLiveSupported: Boolean
)

data class CountryDialInfo(
    val countryName: String,
    val iso2: String,
    val dialCode: String
)

@Keep
@IgnoreExtraProperties
data class UserProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "",
    val accountMode: String = "GLOBAL_MEMBER", // GLOBAL_MEMBER | TRAVELER | LOCAL_PROVIDER
    val preferredLanguage: String = "en",
    val nativeLanguage: String = "en",
    val spokenLanguages: List<String> = emptyList(),
    val learningLanguages: List<String> = emptyList(),
    val interests: List<String> = emptyList(),
    val country: String = "Egypt",
    val city: String = "",
    val photoUri: String = "",
    val bio: String = "",
    val profileVisibility: String = "PUBLIC", // PUBLIC | CONNECTIONS_ONLY | PRIVATE
    val openToLanguageExchange: Boolean = true,
    val privacyConsent: Boolean = true,
    val retainTranslationHistory: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toCreateMap(): Map<String, Any> = mapOf(
        "userId" to userId,
        "displayName" to displayName.ifBlank { "NILORA Member" },
        "accountMode" to accountMode,
        "preferredLanguage" to preferredLanguage,
        "nativeLanguage" to nativeLanguage.ifBlank { preferredLanguage },
        "spokenLanguages" to spokenLanguages,
        "learningLanguages" to learningLanguages,
        "interests" to interests,
        "country" to country.ifBlank { "Egypt" },
        "city" to city,
        "photoUri" to photoUri,
        "bio" to bio,
        "profileVisibility" to profileVisibility,
        "openToLanguageExchange" to openToLanguageExchange,
        "privacyConsent" to privacyConsent,
        "retainTranslationHistory" to retainTranslationHistory,
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
    )
}

@Keep
@IgnoreExtraProperties
data class UserPrivateInfo(
    @DocumentId val id: String = "",
    val userId: String = "",
    val email: String = "",
    val phoneCountryCode: String = "+20",
    val phone: String = "",
    val emergencyContact: String = "",
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class FavoriteItem(
    @DocumentId val id: String = "",
    val userId: String = "",
    val targetId: String = "",
    val targetType: String = "DESTINATION",
    val title: String = "",
    val cityName: String = "",
    val category: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class TravelNote(
    @DocumentId val id: String = "",
    val userId: String = "",
    val title: String = "",
    val content: String = "",
    val cityName: String = "",
    val tripDate: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class AppNotification(
    @DocumentId val id: String = "",
    val userId: String = "",
    val title: String = "",
    val message: String = "",
    val category: String = "SYSTEM",
    val readStatus: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class Destination(
    @DocumentId val id: String = "",
    val title: String = "",
    val countryCode: String = "EG",
    val cityName: String = "",
    val category: String = "HISTORICAL",
    val description: String = "",
    val latitude: Double? = null,
    val longitude: Double? = null,
    val verifiedCoordinates: Boolean = false,
    val facilities: List<String> = emptyList(),
    val operatingHours: String = "",
    val priceInfo: String = "",
    val safetyWarnings: String = "",
    val accessibilityInfo: String = "",
    val officialWebsite: String = "",
    val contactInfo: String = "",
    val photoUrl: String = "",
    val sourceName: String = "",
    val sourceUrl: String = "",
    val lastVerifiedAt: String = "",
    val verificationStatus: String = "PENDING", // PENDING | APPROVED | REJECTED
    val submittedBy: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class ActivityListing(
    @DocumentId val id: String = "",
    val title: String = "",
    val providerId: String = "",
    val providerName: String = "",
    val countryCode: String = "EG",
    val cityName: String = "",
    val category: String = "ADVENTURE",
    val description: String = "",
    val durationLabel: String = "",
    val priceAmount: Double? = null,
    val currency: String = "EGP",
    val priceOnRequest: Boolean = true,
    val availabilityStatus: String = "ON_REQUEST", // AVAILABLE | ON_REQUEST | UNAVAILABLE
    val includedServices: List<String> = emptyList(),
    val exclusions: List<String> = emptyList(),
    val cancellationPolicy: String = "",
    val safetyRequirements: String = "",
    val latitude: Double? = null,
    val longitude: Double? = null,
    val verifiedCoordinates: Boolean = false,
    val sourceName: String = "",
    val verificationStatus: String = "PENDING",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class ProviderProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val fullName: String = "",
    val serviceType: String = "TOUR_GUIDE",
    val languages: List<String> = emptyList(),
    val citiesCovered: List<String> = emptyList(),
    val bio: String = "",
    val licenseNumber: String = "",
    val documentSummary: String = "",
    val documentRefUri: String = "",
    val verificationStatus: String = "PENDING",
    val reviewedBy: String = "",
    val rejectionReason: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class BookingRecord(
    @DocumentId val id: String = "",
    val bookingReference: String = "",
    val travelerId: String = "",
    val travelerName: String = "",
    val providerId: String = "",
    val providerName: String = "",
    val activityId: String = "",
    val activityTitle: String = "",
    val requestedDate: String = "",
    val participantsCount: Long = 1L,
    val notes: String = "",
    val status: String = "REQUEST_SUBMITTED", // REQUEST_SUBMITTED | AWAITING_PROVIDER_CONFIRMATION | CONFIRMED | REJECTED | CANCELLED
    val cancellationReason: String = "",
    val paymentStatus: String = "NOT_CONFIGURED",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class ConversationRoom(
    @DocumentId val id: String = "",
    val roomCode: String = "",
    val hostId: String = "",
    val hostName: String = "",
    val hostLanguage: String = "en",
    val guestId: String = "",
    val guestName: String = "",
    val guestLanguage: String = "ar",
    val memberUids: List<String> = emptyList(),
    val status: String = "WAITING", // WAITING | ACTIVE | ENDED
    val retainTranscript: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class ConversationMessage(
    @DocumentId val id: String = "",
    val roomCode: String = "",
    val memberUids: List<String> = emptyList(),
    val senderId: String = "",
    val senderName: String = "",
    val sourceLanguage: String = "en",
    val targetLanguage: String = "ar",
    val originalText: String = "",
    val translatedText: String = "",
    val confidenceNote: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class ContentReport(
    @DocumentId val id: String = "",
    val reporterId: String = "",
    val targetType: String = "DESTINATION",
    val targetId: String = "",
    val reason: String = "",
    val details: String = "",
    val status: String = "OPEN",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class DataSourceRecord(
    @DocumentId val id: String = "",
    val name: String = "",
    val providerType: String = "",
    val status: String = "AWAITING_CREDENTIALS",
    val notes: String = "",
    val updatedBy: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class AuditLog(
    @DocumentId val id: String = "",
    val actorId: String = "",
    val actorEmail: String = "",
    val action: String = "",
    val targetCollection: String = "",
    val targetId: String = "",
    val summary: String = "",
    val severity: String = "INFO",
    @ServerTimestamp val createdAt: Timestamp? = null
)

// --- PHASE 2: GLOBAL COMMUNITY, 1:1 MESSAGING & MULTILINGUAL GROUPS ---

@Keep
@IgnoreExtraProperties
data class PublicProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "",
    val country: String = "Egypt",
    val city: String = "",
    val nativeLanguage: String = "en",
    val spokenLanguages: List<String> = emptyList(),
    val learningLanguages: List<String> = emptyList(),
    val interests: List<String> = emptyList(),
    val photoUri: String = "",
    val bio: String = "",
    val profileVisibility: String = "PUBLIC", // PUBLIC | CONNECTIONS_ONLY | PRIVATE
    val openToLanguageExchange: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class BlockedUserRecord(
    @DocumentId val id: String = "",
    val blockerId: String = "",
    val blockedUserId: String = "",
    val blockedUserName: String = "",
    val reason: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class ConnectionRequest(
    @DocumentId val id: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val senderCountry: String = "",
    val senderNativeLanguage: String = "en",
    val receiverId: String = "",
    val receiverName: String = "",
    val receiverCountry: String = "",
    val receiverNativeLanguage: String = "en",
    val memberUids: List<String> = emptyList(),
    val introMessage: String = "",
    val status: String = "PENDING", // PENDING | ACCEPTED | DECLINED | CANCELLED
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class DirectConversation(
    @DocumentId val id: String = "",
    val participantIds: List<String> = emptyList(),
    val participantNames: Map<String, String> = emptyMap(),
    val participantLanguages: Map<String, String> = emptyMap(),
    val lastMessagePreview: String = "",
    val lastMessageSenderId: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun partnerId(myUid: String): String =
        participantIds.firstOrNull { it != myUid }.orEmpty()

    fun partnerName(myUid: String): String {
        val pid = partnerId(myUid)
        return participantNames[pid].orEmpty().ifBlank { "NILORA Member" }
    }

    fun partnerLanguage(myUid: String): String {
        val pid = partnerId(myUid)
        return participantLanguages[pid].orEmpty().ifBlank { "en" }
    }
}

@Keep
@IgnoreExtraProperties
data class DirectMessage(
    @DocumentId val id: String = "",
    val conversationId: String = "",
    val participantIds: List<String> = emptyList(),
    val senderId: String = "",
    val senderName: String = "",
    val messageType: String = "TEXT", // TEXT | VOICE_NOTE
    val originalText: String = "",
    val sourceLanguage: String = "en",
    val translations: Map<String, String> = emptyMap(),
    val translationConfidence: String = "",
    val translationNote: String = "",
    val audioBase64: String = "",
    val audioDurationMs: Long = 0L,
    val audioMimeType: String = "",
    val deliveryStatus: String = "SENT", // SENT | DELIVERED | READ
    @ServerTimestamp val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class GroupRoom(
    @DocumentId val id: String = "",
    val name: String = "",
    val description: String = "",
    val ownerId: String = "",
    val moderatorIds: List<String> = emptyList(),
    val memberIds: List<String> = emptyList(),
    val bannedIds: List<String> = emptyList(),
    val primaryLanguages: List<String> = emptyList(),
    val joinPolicy: String = "OPEN", // OPEN | REQUEST_APPROVAL | INVITE_ONLY
    val autoTranslateDefault: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class GroupJoinRequest(
    @DocumentId val id: String = "",
    val groupId: String = "",
    val userId: String = "",
    val userName: String = "",
    val userCountry: String = "",
    val nativeLanguage: String = "en",
    val message: String = "",
    val requestType: String = "JOIN_REQUEST", // JOIN_REQUEST | INVITE
    val status: String = "PENDING", // PENDING | APPROVED | DECLINED
    val invitedBy: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class GroupMessage(
    @DocumentId val id: String = "",
    val groupId: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val originalText: String = "",
    val sourceLanguage: String = "en",
    val translations: Map<String, String> = emptyMap(),
    val translationNote: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)
