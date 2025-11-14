package com.pulselink.util

import android.content.Context
import android.media.RingtoneManager
import android.provider.Settings
import android.net.Uri
import com.pulselink.domain.model.SOUND_RES_PHONE_DEFAULT_NOTIFICATION
import com.pulselink.domain.model.SOUND_RES_PHONE_DEFAULT_RINGTONE
import com.pulselink.domain.model.SoundOption

fun SoundOption.resolveUri(context: Context): Uri? {
    return when (resId) {
        SOUND_RES_PHONE_DEFAULT_NOTIFICATION ->
            RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                ?: Settings.System.DEFAULT_NOTIFICATION_URI
        SOUND_RES_PHONE_DEFAULT_RINGTONE ->
            RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
                ?: Settings.System.DEFAULT_RINGTONE_URI
        else -> Uri.parse("android.resource://${context.packageName}/${resId}")
    }
}
