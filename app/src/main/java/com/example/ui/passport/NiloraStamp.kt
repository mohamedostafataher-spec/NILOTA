package com.example.ui.passport

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.ui.theme.EgyptianGold
import com.example.ui.theme.MidnightNavy
import com.example.ui.theme.OutlineIvory
import com.example.ui.theme.TextSecondaryLight

/**
 * A circular travel stamp, styled like an ink passport stamp: a solid outer
 * ring and a dashed inner ring. Earned stamps show the city name in gold and
 * navy; locked stamps fade back and show a lock.
 */
@Composable
fun NiloraStamp(
    stamp: CityStamp,
    isArabic: Boolean,
    modifier: Modifier = Modifier,
) {
    val ringColor = if (stamp.isEarned) EgyptianGold else OutlineIvory
    val contentColor = if (stamp.isEarned) MidnightNavy else TextSecondaryLight
    val contentAlpha = if (stamp.isEarned) 1f else 0.45f

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier,
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(88.dp),
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val outerStroke = Stroke(width = 2.dp.toPx())
                drawCircle(
                    color = ringColor,
                    radius = (size.minDimension / 2) - 2.dp.toPx(),
                    style = outerStroke,
                )
                val dashedStroke = Stroke(
                    width = 1.5.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f),
                )
                drawCircle(
                    color = ringColor,
                    radius = (size.minDimension / 2) - 8.dp.toPx(),
                    style = dashedStroke,
                )
            }
            if (stamp.isEarned) {
                Text(
                    text = if (isArabic) stamp.nameAr else stamp.nameEn.uppercase(),
                    color = contentColor,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    modifier = Modifier
                        .padding(12.dp)
                        .alpha(contentAlpha),
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier
                        .size(20.dp)
                        .alpha(contentAlpha),
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = if (isArabic) stamp.nameAr else stamp.nameEn,
            color = contentColor,
            style = MaterialTheme.typography.labelSmall,
            textAlign = TextAlign.Center,
            maxLines = 1,
            modifier = Modifier.alpha(contentAlpha),
        )
    }
}
