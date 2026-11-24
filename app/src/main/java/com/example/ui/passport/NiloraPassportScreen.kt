package com.example.ui.passport

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import com.example.ui.theme.EgyptianGoldDark
import com.example.ui.theme.NiloraSpacing
import com.example.ui.theme.NiloraTheme

/**
 * The traveler's passport: rank, progress, and the grid of city stamps.
 * Pure UI: stamp state arrives as a parameter so a ViewModel or Firestore
 * snapshot can feed it later without touching this screen.
 */
@Composable
fun NiloraPassportScreen(
    stamps: List<CityStamp>,
    isArabic: Boolean,
    modifier: Modifier = Modifier,
) {
    val earnedCount = stamps.count { it.isEarned }
    val level = PassportLevel.forStampCount(earnedCount)
    val nextLevel = PassportLevel.next(level)
    val progress = earnedCount.toFloat() / EgyptPassport.TOTAL_STAMPS.toFloat()

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(NiloraSpacing.lg),
        ) {
            Text(
                text = if (isArabic) "جواز نيلورا" else "NILORA Passport",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(modifier = Modifier.height(NiloraSpacing.sm))
            Text(
                text = if (isArabic) level.titleAr else level.titleEn,
                style = MaterialTheme.typography.titleMedium,
                color = EgyptianGoldDark,
            )
            Spacer(modifier = Modifier.height(NiloraSpacing.md))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(NiloraSpacing.sm))
            Text(
                text = buildString {
                    append("$earnedCount / ${EgyptPassport.TOTAL_STAMPS}")
                    append(if (isArabic) " ختم" else " stamps")
                    if (nextLevel != null) {
                        val remaining = nextLevel.minStamps - earnedCount
                        val nextTitle = if (isArabic) nextLevel.titleAr else nextLevel.titleEn
                        append(if (isArabic) " · باقي $remaining على $nextTitle" else " · $remaining to $nextTitle")
                    }
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(NiloraSpacing.lg))
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                verticalArrangement = Arrangement.spacedBy(NiloraSpacing.md),
                horizontalArrangement = Arrangement.spacedBy(NiloraSpacing.md),
                modifier = Modifier.weight(1f),
            ) {
                items(stamps) { stamp ->
                    NiloraStamp(
                        stamp = stamp,
                        isArabic = isArabic,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true, name = "Passport - Arabic, some earned")
@Composable
fun NiloraPassportScreenArabicPreview() {
    NiloraTheme(languageCode = "ar") {
        NiloraPassportScreen(
            stamps = EgyptPassport.cities.mapIndexed { index, stamp ->
                if (index < 5) stamp.copy(earnedAtEpochMs = 1L) else stamp
            },
            isArabic = true,
        )
    }
}

@Preview(showBackground = true, name = "Passport - English, empty")
@Composable
fun NiloraPassportScreenEnglishPreview() {
    NiloraTheme(languageCode = "en") {
        NiloraPassportScreen(
            stamps = EgyptPassport.cities,
            isArabic = false,
        )
    }
}
