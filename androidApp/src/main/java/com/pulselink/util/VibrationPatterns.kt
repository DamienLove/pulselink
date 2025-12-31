package com.pulselink.util

data class VibrationPatternOption(
    val key: String,
    val label: String,
    val pattern: LongArray,
    val isCustom: Boolean = false
)

data class CustomVibrationPattern(
    val id: String,
    val name: String,
    val pattern: LongArray
)

object VibrationPatterns {
    const val MESSAGE_DEFAULT = "message_default"
    const val ALERT_DEFAULT = "alert_default"

    val messageOptions: List<VibrationPatternOption> = listOf(
        VibrationPatternOption(
            key = MESSAGE_DEFAULT,
            label = "Default",
            pattern = longArrayOf(0, 300)  // Single 300ms pulse
        ),
        VibrationPatternOption(
            key = "message_short",
            label = "Short burst",
            pattern = longArrayOf(0, 100, 150, 100)  // Two quick 100ms pulses
        ),
        VibrationPatternOption(
            key = "message_double",
            label = "Double tap",
            pattern = longArrayOf(0, 200, 100, 200)  // Two distinct 200ms pulses
        ),
        VibrationPatternOption(
            key = "message_triple",
            label = "Triple tap",
            pattern = longArrayOf(0, 150, 100, 150, 100, 150)  // Three quick pulses
        ),
        VibrationPatternOption(
            key = "message_long",
            label = "Long pulse",
            pattern = longArrayOf(0, 600)  // Single long 600ms pulse
        ),
        VibrationPatternOption(
            key = "message_rhythm",
            label = "Rhythmic",
            pattern = longArrayOf(0, 100, 100, 100, 100, 300)  // Short-short-long pattern
        ),
        VibrationPatternOption(
            key = "message_strong",
            label = "Strong pulse",
            pattern = longArrayOf(0, 400, 200, 400)  // Two strong pulses
        )
    )

    val alertOptions: List<VibrationPatternOption> = listOf(
        VibrationPatternOption(
            key = ALERT_DEFAULT,
            label = "Default",
            pattern = longArrayOf(0, 300, 200, 300, 200, 300)  // Three steady pulses
        ),
        VibrationPatternOption(
            key = "alert_urgent",
            label = "Urgent",
            pattern = longArrayOf(0, 150, 80, 150, 80, 150, 80, 150, 80, 150)  // Rapid repeated pulses
        ),
        VibrationPatternOption(
            key = "alert_strong",
            label = "Strong wave",
            pattern = longArrayOf(0, 500, 150, 500)  // Two strong long pulses
        ),
        VibrationPatternOption(
            key = "alert_sos",
            label = "S.O.S.",
            pattern = longArrayOf(0, 100, 80, 100, 80, 100, 200, 300, 100, 300, 100, 300, 200, 100, 80, 100, 80, 100)  // ... --- ... (actual Morse code)
        ),
        VibrationPatternOption(
            key = "alert_escalating",
            label = "Escalating",
            pattern = longArrayOf(0, 200, 150, 300, 150, 400, 150, 500)  // Progressively stronger
        ),
        VibrationPatternOption(
            key = "alert_heartbeat",
            label = "Heartbeat",
            pattern = longArrayOf(0, 150, 100, 80, 400, 150, 100, 80)  // Lub-dub pattern
        )
    )

    fun messageOption(key: String?): VibrationPatternOption =
        messageOptions.firstOrNull { it.key == key } ?: messageOptions.first()

    fun alertOption(key: String?): VibrationPatternOption =
        alertOptions.firstOrNull { it.key == key } ?: alertOptions.first()
}
