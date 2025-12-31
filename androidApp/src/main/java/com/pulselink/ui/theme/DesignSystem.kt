package com.pulselink.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.pulselink.ui.theme.FutureDeepColors.BackgroundDark
import com.pulselink.ui.theme.FutureDeepColors.EmergencyDarkRed
import com.pulselink.ui.theme.FutureDeepColors.EmergencyRed
import com.pulselink.ui.theme.FutureDeepColors.PrimaryIndigo
import com.pulselink.ui.theme.FutureDeepColors.SecondaryCyan
import com.pulselink.ui.theme.FutureDeepColors.SurfaceDark

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
        colors = listOf(PrimaryIndigo, SecondaryCyan)
    )

    val EmergencyGradient = Brush.linearGradient(
        colors = listOf(EmergencyRed, EmergencyDarkRed)
    )

    val GlassGradient = Brush.verticalGradient(
        colors = listOf(
            SurfaceDark.copy(alpha = 0.9f),
            SurfaceDark.copy(alpha = 0.7f)
        )
    )
}

object Elevations {
    val card = 0.dp
    val floating = 8.dp
}
