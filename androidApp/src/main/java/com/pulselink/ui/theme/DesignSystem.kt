package com.pulselink.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

// Future Deep v7 Design System Tokens
// The ultimate unified design language for PulseLink Suite.

// Colors
val DeepBackground = Color(0xFF030508) // V7 Deepest Black/Blue
val DeepSurface = Color(0xFF0B101B) // V7 Surface
val DeepSurfaceAlt = Color(0xFF161B2E) // V7 Surface Alt
val DeepAccent = Color(0xFF00F0FF) // V7 Cyber Cyan
val DeepAccentStrong = Color(0xFF00BCD4) // V7 Cyan Strong
val DeepTertiary = Color(0xFFD946EF) // V7 Neon Fuchsia
val DeepError = Color(0xFFFF0055) // V7 Error
val DeepOnSurface = Color(0xFFF0F4F8) // V7 Ink
val DeepMuted = Color(0xFF64748B) // V7 Muted
val DeepSuccess = Color(0xFF00FF9D) // V7 Success

object Spacing {
    val extraSmall = 6.dp
    val small = 10.dp
    val medium = 18.dp
    val large = 26.dp
    val extraLarge = 40.dp
    val section = 64.dp
}

object Layout {
    val cardCornerRadius = 26.dp
    val buttonCornerRadius = 18.dp
    val inputCornerRadius = 18.dp
    val bottomSheetCornerRadius = 36.dp
    val dialogCornerRadius = 28.dp
}

object Gradients {
    // V7 "Alive" Background approximation
    val PrimaryBackground = Brush.verticalGradient(
        colors = listOf(Color(0xFF0A0F1C), DeepBackground)
    )
    
    // Vibrant accent gradient for buttons/FABs
    val BrandGradient = Brush.linearGradient(
        colors = listOf(
            DeepAccent,
            DeepAccentStrong,
            Color(0xFF38BDF8)
        )
    )

    // Futuristic Holographic Gradient
    val Holographic = Brush.linearGradient(
        colors = listOf(
            DeepAccent.copy(alpha = 0.9f),
            Color(0xFFE879F9).copy(alpha = 0.8f), // Lighter fuchsia
            DeepAccentStrong.copy(alpha = 0.9f)
        )
    )

    // "Neon Glow" for active states or borders
    val NeonGlow = Brush.horizontalGradient(
        colors = listOf(
            DeepAccent.copy(alpha = 0.0f),
            DeepAccent.copy(alpha = 0.6f),
            DeepAccent.copy(alpha = 0.0f)
        )
    )

    // New V7 Neon Border - softer, wider
    val NeonBorder = Brush.linearGradient(
        colors = listOf(
            DeepAccent.copy(alpha = 0.5f),
            DeepTertiary.copy(alpha = 0.3f),
            DeepAccent.copy(alpha = 0.5f)
        )
    )

    val EmergencyGradient = Brush.linearGradient(
        colors = listOf(DeepError, Color(0xFF991B1B))
    )

    // Refined V7 Glass Border
    val GlassBorder = Brush.verticalGradient(
        colors = listOf(
            Color.White.copy(alpha = 0.15f),
            Color.White.copy(alpha = 0.05f)
        )
    )

    val SurfaceShine = Brush.linearGradient(
        colors = listOf(
            Color.White.copy(alpha = 0.0f),
            Color.White.copy(alpha = 0.03f),
            Color.White.copy(alpha = 0.0f)
        )
    )
}

object Elevations {
    val flat = 0.dp
    val card = 0.dp
    val floating = 8.dp
    val sticky = 4.dp
}
