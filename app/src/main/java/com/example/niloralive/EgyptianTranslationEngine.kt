package com.example.niloralive

import android.util.Log
import com.example.data.model.NiloraLanguage
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import org.json.JSONObject

object TranslationValidator {
    private val numberRegex = Regex("""\d+(?:[.,]\d+)?""")

    fun validate(
        request: TranslationRequest,
        rawTranslatedText: String,
        confidenceLabel: String
    ): List<String> {
        val warnings = mutableListOf<String>()
        val trimmedOut = rawTranslatedText.trim()
        val trimmedIn = request.sourceText.trim()

        if (trimmedOut.isBlank()) {
            warnings.add("Translation output is empty.")
            return warnings
        }

        if (request.sourceLanguage != request.targetLanguage &&
            trimmedOut.equals(trimmedIn, ignoreCase = true) &&
            trimmedIn.length > 4 &&
            !trimmedIn.all { it.isDigit() || it.isWhitespace() }
        ) {
            warnings.add("Output matches source text without translation; please verify.")
        }

        val sourceNumbers = numberRegex.findAll(trimmedIn).map { it.value }.toList()
        if (sourceNumbers.isNotEmpty()) {
            val missingNumbers = sourceNumbers.filterNot { num ->
                trimmedOut.contains(num) || trimmedOut.contains(toArabicIndicDigits(num))
            }
            if (missingNumbers.isNotEmpty()) {
                warnings.add("Numeric value(s) ${missingNumbers.joinToString()} may not appear verbatim in translation — please verify numbers/prices/dates.")
            }
        }

        if (confidenceLabel == "LOW") {
            warnings.add("Low confidence recognition/translation; please review text.")
        }
        return warnings
    }

    fun toArabicIndicDigits(input: String): String {
        val arabicDigits = charArrayOf('٠', '١', '٢', '٣', '٤', '٥', '٦', '٧', '٨', '٩')
        return buildString {
            input.forEach { ch ->
                if (ch in '0'..'9') append(arabicDigits[ch - '0']) else append(ch)
            }
        }
    }
}

class EgyptianTranslationEngine : TranslationEngine {
    override val engineId: String = "firebase_ai_gemini_3_8_flash_egyptian"

    companion object {
        private const val TAG = "EgyptianTransEngine"
        private const val TIMEOUT_MS = 60_000L
    }

    private val schema = Schema.obj(
        properties = mapOf(
            "translatedText" to Schema.string(
                description = "Accurate conversational translation in the requested target language/dialect, or empty if source is unintelligible."
            ),
            "detectedSourceDialect" to Schema.string(
                description = "Best-effort tag: ar-EG (Egyptian Colloquial), ar-MSA (Modern Standard Arabic), ar-OTHER (Other dialect/uncertain), en, ru, fr, or MIXED."
            ),
            "confidence" to Schema.enumeration(
                values = listOf("HIGH", "MEDIUM", "LOW"),
                description = "Honest confidence in understanding source speech/text accurately."
            ),
            "ambiguityWarning" to Schema.string(
                description = "Note if source was incomplete, noisy, dialectally ambiguous, or mixed-language; empty string if clear."
            )
        )
    )

    private val egyptianModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = schema
                temperature = 0.2f
            },
            systemInstruction = content {
                text(
                    "You are NILORA LIVE's real-time universal multilingual & Egyptian Arabic translation engine.\n" +
                        "CORE PRIORITY — NATURAL EGYPTIAN COLLOQUIAL ARABIC (العامية المصرية / ar-EG):\n" +
                        "1. When target language is Arabic and mode is EGYPTIAN_COLLOQUIAL, ALWAYS output natural, warm, everyday Egyptian spoken Arabic — NOT stiff Modern Standard Arabic (الفصحى).\n" +
                        "   Style guidance example: 'Would you mind telling me where the nearest beach is?' -> 'ممكن تقولي أقرب شاطئ فين؟'\n" +
                        "2. CODE-SWITCHING, IDIOMS & INFORMAL SPEECH: Recognize Egyptian colloquial expressions, idioms (like 'على راسي', 'ماشي يا باشا', 'معلش', 'إيه الأخبار', 'يلا بينا'), Russian/French/English loanwords, and mixed Arabic-English sentences naturally without breaking meaning.\n" +
                        "3. PARTIAL OR FRAGMENTED PHRASES: When an utterance is an incomplete conversational fragment or informal quick phrase, translate its clear natural equivalent while honestly setting confidence='MEDIUM' or 'LOW' and noting any incompleteness in ambiguityWarning.\n" +
                        "4. CONTEXTUAL DOMAIN ADAPTATION: Adapt terminology cleanly to the provided conversation domain (Casual, Travel, Tourism, Sports, Group Activities, Cultural Exchange, or Adventure Instructions).\n" +
                        "5. Distinguish honestly in detectedSourceDialect between ar-EG, ar-MSA, ar-OTHER, en, ru, fr, or MIXED; never claim certainty when uncertain.\n" +
                        "6. PRESERVE STRICTLY: original meaning, conversational intent, questions, politeness, gender references if known, names, numbers, dates, addresses, currencies, negation, and technical terms.\n" +
                        "7. ZERO FABRICATION: Never invent missing words, never embellish, and never silently drop details. If input is garbled/unintelligible, set confidence='LOW', preserve uncertainty, and explain in ambiguityWarning."
                )
            }
        )
    }

    override suspend fun translate(request: TranslationRequest): Result<TranslationOutput> {
        if (request.sourceText.isBlank()) {
            return Result.failure(IllegalArgumentException("Cannot translate empty transcript."))
        }
        val startedAt = System.currentTimeMillis()
        return try {
            val targetDirective = when {
                request.targetLanguage == NiloraLanguage.ARABIC &&
                    request.arabicDialectMode == ArabicDialectMode.EGYPTIAN_COLLOQUIAL ->
                    "Egyptian Colloquial Arabic (ar-EG / العامية المصرية) — use natural spoken Egyptian phrasing, NOT formal MSA unless required for proper nouns/legal terms"
                request.targetLanguage == NiloraLanguage.ARABIC &&
                    request.arabicDialectMode == ArabicDialectMode.MODERN_STANDARD ->
                    "Modern Standard Arabic (ar-MSA / العربية الفصحى)"
                else -> "${request.targetLanguage.displayName} (${request.targetLanguage.code})"
            }

            val sourceHint = if (request.autoDetectSource) {
                "Auto-detect spoken/written language (hint: ${request.sourceLanguage.bcp47Tag}, may be ar-EG, en, ru, fr, or MIXED)"
            } else {
                "${request.sourceLanguage.displayName} (${request.sourceLanguage.bcp47Tag})"
            }

            val historyBlock = if (request.conversationContext.isEmpty()) {
                "None"
            } else {
                request.conversationContext.takeLast(4).joinToString("\n")
            }

            val prompt = """
                Expected Source Language: $sourceHint
                Target Output: $targetDirective
                Conversation Domain: ${request.domain.label} — ${request.domain.directive}
                Recent Conversation Context (for pronoun/gender/topic continuity only):
                $historyBlock

                Utterance to translate verbatim in meaning:
                "${request.sourceText.trim()}"
            """.trimIndent()

            val response = withTimeout(TIMEOUT_MS) {
                egyptianModel.generateContent(prompt)
            }
            val elapsedMs = (System.currentTimeMillis() - startedAt).coerceAtLeast(0L)
            val rawJson = response.text ?: throw IllegalStateException("Translation engine returned an empty payload.")
            val json = JSONObject(rawJson)

            val translatedText = json.optString("translatedText", "").trim()
            val confidence = json.optString("confidence", "MEDIUM").uppercase()
            val ambiguityWarning = json.optString("ambiguityWarning", "").trim()
            val detectedDialect = json.optString("detectedSourceDialect", request.sourceLanguage.bcp47Tag).trim()

            val validationIssues = TranslationValidator.validate(request, translatedText, confidence)

            Result.success(
                TranslationOutput(
                    translatedText = translatedText,
                    detectedSourceDialect = detectedDialect,
                    confidenceLabel = confidence,
                    ambiguityWarning = ambiguityWarning,
                    validationWarnings = validationIssues,
                    latencyMs = elapsedMs
                )
            )
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Translation timed out", e)
            Result.failure(IllegalStateException("Translation timed out after 60s. Check network connectivity and retry."))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Translation failed", e)
            Result.failure(e)
        }
    }

    suspend fun translateWorldLanguage(
        sourceText: String,
        sourceCode: String,
        targetCode: String,
        conversationContext: List<String> = emptyList(),
        domain: TranslationContextDomain = TranslationContextDomain.CASUAL
    ): Result<TranslationOutput> {
        if (sourceText.isBlank()) {
            return Result.failure(IllegalArgumentException("Cannot translate empty message."))
        }
        val autoDetect = sourceCode.equals("AUTO", ignoreCase = true)
        val normalizedSrc = normalizeCoreCode(sourceCode)
        val normalizedDst = normalizeCoreCode(targetCode)
        val isCorePair = !autoDetect && sourceCode in CORE_CODES && targetCode in CORE_CODES
        if (isCorePair) {
            val dialect = if (targetCode.equals("ar-MSA", ignoreCase = true)) {
                ArabicDialectMode.MODERN_STANDARD
            } else {
                ArabicDialectMode.EGYPTIAN_COLLOQUIAL
            }
            return translate(
                TranslationRequest(
                    sourceText = sourceText,
                    sourceLanguage = normalizedSrc,
                    targetLanguage = normalizedDst,
                    arabicDialectMode = dialect,
                    conversationContext = conversationContext,
                    domain = domain,
                    autoDetectSource = false
                )
            )
        }

        val startedAt = System.currentTimeMillis()
        return try {
            val srcLabel = if (autoDetect) {
                "Auto-detect (Arabic ar-EG/ar-MSA, English, Russian, French, Spanish, German, Italian, Turkish, MIXED, or any global language)"
            } else {
                "${com.example.data.model.NiloraTaxonomy.languageDisplayLabel(sourceCode)} ($sourceCode)"
            }
            val dstLabel = if (targetCode.equals("ar-EG", ignoreCase = true) || targetCode.equals("ar", ignoreCase = true)) {
                "Egyptian Colloquial Arabic (ar-EG / العامية المصرية) — use natural everyday Egyptian expressions, NOT formal MSA"
            } else {
                "${com.example.data.model.NiloraTaxonomy.languageDisplayLabel(targetCode)} ($targetCode)"
            }
            val historyBlock = if (conversationContext.isEmpty()) "None" else conversationContext.takeLast(4).joinToString("\n")
            val prompt = """
                Expected Source Language: $srcLabel
                Target Output: $dstLabel
                Conversation Domain: ${domain.label} — ${domain.directive}
                Recent Conversation Context:
                $historyBlock

                Preserve names, numbers, dates, negation, and conversational tone. Handle idioms and mixed code-switching naturally.
                Utterance to translate verbatim in meaning:
                "${sourceText.trim()}"
            """.trimIndent()
            val response = withTimeout(TIMEOUT_MS) { egyptianModel.generateContent(prompt) }
            val elapsedMs = (System.currentTimeMillis() - startedAt).coerceAtLeast(0L)
            val rawJson = response.text ?: throw IllegalStateException("Translation engine returned an empty payload.")
            val json = JSONObject(rawJson)
            val translatedText = json.optString("translatedText", "").trim()
            val confidence = json.optString("confidence", "MEDIUM").uppercase()
            val ambiguityWarning = json.optString("ambiguityWarning", "").trim()
            val detectedDialect = json.optString("detectedSourceDialect", sourceCode).trim()
            val req = TranslationRequest(
                sourceText = sourceText,
                sourceLanguage = normalizedSrc,
                targetLanguage = normalizedDst,
                domain = domain,
                autoDetectSource = autoDetect
            )
            val validationIssues = TranslationValidator.validate(req, translatedText, confidence)

            Result.success(
                TranslationOutput(
                    translatedText = translatedText,
                    detectedSourceDialect = detectedDialect,
                    confidenceLabel = confidence,
                    ambiguityWarning = ambiguityWarning,
                    validationWarnings = validationIssues,
                    latencyMs = elapsedMs
                )
            )
        } catch (e: TimeoutCancellationException) {
            Result.failure(IllegalStateException("Translation timed out after 60s. Check network connectivity."))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "World translation failed", e)
            Result.failure(e)
        }
    }

    private fun normalizeCoreCode(code: String): NiloraLanguage = when {
        code.startsWith("ar", ignoreCase = true) -> NiloraLanguage.ARABIC
        code.startsWith("fr", ignoreCase = true) -> NiloraLanguage.FRENCH
        code.startsWith("ru", ignoreCase = true) -> NiloraLanguage.RUSSIAN
        else -> NiloraLanguage.ENGLISH
    }

    private val CORE_CODES = setOf("ar", "ar-EG", "ar-MSA", "en", "fr", "ru")
}

