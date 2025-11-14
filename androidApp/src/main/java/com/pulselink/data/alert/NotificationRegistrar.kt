package com.pulselink.data.alert

import android.app.NotificationChannel
import android.app.NotificationChannelGroup
import android.app.NotificationManager
import android.content.Context
import android.media.AudioAttributes
import android.os.Build
import android.util.Log
import androidx.core.content.getSystemService
import com.pulselink.R
import com.pulselink.domain.model.AlertProfile
import com.pulselink.domain.model.SoundCategory
import com.pulselink.domain.model.SoundOption
import com.pulselink.util.resolveUri
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationRegistrar @Inject constructor(
    @ApplicationContext private val context: Context
) {
    fun ensureChannels() {
        val manager = context.getSystemService<NotificationManager>() ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val group = NotificationChannelGroup(GROUP_ALERTS, context.getString(R.string.channel_alerts))
            manager.createNotificationChannelGroup(group)

            val background = NotificationChannel(
                CHANNEL_BACKGROUND,
                context.getString(R.string.channel_background),
                NotificationManager.IMPORTANCE_LOW
            ).apply { setGroup(GROUP_ALERTS) }
            manager.createNotificationChannel(background)
        }
    }

    fun ensureAlertChannel(
        category: SoundCategory,
        soundOption: SoundOption?,
        profile: AlertProfile
    ): String {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return when (category) {
                SoundCategory.SIREN -> LEGACY_ALERT_CHANNEL
                SoundCategory.CHIME -> LEGACY_CHECK_IN_CHANNEL
                SoundCategory.CALL -> LEGACY_CALL_CHANNEL
            }
        }
        val manager = context.getSystemService<NotificationManager>() ?: return LEGACY_ALERT_CHANNEL
        val channelId = buildChannelId(category, soundOption)
        Log.d(TAG, "Ensuring alert channel=$channelId category=$category sound=${soundOption?.key}")
        val existing = manager.getNotificationChannel(channelId)
        if (existing != null) {
            validateChannel(existing, profile)
            return channelId
        }
        val (name, importance, usage) = when (category) {
            SoundCategory.SIREN -> Triple(
                context.getString(R.string.channel_alerts),
                NotificationManager.IMPORTANCE_MAX,
                AudioAttributes.USAGE_ALARM
            )
            SoundCategory.CHIME -> Triple(
                context.getString(R.string.channel_check_ins),
                NotificationManager.IMPORTANCE_DEFAULT,
                AudioAttributes.USAGE_NOTIFICATION_EVENT
            )
            SoundCategory.CALL -> Triple(
                context.getString(R.string.channel_calls),
                NotificationManager.IMPORTANCE_HIGH,
                AudioAttributes.USAGE_NOTIFICATION_RINGTONE
            )
        }

        val channelLabel = soundOption?.label?.let { "$name - $it" } ?: name
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(usage)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        val soundUri = soundOption?.resolveUri(context)

        val channel = NotificationChannel(channelId, channelLabel, importance).apply {
            setGroup(GROUP_ALERTS)
            // On Android 15+ the interruption filter no longer disables global DND,
            // so channel-level bypass is the primary mechanism to punch through.
            setBypassDnd(profile.breakThroughDnd)
            enableVibration(profile.vibrate)
            setSound(soundUri, audioAttributes)
        }
        manager.createNotificationChannel(channel)
        manager.getNotificationChannel(channelId)?.let {
            validateChannel(it, profile)
        } ?: Log.e(TAG, "Failed to create notification channel $channelId")
        return channelId
    }

    private fun buildChannelId(category: SoundCategory, soundOption: SoundOption?): String {
        val base = when (category) {
            SoundCategory.SIREN -> "pulse_alert"
            SoundCategory.CHIME -> "pulse_checkin"
            SoundCategory.CALL -> "pulse_call"
        }
        val suffix = soundOption?.key ?: "default"
        return "${base}_$suffix"
    }

    private fun validateChannel(channel: NotificationChannel, profile: AlertProfile) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        if (profile.breakThroughDnd && !channel.canBypassDnd()) {
            val hasPolicy =
                context.getSystemService<NotificationManager>()?.isNotificationPolicyAccessGranted == true
            Log.w(
                TAG,
                "Channel ${channel.id} canBypassDnd=false (hasPolicy=$hasPolicy). User may need to re-enable bypass."
            )
        }
        val expectedImportance = if (profile.breakThroughDnd) NotificationManager.IMPORTANCE_MAX
        else NotificationManager.IMPORTANCE_DEFAULT
        if (channel.importance < expectedImportance) {
            Log.w(TAG, "Channel ${channel.id} importance downgraded to ${channel.importance}")
        }
    }

        companion object {
            private const val TAG = "NotificationRegistrar"
            private const val LEGACY_ALERT_CHANNEL = "pulse_alerts_legacy"
            private const val LEGACY_CHECK_IN_CHANNEL = "pulse_checkins_legacy"
            private const val LEGACY_CALL_CHANNEL = "pulse_call_legacy"
            const val CHANNEL_BACKGROUND = "pulse_background"
            private const val GROUP_ALERTS = "pulse_group_alerts"
        }
}
