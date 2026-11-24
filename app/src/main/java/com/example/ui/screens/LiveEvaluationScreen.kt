package com.example.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.data.model.NiloraLanguage
import com.example.niloralive.*
import com.example.ui.components.NiloraFooter
import com.example.ui.components.NiloraTopBar
import com.example.ui.theme.*
import com.example.ui.viewmodel.TranslatorUiState

@Composable
fun LiveEvaluationScreen(
    state: TranslatorUiState,
    voiceCapabilities: VoiceCapabilityReport,
    onRefreshDiagnostics: () -> Unit,
    onRunAmbientCheck: () -> Unit,
    onSetArabicDialectMode: (ArabicDialectMode) -> Unit,
    onRecordTrial: (
        sourceLang: NiloraLanguage,
        targetLang: NiloraLanguage,
        scenarioLabel: String,
        referenceTranscript: String,
        recognizedTranscript: String,
        referenceTranslation: String,
        humanNotes: String
    ) -> Unit,
    onClearTrials: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val hw = state.hardwareCapabilities

    var sourceLang by remember { mutableStateOf(NiloraLanguage.RUSSIAN) }
    var targetLang by remember { mutableStateOf(NiloraLanguage.ARABIC) }
    val scenarios = remember {
        listOf("Quiet Room", "Background Noise", "Fast Speech", "Slow Speech", "Numbers & Prices", "Mixed Language")
    }
    var selectedScenario by remember { mutableStateOf(scenarios.first()) }
    var referenceTranscript by remember { mutableStateOf("") }
    var recognizedTranscript by remember { mutableStateOf("") }
    var referenceTranslation by remember { mutableStateOf("") }
    var humanNotes by remember { mutableStateOf("") }
    var micDeniedMsg by remember { mutableStateOf<String?>(null) }

    val micLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            micDeniedMsg = null
            onRunAmbientCheck()
        } else {
            micDeniedMsg = "Microphone permission denied. Grant RECORD_AUDIO in system settings to measure real ambient PCM noise."
        }
    }

    Scaffold(
        topBar = {
            NiloraTopBar(
                title = "NILORA LIVE — Diagnostics & Evaluation",
                subtitle = "Real Hardware, Voice & WER Evaluation Lab",
                onBackClick = onBack,
                actions = {
                    IconButton(onClick = onRefreshDiagnostics) {
                        Icon(Icons.Filled.Refresh, contentDescription = "Refresh diagnostics")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .testTag("eval_lazy_list"),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 1. Hardware & Speech Capability Audit
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier.testTag("eval_hardware_card")
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Filled.Memory, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Text("Device Hardware & Speech Audit", style = MaterialTheme.typography.titleMedium)
                        }
                        if (hw != null) {
                            Text("RAM: ${hw.totalRamMb} MB • CPU Cores: ${hw.cpuCores} • ABIs: ${hw.supportedAbis.joinToString().ifBlank { "Unknown" }}", style = MaterialTheme.typography.bodyMedium)
                            Text("Mic Permission: ${if (hw.micPermissionGranted) "Granted" else "Not granted"} • SpeechRecognizer: ${if (hw.speechRecognizerAvailable) "Available" else "Unavailable"}", style = MaterialTheme.typography.bodyMedium)
                            Text("Recognition Services: ${hw.recognitionServicePackages.joinToString().ifBlank { "None detected" }}", style = MaterialTheme.typography.bodySmall)
                            Text("16kHz PCM AudioRecord: ${if (hw.audioRecord16kSupported) "Supported" else "Unsupported"} • Hardware NS: ${hw.hardwareNoiseSuppressorAvailable} • Hardware AEC: ${hw.hardwareEchoCancelerAvailable}", style = MaterialTheme.typography.bodySmall)
                            Text("Local Whisper Feasibility: tiny=${hw.canHostWhisperTinyInTheory}, small=${hw.canHostWhisperSmallInTheory}, native binary bundled=${hw.whisperNativeBinaryBundled}", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }

            // 2. Real Ambient AudioRecord Check
            item {
                Card(shape = RoundedCornerShape(18.dp)) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Real Microphone PCM Noise Check", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "Captures ~1.2 seconds of 16kHz mono PCM via Android AudioRecord with NoiseSuppressor & AcousticEchoCanceler when available. Never simulated.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Button(
                            onClick = {
                                val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
                                if (granted) onRunAmbientCheck() else micLauncher.launch(Manifest.permission.RECORD_AUDIO)
                            },
                            enabled = !state.isSamplingAmbient,
                            modifier = Modifier.testTag("eval_ambient_check_btn")
                        ) {
                            Icon(Icons.Filled.GraphicEq, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text(if (state.isSamplingAmbient) "Sampling Microphone..." else "Measure Real Ambient Noise Floor")
                        }
                        micDeniedMsg?.let { Text(it, color = ErrorCoral, style = MaterialTheme.typography.bodySmall) }
                        state.ambientSampleError?.let { Text(it, color = ErrorCoral, style = MaterialTheme.typography.bodySmall) }
                        state.lastAmbientSample?.let { sample ->
                            Text(
                                "Measured (${sample.sampleRateHz} Hz): RMS=${String.format("%.1f", sample.rmsDbfs)} dBFS • Peak=${String.format("%.1f", sample.peakDbfs)} dBFS • NS=${sample.noiseSuppressorActive} • AEC=${sample.echoCancelerActive}",
                                style = MaterialTheme.typography.labelLarge,
                                color = VerifiedEmerald
                            )
                        }
                    }
                }
            }

            // 3. Honest TTS Voice Report
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = TurquoiseContainer),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Android TextToSpeech Voice Inventory", style = MaterialTheme.typography.titleMedium, color = TurquoiseOnContainer)
                        Text("TTS Initialized: ${voiceCapabilities.ttsInitialized} • Installed Voices: ${voiceCapabilities.installedVoiceCount}", style = MaterialTheme.typography.bodyMedium, color = TurquoiseOnContainer)
                        Text("Arabic Voice Status: ${voiceCapabilities.bestArabicVoiceName}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = TurquoiseOnContainer)
                        Text("English: ${voiceCapabilities.hasEnglishVoice} • Russian: ${voiceCapabilities.hasRussianVoice} • French: ${voiceCapabilities.hasFrenchVoice}", style = MaterialTheme.typography.bodySmall, color = TurquoiseOnContainer)
                        Text(voiceCapabilities.notes, style = MaterialTheme.typography.bodySmall, color = TurquoiseOnContainer.copy(alpha = 0.88f))
                    }
                }
            }

            // 4. Evaluated Speech & Translation Model Matrix
            item {
                Text("Evaluated Speech & Translation Models", style = MaterialTheme.typography.titleLarge)
            }
            items(state.evaluatedModels) { spec ->
                Card(shape = RoundedCornerShape(16.dp)) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(spec.name, style = MaterialTheme.typography.titleMedium)
                        Text("${spec.role} • ${spec.license}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                        Text("Status: ${spec.runtimeStatus}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                        Text("Size: ${spec.sizeEstimate} | Req: ${spec.hardwareRequirement}", style = MaterialTheme.typography.bodySmall)
                        Text("Languages: ${spec.supportedLanguages}", style = MaterialTheme.typography.bodySmall)
                        Text("Limitations: ${spec.limitations}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            // 5. Real Human Speech & WER Evaluation Runner
            item {
                Card(shape = RoundedCornerShape(18.dp), modifier = Modifier.testTag("eval_trial_runner_card")) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Human Speech Evaluation & WER Calculator", style = MaterialTheme.typography.titleLarge)
                        Text(
                            "Enter the actual spoken reference transcript and recognized output (or paste a transcript captured in Live Translator). WER uses deterministic word-level Levenshtein distance and is never fabricated.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Text("Arabic Output Dialect Mode", style = MaterialTheme.typography.labelLarge)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            ArabicDialectMode.entries.forEach { mode ->
                                FilterChip(
                                    selected = state.arabicDialectMode == mode,
                                    onClick = { onSetArabicDialectMode(mode) },
                                    label = { Text(if (mode == ArabicDialectMode.EGYPTIAN_COLLOQUIAL) "Egyptian (ar-EG)" else "MSA (ar-MSA)") }
                                )
                            }
                        }

                        Text("Source Language", style = MaterialTheme.typography.labelLarge)
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(NiloraLanguage.entries) { lang ->
                                FilterChip(
                                    selected = sourceLang == lang,
                                    onClick = { sourceLang = lang },
                                    label = { Text(lang.code.uppercase()) }
                                )
                            }
                        }

                        Text("Target Language", style = MaterialTheme.typography.labelLarge)
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(NiloraLanguage.entries) { lang ->
                                FilterChip(
                                    selected = targetLang == lang,
                                    onClick = { targetLang = lang },
                                    label = { Text(lang.code.uppercase()) }
                                )
                            }
                        }

                        Text("Acoustic Scenario", style = MaterialTheme.typography.labelLarge)
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(scenarios) { sc ->
                                FilterChip(
                                    selected = selectedScenario == sc,
                                    onClick = { selectedScenario = sc },
                                    label = { Text(sc) }
                                )
                            }
                        }

                        OutlinedTextField(
                            value = referenceTranscript,
                            onValueChange = { referenceTranscript = it },
                            label = { Text("Human Reference Transcript (ground truth spoken words)") },
                            modifier = Modifier.fillMaxWidth().testTag("eval_ref_transcript_input")
                        )
                        OutlinedTextField(
                            value = recognizedTranscript,
                            onValueChange = { recognizedTranscript = it },
                            label = { Text("Recognized Transcript (hypothesis to translate & score)") },
                            modifier = Modifier.fillMaxWidth().testTag("eval_rec_transcript_input")
                        )
                        OutlinedTextField(
                            value = referenceTranslation,
                            onValueChange = { referenceTranslation = it },
                            label = { Text("Optional Human Reference Translation") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = humanNotes,
                            onValueChange = { humanNotes = it },
                            label = { Text("Naturalness / Dialect / Error Observations") },
                            modifier = Modifier.fillMaxWidth()
                        )

                        state.pipelineError?.let { Text(it, color = ErrorCoral, style = MaterialTheme.typography.bodySmall) }

                        Button(
                            onClick = {
                                onRecordTrial(
                                    sourceLang,
                                    targetLang,
                                    selectedScenario,
                                    referenceTranscript,
                                    recognizedTranscript,
                                    referenceTranslation,
                                    humanNotes
                                )
                            },
                            enabled = recognizedTranscript.isNotBlank() && state.pipelineStage != PipelineStage.TRANSLATING,
                            modifier = Modifier.fillMaxWidth().testTag("eval_run_trial_btn")
                        ) {
                            Text(if (state.pipelineStage == PipelineStage.TRANSLATING) "Translating..." else "Translate & Compute WER Trial")
                        }
                    }
                }
            }

            // 6. Recorded Evaluation Trials & Export
            if (state.evaluationTrials.isNotEmpty()) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Recorded Trials (${state.evaluationTrials.size})", style = MaterialTheme.typography.titleLarge)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TextButton(onClick = {
                                val report = if (hw != null) {
                                    SpeechEvaluationHarness.formatMarkdownReport(state.evaluationTrials, hw)
                                } else ""
                                if (report.isNotBlank()) {
                                    val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(Intent.EXTRA_TEXT, report)
                                    }
                                    context.startActivity(Intent.createChooser(sendIntent, "Export Evaluation Report"))
                                }
                            }) {
                                Text("Export")
                            }
                            TextButton(onClick = onClearTrials) {
                                Text("Clear", color = ErrorCoral)
                            }
                        }
                    }
                }
                items(state.evaluationTrials) { t ->
                    Card(shape = RoundedCornerShape(16.dp)) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            val werStr = t.computedWer?.let { String.format("%.1f%%", it * 100.0) } ?: "N/A (no reference)"
                            Text("${t.sourceLanguage.code.uppercase()} → ${t.targetLanguage.code.uppercase()} • ${t.scenarioLabel}", style = MaterialTheme.typography.titleSmall)
                            Text("WER: $werStr • Dialect Hint: ${t.detectedDialect} • Translation Latency: ${t.translationLatencyMs}ms", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                            Text("Recognized: ${t.recognizedTranscript}", style = MaterialTheme.typography.bodySmall)
                            Text("Translation: ${t.actualTranslation}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                            if (t.validationWarnings.isNotEmpty()) {
                                Text("Warnings: ${t.validationWarnings.joinToString("; ")}", style = MaterialTheme.typography.labelSmall, color = WarningAmber)
                            }
                        }
                    }
                }
            }

            item { NiloraFooter() }
        }
    }
}
