package com.pulselink.util

import android.app.NotificationManager
import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.SystemClock
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
    @Volatile private var currentFocusRequest: AudioFocusRequest? = null

    fun overrideForAlert(requestDndBypass: Boolean): OverrideResult {
        val audio = audioManager ?: return OverrideResult.failure(
            OverrideResult.FailureReason.AUDIO_SERVICE_UNAVAILABLE,
            "Audio service unavailable"
        )
        Log.d(
            TAG,
            "overrideForAlert(requestDndBypass=$requestDndBypass, sdk=${Build.VERSION.SDK_INT})"
        )
        if (!prefs.getBoolean(KEY_ACTIVE, false)) {
            val currentVolume = audio.getStreamVolume(AudioManager.STREAM_RING)
            val currentNotificationVolume = audio.getStreamVolume(AudioManager.STREAM_NOTIFICATION)
            val currentAlarmVolume = audio.getStreamVolume(AudioManager.STREAM_ALARM)
            val currentMode = audio.ringerMode
            val currentFilter = notificationManager?.currentInterruptionFilter
                ?: NotificationManager.INTERRUPTION_FILTER_ALL
            prefs.edit {
                putBoolean(KEY_ACTIVE, true)
                putInt(KEY_RING_VOLUME, currentVolume)
                putInt(KEY_NOTIFICATION_VOLUME, currentNotificationVolume)
                putInt(KEY_ALARM_VOLUME, currentAlarmVolume)
                putInt(KEY_RING_MODE, currentMode)
                putInt(KEY_INTERRUPTION_FILTER, currentFilter)
                putLong(KEY_TIMESTAMP, System.currentTimeMillis())
            }
        }

        val maxVolume = audio.getStreamMaxVolume(AudioManager.STREAM_RING)
        audio.setStreamVolume(AudioManager.STREAM_RING, maxVolume, 0)
        val maxNotificationVolume = audio.getStreamMaxVolume(AudioManager.STREAM_NOTIFICATION)
        audio.setStreamVolume(AudioManager.STREAM_NOTIFICATION, maxNotificationVolume, 0)
        val maxAlarmVolume = audio.getStreamMaxVolume(AudioManager.STREAM_ALARM)
        audio.setStreamVolume(AudioManager.STREAM_ALARM, maxAlarmVolume, 0)
        waitForVolumePropagation()
        audio.ringerMode = AudioManager.RINGER_MODE_NORMAL

        var dndApplied = false
        var dndPermissionGranted = false
        var limitedByPolicy = false
        val notifManager = notificationManager
        val supportsGlobalZenOverride = Build.VERSION.SDK_INT < Build.VERSION_CODES.VANILLA_ICE_CREAM
        val desiredFilter = NotificationManager.INTERRUPTION_FILTER_ALL
        var channelBypassOnly = false
        if (requestDndBypass && supportsGlobalZenOverride &&
            notifManager?.isNotificationPolicyAccessGranted == true
        ) {
            dndPermissionGranted = true
            runCatching {
                notifManager.setInterruptionFilter(desiredFilter)
            }.onSuccess {
                val postFilter = notifManager.currentInterruptionFilter
                dndApplied = postFilter == desiredFilter || postFilter == NotificationManager.INTERRUPTION_FILTER_ALL
                limitedByPolicy = desiredFilter == NotificationManager.INTERRUPTION_FILTER_ALL &&
                    postFilter != NotificationManager.INTERRUPTION_FILTER_ALL
                if (!dndApplied) {
                    Log.w(
                        TAG,
                        "Interruption filter stayed at $postFilter while requesting $desiredFilter (sdk=${Build.VERSION.SDK_INT})"
                    )
                }
            }.onFailure { error ->
                Log.w(TAG, "Unable to modify interruption filter", error)
            }
        } else if (requestDndBypass && supportsGlobalZenOverride) {
            Log.w(TAG, "Notification policy access missing; cannot bypass DND")
        } else if (requestDndBypass) {
            channelBypassOnly = true
            Log.i(
                TAG,
                "Android 15+ relies on notification channel bypass; skipping global DND change."
            )
        }
        val message = when {
            channelBypassOnly ->
                "Android 15+ relies on channel bypass; leaving system Do Not Disturb untouched."
            requestDndBypass && supportsGlobalZenOverride && !dndPermissionGranted ->
                "Missing Notification Policy access"
            limitedByPolicy ->
                "System limited the interruption filter change"
            else -> null
        }
        return when {
            requestDndBypass && supportsGlobalZenOverride && !dndPermissionGranted ->
                OverrideResult.partial(
                    reason = OverrideResult.FailureReason.POLICY_PERMISSION_MISSING,
                    dndApplied = dndApplied,
                    limited = limitedByPolicy,
                    message = message
                )
            limitedByPolicy ->
                OverrideResult.partial(
                    reason = OverrideResult.FailureReason.POLICY_LIMITED,
                    dndApplied = dndApplied,
                    limited = limitedByPolicy,
                    message = message
                )
            else -> OverrideResult.success(dndApplied, limitedByPolicy, message)
        }.also { result ->
            Log.i(TAG, "Audio override result=$result")
        }
    }

    fun scheduleRestore(delayMillis: Long = DEFAULT_RESTORE_DELAY_MS) {
        pendingRestoreJob?.cancel()
        val effectiveDelay = preferredRestoreDelay(delayMillis)
        pendingRestoreJob = scope.launch {
            delay(effectiveDelay)
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
        val storedAlarmVolume = prefs.getInt(
            KEY_ALARM_VOLUME,
            audio.getStreamVolume(AudioManager.STREAM_ALARM)
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
        val maxAlarmVolume = audio.getStreamMaxVolume(AudioManager.STREAM_ALARM)
        audio.setStreamVolume(
            AudioManager.STREAM_ALARM,
            storedAlarmVolume.coerceIn(0, maxAlarmVolume),
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
        abandonAudioFocus()
        stopTone()
    }

    fun clear() {
        prefs.edit { clear() }
        pendingRestoreJob?.cancel()
        pendingRestoreJob = null
        stopTone()
    }

    fun playTone(soundUri: Uri?, profile: ToneProfile = ToneProfile.CheckIn) {
        if (soundUri == null) return
        synchronized(mediaLock) {
            stopToneLocked()
            try {
                requestAudioFocus(profile)
                val player = MediaPlayer().apply {
                    setAudioAttributes(
                        buildToneAudioAttributes()
                    )
                    setDataSource(context, soundUri)
                    isLooping = profile.looping
                    setVolume(1f, 1f)
                    setOnErrorListener { mp, what, extra ->
                        Log.e(TAG, "MediaPlayer error what=$what extra=$extra")
                        runCatching { mp.stop() }
                        runCatching { mp.release() }
                        synchronized(mediaLock) {
                            if (mediaPlayer === mp) {
                                mediaPlayer = null
                            }
                        }
                        true
                    }
                    setOnCompletionListener {
                        synchronized(mediaLock) {
                            if (mediaPlayer === it) {
                                mediaPlayer = null
                            }
                        }
                        runCatching { it.release() }
                        abandonAudioFocus()
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
        abandonAudioFocus()
    }

    private fun requestAudioFocus(profile: ToneProfile) {
        val audio = audioManager ?: return
        val request = AudioFocusRequest.Builder(profile.focusGain)
            .setAudioAttributes(
                buildToneAudioAttributes()
            )
            .setOnAudioFocusChangeListener { /* no-op */ }
            .setWillPauseWhenDucked(false)
            .build()
        val granted = audio.requestAudioFocus(request) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        if (granted) {
            currentFocusRequest = request
        } else {
            Log.w(TAG, "Audio focus request denied")
        }
    }

    private fun abandonAudioFocus() {
        val request = currentFocusRequest ?: return
        audioManager?.abandonAudioFocusRequest(request)
        currentFocusRequest = null
    }

    private fun waitForVolumePropagation() {
        try {
            SystemClock.sleep(75)
        } catch (ignored: Exception) {
        }
    }

    private fun buildToneAudioAttributes(): AudioAttributes {
        return AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ALARM)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
    }

    private fun preferredRestoreDelay(requestedDelay: Long): Long {
        if (requestedDelay != DEFAULT_RESTORE_DELAY_MS) return requestedDelay
        val storedFilter = prefs.getInt(KEY_INTERRUPTION_FILTER, NotificationManager.INTERRUPTION_FILTER_ALL)
        val originalDndActive = storedFilter != NotificationManager.INTERRUPTION_FILTER_ALL
        return if (originalDndActive) PRIORITY_RESTORE_DELAY_MS else DEFAULT_RESTORE_DELAY_MS
    }

    companion object {
        private const val TAG = "AudioOverrideManager"
        private const val PREF_NAME = "pulselink_audio_override"
        private const val KEY_ACTIVE = "active"
        private const val KEY_RING_VOLUME = "ring_volume"
        private const val KEY_NOTIFICATION_VOLUME = "notification_volume"
        private const val KEY_ALARM_VOLUME = "alarm_volume"
        private const val KEY_RING_MODE = "ring_mode"
        private const val KEY_INTERRUPTION_FILTER = "interruption_filter"
        private const val KEY_TIMESTAMP = "timestamp"

        private const val DEFAULT_RESTORE_DELAY_MS = 120_000L
        private const val PRIORITY_RESTORE_DELAY_MS = 25_000L
    }

    data class OverrideResult(
        val state: State,
        val dndApplied: Boolean = false,
        val limitedByPolicy: Boolean = false,
        val reason: FailureReason? = null,
        val message: String? = null
    ) {
        enum class State { SUCCESS, PARTIAL, FAILURE, SKIPPED }

        val success: Boolean get() = state == State.SUCCESS

        enum class FailureReason {
            AUDIO_SERVICE_UNAVAILABLE,
            POLICY_PERMISSION_MISSING,
            POLICY_LIMITED,
            UNKNOWN
        }

        companion object {
            fun success(dndApplied: Boolean, limited: Boolean, message: String?) =
                OverrideResult(State.SUCCESS, dndApplied, limited, reason = null, message = message)

            fun partial(
                reason: FailureReason,
                dndApplied: Boolean,
                limited: Boolean,
                message: String?
            ) =
                OverrideResult(State.PARTIAL, dndApplied, limited, reason, message)

            fun failure(reason: FailureReason, message: String?): OverrideResult =
                OverrideResult(
                    State.FAILURE,
                    dndApplied = false,
                    limitedByPolicy = false,
                    reason = reason,
                    message = message
                )

            fun skipped(): OverrideResult = OverrideResult(State.SKIPPED, dndApplied = false, limitedByPolicy = false)
        }
    }

    data class ToneProfile(
        val looping: Boolean,
        val focusGain: Int
    ) {
        companion object {
            val Emergency = ToneProfile(looping = true, focusGain = AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
            val CheckIn = ToneProfile(looping = false, focusGain = AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
            val IncomingCall = ToneProfile(looping = true, focusGain = AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE)
        }
    }
}
