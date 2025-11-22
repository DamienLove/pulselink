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
import com.pulselink.domain.model.MessageDirection
import com.pulselink.domain.model.RemotePresence
import com.pulselink.domain.model.SoundCategory
import com.pulselink.domain.repository.AlertRepository
import com.pulselink.domain.repository.BlockedContactRepository
import com.pulselink.domain.repository.ContactRepository
import com.pulselink.domain.repository.MessageRepository
import com.pulselink.domain.repository.SettingsRepository
import com.pulselink.service.AlertRouter
import com.pulselink.util.AudioOverrideManager
import com.pulselink.util.resolveUri
import com.pulselink.ui.EmergencyPopupActivity
import com.pulselink.util.CallStateMonitor
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.SetOptions
import com.google.firebase.Timestamp
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
    private val firestore: FirebaseFirestore
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
                handleRealtimeManualMessage(payload)
            }
        }
    }

    suspend fun sendLinkRequest(contactId: Long) {
        val contact = contactRepository.getContact(contactId) ?: return
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
        val payload = SmsCodec.encodeLinkRequest(deviceId, code, senderName)
        smsSender.sendSms(contact.phoneNumber, payload)
    }

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
        maybeApplyRemoteUid(code, contact.phoneNumber)
        val payload = SmsCodec.encodeLinkAccept(deviceId, code)
        smsSender.sendSms(contact.phoneNumber, payload)
    }

    suspend fun sendPing(contactId: Long): Boolean {
        val contact = contactRepository.getContact(contactId) ?: return false
        if (contact.linkStatus != LinkStatus.LINKED || contact.linkCode.isNullOrBlank()) return false
        val ready = requestRemotePrepare(
            contact = contact,
            tier = EscalationTier.CHECK_IN,
            reason = PulseLinkMessage.AlertPrepareReason.MESSAGE
        )
        val deviceId = settingsRepository.ensureDeviceId()
        val payload = SmsCodec.encodePing(deviceId, contact.linkCode)
        smsSender.sendSms(contact.phoneNumber, payload)
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
        val payload = SmsCodec.encodeCallEnded(deviceId, code, callDuration)
        smsSender.sendSms(contact.phoneNumber, payload, awaitResult = false)
    }

    suspend fun sendSoundOverride(contactId: Long, tier: EscalationTier, soundKey: String?) {
        val contact = contactRepository.getContact(contactId) ?: return
        if (contact.linkStatus != LinkStatus.LINKED) return
        val code = contact.linkCode ?: return
        val deviceId = settingsRepository.ensureDeviceId()
        val payload = SmsCodec.encodeSoundOverride(deviceId, code, tier, soundKey)
        smsSender.sendSms(contact.phoneNumber, payload, awaitResult = false)
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
        maybeApplyRemoteUid(message.code, fromPhone)
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
        maybeApplyRemoteUid(message.code, fromPhone)
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
        val response = SmsCodec.encodeAlertReady(deviceId, message.code, overrideApplied)
        // Fire-and-forget so we don't block the inbound SMS broadcast while waiting for
        // the modem to confirm send/delivery of the acknowledgement.
        smsSender.sendSms(contact.phoneNumber, response, awaitResult = false)
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
            deliverManualMessage(persisted, message.body, overrideApplied = true)
            maybeApplyRemoteUid(message.code, fromPhone)
        } catch (error: Exception) {
            Log.e(TAG, "Failed to process manual message from $fromPhone", error)
        }
    }

    private suspend fun handleRealtimeManualMessage(payload: LinkChannelPayload) {
        try {
            val contact = payload.linkCode?.takeIf { it.isNotBlank() }?.let { contactRepository.getByLinkCode(it) }
                ?: contactRepository.getByRemoteDeviceId(payload.senderId)
                ?: payload.phoneNumber?.takeIf { it.isNotBlank() }?.let { contactRepository.getByPhone(it) }
                ?: return
            markPresence(contact, payload.timestamp)
            deliverManualMessage(contact, payload.body, overrideApplied = true)
            payload.linkCode?.let { maybeApplyRemoteUid(it, contact.phoneNumber) }
        } catch (error: Exception) {
            Log.e(TAG, "Failed to process realtime message ${payload.id}", error)
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

    private suspend fun deliverManualMessage(contact: Contact, rawBody: String, overrideApplied: Boolean) {
        val body = rawBody.ifBlank { context.getString(R.string.ping_received_body) }
        val title = context.getString(R.string.manual_message_title, contact.displayName)
        if (isAutoAlertBody(rawBody)) {
            withContext(Dispatchers.IO) {
                messageRepository.record(
                    ContactMessage(
                        contactId = contact.id,
                        body = body,
                        direction = MessageDirection.INBOUND,
                        overrideSucceeded = overrideApplied
                    )
                )
            }
            return
        }
        remoteActionHandler.playAttentionTone(
            contact = contact,
            tier = EscalationTier.CHECK_IN,
            title = title,
            body = body,
            notificationId = (contact.id.hashCode() and 0xFFFF) + 3000
        )
        withContext(Dispatchers.IO) {
            messageRepository.record(
                ContactMessage(
                    contactId = contact.id,
                    body = body,
                    direction = MessageDirection.INBOUND,
                    overrideSucceeded = overrideApplied
                )
            )
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
            val payload = SmsCodec.encodeManualMessage(deviceId, code, body)
            if (smsSender.sendSms(contact.phoneNumber, payload)) {
                sentAny = true
                messageRepository.record(
                    ContactMessage(
                        contactId = contact.id,
                        body = body,
                        direction = MessageDirection.OUTBOUND,
                        overrideSucceeded = false
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
        when (message.key) {
            CONFIG_REMOTE_SOUND -> {
                val allow = message.value == "1"
                contactRepository.upsert(freshContact.copy(allowRemoteSoundChange = allow))
            }
            CONFIG_REMOTE_OVERRIDE -> {
                val allow = message.value == "1"
                contactRepository.upsert(freshContact.copy(allowRemoteOverride = allow))
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
            val payload = SmsCodec.encodeConfig(deviceId, contact.linkCode, CONFIG_REMOTE_SOUND, if (allow) "1" else "0")
            smsSender.sendSms(contact.phoneNumber, payload)
        }
    }

    suspend fun updateRemoteOverridePermission(contactId: Long, allow: Boolean) {
        val contact = contactRepository.getContact(contactId) ?: return
        val updated = contact.copy(allowRemoteOverride = allow)
        contactRepository.upsert(updated)
        if (contact.linkStatus == LinkStatus.LINKED && !contact.linkCode.isNullOrBlank()) {
            val deviceId = settingsRepository.ensureDeviceId()
            val payload = SmsCodec.encodeConfig(deviceId, contact.linkCode, CONFIG_REMOTE_OVERRIDE, if (allow) "1" else "0")
            smsSender.sendSms(contact.phoneNumber, payload)
        }
    }

    suspend fun prepareRemoteOverride(contactId: Long, tier: EscalationTier): Boolean {
        val contact = contactRepository.getContact(contactId) ?: return false
        if (contact.linkStatus != LinkStatus.LINKED || contact.linkCode.isNullOrBlank()) return false
        return requestRemotePrepare(contact, tier)
    }

        suspend fun sendManualMessage(contactId: Long, message: String): ManualMessageResult {
        Log.d(TAG, "sendManualMessage: START for contactId=$contactId")
        val contact = contactRepository.getContact(contactId)
            ?: return ManualMessageResult.Failure(ManualMessageResult.Failure.Reason.CONTACT_MISSING)
        val code = contact.linkCode
        if (code.isNullOrBlank()) {
            Log.w(TAG, "sendManualMessage: FAILED. Reason: NOT_LINKED for contactId=$contactId")
            return ManualMessageResult.Failure(ManualMessageResult.Failure.Reason.NOT_LINKED)
        }
        return try {
            Log.d(TAG, "sendManualMessage: Attempting realtime send for contactId=$contactId.")
            val realtimeSent = linkChannelService.sendManualMessage(contact, message)
            Log.d(TAG, "sendManualMessage: Realtime send result for contactId=$contactId: $realtimeSent")
            val ready = if (contact.linkStatus == LinkStatus.LINKED) {
                requestRemotePrepare(
                    contact,
                    EscalationTier.CHECK_IN,
                    reason = PulseLinkMessage.AlertPrepareReason.MESSAGE
                )
            } else {
                false
            }
            if (!realtimeSent) {
                Log.d(TAG, "sendManualMessage: Realtime send failed, falling back to SMS for contactId=$contactId.")
            } else {
                Log.d(TAG, "sendManualMessage: Realtime send succeeded; mirroring via SMS for contactId=$contactId.")
            }
            val deviceId = settingsRepository.ensureDeviceId()
            val payload = SmsCodec.encodeManualMessage(deviceId, code, message)
            val smsSent = smsSender.sendSms(contact.phoneNumber, payload)

            val overallSuccess = realtimeSent || smsSent
            if (overallSuccess) {
                Log.d(TAG, "Recording outbound message for contact=$contactId overrideReady=$ready")
                messageRepository.record(
                    ContactMessage(
                        contactId = contact.id,
                        body = message,
                        direction = MessageDirection.OUTBOUND,
                        overrideSucceeded = ready
                    )
                )
            }

            if (smsSent) {
                ManualMessageResult.Success(overrideApplied = ready)
            } else {
                if (realtimeSent) {
                    Log.w(TAG, "sendManualMessage: SMS mirror failed but realtime dispatch succeeded for contactId=$contactId")
                    ManualMessageResult.Success(overrideApplied = ready)
                } else {
                    ManualMessageResult.Failure(ManualMessageResult.Failure.Reason.SMS_FAILED)
                }
            }
        } catch (error: Exception) {
            Log.e(TAG, "Unable to send manual message for contactId=$contactId", error)
            ManualMessageResult.Failure(ManualMessageResult.Failure.Reason.UNKNOWN)
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
        val payload = SmsCodec.encodeAlertPrepare(deviceId, code, tier, reason)
        smsSender.sendSms(contact.phoneNumber, payload)
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
            normalizePhone(existing.phoneNumber) == normalizedIncoming
        }
    }

    suspend fun triggerRemoteAlert(contact: Contact, tier: EscalationTier): RemoteAlertResult {
        if (contact.linkStatus != LinkStatus.LINKED || contact.linkCode.isNullOrBlank()) {
            return RemoteAlertResult(contact.id, contact.displayName, RemoteAlertStatus.NOT_LINKED, tier)
        }
        val deviceId = settingsRepository.ensureDeviceId()
        val code = contact.linkCode
        val preparePayload = SmsCodec.encodeAlertPrepare(
            deviceId,
            code,
            tier,
            PulseLinkMessage.AlertPrepareReason.ALERT
        )
        smsSender.sendSms(contact.phoneNumber, preparePayload, awaitResult = false)
        val alertPayload = SmsCodec.encodeRemoteAlert(deviceId, code, tier)
        val alertSent = smsSender.sendSms(contact.phoneNumber, alertPayload, awaitResult = false)
        return if (alertSent) {
            RemoteAlertResult(contact.id, contact.displayName, RemoteAlertStatus.SUCCESS, tier)
        } else {
            RemoteAlertResult(contact.id, contact.displayName, RemoteAlertStatus.SMS_FAILED, tier)
        }
    }

    suspend fun syncLinksOnLogin() {
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

    private suspend fun mirrorContactToCloud(contact: Contact) {
        val user = auth.currentUser ?: return
        if (user.isAnonymous) return
        val docId = contact.phoneNumber.ifBlank {
            contact.displayName.lowercase().replace("\\s+".toRegex(), "_")
                .ifBlank { contact.displayName.hashCode().toString() }
        }
        val payload = mapOf(
            "displayName" to contact.displayName,
            "phoneNumber" to contact.phoneNumber,
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

    companion object {
        private const val TAG = "ContactLinkManager"
        private const val CHANNEL_ID = "pulselink_link_channel"
        const val CONFIG_REMOTE_SOUND = "ALLOW_SOUND"
        const val CONFIG_REMOTE_OVERRIDE = "ALLOW_OVERRIDE"
        private const val PREPARE_TIMEOUT_MS = 10_000L
        const val COLLECTION_LINKS = "links"
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
    if (digits.length > 10 && digits.startsWith("1")) {
        return digits.drop(1)
    }
    return digits
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
        overrideHoldMs: Long = MESSAGE_OVERRIDE_HOLD_MS
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
        val channel = notificationRegistrar.ensureAlertChannel(category, soundOption, profile)
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
                audioOverrideManager.overrideForAlert(true)
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
                    setVibrate(longArrayOf(0, 250, 250, 250, 500, 250))
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
        val channel = notificationRegistrar.ensureAlertChannel(SoundCategory.CALL, soundOption, profile)
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
                    setVibrate(longArrayOf(0, 250, 250, 250, 500, 250))
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
