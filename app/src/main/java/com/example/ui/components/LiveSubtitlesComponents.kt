package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.NiloraTaxonomy
import com.example.niloralive.SubtitleDisplayMode
import com.example.niloralive.TranslationContextDomain
import com.example.niloralive.TtsVoiceOption
import com.example.ui.theme.*

data class LiveSubtitleItem(
    val speakerName: String,
    val sourceLanguageLabel: String,
    val targetLanguageLabel: String,
    val originalText: String,
    val translatedText: String,
    val isPartial: Boolean = false,
    val confidenceLabel: String = "HIGH", // HIGH | MEDIUM | LOW
    val ambiguityWarning: String = ""
)

/**
 * Reusable Live Subtitles overlay card supporting BOTH / TRANSLATED_ONLY / ORIGINAL_ONLY / NONE,
 * partial vs finalized indicators, low-confidence warnings, and high-contrast accessible typography.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LiveSubtitlesOverlayCard(
    subtitle: LiveSubtitleItem?,
    mode: SubtitleDisplayMode,
    onModeChange: (SubtitleDisplayMode) -> Unit,
    onSpeakTranslation: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("live_subtitles_overlay_card"),
        colors = CardDefaults.cardColors(containerColor = DeepNavyDark),
        border = BorderStroke(1.dp, NileTurquoise.copy(alpha = 0.55f)),
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Subtitles,
                        contentDescription = "Live subtitles",
                        tint = NileTurquoise,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "Live Subtitles",
                        style = MaterialTheme.typography.titleSmall,
                        color = SoftIvory,
                        fontWeight = FontWeight.Bold
                    )
                    if (subtitle != null) {
                        Surface(
                            color = if (subtitle.isPartial) WarningAmberContainer else VerifiedEmeraldContainer,
                            shape = RoundedCornerShape(999.dp)
                        ) {
                            Text(
                                text = if (subtitle.isPartial) "Live / Partial" else "Final",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (subtitle.isPartial) Color(0xFF78350F) else Color(0xFF065F46),
                                modifier = Modifier
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                                    .testTag("subtitle_status_pill")
                            )
                        }
                    }
                }
                if (subtitle != null && !subtitle.isPartial && onSpeakTranslation != null && subtitle.translatedText.isNotBlank()) {
                    IconButton(
                        onClick = onSpeakTranslation,
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("subtitle_speak_btn")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                            contentDescription = "Speak translated subtitle",
                            tint = NileTurquoise
                        )
                    }
                }
            }

            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(SubtitleDisplayMode.entries) { itemMode ->
                    FilterChip(
                        selected = mode == itemMode,
                        onClick = { onModeChange(itemMode) },
                        label = { Text(itemMode.label, maxLines = 1) },
                        modifier = Modifier.testTag("subtitle_mode_${itemMode.key}")
                    )
                }
            }

            if (mode == SubtitleDisplayMode.NONE) {
                Text(
                    text = "Subtitles hidden. Select a subtitle mode above to display speech and translations.",
                    style = MaterialTheme.typography.bodySmall,
                    color = WarmSand.copy(alpha = 0.8f),
                    modifier = Modifier.testTag("subtitle_hidden_hint")
                )
            } else if (subtitle == null || (subtitle.originalText.isBlank() && subtitle.translatedText.isBlank())) {
                Text(
                    text = "Waiting for speech or message — partial transcripts and translations will appear here.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = WarmSand.copy(alpha = 0.82f)
                )
            } else {
                Text(
                    text = "${subtitle.speakerName} • ${subtitle.sourceLanguageLabel} → ${subtitle.targetLanguageLabel}",
                    style = MaterialTheme.typography.labelMedium,
                    color = EgyptianGold,
                    fontWeight = FontWeight.SemiBold
                )
                if (mode == SubtitleDisplayMode.BOTH || mode == SubtitleDisplayMode.ORIGINAL_ONLY) {
                    Text(
                        text = subtitle.originalText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = SoftIvory.copy(alpha = if (mode == SubtitleDisplayMode.ORIGINAL_ONLY) 1f else 0.85f),
                        modifier = Modifier.testTag("subtitle_original_text")
                    )
                }
                if ((mode == SubtitleDisplayMode.BOTH || mode == SubtitleDisplayMode.TRANSLATED_ONLY) && subtitle.translatedText.isNotBlank()) {
                    Text(
                        text = subtitle.translatedText,
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.testTag("subtitle_translated_text")
                    )
                }
                if (subtitle.confidenceLabel == "LOW" || subtitle.ambiguityWarning.isNotBlank()) {
                    Surface(
                        color = WarningAmberContainer,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Filled.WarningAmber, contentDescription = null, tint = Color(0xFF78350F), modifier = Modifier.size(16.dp))
                            Text(
                                text = subtitle.ambiguityWarning.ifBlank { "Low confidence recognition/translation — please verify." },
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF78350F),
                                modifier = Modifier.testTag("subtitle_low_confidence_warning")
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TranslationDomainAndAudioBar(
    selectedDomain: TranslationContextDomain,
    autoDetectSource: Boolean,
    preferOriginalAudio: Boolean,
    availableVoices: List<TtsVoiceOption>,
    selectedVoiceName: String,
    onSelectDomain: (TranslationContextDomain) -> Unit,
    onToggleAutoDetect: (Boolean) -> Unit,
    onTogglePreferOriginalAudio: (Boolean) -> Unit,
    onSelectVoice: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var showVoiceDialog by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            items(TranslationContextDomain.entries) { domain ->
                FilterChip(
                    selected = selectedDomain == domain,
                    onClick = { onSelectDomain(domain) },
                    label = { Text(domain.label, maxLines = 1) },
                    modifier = Modifier.testTag("domain_chip_${domain.name}")
                )
            }
        }
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            FilterChip(
                selected = autoDetectSource,
                onClick = { onToggleAutoDetect(!autoDetectSource) },
                leadingIcon = { Icon(Icons.Filled.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp)) },
                label = { Text(if (autoDetectSource) "Auto-Detect: ON" else "Auto-Detect: OFF") },
                modifier = Modifier.testTag("auto_detect_lang_chip")
            )
            FilterChip(
                selected = preferOriginalAudio,
                onClick = { onTogglePreferOriginalAudio(!preferOriginalAudio) },
                leadingIcon = { Icon(Icons.Filled.RecordVoiceOver, contentDescription = null, modifier = Modifier.size(16.dp)) },
                label = { Text(if (preferOriginalAudio) "Audio: Original" else "Audio: Translated") },
                modifier = Modifier.testTag("audio_mode_chip")
            )
            AssistChip(
                onClick = { showVoiceDialog = true },
                leadingIcon = { Icon(Icons.Filled.GraphicEq, contentDescription = null, modifier = Modifier.size(16.dp)) },
                label = {
                    val label = if (selectedVoiceName.isBlank()) "Voice: Auto (${availableVoices.size})" else "Voice: ${selectedVoiceName.take(14)}"
                    Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis)
                },
                modifier = Modifier.testTag("voice_picker_chip")
            )
        }
    }

    if (showVoiceDialog) {
        AlertDialog(
            onDismissRequest = { showVoiceDialog = false },
            title = { Text("Select TTS Voice") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = if (availableVoices.isEmpty()) {
                            "No installed Android TTS voice list returned on this device yet. System default voice or subtitles fallback will be used."
                        } else {
                            "Choose an installed device TextToSpeech voice (${availableVoices.size} detected):"
                        },
                        style = MaterialTheme.typography.bodySmall
                    )
                    OutlinedCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onSelectVoice("")
                                showVoiceDialog = false
                            }
                    ) {
                        Text(
                            text = "Automatic Best Match (Recommended • ar-EG priority)",
                            modifier = Modifier.padding(12.dp),
                            fontWeight = if (selectedVoiceName.isBlank()) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                    availableVoices.take(8).forEach { voice ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onSelectVoice(voice.name)
                                    showVoiceDialog = false
                                },
                            colors = CardDefaults.cardColors(
                                containerColor = if (selectedVoiceName == voice.name) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface
                            )
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(voice.name, style = MaterialTheme.typography.labelLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(
                                    "${voice.localeTag} • ${if (voice.isNetworkRequired) "Network voice" else "Offline voice"}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showVoiceDialog = false }) { Text("Close") }
            }
        )
    }
}

@Composable
fun OneLanguageIdentitySummaryBanner(
    interfaceLangCode: String,
    spokenLangCode: String,
    targetTranslationCode: String,
    subtitleMode: SubtitleDisplayMode,
    onEditLanguageIdentity: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("language_identity_summary_banner"),
        shape = RoundedCornerShape(16.dp),
        color = TurquoiseContainer.copy(alpha = 0.72f),
        border = BorderStroke(1.dp, NileTurquoise.copy(alpha = 0.45f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = "One Language Identity",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = TurquoiseOnContainer
                )
                Text(
                    text = "Speak: ${NiloraTaxonomy.languageDisplayLabel(spokenLangCode).substringBefore(" (")} • Translate to: ${NiloraTaxonomy.languageDisplayLabel(targetTranslationCode).substringBefore(" (")} • UI: ${interfaceLangCode.uppercase()} • Subtitles: ${subtitleMode.label}",
                    style = MaterialTheme.typography.bodySmall,
                    color = TurquoiseOnContainer.copy(alpha = 0.88f)
                )
            }
            TextButton(onClick = onEditLanguageIdentity) {
                Text("Edit")
            }
        }
    }
}
