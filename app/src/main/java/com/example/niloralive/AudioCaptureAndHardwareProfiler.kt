package com.example.niloralive

import android.Manifest
import android.annotation.SuppressLint
import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.media.audiofx.AcousticEchoCanceler
import android.media.audiofx.NoiseSuppressor
import android.os.Build
import android.speech.RecognitionService
import android.speech.SpeechRecognizer
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.log10
import kotlin.math.sqrt

object AudioCaptureAndHardwareProfiler {

    fun inspectCapabilities(context: Context): HardwareAndSpeechCapabilities {
        val appContext = context.applicationContext
        val micGranted = ContextCompat.checkSelfPermission(
            appContext,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        val recognizerAvail = try {
            SpeechRecognizer.isRecognitionAvailable(appContext)
        } catch (_: Exception) {
            false
        }

        val servicePackages = try {
            val intent = Intent(RecognitionService.SERVICE_INTERFACE)
            val resolved = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                appContext.packageManager.queryIntentServices(
                    intent,
                    PackageManager.ResolveInfoFlags.of(0)
                )
            } else {
                @Suppress("DEPRECATION")
                appContext.packageManager.queryIntentServices(intent, 0)
            }
            resolved.mapNotNull { it.serviceInfo?.packageName }.distinct()
        } catch (_: Exception) {
            emptyList()
        }

        val minBuf = try {
            AudioRecord.getMinBufferSize(
                16_000,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            )
        } catch (_: Exception) {
            AudioRecord.ERROR_BAD_VALUE
        }
        val pcmSupported = minBuf > 0

        val nsAvail = try {
            NoiseSuppressor.isAvailable()
        } catch (_: Exception) {
            false
        }
        val aecAvail = try {
            AcousticEchoCanceler.isAvailable()
        } catch (_: Exception) {
            false
        }

        val memInfo = ActivityManager.MemoryInfo()
        val am = appContext.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
        am?.getMemoryInfo(memInfo)
        val totalRamMb = memInfo.totalMem / (1024L * 1024L)
        val cores = Runtime.getRuntime().availableProcessors()
        val abis = Build.SUPPORTED_ABIS?.toList().orEmpty()

        return HardwareAndSpeechCapabilities(
            micPermissionGranted = micGranted,
            speechRecognizerAvailable = recognizerAvail,
            recognitionServicePackages = servicePackages,
            audioRecord16kSupported = pcmSupported,
            hardwareNoiseSuppressorAvailable = nsAvail,
            hardwareEchoCancelerAvailable = aecAvail,
            totalRamMb = totalRamMb,
            cpuCores = cores,
            supportedAbis = abis,
            canHostWhisperTinyInTheory = totalRamMb >= 2048 && abis.any { it.contains("arm64") || it.contains("x86_64") },
            canHostWhisperSmallInTheory = totalRamMb >= 4096 && abis.any { it.contains("arm64") || it.contains("x86_64") },
            whisperNativeBinaryBundled = false
        )
    }

    fun getEvaluatedModelCatalog(hw: HardwareAndSpeechCapabilities): List<ModelSpecInfo> = listOf(
        ModelSpecInfo(
            id = "android_stt",
            name = "Android System SpeechRecognizer (On-Device / Service Provider)",
            role = "Speech-to-Text (Active Default)",
            license = "Android Platform API (Provider-dependent)",
            sizeEstimate = "0 MB bundled (uses system RecognitionService)",
            hardwareRequirement = "Android API 24+, Microphone; quality varies by device vendor",
            supportedLanguages = "ar-EG, en-US, ru-RU, fr-FR (depends on installed language packs)",
            runtimeStatus = if (hw.speechRecognizerAvailable) {
                "ACTIVE (${hw.recognitionServicePackages.joinToString().ifBlank { "System service" }})"
            } else {
                "UNAVAILABLE ON THIS DEVICE (editable text input fallback active)"
            },
            limitations = "Dialect recognition for Egyptian Arabic depends on installed Google/vendor speech packs; background noise affects accuracy."
        ),
        ModelSpecInfo(
            id = "firebase_ai_gemini",
            name = "Firebase AI Logic — Gemini 3.8 Flash",
            role = "Egyptian Arabic Dialect & Multilingual Translation (Active Default)",
            license = "Google Firebase Terms / Free-tier quota within project limits",
            sizeEstimate = "Cloud API via App Check (0 MB local weights)",
            hardwareRequirement = "Active internet connection + Firebase App Check verification",
            supportedLanguages = "Egyptian Arabic (ar-EG), MSA (ar-MSA), English (en), Russian (ru), French (fr)",
            runtimeStatus = "ACTIVE (Requires network & explicit user consent)",
            limitations = "Cloud-dependent; subject to Firebase Spark/Blaze project rate limits and network latency."
        ),
        ModelSpecInfo(
            id = "whisper_cpp",
            name = "whisper.cpp (OpenAI Whisper GGUF)",
            role = "Offline Open-Source Speech Recognition (Evaluated Alternative)",
            license = "MIT License",
            sizeEstimate = "tiny ~75 MB (Q5), base ~142 MB, small ~466 MB",
            hardwareRequirement = "arm64-v8a NDK JNI (libwhisper.so), >=2 GB RAM for tiny, >=4 GB RAM for small",
            supportedLanguages = "Multilingual (ar, en, ru, fr) — Arabic outputs mainly MSA unless fine-tuned for ar-EG",
            runtimeStatus = "NOT BUNDLED (Device RAM: ${hw.totalRamMb} MB, tiny-capable=${hw.canHostWhisperTinyInTheory})",
            limitations = "Requires compiling C++ JNI native library & downloading 75–466 MB model weights; base/tiny models often normalize Egyptian dialect into MSA."
        ),
        ModelSpecInfo(
            id = "faster_whisper",
            name = "faster-whisper (CTranslate2)",
            role = "Server / Desktop Speech Recognition (Evaluated Alternative)",
            license = "MIT License",
            sizeEstimate = "tiny ~75 MB to large-v3 ~1.6 GB (INT8)",
            hardwareRequirement = "Python + CTranslate2 runtime (Linux/GPU server or desktop; not native Android APK)",
            supportedLanguages = "Multilingual (ar, en, ru, fr)",
            runtimeStatus = "SERVER-ONLY (Not deployable inside client-only Android APK)",
            limitations = "Requires external Python server hosting; would add network hop and free-tier server memory constraints."
        ),
        ModelSpecInfo(
            id = "nllb_200",
            name = "Meta NLLB-200-distilled-600M",
            role = "Open-Source Offline Translation (Evaluated Alternative)",
            license = "CC-BY-NC 4.0 (Non-Commercial unless licensed separately)",
            sizeEstimate = "~1.2 GB INT8 / ~2.4 GB FP16",
            hardwareRequirement = "Server GPU/CPU (>=4 GB RAM) or ONNX Runtime Mobile (heavy for mid-range phones)",
            supportedLanguages = "Includes arz_Arab (Egyptian Arabic), arb_Arab (MSA), eng_Latn, rus_Cyrl, fra_Latn",
            runtimeStatus = "NOT BUNDLED (Exceeds lightweight APK size; CC-BY-NC restricts commercial use)",
            limitations = "Large memory footprint on mobile; shorter conversational slang quality trails instruction-tuned dialect models."
        ),
        ModelSpecInfo(
            id = "android_tts",
            name = "Android TextToSpeech (Installed System Voices)",
            role = "Natural Voice Generation (Active Default)",
            license = "Android Platform API (Zero-cost on-device)",
            sizeEstimate = "0 MB bundled (uses installed system voices)",
            hardwareRequirement = "Android TTS engine with language voice data installed",
            supportedLanguages = "ar-EG / generic ar, en-US, ru-RU, fr-FR",
            runtimeStatus = "ACTIVE (Voice availability checked dynamically per locale)",
            limitations = "Some Android devices ship only generic 'ar' voices without dedicated Egyptian 'ar-EG' voice packages."
        )
    )

    /**
     * Measures real ambient microphone noise floor and peak dBFS using AudioRecord (16kHz PCM mono)
     * with optional hardware NoiseSuppressor & AcousticEchoCanceler attached if supported by device.
     * Used in the Speech Diagnostics & Evaluation Lab (never fakes measurements).
     */
    @SuppressLint("MissingPermission")
    suspend fun measureRealAmbientNoiseSample(
        context: Context,
        durationMs: Long = 1200L
    ): Result<AmbientAudioSample> = withContext(Dispatchers.IO) {
        val granted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
        if (!granted) {
            return@withContext Result.failure(SecurityException("RECORD_AUDIO permission not granted."))
        }

        val sampleRate = 16_000
        val minBuf = AudioRecord.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        if (minBuf <= 0) {
            return@withContext Result.failure(IllegalStateException("Device does not support 16kHz PCM mono AudioRecord."))
        }

        var recorder: AudioRecord? = null
        var ns: NoiseSuppressor? = null
        var aec: AcousticEchoCanceler? = null
        try {
            recorder = AudioRecord(
                MediaRecorder.AudioSource.VOICE_COMMUNICATION,
                sampleRate,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                minBuf * 2
            )
            if (recorder.state != AudioRecord.STATE_INITIALIZED) {
                return@withContext Result.failure(IllegalStateException("AudioRecord failed to initialize microphone."))
            }
            val sessionId = recorder.audioSessionId
            val nsAttached = if (NoiseSuppressor.isAvailable()) {
                ns = NoiseSuppressor.create(sessionId)?.apply { enabled = true }
                ns != null
            } else false
            val aecAttached = if (AcousticEchoCanceler.isAvailable()) {
                aec = AcousticEchoCanceler.create(sessionId)?.apply { enabled = true }
                aec != null
            } else false

            recorder.startRecording()
            val buffer = ShortArray(minBuf / 2)
            val deadline = System.currentTimeMillis() + durationMs
            var sumSq = 0.0
            var totalSamples = 0L
            var peak = 0

            while (System.currentTimeMillis() < deadline) {
                val read = recorder.read(buffer, 0, buffer.size)
                if (read > 0) {
                    for (i in 0 until read) {
                        val s = buffer[i].toInt()
                        val absVal = kotlin.math.abs(s)
                        if (absVal > peak) peak = absVal
                        sumSq += s.toDouble() * s.toDouble()
                    }
                    totalSamples += read
                }
            }
            recorder.stop()

            if (totalSamples == 0L) {
                return@withContext Result.failure(IllegalStateException("No PCM audio samples received from microphone."))
            }
            val rms = sqrt(sumSq / totalSamples)
            val rmsDbfs = if (rms > 0.0) 20.0 * log10(rms / 32767.0) else -96.0
            val peakDbfs = if (peak > 0) 20.0 * log10(peak / 32767.0) else -96.0

            Result.success(
                AmbientAudioSample(
                    sampleRateHz = sampleRate,
                    rmsDbfs = rmsDbfs,
                    peakDbfs = peakDbfs,
                    noiseSuppressorActive = nsAttached,
                    echoCancelerActive = aecAttached
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        } finally {
            try {
                ns?.release()
                aec?.release()
                recorder?.release()
            } catch (_: Exception) {
            }
        }
    }
}

data class AmbientAudioSample(
    val sampleRateHz: Int,
    val rmsDbfs: Double,
    val peakDbfs: Double,
    val noiseSuppressorActive: Boolean,
    val echoCancelerActive: Boolean
)
