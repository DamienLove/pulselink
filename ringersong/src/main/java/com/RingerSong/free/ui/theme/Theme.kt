package com.RingerSong.free.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.RingerSong.free.data.ThemeConfig
import com.pulselink.shared.ui.theme.hexToColor

private val LightColorScheme = lightColorScheme(
    primary = Ocean,
    secondary = Azure,
    tertiary = Sky,
    background = Mist,
    surface = Ice,
    surfaceVariant = Sky.copy(alpha = 0.35f),
    onPrimary = Mist,
    onSecondary = Mist,
    onTertiary = Ink,
    onBackground = Ink,
    onSurface = Ink,
    onSurfaceVariant = Ink.copy(alpha = 0.65f),
    primaryContainer = Sky.copy(alpha = 0.6f),
    onPrimaryContainer = Ink,
    tertiaryContainer = Sky.copy(alpha = 0.3f),
    onTertiaryContainer = Ink
)

private val DarkColorScheme = darkColorScheme(
    primary = Sky,
    secondary = Azure,
    tertiary = Ice,
    background = Ink,
    surface = DeepBlue,
    surfaceVariant = Ocean,
    onPrimary = Ink,
    onSecondary = Mist,
    onTertiary = Ink,
    onBackground = Mist,
    onSurface = Mist,
    onSurfaceVariant = Mist.copy(alpha = 0.7f),
    primaryContainer = Ocean,
    onPrimaryContainer = Mist,
    tertiaryContainer = Azure.copy(alpha = 0.2f),
    onTertiaryContainer = Mist
)

@Composable
fun RingerSongTheme(
    darkTheme: Boolean = false,
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
