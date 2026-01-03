package com.pulselink.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

// Future Deep 3.0 Design System Tokens
// Synchronized with Web v3 Redesign (Nebula/Electric)

// Colors
val DeepBackground = Color(0xFF030407)
val DeepSurface = Color(0xFF0E121E)
val DeepSurfaceAlt = Color(0xFF161B2E)

val DeepAccent = Color(0xFF22D3EE)         // Cyan
val DeepAccentStrong = Color(0xFF0EA5E9)

val DeepAccentSecondary = Color(0xFFA855F7) // Electric Purple
val DeepAccentTertiary = Color(0xFFF472B6)  // Neon Pink

val DeepError = Color(0xFFEF4444)

object Spacing {
    val extraSmall = 6.dp
    val small = 10.dp
    val medium = 18.dp
    val large = 26.dp
    val extraLarge = 40.dp
    val section = 64.dp
}

object Layout {
    val cardCornerRadius = 24.dp // Increased for v3
    val buttonCornerRadius = 14.dp
    val inputCornerRadius = 14.dp
    val bottomSheetCornerRadius = 32.dp
    val dialogCornerRadius = 24.dp
}

object Gradients {
    // "Nebula" Background Effect
    // Using a vertical approximation for the complex web radial gradient
    val PrimaryBackground = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF0F0518), // Subtle purple tint at top
            DeepBackground,
            DeepBackground
        )
    )
    
    // Electric Gradient for primary actions
    val BrandGradient = Brush.linearGradient(
        colors = listOf(DeepAccent, DeepAccentStrong)
    )

    // Secondary "Future" Gradient
    val SecondaryGradient = Brush.linearGradient(
        colors = listOf(DeepAccentSecondary, Color(0xFF7C3AED))
    )

    val EmergencyGradient = Brush.linearGradient(
        colors = listOf(DeepError, Color(0xFF991B1B))
    )

    // Glassmorphic border
    val GlassBorder = Brush.verticalGradient(
        colors = listOf(
            Color.White.copy(alpha = 0.12f),
            Color.White.copy(alpha = 0.02f)
        )
    )
}

object Elevations {
    val flat = 0.dp
    val card = 0.dp
    val floating = 8.dp
    val sticky = 4.dp
}
