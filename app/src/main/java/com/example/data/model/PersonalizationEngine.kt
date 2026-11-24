package com.example.data.model

/**
 * Transparent, deterministic personalization ranker.
 * Uses only explicit user-controlled signals: interests, spoken/learning languages,
 * preferred city, and saved places. Never uses hidden profiling or fake scores.
 */
object PersonalizationEngine {

    data class RankedItem<T>(
        val item: T,
        val score: Int,
        val matchReason: String
    )

    fun rankDestinations(
        destinations: List<Destination>,
        profile: UserProfile?,
        savedIds: Set<String>,
        enabled: Boolean
    ): List<RankedItem<Destination>> {
        if (!enabled) {
            return destinations.map { RankedItem(it, 0, "Personalization paused") }
        }
        val userCity = profile?.city.orEmpty().trim()
        val interests = profile?.interests.orEmpty().map { it.lowercase() }

        return destinations.map { dest ->
            var score = 0
            val reasons = mutableListOf<String>()

            if (userCity.isNotBlank() && dest.cityName.contains(userCity, ignoreCase = true)) {
                score += 30
                reasons.add("Near $userCity")
            }
            if (dest.id in savedIds) {
                score += 20
                reasons.add("Saved")
            }
            val text = "${dest.title} ${dest.category} ${dest.description}".lowercase()
            val matchedInterest = interests.firstOrNull { interest ->
                val token = interest.substringBefore(" ").trim()
                token.length >= 3 && text.contains(token)
            }
            if (matchedInterest != null) {
                score += 25
                reasons.add("Matches $matchedInterest")
            }
            if (dest.verifiedCoordinates) {
                score += 5
            }
            RankedItem(dest, score, reasons.joinToString(" • "))
        }.sortedByDescending { it.score }
    }

    fun rankAdventures(
        adventures: List<AdventureExperience>,
        profile: UserProfile?,
        enabled: Boolean
    ): List<RankedItem<AdventureExperience>> {
        if (!enabled) {
            return adventures.map { RankedItem(it, 0, "Personalization paused") }
        }
        val userCity = profile?.city.orEmpty().trim()
        val interests = profile?.interests.orEmpty().map { it.lowercase() }

        return adventures.map { adv ->
            var score = 0
            val reasons = mutableListOf<String>()
            if (userCity.isNotBlank() && adv.destinationName.contains(userCity, ignoreCase = true)) {
                score += 30
                reasons.add("Your city")
            }
            val text = "${adv.title} ${adv.subtitle} ${adv.adventureType.labelEn} ${adv.destinationName}".lowercase()
            val matched = interests.firstOrNull { interest ->
                val token = interest.substringBefore(" ").trim()
                token.length >= 3 && text.contains(token)
            }
            if (matched != null) {
                score += 25
                reasons.add(matched)
            }
            if (adv.isSecretSpot) {
                score += 5
            }
            RankedItem(adv, score, reasons.joinToString(" • "))
        }.sortedByDescending { it.score }
    }

    fun rankEvents(
        events: List<RealityEvent>,
        profile: UserProfile?,
        enabled: Boolean
    ): List<RankedItem<RealityEvent>> {
        if (!enabled) return events.map { RankedItem(it, 0, "Personalization paused") }
        val userCity = profile?.city.orEmpty().trim()
        val interests = profile?.interests.orEmpty().map { it.lowercase() }
        return events.map { ev ->
            var score = 0
            val reasons = mutableListOf<String>()
            if (userCity.isNotBlank() && ev.cityName.contains(userCity, ignoreCase = true)) {
                score += 35
                reasons.add("In $userCity")
            }
            if (ev.freeVerified) {
                score += 10
                reasons.add("Free Verified")
            }
            val text = "${ev.title} ${ev.category} ${ev.description}".lowercase()
            val matched = interests.firstOrNull { interest ->
                val token = interest.substringBefore(" ").trim()
                token.length >= 3 && text.contains(token)
            }
            if (matched != null) {
                score += 20
                reasons.add(matched)
            }
            RankedItem(ev, score, reasons.joinToString(" • "))
        }.sortedByDescending { it.score }
    }

    fun rankTeamActivities(
        activities: List<TeamActivityPost>,
        profile: UserProfile?,
        enabled: Boolean
    ): List<RankedItem<TeamActivityPost>> {
        if (!enabled) return activities.map { RankedItem(it, 0, "Personalization paused") }
        val userCity = profile?.city.orEmpty().trim()
        val userCountry = profile?.country.orEmpty().trim()
        val interests = profile?.interests.orEmpty().map { it.lowercase() }

        return activities.map { post ->
            var score = 0
            val reasons = mutableListOf<String>()
            if (userCity.isNotBlank() && post.cityName.contains(userCity, ignoreCase = true)) {
                score += 30
                reasons.add("Near $userCity")
            } else if (userCountry.isNotBlank() && post.countryName.contains(userCountry, ignoreCase = true)) {
                score += 15
                reasons.add(post.countryName)
            }
            val text = "${post.title} ${post.subCategory} ${post.domain}".lowercase()
            val matched = interests.firstOrNull { interest ->
                val token = interest.substringBefore(" ").trim()
                token.length >= 3 && text.contains(token)
            }
            if (matched != null) {
                score += 20
                reasons.add(matched)
            }
            if (post.participantIds.size < post.maxParticipants) score += 5
            RankedItem(post, score, reasons.joinToString(" • "))
        }.sortedByDescending { it.score }
    }

    fun rankCrews(
        crews: List<NiloraCrew>,
        profile: UserProfile?,
        enabled: Boolean
    ): List<RankedItem<NiloraCrew>> {
        if (!enabled) return crews.map { RankedItem(it, 0, "Personalization paused") }
        val userCity = profile?.city.orEmpty().trim()
        val interests = profile?.interests.orEmpty().map { it.lowercase() }.toSet()

        return crews.map { crew ->
            var score = 0
            val reasons = mutableListOf<String>()
            val sharedInterest = crew.sharedInterests.firstOrNull { it.lowercase() in interests }
            if (sharedInterest != null) {
                score += 30
                reasons.add(sharedInterest)
            }
            if (userCity.isNotBlank() && crew.focusDestination.contains(userCity, ignoreCase = true)) {
                score += 20
                reasons.add(crew.focusDestination)
            }
            RankedItem(crew, score, reasons.joinToString(" • "))
        }.sortedByDescending { it.score }
    }
}
