package com.pulselink.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.util.Log
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject
import com.pulselink.data.link.ContactLinkManager
import com.pulselink.data.link.RemoteActionHandler
import com.pulselink.data.emergency.EmergencyLocationRepository
import com.pulselink.data.emergency.parseEmergencyAlertLocation
import com.pulselink.data.ai.AiAssistantRepository
import com.pulselink.data.sms.OtpHelper
import com.pulselink.data.sms.OtpNotifier
import com.pulselink.data.sms.SmsCodec
import com.pulselink.data.sms.MessageNotificationManager
import com.pulselink.data.sms.SmsStore
import com.pulselink.service.AlertRouter
import com.pulselink.domain.repository.ContactRepository
import com.pulselink.domain.repository.SettingsRepository
import com.pulselink.domain.model.Contact
import com.pulselink.domain.model.EscalationTier
import com.pulselink.domain.model.MessageUrgency
import com.pulselink.domain.model.PulseLinkSettings
import com.pulselink.domain.model.VolumeHint
import com.pulselink.BuildConfig
import android.telephony.PhoneNumberUtils
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.first

@AndroidEntryPoint
class PulseLinkSmsReceiver : BroadcastReceiver() {

    @Inject lateinit var alertRouter: AlertRouter
    @Inject lateinit var contactLinkManager: ContactLinkManager
    @Inject lateinit var smsStore: SmsStore
    @Inject lateinit var contactRepository: ContactRepository
    @Inject lateinit var remoteActionHandler: RemoteActionHandler
    @Inject lateinit var emergencyLocationRepository: EmergencyLocationRepository
    @Inject lateinit var aiAssistantRepository: AiAssistantRepository
    @Inject lateinit var settingsRepository: SettingsRepository
    @Inject lateinit var smsSyncTrigger: com.pulselink.data.sms.SmsSyncTrigger

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action != Telephony.Sms.Intents.SMS_DELIVER_ACTION) {
            return
        }
        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        val mergedBody = messages.joinToString(separator = "") { it.messageBody }
        val body = mergedBody.trim()
        if (body.isBlank()) return
        val origin = messages.firstOrNull()?.originatingAddress.orEmpty()
        val timestamp = messages.maxOfOrNull { it.timestampMillis } ?: System.currentTimeMillis()
        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.Default).launch {
            try {
                try {
                    smsStore.insertIncoming(origin, body, timestamp)
                    // Trigger immediate sync to cloud when message received
                    smsSyncTrigger.triggerSync()
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to insert incoming SMS from $origin", e)
                }
                val settings = settingsRepository.settings.first()
                val completed = withTimeoutOrNull(8_000L) {
                    val parsed = SmsCodec.parse(body)
                    if (parsed != null) {
                        contactLinkManager.handleInbound(parsed, origin)
                    } else {
                        alertRouter.onInboundMessage(body)
                        val alertLocation = parseEmergencyAlertLocation(body)
                        if (alertLocation != null && alertLocation.severity == EmergencyLocationRepository.SEVERITY_EMERGENCY) {
                            val normalized = PhoneNumberUtils.normalizeNumber(origin)
                            val contact = contactRepository.getByPhone(origin)
                                ?: contactRepository.getByPhone(normalized)
                            emergencyLocationRepository.recordIncoming(origin, contact, alertLocation, body)
                        }
                        val otpCode = if (OtpHelper.isOtpMessage(origin, body)) {
                            OtpHelper.extractCode(body)
                        } else {
                            null
                        }
                        if (otpCode != null) {
                            OtpNotifier.notify(context, origin, otpCode)
                        }

                        val pinMatch = PIN_REGEX.find(body)
                        if (pinMatch != null) {
                            val pin = pinMatch.groupValues[1]
                            val normalized = PhoneNumberUtils.normalizeNumber(origin)
                            val contact = contactRepository.getByPhone(origin)
                                ?: contactRepository.getByPhone(normalized)

                            if (contact != null && contact.remotePin == pin) {
                                val tier = EscalationTier.EMERGENCY
                                remoteActionHandler.playAttentionTone(
                                    contact = contact,
                                    tier = tier,
                                    title = "Emergency override by PIN",
                                    body = body,
                                    notificationId = (contact.id.hashCode() and 0xFFFF) + 9000,
                                    forceBypass = true,
                                    volumeHint = VolumeHint.MAX
                                )
                            }
                        }

                        handleTrustedSms(origin, body, settings)
                        val threadId = runCatching {
                            Telephony.Threads.getOrCreateThreadId(context, origin)
                        }.getOrNull()
                        MessageNotificationManager.notifyIncoming(
                            context = context,
                            address = origin,
                            body = body,
                            timestamp = timestamp,
                            settings = settings,
                            threadId = threadId
                        )
                    }
                }
                if (completed == null) {
                    Log.w(TAG, "SMS processing timed out for sender: $origin")
                }
            } catch (error: Exception) {
                Log.e(TAG, "Failed to handle inbound SMS from $origin", error)
            } finally {
                pendingResult.finish()
            }
        }
    }

    private suspend fun handleTrustedSms(origin: String, body: String, settings: PulseLinkSettings) {
        val normalized = PhoneNumberUtils.normalizeNumber(origin)
        val contact = contactRepository.getByPhone(origin)
            ?: contactRepository.getByPhone(normalized)
        val premium = BuildConfig.PREMIUM_FEATURES || settings.premiumUnlocked
        val aiEnabled = premium && settings.aiUrgencyEnabled
        val includeUnknown = settings.aiUrgencyIncludeUnknown

        if (contact == null && !(aiEnabled && includeUnknown)) return

        val normalizedBody = body.lowercase()
        val primaryPhrase = settings.primaryPhrase.trim().lowercase()
        val hasPrimaryPhrase = primaryPhrase.isNotBlank() && normalizedBody.contains(primaryPhrase)

        val baseUrgency = contact?.let {
            when {
                hasPrimaryPhrase -> MessageUrgency.EMERGENCY
                OtpHelper.isUrgentBody(body) -> MessageUrgency.URGENT
                it.escalationTier == EscalationTier.EMERGENCY -> MessageUrgency.URGENT
                else -> MessageUrgency.STANDARD
            }
        } ?: MessageUrgency.STANDARD

        var effectiveUrgency = baseUrgency
        var aiUpgraded = false

        if (aiEnabled && (contact != null || includeUnknown) && FirebaseAuth.getInstance().currentUser != null) {
            val aiResult = runCatching {
                withTimeoutOrNull(AI_CLASSIFY_TIMEOUT_MS) {
                    aiAssistantRepository.classifyUrgency(body)
                }
            }.getOrNull()
            if (aiResult != null && aiResult.confidence >= AI_CONFIDENCE_THRESHOLD) {
                if (urgencyRank(aiResult.urgency) > urgencyRank(baseUrgency)) {
                    effectiveUrgency = aiResult.urgency
                    aiUpgraded = true
                }
            }
        }

        if (contact == null && !aiUpgraded) return

        // Only play attention tone for EMERGENCY messages, not URGENT/STANDARD
        // This prevents false alarms from AI misclassifications (fixes #266, #251, #243)
        // QA TEST: Send normal messages like "miss you" or beacon check-ins between trusted contacts
        // EXPECTED: Should NOT trigger emergency sirens/attention tones
        // EXPECTED: Only messages with emergency phrase or high-confidence emergency classification should trigger alerts
        if (effectiveUrgency != MessageUrgency.EMERGENCY) return

        val tier = when {
            effectiveUrgency != MessageUrgency.EMERGENCY -> EscalationTier.CHECK_IN
            aiUpgraded && !settings.aiUrgencyBypassDnd -> EscalationTier.CHECK_IN
            else -> EscalationTier.EMERGENCY
        }
        val volumeHint = when (effectiveUrgency) {
            MessageUrgency.EMERGENCY -> VolumeHint.MAX
            MessageUrgency.URGENT -> VolumeHint.HIGH
            MessageUrgency.STANDARD -> VolumeHint.MEDIUM
        }
        val targetContact = contact ?: buildUnknownContact(origin, settings.aiUrgencyBypassDnd)
        val title = "Message from ${targetContact.displayName}"
        val preview = body.take(160).ifBlank { "New message received." }
        remoteActionHandler.playAttentionTone(
            contact = targetContact,
            tier = tier,
            title = title,
            body = preview,
            notificationId = (targetContact.id.hashCode() and 0xFFFF) + 7000,
            forceBypass = aiUpgraded && settings.aiUrgencyBypassDnd,
            volumeHint = volumeHint
        )
    }

    private fun urgencyRank(urgency: MessageUrgency): Int = when (urgency) {
        MessageUrgency.STANDARD -> 0
        MessageUrgency.URGENT -> 1
        MessageUrgency.EMERGENCY -> 2
    }

    private fun buildUnknownContact(origin: String, allowOverride: Boolean): Contact {
        val displayName = origin.ifBlank { "Unknown sender" }
        return Contact(
            id = origin.hashCode().toLong(),
            displayName = displayName,
            phoneNumber = origin,
            escalationTier = EscalationTier.CHECK_IN,
            allowRemoteOverride = allowOverride
        )
    }

    companion object {
        private const val TAG = "PulseLinkSmsReceiver"
        private const val AI_CLASSIFY_TIMEOUT_MS = 4_000L
        private const val AI_CONFIDENCE_THRESHOLD = 0.75f  // Increased from 0.6 to reduce false positives
        private val PIN_REGEX = Regex("pulselink\\s+(\\d+)\\s+emergency", RegexOption.IGNORE_CASE)
    }
}
