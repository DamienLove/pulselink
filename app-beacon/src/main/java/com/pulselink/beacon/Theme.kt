package com.pulselink.beacon

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val BeaconBlue = Color(0xFFDCEFFF)
private val BeaconBlueStrong = Color(0xFF6EB2FF)
private val BeaconText = Color(0xFF1E1E1E)
private val BeaconOnMuted = Color(0xFF5C5C5C)
private val BeaconSurface = Color(0xFFF7F7F7)
private val BeaconSurfaceStrong = Color(0xFFF0F0F0)
private val BeaconAccent = Color(0xFF1E88E5)
private val BeaconSuccess = Color(0xFF60C659)
private val BeaconDanger = Color(0xFFF04444)

private val LightColors = lightColorScheme(
    primary = BeaconAccent,
    onPrimary = Color.White,
    secondary = BeaconBlueStrong,
    onSecondary = Color.White,
    background = Color.White,
    onBackground = BeaconText,
    surface = BeaconSurface,
    onSurface = BeaconText,
    surfaceVariant = BeaconSurfaceStrong,
    onSurfaceVariant = BeaconOnMuted,
    tertiary = BeaconBlue,
    error = BeaconDanger
)

private val DarkColors = darkColorScheme(
    primary = BeaconAccent,
    secondary = BeaconBlueStrong,
    tertiary = BeaconBlue
)

@Composable
fun PulseLinkBeaconTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = MaterialTheme.typography,
        content = content
    )
}
