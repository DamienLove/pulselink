package com.pulselink.data.alert

import android.app.NotificationChannel
import android.app.NotificationChannelGroup
import android.app.NotificationManager
import android.media.AudioAttributes
import android.content.Context
import android.os.Build
import androidx.core.content.getSystemService
import com.pulselink.R
import com.pulselink.domain.model.AlertProfile
import com.pulselink.domain.model.SoundCategory
import com.pulselink.domain.model.SoundOption
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
            }
        }
        val manager = context.getSystemService<NotificationManager>() ?: return LEGACY_ALERT_CHANNEL
        val channelId = buildChannelId(category, soundOption)
        val existing = manager.getNotificationChannel(channelId)
        if (existing != null) {
            return channelId
        }
        val (name, importance, usage) = when (category) {
            SoundCategory.SIREN -> Triple(
                context.getString(R.string.channel_alerts),
                NotificationManager.IMPORTANCE_HIGH,
                AudioAttributes.USAGE_ALARM
            )
            SoundCategory.CHIME -> Triple(
                context.getString(R.string.channel_check_ins),
                NotificationManager.IMPORTANCE_DEFAULT,
                AudioAttributes.USAGE_NOTIFICATION_EVENT
            )
        }

        val channelLabel = soundOption?.label?.let { "$name - $it" } ?: name
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(usage)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        val soundUri = soundOption?.let {
            android.net.Uri.parse("android.resource://${context.packageName}/${it.resId}")
        }

        val channel = NotificationChannel(channelId, channelLabel, importance).apply {
            setGroup(GROUP_ALERTS)
            setBypassDnd(profile.breakThroughDnd)
            enableVibration(profile.vibrate)
            setSound(soundUri, audioAttributes)
        }
        manager.createNotificationChannel(channel)
        return channelId
    }

    private fun buildChannelId(category: SoundCategory, soundOption: SoundOption?): String {
        val base = when (category) {
            SoundCategory.SIREN -> "pulse_alert"
            SoundCategory.CHIME -> "pulse_checkin"
        }
        val suffix = soundOption?.key ?: "default"
        return "${base}_$suffix"
    }

    companion object {
        private const val LEGACY_ALERT_CHANNEL = "pulse_alerts_legacy"
        private const val LEGACY_CHECK_IN_CHANNEL = "pulse_checkins_legacy"
        const val CHANNEL_BACKGROUND = "pulse_background"
        private const val GROUP_ALERTS = "pulse_group_alerts"
    }
}
