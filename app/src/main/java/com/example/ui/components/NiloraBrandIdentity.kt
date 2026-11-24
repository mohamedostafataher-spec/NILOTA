package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

enum class BrandLogoVariant {
    ON_LIGHT,
    ON_DARK,
    MONOCHROME_GOLD
}

/**
 * Original NILORA geometric emblem ("GLOBAL SOUL — EGYPTIAN ORIGIN").
 * Combines an architectural Nile keystone arch, dual acoustic/language horizon waves,
 * and converging bridge lines — avoiding generic globe clichés.
 */
@Composable
fun NiloraBrandSymbol(
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    variant: BrandLogoVariant = BrandLogoVariant.ON_LIGHT
) {
    val frameColor = when (variant) {
        BrandLogoVariant.ON_LIGHT -> MidnightNavy
        BrandLogoVariant.ON_DARK -> SoftIvory
        BrandLogoVariant.MONOCHROME_GOLD -> EgyptianGold
    }
    val waveColor = when (variant) {
        BrandLogoVariant.MONOCHROME_GOLD -> EgyptianGold
        else -> NileTurquoise
    }
    val accentGold = when (variant) {
        BrandLogoVariant.ON_LIGHT -> EgyptianGoldDark
        else -> EgyptianGold
    }

    Canvas(
        modifier = modifier
            .size(size)
            .semantics { contentDescription = "NILORA Geometric Emblem" }
    ) {
        val w = this.size.width
        val h = this.size.height
        val cx = w / 2f
        val cy = h / 2f
        val stroke = (w * 0.068f).coerceAtLeast(1.8f)

        // Outer Egyptian architectural diamond / keystone
        val diamond = Path().apply {
            moveTo(cx, h * 0.08f)
            lineTo(w * 0.90f, cy)
            lineTo(cx, h * 0.92f)
            lineTo(w * 0.10f, cy)
            close()
        }
        drawPath(
            path = diamond,
            color = frameColor,
            style = Stroke(width = stroke, join = StrokeJoin.Round)
        )

        // Inner Nile horizon & acoustic waves (symbolizing voice, water & movement)
        val upperArc = Path().apply {
            moveTo(w * 0.24f, cy - h * 0.07f)
            cubicTo(
                w * 0.40f, cy - h * 0.23f,
                w * 0.60f, cy + h * 0.09f,
                w * 0.76f, cy - h * 0.07f
            )
        }
        val lowerArc = Path().apply {
            moveTo(w * 0.24f, cy + h * 0.08f)
            cubicTo(
                w * 0.40f, cy - h * 0.08f,
                w * 0.60f, cy + h * 0.24f,
                w * 0.76f, cy + h * 0.08f
            )
        }
        drawPath(
            path = upperArc,
            color = waveColor,
            style = Stroke(width = stroke * 0.95f, cap = StrokeCap.Round)
        )
        drawPath(
            path = lowerArc,
            color = accentGold,
            style = Stroke(width = stroke * 0.95f, cap = StrokeCap.Round)
        )

        // Twin apex & nadir compass points
        drawCircle(
            color = accentGold,
            radius = stroke * 0.85f,
            center = Offset(cx, h * 0.24f)
        )
        drawCircle(
            color = waveColor,
            radius = stroke * 0.85f,
            center = Offset(cx, h * 0.76f)
        )
    }
}

@Composable
fun NiloraBrandWordmark(
    modifier: Modifier = Modifier,
    variant: BrandLogoVariant = BrandLogoVariant.ON_LIGHT,
    showTagline: Boolean = true,
    symbolSize: Dp = 40.dp
) {
    val wordColor = when (variant) {
        BrandLogoVariant.ON_LIGHT -> MidnightNavy
        BrandLogoVariant.ON_DARK -> SoftIvory
        BrandLogoVariant.MONOCHROME_GOLD -> EgyptianGold
    }
    val subtitleColor = when (variant) {
        BrandLogoVariant.ON_LIGHT -> NileTurquoiseDark
        BrandLogoVariant.ON_DARK -> WarmSand
        BrandLogoVariant.MONOCHROME_GOLD -> EgyptianGold.copy(alpha = 0.85f)
    }

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        NiloraBrandSymbol(size = symbolSize, variant = variant)
        Column {
            Text(
                text = "NILORA",
                style = MaterialTheme.typography.headlineMedium.copy(
                    letterSpacing = 2.6.sp,
                    fontWeight = FontWeight.Bold
                ),
                color = wordColor
            )
            if (showTagline) {
                Text(
                    text = "GLOBAL SOUL • EGYPTIAN ORIGIN",
                    style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.8.sp),
                    color = subtitleColor,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

/**
 * Subtle geometric Egyptian horizon divider with turquoise & gold diamond node.
 */
@Composable
fun NiloraGeometricDivider(
    modifier: Modifier = Modifier,
    lineColor: Color = EgyptianGold.copy(alpha = 0.55f),
    centerAccent: Color = NileTurquoise
) {
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(14.dp)
    ) {
        val cy = size.height / 2f
        val cx = size.width / 2f
        val gap = 14f
        drawLine(
            color = lineColor,
            start = Offset(0f, cy),
            end = Offset((cx - gap).coerceAtLeast(0f), cy),
            strokeWidth = 2f
        )
        drawLine(
            color = lineColor,
            start = Offset((cx + gap).coerceAtMost(size.width), cy),
            end = Offset(size.width, cy),
            strokeWidth = 2f
        )
        val node = Path().apply {
            moveTo(cx, cy - 5f)
            lineTo(cx + 7f, cy)
            lineTo(cx, cy + 5f)
            lineTo(cx - 7f, cy)
            close()
        }
        drawPath(node, color = centerAccent)
    }
}
