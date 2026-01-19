package com.RingerSong.free.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.RingerSong.free.data.ThemeConfig
import com.pulselink.shared.ui.theme.hexToColor

// Legacy Light - Not recommended but kept
private val LightColorScheme = lightColorScheme(
    primary = Ocean,
    secondary = Sky,
    tertiary = NeonPurple,
    background = Color(0xFFF0F6FC),
    surface = Color(0xFFFFFFFF),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color(0xFF03050A),
    onSurface = Color(0xFF03050A)
)

private val DarkColorScheme = darkColorScheme(
    primary = Sky,
    secondary = Ocean,
    tertiary = NeonPurple,
    background = DeepBackground,
    surface = DeepSurface,
    surfaceVariant = DeepSurfaceAlt,
    onPrimary = DeepBackground,
    onSecondary = DeepBackground,
    onTertiary = Ink,
    onBackground = Ink,
    onSurface = Ink,
    onSurfaceVariant = Mist,
    error = Error,
    primaryContainer = Ocean.copy(alpha = 0.2f),
    onPrimaryContainer = Sky,
    tertiaryContainer = NeonPurple.copy(alpha = 0.2f),
    onTertiaryContainer = Ink
)

@Composable
fun RingerSongTheme(
    darkTheme: Boolean = true, // Default to True for Future Deep consistency
    themeConfig: ThemeConfig? = null,
    content: @Composable () -> Unit
) {
    // Prioritize Dark Theme for the suite aesthetic unless explicitly overridden
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
        primaryContainer = palette.accentColor.copy(alpha = 0.2f),
        onPrimaryContainer = palette.accentColor,
        tertiaryContainer = palette.incomingColor.copy(alpha = 0.2f),
        onTertiaryContainer = onBackground
    )
}
