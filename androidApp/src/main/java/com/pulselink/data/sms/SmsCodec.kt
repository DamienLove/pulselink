package com.pulselink.data.sms

import android.net.Uri
import com.pulselink.domain.model.EscalationTier

object SmsCodec {
    private const val PREFIX = "PULSELINK"

    enum class Type(val wire: String) {
        LINK_REQUEST("LINK_REQ"),
        LINK_ACCEPT("LINK_ACCEPT"),
        PING("PING"),
        ALERT("ALERT"),
        ALERT_PREPARE("ALERT_PREP"),
        ALERT_READY("ALERT_READY"),
        MESSAGE("MSG"),
        SOUND_OVERRIDE("SOUND"),
        CONFIG("CONFIG")
    }

    fun encodeLinkRequest(senderId: String, code: String, senderName: String): String =
        build(Type.LINK_REQUEST, senderId, code, Uri.encode(senderName))

    fun encodeLinkAccept(senderId: String, code: String): String =
        build(Type.LINK_ACCEPT, senderId, code)

    fun encodePing(senderId: String, code: String): String =
        build(Type.PING, senderId, code)

    fun encodeRemoteAlert(senderId: String, code: String, tier: EscalationTier): String =
        build(Type.ALERT, senderId, code, tier.name)

    fun encodeAlertPrepare(senderId: String, code: String, tier: EscalationTier): String =
        build(Type.ALERT_PREPARE, senderId, code, tier.name)

    fun encodeAlertReady(senderId: String, code: String, ready: Boolean): String =
        build(Type.ALERT_READY, senderId, code, if (ready) "1" else "0")

    fun encodeManualMessage(senderId: String, code: String, body: String): String =
        build(Type.MESSAGE, senderId, code, Uri.encode(body))

    fun encodeSoundOverride(
        senderId: String,
        code: String,
        tier: EscalationTier,
        soundKey: String?
    ): String = build(Type.SOUND_OVERRIDE, senderId, code, tier.name, soundKey.orEmpty())

    fun encodeConfig(senderId: String, code: String, key: String, value: String): String =
        build(Type.CONFIG, senderId, code, key, value)

    private fun build(type: Type, vararg parts: String): String =
        listOf(PREFIX, type.wire, *parts).joinToString("|")

    fun parse(body: String): PulseLinkMessage? {
        if (!body.startsWith(PREFIX)) return null
        val tokens = body.split('|')
        if (tokens.size < 3) return null
        val typeToken = tokens[1]
        val senderId = tokens.getOrNull(2) ?: return null
        val code = tokens.getOrNull(3) ?: return null
        return when (typeToken) {
            Type.LINK_REQUEST.wire -> {
                val name = tokens.getOrNull(4)?.let { Uri.decode(it) } ?: ""
                PulseLinkMessage.LinkRequest(senderId, code, name)
            }
            Type.LINK_ACCEPT.wire -> PulseLinkMessage.LinkAccept(senderId, code)
            Type.PING.wire -> PulseLinkMessage.Ping(senderId, code)
            Type.ALERT.wire -> {
                val tierToken = tokens.getOrNull(4) ?: return null
                val tier = runCatching { EscalationTier.valueOf(tierToken) }.getOrNull() ?: return null
                PulseLinkMessage.RemoteAlert(senderId, code, tier)
            }
            Type.ALERT_PREPARE.wire -> {
                val tierToken = tokens.getOrNull(4) ?: return null
                val tier = runCatching { EscalationTier.valueOf(tierToken) }.getOrNull() ?: return null
                PulseLinkMessage.AlertPrepare(senderId, code, tier)
            }
            Type.ALERT_READY.wire -> {
                val readyToken = tokens.getOrNull(4) ?: "0"
                val ready = readyToken == "1"
                PulseLinkMessage.AlertReady(senderId, code, ready)
            }
            Type.MESSAGE.wire -> {
                val body = tokens.getOrNull(4)?.let { Uri.decode(it) } ?: ""
                PulseLinkMessage.ManualMessage(senderId, code, body)
            }
            Type.SOUND_OVERRIDE.wire -> {
                val tierToken = tokens.getOrNull(4) ?: return null
                val tier = runCatching { EscalationTier.valueOf(tierToken) }.getOrNull() ?: return null
                val soundKey = tokens.getOrNull(5)?.takeIf { it.isNotEmpty() }
                PulseLinkMessage.SoundOverride(senderId, code, tier, soundKey)
            }
            Type.CONFIG.wire -> {
                val key = tokens.getOrNull(4) ?: return null
                val value = tokens.getOrNull(5) ?: ""
                PulseLinkMessage.ConfigUpdate(senderId, code, key, value)
            }
            else -> null
        }
    }
}
