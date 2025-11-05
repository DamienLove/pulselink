package com.pulselink.util

import android.app.NotificationManager
import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import android.util.Log
import androidx.core.content.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Singleton
class AudioOverrideManager @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val audioManager: AudioManager? = context.getSystemService(AudioManager::class.java)
    private val notificationManager: NotificationManager? =
        context.getSystemService(NotificationManager::class.java)
    private val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    @Volatile private var pendingRestoreJob: Job? = null
    @Volatile private var mediaPlayer: MediaPlayer? = null
    private val mediaLock = Any()

    fun overrideForAlert(requestDndBypass: Boolean): Boolean {
        val audio = audioManager ?: return false
        if (!prefs.getBoolean(KEY_ACTIVE, false)) {
            val currentVolume = audio.getStreamVolume(AudioManager.STREAM_RING)
            val currentNotificationVolume = audio.getStreamVolume(AudioManager.STREAM_NOTIFICATION)
            val currentMode = audio.ringerMode
            val currentFilter = notificationManager?.currentInterruptionFilter
                ?: NotificationManager.INTERRUPTION_FILTER_ALL
            prefs.edit {
                putBoolean(KEY_ACTIVE, true)
                putInt(KEY_RING_VOLUME, currentVolume)
                putInt(KEY_NOTIFICATION_VOLUME, currentNotificationVolume)
                putInt(KEY_RING_MODE, currentMode)
                putInt(KEY_INTERRUPTION_FILTER, currentFilter)
                putLong(KEY_TIMESTAMP, System.currentTimeMillis())
            }
        }

        val maxVolume = audio.getStreamMaxVolume(AudioManager.STREAM_RING)
        audio.setStreamVolume(AudioManager.STREAM_RING, maxVolume, 0)
        val maxNotificationVolume = audio.getStreamMaxVolume(AudioManager.STREAM_NOTIFICATION)
        audio.setStreamVolume(AudioManager.STREAM_NOTIFICATION, maxNotificationVolume, 0)
        audio.ringerMode = AudioManager.RINGER_MODE_NORMAL

        if (requestDndBypass && notificationManager?.isNotificationPolicyAccessGranted == true) {
            notificationManager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALL)
        }
        return true
    }

    fun scheduleRestore(delayMillis: Long = DEFAULT_RESTORE_DELAY_MS) {
        pendingRestoreJob?.cancel()
        pendingRestoreJob = scope.launch {
            delay(delayMillis)
            restoreIfNeeded()
        }
    }

    fun cancelScheduledRestore() {
        pendingRestoreJob?.cancel()
        pendingRestoreJob = null
        stopTone()
        restoreIfNeeded()
    }

    fun restoreIfNeeded() {
        val audio = audioManager ?: return
        val wasActive = prefs.getBoolean(KEY_ACTIVE, false)
        if (!wasActive) return

        val storedVolume = prefs.getInt(KEY_RING_VOLUME, audio.getStreamVolume(AudioManager.STREAM_RING))
        val storedNotificationVolume = prefs.getInt(
            KEY_NOTIFICATION_VOLUME,
            audio.getStreamVolume(AudioManager.STREAM_NOTIFICATION)
        )
        val storedMode = prefs.getInt(KEY_RING_MODE, audio.ringerMode)
        val storedFilter = prefs.getInt(KEY_INTERRUPTION_FILTER, NotificationManager.INTERRUPTION_FILTER_ALL)

        val maxVolume = audio.getStreamMaxVolume(AudioManager.STREAM_RING)
        audio.setStreamVolume(AudioManager.STREAM_RING, storedVolume.coerceIn(0, maxVolume), 0)
        val maxNotificationVolume = audio.getStreamMaxVolume(AudioManager.STREAM_NOTIFICATION)
        audio.setStreamVolume(
            AudioManager.STREAM_NOTIFICATION,
            storedNotificationVolume.coerceIn(0, maxNotificationVolume),
            0
        )
        audio.ringerMode = storedMode

        notificationManager?.let { nm ->
            if (nm.isNotificationPolicyAccessGranted) {
                nm.setInterruptionFilter(storedFilter)
            }
        }

        prefs.edit { clear() }
        pendingRestoreJob?.cancel()
        pendingRestoreJob = null
        stopTone()
    }

    fun clear() {
        prefs.edit { clear() }
        pendingRestoreJob?.cancel()
        pendingRestoreJob = null
        stopTone()
    }

    fun playTone(soundUri: Uri?, looping: Boolean = false) {
        if (soundUri == null) return
        synchronized(mediaLock) {
            stopToneLocked()
            try {
                val player = MediaPlayer().apply {
                    setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_ALARM)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                    setDataSource(context, soundUri)
                    isLooping = looping
                    setOnCompletionListener {
                        synchronized(mediaLock) {
                            if (mediaPlayer === it) {
                                mediaPlayer = null
                            }
                        }
                        runCatching { it.release() }
                    }
                    prepare()
                    start()
                }
                mediaPlayer = player
            } catch (error: Exception) {
                Log.e(TAG, "Unable to play override tone", error)
                stopToneLocked()
            }
        }
    }

    fun stopTone() {
        synchronized(mediaLock) {
            stopToneLocked()
        }
    }

    private fun stopToneLocked() {
        mediaPlayer?.let {
            runCatching {
                if (it.isPlaying) {
                    it.stop()
                }
            }
            runCatching { it.release() }
        }
        mediaPlayer = null
    }

    companion object {
        private const val TAG = "AudioOverrideManager"
        private const val PREF_NAME = "pulselink_audio_override"
        private const val KEY_ACTIVE = "active"
        private const val KEY_RING_VOLUME = "ring_volume"
        private const val KEY_NOTIFICATION_VOLUME = "notification_volume"
        private const val KEY_RING_MODE = "ring_mode"
        private const val KEY_INTERRUPTION_FILTER = "interruption_filter"
        private const val KEY_TIMESTAMP = "timestamp"

        private const val DEFAULT_RESTORE_DELAY_MS = 120_000L
    }
}
