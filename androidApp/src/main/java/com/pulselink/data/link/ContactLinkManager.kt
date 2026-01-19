package com.pulselink.data.link

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.provider.CallLog
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.pulselink.R
import com.pulselink.data.alert.NotificationRegistrar
import com.pulselink.data.alert.SoundCatalog
import com.pulselink.data.sms.PulseLinkMessage
import com.pulselink.data.sms.SmsCodec
import com.pulselink.data.sms.SmsSender
import com.pulselink.domain.model.AlertEvent
import com.pulselink.domain.model.Contact
import com.pulselink.domain.model.ContactMessage
import com.pulselink.domain.model.EscalationTier
import com.pulselink.domain.model.LinkStatus
import com.pulselink.domain.model.ManualMessageResult
import com.pulselink.domain.model.MessageChannel
import com.pulselink.domain.model.MessageDirection
import com.pulselink.domain.model.MessageStatus
import com.pulselink.domain.model.RemotePresence
import com.pulselink.domain.model.SoundCategory
import com.pulselink.domain.repository.AlertRepository
import com.pulselink.domain.repository.BlockedContactRepository
import com.pulselink.domain.repository.ContactRepository
import com.pulselink.domain.repository.MessageRepository
import com.pulselink.domain.repository.SettingsRepository
import com.pulselink.service.AlertRouter
import com.pulselink.util.AudioOverrideManager
import com.pulselink.util.VibrationPatterns
import com.pulselink.util.resolveUri
import com.pulselink.ui.EmergencyPopupActivity
import com.pulselink.util.CallStateMonitor
import com.pulselink.widget.WidgetStateManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.SetOptions
import com.google.firebase.Timestamp
import com.google.firebase.functions.FirebaseFunctions
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@android.annotation.SuppressLint("MissingPermission")
@Singleton
class ContactLinkManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val smsSender: SmsSender,
    private val settingsRepository: SettingsRepository,
    private val alertRepository: AlertRepository,
    private val contactRepository: ContactRepository,
    private val blockedContactRepository: BlockedContactRepository,
    private val remoteActionHandler: RemoteActionHandler,
    private val messageRepository: MessageRepository,
    private val callStateMonitor: CallStateMonitor,
    private val linkChannelService: LinkChannelService,
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val functions: FirebaseFunctions,
    private val widgetStateManager: WidgetStateManager,
    private val messageDeliveryTracker: MessageDeliveryTracker
) {

    private val notificationManager by lazy { NotificationManagerCompat.from(context) }
    private val alertHandshake = ConcurrentHashMap<String, CompletableDeferred<Boolean>>()
    @Volatile private var incomingMonitorActive = false
    private val monitorScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val remoteAlertDedup = ConcurrentHashMap<String, Long>()
    enum class CallPreparationResult { READY, TIMEOUT, FAILED }

    init {
        linkChannelService.start()
        monitorScope.launch {
            linkChannelService.inboundMessages.collect { payload ->
                handleRealtimeMessage(payload)
            }
        }
    }

    private suspend fun sendMessageWithFallback(
        contact: Contact,
        message: PulseLinkMessage,
        smsBody: String,
        awaitSmsResult: Boolean = true
    ): Boolean {
        val settings = settingsRepository.settings.first()
        val priority = settings.messagingChannelPriority // Default is [FIREBASE, SMS, EMAIL]
        val skipSms = message is PulseLinkMessage.AlertPrepare &&
            message.reason == PulseLinkMessage.AlertPrepareReason.MESSAGE

        for (channel in priority) {
            when (channel) {
                MessageChannel.FIREBASE -> {
                    val isHandshake = message is PulseLinkMessage.LinkRequest || message is PulseLinkMessage.LinkAccept
                    if (settings.firebaseMessagingEnabled &&
                        !contact.remoteDeviceId.isNullOrBlank() &&
                        (contact.linkStatus == LinkStatus.LINKED || isHandshake)) {

                        messageDeliveryTracker.recordAttempt(contact.id, MessageChannel.FIREBASE)
                        val success = linkChannelService.sendMessage(message)
                        if (success) {
                            messageDeliveryTracker.recordSuccess(contact.id, MessageChannel.FIREBASE)
                            return true
                        } else {
                            messageDeliveryTracker.recordFailure(contact.id, MessageChannel.FIREBASE)
                        }
                    }
                }
                MessageChannel.SMS -> {
                     if (!skipSms) {
                         val targetPhone = contact.primaryPhone()
                         if (!targetPhone.isNullOrBlank() && hasSmsPermission()) {
                             messageDeliveryTracker.recordAttempt(contact.id, MessageChannel.SMS)
                             val success = smsSender.sendSms(targetPhone, smsBody, awaitResult = awaitSmsResult)
                             if (success) {
                                 messageDeliveryTracker.recordSuccess(contact.id, MessageChannel.SMS)
                                 return true
                             } else {
                                 messageDeliveryTracker.recordFailure(contact.id, MessageChannel.SMS)
                             }
                         }
                     }
                }
                MessageChannel.EMAIL -> {
                    val hasPhone = !contact.primaryPhone().isNullOrBlank()
                    val hasEmail = !contact.primaryEmail().isNullOrBlank()
                    val allowEmail = settings.emailFallbackEnabled || !hasPhone
                    if (allowEmail && hasEmail) {
                        messageDeliveryTracker.recordAttempt(contact.id, MessageChannel.EMAIL)
                        // Trigger email via Cloud Function
                        val success = sendEmailNotification(contact, message)
                        if (success) {
                             messageDeliveryTracker.recordSuccess(contact.id, MessageChannel.EMAIL)
                             return true
                        } else {
                             messageDeliveryTracker.recordFailure(contact.id, MessageChannel.EMAIL)
                        }
                    }
                }
            }
        }
        return false
    }

    private suspend fun sendEmailNotification(contact: Contact, message: PulseLinkMessage): Boolean {
        // Call the Firebase Function 'sendEmailNotification'
        val targetEmail = contact.primaryEmail()?.takeIf { it.isNotBlank() } ?: return false
        val data = hashMapOf(
            "email" to targetEmail,
            "messageType" to message::class.simpleName,
            "senderName" to (auth.currentUser?.displayName ?: "PulseLink User"),
            "payload" to when(message) {
                is PulseLinkMessage.LinkRequest -> mapOf("code" to message.code, "senderName" to message.senderName)
                is PulseLinkMessage.ManualMessage -> mapOf("body" to message.body, "urgency" to message.urgency.name)
                // Add other types as needed for email templates
                else -> mapOf("code" to message.code)
            }
        )

        return runCatching {
            functions.getHttpsCallable("sendEmailNotification").call(data).await()
        }.isSuccess
    }


    @android.annotation.SuppressLint("MissingPermission")
    suspend fun sendLinkRequest(contactId: Long): Boolean {
        val contact = contactRepository.getContact(contactId) ?: return false
        val deviceId = settingsRepository.ensureDeviceId()
        val code = contact.linkCode ?: UUID.randomUUID().toString()
        val updated = contact.copy(
            linkCode = code,
            linkStatus = LinkStatus.OUTBOUND_PENDING,
            pendingApproval = true
        )
        contactRepository.upsert(updated)
        upsertLinkDoc(code)
        val senderName = settingsRepository.settings.first().ownerName.ifBlank { contact.displayName }

        val message = PulseLinkMessage.LinkRequest(deviceId, code, senderName)
        val smsBody = SmsCodec.encodeLinkRequest(deviceId, code, senderName)

        // Use standard fallback, but note that Firebase might fail if remoteDeviceId is unknown.
        // sendMessageWithFallback handles null remoteDeviceId gracefully by skipping Firebase.
        val sent = sendMessageWithFallback(contact, message, smsBody)

        var finalSent = sent
        if (!finalSent && contact.remoteDeviceId.isNullOrBlank()) {
             val phone = contact.primaryPhone()?.takeIf { it.isNotBlank() }
             val email = contact.primaryEmail()?.takeIf { it.isNotBlank() }
             val info = resolveRemoteUser(phone, email)
             if (info != null) {
                 val resolvedContact = updated.copy(
                     remoteUid = info.uid,
                     remoteDeviceId = info.deviceId
                 )
                 contactRepository.upsert(resolvedContact)
                 finalSent = sendMessageWithFallback(resolvedContact, message, smsBody)
             }
        }

        if (!finalSent) {
             // If standard fallback failed (e.g. no phone, or SMS failed), try email explicitly if not covered by fallback loop
             // (Though email IS in the fallback loop if enabled).
             // But sendEmailLinkRequest in original code had specific logic for invites.
             // We should preserve the specific email invite logic if fallback didn't handle it.

             val targetEmail = normalizeEmail(contact.primaryEmail())
             if (targetEmail.isNotBlank()) {
                 sendEmailLinkRequest(code, targetEmail, senderName, updated)
                 return true
             } else {
                 Log.w(TAG, "sendLinkRequest: no channel available for contactId=$contactId")
                 return false
             }
        }
        return finalSent
    }

    private suspend fun sendEmailLinkRequest(
        code: String,
        targetEmail: String,
        senderName: String,
        contact: Contact
    ) {
        val sender = auth.currentUser ?: run {
            Log.w(TAG, "sendEmailLinkRequest: no authenticated user")
            return
        }
        val payload = hashMapOf(
            "code" to code,
            "senderUid" to sender.uid,
            "senderDeviceId" to settingsRepository.ensureDeviceId(),
            "senderName" to senderName,
            "senderEmail" to normalizeEmail(sender.email),
            "targetEmailLowercase" to targetEmail,
            "contactName" to contact.displayName,
            "createdAt" to FieldValue.serverTimestamp()
        )
        runCatching {
            firestore.collection(COLLECTION_EMAIL_INVITES)
                .add(payload)
                .await()
        }.onFailure { error ->
            Log.w(TAG, "Unable to enqueue email-based link invite for $targetEmail", error)
        }
    }

    @android.annotation.SuppressLint("MissingPermission")
    suspend fun approveLink(contactId: Long) {
        val contact = contactRepository.getContact(contactId) ?: return
        val deviceId = settingsRepository.ensureDeviceId()
        val code = contact.linkCode ?: UUID.randomUUID().toString()
        val updated = contact.copy(
            linkCode = code,
            linkStatus = LinkStatus.LINKED,
            pendingApproval = false,
            allowRemoteOverride = if (contact.linkStatus == LinkStatus.LINKED) {
                contact.allowRemoteOverride
            } else {
                true
            }
        )
        contactRepository.upsert(updated)
        upsertLinkDoc(code)

        val targetPhone = contact.primaryPhone()
        if (!targetPhone.isNullOrBlank()) {
            maybeApplyRemoteUid(code, targetPhone)
        }

        val message = PulseLinkMessage.LinkAccept(deviceId, code)
        val smsBody = SmsCodec.encodeLinkAccept(deviceId, code)

        val sent = sendMessageWithFallback(contact, message, smsBody)

        if (!sent) {
            val targetEmail = normalizeEmail(contact.primaryEmail())
            if (targetEmail.isNotBlank()) {
                Log.i(TAG, "approveLink: recorded cloud approval for code=$code (email-only contact)")
            } else {
                Log.w(TAG, "approveLink: failed to send accept to $contactId")
            }
        }
    }

    private fun hasSmsPermission(): Boolean =
        androidx.core.content.ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.SEND_SMS
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED

    suspend fun sendPing(contactId: Long): Boolean {
        val contact = contactRepository.getContact(contactId) ?: return false
        if (contact.linkStatus != LinkStatus.LINKED || contact.linkCode.isNullOrBlank()) return false

        val ready = requestRemotePrepare(
            contact = contact,
            tier = EscalationTier.CHECK_IN,
            reason = PulseLinkMessage.AlertPrepareReason.MESSAGE
        )

        val deviceId = settingsRepository.ensureDeviceId()
        val message = PulseLinkMessage.Ping(deviceId, contact.linkCode!!)
        val smsBody = SmsCodec.encodePing(deviceId, contact.linkCode!!)

        sendMessageWithFallback(contact, message, smsBody)
        return ready
    }

    suspend fun prepareRemoteCall(contactId: Long): CallPreparationResult {
        val contact = contactRepository.getContact(contactId) ?: return CallPreparationResult.FAILED
        if (contact.linkStatus != LinkStatus.LINKED || contact.linkCode.isNullOrBlank()) {
            return CallPreparationResult.FAILED
        }

        val ready = requestRemotePrepare(
            contact = contact,
            tier = EscalationTier.EMERGENCY,
            reason = PulseLinkMessage.AlertPrepareReason.CALL
        )
        return if (ready) CallPreparationResult.READY else CallPreparationResult.TIMEOUT
    }

    suspend fun sendCallEndedNotification(contactId: Long, callDuration: Long) {
        val contact = contactRepository.getContact(contactId) ?: return
        val code = contact.linkCode ?: return
        val deviceId = settingsRepository.ensureDeviceId()

        val message = PulseLinkMessage.CallEnded(deviceId, code, callDuration)
        val smsBody = SmsCodec.encodeCallEnded(deviceId, code, callDuration)

        sendMessageWithFallback(contact, message, smsBody, awaitSmsResult = false)
    }

    suspend fun sendSoundOverride(contactId: Long, tier: EscalationTier, soundKey: String?) {
        val contact = contactRepository.getContact(contactId) ?: return
        if (contact.linkStatus != LinkStatus.LINKED) return
        val code = contact.linkCode ?: return
        val deviceId = settingsRepository.ensureDeviceId()

        val message = PulseLinkMessage.SoundOverride(deviceId, code, tier, soundKey)
        val smsBody = SmsCodec.encodeSoundOverride(deviceId, code, tier, soundKey)

        sendMessageWithFallback(contact, message, smsBody, awaitSmsResult = false)
    }

    suspend fun handleInbound(message: PulseLinkMessage, fromPhone: String) {
        val localDeviceId = settingsRepository.ensureDeviceId()
        if (message.senderId == localDeviceId) {
            Log.d(TAG, "Ignoring inbound message from self (senderId == localDeviceId).")
            return
        }

        when (message) {
            is PulseLinkMessage.LinkRequest -> handleLinkRequest(message, fromPhone)
            is PulseLinkMessage.LinkAccept -> handleLinkAccept(message, fromPhone)
            is PulseLinkMessage.Ping -> handlePing(message)
            is PulseLinkMessage.AlertPrepare -> handleAlertPrepare(message)
            is PulseLinkMessage.AlertReady -> handleAlertReady(message)
            is PulseLinkMessage.RemoteAlert -> handleRemoteAlert(message)
            is PulseLinkMessage.SoundOverride -> handleSoundOverride(message)
            is PulseLinkMessage.ManualMessage -> handleManualMessage(message, fromPhone)
            is PulseLinkMessage.ConfigUpdate -> handleConfigUpdate(message)
            is PulseLinkMessage.CallEnded -> handleCallEnded(message)
        }
    }

    private suspend fun isSenderBlocked(
        phoneNumber: String?,
        linkCode: String?,
        remoteDeviceId: String?
    ): Boolean {
        return blockedContactRepository.isBlocked(phoneNumber, linkCode, remoteDeviceId)
    }

    private suspend fun handleLinkRequest(message: PulseLinkMessage.LinkRequest, fromPhone: String) {
        if (isSenderBlocked(fromPhone, message.code, message.senderId)) {
            Log.i(TAG, "Ignoring link request from blocked sender: $fromPhone")
            return
        }
        val now = System.currentTimeMillis()
        val existing = contactRepository.getByLinkCode(message.code)
            ?: findContactByPhoneFlexible(fromPhone)
        val base = existing ?: Contact(
            displayName = if (message.senderName.isNotBlank()) message.senderName else fromPhone,
            phoneNumber = fromPhone
        )
        val updated = base.copy(
            linkStatus = LinkStatus.INBOUND_REQUEST,
            linkCode = message.code,
            remoteDeviceId = message.senderId,
            pendingApproval = true,
            remoteLastSeen = now,
            remotePresence = presenceFrom(now)
        )
        contactRepository.upsert(updated)
        mirrorContactToCloud(updated)
        upsertLinkDoc(message.code)
        if (fromPhone.isNotBlank()) maybeApplyRemoteUid(message.code, fromPhone)
        val persisted = contactRepository.getByLinkCode(message.code)
            ?: findContactByPhoneFlexible(fromPhone)
            ?: updated
        notifyLinkRequest(persisted)
    }

    private suspend fun handleLinkAccept(message: PulseLinkMessage.LinkAccept, fromPhone: String) {
        if (isSenderBlocked(fromPhone, message.code, message.senderId)) {
            Log.i(TAG, "Ignoring link accept from blocked sender: $fromPhone")
            return
        }
        val now = System.currentTimeMillis()
        val base = contactRepository.getByLinkCode(message.code) ?: findContactByPhoneFlexible(fromPhone)
        val resolved = base ?: Contact(
            displayName = fromPhone.ifBlank { context.getString(R.string.app_name) },
            phoneNumber = fromPhone,
            linkCode = message.code,
            remoteDeviceId = message.senderId,
            linkStatus = LinkStatus.NONE
        )
        val allowOverride = if (resolved.linkStatus == LinkStatus.LINKED) {
            resolved.allowRemoteOverride
        } else {
            true
        }
        val linked = resolved.copy(
            linkStatus = LinkStatus.LINKED,
            remoteDeviceId = message.senderId,
            pendingApproval = false,
            allowRemoteOverride = allowOverride,
            remoteLastSeen = now,
            remotePresence = presenceFrom(now)
        )
        contactRepository.upsert(linked)
        mirrorContactToCloud(linked)
        upsertLinkDoc(message.code)
        if (fromPhone.isNotBlank()) maybeApplyRemoteUid(message.code, fromPhone)
        notifyLinked(linked)
    }

    private suspend fun handlePing(message: PulseLinkMessage.Ping) {
        val contact = contactRepository.getByLinkCode(message.code) ?: return
        markPresence(contact)
        val title = context.getString(R.string.ping_received_title, contact.displayName)
        val body = context.getString(R.string.ping_received_body)
        remoteActionHandler.playAttentionTone(
            contact = contact,
            tier = EscalationTier.CHECK_IN,
            title = title,
            body = body,
            notificationId = (contact.id.hashCode() and 0xFFFF) + 2000
        )
    }

    private suspend fun handleRemoteAlert(message: PulseLinkMessage.RemoteAlert) {
        if (!shouldProcessRemoteAlert(message.senderId, message.code)) {
            Log.d(TAG, "Ignoring duplicate remote alert sender=${message.senderId} code=${message.code}")
            return
        }
        val contact = contactRepository.getByLinkCode(message.code) ?: return
        markPresence(contact)
        val settings = settingsRepository.settings.first()
        val resolvedSoundKey = when (message.tier) {
            EscalationTier.EMERGENCY -> contact.emergencySoundKey ?: settings.emergencyProfile.soundKey
            EscalationTier.CHECK_IN -> contact.checkInSoundKey ?: settings.checkInProfile.soundKey
        }
        val overrideResult = remoteActionHandler.prepareForAlert(contact)
        if (!overrideResult.success) {
            Log.w(
                TAG,
                "Remote emergency override limited for ${contact.displayName} reason=${overrideResult.reason} message=${overrideResult.message}"
            )
        }
        remoteActionHandler.routeRemoteAlert(contact, message.tier, setOf(contact.id))
        if (message.tier == EscalationTier.EMERGENCY) {
            val title = context.getString(R.string.emergency_alert_title, contact.displayName)
            val body = context.getString(R.string.emergency_alert_body)
            remoteActionHandler.playAttentionTone(
                contact = contact,
                tier = EscalationTier.EMERGENCY,
                title = title,
                body = body,
                notificationId = (contact.id.hashCode() and 0xFFFF) + 5000,
                forceBypass = true
            )
            remoteActionHandler.showEmergencyPopup(contact, message.tier)

        }
        if (message.tier == EscalationTier.EMERGENCY) {
            settingsRepository.setEmergencyActive(true)
            widgetStateManager.requestWidgetUpdate()
        }
        alertRepository.record(
            AlertEvent(
                timestamp = System.currentTimeMillis(),
                triggeredBy = "Remote alert from ${contact.displayName}",
                tier = message.tier,
                contactCount = 0,
                sentSms = false,
                sharedLocation = false,
                contactId = contact.id,
                contactName = contact.displayName,
                isIncoming = true,
                soundKey = resolvedSoundKey
            )
        )
    }

    private suspend fun handleAlertPrepare(message: PulseLinkMessage.AlertPrepare) {
        val contact = contactRepository.getByLinkCode(message.code) ?: return
        markPresence(contact)
        val overrideResult = remoteActionHandler.prepareForAlert(contact, message.reason)
        val overrideApplied = overrideResult.state != AudioOverrideManager.OverrideResult.State.FAILURE &&
            overrideResult.state != AudioOverrideManager.OverrideResult.State.SKIPPED
        if (!overrideApplied) {
            Log.w(
                TAG,
                "Unable to apply remote override for contact ${contact.displayName} reason=${overrideResult.reason} message=${overrideResult.message}"
            )
        }
        val deviceId = settingsRepository.ensureDeviceId()
        val response = PulseLinkMessage.AlertReady(deviceId, message.code, overrideApplied)
        val smsBody = SmsCodec.encodeAlertReady(deviceId, message.code, overrideApplied)

        sendMessageWithFallback(contact, response, smsBody, awaitSmsResult = false)

        if (message.reason == PulseLinkMessage.AlertPrepareReason.CALL) {
            remoteActionHandler.notifyIncomingCall(contact, message.tier)
        }
    }

    private fun handleAlertReady(message: PulseLinkMessage.AlertReady) {
        alertHandshake.remove(message.code)?.complete(message.ready)
    }

    private fun shouldProcessRemoteAlert(senderId: String, code: String): Boolean {
        val now = System.currentTimeMillis()
        val key = "$senderId|$code"
        val last = remoteAlertDedup[key]
        if (last != null && now - last < REMOTE_ALERT_DEDUP_WINDOW_MS) {
            return false
        }
        remoteAlertDedup[key] = now
        if (remoteAlertDedup.size > REMOTE_ALERT_DEDUP_MAX) {
            val cutoff = now - REMOTE_ALERT_DEDUP_WINDOW_MS
            remoteAlertDedup.entries.removeIf { it.value < cutoff }
        }
        return true
    }

    private suspend fun handleSoundOverride(message: PulseLinkMessage.SoundOverride) {
        val contact = contactRepository.getByLinkCode(message.code) ?: return
        val freshContact = markPresence(contact)
        if (!freshContact.allowRemoteSoundChange) return
        val updated = when (message.tier) {
            EscalationTier.EMERGENCY -> freshContact.copy(emergencySoundKey = message.soundKey)
            EscalationTier.CHECK_IN -> freshContact.copy(checkInSoundKey = message.soundKey)
        }
        contactRepository.upsert(updated)
        Log.d(TAG, "Applied remote sound override for ${freshContact.displayName} tier=${message.tier} key=${message.soundKey}")
    }

    private suspend fun handleManualMessage(message: PulseLinkMessage.ManualMessage, fromPhone: String) {
        try {
            val persisted = resolveContactForManualMessage(message, fromPhone) ?: return
            markPresence(persisted)
            deliverManualMessage(
                contact = persisted,
                rawBody = message.body,
                overrideApplied = true,
                urgency = message.urgency,
                volumeHint = message.volumeHint
            )
            if (fromPhone.isNotBlank()) maybeApplyRemoteUid(message.code, fromPhone)
        } catch (error: Exception) {
            Log.e(TAG, "Failed to process manual message from $fromPhone", error)
        }
    }

    private suspend fun handleRealtimeMessage(payload: LinkChannelPayload) {
        try {
            val message = convertPayloadToMessage(payload) ?: return

            // Find contact to process. Note: payload has phoneNumber if available.
            // Also message.code might be useful.
            val phoneNumber = payload.payload["phoneNumber"] as? String ?: ""

            handleInbound(message, phoneNumber)

        } catch (error: Exception) {
            Log.e(TAG, "Failed to process realtime message ${payload.id}", error)
        }
    }

    private fun convertPayloadToMessage(payload: LinkChannelPayload): PulseLinkMessage? {
        val map = payload.payload
        val linkCode = map["linkCode"] as? String ?: return null
        val senderId = payload.senderId

        // Helper to check map types
        fun getStr(k: String) = map[k] as? String

        return when (payload.type) {
            "link_request" -> PulseLinkMessage.LinkRequest(senderId, linkCode, getStr("senderName").orEmpty())
            "link_accept" -> PulseLinkMessage.LinkAccept(senderId, linkCode)
            "ping" -> PulseLinkMessage.Ping(senderId, linkCode)
            "alert" -> {
                val tier = runCatching { EscalationTier.valueOf(getStr("tier").orEmpty()) }.getOrNull() ?: return null
                PulseLinkMessage.RemoteAlert(senderId, linkCode, tier)
            }
            "alert_prepare" -> {
                val tier = runCatching { EscalationTier.valueOf(getStr("tier").orEmpty()) }.getOrNull() ?: return null
                val reason = runCatching { PulseLinkMessage.AlertPrepareReason.valueOf(getStr("reason").orEmpty()) }.getOrDefault(PulseLinkMessage.AlertPrepareReason.ALERT)
                PulseLinkMessage.AlertPrepare(senderId, linkCode, tier, reason)
            }
            "alert_ready" -> {
                PulseLinkMessage.AlertReady(senderId, linkCode, map["ready"] == true)
            }
            "sound_override" -> {
                val tier = runCatching { EscalationTier.valueOf(getStr("tier").orEmpty()) }.getOrNull() ?: return null
                PulseLinkMessage.SoundOverride(senderId, linkCode, tier, getStr("soundKey"))
            }
            "manual" -> {
                val urgency = runCatching { com.pulselink.domain.model.MessageUrgency.valueOf(getStr("urgency").orEmpty()) }.getOrDefault(com.pulselink.domain.model.MessageUrgency.STANDARD)
                val volume = getStr("volumeHint")?.let { runCatching { com.pulselink.domain.model.VolumeHint.valueOf(it) }.getOrNull() }
                PulseLinkMessage.ManualMessage(senderId, linkCode, getStr("body").orEmpty(), urgency, volume)
            }
            "config" -> PulseLinkMessage.ConfigUpdate(senderId, linkCode, getStr("key").orEmpty(), getStr("value").orEmpty())
            "call_ended" -> PulseLinkMessage.CallEnded(senderId, linkCode, (map["callDuration"] as? Number)?.toLong() ?: 0L)
            else -> null
        }
    }

    private suspend fun upsertLinkDoc(code: String) {
        val uid = auth.currentUser?.uid ?: return
        val phone = auth.currentUser?.phoneNumber
        val updates = buildMap<String, Any> {
            put("uids", FieldValue.arrayUnion(uid))
            put("lastSeen.$uid", FieldValue.serverTimestamp())
            if (!phone.isNullOrBlank()) {
                put("phones.$uid", phone)
            }
        }
        runCatching {
            firestore.collection(COLLECTION_LINKS)
                .document(code)
                .set(updates, SetOptions.merge())
                .await()
        }.onFailure { error ->
            Log.w(TAG, "Unable to upsert link doc for $code", error)
        }
    }

    private suspend fun maybeApplyRemoteUid(code: String, phone: String) {
        val uid = auth.currentUser?.uid ?: return
        runCatching {
            val snapshot = firestore.collection(COLLECTION_LINKS).document(code).get().await()
            val uids = snapshot.get("uids") as? List<*>
            val remoteUid = uids?.mapNotNull { it as? String }?.firstOrNull { it != uid }
            val lastSeenMap = snapshot.get("lastSeen") as? Map<*, *>
            val remoteLastSeen = (lastSeenMap?.get(remoteUid) as? Timestamp)?.toDate()?.time
            val presence = presenceFromNullable(remoteLastSeen)
            if (!remoteUid.isNullOrBlank()) {
                val contact = contactRepository.getByLinkCode(code)
                    ?: contactRepository.getByPhone(phone)
                if (contact != null &&
                    (contact.remoteUid != remoteUid ||
                        contact.remoteLastSeen != remoteLastSeen ||
                        contact.remotePresence != presence)
                ) {
                    contactRepository.upsert(
                        contact.copy(
                            remoteUid = remoteUid,
                            remoteLastSeen = remoteLastSeen,
                            remotePresence = presence,
                            linkStatus = LinkStatus.LINKED
                        )
                    )
                }
            }
        }.onFailure { error ->
            Log.w(TAG, "Unable to resolve remoteUid for link $code", error)
        }
    }

    private suspend fun deliverManualMessage(
        contact: Contact,
        rawBody: String,
        overrideApplied: Boolean,
        urgency: com.pulselink.domain.model.MessageUrgency,
        volumeHint: com.pulselink.domain.model.VolumeHint?
    ) {
        val body = rawBody.ifBlank { context.getString(R.string.ping_received_body) }
        val title = context.getString(R.string.manual_message_title, contact.displayName)
        if (isAutoAlertBody(rawBody)) {
            withContext(Dispatchers.IO) {
            messageRepository.record(
                ContactMessage(
                    contactId = contact.id,
                    body = body,
                    direction = MessageDirection.INBOUND,
                    overrideSucceeded = overrideApplied,
                    status = MessageStatus.READ
                )
            )
        }
            return
        }
        val tier = when (urgency) {
            com.pulselink.domain.model.MessageUrgency.EMERGENCY,
            com.pulselink.domain.model.MessageUrgency.URGENT -> EscalationTier.EMERGENCY
            com.pulselink.domain.model.MessageUrgency.STANDARD -> EscalationTier.CHECK_IN
        }
        remoteActionHandler.playAttentionTone(
            contact = contact,
            tier = tier,
            title = title,
            body = body,
            notificationId = (contact.id.hashCode() and 0xFFFF) + 3000,
            forceBypass = true,
            volumeHint = volumeHint
        )
        withContext(Dispatchers.IO) {
            messageRepository.record(
                ContactMessage(
                    contactId = contact.id,
                    body = body,
                    direction = MessageDirection.INBOUND,
                    overrideSucceeded = overrideApplied,
                    status = MessageStatus.READ
                )
            )
            if (contact.escalationTier == EscalationTier.EMERGENCY) {
                settingsRepository.setEmergencyActive(true)
                widgetStateManager.requestWidgetUpdate()
            }
        }
    }

    private fun isAutoAlertBody(body: String): Boolean =
        body.startsWith("PulseLink EMERGENCY") || body.startsWith("PulseLink CHECK-IN")

    suspend fun cancelActiveEmergency(): Boolean = withContext(Dispatchers.IO) {
        val contacts = contactRepository.getEmergencyContacts()
        if (contacts.isEmpty()) return@withContext false
        val deviceId = settingsRepository.ensureDeviceId()
        val body = context.getString(R.string.cancel_emergency_sms_body)
        var sentAny = false
        contacts.forEach { contact ->
            val code = contact.linkCode ?: return@forEach
            val message = PulseLinkMessage.ManualMessage(
                deviceId,
                code,
                body,
                com.pulselink.domain.model.MessageUrgency.STANDARD,
                null
            )
            val smsBody = SmsCodec.encodeManualMessage(
                deviceId,
                code,
                body,
                com.pulselink.domain.model.MessageUrgency.STANDARD,
                null
            )

            if (sendMessageWithFallback(contact, message, smsBody)) {
                sentAny = true
                messageRepository.record(
                    ContactMessage(
                        contactId = contact.id,
                        body = body,
                        direction = MessageDirection.OUTBOUND,
                        overrideSucceeded = false,
                        status = MessageStatus.SENT
                    )
                )
            }
        }
        sentAny
    }

    private suspend fun handleCallEnded(message: PulseLinkMessage.CallEnded) {
        val contact = contactRepository.getByLinkCode(message.code) ?: return
        markPresence(contact)
        remoteActionHandler.finishCall(contact, message.callDuration)
    }

    private suspend fun resolveContactForManualMessage(
        message: PulseLinkMessage.ManualMessage,
        fromPhone: String
    ): Contact? = withContext(Dispatchers.IO) {
        if (isSenderBlocked(fromPhone, message.code, message.senderId)) {
            val displayName = when {
                fromPhone.isNotBlank() -> fromPhone
                message.senderId.isNotBlank() -> message.senderId
                else -> context.getString(R.string.app_name)
            }
            notifyBlockedAttempt(displayName)
            return@withContext null
        }
        val initial = contactRepository.getByLinkCode(message.code)
            ?: findContactByPhoneFlexible(fromPhone)
            ?: run {
                if (fromPhone.isBlank() && message.code.isBlank()) return@withContext null
                val placeholder = Contact(
                    displayName = if (fromPhone.isNotBlank()) fromPhone else message.senderId,
                    phoneNumber = fromPhone,
                    linkCode = message.code.takeIf { it.isNotBlank() },
                    remoteDeviceId = message.senderId,
                    linkStatus = if (message.code.isNotBlank()) LinkStatus.INBOUND_REQUEST else LinkStatus.NONE,
                    pendingApproval = message.code.isNotBlank()
                )
                contactRepository.upsert(placeholder)
                val byCode = message.code.takeIf { it.isNotBlank() }?.let { code ->
                    contactRepository.getByLinkCode(code)
                }
                byCode ?: findContactByPhoneFlexible(fromPhone)
            }
            ?: return@withContext null
        val resolved = initial.resolveLinkState(message)
        if (resolved !== initial) {
            contactRepository.upsert(resolved)
            contactRepository.getContact(resolved.id) ?: resolved
        } else {
            resolved
        }
    }

    private suspend fun handleConfigUpdate(message: PulseLinkMessage.ConfigUpdate) {
        val contact = contactRepository.getByLinkCode(message.code) ?: return
        val freshContact = markPresence(contact)
        val settings = settingsRepository.settings.first()
        if (!settings.autoUpdateContactInfo) return
        when (message.key) {
            CONFIG_REMOTE_SOUND -> {
                val allow = message.value == "1"
                contactRepository.upsert(freshContact.copy(allowRemoteSoundChange = allow))
            }
            CONFIG_REMOTE_OVERRIDE -> {
                val allow = message.value == "1"
                contactRepository.upsert(freshContact.copy(allowRemoteOverride = allow))
            }
            CONFIG_PHONE_UPDATE -> {
                if (message.value.isNotBlank()) {
                    contactRepository.upsert(freshContact.copy(phoneNumber = message.value))
                }
            }
            CONFIG_EMAIL_UPDATE -> {
                contactRepository.upsert(freshContact.copy(email = message.value.ifBlank { null }))
            }
        }
    }

    private fun notifyLinkRequest(contact: Contact) {
        ensureChannel()
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle(context.getString(R.string.link_request_title))
            .setContentText(context.getString(R.string.link_request_body, contact.displayName))
            .setSmallIcon(R.drawable.ic_logo)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()
        notificationManager.notify(contact.id.toInt(), notification)
    }

    private fun notifyLinked(contact: Contact) {
        ensureChannel()
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle(context.getString(R.string.link_success_title))
            .setContentText(context.getString(R.string.link_success_body, contact.displayName))
            .setSmallIcon(R.drawable.ic_logo)
            .setAutoCancel(true)
            .build()
        notificationManager.notify((contact.id.hashCode() and 0xFFFF) + 1000, notification)
    }

    private fun notifyBlockedAttempt(name: String) {
        ensureChannel()
        val safeName = name.ifBlank { context.getString(R.string.app_name) }
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle(context.getString(R.string.blocked_contact_attempt_title))
            .setContentText(context.getString(R.string.blocked_contact_attempt_body, safeName))
            .setSmallIcon(R.drawable.ic_logo)
            .setAutoCancel(true)
            .build()
        notificationManager.notify((safeName.hashCode() and 0xFFFF) + 4000, notification)
    }

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.link_notification_channel),
            NotificationManager.IMPORTANCE_HIGH
        )
        nm.createNotificationChannel(channel)
    }

    suspend fun updateRemoteSoundPermission(contactId: Long, allow: Boolean) {
        val contact = contactRepository.getContact(contactId) ?: return
        val updated = contact.copy(allowRemoteSoundChange = allow)
        contactRepository.upsert(updated)
        if (contact.linkStatus == LinkStatus.LINKED && !contact.linkCode.isNullOrBlank()) {
            val deviceId = settingsRepository.ensureDeviceId()
            val message = PulseLinkMessage.ConfigUpdate(deviceId, contact.linkCode!!, CONFIG_REMOTE_SOUND, if (allow) "1" else "0")
            val smsBody = SmsCodec.encodeConfig(deviceId, contact.linkCode!!, CONFIG_REMOTE_SOUND, if (allow) "1" else "0")
            sendMessageWithFallback(contact, message, smsBody)
        }
    }

    suspend fun updateRemoteOverridePermission(contactId: Long, allow: Boolean) {
        val contact = contactRepository.getContact(contactId) ?: return
        val updated = contact.copy(allowRemoteOverride = allow)
        contactRepository.upsert(updated)
        if (contact.linkStatus == LinkStatus.LINKED && !contact.linkCode.isNullOrBlank()) {
            val deviceId = settingsRepository.ensureDeviceId()
            val message = PulseLinkMessage.ConfigUpdate(deviceId, contact.linkCode!!, CONFIG_REMOTE_OVERRIDE, if (allow) "1" else "0")
            val smsBody = SmsCodec.encodeConfig(deviceId, contact.linkCode!!, CONFIG_REMOTE_OVERRIDE, if (allow) "1" else "0")
            sendMessageWithFallback(contact, message, smsBody)
        }
    }

    suspend fun broadcastProfileUpdate(): Result<Int> = runCatching {
        val phone = auth.currentUser?.phoneNumber
        val email = auth.currentUser?.email
        val deviceId = settingsRepository.ensureDeviceId()
        val linkedContacts = contactRepository.getLinkedContacts()
        var sent = 0
        linkedContacts.forEach { contact ->
            val code = contact.linkCode ?: return@forEach
            phone?.let {
                contact.primaryPhone()?.let { target ->
                    val message = PulseLinkMessage.ConfigUpdate(deviceId, code, CONFIG_PHONE_UPDATE, it)
                    val smsBody = SmsCodec.encodeConfig(deviceId, code, CONFIG_PHONE_UPDATE, it)
                    if (sendMessageWithFallback(contact, message, smsBody)) sent++
                }
            }
            email?.let {
                contact.primaryPhone()?.let { target ->
                    val message = PulseLinkMessage.ConfigUpdate(deviceId, code, CONFIG_EMAIL_UPDATE, it)
                    val smsBody = SmsCodec.encodeConfig(deviceId, code, CONFIG_EMAIL_UPDATE, it)
                    if (sendMessageWithFallback(contact, message, smsBody)) sent++
                }
            }
        }
        sent
    }

    suspend fun hydrateContactFromEmail(contactId: Long) {
        val contact = contactRepository.getContact(contactId) ?: return
        if (!contact.email.isNullOrBlank() && contact.remoteDeviceId.isNullOrBlank()) {
            maybeResolveRemoteIdentity(contact)
        }
    }

    suspend fun prepareRemoteOverride(contactId: Long, tier: EscalationTier): Boolean {
        val contact = contactRepository.getContact(contactId) ?: return false
        if (contact.linkStatus != LinkStatus.LINKED || contact.linkCode.isNullOrBlank()) return false
        return requestRemotePrepare(contact, tier)
    }

    suspend fun sendManualMessage(
        contactId: Long,
        message: String,
        urgency: com.pulselink.domain.model.MessageUrgency = com.pulselink.domain.model.MessageUrgency.STANDARD,
        volumeHint: com.pulselink.domain.model.VolumeHint? = null
    ): ManualMessageResult {
        Log.d(TAG, "sendManualMessage: START for contactId=$contactId")
        var contact = contactRepository.getContact(contactId)
            ?: return ManualMessageResult.Failure(ManualMessageResult.Failure.Reason.CONTACT_MISSING)

        if ((contact.remoteDeviceId.isNullOrBlank() || contact.linkStatus != LinkStatus.LINKED) &&
            !contact.email.isNullOrBlank()
        ) {
            maybeResolveRemoteIdentity(contact)?.let { resolved ->
                contact = resolved
            }
        }

        // Just use sendMessageWithFallback.
        val deviceId = settingsRepository.ensureDeviceId()
        if (contact.linkCode.isNullOrBlank()) {
             Log.w(TAG, "sendManualMessage: FAILED. Reason: NOT_LINKED for contactId=$contactId")
             return ManualMessageResult.Failure(ManualMessageResult.Failure.Reason.NOT_LINKED)
        }

        val plMessage = PulseLinkMessage.ManualMessage(
            deviceId,
            contact.linkCode!!,
            message,
            urgency,
            volumeHint
        )
        val smsBody = SmsCodec.encodeManualMessage(
            deviceId,
            contact.linkCode!!,
            message,
            urgency,
            volumeHint
        )

        val ready = if (contact.linkStatus == LinkStatus.LINKED) {
            requestRemotePrepare(
                contact,
                EscalationTier.CHECK_IN,
                reason = PulseLinkMessage.AlertPrepareReason.MESSAGE
            )
        } else false

        val success = sendMessageWithFallback(contact, plMessage, smsBody)

        if (success) {
            Log.d(TAG, "Recording outbound message for contact=$contactId overrideReady=$ready")
             messageRepository.record(
                ContactMessage(
                    contactId = contact.id,
                    body = message,
                    direction = MessageDirection.OUTBOUND,
                    overrideSucceeded = ready,
                    status = MessageStatus.SENT
                )
            )
            return ManualMessageResult.Success(overrideApplied = ready)
        } else {
            return ManualMessageResult.Failure(ManualMessageResult.Failure.Reason.SMS_FAILED) // Use generalized failure?
        }
    }

    private suspend fun requestRemotePrepare(
        contact: Contact,
        tier: EscalationTier,
        reason: PulseLinkMessage.AlertPrepareReason = PulseLinkMessage.AlertPrepareReason.ALERT
    ): Boolean {
        if (!contact.allowRemoteOverride && reason == PulseLinkMessage.AlertPrepareReason.MESSAGE) return true
        val code = contact.linkCode ?: return false
        alertHandshake.remove(code)?.cancel()
        val deviceId = settingsRepository.ensureDeviceId()
        val deferred = CompletableDeferred<Boolean>()
        alertHandshake[code] = deferred

        val message = PulseLinkMessage.AlertPrepare(deviceId, code, tier, reason)
        val smsBody = SmsCodec.encodeAlertPrepare(deviceId, code, tier, reason)

        sendMessageWithFallback(contact, message, smsBody)

        val ready = withTimeoutOrNull(PREPARE_TIMEOUT_MS) { deferred.await() } ?: false
        alertHandshake.remove(code)
        if (!ready) {
            Log.w(TAG, "Remote contact did not acknowledge alert preparation for code $code (reason=$reason)")
        }
        return ready
    }

    fun startIncomingMonitoring() {
        if (incomingMonitorActive) return
        runCatching {
            callStateMonitor.monitorIncomingCalls(
                onRinging = { phone ->
                    monitorScope.launch { handleIncomingRinging(phone) }
                },
                onCallFinished = {
                    monitorScope.launch { remoteActionHandler.stopIncomingCallTone() }
                }
            )
            incomingMonitorActive = true
        }.onFailure { error ->
            incomingMonitorActive = false
            Log.w(TAG, "Unable to monitor incoming calls", error)
        }
    }

    fun stopIncomingMonitoring() {
        if (!incomingMonitorActive) return
        incomingMonitorActive = false
        callStateMonitor.stopIncomingMonitoring()
        monitorScope.coroutineContext.cancelChildren()
        remoteActionHandler.stopIncomingCallTone()
    }

    private suspend fun handleIncomingRinging(phone: String?) {
        val resolvedNumber = phone?.takeIf { it.isNotBlank() } ?: latestIncomingNumber()
        if (resolvedNumber.isNullOrBlank()) return
        val contact = findContactByPhoneFlexible(resolvedNumber) ?: return
        if (contact.linkStatus != LinkStatus.LINKED) return
        try {
            remoteActionHandler.handleIncomingCall(contact)
        } catch (error: Exception) {
            Log.e(TAG, "Failed to process incoming call for ${contact.displayName}", error)
        }
    }

    private fun latestIncomingNumber(): String? {
        return runCatching {
            context.contentResolver.query(
                CallLog.Calls.CONTENT_URI,
                arrayOf(CallLog.Calls.NUMBER, CallLog.Calls.TYPE),
                "${CallLog.Calls.TYPE} = ?",
                arrayOf(CallLog.Calls.INCOMING_TYPE.toString()),
                "${CallLog.Calls.DATE} DESC"
            )?.use { cursor ->
                if (cursor.moveToFirst()) cursor.getString(0) else null
            }
        }.getOrNull()
    }

    private suspend fun findContactByPhoneFlexible(phone: String): Contact? {
        contactRepository.getByPhone(phone)?.let { return it }
        val normalizedIncoming = normalizePhone(phone)
        if (normalizedIncoming.isEmpty()) return null
        return contactRepository.observeContacts().first().firstOrNull { existing ->
            (listOf(existing.phoneNumber) + existing.additionalPhones)
                .map { normalizePhone(it) }
                .any { it == normalizedIncoming }
        }
    }

    suspend fun triggerRemoteAlert(contact: Contact, tier: EscalationTier): RemoteAlertResult {
        if (contact.linkStatus != LinkStatus.LINKED || contact.linkCode.isNullOrBlank()) {
            return RemoteAlertResult(contact.id, contact.displayName, RemoteAlertStatus.NOT_LINKED, tier)
        }
        val deviceId = settingsRepository.ensureDeviceId()
        val code = contact.linkCode!!

        // Prepare
        val prepareMessage = PulseLinkMessage.AlertPrepare(
            deviceId,
            code,
            tier,
            PulseLinkMessage.AlertPrepareReason.ALERT
        )
        val prepareSms = SmsCodec.encodeAlertPrepare(
            deviceId,
            code,
            tier,
            PulseLinkMessage.AlertPrepareReason.ALERT
        )

        sendMessageWithFallback(contact, prepareMessage, prepareSms, awaitSmsResult = false)

        // Alert
        val alertMessage = PulseLinkMessage.RemoteAlert(deviceId, code, tier)
        val alertSms = SmsCodec.encodeRemoteAlert(deviceId, code, tier)

        val alertSent = sendMessageWithFallback(contact, alertMessage, alertSms, awaitSmsResult = false)

        return if (alertSent) {
            RemoteAlertResult(contact.id, contact.displayName, RemoteAlertStatus.SUCCESS, tier)
        } else {
            RemoteAlertResult(contact.id, contact.displayName, RemoteAlertStatus.SMS_FAILED, tier)
        }
    }

    suspend fun syncLinksOnLogin() {
        fetchEmailInvitesForCurrentUser()
        val uid = auth.currentUser?.uid ?: return
        val phone = auth.currentUser?.phoneNumber
        val linkCollection = firestore.collection(COLLECTION_LINKS)
        val snapshot = runCatching { linkCollection.whereArrayContains("uids", uid).get().await() }
            .getOrElse { error ->
                Log.w(TAG, "Unable to fetch link docs for presence sync", error)
                return
            }
        snapshot.documents.forEach { doc ->
            val code = doc.id
            val updates = buildMap<String, Any> {
                put("uids", FieldValue.arrayUnion(uid))
                put("lastSeen.$uid", FieldValue.serverTimestamp())
                if (!phone.isNullOrBlank()) put("phones.$uid", phone)
            }
            runCatching {
                linkCollection.document(code).set(updates, SetOptions.merge()).await()
            }.onFailure { error ->
                Log.w(TAG, "Unable to update presence for link $code", error)
            }

            val uids = (doc.get("uids") as? List<*>)?.mapNotNull { it as? String }.orEmpty()
            val remoteUid = uids.firstOrNull { it != uid }
            val phones = doc.get("phones") as? Map<*, *>
            val remotePhone = remoteUid?.let { ru -> phones?.get(ru) as? String }
            val lastSeenMap = doc.get("lastSeen") as? Map<*, *>
            val remoteLastSeen = remoteUid?.let { ru ->
                (lastSeenMap?.get(ru) as? Timestamp)?.toDate()?.time
            }
            val presence = presenceFromNullable(remoteLastSeen)

            if (remoteUid != null) {
                val contact = contactRepository.getByRemoteUid(remoteUid)
                    ?: contactRepository.getByLinkCode(code)
                    ?: remotePhone?.let { contactRepository.getByPhone(it) }
                contact?.let {
                    val updated = it.copy(
                        remoteUid = remoteUid,
                        linkStatus = LinkStatus.LINKED,
                        linkCode = it.linkCode ?: code,
                        phoneNumber = if (!remotePhone.isNullOrBlank()) remotePhone else it.phoneNumber,
                        remoteLastSeen = remoteLastSeen,
                        remotePresence = presence
                    )
                    contactRepository.upsert(updated)
                }
            }
        }
    }

    private fun presenceFrom(lastSeenMillis: Long): RemotePresence {
        val age = System.currentTimeMillis() - lastSeenMillis
        return when {
            age < 3 * 60 * 1000L -> RemotePresence.ONLINE
            age < 60 * 60 * 1000L -> RemotePresence.RECENT
            age < 24 * 60 * 60 * 1000L -> RemotePresence.OFFLINE
            else -> RemotePresence.STALE
        }
    }

    private fun presenceFromNullable(lastSeenMillis: Long?): RemotePresence {
        return lastSeenMillis?.let { presenceFrom(it) } ?: RemotePresence.STALE
    }

    private suspend fun fetchEmailInvitesForCurrentUser() {
        val email = normalizeEmail(auth.currentUser?.email)
        if (email.isBlank()) return
        val snapshot = runCatching {
            firestore.collection(COLLECTION_EMAIL_INVITES)
                .whereEqualTo("targetEmailLowercase", email)
                .get()
                .await()
        }.getOrElse { error ->
            Log.w(TAG, "Unable to fetch email-based link invites", error)
            return
        }
        if (snapshot.isEmpty) return

        val existing = contactRepository.observeContacts().first()
        var nextOrder = (existing.maxOfOrNull { it.contactOrder } ?: -1) + 1

        snapshot.documents.forEach { doc ->
            val code = doc.getString("code").orEmpty().ifBlank { UUID.randomUUID().toString() }
            val senderName = doc.getString("senderName").orEmpty()
            val senderEmail = normalizeEmail(doc.getString("senderEmail"))
            val senderDeviceId = doc.getString("senderDeviceId")
            val senderUid = doc.getString("senderUid")
            val base = contactRepository.getByLinkCode(code)
                ?: contactRepository.getByEmail(senderEmail)
                ?: Contact(
                    displayName = senderName.ifBlank { senderEmail.ifBlank { context.getString(R.string.app_name) } },
                    email = senderEmail.takeIf { it.isNotBlank() },
                    contactOrder = nextOrder++
                )
            val updated = base.copy(
                linkStatus = LinkStatus.INBOUND_REQUEST,
                linkCode = code,
                pendingApproval = true,
                remoteDeviceId = senderDeviceId ?: base.remoteDeviceId,
                remoteUid = senderUid ?: base.remoteUid,
                remotePresence = base.remotePresence.takeIf { it != RemotePresence.UNKNOWN }
                    ?: RemotePresence.RECENT
            )
            contactRepository.upsert(updated)
            upsertLinkDoc(code)
            runCatching { firestore.collection(COLLECTION_EMAIL_INVITES).document(doc.id).delete().await() }
                .onFailure { error -> Log.w(TAG, "Unable to clear processed email invite ${doc.id}", error) }
        }
    }

    private suspend fun markPresence(contact: Contact, observedAt: Long = System.currentTimeMillis()): Contact {
        val latest = maxOf(contact.remoteLastSeen ?: 0L, observedAt)
        val presence = presenceFrom(latest)
        if (contact.remoteLastSeen == latest && contact.remotePresence == presence) return contact
        val updated = contact.copy(
            remoteLastSeen = latest,
            remotePresence = presence
        )
        contactRepository.upsert(updated)
        return updated
    }

    private suspend fun maybeResolveRemoteIdentity(contact: Contact): Contact? {
        val email = normalizeEmail(contact.primaryEmail()).takeIf { it.isNotBlank() } ?: return null
        val info = resolveRemoteUser(null, email) ?: return null

        val updated = contact.copy(
            remoteUid = info.uid,
            remoteDeviceId = info.deviceId,
            linkStatus = LinkStatus.LINKED,
            pendingApproval = false
        )
        contactRepository.upsert(updated)
        mirrorContactToCloud(updated)
        return updated
    }

    private suspend fun mirrorContactToCloud(contact: Contact) {
        val user = auth.currentUser ?: return
        if (user.isAnonymous) return
        val docId = contactDocId(contact)
        val payload = mapOf(
            "displayName" to contact.displayName,
            "phoneNumber" to contact.phoneNumber,
            "email" to contact.email,
            "additionalPhones" to contact.additionalPhones,
            "additionalEmails" to contact.additionalEmails,
            "escalationTier" to contact.escalationTier.name,
            "includeLocation" to contact.includeLocation,
            "autoCall" to contact.autoCall,
            "emergencySoundKey" to contact.emergencySoundKey,
            "checkInSoundKey" to contact.checkInSoundKey,
            "contactOrder" to contact.contactOrder,
            "allowRemoteSoundChange" to contact.allowRemoteSoundChange,
            "allowRemoteOverride" to contact.allowRemoteOverride,
            "linkStatus" to contact.linkStatus.name,
            "linkCode" to contact.linkCode,
            "remoteDeviceId" to contact.remoteDeviceId,
            "pendingApproval" to contact.pendingApproval,
            "remoteUid" to contact.remoteUid,
            "updatedAt" to FieldValue.serverTimestamp()
        )
        runCatching {
            firestore.collection("users").document(user.uid)
                .collection("trustedContacts")
                .document(docId)
                .set(payload, SetOptions.merge())
                .await()
        }.onFailure { error ->
            Log.w(TAG, "Unable to mirror contact to cloud", error)
        }
    }

    private suspend fun resolveRemoteUser(phone: String?, email: String?): ContactLinkInfo? {
        if (phone.isNullOrBlank() && email.isNullOrBlank()) return null
        return runCatching {
            val data = hashMapOf(
                "phoneNumber" to phone,
                "email" to email
            )
            val result = functions.getHttpsCallable("findUser").call(data).await()
            val map = result.data as? Map<String, Any> ?: return@runCatching null
            if (map["found"] != true) return@runCatching null

            ContactLinkInfo(
                uid = map["uid"] as String,
                deviceId = map["deviceId"] as String,
                displayName = map["displayName"] as? String,
                avatarUrl = map["avatarUrl"] as? String
            )
        }.getOrNull()
    }

    private data class ContactLinkInfo(
        val uid: String,
        val deviceId: String,
        val displayName: String?,
        val avatarUrl: String?
    )

    companion object {
        private const val TAG = "ContactLinkManager"
        private const val CHANNEL_ID = "pulselink_link_channel"
        const val CONFIG_REMOTE_SOUND = "ALLOW_SOUND"
        const val CONFIG_REMOTE_OVERRIDE = "ALLOW_OVERRIDE"
        const val CONFIG_PHONE_UPDATE = "PHONE"
        const val CONFIG_EMAIL_UPDATE = "EMAIL"
        private const val PREPARE_TIMEOUT_MS = 10_000L
        const val COLLECTION_LINKS = "links"
        const val COLLECTION_EMAIL_INVITES = "linkEmailInvites"
        private const val REMOTE_ALERT_DEDUP_WINDOW_MS = 15_000L
        private const val REMOTE_ALERT_DEDUP_MAX = 50
    }
}

private fun Contact.resolveLinkState(message: PulseLinkMessage.ManualMessage): Contact {
    var updated = this
    var needsUpdate = false
    if (message.code.isNotBlank() && message.code != updated.linkCode) {
        updated = updated.copy(linkCode = message.code)
        needsUpdate = true
    }
    if (updated.remoteDeviceId != message.senderId && message.senderId.isNotBlank()) {
        updated = updated.copy(remoteDeviceId = message.senderId)
        needsUpdate = true
    }
    return if (needsUpdate) updated else this
}

data class RemoteAlertResult(
    val contactId: Long,
    val contactName: String,
    val status: RemoteAlertStatus,
    val tier: EscalationTier
)

enum class RemoteAlertStatus { SUCCESS, NOT_LINKED, SMS_FAILED }

private fun normalizePhone(input: String): String {
    if (input.isBlank()) return ""
    val digits = buildString {
        input.forEach { ch ->
            if (ch.isDigit()) append(ch)
        }
    }
    return if (input.startsWith("+")) "+$digits" else digits
}

private fun normalizeEmail(input: String?): String =
    input?.trim()?.lowercase().orEmpty()

private fun Contact.primaryPhone(): String? =
    (listOf(phoneNumber) + additionalPhones).firstOrNull { it.isNotBlank() }

private fun Contact.primaryEmail(): String? =
    (listOfNotNull(email) + additionalEmails).firstOrNull { it.isNotBlank() }

private fun contactDocId(contact: Contact): String {
    val phoneRaw = contact.primaryPhone()?.trim().orEmpty()
    val normalizedEmail = normalizeEmail(contact.email)
    return when {
        phoneRaw.isNotBlank() -> phoneRaw
        normalizedEmail.isNotBlank() -> "email_$normalizedEmail"
        !contact.remoteUid.isNullOrBlank() -> "uid_${contact.remoteUid}"
        !contact.linkCode.isNullOrBlank() -> "link_${contact.linkCode}"
        else -> contact.displayName.lowercase().replace("\\s+".toRegex(), "_")
            .ifBlank { contact.displayName.hashCode().toString() }
    }
}

@Singleton
class RemoteActionHandler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val alertRouter: AlertRouter,
    private val audioOverrideManager: AudioOverrideManager,
    private val settingsRepository: SettingsRepository,
    private val notificationRegistrar: NotificationRegistrar,
    private val soundCatalog: SoundCatalog
) {

    suspend fun prepareForAlert(
        contact: Contact,
        reason: PulseLinkMessage.AlertPrepareReason = PulseLinkMessage.AlertPrepareReason.ALERT
    ): AudioOverrideManager.OverrideResult {
        val shouldOverride = contact.allowRemoteOverride ||
            reason == PulseLinkMessage.AlertPrepareReason.CALL ||
            reason == PulseLinkMessage.AlertPrepareReason.ALERT
        if (!shouldOverride) return AudioOverrideManager.OverrideResult.skipped()
        return withContext(Dispatchers.Main) {
            Log.d(
                TAG,
                "prepareForAlert contact=${contact.displayName} reason=$reason allowRemote=${contact.allowRemoteOverride}"
            )
            val result = runCatching { audioOverrideManager.overrideForAlert(true) }
                .onFailure { error -> Log.e(TAG, "Remote override failed", error) }
                .getOrElse {
                    AudioOverrideManager.OverrideResult.failure(
                        AudioOverrideManager.OverrideResult.FailureReason.UNKNOWN,
                        it.message
                    )
                }
            if (result.state != AudioOverrideManager.OverrideResult.State.FAILURE &&
                result.state != AudioOverrideManager.OverrideResult.State.SKIPPED
            ) {
                val delay = when (reason) {
                    PulseLinkMessage.AlertPrepareReason.CALL -> CALL_OVERRIDE_HOLD_MS
                    PulseLinkMessage.AlertPrepareReason.MESSAGE -> MESSAGE_OVERRIDE_HOLD_MS
                    PulseLinkMessage.AlertPrepareReason.ALERT -> DEFAULT_OVERRIDE_HOLD_MS
                }
                audioOverrideManager.scheduleRestore(delay)
            }
            result
        }
    }

    suspend fun routeRemoteAlert(
        contact: Contact,
        tier: EscalationTier,
        excludeContactIds: Set<Long> = emptySet()
    ) {
        val updatedExcludes = excludeContactIds + contact.id
        alertRouter.dispatchManual(
            tier = tier,
            trigger = "Remote trigger from ${contact.displayName}",
            excludeContactIds = updatedExcludes
        )
    }

    suspend fun playAttentionTone(
        contact: Contact,
        tier: EscalationTier,
        title: String,
        body: String,
        notificationId: Int,
        forceBypass: Boolean = false,
        overrideHoldMs: Long = MESSAGE_OVERRIDE_HOLD_MS,
        volumeHint: com.pulselink.domain.model.VolumeHint? = null
    ) {
        val settings = settingsRepository.settings.first()
        val (profile, category, soundKey) = when (tier) {
            EscalationTier.EMERGENCY -> Triple(
                settings.emergencyProfile,
                SoundCategory.SIREN,
                contact.emergencySoundKey ?: settings.emergencyProfile.soundKey
            )
            EscalationTier.CHECK_IN -> Triple(
                settings.checkInProfile,
                SoundCategory.CHIME,
                contact.checkInSoundKey ?: settings.checkInProfile.soundKey
            )
        }
        val soundOption = soundCatalog.resolve(soundKey, category)
        val channel = notificationRegistrar.ensureAlertChannel(category, soundOption, profile, settings.customVibrationPattern)
        val requestBypass = forceBypass ||
            profile.breakThroughDnd ||
            tier == EscalationTier.EMERGENCY ||
            contact.allowRemoteOverride
        val toneProfile = if (tier == EscalationTier.EMERGENCY) {
            AudioOverrideManager.ToneProfile.Emergency
        } else {
            AudioOverrideManager.ToneProfile.CheckIn
        }
        val overrideResult = if (requestBypass) {
            withContext(Dispatchers.Main) {
                audioOverrideManager.overrideForAlert(true, volumeHint)
            }
        } else {
            AudioOverrideManager.OverrideResult.skipped()
        }
        val notificationBuilder = NotificationCompat.Builder(context, channel)
            .setSmallIcon(R.drawable.ic_logo)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setAutoCancel(true)
            .apply {
                if (profile.vibrate) {
                    val pattern = VibrationPatterns.patternForAlertKey(profile.vibrationPatternKey, settings.customVibrationPattern)
                    setVibrate(pattern)
                }
                if (requestBypass) {
                    setCategory(NotificationCompat.CATEGORY_ALARM)
                }
            }
        soundOption?.resolveUri(context)?.let { soundUri ->
            notificationBuilder.setSound(soundUri)
            if (overrideResult.state != AudioOverrideManager.OverrideResult.State.SKIPPED) {
                delay(AUDIO_PRIME_DELAY_MS)
            }
            audioOverrideManager.playTone(soundUri, profile = toneProfile)
        }
        val notification = notificationBuilder.build()

        NotificationManagerCompat.from(context).notify(notificationId, notification)

        if (overrideResult.state != AudioOverrideManager.OverrideResult.State.FAILURE &&
            overrideResult.state != AudioOverrideManager.OverrideResult.State.SKIPPED
        ) {
            audioOverrideManager.scheduleRestore(overrideHoldMs)
        }
    }

    suspend fun handleIncomingCall(contact: Contact) {
        val settings = settingsRepository.settings.first()
        val profile = settings.emergencyProfile
        val soundKey = settings.callSoundKey
            ?: contact.emergencySoundKey
            ?: profile.soundKey
        val soundOption = soundCatalog.resolve(soundKey, SoundCategory.CALL)
        val channel = notificationRegistrar.ensureAlertChannel(SoundCategory.CALL, soundOption, profile, settings.customVibrationPattern)
        val overrideResult = audioOverrideManager.overrideForAlert(true)
        if (!overrideResult.success) {
            Log.w(
                TAG,
                "Incoming call override limited state=${overrideResult.state} reason=${overrideResult.reason} message=${overrideResult.message}"
            )
        } else {
            Log.d(TAG, "Incoming call override applied for ${contact.displayName}")
        }
        val soundUri = soundOption?.resolveUri(context)
        if (soundUri != null) {
            if (overrideResult.state != AudioOverrideManager.OverrideResult.State.SKIPPED) {
                delay(AUDIO_PRIME_DELAY_MS)
            }
            audioOverrideManager.playTone(soundUri, profile = AudioOverrideManager.ToneProfile.IncomingCall)
        }
        val title = context.getString(R.string.incoming_call_alert_title, contact.displayName)
        val body = context.getString(R.string.incoming_call_detected, contact.displayName)
        val notification = NotificationCompat.Builder(context, channel)
            .setSmallIcon(R.drawable.ic_logo)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .apply {
                if (profile.vibrate) {
                    val pattern = VibrationPatterns.patternForAlertKey(profile.vibrationPatternKey, settings.customVibrationPattern)
                    setVibrate(pattern)
                }
            }
            .build()
        NotificationManagerCompat.from(context)
            .notify((contact.id.hashCode() and 0xFFFF) + 6000, notification)
        if (overrideResult.state != AudioOverrideManager.OverrideResult.State.FAILURE &&
            overrideResult.state != AudioOverrideManager.OverrideResult.State.SKIPPED
        ) {
            audioOverrideManager.scheduleRestore(CALL_OVERRIDE_HOLD_MS)
        }
    }

    fun stopIncomingCallTone() {
        audioOverrideManager.cancelScheduledRestore()
    }

    suspend fun notifyIncomingCall(contact: Contact, tier: EscalationTier) {
        val title = context.getString(R.string.incoming_call_alert_title, contact.displayName)
        val body = context.getString(R.string.incoming_call_alert_body)
        playAttentionTone(
            contact = contact,
            tier = tier,
            title = title,
            body = body,
            notificationId = (contact.id.hashCode() and 0xFFFF) + 4000,
            forceBypass = true,
            overrideHoldMs = CALL_OVERRIDE_HOLD_MS
        )
    }

    suspend fun showEmergencyPopup(contact: Contact, tier: EscalationTier) {
        withContext(Dispatchers.Main) {
            val intent = EmergencyPopupActivity.newIntent(
                context,
                contact.displayName,
                tier.name
            )
            context.startActivity(intent)
        }
    }

    @Suppress("UNUSED_PARAMETER")
    suspend fun finishCall(contact: Contact, callDuration: Long) {
        withContext(Dispatchers.Main) {
            audioOverrideManager.cancelScheduledRestore()
            audioOverrideManager.restoreIfNeeded()
        }
    }

    companion object {
        private const val DEFAULT_OVERRIDE_HOLD_MS = 120_000L
        private const val MESSAGE_OVERRIDE_HOLD_MS = 90_000L
        private const val CALL_OVERRIDE_HOLD_MS = 180_000L
        private const val AUDIO_PRIME_DELAY_MS = 90L
        private const val TAG = "RemoteActionHandler"
    }
}
