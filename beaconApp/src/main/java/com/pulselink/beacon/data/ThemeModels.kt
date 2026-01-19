package com.pulselink.beacon.data

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight

enum class ThemeTarget {
    IncomingBubble,
    OutgoingBubble,
    BubbleFrame,
    ThreadBackground,
    InboxBackground,
    Accent
}

enum class ThemeFont(val label: String, val family: FontFamily, val weight: FontWeight = FontWeight.Normal) {
    System("System", FontFamily.SansSerif),
    Rounded("Rounded", FontFamily.Cursive),
    Serif("Serif", FontFamily.Serif),
    Mono("Mono", FontFamily.Monospace);

    companion object {
        fun fromName(raw: String?): ThemeFont = values().firstOrNull { it.name == raw } ?: System
    }
}

enum class InboxIconVariant(val label: String) {
    Beacon("Beacon Pulse"),
    Bubble("Chat Bubble"),
    Shield("Shielded"),
    Minimal("Minimal Dot");

    companion object {
        fun fromName(raw: String?): InboxIconVariant =
            values().firstOrNull { it.name == raw } ?: Beacon
    }
}

data class ThemePalette(
    val incoming: Long,
    val outgoing: Long,
    val frame: Long,
    val accent: Long,
    val threadBackground: Long,
    val inboxBackground: Long,
    val bubbleRadius: Float,
    val font: ThemeFont,
    val iconVariant: InboxIconVariant
) {
    val incomingColor: Color get() = incoming.asColor()
    val outgoingColor: Color get() = outgoing.asColor()
    val frameColor: Color get() = frame.asColor()
    val accentColor: Color get() = accent.asColor()
    val threadBackgroundColor: Color get() = threadBackground.asColor()
    val inboxBackgroundColor: Color get() = inboxBackground.asColor()

    fun withTarget(target: ThemeTarget, color: Color): ThemePalette = when (target) {
        ThemeTarget.IncomingBubble -> copy(incoming = color.toLong())
        ThemeTarget.OutgoingBubble -> copy(outgoing = color.toLong())
        ThemeTarget.BubbleFrame -> copy(frame = color.toLong())
        ThemeTarget.ThreadBackground -> copy(threadBackground = color.toLong())
        ThemeTarget.InboxBackground -> copy(inboxBackground = color.toLong())
        ThemeTarget.Accent -> copy(accent = color.toLong())
    }

    fun encode(): String = listOf(
        incoming,
        outgoing,
        frame,
        accent,
        threadBackground,
        inboxBackground,
        bubbleRadius,
        font.name,
        iconVariant.name
    ).joinToString("|")

    companion object {
        // Updated to "Future Deep v9 Neon Noir" Default Palette
        fun default(): ThemePalette = ThemePalette(
            incoming = 0xFF1C2230, // Deep Surface Alt
            outgoing = 0xFF00F0FF, // V9 Electric Cyan
            frame = 0xFF05080E,    // Deep Surface
            accent = 0xFF00F0FF,   // V9 Electric Cyan
            threadBackground = 0xFF000000, // Pitch Black
            inboxBackground = 0xFF000000,  // Pitch Black
            bubbleRadius = 22f,
            font = ThemeFont.System,
            iconVariant = InboxIconVariant.Beacon
        )

        fun decode(raw: String?): ThemePalette {
            if (raw.isNullOrBlank()) return default()
            val parts = raw.split("|")
            return try {
                ThemePalette(
                    incoming = parts.getOrNull(0)?.toLong() ?: default().incoming,
                    outgoing = parts.getOrNull(1)?.toLong() ?: default().outgoing,
                    frame = parts.getOrNull(2)?.toLong() ?: default().frame,
                    accent = parts.getOrNull(3)?.toLong() ?: default().accent,
                    threadBackground = parts.getOrNull(4)?.toLong() ?: default().threadBackground,
                    inboxBackground = parts.getOrNull(5)?.toLong() ?: default().inboxBackground,
                    bubbleRadius = parts.getOrNull(6)?.toFloat() ?: default().bubbleRadius,
                    font = ThemeFont.fromName(parts.getOrNull(7)),
                    iconVariant = InboxIconVariant.fromName(parts.getOrNull(8))
                )
            } catch (_: Exception) {
                default()
            }
        }
    }
}

data class ThemeState(
    val global: ThemePalette = ThemePalette.default(),
    val contactThemes: Map<String, ThemePalette> = emptyMap()
) {
    fun forAddress(address: String?): ThemePalette {
        val key = address.orEmpty()
        return contactThemes[key] ?: global
    }
}

fun Color.toLong(): Long = this.toArgb().toLong()
fun Long.asColor(): Color = Color(this.toInt())
