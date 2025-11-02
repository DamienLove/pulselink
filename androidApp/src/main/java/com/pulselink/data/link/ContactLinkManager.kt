package com.pulselink.data.link

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.media.AudioManager
import android.os.Build
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
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID
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
        val payload = SmsCodec.encodePing(deviceId, contact.linkCode)
        smsSender.sendSms(contact.phoneNumber, payload)
    }

    suspend fun handleInbound(message: PulseLinkMessage, fromPhone: String) {
        when (message) {
            is PulseLinkMessage.LinkRequest -> handleLinkRequest(message, fromPhone)
            is PulseLinkMessage.LinkAccept -> handleLinkAccept(message, fromPhone)
            is PulseLinkMessage.Ping -> handlePing(message)
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
        private const val CHANNEL_ID = "pulselink_link_channel"
        const val CONFIG_REMOTE_SOUND = "ALLOW_SOUND"
        const val CONFIG_REMOTE_OVERRIDE = "ALLOW_OVERRIDE"
    }
}

@Singleton
class RemoteActionHandler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val alertRouter: com.pulselink.service.AlertRouter
) {

    private val audioManager: AudioManager by lazy {
        context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    }

    suspend fun prepareForAlert(contact: Contact) {
        if (!contact.allowRemoteOverride) return
        withContext(Dispatchers.Main) {
            val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_RING)
            audioManager.setStreamVolume(AudioManager.STREAM_RING, maxVolume, 0)
            audioManager.ringerMode = AudioManager.RINGER_MODE_NORMAL
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (notificationManager.isNotificationPolicyAccessGranted) {
                notificationManager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALL)
            }
        }
    }

    suspend fun routeRemoteAlert(contact: Contact, tier: EscalationTier) {
        alertRouter.dispatchManual(tier, "Remote trigger from ${contact.displayName}")
    }
}
