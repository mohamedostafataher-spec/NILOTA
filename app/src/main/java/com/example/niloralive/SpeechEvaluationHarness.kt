package com.example.niloralive

import com.example.data.model.NiloraLanguage
import kotlin.math.min

data class SpeechEvaluationTrial(
    val id: String,
    val sourceLanguage: NiloraLanguage,
    val targetLanguage: NiloraLanguage,
    val scenarioLabel: String, // Quiet / Background Noise / Fast / Slow / Numbers & Names / Mixed-Language
    val referenceTranscript: String,
    val recognizedTranscript: String,
    val computedWer: Double?, // null until human provides non-blank referenceTranscript
    val referenceTranslation: String,
    val actualTranslation: String,
    val detectedDialect: String,
    val sttLatencyMs: Long,
    val translationLatencyMs: Long,
    val validationWarnings: List<String>,
    val humanNaturalnessNotes: String = ""
)

object SpeechEvaluationHarness {

    /**
     * Real deterministic Levenshtein Word Error Rate (WER).
     * Returns null when reference is blank (never invents a score).
     */
    fun computeWordErrorRate(reference: String, hypothesis: String): Double? {
        val refWords = tokenize(reference)
        if (refWords.isEmpty()) return null
        val hypWords = tokenize(hypothesis)

        val n = refWords.size
        val m = hypWords.size
        val dp = Array(n + 1) { IntArray(m + 1) }
        for (i in 0..n) dp[i][0] = i
        for (j in 0..m) dp[0][j] = j

        for (i in 1..n) {
            for (j in 1..m) {
                val cost = if (refWords[i - 1] == hypWords[j - 1]) 0 else 1
                dp[i][j] = min(
                    min(dp[i - 1][j] + 1, dp[i][j - 1] + 1),
                    dp[i - 1][j - 1] + cost
                )
            }
        }
        return dp[n][m].toDouble() / n.toDouble()
    }

    private fun tokenize(text: String): List<String> =
        text.trim()
            .lowercase()
            .replace(Regex("""[^\p{L}\p{N}\s]"""), " ")
            .split(Regex("""\s+"""))
            .filter { it.isNotBlank() }

    fun formatMarkdownReport(trials: List<SpeechEvaluationTrial>, hw: HardwareAndSpeechCapabilities): String {
        if (trials.isEmpty()) {
            return "No real speech evaluation trials recorded yet. Speak with a consenting test participant, enter the human-verified reference transcript, and record observations."
        }
        return buildString {
            appendLine("# NILORA LIVE — Device Speech Evaluation Report")
            appendLine("Device RAM: ${hw.totalRamMb} MB | CPU Cores: ${hw.cpuCores} | SpeechRecognizer Available: ${hw.speechRecognizerAvailable}")
            appendLine("Trials Recorded: ${trials.size}")
            appendLine()
            trials.forEachIndexed { idx, t ->
                val werText = t.computedWer?.let { String.format("%.1f%%", it * 100.0) } ?: "Not computed (no reference transcript)"
                appendLine("## Trial ${idx + 1}: ${t.sourceLanguage.code} -> ${t.targetLanguage.code} (${t.scenarioLabel})")
                appendLine("- Reference Transcript: ${t.referenceTranscript.ifBlank { "[Not entered]" }}")
                appendLine("- Recognized Transcript: ${t.recognizedTranscript}")
                appendLine("- Word Error Rate (WER): $werText")
                appendLine("- Reference Translation: ${t.referenceTranslation.ifBlank { "[Not entered]" }}")
                appendLine("- Actual Translation: ${t.actualTranslation}")
                appendLine("- Detected Dialect Hint: ${t.detectedDialect}")
                appendLine("- Measured Latency: STT=${t.sttLatencyMs}ms, Translation=${t.translationLatencyMs}ms")
                if (t.validationWarnings.isNotEmpty()) {
                    appendLine("- Warnings: ${t.validationWarnings.joinToString("; ")}")
                }
                if (t.humanNaturalnessNotes.isNotBlank()) {
                    appendLine("- Reviewer Notes: ${t.humanNaturalnessNotes}")
                }
                appendLine()
            }
        }
    }
}

data class BenchmarkUtteranceCase(
    val id: String,
    val scenario: String,
    val sourceLanguage: NiloraLanguage,
    val targetLanguage: NiloraLanguage,
    val domain: TranslationContextDomain,
    val referenceTranscript: String,
    val sampleRecognizedHypothesis: String,
    val referenceTranslation: String,
    val isCodeSwitched: Boolean = false
)

object EgyptianArabicBenchmarkSuite {
    /**
     * Representative reference utterances for offline deterministic WER and validator checks
     * (clearly distinct from live on-device SpeechRecognizer / live Gemini tests).
     */
    val cases = listOf(
        BenchmarkUtteranceCase(
            id = "eg_travel_price",
            scenario = "Travel & Numbers (ar-EG -> en)",
            sourceLanguage = NiloraLanguage.ARABIC,
            targetLanguage = NiloraLanguage.ENGLISH,
            domain = TranslationContextDomain.TRAVEL,
            referenceTranscript = "بكام التذكرة للمتحف المصري الكبير الساعة 10 الصبح",
            sampleRecognizedHypothesis = "بكام التذكرة للمتحف المصري الكبير الساعة 10 الصبح",
            referenceTranslation = "How much is the ticket to the Grand Egyptian Museum at 10 in the morning?"
        ),
        BenchmarkUtteranceCase(
            id = "ru_to_eg_beach",
            scenario = "Tourism (ru -> ar-EG)",
            sourceLanguage = NiloraLanguage.RUSSIAN,
            targetLanguage = NiloraLanguage.ARABIC,
            domain = TranslationContextDomain.TOURISM,
            referenceTranscript = "Подскажите пожалуйста где ближайший пляж в Дахабе",
            sampleRecognizedHypothesis = "Подскажите пожалуйста где ближайший пляж в Дахабе",
            referenceTranslation = "ممكن تقولي أقرب شاطئ في دهب فين لو سمحت؟"
        ),
        BenchmarkUtteranceCase(
            id = "eg_mixed_code_switch",
            scenario = "Mixed ar-EG + English Code-Switching",
            sourceLanguage = NiloraLanguage.ARABIC,
            targetLanguage = NiloraLanguage.ENGLISH,
            domain = TranslationContextDomain.SPORTS,
            referenceTranscript = "يلا نلعب padel الساعة 8 في الزمالك",
            sampleRecognizedHypothesis = "يلا نلعب بادل الساعة 8 في الزمالك",
            referenceTranslation = "Let's play padel at 8 o'clock in Zamalek",
            isCodeSwitched = true
        ),
        BenchmarkUtteranceCase(
            id = "en_to_eg_idiom",
            scenario = "Colloquial Greeting (en -> ar-EG)",
            sourceLanguage = NiloraLanguage.ENGLISH,
            targetLanguage = NiloraLanguage.ARABIC,
            domain = TranslationContextDomain.CASUAL,
            referenceTranscript = "No worries, everything is going smoothly today",
            sampleRecognizedHypothesis = "No worries everything is going smoothly today",
            referenceTranslation = "معلش يا عم، كل حاجة ماشية زي الفل النهاردة"
        ),
        BenchmarkUtteranceCase(
            id = "fr_to_eg_group",
            scenario = "Group Activity Scheduling (fr -> ar-EG)",
            sourceLanguage = NiloraLanguage.FRENCH,
            targetLanguage = NiloraLanguage.ARABIC,
            domain = TranslationContextDomain.GROUP_ACTIVITIES,
            referenceTranscript = "On se retrouve à 18 heures devant la porte 3",
            sampleRecognizedHypothesis = "On se retrouve à 18 heures devant la porte 3",
            referenceTranslation = "هنتقابل الساعة 6 المغرب قدام بوابة 3"
        )
    )
}
