package com.example.niloralive

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Build
import android.util.Base64
import androidx.core.content.ContextCompat
import java.io.File

data class RecordedVoiceClip(
    val audioBase64: String,
    val durationMs: Long,
    val mimeType: String = "audio/mp4"
)

/**
 * Real Android MediaRecorder + MediaPlayer wrapper for 1:1 voice notes.
 *
 * Strictly device-captured audio only (no synthetic/simulated waveforms or prerecorded stubs).
 * Captures compact AAC (.m4a, 16 kHz / 16 kbps mono, max 20s) bounded within Firestore's
 * 95,000-char payload limit so cross-device voice notes work on the free Spark tier.
 */
class VoiceMessageAudioRecorder(private val context: Context) {

    private var recorder: MediaRecorder? = null
    private var outputFile: File? = null
    private var startedAtMs: Long = 0L

    private var mediaPlayer: MediaPlayer? = null
    private var activePlaybackId: String? = null

    fun hasRecordPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
            PackageManager.PERMISSION_GRANTED

    fun startRecording(): Result<Unit> {
        if (!hasRecordPermission()) {
            return Result.failure(IllegalStateException("Microphone permission is required to record a voice message."))
        }
        stopRecordingCleanup()
        return try {
            val file = File(context.cacheDir, "nilora_voice_${System.currentTimeMillis()}.m4a")
            val mr = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }
            mr.setAudioSource(MediaRecorder.AudioSource.MIC)
            mr.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            mr.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            mr.setAudioSamplingRate(16000)
            mr.setAudioEncodingBitRate(16000)
            mr.setAudioChannels(1)
            mr.setMaxDuration(MAX_DURATION_MS)
            mr.setOutputFile(file.absolutePath)
            mr.prepare()
            mr.start()

            recorder = mr
            outputFile = file
            startedAtMs = System.currentTimeMillis()
            Result.success(Unit)
        } catch (e: Exception) {
            stopRecordingCleanup()
            Result.failure(IllegalStateException("Unable to access device microphone: ${e.localizedMessage ?: "Audio hardware busy"}"))
        }
    }

    fun stopRecording(): Result<RecordedVoiceClip> {
        val mr = recorder ?: return Result.failure(IllegalStateException("No active voice recording."))
        val file = outputFile
        val elapsedMs = (System.currentTimeMillis() - startedAtMs).coerceIn(300L, MAX_DURATION_MS.toLong())
        return try {
            mr.stop()
            mr.release()
            recorder = null
            if (file == null || !file.exists() || file.length() == 0L) {
                Result.failure(IllegalStateException("Voice recording produced an empty file."))
            } else {
                val bytes = file.readBytes()
                file.delete()
                outputFile = null
                val encoded = Base64.encodeToString(bytes, Base64.NO_WRAP)
                if (encoded.length > MAX_BASE64_LENGTH) {
                    Result.failure(
                        IllegalStateException("Voice note is too long (${bytes.size / 1024} KB). Keep voice notes under 20 seconds.")
                    )
                } else {
                    Result.success(RecordedVoiceClip(audioBase64 = encoded, durationMs = elapsedMs))
                }
            }
        } catch (e: Exception) {
            stopRecordingCleanup()
            Result.failure(IllegalStateException("Recording stopped unsuccessfully: ${e.localizedMessage ?: "Too short"}"))
        }
    }

    fun cancelRecording() {
        stopRecordingCleanup()
    }

    fun playVoiceClip(
        messageId: String,
        audioBase64: String,
        onComplete: () -> Unit,
        onError: (String) -> Unit
    ) {
        stopPlayback()
        if (audioBase64.isBlank()) {
            onError("No audio data attached to this message.")
            return
        }
        try {
            val bytes = Base64.decode(audioBase64, Base64.DEFAULT)
            val tempFile = File(context.cacheDir, "nilora_play_${messageId.take(24)}.m4a")
            tempFile.writeBytes(bytes)
            val player = MediaPlayer()
            player.setDataSource(tempFile.absolutePath)
            player.setOnCompletionListener {
                tempFile.delete()
                stopPlayback()
                onComplete()
            }
            player.setOnErrorListener { _, what, _ ->
                tempFile.delete()
                stopPlayback()
                onError("Audio playback error ($what).")
                true
            }
            player.prepare()
            player.start()
            mediaPlayer = player
            activePlaybackId = messageId
        } catch (e: Exception) {
            stopPlayback()
            onError("Unable to play voice message: ${e.localizedMessage ?: "Unsupported audio format"}")
        }
    }

    fun stopPlayback() {
        activePlaybackId = null
        try {
            mediaPlayer?.stop()
        } catch (_: Exception) {
        }
        try {
            mediaPlayer?.release()
        } catch (_: Exception) {
        }
        mediaPlayer = null
    }

    fun currentlyPlayingId(): String? = activePlaybackId

    fun release() {
        stopRecordingCleanup()
        stopPlayback()
    }

    private fun stopRecordingCleanup() {
        try {
            recorder?.stop()
        } catch (_: Exception) {
        }
        try {
            recorder?.release()
        } catch (_: Exception) {
        }
        recorder = null
        outputFile?.delete()
        outputFile = null
    }

    companion object {
        const val MAX_DURATION_MS = 20_000
        const val MAX_BASE64_LENGTH = 90_000
    }
}
