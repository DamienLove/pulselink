package com.pulselink.beacon.sms

object SmsCodec {
    private const val PREFIX = "PULSELINK"

    fun parse(body: String): PulseLinkMessage? {
        if (!body.startsWith(PREFIX, ignoreCase = true)) return null
        val tokens = body.split('|')
        if (tokens.size < 4) return null
        val type = tokens[1]
        val senderId = tokens[2]
        val code = tokens[3]
        return when (type.uppercase()) {
            "ALERT" -> {
                val tier = tokens.getOrNull(4)?.let { runCatching { EscalationTier.valueOf(it) }.getOrNull() }
                    ?: EscalationTier.EMERGENCY
                PulseLinkMessage.ManualMessage(senderId, code, "ALERT", tier)
            }
            "MSG", "MESSAGE" -> {
                val msgBody = tokens.getOrNull(4) ?: ""
                PulseLinkMessage.ManualMessage(senderId, code, msgBody, EscalationTier.EMERGENCY)
            }
            else -> null
        }
    }
}

enum class EscalationTier { EMERGENCY, CHECK_IN }
