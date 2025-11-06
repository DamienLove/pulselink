package com.pulselink.data.link

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.pulselink.R
import com.pulselink.data.alert.NotificationRegistrar
import com.pulselink.data.alert.SoundCatalog
import com.pulselink.data.sms.PulseLinkMessage
import com.pulselink.data.sms.SmsCodec
import com.pulselink.data.sms.SmsSender
import com.pulselink.domain.model.Contact
import com.pulselink.domain.model.ContactMessage
import com.pulselink.domain.model.EscalationTier
import com.pulselink.domain.model.LinkStatus
import com.pulselink.domain.model.ManualMessageResult
import com.pulselink.domain.model.MessageDirection
import com.pulselink.domain.model.SoundCategory
import com.pulselink.domain.repository.ContactRepository
import com.pulselink.domain.repository.MessageRepository
import com.pulselink.domain.repository.SettingsRepository
import com.pulselink.service.AlertRouter
import com.pulselink.util.AudioOverrideManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.flow.first
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ContactLinkManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val smsSender: SmsSender,
    private val settingsRepository: SettingsRepository,
    private val contactRepository: ContactRepository,
    private val remoteActionHandler: RemoteActionHandler,
    private val messageRepository: MessageRepository
) {

    private val notificationManager by lazy { NotificationManagerCompat.from(context) }
    private val alertHandshake = ConcurrentHashMap<String, CompletableDeferred<Boolean>>()
    enum class CallPreparationResult { READY, TIMEOUT, FAILED }

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

    suspend fun handleInbound(message: PulseLinkMessage, fromPhone: String) {
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

    private suspend fun handleLinkRequest(message: PulseLinkMessage.LinkRequest, fromPhone: String) {
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
            pendingApproval = true
        )
        contactRepository.upsert(updated)
        val persisted = contactRepository.getByLinkCode(message.code)
            ?: findContactByPhoneFlexible(fromPhone)
            ?: updated
        notifyLinkRequest(persisted)
    }

    private suspend fun handleLinkAccept(message: PulseLinkMessage.LinkAccept, fromPhone: String) {
        val base = contactRepository.getByLinkCode(message.code) ?: findContactByPhoneFlexible(fromPhone)
        base?.let {
            val allowOverride = if (it.linkStatus == LinkStatus.LINKED) {
                it.allowRemoteOverride
            } else {
                true
            }
            val linked = it.copy(
                linkStatus = LinkStatus.LINKED,
                remoteDeviceId = message.senderId,
                pendingApproval = false,
                allowRemoteOverride = allowOverride
            )
            contactRepository.upsert(linked)
            notifyLinked(linked)
        }
    }

    private suspend fun handlePing(message: PulseLinkMessage.Ping) {
        val contact = contactRepository.getByLinkCode(message.code) ?: return
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
        val contact = contactRepository.getByLinkCode(message.code) ?: return
        remoteActionHandler.prepareForAlert(contact)
        remoteActionHandler.routeRemoteAlert(contact, message.tier)
    }

    private suspend fun handleAlertPrepare(message: PulseLinkMessage.AlertPrepare) {
        val contact = contactRepository.getByLinkCode(message.code) ?: return
        val overrideApplied = remoteActionHandler.prepareForAlert(contact, message.reason)
        if (!overrideApplied) {
            Log.w(TAG, "Unable to apply remote override for contact ${contact.displayName}")
        }
        val deviceId = settingsRepository.ensureDeviceId()
        val response = SmsCodec.encodeAlertReady(deviceId, message.code, overrideApplied)
        smsSender.sendSms(contact.phoneNumber, response)
    }

    private fun handleAlertReady(message: PulseLinkMessage.AlertReady) {
        alertHandshake.remove(message.code)?.complete(message.ready)
    }

    private suspend fun handleSoundOverride(message: PulseLinkMessage.SoundOverride) {
        val contact = contactRepository.getByLinkCode(message.code) ?: return
        if (!contact.allowRemoteSoundChange) return
        val updated = when (message.tier) {
            EscalationTier.EMERGENCY -> contact.copy(emergencySoundKey = message.soundKey)
            EscalationTier.CHECK_IN -> contact.copy(checkInSoundKey = message.soundKey)
        }
        contactRepository.upsert(updated)
    }

    private suspend fun handleManualMessage(message: PulseLinkMessage.ManualMessage, fromPhone: String) {
        try {
            val persisted = resolveContactForManualMessage(message, fromPhone) ?: return
            val title = context.getString(R.string.manual_message_title, persisted.displayName)
            val body = message.body.ifBlank { context.getString(R.string.ping_received_body) }
            remoteActionHandler.playAttentionTone(
                contact = persisted,
                tier = EscalationTier.CHECK_IN,
                title = title,
                body = body,
                notificationId = (persisted.id.hashCode() and 0xFFFF) + 3000
            )
            withContext(Dispatchers.IO) {
                messageRepository.record(
                    ContactMessage(
                        contactId = persisted.id,
                        body = body,
                        direction = MessageDirection.INBOUND,
                        overrideSucceeded = true
                    )
                )
            }
        } catch (error: Exception) {
            Log.e(TAG, "Failed to process manual message from $fromPhone", error)
        }
    }

    private suspend fun handleCallEnded(message: PulseLinkMessage.CallEnded) {
        val contact = contactRepository.getByLinkCode(message.code) ?: return
        remoteActionHandler.finishCall(contact, message.callDuration)
    }

    private suspend fun resolveContactForManualMessage(
        message: PulseLinkMessage.ManualMessage,
        fromPhone: String
    ): Contact? = withContext(Dispatchers.IO) {
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
        when (message.key) {
            CONFIG_REMOTE_SOUND -> {
                val allow = message.value == "1"
                contactRepository.upsert(contact.copy(allowRemoteSoundChange = allow))
            }
            CONFIG_REMOTE_OVERRIDE -> {
                val allow = message.value == "1"
                contactRepository.upsert(contact.copy(allowRemoteOverride = allow))
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
        val contact = contactRepository.getContact(contactId)
            ?: return ManualMessageResult.Failure(ManualMessageResult.Failure.Reason.CONTACT_MISSING)
        val code = contact.linkCode
        if (code.isNullOrBlank()) {
            return ManualMessageResult.Failure(ManualMessageResult.Failure.Reason.NOT_LINKED)
        }
        return try {
            val ready = if (contact.linkStatus == LinkStatus.LINKED) {
                requestRemotePrepare(contact, EscalationTier.CHECK_IN)
            } else {
                false
            }
            val deviceId = settingsRepository.ensureDeviceId()
            val payload = SmsCodec.encodeManualMessage(deviceId, code, message)
            val sent = smsSender.sendSms(contact.phoneNumber, payload)
            if (!sent) {
                ManualMessageResult.Failure(ManualMessageResult.Failure.Reason.SMS_FAILED)
            } else {
                messageRepository.record(
                    ContactMessage(
                        contactId = contact.id,
                        body = message,
                        direction = MessageDirection.OUTBOUND,
                        overrideSucceeded = ready
                    )
                )
                ManualMessageResult.Success(overrideApplied = ready)
            }
        } catch (error: Exception) {
            Log.e(TAG, "Unable to send manual message", error)
            ManualMessageResult.Failure(ManualMessageResult.Failure.Reason.UNKNOWN)
        }
    }

    private suspend fun requestRemotePrepare(
        contact: Contact,
        tier: EscalationTier,
        reason: PulseLinkMessage.AlertPrepareReason = PulseLinkMessage.AlertPrepareReason.ALERT
    ): Boolean {
        if (!contact.allowRemoteOverride && reason != PulseLinkMessage.AlertPrepareReason.CALL) return true
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

    private suspend fun findContactByPhoneFlexible(phone: String): Contact? {
        contactRepository.getByPhone(phone)?.let { return it }
        val normalizedIncoming = normalizePhone(phone)
        if (normalizedIncoming.isEmpty()) return null
        return contactRepository.observeContacts().first().firstOrNull { existing ->
            normalizePhone(existing.phoneNumber) == normalizedIncoming
        }
    }

    companion object {
        private const val TAG = "ContactLinkManager"
        private const val CHANNEL_ID = "pulselink_link_channel"
        const val CONFIG_REMOTE_SOUND = "ALLOW_SOUND"
        const val CONFIG_REMOTE_OVERRIDE = "ALLOW_OVERRIDE"
        private const val PREPARE_TIMEOUT_MS = 10_000L
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
    if (updated.linkStatus != LinkStatus.LINKED) {
        updated = updated.copy(
            linkStatus = LinkStatus.LINKED,
            pendingApproval = false
        )
        needsUpdate = true
    }
    return if (needsUpdate) updated else this
}

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
    ): Boolean {
        val shouldOverride = contact.allowRemoteOverride || reason == PulseLinkMessage.AlertPrepareReason.CALL
        if (!shouldOverride) return false
        return withContext(Dispatchers.Main) {
            val applied = audioOverrideManager.overrideForAlert(true)
            if (applied) {
                val delay = when (reason) {
                    PulseLinkMessage.AlertPrepareReason.CALL -> CALL_OVERRIDE_HOLD_MS
                    PulseLinkMessage.AlertPrepareReason.MESSAGE -> MESSAGE_OVERRIDE_HOLD_MS
                    PulseLinkMessage.AlertPrepareReason.ALERT -> DEFAULT_OVERRIDE_HOLD_MS
                }
                audioOverrideManager.scheduleRestore(delay)
            }
            applied
        }
    }

    suspend fun routeRemoteAlert(contact: Contact, tier: EscalationTier) {
        alertRouter.dispatchManual(tier, "Remote trigger from ${contact.displayName}")
    }

    suspend fun playAttentionTone(
        contact: Contact,
        tier: EscalationTier,
        title: String,
        body: String,
        notificationId: Int
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
        val requestBypass = profile.breakThroughDnd || contact.allowRemoteOverride
        val overrideApplied = withContext(Dispatchers.Main) {
            audioOverrideManager.overrideForAlert(requestBypass)
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
        soundOption?.let {
            val soundUri = android.net.Uri.parse("android.resource://${context.packageName}/${it.resId}")
            notificationBuilder.setSound(soundUri)
        }
        val notification = notificationBuilder.build()

        NotificationManagerCompat.from(context).notify(notificationId, notification)

        if (overrideApplied) {
            audioOverrideManager.scheduleRestore(MESSAGE_OVERRIDE_HOLD_MS)
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
    }
}
