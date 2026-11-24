package com.example.adventure3d

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.example.data.model.AdventureBiome
import com.example.data.model.AdventureExperience
import com.example.data.model.AdventurePoi
import com.example.data.model.SessionParticipantPresence
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

private data class DrawableItem3D(
    val depthZ: Float,
    val draw: DrawScope.() -> Unit
)

@Composable
fun Immersive3DWorldCanvas(
    adventure: AdventureExperience,
    camera: Camera3D,
    currentUserId: String,
    participants: List<SessionParticipantPresence>,
    discoveredPoiIds: Set<String>,
    selectedPoi: AdventurePoi?,
    audioMuted: Boolean,
    onCameraChange: (Camera3D) -> Unit,
    onSelectPoi: (AdventurePoi) -> Unit,
    onPointAtPoi: (AdventurePoi) -> Unit,
    onToggleAudio: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sceneData = remember(adventure.id) { ProceduralWorldGenerator.buildScene(adventure) }
    var viewportSize by remember { mutableStateOf(IntSize(1, 1)) }
    var timeSec by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(adventure.id) {
        while (true) {
            delay(33L) // ~30 fps smooth mobile battery-friendly loop
            timeSec += 0.033f
        }
    }

    val currentCamera by rememberUpdatedState(camera)
    val isAquatic = adventure.biome == AdventureBiome.RED_SEA_REEF ||
        adventure.biome == AdventureBiome.MEDITERRANEAN_COAST

    // Compute projected 2D overlay labels for participants & POIs so Compose text renders crisply
    val otherParticipants = remember(participants, currentUserId) {
        participants.filter { it.userId != currentUserId }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(MidnightNavy)
            .onSizeChanged { viewportSize = it }
            .testTag("immersive_3d_viewport")
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(adventure.id) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        onCameraChange(
                            currentCamera.rotate(
                                deltaYawRad = -dragAmount.x * 0.0042f,
                                deltaPitchRad = -dragAmount.y * 0.0036f
                            )
                        )
                    }
                }
                .pointerInput(adventure.id, viewportSize) {
                    detectTapGestures { offset ->
                        val w = viewportSize.width.toFloat().coerceAtLeast(1f)
                        val h = viewportSize.height.toFloat().coerceAtLeast(1f)
                        val tapped = ProjectionMath3D.hitTestPoi(
                            tapX = offset.x,
                            tapY = offset.y,
                            pois = adventure.pois,
                            camera = currentCamera,
                            viewportWidth = w,
                            viewportHeight = h,
                            hitRadiusPx = 76f
                        )
                        if (tapped != null) {
                            onSelectPoi(tapped)
                        }
                    }
                }
        ) {
            val w = size.width
            val h = size.height

            // 1. Atmospheric Depth Backdrop & Lighting
            drawAtmosphere(adventure.biome, currentCamera, timeSec, w, h)

            // 2. Collect 3D scene items for back-to-front Painter's sorting
            val drawQueue = ArrayList<DrawableItem3D>(120)

            // Sunlit seafloor / terrain grid contours
            for (gx in -24..24 step 8) {
                for (gz in -36..4 step 8) {
                    val floorY = if (isAquatic) (-10.2f - sin((gx * 0.25f).toDouble()).toFloat() * 1.1f) else -3.6f
                    val p = ProjectionMath3D.projectPoint(Vec3(gx.toFloat(), floorY, gz.toFloat()), currentCamera, w, h)
                    if (p != null && p.depthZ < 44f) {
                        val fog = (1f - (p.depthZ / 44f)).coerceIn(0.08f, 0.7f)
                        val dotColor = if (isAquatic) WarmSand.copy(alpha = fog * 0.25f) else EgyptianGold.copy(alpha = fog * 0.22f)
                        drawQueue.add(DrawableItem3D(p.depthZ) {
                            drawCircle(color = dotColor, radius = (p.scale * 1.3f).coerceIn(4f, 46f), center = Offset(p.screenX, p.screenY))
                        })
                    }
                }
            }

            // Reef Coral / Desert / Heritage Formations
            val coralPalette = listOf(
                SunsetCoral,
                NileTurquoise,
                EgyptianGold,
                Color(0xFFDF6C8E),
                WarmSand
            )
            sceneData.formations.forEach { f ->
                val proj = ProjectionMath3D.projectPoint(f.pos, currentCamera, w, h) ?: return@forEach
                if (proj.depthZ > 45f) return@forEach
                val fogFactor = (1f - proj.depthZ / 46f).coerceIn(0.15f, 1f)
                val baseColor = if (isAquatic) coralPalette[f.colorIndex] else WarmSandDark
                val tinted = baseColor.copy(alpha = 0.88f * fogFactor)
                val radiusPx = (proj.scale * f.size).coerceIn(4f, 90f)
                drawQueue.add(DrawableItem3D(proj.depthZ) {
                    when (f.kind) {
                        0 -> { // Staghorn branches
                            for (b in -1..1) {
                                val tipX = proj.screenX + b * radiusPx * 0.65f
                                val tipY = proj.screenY - radiusPx * (1.25f - kotlin.math.abs(b) * 0.2f)
                                drawLine(
                                    color = tinted,
                                    start = Offset(proj.screenX, proj.screenY),
                                    end = Offset(tipX, tipY),
                                    strokeWidth = (radiusPx * 0.28f).coerceIn(2f, 14f)
                                )
                                drawCircle(
                                    color = tinted.copy(alpha = tinted.alpha * 0.9f),
                                    radius = (radiusPx * 0.24f).coerceIn(2f, 12f),
                                    center = Offset(tipX, tipY)
                                )
                            }
                        }
                        1 -> { // Brain Coral Dome
                            drawArc(
                                color = tinted,
                                startAngle = 180f,
                                sweepAngle = 180f,
                                useCenter = true,
                                topLeft = Offset(proj.screenX - radiusPx, proj.screenY - radiusPx * 0.7f),
                                size = Size(radiusPx * 2f, radiusPx * 1.4f)
                            )
                            drawArc(
                                color = Color.White.copy(alpha = 0.16f * fogFactor),
                                startAngle = 195f,
                                sweepAngle = 140f,
                                useCenter = false,
                                topLeft = Offset(proj.screenX - radiusPx * 0.8f, proj.screenY - radiusPx * 0.55f),
                                size = Size(radiusPx * 1.6f, radiusPx * 1.1f),
                                style = Stroke(width = 2f)
                            )
                        }
                        2 -> { // Sea Fan
                            drawOval(
                                color = tinted.copy(alpha = tinted.alpha * 0.72f),
                                topLeft = Offset(proj.screenX - radiusPx * 0.9f, proj.screenY - radiusPx * 1.3f),
                                size = Size(radiusPx * 1.8f, radiusPx * 1.5f)
                            )
                        }
                        else -> { // Sandstone Pillar / Oasis Rock
                            drawRect(
                                color = tinted,
                                topLeft = Offset(proj.screenX - radiusPx * 0.45f, proj.screenY - radiusPx * 1.9f),
                                size = Size(radiusPx * 0.9f, radiusPx * 2.0f)
                            )
                        }
                    }
                })
            }

            // Animated Marine Life / Desert Birds
            sceneData.creatures.forEach { c ->
                val angle = timeSec * c.speed + c.phase
                val wx = c.center.x + cos(angle) * c.radiusX
                val wz = c.center.z + sin(angle) * c.radiusZ
                val wy = c.center.y + sin(timeSec * 1.4f + c.phase) * 0.35f
                val proj = ProjectionMath3D.projectPoint(Vec3(wx, wy, wz), currentCamera, w, h) ?: return@forEach
                if (proj.depthZ > 42f) return@forEach
                val fog = (1f - proj.depthZ / 42f).coerceIn(0.2f, 1f)
                val bodyPx = (proj.scale * c.size).coerceIn(4f, 44f)
                val swimDir = if (sin(angle) >= 0f) 1f else -1f

                drawQueue.add(DrawableItem3D(proj.depthZ) {
                    when (c.species) {
                        2 -> { // Gliding Sea Turtle
                            drawOval(
                                color = Color(0xFF4CA37F).copy(alpha = 0.92f * fog),
                                topLeft = Offset(proj.screenX - bodyPx, proj.screenY - bodyPx * 0.48f),
                                size = Size(bodyPx * 2f, bodyPx * 0.96f)
                            )
                            val flap = sin(timeSec * 3.5f) * bodyPx * 0.45f
                            drawLine(
                                color = Color(0xFF7AD0A6).copy(alpha = 0.9f * fog),
                                start = Offset(proj.screenX, proj.screenY),
                                end = Offset(proj.screenX + swimDir * bodyPx * 0.5f, proj.screenY + flap + bodyPx * 0.4f),
                                strokeWidth = (bodyPx * 0.28f).coerceIn(2f, 9f)
                            )
                        }
                        1 -> { // Red Sea Clownfish
                            val color = SunsetCoral.copy(alpha = 0.96f * fog)
                            drawOval(
                                color = color,
                                topLeft = Offset(proj.screenX - bodyPx * 0.7f, proj.screenY - bodyPx * 0.4f),
                                size = Size(bodyPx * 1.4f, bodyPx * 0.8f)
                            )
                            drawLine(
                                color = Color.White.copy(alpha = 0.92f * fog),
                                start = Offset(proj.screenX, proj.screenY - bodyPx * 0.36f),
                                end = Offset(proj.screenX, proj.screenY + bodyPx * 0.36f),
                                strokeWidth = (bodyPx * 0.25f).coerceIn(1.5f, 5f)
                            )
                        }
                        0 -> { // Vibrant Reef Anthias
                            val fishColor = EgyptianGold.copy(alpha = 0.92f * fog)
                            drawOval(
                                color = fishColor,
                                topLeft = Offset(proj.screenX - bodyPx * 0.65f, proj.screenY - bodyPx * 0.32f),
                                size = Size(bodyPx * 1.3f, bodyPx * 0.64f)
                            )
                            val tailX = proj.screenX - swimDir * bodyPx * 0.75f
                            val tailOsc = sin(timeSec * 7f + c.phase) * bodyPx * 0.22f
                            drawLine(
                                color = fishColor,
                                start = Offset(proj.screenX - swimDir * bodyPx * 0.4f, proj.screenY),
                                end = Offset(tailX, proj.screenY + tailOsc),
                                strokeWidth = (bodyPx * 0.28f).coerceIn(2f, 7f)
                            )
                        }
                        else -> { // Desert Falcon / Nile Bird Silhouette
                            val wingY = sin(timeSec * 5f + c.phase) * bodyPx * 0.4f
                            drawLine(
                                color = DeepNavyDark.copy(alpha = 0.75f * fog),
                                start = Offset(proj.screenX - bodyPx, proj.screenY + wingY),
                                end = Offset(proj.screenX, proj.screenY),
                                strokeWidth = 3f
                            )
                            drawLine(
                                color = DeepNavyDark.copy(alpha = 0.75f * fog),
                                start = Offset(proj.screenX, proj.screenY),
                                end = Offset(proj.screenX + bodyPx, proj.screenY + wingY),
                                strokeWidth = 3f
                            )
                        }
                    }
                })
            }

            // Rising Bubbles (Aquatic biomes)
            if (isAquatic) {
                sceneData.bubbles.forEach { b ->
                    val cycleY = -10f + ((timeSec * b.speedY + b.phase) % 10f)
                    val wobbleX = b.x + sin(timeSec * 2f + b.phase) * 0.22f
                    val proj = ProjectionMath3D.projectPoint(Vec3(wobbleX, cycleY, b.baseZ), currentCamera, w, h)
                        ?: return@forEach
                    if (proj.depthZ < 30f) {
                        val r = (proj.scale * b.size).coerceIn(2f, 11f)
                        drawQueue.add(DrawableItem3D(proj.depthZ) {
                            drawCircle(
                                color = Color.White.copy(alpha = 0.45f),
                                radius = r,
                                center = Offset(proj.screenX, proj.screenY),
                                style = Stroke(width = 1.5f)
                            )
                        })
                    }
                }
            }

            // Interactive Discovery POIs in 3D space
            val pulse = 0.85f + 0.2f * sin(timeSec * 3.4f)
            adventure.pois.forEach { poi ->
                val proj = ProjectionMath3D.projectPoint(Vec3(poi.posX, poi.posY, poi.posZ), currentCamera, w, h)
                    ?: return@forEach
                val isDiscovered = poi.id in discoveredPoiIds
                val isSelected = selectedPoi?.id == poi.id
                val ringColor = when {
                    isSelected -> SunsetCoral
                    isDiscovered -> NileTurquoise
                    else -> EgyptianGold
                }
                val markerR = (proj.scale * 0.65f).coerceIn(16f, 44f)
                drawQueue.add(DrawableItem3D(proj.depthZ) {
                    drawCircle(
                        color = ringColor.copy(alpha = 0.22f),
                        radius = markerR * 1.45f * pulse,
                        center = Offset(proj.screenX, proj.screenY)
                    )
                    drawCircle(
                        color = ringColor.copy(alpha = 0.92f),
                        radius = markerR * 0.62f,
                        center = Offset(proj.screenX, proj.screenY),
                        style = Stroke(width = 3.5f)
                    )
                    drawCircle(
                        color = SoftIvory,
                        radius = markerR * 0.24f,
                        center = Offset(proj.screenX, proj.screenY)
                    )
                })
            }

            // Real Other Live Participants & Pointing Beacons
            otherParticipants.forEach { p ->
                val proj = ProjectionMath3D.projectPoint(
                    Vec3(p.posX.toFloat(), p.posY.toFloat(), p.posZ.toFloat()),
                    currentCamera,
                    w,
                    h
                ) ?: return@forEach
                val avRadius = (proj.scale * 0.72f).coerceIn(16f, 42f)
                drawQueue.add(DrawableItem3D(proj.depthZ) {
                    drawCircle(
                        color = NileTurquoise.copy(alpha = 0.30f),
                        radius = avRadius * 1.4f,
                        center = Offset(proj.screenX, proj.screenY)
                    )
                    drawCircle(
                        color = NileTurquoise,
                        radius = avRadius,
                        center = Offset(proj.screenX, proj.screenY)
                    )
                    drawCircle(
                        color = SoftIvory,
                        radius = avRadius * 0.42f,
                        center = Offset(proj.screenX, proj.screenY - avRadius * 0.15f)
                    )
                    // Draw Pointing Ray if companion is actively pointing at a 3D coordinate
                    if (p.pointedPoiId.isNotBlank()) {
                        val targetProj = ProjectionMath3D.projectPoint(
                            Vec3(p.pointedX.toFloat(), p.pointedY.toFloat(), p.pointedZ.toFloat()),
                            currentCamera,
                            w,
                            h
                        )
                        if (targetProj != null) {
                            drawLine(
                                color = SunsetCoral.copy(alpha = 0.82f),
                                start = Offset(proj.screenX, proj.screenY),
                                end = Offset(targetProj.screenX, targetProj.screenY),
                                strokeWidth = 3f
                            )
                        }
                    }
                })
            }

            // Sort far -> near & render
            drawQueue.sortByDescending { it.depthZ }
            drawQueue.forEach { it.draw(this) }
        }

        // Projected 2D POI Labels & Participant Name Badges
        val wPx = viewportSize.width.toFloat().coerceAtLeast(1f)
        val hPx = viewportSize.height.toFloat().coerceAtLeast(1f)

        adventure.pois.forEach { poi ->
            val proj = ProjectionMath3D.projectPoint(Vec3(poi.posX, poi.posY, poi.posZ), camera, wPx, hPx)
            if (proj != null && proj.screenX in 16f..(wPx - 16f) && proj.screenY in 20f..(hPx - 20f)) {
                val dist = ProjectionMath3D.distanceBetween(
                    Vec3(camera.x, camera.y, camera.z),
                    Vec3(poi.posX, poi.posY, poi.posZ)
                ).roundToInt()
                Surface(
                    onClick = { onSelectPoi(poi) },
                    color = MidnightNavy.copy(alpha = 0.82f),
                    shape = RoundedCornerShape(999.dp),
                    border = BorderStroke(1.dp, if (poi.id in discoveredPoiIds) NileTurquoise else EgyptianGold),
                    modifier = Modifier
                        .offset {
                            IntOffset(
                                (proj.screenX - 74).roundToInt().coerceIn(8, (viewportSize.width - 150).coerceAtLeast(8)),
                                (proj.screenY - 58).roundToInt().coerceIn(8, (viewportSize.height - 60).coerceAtLeast(8))
                            )
                        }
                        .testTag("poi_marker_${poi.id}")
                ) {
                    Text(
                        text = "${poi.name} • ${dist}m",
                        style = MaterialTheme.typography.labelSmall,
                        color = SoftIvory,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }

        otherParticipants.forEach { p ->
            val proj = ProjectionMath3D.projectPoint(
                Vec3(p.posX.toFloat(), p.posY.toFloat(), p.posZ.toFloat()),
                camera,
                wPx,
                hPx
            )
            if (proj != null && proj.screenX in 16f..(wPx - 16f) && proj.screenY in 20f..(hPx - 20f)) {
                Surface(
                    color = NileTurquoiseDark.copy(alpha = 0.9f),
                    shape = RoundedCornerShape(999.dp),
                    modifier = Modifier.offset {
                        IntOffset(
                            (proj.screenX - 65).roundToInt().coerceIn(8, (viewportSize.width - 140).coerceAtLeast(8)),
                            (proj.screenY - 54).roundToInt().coerceIn(8, (viewportSize.height - 56).coerceAtLeast(8))
                        )
                    }
                ) {
                    Text(
                        text = "${p.displayName} (${p.country})",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        maxLines = 1,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }
        }

        // Top HUD Telemetry Bar (Depth, Heading, Audio Mute, Participants Count)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
                .align(Alignment.TopCenter),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                color = DeepNavyDark.copy(alpha = 0.78f),
                shape = RoundedCornerShape(999.dp)
            ) {
                val depthM = (-camera.y).roundToInt().coerceAtLeast(0)
                val heading = (((camera.yawRad * 180.0 / PI).roundToInt() % 360) + 360) % 360
                Text(
                    text = if (isAquatic) "Depth ${depthM}m • ${heading}°" else "Elev ${depthM}m • ${heading}°",
                    style = MaterialTheme.typography.labelSmall,
                    color = SoftIvory,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    color = DeepNavyDark.copy(alpha = 0.78f),
                    shape = RoundedCornerShape(999.dp)
                ) {
                    Text(
                        text = "${discoveredPoiIds.size}/${adventure.pois.size} POIs",
                        style = MaterialTheme.typography.labelSmall,
                        color = EgyptianGold,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
                IconButton(
                    onClick = onToggleAudio,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(DeepNavyDark.copy(alpha = 0.78f))
                        .testTag("adventure_3d_audio_toggle")
                ) {
                    Icon(
                        imageVector = if (audioMuted) Icons.Filled.VolumeOff else Icons.Filled.VolumeUp,
                        contentDescription = if (audioMuted) "Unmute Ambient Sound" else "Mute Ambient Sound",
                        tint = SoftIvory
                    )
                }
            }
        }

        // Bottom-Left Movement Controls & Bottom-Right Buoyancy / Point Helper
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp)
                .align(Alignment.BottomCenter),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            // Directional Pad (48dp touch targets)
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                FilledTonalIconButton(
                    onClick = { onCameraChange(camera.move(forwardDelta = 1.8f, 0f, 0f)) },
                    modifier = Modifier.size(48.dp).testTag("move_forward_btn")
                ) { Icon(Icons.Filled.KeyboardArrowUp, contentDescription = "Move Forward") }
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    FilledTonalIconButton(
                        onClick = { onCameraChange(camera.move(0f, strafeDelta = -1.6f, 0f)) },
                        modifier = Modifier.size(48.dp).testTag("move_left_btn")
                    ) { Icon(Icons.Filled.KeyboardArrowLeft, contentDescription = "Move Left") }
                    FilledTonalIconButton(
                        onClick = { onCameraChange(camera.move(forwardDelta = -1.8f, 0f, 0f)) },
                        modifier = Modifier.size(48.dp).testTag("move_back_btn")
                    ) { Icon(Icons.Filled.KeyboardArrowDown, contentDescription = "Move Backward") }
                    FilledTonalIconButton(
                        onClick = { onCameraChange(camera.move(0f, strafeDelta = 1.6f, 0f)) },
                        modifier = Modifier.size(48.dp).testTag("move_right_btn")
                    ) { Icon(Icons.Filled.KeyboardArrowRight, contentDescription = "Move Right") }
                }
            }

            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                if (selectedPoi != null) {
                    FilledTonalButton(
                        onClick = { onPointAtPoi(selectedPoi) },
                        modifier = Modifier.defaultMinSize(minHeight = 48.dp).testTag("point_at_poi_btn")
                    ) {
                        Icon(Icons.Filled.NearMe, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Point Here", style = MaterialTheme.typography.labelMedium)
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilledTonalIconButton(
                        onClick = { onCameraChange(camera.move(0f, 0f, verticalDelta = 1.0f)) },
                        modifier = Modifier.size(48.dp).testTag("move_up_btn")
                    ) { Icon(Icons.Filled.ArrowUpward, contentDescription = "Ascend") }
                    FilledTonalIconButton(
                        onClick = { onCameraChange(camera.move(0f, 0f, verticalDelta = -1.0f)) },
                        modifier = Modifier.size(48.dp).testTag("move_down_btn")
                    ) { Icon(Icons.Filled.ArrowDownward, contentDescription = "Descend") }
                }
            }
        }
    }
}

private fun DrawScope.drawAtmosphere(
    biome: AdventureBiome,
    camera: Camera3D,
    timeSec: Float,
    w: Float,
    h: Float
) {
    val horizonOffset = (-camera.pitchRad * h * 0.45f).coerceIn(-h * 0.35f, h * 0.35f)
    when (biome) {
        AdventureBiome.RED_SEA_REEF, AdventureBiome.MEDITERRANEAN_COAST -> {
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF2ED5CE),
                        NileTurquoiseDark,
                        MidnightNavy,
                        Color(0xFF06101F)
                    )
                )
            )
            // Refracted Underwater Caustic Sunbeams
            val rayShift = sin(timeSec * 0.6f) * (w * 0.04f)
            for (i in 0..3) {
                val baseX = w * (0.18f + i * 0.22f) + rayShift - (camera.yawRad * 45f) % (w * 0.5f)
                val path = Path().apply {
                    moveTo(baseX, 0f)
                    lineTo(baseX + w * 0.08f, 0f)
                    lineTo(baseX + w * 0.22f, h * 0.85f)
                    lineTo(baseX - w * 0.04f, h * 0.85f)
                    close()
                }
                drawPath(
                    path = path,
                    brush = Brush.verticalGradient(
                        listOf(Color.White.copy(alpha = 0.14f), Color.Transparent)
                    )
                )
            }
        }
        AdventureBiome.DESERT_OASIS -> {
            val split = (h * 0.48f + horizonOffset).coerceIn(h * 0.2f, h * 0.78f)
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(MidnightNavy, SunsetCoral.copy(alpha = 0.72f), WarmSand),
                    startY = 0f,
                    endY = split
                ),
                size = Size(w, split)
            )
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(WarmSandDark, Color(0xFF6B5335), NileTurquoiseDark.copy(alpha = 0.6f)),
                    startY = split,
                    endY = h
                ),
                topLeft = Offset(0f, split),
                size = Size(w, h - split)
            )
        }
        AdventureBiome.NILE_HERITAGE -> {
            val split = (h * 0.46f + horizonOffset).coerceIn(h * 0.2f, h * 0.78f)
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(MidnightNavy, Color(0xFF3E4C78), EgyptianGold),
                    startY = 0f,
                    endY = split
                ),
                size = Size(w, split)
            )
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(NileTurquoiseDark, MidnightNavy, DeepNavyDark),
                    startY = split,
                    endY = h
                ),
                topLeft = Offset(0f, split),
                size = Size(w, h - split)
            )
        }
    }
}
