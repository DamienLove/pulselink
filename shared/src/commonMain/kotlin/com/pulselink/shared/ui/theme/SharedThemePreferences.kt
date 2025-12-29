package com.pulselink.shared.ui.theme

import androidx.compose.ui.graphics.Color
import kotlinx.serialization.Serializable

@Serializable
data class SharedThemePreferences(
    val primaryColor: String = "#6750A4",
    val secondaryColor: String = "#625B71",
    val bubbleOutgoing: String = "#D0BCFF",
    val bubbleIncoming: String = "#E8DEF8",
    val backgroundColor: String = "#FFFFFF",
    val surfaceColor: String = "#FFFFFF",
    val onBackgroundColor: String = "#000000",
    val onBubbleOutgoing: String = "#000000",
    val onBubbleIncoming: String = "#000000",
    val appBackgroundGradientStart: String? = null,
    val appBackgroundGradientEnd: String? = null
) {
    fun toPalette(): ThemePalette {
        val defaults = ThemePalette.default()
        return ThemePalette(
            incoming = bubbleIncoming.toColorOr(defaults.incomingColor).toThemeLong(),
            outgoing = bubbleOutgoing.toColorOr(defaults.outgoingColor).toThemeLong(),
            frame = onBubbleOutgoing.toColorOr(defaults.frameColor).toThemeLong(),
            accent = primaryColor.toColorOr(defaults.accentColor).toThemeLong(),
            threadBackground = backgroundColor.toColorOr(defaults.threadBackgroundColor).toThemeLong(),
            inboxBackground = surfaceColor.toColorOr(defaults.inboxBackgroundColor).toThemeLong(),
            bubbleRadius = defaults.bubbleRadius,
            font = defaults.font,
            iconVariant = defaults.iconVariant
        )
    }

    fun onBackground(): Color = onBackgroundColor.toColorOr(Color.Black)

    companion object {
        fun fromMap(map: Map<*, *>?): SharedThemePreferences {
            if (map == null) return SharedThemePreferences()
            return SharedThemePreferences(
                primaryColor = map["primaryColor"] as? String ?: "#6750A4",
                secondaryColor = map["secondaryColor"] as? String ?: "#625B71",
                bubbleOutgoing = map["bubbleOutgoing"] as? String ?: "#D0BCFF",
                bubbleIncoming = map["bubbleIncoming"] as? String ?: "#E8DEF8",
                backgroundColor = map["backgroundColor"] as? String ?: "#FFFFFF",
                surfaceColor = map["topBarColor"] as? String
                    ?: map["surfaceColor"] as? String ?: "#FFFFFF",
                onBackgroundColor = map["onBackground"] as? String
                    ?: map["onBackgroundColor"] as? String ?: "#000000",
                onBubbleOutgoing = map["onBubbleOutgoing"] as? String ?: "#000000",
                onBubbleIncoming = map["onBubbleIncoming"] as? String ?: "#000000",
                appBackgroundGradientStart = map["appBackgroundGradientStart"] as? String,
                appBackgroundGradientEnd = map["appBackgroundGradientEnd"] as? String
            )
        }
    }
}

private fun String.toColorOr(fallback: Color): Color = hexToColor(this) ?: fallback

fun hexToColor(raw: String?): Color? {
    if (raw.isNullOrBlank()) return null
    val clean = raw.trim().removePrefix("#")
    val hex = when (clean.length) {
        3 -> clean.map { "$it$it" }.joinToString("")
        6 -> "FF$clean"
        8 -> clean
        else -> return null
    }
    return runCatching {
        val value = hex.toLong(16)
        Color(value)
    }.getOrNull()
}
