package com.pulselink.receiver

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import com.pulselink.data.link.ContactLinkManager
import com.pulselink.data.link.LinkChannelPayload
import com.pulselink.data.webrtc.WebRtcSignaler
import com.pulselink.domain.model.EscalationTier
import com.pulselink.data.sms.PulseLinkMessage
import com.pulselink.domain.model.MessageUrgency
import com.pulselink.domain.model.VolumeHint
import android.util.Log

@AndroidEntryPoint
class PulseLinkFcmService : FirebaseMessagingService() {

    @Inject lateinit var contactLinkManager: ContactLinkManager
    @Inject lateinit var webRtcSignaler: WebRtcSignaler
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "FCM token refreshed")
        scope.launch {
            runCatching { contactLinkManager.handleLocalFcmToken(token) }
                .onFailure { error -> Log.e(TAG, "Failed to propagate FCM token", error) }
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        val data = message.data
        if (data.isNullOrEmpty()) return
        val type = data["type"].orEmpty()
        val payload = LinkChannelPayload(
            id = data["id"].orEmpty(),
            senderId = data["senderId"].orEmpty(),
            receiverId = data["receiverId"].orEmpty(),
            body = data["body"].orEmpty(),
            timestamp = data["timestamp"]?.toLongOrNull() ?: System.currentTimeMillis(),
            linkCode = data["linkCode"],
            phoneNumber = data["phoneNumber"],
            urgency = data["urgency"]?.let { runCatching { MessageUrgency.valueOf(it) }.getOrNull() }
                ?: MessageUrgency.STANDARD,
            volumeHint = data["volumeHint"]
                ?.takeIf { it.isNotBlank() }
                ?.let { runCatching { VolumeHint.valueOf(it) }.getOrNull() },
            type = type,
            tier = data["tier"]?.let { runCatching { EscalationTier.valueOf(it) }.getOrNull() },
            reason = data["reason"]?.let { runCatching { PulseLinkMessage.AlertPrepareReason.valueOf(it) }.getOrNull() },
            ready = data["ready"]?.toBooleanStrictOrNull(),
            senderName = data["senderName"],
            senderEmail = data["senderEmail"]
        )
        scope.launch {
            runCatching {
                when (type) {
                    "WEBRTC_OFFER" -> {
                        val contact = contactLinkManager.resolveContactForPayload(payload) ?: return@runCatching
                        val sdp = data["sdp"].orEmpty()
                        webRtcSignaler.handleOffer(contact, sdp)
                    }
                    "WEBRTC_ANSWER" -> {
                        val contact = contactLinkManager.resolveContactForPayload(payload) ?: return@runCatching
                        val sdp = data["sdp"].orEmpty()
                        webRtcSignaler.handleAnswer(contact, sdp)
                    }
                    "WEBRTC_ICE" -> {
                        val contact = contactLinkManager.resolveContactForPayload(payload) ?: return@runCatching
                        val mid = data["sdpMid"].orEmpty()
                        val index = data["sdpMLineIndex"]?.toIntOrNull() ?: 0
                        val cand = data["candidate"].orEmpty()
                        webRtcSignaler.handleIce(contact, mid, index, cand)
                    }
                    else -> contactLinkManager.handlePushPayload(payload)
                }
            }.onFailure { error ->
                Log.e(TAG, "Failed to handle FCM payload type=$type", error)
            }
        }
    }

    companion object {
        private const val TAG = "PulseLinkFcmService"
    }
}
