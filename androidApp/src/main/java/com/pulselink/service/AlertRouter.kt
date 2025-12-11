package com.pulselink.service

import android.util.Log
import com.pulselink.data.alert.AlertDispatcher
import com.pulselink.data.alert.AlertDispatcher.AlertResult
import com.pulselink.data.link.ContactLinkManager
import com.pulselink.data.link.RemoteAlertResult
import com.pulselink.data.link.RemoteAlertStatus
import com.pulselink.domain.model.AlertEvent
import com.pulselink.domain.model.EscalationTier
import com.pulselink.domain.repository.AlertRepository
import com.pulselink.domain.repository.ContactRepository
import com.pulselink.domain.repository.SettingsRepository
import dagger.Lazy
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton
import java.util.Locale

@Singleton
class AlertRouter @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val contactRepository: ContactRepository,
    private val alertRepository: AlertRepository,
    private val dispatcher: AlertDispatcher,
    private val contactLinkManager: Lazy<ContactLinkManager>
) {
    private val mutex = Mutex()

    suspend fun onPhraseDetected(phrase: String) {
        mutex.withLock {
            val settings = settingsRepository.settings.first()
            val normalized = phrase.lowercase().trim()
            val phrases = settings.phrases()
            val matchIndex = phrases.indexOfFirst { normalized.contains(it) }
            if (matchIndex == -1) return
            val tier = if (matchIndex == 0) EscalationTier.EMERGENCY else EscalationTier.CHECK_IN
            route(tier, normalized, settings, emptySet(), playLocalSound = false)
        }
    }

    suspend fun dispatchManual(
        tier: EscalationTier,
        trigger: String,
        excludeContactIds: Set<Long> = emptySet(),
        playLocalSound: Boolean = false
    ): AlertResult? {
        return mutex.withLock {
            val settings = settingsRepository.settings.first()
            route(tier, trigger, settings, excludeContactIds, playLocalSound)
        }
    }

    suspend fun onInboundMessage(body: String, sender: String) {
        if (handleAutomationTrigger(body, sender)) return
        val sanitized = body.lowercase(Locale.US)
        if (sanitized.contains("ack pulselink")) {
            alertRepository.record(
                AlertEvent(
                    timestamp = System.currentTimeMillis(),
                    triggeredBy = "Inbound acknowledgement",
                    tier = EscalationTier.CHECK_IN,
                    contactCount = 0,
                    sentSms = false,
                    sharedLocation = false,
                    isIncoming = true
                )
            )
        }
    }

    private suspend fun route(
        tier: EscalationTier,
        trigger: String,
        settings: com.pulselink.domain.model.PulseLinkSettings,
        excludeContactIds: Set<Long>,
        playLocalSound: Boolean
    ): AlertResult? = coroutineScope {
        val contacts = when (tier) {
            EscalationTier.EMERGENCY -> contactRepository.getEmergencyContacts()
            EscalationTier.CHECK_IN -> contactRepository.getCheckInContacts()
        }.filterNot { excludeContactIds.contains(it.id) }
        if (contacts.isEmpty()) {
            return@coroutineScope null
        }

        val remoteJobs = if (tier == EscalationTier.EMERGENCY) {
            contacts.map { contact ->
                async {
                    contactLinkManager.get().triggerRemoteAlert(contact, tier)
                }
            }
        } else emptyList()

        val result: AlertResult = dispatcher.dispatch(
            phrase = trigger,
            tier = tier,
            contacts = contacts,
            settings = settings,
            shouldPlayLocalSound = playLocalSound
        )

        remoteJobs.forEach { deferred ->
            val remoteResult = runCatching { deferred.await() }
                .onFailure { error -> Log.e(TAG, "Remote override task failed", error) }
                .getOrNull()
            if (remoteResult != null) {
                Log.d(
                    TAG,
                    "Remote alert ${remoteResult.status} for ${remoteResult.contactName} (${remoteResult.contactId})"
                )
                if (remoteResult.status == RemoteAlertStatus.SMS_FAILED) {
                    Log.w(TAG, "Unable to deliver remote alert control SMS to ${remoteResult.contactName}")
                }
            }
        }

        alertRepository.record(
            AlertEvent(
                timestamp = System.currentTimeMillis(),
                triggeredBy = trigger,
                tier = tier,
                contactCount = contacts.size,
                sentSms = result.notifiedContacts > 0,
                sharedLocation = result.sharedLocation,
                contactId = result.contactId,
                contactName = null,
                isIncoming = false,
                soundKey = result.soundKey
            )
        )
        result
    }

    private suspend fun handleAutomationTrigger(body: String, sender: String): Boolean {
        val tokens = body.trim().lowercase(Locale.US).split(Regex("\\s+")).filter { it.isNotBlank() }
        if (tokens.isEmpty() || tokens.first() != "pulselink") return false
        val isCancel = tokens.getOrNull(1)?.equals("cancel", ignoreCase = true) == true
        val pin = if (isCancel) tokens.getOrNull(2) else tokens.getOrNull(1)
        if (pin.isNullOrBlank()) return false

        val contact = findContactByPin(pin, sender) ?: return false

        return if (isCancel) {
            val cancelled = contactLinkManager.get().cancelActiveEmergency()
            if (cancelled) {
                settingsRepository.setEmergencyActive(false)
                alertRepository.record(
                    AlertEvent(
                        timestamp = System.currentTimeMillis(),
                        triggeredBy = "SMS cancel from ${contact.displayName}",
                        tier = EscalationTier.EMERGENCY,
                        contactCount = 0,
                        sentSms = false,
                        sharedLocation = false,
                        contactId = contact.id,
                        contactName = contact.displayName,
                        isIncoming = true
                    )
                )
            }
            cancelled
        } else {
            val result = dispatchManual(
                tier = EscalationTier.EMERGENCY,
                trigger = "SMS trigger from ${contact.displayName}",
                playLocalSound = true
            )
            settingsRepository.setEmergencyActive(true)
            alertRepository.record(
                AlertEvent(
                    timestamp = System.currentTimeMillis(),
                    triggeredBy = "SMS trigger from ${contact.displayName}",
                    tier = EscalationTier.EMERGENCY,
                    contactCount = result?.notifiedContacts ?: 0,
                    sentSms = (result?.notifiedContacts ?: 0) > 0,
                    sharedLocation = result?.sharedLocation ?: false,
                    contactId = contact.id,
                    contactName = contact.displayName,
                    isIncoming = true,
                    soundKey = result?.soundKey
                )
            )
            true
        }
    }

    private suspend fun findContactByPin(pin: String, sender: String): com.pulselink.domain.model.Contact? {
        val normalizedSender = normalizePhone(sender)
        if (normalizedSender.isBlank()) return null
        val contacts = contactRepository.getAll()
        return contacts.firstOrNull { contact ->
            contact.remoteTriggerPin.equals(pin, ignoreCase = false) &&
                (listOf(contact.phoneNumber) + contact.additionalPhones)
                    .map { normalizePhone(it) }
                    .any { it.isNotBlank() && it == normalizedSender }
        }
    }

    private fun normalizePhone(input: String): String {
        if (input.isBlank()) return ""
        val digits = buildString {
            input.forEach { ch ->
                if (ch.isDigit()) append(ch)
            }
        }
        return if (input.startsWith("+")) "+$digits" else digits
    }

    companion object {
        private const val TAG = "AlertRouter"
    }
}
