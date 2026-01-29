package com.RingerSong.free.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.telephony.TelephonyManager
import android.util.Log
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ProcessLifecycleOwner
import com.RingerSong.free.data.AppStateStore
import com.RingerSong.free.service.RingerPlaybackService
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class CallStateReceiver : BroadcastReceiver() {

    @Inject lateinit var appStateStore: AppStateStore

    companion object {
        private const val TAG = "CallStateReceiver"
        private var lastState = TelephonyManager.EXTRA_STATE_IDLE
    }

    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "onReceive: action=${intent.action}")
        if (intent.action != TelephonyManager.ACTION_PHONE_STATE_CHANGED) return

        // If we hold the Call Screening role, the RingerCallScreeningService will handle everything.
        // We should skip execution here to avoid double-handling (silencing fights, double playback start).
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roleManager = context.getSystemService(Context.ROLE_SERVICE) as? android.app.role.RoleManager
            if (roleManager != null && roleManager.isRoleHeld(android.app.role.RoleManager.ROLE_CALL_SCREENING)) {
                Log.d(TAG, "Skipping Receiver: ROLE_CALL_SCREENING is held, Service will handle.")
                return
            }
        }

        val phoneState = intent.getStringExtra(TelephonyManager.EXTRA_STATE) ?: return
        Log.d(TAG, "Phone state: $phoneState (last: $lastState)")

        when (phoneState) {
            TelephonyManager.EXTRA_STATE_RINGING -> {
                val number = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER)
                Log.d(TAG, "RINGING from: $number - Starting RingerPlaybackService")

                val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? android.media.AudioManager
                var originalVolume = -1

                // Retry logic for silencing ringer
                // Sometimes the system resets volume immediately after ringing starts, so we need to be persistent
                if (audioManager != null) {
                    try {
                        originalVolume = audioManager.getStreamVolume(android.media.AudioManager.STREAM_RING)

                        // Loop a few times to ensure silence sticks
                        CoroutineScope(Dispatchers.Main).launch {
                            repeat(3) {
                                try {
                                    audioManager.setStreamVolume(android.media.AudioManager.STREAM_RING, 0, 0)
                                } catch (e: Exception) {
                                    Log.e(TAG, "Error silencing ringer attempt $it", e)
                                }
                                kotlinx.coroutines.delay(100)
                            }
                        }
                        // Initial silence
                        audioManager.setStreamVolume(android.media.AudioManager.STREAM_RING, 0, 0)

                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to capture/silence ringer", e)
                    }
                }

                val serviceIntent = Intent(context, RingerPlaybackService::class.java).apply {
                    action = RingerPlaybackService.ACTION_PLAY_SEGMENT
                    putExtra(RingerPlaybackService.EXTRA_PHONE_NUMBER, number)
                    if (originalVolume != -1) {
                        putExtra(RingerPlaybackService.EXTRA_ORIGINAL_VOLUME, originalVolume)
                    }
                }

                try {
                    if (!canStartServiceFromBackground(context)) {
                        Log.w(TAG, "Skipping playback service start; app not in foreground and missing overlay permission.")
                        // Restore volume if we can't start!
                        if (originalVolume != -1) {
                             audioManager?.setStreamVolume(android.media.AudioManager.STREAM_RING, originalVolume, 0)
                        }
                        return
                    }

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        context.startForegroundService(serviceIntent)
                    } else {
                        context.startService(serviceIntent)
                    }
                } catch (e: Throwable) {
                    Log.e(TAG, "Failed to start service", e)
                    // Failsafe: Restore volume if service start failed
                    if (originalVolume != -1) {
                        try {
                            audioManager?.setStreamVolume(android.media.AudioManager.STREAM_RING, originalVolume, 0)
                        } catch (restoreEx: Exception) {
                            Log.e(TAG, "Failed to restore volume in failsafe", restoreEx)
                        }
                    }
                }
            }

            TelephonyManager.EXTRA_STATE_IDLE -> {
                // Call ended or rejected
                if (lastState == TelephonyManager.EXTRA_STATE_RINGING ||
                    lastState == TelephonyManager.EXTRA_STATE_OFFHOOK) {

                    Log.d(TAG, "Call ended - Stopping RingerPlaybackService")
                    val serviceIntent = Intent(context, RingerPlaybackService::class.java).apply {
                        action = RingerPlaybackService.ACTION_STOP_PLAYBACK
                    }
                    try {
                        context.startService(serviceIntent)
                    } catch (e: Throwable) {
                        Log.e(TAG, "Failed to send STOP intent", e)
                    }

                }
            }

            TelephonyManager.EXTRA_STATE_OFFHOOK -> {
                // User answered the call. Stop ringing.
                Log.d(TAG, "Call Answered - Stopping RingerPlaybackService")
                val serviceIntent = Intent(context, RingerPlaybackService::class.java).apply {
                    action = RingerPlaybackService.ACTION_STOP_PLAYBACK
                }
                try {
                    // For stopping, we try to startService.
                    // Note: Stopping doesn't strictly require foreground start permissions if we are just sending an intent,
                    // but on Android O+ background start limits apply.
                    // However, if we were already running foreground, we should be fine?
                    // Actually, if the service is running, startService is allowed.
                    try {
                        context.startService(serviceIntent)
                    } catch (e: Exception) {
                        Log.w(TAG, "Could not send stop intent: ${e.message}")
                    }
                } catch (e: Throwable) {
                    Log.e(TAG, "Failed to send STOP intent", e)
                }
            }
        }

        lastState = phoneState
    }

    private fun canStartServiceFromBackground(context: Context): Boolean {
        // If we have overlay permission, we are exempt from background start restrictions on Android 10+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
            android.provider.Settings.canDrawOverlays(context)) {
            return true
        }

        // If targeting Android 12+ (S) and no overlay permission, we must be in foreground
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val isInForeground = ProcessLifecycleOwner.get().lifecycle.currentState
                .isAtLeast(Lifecycle.State.STARTED)
            if (!isInForeground) {
                 Log.e(TAG, "Cannot start RingerPlaybackService: App is in background and missing SYSTEM_ALERT_WINDOW (Overlay) permission.")
            }
            return isInForeground
        }

        return true
    }
}
