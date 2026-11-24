package com.example.data.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

enum class PostcardCategory(
    val key: String,
    val labelEn: String,
    val labelAr: String,
    val labelFr: String,
    val labelRu: String
) {
    STORY("STORY", "Cultural Story", "قصة ثقافية", "Histoire culturelle", "Культурная история"),
    QUESTION("QUESTION", "World Question", "سؤال للعالم", "Question au monde", "Вопрос миру"),
    EXPRESSION("EXPRESSION", "Local Expression", "تعبير محلي", "Expression locale", "Местное выражение"),
    FOOD("FOOD", "Food & Table", "الطعام والمائدة", "Cuisine & table", "Еда и традиции"),
    DAILY_LIFE("DAILY_LIFE", "Daily Moment", "لحظة يومية", "Moment du quotidien", "Момент дня"),
    TRADITION("TRADITION", "Tradition", "تراث وتقاليد", "Tradition", "Традиция");

    fun label(lang: NiloraLanguage): String = when (lang) {
        NiloraLanguage.ARABIC -> labelAr
        NiloraLanguage.FRENCH -> labelFr
        NiloraLanguage.RUSSIAN -> labelRu
        NiloraLanguage.ENGLISH -> labelEn
    }

    companion object {
        fun fromKey(key: String?): PostcardCategory =
            entries.firstOrNull { it.key == key } ?: STORY
    }
}

enum class SwapPromptCategory(
    val key: String,
    val titleEn: String,
    val defaultPromptEn: String
) {
    TEACH_PHRASE(
        "TEACH_PHRASE",
        "Teach a Warm Local Phrase",
        "Teach me one warm everyday phrase from your home and when people actually say it."
    ),
    MORNING_ROUTINE(
        "MORNING_ROUTINE",
        "Morning Rituals",
        "What does the first hour of a normal morning sound, taste, and feel like in your city?"
    ),
    FOOD_TRADITION(
        "FOOD_TRADITION",
        "A Dish With a Memory",
        "Share one homemade or street food dish from your culture and the story behind it."
    ),
    LOCAL_SECRET(
        "LOCAL_SECRET",
        "Quiet Local Corner",
        "Describe a peaceful public neighborhood corner or viewpoint locals love that visitors rarely notice."
    ),
    MUSIC_SONG(
        "MUSIC_SONG",
        "Music & Mood",
        "Share a song lyric, rhythm, or traditional sound that instantly reminds you of home."
    ),
    EVERYDAY_TRADITION(
        "EVERYDAY_TRADITION",
        "Everyday Hospitality",
        "How do people welcome a guest or neighbor into their space where you live?"
    );

    companion object {
        fun fromKey(key: String?): SwapPromptCategory =
            entries.firstOrNull { it.key == key } ?: TEACH_PHRASE
    }
}

enum class QuestMissionType(
    val key: String,
    val titleEn: String,
    val defaultObjectiveEn: String,
    val suggestedGoal: Long
) {
    FIVE_SIMILARITIES(
        "FIVE_SIMILARITIES",
        "5 Cultural Similarities",
        "Collect five surprising similarities in daily life across different countries.",
        5L
    ),
    THREE_EXPRESSIONS(
        "THREE_EXPRESSIONS",
        "3 Everyday Expressions",
        "Exchange three untranslatable or heartwarming local expressions with real voice or context.",
        3L
    ),
    LOCAL_TRADITION(
        "LOCAL_TRADITION",
        "Understand One Local Tradition",
        "Explain one celebration, tea/coffee custom, or family tradition from each participant's country.",
        3L
    ),
    FOOD_STORY(
        "FOOD_STORY",
        "Share Bread Across Borders",
        "Document four comfort foods from different homes and why they bring people together.",
        4L
    ),
    EVERYDAY_COMPARE(
        "EVERYDAY_COMPARE",
        "Morning to Night Compare",
        "Compare three everyday habits — commuting, greetings, or evening walks — across borders.",
        3L
    ),
    CUSTOM(
        "CUSTOM",
        "Custom Shared Quest",
        "Propose a genuine cross-cultural question or creative challenge to complete together.",
        3L
    );

    companion object {
        fun fromKey(key: String?): QuestMissionType =
            entries.firstOrNull { it.key == key } ?: THREE_EXPRESSIONS
    }
}

enum class MemoryType(val key: String, val labelEn: String) {
    WORLD_POSTCARD("WORLD_POSTCARD", "World Postcard"),
    CULTURE_SWAP("CULTURE_SWAP", "Culture Swap"),
    WORLD_QUEST("WORLD_QUEST", "World Quest"),
    CONVERSATION("CONVERSATION", "Conversation Moment"),
    VOICE_MEMORY("VOICE_MEMORY", "Voice Memory");

    companion object {
        fun fromKey(key: String?): MemoryType =
            entries.firstOrNull { it.key == key } ?: VOICE_MEMORY
    }
}

enum class MemoryVisibility(val key: String, val labelEn: String) {
    PRIVATE("PRIVATE", "Private (Only Me)"),
    SHARED_WITH_PARTICIPANTS("SHARED_WITH_PARTICIPANTS", "Shared with Participants"),
    PUBLIC("PUBLIC", "Public Story");

    companion object {
        fun fromKey(key: String?): MemoryVisibility =
            entries.firstOrNull { it.key == key } ?: PRIVATE
    }
}

@Keep
@IgnoreExtraProperties
data class WorldPostcard(
    @DocumentId val id: String = "",
    val authorId: String = "",
    val authorName: String = "",
    val country: String = "Egypt",
    val city: String = "",
    val languageCode: String = "ar-EG",
    val category: String = "STORY",
    val title: String = "",
    val storyText: String = "",
    val localExpression: String = "",
    val questionPrompt: String = "",
    val photoBase64: String = "",
    val audioBase64: String = "",
    val audioDurationMs: Long = 0L,
    val translations: Map<String, String> = emptyMap(),
    val translationNote: String = "",
    val participantIds: List<String> = emptyList(),
    val countriesChain: List<String> = emptyList(),
    val contributionsCount: Long = 0L,
    val allowReplies: Boolean = true,
    val allowPrivateConnect: Boolean = true,
    val visibility: String = "PUBLIC",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class PostcardContribution(
    @DocumentId val id: String = "",
    val postcardId: String = "",
    val authorId: String = "",
    val authorName: String = "",
    val country: String = "",
    val city: String = "",
    val languageCode: String = "en",
    val responseText: String = "",
    val localExpression: String = "",
    val photoBase64: String = "",
    val audioBase64: String = "",
    val audioDurationMs: Long = 0L,
    val translations: Map<String, String> = emptyMap(),
    val translationNote: String = "",
    val allowPrivateConnect: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class CultureSwap(
    @DocumentId val id: String = "",
    val creatorId: String = "",
    val creatorName: String = "",
    val creatorCountry: String = "",
    val creatorLanguage: String = "en",
    val promptCategory: String = "TEACH_PHRASE",
    val promptText: String = "",
    val participationMode: String = "TWO_PERSON", // TWO_PERSON | GROUP
    val maxParticipants: Long = 2L,
    val participantIds: List<String> = emptyList(),
    val participantNames: Map<String, String> = emptyMap(),
    val participantCountries: Map<String, String> = emptyMap(),
    val entriesCount: Long = 0L,
    val status: String = "OPEN", // OPEN | EXCHANGED | COMPLETED
    val allowPrivateConnect: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class CultureSwapEntry(
    @DocumentId val id: String = "",
    val swapId: String = "",
    val authorId: String = "",
    val authorName: String = "",
    val country: String = "",
    val languageCode: String = "en",
    val contentText: String = "",
    val expressionSample: String = "",
    val photoBase64: String = "",
    val audioBase64: String = "",
    val audioDurationMs: Long = 0L,
    val translations: Map<String, String> = emptyMap(),
    val translationNote: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class WorldQuest(
    @DocumentId val id: String = "",
    val creatorId: String = "",
    val creatorName: String = "",
    val missionType: String = "THREE_EXPRESSIONS",
    val title: String = "",
    val objective: String = "",
    val targetGoal: Long = 3L,
    val participantIds: List<String> = emptyList(),
    val participantCountries: List<String> = emptyList(),
    val contributionsCount: Long = 0L,
    val status: String = "ACTIVE", // ACTIVE | COMPLETED
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun progressFraction(): Float =
        if (targetGoal <= 0L) 0f else (contributionsCount.toFloat() / targetGoal.toFloat()).coerceIn(0f, 1f)

    fun isGoalMet(): Boolean = contributionsCount >= targetGoal
}

@Keep
@IgnoreExtraProperties
data class WorldQuestContribution(
    @DocumentId val id: String = "",
    val questId: String = "",
    val authorId: String = "",
    val authorName: String = "",
    val country: String = "",
    val languageCode: String = "en",
    val contentText: String = "",
    val photoBase64: String = "",
    val audioBase64: String = "",
    val audioDurationMs: Long = 0L,
    val translations: Map<String, String> = emptyMap(),
    val translationNote: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class NiloraMemory(
    @DocumentId val id: String = "",
    val ownerId: String = "",
    val ownerName: String = "",
    val memoryType: String = "VOICE_MEMORY",
    val sourceId: String = "",
    val title: String = "",
    val summaryText: String = "",
    val originalLanguage: String = "en",
    val translations: Map<String, String> = emptyMap(),
    val countries: List<String> = emptyList(),
    val partnerIds: List<String> = emptyList(),
    val partnerNames: List<String> = emptyList(),
    val placeLabel: String = "",
    val photoBase64: String = "",
    val audioBase64: String = "",
    val audioDurationMs: Long = 0L,
    val retainAudioConsent: Boolean = false,
    val visibility: String = "PRIVATE", // PRIVATE | SHARED_WITH_PARTICIPANTS | PUBLIC
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

object SafeMeetupPolicy {
    val guidelines = listOf(
        "Meet only after mutual consent and translated introduction in NILORA.",
        "Always choose busy, well-lit public places during daytime hours.",
        "Never share exact home addresses or live private GPS coordinates.",
        "Share your meetup venue and time with a trusted friend or family member.",
        "You can block or report any participant at any stage."
    )

    val publicVenueSuggestions = listOf(
        "Public Museum Courtyard",
        "Busy Waterfront Promenade",
        "Cultural Library Café",
        "University Plaza",
        "Verified Tourism Landmark Entrance"
    )
}
