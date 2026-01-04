package com.pulselink.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Future Deep v2 Palette - Ultra Dark & Neon
private val DeepBackground = Color(0xFF020408) // Almost black
private val DeepSurface = Color(0xFF0A0F1E)    // Deep Navy
private val DeepSurfaceAlt = Color(0xFF111827) // Slate 900
private val DeepAccent = Color(0xFF22D3EE)     // Cyan 400 (Neon)
private val DeepAccentStrong = Color(0xFF0EA5E9) // Sky 500
private val DeepSecondary = Color(0xFF818CF8)  // Indigo 400
private val DeepError = Color(0xFFF43F5E)      // Rose 500
private val DeepOnBackground = Color(0xFFF1F5F9) // Slate 100
private val DeepOnSurface = Color(0xFFF1F5F9)
private val DeepOnSurfaceVariant = Color(0xFF94A3B8) // Slate 400

private val DarkColors = darkColorScheme(
    primary = DeepAccent,
    onPrimary = Color(0xFF020408), // Dark contrast on neon
    primaryContainer = Color(0xFF083344), // Cyan 950
    onPrimaryContainer = Color(0xFFCFFAFE), // Cyan 100
    secondary = DeepSecondary,
    onSecondary = Color(0xFF1E1B4B), // Indigo 950
    secondaryContainer = Color(0xFF312E81), // Indigo 900
    onSecondaryContainer = Color(0xFFE0E7FF), // Indigo 100
    background = DeepBackground,
    onBackground = DeepOnBackground,
    surface = DeepSurface,
    onSurface = DeepOnSurface,
    surfaceVariant = DeepSurfaceAlt,
    onSurfaceVariant = DeepOnSurfaceVariant,
    error = DeepError,
    onError = Color(0xFFFFF1F2)
)

// Legacy Light Mode - Minimal support, as design is dark-first
private val LightColors = lightColorScheme(
    primary = Color(0xFF0EA5E9),
    secondary = Color(0xFF6366F1),
    surface = Color(0xFFF8FAFC),
    onSurface = Color(0xFF0F172A),
    background = Color(0xFFFFFFFF),
    onBackground = Color(0xFF0F172A),
    surfaceVariant = Color(0xFFE2E8F0),
    onSurfaceVariant = Color(0xFF64748B),
    error = Color(0xFFEF4444),
    onError = Color(0xFFFFFFFF)
)

@Composable
fun PulseLinkTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    // Enforce "Future Deep" dark mode for brand consistency,
    // unless explicitly overridden by user settings (future work).
    // For now, the "Future" look depends on the dark palette.
    val colors = DarkColors

    MaterialTheme(
        colorScheme = colors,
        typography = MaterialTheme.typography,
        content = content
    )
}
