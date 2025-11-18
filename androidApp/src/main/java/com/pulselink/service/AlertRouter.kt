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
            route(tier, normalized, settings, emptySet())
        }
    }

    suspend fun dispatchManual(
        tier: EscalationTier,
        trigger: String,
        excludeContactIds: Set<Long> = emptySet()
    ): AlertResult? {
        return mutex.withLock {
            val settings = settingsRepository.settings.first()
            route(tier, trigger, settings, excludeContactIds)
        }
    }

    suspend fun onInboundMessage(body: String) {
        val sanitized = body.lowercase()
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
        excludeContactIds: Set<Long>
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

        val result: AlertResult = dispatcher.dispatch(trigger, tier, contacts, settings)

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

    companion object {
        private const val TAG = "AlertRouter"
    }
}
