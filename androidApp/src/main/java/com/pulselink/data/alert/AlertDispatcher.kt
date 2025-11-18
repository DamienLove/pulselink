package com.pulselink.data.alert

import android.Manifest
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresPermission
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.PendingIntentCompat
import com.pulselink.R
import com.pulselink.data.location.LocationProvider
import com.pulselink.data.sms.SmsSender
import com.pulselink.domain.model.AlertProfile
import com.pulselink.domain.model.Contact
import com.pulselink.domain.model.EscalationTier
import com.pulselink.domain.model.SoundCategory
import com.pulselink.domain.model.SoundOption
import com.pulselink.domain.model.PulseLinkSettings
import com.pulselink.util.AudioOverrideManager
import com.pulselink.util.resolveUri
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AlertDispatcher @Inject constructor(
    @ApplicationContext private val context: Context,
    private val smsSender: SmsSender,
    private val locationProvider: LocationProvider,
    private val registrar: NotificationRegistrar,
    private val soundCatalog: SoundCatalog,
    private val audioOverrideManager: AudioOverrideManager
) {

    suspend fun dispatch(
        phrase: String,
        tier: EscalationTier,
        contacts: List<Contact>,
        settings: PulseLinkSettings,
        contactId: Long? = null
    ): AlertResult = withContext(Dispatchers.IO) {
        registrar.ensureChannels()

        val locationText = if (settings.includeLocation) buildLocationText() else null
        val message = buildMessage(phrase, tier, locationText)

        val (profile, soundCategory) = when (tier) {
            EscalationTier.EMERGENCY -> settings.emergencyProfile to SoundCategory.SIREN
            EscalationTier.CHECK_IN -> settings.checkInProfile to SoundCategory.CHIME
        }
        val soundOption = soundCatalog.resolve(profile.soundKey, soundCategory)
        val channelId = registrar.ensureAlertChannel(soundCategory, soundOption, profile)
        val orderedContacts = contacts.sortedWith(
            compareBy<Contact> { it.contactOrder }.thenBy { it.displayName.lowercase(Locale.US) }
        )

        val shouldOverrideAudio = profile.breakThroughDnd || tier == EscalationTier.EMERGENCY
        val notificationManager = context.getSystemService(NotificationManager::class.java)
        val preFilter = notificationManager?.currentInterruptionFilter
        val overrideResult = if (shouldOverrideAudio) {
            Log.d(
                TAG,
                "Attempting audio override tier=$tier breakThrough=${profile.breakThroughDnd} sdk=${Build.VERSION.SDK_INT} hasPolicy=${notificationManager?.isNotificationPolicyAccessGranted}"
            )
            audioOverrideManager.overrideForAlert(profile.breakThroughDnd).also { result ->
                val postFilter = notificationManager?.currentInterruptionFilter
                if (!result.success) {
                    Log.w(
                        TAG,
                        "Audio override state=${result.state} reason=${result.reason} message=${result.message} preFilter=$preFilter postFilter=$postFilter"
                    )
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
                        Log.w(
                            TAG,
                            "Android 15+ only toggles the app's AutomaticZenRule; rely on channel bypass for full DND break-through."
                        )
                    }
                } else {
                    Log.d(TAG, "Audio override applied preFilter=$preFilter postFilter=$postFilter")
                }
            }
        } else {
            AudioOverrideManager.OverrideResult.skipped()
        }

        val smsCount = try {
            sendSms(tier, message, orderedContacts)
        } catch (error: Exception) {
            Log.e(TAG, "Unable to send alert SMS", error)
            0
        }

        sendNotification(
            channel = channelId,
            tier = tier,
            message = message,
            profile = profile,
            soundOption = soundOption,
            primaryContact = orderedContacts.firstOrNull()
        )

        if (shouldOverrideAudio && overrideResult.state != AudioOverrideManager.OverrideResult.State.FAILURE && overrideResult.state != AudioOverrideManager.OverrideResult.State.SKIPPED) {
            audioOverrideManager.scheduleRestore()
        }

        val resolvedSoundKey = soundOption?.key ?: profile.soundKey
        AlertResult(
            message = message,
            notifiedContacts = smsCount,
            sharedLocation = locationText != null,
            overrideResult = overrideResult.takeIf { shouldOverrideAudio },
            soundKey = resolvedSoundKey,
            contactId = contactId
        )
    }

    private suspend fun buildLocationText(): String? {
        val location = runCatching { locationProvider.lastKnownLocation() }.getOrNull() ?: return null
        val geoUri = "https://maps.google.com/?q=${location.latitude},${location.longitude}"
        val timestamp = SimpleDateFormat("MMM d, HH:mm", Locale.US).format(Date())
        return "Last known location @ $timestamp: $geoUri"
    }

    @RequiresPermission(Manifest.permission.SEND_SMS)
    private suspend fun sendSms(
        tier: EscalationTier,
        message: String,
        contacts: List<Contact>
    ): Int {
        val delay = if (tier == EscalationTier.EMERGENCY) SEQUENTIAL_CONTACT_DELAY_MS else 0L
        return smsSender.sendAlert(message, contacts, delay)
    }

    private fun buildMessage(phrase: String, tier: EscalationTier, locationText: String?): String {
        val header = when (tier) {
            EscalationTier.EMERGENCY -> "EMERGENCY" to "I need help right now."
            EscalationTier.CHECK_IN -> "I'M SAFE" to "I'm safe and sharing my location."
        }
        val builder = StringBuilder()
            .append("PulseLink ${header.first}: ${header.second}\n")
            .append("Phrase triggered: \"")
            .append(phrase)
            .append("\".")
        if (locationText != null) {
            builder.append("\n").append(locationText)
        }
        builder.append("\nThis message was sent automatically via PulseLink.")
        return builder.toString()
    }

    private fun sendNotification(
        channel: String,
        tier: EscalationTier,
        message: String,
        profile: AlertProfile,
        soundOption: SoundOption?,
        primaryContact: Contact?
    ) {
        val manager = NotificationManagerCompat.from(context)
        val builder = NotificationCompat.Builder(context, channel)
            .setSmallIcon(R.drawable.ic_logo)
            .setContentTitle(
                if (tier == EscalationTier.EMERGENCY) context.getString(R.string.notification_title_alert)
                else context.getString(R.string.notification_title_check_in)
            )
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setAutoCancel(true)

        if (profile.vibrate) builder.setVibrate(longArrayOf(0, 250, 250, 250, 500, 250))
        if (profile.breakThroughDnd) builder.setCategory(NotificationCompat.CATEGORY_ALARM)

        val soundUri = soundOption?.resolveUri(context)
        if (soundUri != null) {
            builder.setSound(soundUri)
        }

        val callIntent = primaryContact?.let {
            val dial = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${'$'}{it.phoneNumber}"))
            PendingIntentCompat.getActivity(
                context,
                it.id.toInt(),
                dial,
                PendingIntent.FLAG_UPDATE_CURRENT,
                false
            )
        }
        if (callIntent != null) {
            builder.addAction(
                R.drawable.ic_logo,
                context.getString(R.string.notification_action_call),
                callIntent
            )
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val sysManager = context.getSystemService(NotificationManager::class.java)
            if (sysManager?.getNotificationChannel(channel) == null) {
                Log.w(TAG, "Expected notification channel $channel missing; re-registering")
                registrar.ensureAlertChannel(
                    SoundCategory.SIREN.takeIf { tier == EscalationTier.EMERGENCY } ?: SoundCategory.CHIME,
                    soundOption,
                    profile
                )
            }
        }

        manager.notify(tier.ordinal + 100, builder.build())
    }

    data class AlertResult(
        val message: String,
        val notifiedContacts: Int,
        val sharedLocation: Boolean,
        val overrideResult: AudioOverrideManager.OverrideResult? = null,
        val soundKey: String? = null,
        val contactId: Long? = null
    )

    companion object {
        private const val TAG = "AlertDispatcher"
        private const val SEQUENTIAL_CONTACT_DELAY_MS = 1_500L
    }
}
