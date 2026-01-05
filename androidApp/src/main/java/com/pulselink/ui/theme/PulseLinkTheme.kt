package com.pulselink.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Future Deep v2 Palette - Dark Mode Optimized
private val DeepBackground = Color(0xFF05070F)
private val DeepSurface = Color(0xFF0C1326)
private val DeepSurfaceAlt = Color(0xFF0F1A30)
private val DeepAccent = Color(0xFF22D3EE) // Cyan/Electric Blue
private val DeepAccentStrong = Color(0xFF0EA5E9)
private val DeepTertiary = Color(0xFFBC13FE) // Neon Purple
private val DeepError = Color(0xFFEF4444)
private val DeepOnBackground = Color(0xFFEEF2FB)
private val DeepOnSurface = Color(0xFFEEF2FB)
private val DeepOnSurfaceVariant = Color(0xFF9FB3C8) // Muted

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
    onBackground = DeepOnBackground,
    surface = DeepSurface,
    onSurface = DeepOnSurface,
    surfaceVariant = DeepSurfaceAlt,
    onSurfaceVariant = DeepOnSurfaceVariant,
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
        typography = MaterialTheme.typography, // Should be updated in Type.kt ideally, but out of scope for now
        content = content
    )
}
