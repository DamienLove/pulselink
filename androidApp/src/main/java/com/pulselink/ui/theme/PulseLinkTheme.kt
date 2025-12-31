package com.pulselink.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Future Deep Palette
private val BackgroundDark = Color(0xFF030407)
private val SurfaceDark = Color(0xFF0F111A)
private val SurfaceAltDark = Color(0xFF161B2C)
private val PrimaryIndigo = Color(0xFF6366F1)
private val SecondaryCyan = Color(0xFF22D3EE)
private val OnBackgroundDark = Color(0xFFEEF2FB)
private val MutedDark = Color(0xFF64748B)

// We primarily support Dark Theme for the "Future" look, but will provide a functional Light theme.
private val DarkColors = darkColorScheme(
    primary = PrimaryIndigo,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF4338CA), // Indigo 700
    onPrimaryContainer = Color(0xFFE0E7FF),
    secondary = SecondaryCyan,
    onSecondary = Color(0xFF0F172A), // Slate 900
    secondaryContainer = Color(0xFF0E7490), // Cyan 700
    onSecondaryContainer = Color(0xFFCFFAFE),
    tertiary = Color(0xFFA855F7), // Purple
    background = BackgroundDark,
    onBackground = OnBackgroundDark,
    surface = SurfaceDark,
    onSurface = OnBackgroundDark,
    surfaceVariant = SurfaceAltDark,
    onSurfaceVariant = Color(0xFF94A3B8), // Slate 400
    outline = Color(0xFF334155),
    error = Color(0xFFEF4444),
    onError = Color.White
)

private val LightColors = lightColorScheme(
    primary = Color(0xFF4F46E5), // Indigo 600
    onPrimary = Color.White,
    secondary = Color(0xFF06B6D4), // Cyan 500
    onSecondary = Color.White,
    background = Color(0xFFF8FAFC), // Slate 50
    onBackground = Color(0xFF0F172A), // Slate 900
    surface = Color.White,
    onSurface = Color(0xFF0F172A),
    surfaceVariant = Color(0xFFF1F5F9), // Slate 100
    onSurfaceVariant = Color(0xFF64748B),
    error = Color(0xFFDC2626)
)

@Composable
fun PulseLinkTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    // Force Dark Theme if the user wants the "Future" look, but respect system setting for now.
    // Ideally, we'd have an in-app setting for this.
    val colors = if (darkTheme) DarkColors else LightColors
    MaterialTheme(
        colorScheme = colors,
        typography = MaterialTheme.typography, // Default Typography for now, could be customized
        content = content
    )
}
