package com.pulselink.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.pulselink.ui.theme.FutureDeepColors.BackgroundDark
import com.pulselink.ui.theme.FutureDeepColors.MutedDark
import com.pulselink.ui.theme.FutureDeepColors.OnBackgroundDark
import com.pulselink.ui.theme.FutureDeepColors.PrimaryIndigo
import com.pulselink.ui.theme.FutureDeepColors.SecondaryCyan
import com.pulselink.ui.theme.FutureDeepColors.SurfaceAltDark
import com.pulselink.ui.theme.FutureDeepColors.SurfaceDark

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
    onSurfaceVariant = MutedDark,
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
    val colors = if (darkTheme) DarkColors else LightColors
    MaterialTheme(
        colorScheme = colors,
        typography = MaterialTheme.typography,
        content = content
    )
}
