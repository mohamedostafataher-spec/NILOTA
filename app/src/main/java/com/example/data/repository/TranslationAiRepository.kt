package com.example.data.repository

import android.util.Log
import com.example.data.model.*
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withTimeout
import org.json.JSONObject

data class StructuredTranslationResult(
    val translatedText: String,
    val confidence: String, // HIGH | MEDIUM | LOW
    val ambiguityWarning: String,
    val detectedSourceLanguage: String
)

class TranslationAiRepository {
    companion object {
        private const val TAG = "NiloraAiRepo"
        private const val TIMEOUT_MS = 60_000L
    }

    private val translationSchema = Schema.obj(
        properties = mapOf(
            "translatedText" to Schema.string(description = "Accurate natural translation into target language, or empty if unintelligible."),
            "confidence" to Schema.enumeration(
                values = listOf("HIGH", "MEDIUM", "LOW"),
                description = "Confidence in understanding the speech/text accurately."
            ),
            "ambiguityWarning" to Schema.string(description = "Explanation if speech was incomplete, noisy, ambiguous, or unintelligible; empty string if clear."),
            "detectedSourceLanguage" to Schema.string(description = "Detected language code or dialect note.")
        )
    )

    private val translationModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = translationSchema
                temperature = 0.2f
            },
            systemInstruction = content {
                text(
                    "You are NILORA's real-time travel voice translation engine. " +
                        "Translate accurately between Arabic (including Egyptian Arabic), English, Russian, and French. " +
                        "NEVER fabricate or guess missing meaning when input is garbled, noisy, incomplete, or nonsensical. " +
                        "If confidence is insufficient, set confidence='LOW', leave translatedText empty or minimal, and explain the issue clearly in ambiguityWarning."
                )
            }
        )
    }

    private val assistantModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            systemInstruction = content {
                text(
                    "You are the NILORA AI Travel Assistant (Created & Founded by Mohamed Mostafa Taher). " +
                        "Strict Grounding & Data Integrity Rules:\n" +
                        "1. Only state specific prices, opening hours, coordinates, or live availability if present in the Verified Context provided.\n" +
                        "2. Always distinguish clearly using short tags: [Verified Fact], [User-Provided], [AI Suggestion], or [Unverified / Unavailable].\n" +
                        "3. If verified business data for a city/category is empty or unavailable, honestly state that no verified listing is currently stored in NILORA's database and do NOT invent hotels, restaurants, prices, or schedules.\n" +
                        "4. Never claim to execute payments or confirm bookings directly; guide users to NILORA's Booking Request workflow.\n" +
                        "5. Support English, Arabic (including Egyptian dialect), French, and Russian seamlessly."
                )
            }
        )
    }

    suspend fun translate(
        inputText: String,
        sourceLang: NiloraLanguage,
        targetLang: NiloraLanguage
    ): Result<StructuredTranslationResult> {
        if (inputText.isBlank()) {
            return Result.failure(IllegalArgumentException("No speech or text captured to translate."))
        }
        return try {
            val prompt = """
                Source language expected: ${sourceLang.displayName} (${sourceLang.code})
                Target language: ${targetLang.displayName} (${targetLang.code})
                Utterance: "$inputText"
            """.trimIndent()

            val response = withTimeout(TIMEOUT_MS) {
                translationModel.generateContent(prompt)
            }
            val rawJson = response.text ?: throw IllegalStateException("Translation service returned empty response.")
            val json = JSONObject(rawJson)
            Result.success(
                StructuredTranslationResult(
                    translatedText = json.optString("translatedText", ""),
                    confidence = json.optString("confidence", "MEDIUM"),
                    ambiguityWarning = json.optString("ambiguityWarning", ""),
                    detectedSourceLanguage = json.optString("detectedSourceLanguage", sourceLang.code)
                )
            )
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Translation timed out", e)
            Result.failure(IllegalStateException("Translation timed out after 60 seconds. Please check connection and retry."))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Translation failed", e)
            Result.failure(e)
        }
    }

    fun streamAssistantResponse(
        userQuery: String,
        responseLanguage: NiloraLanguage,
        verifiedDestinations: List<Destination>,
        verifiedActivities: List<ActivityListing>,
        verifiedProviders: List<ProviderProfile>,
        realityEvents: List<RealityEvent> = emptyList(),
        teamActivities: List<TeamActivityPost> = emptyList(),
        missions: List<RealityMission> = emptyList(),
        challenges: List<GlobalChallenge> = emptyList(),
        bridgeLinks: List<RealityBridgeLink> = emptyList()
    ): Flow<String> = flow {
        val contextSummary = buildString {
            appendLine("=== VERIFIED NILORA DATABASE CONTEXT ===")
            appendLine("Verified Destinations (${verifiedDestinations.size}):")
            verifiedDestinations.take(15).forEach { d ->
                appendLine("- ${d.title} | ${d.cityName} | ${d.category} | Hours: ${d.operatingHours.ifBlank { "Unavailable" }} | Price: ${d.priceInfo.ifBlank { "Unavailable" }} | Source: ${d.sourceName}")
            }
            appendLine("Verified Activities (${verifiedActivities.size}):")
            verifiedActivities.take(15).forEach { a ->
                val priceDesc = if (a.priceOnRequest || a.priceAmount == null) "Price on request" else "${a.priceAmount} ${a.currency}"
                appendLine("- ${a.title} | ${a.cityName} | Provider: ${a.providerName} | $priceDesc | Availability: ${a.availabilityStatus}")
            }
            appendLine("Verified Local Providers (${verifiedProviders.size}):")
            verifiedProviders.take(10).forEach { p ->
                appendLine("- ${p.fullName} | ${p.serviceType} | Languages: ${p.languages.joinToString()} | Cities: ${p.citiesCovered.joinToString()}")
            }
            appendLine("Reality Events (${realityEvents.size}):")
            realityEvents.take(10).forEach { ev ->
                appendLine("- ${ev.title} | ${ev.cityName} | Venue: ${ev.venueName} | Date: ${ev.startDateIso} | Tier: ${ev.verificationTier} | Entry: ${if (ev.freeVerified) "Free Verified" else ev.priceNote}")
            }
            appendLine("Open Go/Play/Team-Up & Impact Activities (${teamActivities.size}):")
            teamActivities.take(10).forEach { act ->
                appendLine("- ${act.title} | ${act.cityName} | Domain: ${act.domain}/${act.subCategory} | Spot: ${act.publicMeetingSpot} | ${act.participantIds.size}/${act.maxParticipants} joined | Status: ${act.teamStatus}")
            }
            appendLine("Missions (${missions.size}) & Global Challenges (${challenges.size}):")
            missions.take(5).forEach { m ->
                appendLine("- Mission: ${m.title} (${m.executionMode}) in ${m.cityName} (${m.submissionsCount}/${m.targetSteps})")
            }
            challenges.take(5).forEach { ch ->
                appendLine("- Challenge: ${ch.title} (${ch.challengeScope}) ${ch.entriesCount}/${ch.targetGoal}")
            }
            if (bridgeLinks.isNotEmpty()) {
                appendLine("User's Reality Bridge Plans (${bridgeLinks.size}):")
                bridgeLinks.take(5).forEach { b ->
                    appendLine("- ${b.virtualAdventureTitle} -> ${b.targetTitle} [Stage: ${b.stage}]")
                }
            }
            appendLine("=== END CONTEXT ===")
        }

        val prompt = """
            Respond in ${responseLanguage.displayName}.
            $contextSummary
            User Question: $userQuery
        """.trimIndent()

        val accumulated = StringBuilder()
        withTimeout(TIMEOUT_MS) {
            assistantModel.generateContentStream(prompt).collect { chunk ->
                val piece = chunk.text.orEmpty()
                if (piece.isNotEmpty()) {
                    accumulated.append(piece)
                    emit(accumulated.toString())
                }
            }
        }
        if (accumulated.isBlank()) {
            throw IllegalStateException("AI Assistant returned an empty response.")
        }
    }
}
