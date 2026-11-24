package com.example.ui.components

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.data.model.NiloraTaxonomy
import com.example.data.model.SafeMeetupPolicy
import com.example.niloralive.rememberDecodedBase64Image
import com.example.ui.theme.*
import com.example.ui.viewmodel.VoiceMediaDraftState

@Composable
fun EditorialActionPillarCard(
    eyebrow: String,
    title: String,
    subtitle: String,
    icon: ImageVector,
    containerColor: Color,
    contentColor: Color,
    accentColor: Color,
    testTag: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag(testTag)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        color = containerColor,
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.35f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(48.dp),
                shape = CircleShape,
                color = accentColor.copy(alpha = 0.22f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = accentColor,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = eyebrow.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = accentColor,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = contentColor,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = contentColor.copy(alpha = 0.85f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Icon(
                imageVector = Icons.Filled.ArrowForward,
                contentDescription = null,
                tint = accentColor,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
fun ConnectionJourneyStrip(
    modifier: Modifier = Modifier
) {
    val steps = listOf(
        "1. Discover Moment",
        "2. Contribute",
        "3. Exchange",
        "4. Connect",
        "5. Private Chat",
        "6. Safe Public Meet"
    )
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(steps) { step ->
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f),
                shape = RoundedCornerShape(999.dp),
                border = BorderStroke(1.dp, EgyptianGold.copy(alpha = 0.32f))
            ) {
                Text(
                    text = step,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                )
            }
        }
    }
}

@Composable
fun CountryChainRow(
    countries: List<String>,
    modifier: Modifier = Modifier
) {
    if (countries.isEmpty()) return
    LazyRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        items(countries) { country ->
            Surface(
                color = NileTurquoiseContainer,
                shape = RoundedCornerShape(999.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.Public,
                        contentDescription = null,
                        tint = NileTurquoiseOnContainer,
                        modifier = Modifier.size(12.dp)
                    )
                    Text(
                        text = country,
                        style = MaterialTheme.typography.labelSmall,
                        color = NileTurquoiseOnContainer,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
fun InlineTranslationBox(
    originalText: String,
    sourceLanguageCode: String,
    translations: Map<String, String>,
    translationNote: String,
    targetLanguageCode: String,
    isTranslating: Boolean,
    onTranslateClick: () -> Unit,
    onSpeakOriginal: (() -> Unit)? = null,
    onSpeakTranslation: ((String) -> Unit)? = null,
    testTagPrefix: String = "inline_trans"
) {
    var showOriginal by remember { mutableStateOf(true) }
    val translated = translations[targetLanguageCode].orEmpty()

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Original (${NiloraTaxonomy.languageDisplayLabel(sourceLanguageCode)})",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                if (onSpeakOriginal != null) {
                    IconButton(
                        onClick = onSpeakOriginal,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                            contentDescription = "Listen to original",
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                if (translated.isNotBlank()) {
                    TextButton(onClick = { showOriginal = !showOriginal }) {
                        Text(if (showOriginal) "Hide Original" else "Show Original")
                    }
                }
            }
        }

        if (showOriginal || translated.isBlank()) {
            Text(
                text = originalText,
                style = MaterialTheme.typography.bodyLarge
            )
        }

        if (translated.isNotBlank()) {
            Surface(
                color = NileTurquoiseContainer.copy(alpha = 0.5f),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, NileTurquoise.copy(alpha = 0.4f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "NILORA LIVE -> $targetLanguageCode",
                            style = MaterialTheme.typography.labelSmall,
                            color = NileTurquoiseDark,
                            fontWeight = FontWeight.Bold
                        )
                        if (onSpeakTranslation != null) {
                            IconButton(
                                onClick = { onSpeakTranslation(translated) },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                    contentDescription = "Listen to translation",
                                    tint = NileTurquoiseDark,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                    Text(
                        text = translated,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (translationNote.isNotBlank()) {
                        Text(
                            text = translationNote,
                            style = MaterialTheme.typography.labelSmall,
                            color = WarningAmber
                        )
                    }
                    Text(
                        text = "Automated cultural translation; verify idioms directly with participants.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            OutlinedButton(
                onClick = onTranslateClick,
                enabled = !isTranslating,
                modifier = Modifier.testTag("${testTagPrefix}_translate_btn")
            ) {
                Icon(Icons.Filled.Translate, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text(if (isTranslating) "Translating..." else "Translate to $targetLanguageCode")
            }
        }
    }
}

@Composable
fun TargetLanguageQuickSelector(
    selectedCode: String,
    onSelectCode: (String) -> Unit
) {
    val quickCodes = listOf("ar-EG", "en", "ru", "fr", "ar-MSA")
    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        items(quickCodes) { code ->
            FilterChip(
                selected = selectedCode == code,
                onClick = { onSelectCode(code) },
                label = { Text(code) }
            )
        }
    }
}

@Composable
fun ExperienceMediaAttachmentBar(
    draft: VoiceMediaDraftState,
    languageCode: String,
    onPickPhotoUri: (android.net.Uri?) -> Unit,
    onClearPhoto: () -> Unit,
    onStartRecording: () -> Unit,
    onStopRecording: () -> Unit,
    onClearRecording: () -> Unit,
    onPlayPreviewClip: (String) -> Unit,
    onDictateText: () -> Unit
) {
    val context = LocalContext.current
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        onPickPhotoUri(uri)
    }
    val micPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) onStartRecording()
    }

    val decodedPreview = rememberDecodedBase64Image(draft.photoBase64)

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = {
                    photoPickerLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                },
                enabled = !draft.isEncodingPhoto,
                modifier = Modifier
                    .weight(1f)
                    .testTag("attach_photo_btn")
            ) {
                Icon(Icons.Filled.PhotoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(if (draft.isEncodingPhoto) "Processing..." else "Add Photo")
            }

            OutlinedButton(
                onClick = {
                    if (draft.isRecording) {
                        onStopRecording()
                    } else {
                        val hasPerm = ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.RECORD_AUDIO
                        ) == PackageManager.PERMISSION_GRANTED
                        if (hasPerm) {
                            onStartRecording()
                        } else {
                            micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        }
                    }
                },
                modifier = Modifier
                    .weight(1f)
                    .testTag("attach_voice_btn")
            ) {
                Icon(
                    imageVector = if (draft.isRecording) Icons.Filled.Stop else Icons.Filled.Mic,
                    contentDescription = null,
                    tint = if (draft.isRecording) ErrorCoral else LocalContentColor.current,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(if (draft.isRecording) "Stop Voice" else "Record Voice")
            }
        }

        OutlinedButton(
            onClick = {
                val hasPerm = ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.RECORD_AUDIO
                ) == PackageManager.PERMISSION_GRANTED
                if (hasPerm) {
                    onDictateText()
                } else {
                    micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                }
            },
            enabled = !draft.isTranscribing,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("dictate_voice_text_btn")
        ) {
            Icon(Icons.Filled.RecordVoiceOver, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text(if (draft.isTranscribing) "Listening ($languageCode)..." else "Speak to Write ($languageCode)")
        }

        Text(
            text = "Photo upload uses Android Photo Picker (no broad storage permissions). Voice clips record up to 20s via device microphone. Short video cloud upload requires Firebase Storage bucket setup.",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        if (draft.errorBanner != null) {
            Text(
                text = draft.errorBanner,
                style = MaterialTheme.typography.bodySmall,
                color = ErrorCoral
            )
        }

        if (decodedPreview != null) {
            Card(shape = RoundedCornerShape(14.dp)) {
                Box {
                    Image(
                        bitmap = decodedPreview,
                        contentDescription = "Selected experience photo",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(170.dp),
                        contentScale = ContentScale.Crop
                    )
                    FilledIconButton(
                        onClick = onClearPhoto,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                    ) {
                        Icon(Icons.Filled.Close, contentDescription = "Remove photo")
                    }
                }
            }
        }

        if (draft.recordedClip != null) {
            Surface(
                color = EgyptianGoldContainer,
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { onPlayPreviewClip(draft.recordedClip.audioBase64) }) {
                            Icon(
                                imageVector = if (draft.playingAudioId == "draft_preview") Icons.Filled.Stop else Icons.Filled.PlayArrow,
                                contentDescription = "Play recorded clip"
                            )
                        }
                        Text(
                            text = "Voice attached (${(draft.recordedClip.durationMs / 1000L).coerceAtLeast(1)}s)",
                            style = MaterialTheme.typography.labelLarge,
                            color = EgyptianGoldOnContainer
                        )
                    }
                    IconButton(onClick = onClearRecording) {
                        Icon(Icons.Filled.DeleteOutline, contentDescription = "Remove voice clip")
                    }
                }
            }
        }
    }
}

@Composable
fun Base64PhotoPreview(
    photoBase64: String,
    contentDescription: String,
    modifier: Modifier = Modifier
) {
    val bmp = rememberDecodedBase64Image(photoBase64) ?: return
    Image(
        bitmap = bmp,
        contentDescription = contentDescription,
        modifier = modifier
            .fillMaxWidth()
            .height(190.dp)
            .clip(RoundedCornerShape(14.dp)),
        contentScale = ContentScale.Crop
    )
}

@Composable
fun VoiceAudioPlayerChip(
    clipId: String,
    durationMs: Long,
    isPlaying: Boolean,
    onTogglePlay: () -> Unit
) {
    Surface(
        color = EgyptianGoldContainer.copy(alpha = 0.85f),
        shape = RoundedCornerShape(999.dp),
        modifier = Modifier.clickable(onClick = onTogglePlay)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (isPlaying) Icons.Filled.Stop else Icons.Filled.PlayArrow,
                contentDescription = if (isPlaying) "Stop audio" else "Play voice audio",
                tint = EgyptianGoldOnContainer,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = if (isPlaying) "Playing voice..." else "Listen Voice (${(durationMs / 1000L).coerceAtLeast(1)}s)",
                style = MaterialTheme.typography.labelMedium,
                color = EgyptianGoldOnContainer,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
fun SafeMeetupGuidanceCard(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = VerifiedEmeraldContainer.copy(alpha = 0.55f),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, VerifiedEmerald.copy(alpha = 0.45f))
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Filled.Shield, contentDescription = null, tint = VerifiedEmerald)
                Text(
                    text = "Safe Public Meetup & Privacy Rules",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
            }
            SafeMeetupPolicy.guidelines.forEach { rule ->
                Text(
                    text = "• $rule",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Text(
                text = "Suggested public venues: ${SafeMeetupPolicy.publicVenueSuggestions.joinToString(" • ")}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
