package com.pulselink.beacon

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import com.pulselink.beacon.data.ThemePalette
import com.pulselink.beacon.data.ThemeFont

@Composable
fun BeaconTheme(
    theme: ThemePalette = ThemePalette.default(),
    content: @Composable () -> Unit
) {
    val lightColors = lightColorScheme(
        primary = theme.accentColor,
        onPrimary = Color.Black,
        primaryContainer = theme.outgoingColor,
        secondary = theme.incomingColor,
        surface = theme.inboxBackgroundColor,
        background = theme.inboxBackgroundColor,
        onSurface = Color.Black,
        onSecondary = Color.Black
    )
    
    val baseRadius = theme.bubbleRadius.dp
    val shapes = when(theme.uiStyle) {
        "Retro Terminal" -> Shapes(
            extraSmall = RoundedCornerShape(2.dp),
            small = RoundedCornerShape(4.dp),
            medium = RoundedCornerShape(4.dp),
            large = RoundedCornerShape(8.dp),
            extraLarge = RoundedCornerShape(12.dp)
        )
        "Soft Layers" -> Shapes(
            extraSmall = RoundedCornerShape(12.dp),
            small = RoundedCornerShape(16.dp),
            medium = RoundedCornerShape(28.dp),
            large = RoundedCornerShape(32.dp),
            extraLarge = RoundedCornerShape(40.dp)
        )
        "Playful Pop" -> Shapes(
            extraSmall = RoundedCornerShape(14.dp),
            small = RoundedCornerShape(18.dp),
            medium = RoundedCornerShape(30.dp),
            large = RoundedCornerShape(36.dp),
            extraLarge = RoundedCornerShape(48.dp)
        )
        "Neon Glass" -> Shapes(
            extraSmall = RoundedCornerShape(6.dp),
            small = RoundedCornerShape(10.dp),
            medium = RoundedCornerShape(22.dp),
            large = RoundedCornerShape(32.dp),
            extraLarge = RoundedCornerShape(40.dp)
        )
        else -> Shapes( // Clean Minimal
            extraSmall = RoundedCornerShape(4.dp),
            small = RoundedCornerShape(8.dp),
            medium = RoundedCornerShape(14.dp),
            large = RoundedCornerShape(18.dp),
            extraLarge = RoundedCornerShape(24.dp)
        )
    }

    MaterialTheme(
        colorScheme = lightColors,
        shapes = shapes,
        typography = MaterialTheme.typography.copy(
            bodyMedium = MaterialTheme.typography.bodyMedium.merge(theme.font.toTextStyle()),
            bodyLarge = MaterialTheme.typography.bodyLarge.merge(theme.font.toTextStyle()),
            titleMedium = MaterialTheme.typography.titleMedium.merge(theme.font.toTextStyle()),
            labelLarge = MaterialTheme.typography.labelLarge.merge(theme.font.toTextStyle())
        ),
        content = content
    )
}

private fun ThemeFont.toTextStyle(): TextStyle =
    TextStyle(fontFamily = family, fontWeight = weight)
