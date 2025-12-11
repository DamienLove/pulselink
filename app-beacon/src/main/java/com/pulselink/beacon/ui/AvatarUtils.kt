package com.pulselink.beacon.ui

import androidx.compose.ui.graphics.Color
import kotlin.math.abs

private val avatarPalette = listOf(
    Color(0xFF5E35B1),
    Color(0xFF00897B),
    Color(0xFF1E88E5),
    Color(0xFFF4511E),
    Color(0xFF6D4C41),
    Color(0xFF3949AB),
    Color(0xFF00838F),
    Color(0xFF7CB342),
    Color(0xFFAFB42B),
    Color(0xFF8E24AA)
)

fun colorFromName(name: String): Color {
    if (name.isBlank()) return avatarPalette.first()
    val index = abs(name.lowercase().hashCode()) % avatarPalette.size
    return avatarPalette[index]
}
