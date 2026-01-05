package com.pulselink.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

// Future Deep v2 Design System Tokens
// Aligned with PulseLinkTheme.kt and Web v2 Redesign

// Colors
private val DeepBackground = Color(0xFF05070F)
private val DeepSurface = Color(0xFF0C1326)
private val DeepAccent = Color(0xFF22D3EE)
private val DeepAccentStrong = Color(0xFF0EA5E9)
private val DeepTertiary = Color(0xFFBC13FE) // Neon Purple
private val DeepError = Color(0xFFEF4444)

object Spacing {
    val extraSmall = 6.dp // Increased breathing room slightly
    val small = 10.dp
    val medium = 18.dp
    val large = 26.dp
    val extraLarge = 40.dp
    val section = 64.dp
}

object Layout {
    val cardCornerRadius = 20.dp
    val buttonCornerRadius = 14.dp
    val inputCornerRadius = 14.dp
    val bottomSheetCornerRadius = 32.dp
    val dialogCornerRadius = 24.dp
}

object Gradients {
    // Subtle radial-like effect for backgrounds using vertical gradient approximation
    val PrimaryBackground = Brush.verticalGradient(
        colors = listOf(Color(0xFF0D1224), DeepBackground)
    )
    
    // Vibrant accent gradient for buttons/FABs
    val BrandGradient = Brush.linearGradient(
        colors = listOf(DeepAccent, DeepAccentStrong)
    )

    // Futuristic Holographic Gradient
    val Holographic = Brush.linearGradient(
        colors = listOf(DeepAccent, DeepTertiary, DeepAccentStrong)
    )

    val EmergencyGradient = Brush.linearGradient(
        colors = listOf(DeepError, Color(0xFF991B1B))
    )

    // Glassmorphic border gradient simulation
    val GlassBorder = Brush.verticalGradient(
        colors = listOf(
            Color.White.copy(alpha = 0.15f),
            Color.White.copy(alpha = 0.05f)
        )
    )
}

object Elevations {
    val flat = 0.dp
    val card = 0.dp // Flat style with borders preferred in Future Deep
    val floating = 8.dp
    val sticky = 4.dp
}
