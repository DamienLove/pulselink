package com.pulselink.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

// Future Deep Tokens
private val BackgroundDark = Color(0xFF030407)
private val BrandPrimary = Color(0xFF6366F1) // Indigo
private val BrandSecondary = Color(0xFF22D3EE) // Cyan
private val EmergencyRed = Color(0xFFEF4444)
private val EmergencyDarkRed = Color(0xFF991B1B)

object Spacing {
    val extraSmall = 4.dp
    val small = 8.dp
    val medium = 16.dp
    val large = 24.dp
    val extraLarge = 32.dp
    val section = 48.dp
}

object Layout {
    val cardCornerRadius = 16.dp
    val buttonCornerRadius = 12.dp
    val inputCornerRadius = 12.dp
    val bottomSheetCornerRadius = 24.dp
}

object Gradients {
    val PrimaryBackground = Brush.verticalGradient(
        colors = listOf(BackgroundDark, Color(0xFF0B0E16))
    )
    
    val BrandGradient = Brush.horizontalGradient(
        colors = listOf(BrandPrimary, BrandSecondary)
    )

    val EmergencyGradient = Brush.linearGradient(
        colors = listOf(EmergencyRed, EmergencyDarkRed)
    )

    val GlassGradient = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF0F111A).copy(alpha = 0.9f),
            Color(0xFF0F111A).copy(alpha = 0.7f)
        )
    )
}

object Elevations {
    val card = 0.dp // Flat for glassmorphism usually, or subtle shadow
    val floating = 8.dp
}
