package com.example.ui.passport

/**
 * One collectible travel stamp: a city the traveler has physically visited
 * (GPS-verified) or unlocked through a quest.
 *
 * Inspired by passport stamps and Bottled's slow-travel letters: stamps are
 * earned in the real world, not tapped out in a feed.
 */
data class CityStamp(
    val cityId: String,
    val nameEn: String,
    val nameAr: String,
    val earnedAtEpochMs: Long? = null,
) {
    val isEarned: Boolean get() = earnedAtEpochMs != null
}

/**
 * Traveler rank derived from collected stamps. Ordered by [minStamps].
 */
enum class PassportLevel(val minStamps: Int, val titleEn: String, val titleAr: String) {
    NEWCOMER(0, "Newcomer", "وافد جديد"),
    EXPLORER(1, "Explorer", "مستكشف"),
    ADVENTURER(4, "Adventurer", "مغامر"),
    NILE_MASTER(8, "Nile Master", "سيّد النيل"),
    GLOBAL_SOUL(12, "Global Soul", "روح العالم");

    companion object {
        fun forStampCount(count: Int): PassportLevel =
            values().lastOrNull { count >= it.minStamps } ?: NEWCOMER

        fun next(level: PassportLevel): PassportLevel? =
            values().getOrNull(level.ordinal + 1)
    }
}

/**
 * The inaugural Egypt passport: one stamp per launch city.
 * IDs stay stable because earned stamps will reference them in Firestore.
 */
object EgyptPassport {
    const val TOTAL_STAMPS = 12

    val cities: List<CityStamp> = listOf(
        CityStamp("cairo", "Cairo", "القاهرة"),
        CityStamp("giza", "Giza", "الجيزة"),
        CityStamp("alexandria", "Alexandria", "الإسكندرية"),
        CityStamp("sharm_el_sheikh", "Sharm El Sheikh", "شرم الشيخ"),
        CityStamp("hurghada", "Hurghada", "الغردقة"),
        CityStamp("dahab", "Dahab", "دهب"),
        CityStamp("marsa_alam", "Marsa Alam", "مرسى علم"),
        CityStamp("luxor", "Luxor", "الأقصر"),
        CityStamp("aswan", "Aswan", "أسوان"),
        CityStamp("el_gouna", "El Gouna", "الجونة"),
        CityStamp("siwa", "Siwa", "سيوة"),
        CityStamp("north_coast", "North Coast", "الساحل الشمالي"),
    )
}
