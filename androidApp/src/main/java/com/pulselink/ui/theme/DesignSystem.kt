package com.pulselink.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

// Future Deep v9 Design System Tokens
// The ultimate unified design language for PulseLink Suite.
// V9: "Neon Noir Refined" - Sharper contrast, deeper blacks, more glassmorphism.

// Colors
val DeepBackground = Color(0xFF000000) // V9 Pitch Black
val DeepSurface = Color(0xFF05080E) // V9 Surface (Very Deep Blue)
val DeepSurfaceAlt = Color(0xFF0A0F1C) // V9 Surface Alt
val DeepAccent = Color(0xFF00F0FF) // V9 Electric Cyan
val DeepAccentStrong = Color(0xFF00B8D4) // Cyan 700
val DeepTertiary = Color(0xFFE000FF) // V9 Neon Magenta
val DeepError = Color(0xFFFF2E4D) // V9 Error
val DeepOnSurface = Color(0xFFE0F7FA) // V9 Ink
val DeepMuted = Color(0xFF64748B) // Slate 500

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
    // V9 "Void" Background
    val PrimaryBackground = Brush.verticalGradient(
        colors = listOf(Color(0xFF020408), DeepBackground)
    )
    
    // Vibrant accent gradient for buttons/FABs
    val BrandGradient = Brush.linearGradient(
        colors = listOf(
            DeepAccent,
            Color(0xFF00E5FF),
            DeepAccentStrong
        )
    )

    // Futuristic Holographic Gradient
    val Holographic = Brush.linearGradient(
        colors = listOf(
            DeepAccent.copy(alpha = 0.9f),
            Color(0xFFD8B4FE).copy(alpha = 0.8f),
            DeepAccentStrong.copy(alpha = 0.9f)
        )
    )

    // "Neon Glow" for active states or borders
    val NeonGlow = Brush.horizontalGradient(
        colors = listOf(
            DeepAccent.copy(alpha = 0.0f),
            DeepAccent.copy(alpha = 0.8f),
            DeepAccent.copy(alpha = 0.0f)
        )
    )

    // V9 Neon Border - sharper
    val NeonBorder = Brush.linearGradient(
        colors = listOf(
            DeepAccent.copy(alpha = 0.6f),
            DeepTertiary.copy(alpha = 0.4f),
            DeepAccent.copy(alpha = 0.6f)
        )
    )

    // V9 Holographic Border
    val HolographicBorder = Brush.sweepGradient(
        colors = listOf(
            DeepAccent.copy(alpha = 0.4f),
            DeepTertiary.copy(alpha = 0.4f),
            Color.White.copy(alpha = 0.6f), // Brighter Shine
            DeepAccent.copy(alpha = 0.4f)
        )
    )

    val EmergencyGradient = Brush.linearGradient(
        colors = listOf(DeepError, Color(0xFF991B1B))
    )

    // Refined V9 Glass Border
    val GlassBorder = Brush.verticalGradient(
        colors = listOf(
            Color.White.copy(alpha = 0.15f),
            Color.White.copy(alpha = 0.03f)
        )
    )

    val SurfaceShine = Brush.linearGradient(
        colors = listOf(
            Color.White.copy(alpha = 0.0f),
            Color.White.copy(alpha = 0.07f),
            Color.White.copy(alpha = 0.0f)
        )
    )

    // V9 New: Holographic Surface for cards
    val HolographicSurface = Brush.linearGradient(
        colors = listOf(
            Color(0xFF0A0F1C).copy(alpha = 0.9f),
            Color(0xFF05080E).copy(alpha = 0.95f)
        )
    )
}

object Elevations {
    val flat = 0.dp
    val card = 0.dp
    val floating = 12.dp
    val sticky = 4.dp
}
