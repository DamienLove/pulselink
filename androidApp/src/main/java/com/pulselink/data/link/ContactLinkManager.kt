package com.pulselink.data.link

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.pulselink.R
import com.pulselink.data.sms.PulseLinkMessage
import com.pulselink.data.sms.SmsCodec
import com.pulselink.data.sms.SmsSender
import com.pulselink.domain.model.Contact
import com.pulselink.domain.model.EscalationTier
import com.pulselink.domain.model.LinkStatus
import com.pulselink.domain.repository.ContactRepository
import com.pulselink.domain.repository.SettingsRepository
import com.pulselink.util.AudioOverrideManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
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
    private val remoteActionHandler: RemoteActionHandler
) {

    private val notificationManager by lazy { NotificationManagerCompat.from(context) }
    private val alertHandshake = ConcurrentHashMap<String, CompletableDeferred<Boolean>>()

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
        val payload = SmsCodec.encodeLinkRequest(deviceId, code, contact.displayName)
        smsSender.sendSms(contact.phoneNumber, payload)
    }

    suspend fun approveLink(contactId: Long) {
        val contact = contactRepository.getContact(contactId) ?: return
        val deviceId = settingsRepository.ensureDeviceId()
        val code = contact.linkCode ?: UUID.randomUUID().toString()
        val updated = contact.copy(
            linkCode = code,
            linkStatus = LinkStatus.LINKED,
            pendingApproval = false
        )
        contactRepository.upsert(updated)
        val payload = SmsCodec.encodeLinkAccept(deviceId, code)
        smsSender.sendSms(contact.phoneNumber, payload)
    }

    suspend fun sendPing(contactId: Long) {
        val contact = contactRepository.getContact(contactId) ?: return
        if (contact.linkStatus != LinkStatus.LINKED || contact.linkCode.isNullOrBlank()) return
        val deviceId = settingsRepository.ensureDeviceId()
        val linkCode = contact.linkCode
        if (contact.allowRemoteOverride) {
            val deferred = CompletableDeferred<Boolean>()
            alertHandshake[linkCode] = deferred
            val preparePayload = SmsCodec.encodeAlertPrepare(deviceId, linkCode, EscalationTier.CHECK_IN)
            smsSender.sendSms(contact.phoneNumber, preparePayload)
            val ready = withTimeoutOrNull(PREPARE_TIMEOUT_MS) { deferred.await() } ?: false
            alertHandshake.remove(linkCode)
            if (!ready) {
                Log.w(TAG, "Remote contact did not acknowledge alert preparation for code $linkCode")
            }
        }
        val payload = SmsCodec.encodePing(deviceId, linkCode)
        smsSender.sendSms(contact.phoneNumber, payload)
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
            is PulseLinkMessage.ConfigUpdate -> handleConfigUpdate(message)
        }
    }

    private suspend fun handleLinkRequest(message: PulseLinkMessage.LinkRequest, fromPhone: String) {
        val existingByCode = contactRepository.getByLinkCode(message.code)
        val existingByPhone = contactRepository.getByPhone(fromPhone)
        val base = existingByCode ?: existingByPhone
        val updated = (base ?: Contact(
            displayName = if (message.senderName.isNotBlank()) message.senderName else fromPhone,
            phoneNumber = fromPhone
        )).copy(
            linkStatus = LinkStatus.INBOUND_REQUEST,
            linkCode = message.code,
            remoteDeviceId = message.senderId,
            pendingApproval = true
        )
        contactRepository.upsert(updated)
        notifyLinkRequest(updated)
    }

    private suspend fun handleLinkAccept(message: PulseLinkMessage.LinkAccept, fromPhone: String) {
        val base = contactRepository.getByLinkCode(message.code) ?: contactRepository.getByPhone(fromPhone)
        base?.let {
            val linked = it.copy(
                linkStatus = LinkStatus.LINKED,
                remoteDeviceId = message.senderId,
                pendingApproval = false
            )
            contactRepository.upsert(linked)
            notifyLinked(linked)
        }
    }

    private suspend fun handlePing(message: PulseLinkMessage.Ping) {
        val contact = contactRepository.getByLinkCode(message.code) ?: return
        notifyPing(contact)
    }

    private suspend fun handleRemoteAlert(message: PulseLinkMessage.RemoteAlert) {
        val contact = contactRepository.getByLinkCode(message.code) ?: return
        remoteActionHandler.prepareForAlert(contact)
        remoteActionHandler.routeRemoteAlert(contact, message.tier)
    }

    private suspend fun handleAlertPrepare(message: PulseLinkMessage.AlertPrepare) {
        val contact = contactRepository.getByLinkCode(message.code) ?: return
        val overrideApplied = remoteActionHandler.prepareForAlert(contact)
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

    private fun notifyPing(contact: Contact) {
        ensureChannel()
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle(context.getString(R.string.ping_received_title, contact.displayName))
            .setContentText(context.getString(R.string.ping_received_body))
            .setSmallIcon(R.drawable.ic_logo)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()
        notificationManager.notify((contact.id.hashCode() and 0xFFFF) + 2000, notification)
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

    companion object {
        private const val TAG = "ContactLinkManager"
        private const val CHANNEL_ID = "pulselink_link_channel"
        const val CONFIG_REMOTE_SOUND = "ALLOW_SOUND"
        const val CONFIG_REMOTE_OVERRIDE = "ALLOW_OVERRIDE"
        private const val PREPARE_TIMEOUT_MS = 10_000L
    }
}

@Singleton
class RemoteActionHandler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val alertRouter: com.pulselink.service.AlertRouter,
    private val audioOverrideManager: AudioOverrideManager
) {

    suspend fun prepareForAlert(contact: Contact): Boolean {
        if (!contact.allowRemoteOverride) return false
        return withContext(Dispatchers.Main) {
            val applied = audioOverrideManager.overrideForAlert(true)
            if (applied) {
                audioOverrideManager.scheduleRestore()
            }
            applied
        }
    }

    suspend fun routeRemoteAlert(contact: Contact, tier: EscalationTier) {
        alertRouter.dispatchManual(tier, "Remote trigger from ${contact.displayName}")
    }
}
