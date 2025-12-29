package com.RingerSong.free.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.TelephonyManager
import android.util.Log
import androidx.core.content.ContextCompat
import com.RingerSong.free.data.AppStateStore
import com.RingerSong.free.service.RingerPlaybackService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class CallStateReceiver : BroadcastReceiver() {
    companion object {
        private const val TAG = "CallStateReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "onReceive: action=${intent.action}")
        if (intent.action != TelephonyManager.ACTION_PHONE_STATE_CHANGED) return

        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            val store = AppStateStore(context)
            val state = store.stateFlow.first()
            Log.d(TAG, "RingerSong enabled: ${state.settings.enabled}, songs: ${state.songs.size}")

            if (!state.settings.enabled) {
                Log.w(TAG, "RingerSong is disabled - not playing")
                pending.finish()
                return@launch
            }

            val phoneState = intent.getStringExtra(TelephonyManager.EXTRA_STATE)
            Log.d(TAG, "Phone state: $phoneState")

            when (phoneState) {
                TelephonyManager.EXTRA_STATE_RINGING -> {
                    val number = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER)
                    Log.d(TAG, "RINGING - Starting playback service for number: $number")
                    val serviceIntent = Intent(context, RingerPlaybackService::class.java).apply {
                        action = RingerPlaybackService.ACTION_PLAY_SEGMENT
                        putExtra(RingerPlaybackService.EXTRA_PHONE_NUMBER, number)
                    }
                    ContextCompat.startForegroundService(context, serviceIntent)
                }
                TelephonyManager.EXTRA_STATE_IDLE,
                TelephonyManager.EXTRA_STATE_OFFHOOK -> {
                    Log.d(TAG, "Call ended or answered - Stopping playback")
                    val stopIntent = Intent(context, RingerPlaybackService::class.java).apply {
                        action = RingerPlaybackService.ACTION_STOP_PLAYBACK
                    }
                    context.startService(stopIntent)
                }
            }
            pending.finish()
        }
    }
}
