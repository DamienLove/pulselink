package com.pulselink.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColors = darkColorScheme(
    primary = DeepAccent,
    onPrimary = Color(0xFF04101C), // Deep dark for contrast on bright accent
    primaryContainer = Color(0xFF0D3642), // Darker cyan container
    onPrimaryContainer = Color(0xFFCFFAFE),
    secondary = DeepAccentStrong,
    onSecondary = Color(0xFF04101C),
    tertiary = DeepTertiary,
    onTertiary = Color(0xFFFFFFFF),
    background = DeepBackground,
    onBackground = DeepOnSurface, // Using v5 Ink
    surface = DeepSurface,
    onSurface = DeepOnSurface,
    surfaceVariant = DeepSurfaceAlt,
    onSurfaceVariant = DeepMuted,
    error = DeepError,
    onError = Color(0xFF2B0B0B)
)

// Legacy Light Mode - Kept for compatibility but design system prefers Dark
private val LightColors = lightColorScheme(
    primary = Color(0xFF1A237E),
    secondary = Color(0xFFFFEA00),
    surface = Color(0xFFF4F4FF),
    onSurface = Color(0xFF060713),
    background = Color(0xFFF6F7FF),
    onBackground = Color(0xFF111321),
    surfaceVariant = Color(0xFFE7E9F6),
    onSurfaceVariant = Color(0xFF444A5F),
    error = Color(0xFFD32F2F),
    onError = Color(0xFFFFFFFF)
)

@Composable
fun PulseLinkTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    // Future Deep design requires dark mode for full fidelity.
    // System setting is ignored to ensure brand consistency.
    val colors = DarkColors

    MaterialTheme(
        colorScheme = colors,
        typography = MaterialTheme.typography,
        content = content
    )
}
