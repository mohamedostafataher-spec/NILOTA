package com.example.data.repository

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.niloraDataStore by preferencesDataStore(name = "nilora_user_prefs")

data class LocalAppPreferences(
    val languageCode: String = "en",
    val spokenLanguageCode: String = "en",
    val targetTranslationLanguageCode: String = "ar-EG",
    val subtitleDisplayMode: String = "BOTH", // BOTH | TRANSLATED_ONLY | ORIGINAL_ONLY | NONE
    val preferOriginalAudio: Boolean = false,
    val selectedTtsVoiceName: String = "",
    val personalizationEnabled: Boolean = true,
    val skippedOptionalOnboarding: Boolean = false,
    val onboardingDone: Boolean = false,
    val darkThemeOverride: String = "SYSTEM", // SYSTEM | LIGHT | DARK
    val retainTranslationTranscripts: Boolean = false,
    val privacyPolicyAcknowledged: Boolean = false,
    val voiceTransmissionConsent: Boolean = false,
    val preferEgyptianColloquial: Boolean = true,
    val autoTranslateAfterSpeech: Boolean = true,
    val ttsSpeed: Float = 1.0f,
    val ttsVolume: Float = 1.0f
)

class UserPreferencesRepository(private val context: Context) {
    private object Keys {
        val LANGUAGE = stringPreferencesKey("language_code")
        val SPOKEN_LANG = stringPreferencesKey("spoken_language_code")
        val TARGET_TRANS_LANG = stringPreferencesKey("target_translation_language_code")
        val SUBTITLE_MODE = stringPreferencesKey("subtitle_display_mode")
        val PREFER_ORIGINAL_AUDIO = booleanPreferencesKey("prefer_original_audio")
        val SELECTED_TTS_VOICE = stringPreferencesKey("selected_tts_voice_name")
        val PERSONALIZATION_ENABLED = booleanPreferencesKey("personalization_enabled")
        val SKIPPED_OPTIONAL_ONBOARDING = booleanPreferencesKey("skipped_optional_onboarding")
        val ONBOARDING_DONE = booleanPreferencesKey("onboarding_done")
        val DARK_THEME = stringPreferencesKey("dark_theme_override")
        val RETAIN_TRANSCRIPTS = booleanPreferencesKey("retain_transcripts")
        val PRIVACY_ACK = booleanPreferencesKey("privacy_ack")
        val VOICE_CONSENT = booleanPreferencesKey("voice_transmission_consent")
        val PREFER_EGYPTIAN = booleanPreferencesKey("prefer_egyptian_colloquial")
        val AUTO_TRANSLATE = booleanPreferencesKey("auto_translate_after_speech")
        val TTS_SPEED = floatPreferencesKey("tts_speed")
        val TTS_VOLUME = floatPreferencesKey("tts_volume")
    }

    val preferencesFlow: Flow<LocalAppPreferences> = context.niloraDataStore.data.map { prefs ->
        val uiLang = prefs[Keys.LANGUAGE] ?: "en"
        val defaultSpoken = if (uiLang == "ar") "ar-EG" else uiLang
        val defaultTarget = if (uiLang == "ar") "en" else "ar-EG"
        LocalAppPreferences(
            languageCode = uiLang,
            spokenLanguageCode = prefs[Keys.SPOKEN_LANG] ?: defaultSpoken,
            targetTranslationLanguageCode = prefs[Keys.TARGET_TRANS_LANG] ?: defaultTarget,
            subtitleDisplayMode = prefs[Keys.SUBTITLE_MODE] ?: "BOTH",
            preferOriginalAudio = prefs[Keys.PREFER_ORIGINAL_AUDIO] ?: false,
            selectedTtsVoiceName = prefs[Keys.SELECTED_TTS_VOICE] ?: "",
            personalizationEnabled = prefs[Keys.PERSONALIZATION_ENABLED] ?: true,
            skippedOptionalOnboarding = prefs[Keys.SKIPPED_OPTIONAL_ONBOARDING] ?: false,
            onboardingDone = prefs[Keys.ONBOARDING_DONE] ?: false,
            darkThemeOverride = prefs[Keys.DARK_THEME] ?: "SYSTEM",
            retainTranslationTranscripts = prefs[Keys.RETAIN_TRANSCRIPTS] ?: false,
            privacyPolicyAcknowledged = prefs[Keys.PRIVACY_ACK] ?: false,
            voiceTransmissionConsent = prefs[Keys.VOICE_CONSENT] ?: false,
            preferEgyptianColloquial = prefs[Keys.PREFER_EGYPTIAN] ?: true,
            autoTranslateAfterSpeech = prefs[Keys.AUTO_TRANSLATE] ?: true,
            ttsSpeed = (prefs[Keys.TTS_SPEED] ?: 1.0f).coerceIn(0.6f, 1.6f),
            ttsVolume = (prefs[Keys.TTS_VOLUME] ?: 1.0f).coerceIn(0f, 1f)
        )
    }

    suspend fun setLanguage(code: String) {
        context.niloraDataStore.edit { it[Keys.LANGUAGE] = code }
    }

    suspend fun setSpokenLanguageCode(code: String) {
        context.niloraDataStore.edit { it[Keys.SPOKEN_LANG] = code }
    }

    suspend fun setTargetTranslationLanguageCode(code: String) {
        context.niloraDataStore.edit { it[Keys.TARGET_TRANS_LANG] = code }
    }

    suspend fun setLanguageIdentity(
        interfaceLanguageCode: String,
        spokenLanguageCode: String,
        targetTranslationLanguageCode: String,
        preferEgyptianColloquial: Boolean = true
    ) {
        context.niloraDataStore.edit {
            it[Keys.LANGUAGE] = interfaceLanguageCode
            it[Keys.SPOKEN_LANG] = spokenLanguageCode
            it[Keys.TARGET_TRANS_LANG] = targetTranslationLanguageCode
            it[Keys.PREFER_EGYPTIAN] = preferEgyptianColloquial
        }
    }

    suspend fun setSubtitleDisplayMode(mode: String) {
        context.niloraDataStore.edit { it[Keys.SUBTITLE_MODE] = mode }
    }

    suspend fun setPreferOriginalAudio(preferOriginal: Boolean) {
        context.niloraDataStore.edit { it[Keys.PREFER_ORIGINAL_AUDIO] = preferOriginal }
    }

    suspend fun setSelectedTtsVoiceName(voiceName: String) {
        context.niloraDataStore.edit { it[Keys.SELECTED_TTS_VOICE] = voiceName }
    }

    suspend fun setPersonalizationEnabled(enabled: Boolean) {
        context.niloraDataStore.edit { it[Keys.PERSONALIZATION_ENABLED] = enabled }
    }

    suspend fun setSkippedOptionalOnboarding(skipped: Boolean) {
        context.niloraDataStore.edit { it[Keys.SKIPPED_OPTIONAL_ONBOARDING] = skipped }
    }

    suspend fun setOnboardingDone(done: Boolean) {
        context.niloraDataStore.edit { it[Keys.ONBOARDING_DONE] = done }
    }

    suspend fun setDarkThemeOverride(mode: String) {
        context.niloraDataStore.edit { it[Keys.DARK_THEME] = mode }
    }

    suspend fun setRetainTranscripts(retain: Boolean) {
        context.niloraDataStore.edit { it[Keys.RETAIN_TRANSCRIPTS] = retain }
    }

    suspend fun setPrivacyAcknowledged(ack: Boolean) {
        context.niloraDataStore.edit { it[Keys.PRIVACY_ACK] = ack }
    }

    suspend fun setVoiceTransmissionConsent(granted: Boolean) {
        context.niloraDataStore.edit { it[Keys.VOICE_CONSENT] = granted }
    }

    suspend fun setPreferEgyptianColloquial(preferEgyptian: Boolean) {
        context.niloraDataStore.edit { it[Keys.PREFER_EGYPTIAN] = preferEgyptian }
    }

    suspend fun setAutoTranslateAfterSpeech(autoTranslate: Boolean) {
        context.niloraDataStore.edit { it[Keys.AUTO_TRANSLATE] = autoTranslate }
    }

    suspend fun setTtsSpeed(speed: Float) {
        context.niloraDataStore.edit { it[Keys.TTS_SPEED] = speed.coerceIn(0.6f, 1.6f) }
    }

    suspend fun setTtsVolume(volume: Float) {
        context.niloraDataStore.edit { it[Keys.TTS_VOLUME] = volume.coerceIn(0f, 1f) }
    }
}
