package com.pulselink.data.sms

import com.pulselink.domain.model.EscalationTier

sealed class PulseLinkMessage {
    abstract val senderId: String
    abstract val code: String

    data class LinkRequest(
        override val senderId: String,
        override val code: String,
        val senderName: String
    ) : PulseLinkMessage()

    data class LinkAccept(
        override val senderId: String,
        override val code: String
    ) : PulseLinkMessage()

    data class Ping(
        override val senderId: String,
        override val code: String
    ) : PulseLinkMessage()

    data class RemoteAlert(
        override val senderId: String,
        override val code: String,
        val tier: EscalationTier
    ) : PulseLinkMessage()

    data class SoundOverride(
        override val senderId: String,
        override val code: String,
        val tier: EscalationTier,
        val soundKey: String?
    ) : PulseLinkMessage()

    data class ConfigUpdate(
        override val senderId: String,
        override val code: String,
        val key: String,
        val value: String
    ) : PulseLinkMessage()
}
