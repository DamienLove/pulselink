package com.pulselink.beacon.sms

sealed class PulseLinkMessage {
    data class ManualMessage(
        val senderId: String,
        val code: String,
        val body: String,
        val tier: EscalationTier = EscalationTier.EMERGENCY
    ) : PulseLinkMessage()
}
