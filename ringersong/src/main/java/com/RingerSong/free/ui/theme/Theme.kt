package com.RingerSong.free.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.RingerSong.free.data.ThemeConfig
import com.pulselink.shared.ui.theme.hexToColor

// Default to Dark Theme colors for Future Deep
private val DarkColorScheme = darkColorScheme(
    primary = Indigo,
    secondary = Cyan,
    tertiary = Azure,
    background = DeepBg,
    surface = Surface,
    surfaceVariant = SurfaceAlt,
    onPrimary = Color.White,
    onSecondary = DeepBg,
    onTertiary = Color.White,
    onBackground = Ink,
    onSurface = Ink,
    onSurfaceVariant = Muted,
    primaryContainer = Indigo.copy(alpha = 0.2f),
    onPrimaryContainer = Ink,
    tertiaryContainer = Cyan.copy(alpha = 0.2f),
    onTertiaryContainer = Ink
)

// Minimal light theme fallback
private val LightColorScheme = lightColorScheme(
    primary = Indigo,
    secondary = Cyan,
    tertiary = Azure,
    background = Color(0xFFF8FAFC),
    surface = Color.White,
    surfaceVariant = Color(0xFFE2E8F0),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color(0xFF0F172A),
    onSurface = Color(0xFF0F172A),
    onSurfaceVariant = Color(0xFF64748B)
)

@Composable
fun RingerSongTheme(
    darkTheme: Boolean = true, // Default to true for Future Deep look
    themeConfig: ThemeConfig? = null,
    content: @Composable () -> Unit
) {
    val baseScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    val colorScheme = themeConfig?.let { buildColorScheme(baseScheme, it) } ?: baseScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        content = content
    )
}

private fun buildColorScheme(base: androidx.compose.material3.ColorScheme, theme: ThemeConfig): androidx.compose.material3.ColorScheme {
    val palette = theme.toPalette()
    val onBackground = hexToColor(theme.onBackgroundColor) ?: base.onBackground
    return base.copy(
        primary = palette.accentColor,
        secondary = palette.outgoingColor,
        tertiary = palette.incomingColor,
        background = palette.threadBackgroundColor,
        surface = palette.inboxBackgroundColor,
        surfaceVariant = palette.inboxBackgroundColor.copy(alpha = 0.85f),
        onBackground = onBackground,
        onSurface = onBackground,
        onSurfaceVariant = onBackground.copy(alpha = 0.8f),
        primaryContainer = palette.accentColor.copy(alpha = 0.82f),
        onPrimaryContainer = onBackground,
        tertiaryContainer = palette.incomingColor.copy(alpha = 0.35f),
        onTertiaryContainer = onBackground
    )
}
