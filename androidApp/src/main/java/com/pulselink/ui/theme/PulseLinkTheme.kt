package com.pulselink.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColors = darkColorScheme(
    primary = Color(0xFF5E6FFF),
    secondary = Color(0xFFFFEA00),
    surface = Color(0xFF0F101A),
    onSurface = Color(0xFFE8EAFF),
    background = Color(0xFF0B0E16),
    onBackground = Color(0xFFE8EAFF),
    surfaceVariant = Color(0xFF1C1F2B),
    onSurfaceVariant = Color(0xFFBDC1D6),
    error = Color(0xFFEF6666),
    onError = Color(0xFF320000)
)

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
    val colors = if (darkTheme) DarkColors else LightColors
    MaterialTheme(
        colorScheme = colors,
        typography = MaterialTheme.typography,
        content = content
    )
}
