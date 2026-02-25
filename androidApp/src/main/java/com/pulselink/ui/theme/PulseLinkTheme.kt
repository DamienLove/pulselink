package com.pulselink.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.pulselink.domain.model.ThemePreferences
import com.pulselink.util.parseColorOr

@Composable
fun PulseLinkTheme(
    theme: ThemePreferences = ThemePreferences(),
    content: @Composable () -> Unit
) {
    val primary = parseColorOr(DeepAccent, theme.primaryColor)
    val secondary = parseColorOr(DeepAccentStrong, theme.secondaryColor)
    val background = parseColorOr(DeepBackground, theme.backgroundColor)
    val surface = parseColorOr(DeepSurface, theme.topBarColor)
    val onBackground = parseColorOr(DeepOnSurface, theme.onBackground)
    val onSurface = parseColorOr(DeepOnSurface, theme.onTopBarColor)
    val surfaceVariant = parseColorOr(DeepSurfaceAlt, theme.bubbleIncoming)
    val onSurfaceVariant = parseColorOr(DeepMuted, theme.timestampColor ?: theme.onBackground)

    val colors = darkColorScheme(
        primary = primary,
        onPrimary = Color(0xFF04101C),
        primaryContainer = primary.copy(alpha = 0.2f),
        onPrimaryContainer = Color(0xFFCFFAFE),
        secondary = secondary,
        onSecondary = Color(0xFF04101C),
        tertiary = DeepTertiary,
        onTertiary = Color(0xFFFFFFFF),
        background = background,
        onBackground = onBackground,
        surface = surface,
        onSurface = onSurface,
        surfaceVariant = surfaceVariant,
        onSurfaceVariant = onSurfaceVariant,
        error = DeepError,
        onError = Color(0xFF2B0B0B)
    )

    val shapes = when(theme.uiStyle) {
        "Retro Terminal" -> Shapes(
            extraSmall = RoundedCornerShape(2.dp),
            small = RoundedCornerShape(4.dp),
            medium = RoundedCornerShape(4.dp),
            large = RoundedCornerShape(8.dp),
            extraLarge = RoundedCornerShape(12.dp)
        )
        "Soft Layers" -> Shapes(
            extraSmall = RoundedCornerShape(12.dp),
            small = RoundedCornerShape(16.dp),
            medium = RoundedCornerShape(28.dp),
            large = RoundedCornerShape(32.dp),
            extraLarge = RoundedCornerShape(40.dp)
        )
        "Playful Pop" -> Shapes(
            extraSmall = RoundedCornerShape(14.dp),
            small = RoundedCornerShape(18.dp),
            medium = RoundedCornerShape(30.dp),
            large = RoundedCornerShape(36.dp),
            extraLarge = RoundedCornerShape(48.dp)
        )
        "Neon Glass" -> Shapes(
            extraSmall = RoundedCornerShape(6.dp),
            small = RoundedCornerShape(10.dp),
            medium = RoundedCornerShape(26.dp),
            large = RoundedCornerShape(32.dp),
            extraLarge = RoundedCornerShape(40.dp)
        )
        else -> Shapes( // Clean Minimal
            extraSmall = RoundedCornerShape(4.dp),
            small = RoundedCornerShape(8.dp),
            medium = RoundedCornerShape(14.dp),
            large = RoundedCornerShape(18.dp),
            extraLarge = RoundedCornerShape(24.dp)
        )
    }

    MaterialTheme(
        colorScheme = colors,
        shapes = shapes,
        typography = MaterialTheme.typography,
        content = content
    )
}
