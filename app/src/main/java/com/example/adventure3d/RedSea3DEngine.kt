package com.example.adventure3d

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import com.example.data.model.AdventureBiome
import com.example.data.model.AdventureExperience
import com.example.data.model.AdventurePoi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

data class Vec3(val x: Float, val y: Float, val z: Float)

data class Camera3D(
    val x: Float = 0f,
    val y: Float = -3.8f,
    val z: Float = 3.5f,
    val yawRad: Float = 0f,
    val pitchRad: Float = -0.06f
) {
    fun move(forwardDelta: Float, strafeDelta: Float, verticalDelta: Float): Camera3D {
        val sinY = sin(yawRad)
        val cosY = cos(yawRad)
        val dx = forwardDelta * (-sinY) + strafeDelta * cosY
        val dz = forwardDelta * (-cosY) - strafeDelta * sinY
        val nx = (x + dx).coerceIn(-38f, 38f)
        val ny = (y + verticalDelta).coerceIn(-16f, -0.8f)
        val nz = (z + dz).coerceIn(-48f, 24f)
        return copy(x = nx, y = ny, z = nz)
    }

    fun rotate(deltaYawRad: Float, deltaPitchRad: Float): Camera3D {
        val nextPitch = (pitchRad + deltaPitchRad).coerceIn(-0.92f, 0.85f)
        return copy(yawRad = yawRad + deltaYawRad, pitchRad = nextPitch)
    }
}

data class ProjectedPoint(
    val screenX: Float,
    val screenY: Float,
    val depthZ: Float,
    val scale: Float
)

data class CoralFormation3D(
    val id: String,
    val kind: Int, // 0 = staghorn, 1 = brain dome, 2 = sea fan, 3 = sandstone/pillar
    val pos: Vec3,
    val size: Float,
    val colorIndex: Int
)

data class MarineCreature3D(
    val id: String,
    val center: Vec3,
    val radiusX: Float,
    val radiusZ: Float,
    val speed: Float,
    val phase: Float,
    val size: Float,
    val species: Int // 0 = anthias, 1 = clownfish, 2 = turtle, 3 = bird/sail
)

data class BubbleParticle3D(
    val x: Float,
    val baseZ: Float,
    val speedY: Float,
    val phase: Float,
    val size: Float
)

object ProjectionMath3D {
    fun projectPoint(
        world: Vec3,
        camera: Camera3D,
        viewportWidth: Float,
        viewportHeight: Float,
        fovScale: Float = 0.95f
    ): ProjectedPoint? {
        val dx = world.x - camera.x
        val dy = world.y - camera.y
        val dz = world.z - camera.z

        // Yaw around Y axis (looking along -Z when yaw == 0)
        val cosY = cos(-camera.yawRad)
        val sinY = sin(-camera.yawRad)
        val x1 = dx * cosY - dz * sinY
        val z1 = dx * sinY + dz * cosY

        // Pitch around X axis
        val cosP = cos(-camera.pitchRad)
        val sinP = sin(-camera.pitchRad)
        val y2 = dy * cosP - (-z1) * sinP
        val forwardDepth = -(dy * sinP + (-z1) * cosP) * -1f
        // Compute camera forward coordinate explicitly: forward = -z in view space
        val viewZ = -z1 * cosP - dy * sinP
        val viewY = dy * cosP - (-z1) * sinP

        if (viewZ <= 0.45f) return null
        val focal = minOf(viewportWidth, viewportHeight) * fovScale
        val invZ = focal / viewZ
        val sx = viewportWidth * 0.5f + x1 * invZ
        val sy = viewportHeight * 0.5f - viewY * invZ
        return ProjectedPoint(
            screenX = sx,
            screenY = sy,
            depthZ = viewZ,
            scale = invZ
        )
    }

    fun distanceBetween(a: Vec3, b: Vec3): Float {
        val dx = a.x - b.x
        val dy = a.y - b.y
        val dz = a.z - b.z
        return sqrt(dx * dx + dy * dy + dz * dz)
    }

    fun hitTestPoi(
        tapX: Float,
        tapY: Float,
        pois: List<AdventurePoi>,
        camera: Camera3D,
        viewportWidth: Float,
        viewportHeight: Float,
        hitRadiusPx: Float = 68f
    ): AdventurePoi? {
        var bestPoi: AdventurePoi? = null
        var bestDist = hitRadiusPx
        for (poi in pois) {
            val projected = projectPoint(Vec3(poi.posX, poi.posY, poi.posZ), camera, viewportWidth, viewportHeight)
                ?: continue
            val d = sqrt((projected.screenX - tapX) * (projected.screenX - tapX) + (projected.screenY - tapY) * (projected.screenY - tapY))
            if (d <= bestDist) {
                bestDist = d
                bestPoi = poi
            }
        }
        return bestPoi
    }
}

data class AdventureSceneData(
    val biome: AdventureBiome,
    val formations: List<CoralFormation3D>,
    val creatures: List<MarineCreature3D>,
    val bubbles: List<BubbleParticle3D>
)

object ProceduralWorldGenerator {
    fun buildScene(experience: AdventureExperience): AdventureSceneData {
        val rng = Random(experience.id.hashCode())
        val isAquatic = experience.biome == AdventureBiome.RED_SEA_REEF ||
            experience.biome == AdventureBiome.MEDITERRANEAN_COAST

        val formations = buildList {
            // Generate structured terrain clusters + formations around POIs and canyon corridors
            for (i in 0 until 44) {
                val fx = rng.nextFloat() * 52f - 26f
                val fz = rng.nextFloat() * 44f - 36f
                val floorY = -9.5f - sin((fx * 0.25f).toDouble()).toFloat() * 1.4f
                add(
                    CoralFormation3D(
                        id = "form_$i",
                        kind = if (isAquatic) i % 3 else 3,
                        pos = Vec3(fx, floorY, fz),
                        size = 1.1f + rng.nextFloat() * 2.1f,
                        colorIndex = i % 5
                    )
                )
            }
            // Anchor vivid reef formations right beside every Discovery POI
            experience.pois.forEachIndexed { idx, poi ->
                add(
                    CoralFormation3D(
                        id = "poi_coral_${poi.id}",
                        kind = if (isAquatic) idx % 3 else 3,
                        pos = Vec3(poi.posX + 1.4f, poi.posY - 1.3f, poi.posZ - 1.1f),
                        size = 2.4f,
                        colorIndex = (idx + 2) % 5
                    )
                )
            }
        }

        val creatures = buildList {
            val count = if (isAquatic) 22 else 8
            for (i in 0 until count) {
                add(
                    MarineCreature3D(
                        id = "creature_$i",
                        center = Vec3(
                            x = rng.nextFloat() * 28f - 14f,
                            y = if (isAquatic) (-2.2f - rng.nextFloat() * 6.5f) else (0.5f + rng.nextFloat() * 3.5f),
                            z = -6f - rng.nextFloat() * 26f
                        ),
                        radiusX = 2.2f + rng.nextFloat() * 4.5f,
                        radiusZ = 1.8f + rng.nextFloat() * 3.8f,
                        speed = 0.35f + rng.nextFloat() * 0.55f,
                        phase = rng.nextFloat() * (2f * PI.toFloat()),
                        size = if (isAquatic && i == 0) 1.55f else 0.45f + rng.nextFloat() * 0.45f,
                        species = if (!isAquatic) 3 else when {
                            i == 0 -> 2 // gliding sea turtle
                            i % 4 == 0 -> 1 // Red Sea clownfish
                            else -> 0 // anthias / reef fish
                        }
                    )
                )
            }
        }

        val bubbles = buildList {
            for (i in 0 until 26) {
                add(
                    BubbleParticle3D(
                        x = rng.nextFloat() * 34f - 17f,
                        baseZ = -4f - rng.nextFloat() * 28f,
                        speedY = 0.65f + rng.nextFloat() * 0.8f,
                        phase = rng.nextFloat() * 12f,
                        size = 0.06f + rng.nextFloat() * 0.08f
                    )
                )
            }
        }

        return AdventureSceneData(
            biome = experience.biome,
            formations = formations,
            creatures = creatures,
            bubbles = bubbles
        )
    }
}

/**
 * Pure open-source procedural PCM ambient sound generator using Android's AudioTrack.
 * Produces soft low-passed ocean current ambiance (or desert wind) with zero external copyrighted assets.
 */
class AmbientOceanAudioEngine {
    private var audioJob: Job? = null
    private var track: AudioTrack? = null
    @Volatile
    private var chimeTriggerFrames = 0

    fun start(biome: AdventureBiome, scope: CoroutineScope) {
        if (audioJob != null) return
        audioJob = scope.launch(Dispatchers.IO) {
            try {
                val sampleRate = 16000
                val minBuf = AudioTrack.getMinBufferSize(
                    sampleRate,
                    AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT
                )
                if (minBuf <= 0) return@launch
                val audioTrack = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(sampleRate)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build()
                    )
                    .setBufferSizeInBytes(maxOf(minBuf, 4096))
                    .setTransferMode(AudioTrack.MODE_STREAM)
                    .build()

                track = audioTrack
                audioTrack.play()

                val buffer = ShortArray(1024)
                var lp = 0.0
                var lp2 = 0.0
                var phase = 0.0
                val isAquatic = biome == AdventureBiome.RED_SEA_REEF || biome == AdventureBiome.MEDITERRANEAN_COAST
                val lfoStep = 2.0 * PI * (if (isAquatic) 0.14 else 0.09) / sampleRate
                var chimePhase = 0.0

                while (isActive) {
                    for (i in buffer.indices) {
                        val white = (Math.random() * 2.0 - 1.0)
                        // Gentle double low-pass filter mimics hydrophone water flow / soft wind
                        val cutoff = if (isAquatic) 0.038 else 0.025
                        lp += cutoff * (white - lp)
                        lp2 += cutoff * (lp - lp2)
                        phase += lfoStep
                        val swell = 0.55 + 0.45 * sin(phase)
                        var sample = lp2 * swell * 1800.0

                        if (chimeTriggerFrames > 0) {
                            chimeTriggerFrames--
                            val env = (chimeTriggerFrames / 4800.0).coerceIn(0.0, 1.0)
                            chimePhase += 2.0 * PI * 528.0 / sampleRate
                            sample += sin(chimePhase) * env * 1100.0
                        }
                        buffer[i] = sample.toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
                    }
                    audioTrack.write(buffer, 0, buffer.size)
                }
            } catch (_: Throwable) {
                // Gracefully no-op on Robolectric JVM or hardware lacking audio sink
            }
        }
    }

    fun playDiscoveryChime() {
        chimeTriggerFrames = 4800
    }

    fun stop() {
        audioJob?.cancel()
        audioJob = null
        try {
            track?.stop()
            track?.release()
        } catch (_: Throwable) {
        }
        track = null
    }
}
