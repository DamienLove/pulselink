package com.pulselink.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

// Future Deep v3 Design System Tokens
// Aligned with PulseLinkTheme.kt and Web v3 Redesign

// Colors
val DeepBackground = Color(0xFF05070F)
val DeepSurface = Color(0xFF0E121E) // Slightly brighter than bg for surface
val DeepAccent = Color(0xFF22D3EE) // Cyan
val DeepAccentStrong = Color(0xFF0EA5E9)
val DeepTertiary = Color(0xFFBC13FE) // Neon Purple
val DeepError = Color(0xFFEF4444)
val DeepOnSurface = Color(0xFFEEF2FB)

object Spacing {
    val extraSmall = 6.dp
    val small = 10.dp
    val medium = 18.dp
    val large = 26.dp
    val extraLarge = 40.dp
    val section = 64.dp
}

object Layout {
    val cardCornerRadius = 26.dp // Increased for v3 smoother look
    val buttonCornerRadius = 18.dp
    val inputCornerRadius = 18.dp
    val bottomSheetCornerRadius = 36.dp
    val dialogCornerRadius = 28.dp
}

object Gradients {
    // Subtle radial-like effect for backgrounds using vertical gradient approximation
    val PrimaryBackground = Brush.verticalGradient(
        colors = listOf(Color(0xFF0D1224), DeepBackground)
    )
    
    // Vibrant accent gradient for buttons/FABs
    val BrandGradient = Brush.linearGradient(
        colors = listOf(
            DeepAccent,
            DeepAccentStrong,
            Color(0xFF38BDF8) // Sky 400 for extra pop
        )
    )

    // Futuristic Holographic Gradient for special cards
    val Holographic = Brush.linearGradient(
        colors = listOf(
            DeepAccent.copy(alpha = 0.9f),
            Color(0xFFD8B4FE).copy(alpha = 0.8f), // Soft Purple
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

    val EmergencyGradient = Brush.linearGradient(
        colors = listOf(DeepError, Color(0xFF991B1B))
    )

    // Glassmorphic border gradient simulation
    val GlassBorder = Brush.verticalGradient(
        colors = listOf(
            Color.White.copy(alpha = 0.25f), // Brighter top for highlight
            Color.White.copy(alpha = 0.05f)
        )
    )

    val GlassSurface = Brush.verticalGradient(
        colors = listOf(
            Color.White.copy(alpha = 0.1f),
            Color.White.copy(alpha = 0.03f)
        )
    )

    val NeonBorder = Brush.horizontalGradient(
        colors = listOf(
            DeepAccent.copy(alpha = 0.5f),
            DeepAccentStrong.copy(alpha = 0.8f),
            DeepAccent.copy(alpha = 0.5f)
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
    val card = 0.dp // Flat style with borders preferred in Future Deep
    val floating = 8.dp
    val sticky = 4.dp
}
