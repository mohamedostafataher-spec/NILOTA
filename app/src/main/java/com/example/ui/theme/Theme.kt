package com.example.ui.theme

import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

val LocalReducedMotion = compositionLocalOf { false }

private val NiloraLightColorScheme = lightColorScheme(
    primary = MidnightNavy,
    onPrimary = Color.White,
    primaryContainer = MidnightNavyLight,
    onPrimaryContainer = Color.White,
    secondary = NileTurquoiseDark,
    onSecondary = Color.White,
    secondaryContainer = NileTurquoiseContainer,
    onSecondaryContainer = NileTurquoiseOnContainer,
    tertiary = EgyptianGoldDark,
    onTertiary = Color.White,
    tertiaryContainer = EgyptianGoldContainer,
    onTertiaryContainer = EgyptianGoldOnContainer,
    background = SoftIvory,
    onBackground = TextPrimaryLight,
    surface = SurfaceWhite,
    onSurface = TextPrimaryLight,
    surfaceVariant = SurfaceIvoryMuted,
    onSurfaceVariant = TextSecondaryLight,
    outline = OutlineIvory,
    error = CoralAccessible,
    onError = Color.White,
    errorContainer = CoralContainer,
    onErrorContainer = CoralAccessible
)

private val NiloraDarkColorScheme = darkColorScheme(
    primary = NileTurquoise,
    onPrimary = MidnightNavyDark,
    primaryContainer = MidnightNavyLight,
    onPrimaryContainer = SoftIvory,
    secondary = EgyptianGold,
    onSecondary = MidnightNavyDark,
    secondaryContainer = DarkSurfaceVariant,
    onSecondaryContainer = NileTurquoise,
    tertiary = WarmSand,
    onTertiary = MidnightNavyDark,
    tertiaryContainer = DarkSurfaceVariant,
    onTertiaryContainer = WarmSand,
    background = DarkBackground,
    onBackground = TextPrimaryDark,
    surface = DarkSurface,
    onSurface = TextPrimaryDark,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = TextSecondaryDark,
    outline = Color(0xFF2B3E5F),
    error = CoralAccent,
    onError = MidnightNavyDark,
    errorContainer = Color(0xFF4E1E19),
    onErrorContainer = CoralAccent
)

val NiloraShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun rememberSystemReducedMotion(): Boolean {
    val context = LocalContext.current
    return remember(context) {
        try {
            Settings.Global.getFloat(
                context.contentResolver,
                Settings.Global.ANIMATOR_DURATION_SCALE,
                1.0f
            ) == 0f
        } catch (_: Throwable) {
            false
        }
    }
}

@Composable
fun NiloraTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    languageCode: String = "en",
    reducedMotionOverride: Boolean? = null,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> NiloraDarkColorScheme
        else -> NiloraLightColorScheme
    }
    val isArabic = languageCode.startsWith("ar", ignoreCase = true)
    val layoutDirection = if (isArabic) LayoutDirection.Rtl else LayoutDirection.Ltr
    val reduceMotion = reducedMotionOverride ?: rememberSystemReducedMotion()

    CompositionLocalProvider(
        LocalLayoutDirection provides layoutDirection,
        LocalReducedMotion provides reduceMotion
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = buildNiloraTypography(isArabic),
            shapes = NiloraShapes,
            content = content
        )
    }
}
