package com.RingerSong.free.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.RingerSong.free.R
import com.RingerSong.free.data.AppStateStore
import com.RingerSong.free.data.SongEntry
import com.RingerSong.free.data.SongSource
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class RingerPlaybackService : Service() {

    @Inject lateinit var appStateStore: AppStateStore
    @Inject lateinit var spotifyPlayer: SpotifyPlayerManager

    private val scope = CoroutineScope(Dispatchers.Main + Job())
    private var mediaPlayer: MediaPlayer? = null
    private var audioManager: AudioManager? = null
    private var originalRingerVolume = -1
    private var originalMusicVolume = -1
    private var isPlaying = false

    companion object {
        const val ACTION_PLAY_SEGMENT = "com.RingerSong.action.PLAY_SEGMENT"
        const val ACTION_STOP_PLAYBACK = "com.RingerSong.action.STOP_PLAYBACK"
        const val EXTRA_PHONE_NUMBER = "com.RingerSong.extra.PHONE_NUMBER"
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "ringer_playback"
        private const val TAG = "RingerPlaybackService"
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_PLAY_SEGMENT -> {
                val phoneNumber = intent.getStringExtra(EXTRA_PHONE_NUMBER)
                Log.d(TAG, "Starting playback for $phoneNumber")
                startForeground(NOTIFICATION_ID, buildNotification())
                silenceSystemRinger()
                playRandomSong(phoneNumber)
            }
            ACTION_STOP_PLAYBACK -> {
                Log.d(TAG, "Stopping playback")
                stopPlayback()
                restoreSystemRinger()
                stopForeground(true)
                stopSelf()
            }
        }
        return START_NOT_STICKY
    }

    private fun silenceSystemRinger() {
        audioManager?.let { am ->
            // Only capture if we haven't already (to prevent capturing 0)
            if (originalRingerVolume == -1) {
                originalRingerVolume = am.getStreamVolume(AudioManager.STREAM_RING)
                Log.d(TAG, "Captured original ringer volume: $originalRingerVolume")

                // Sync Music Volume to Ringer Volume (so user can hear the music even if media is muted)
                syncMusicVolumeToRingerLevel(am, originalRingerVolume)
            } else {
                Log.d(TAG, "Already captured ringer volume: $originalRingerVolume")
            }

            // We set STREAM_RING to 0 to silence the default ringer.
            try {
                am.setStreamVolume(AudioManager.STREAM_RING, 0, 0)
                Log.d(TAG, "Silenced system ringer")

                // Double check
                val currentVol = am.getStreamVolume(AudioManager.STREAM_RING)
                if (currentVol != 0) {
                    Log.w(TAG, "Failed to silence ringer completely. Current vol: $currentVol")
                }
            } catch (e: SecurityException) {
                Log.e(TAG, "SecurityException silencing ringer: ${e.message}")
            }

            // Request Audio Focus to ensure our music plays over anything else
            requestAudioFocus()
        }
    }

    private fun syncMusicVolumeToRingerLevel(am: AudioManager, targetRingerVol: Int) {
        try {
            val ringerMax = am.getStreamMaxVolume(AudioManager.STREAM_RING)
            val musicMax = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC)

            // Avoid division by zero
            if (ringerMax == 0) return

            val ratio = targetRingerVol.toFloat() / ringerMax.toFloat()
            val targetMusicVol = (ratio * musicMax).toInt()

            // Ensure at least some volume if ringer was on (e.g. if ratio is small but non-zero)
            val finalVol = if (targetRingerVol > 0 && targetMusicVol == 0) 1 else targetMusicVol

            // Capture original music volume
            if (originalMusicVolume == -1) {
                originalMusicVolume = am.getStreamVolume(AudioManager.STREAM_MUSIC)
            }

            am.setStreamVolume(AudioManager.STREAM_MUSIC, finalVol, 0)
            Log.d(TAG, "Synced music volume to $finalVol (ringer was $targetRingerVol/$ringerMax)")
        } catch (e: Exception) {
            Log.e(TAG, "Error syncing music volume", e)
        }
    }

    private fun requestAudioFocus() {
        audioManager?.let { am ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val focusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .build()
                    )
                    .build()
                am.requestAudioFocus(focusRequest)
            } else {
                @Suppress("DEPRECATION")
                am.requestAudioFocus(
                    null,
                    AudioManager.STREAM_MUSIC,
                    AudioManager.AUDIOFOCUS_GAIN_TRANSIENT
                )
            }
        }
    }

    private fun abandonAudioFocus() {
         audioManager?.let { am ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val focusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT).build()
                am.abandonAudioFocusRequest(focusRequest)
            } else {
                @Suppress("DEPRECATION")
                am.abandonAudioFocus(null)
            }
        }
    }

    private fun restoreSystemRinger() {
        if (originalRingerVolume != -1) {
            try {
                audioManager?.setStreamVolume(AudioManager.STREAM_RING, originalRingerVolume, 0)
                Log.d(TAG, "Restored system ringer to $originalRingerVolume")
            } catch (e: Exception) {
                Log.e(TAG, "Error restoring ringer", e)
            }
            originalRingerVolume = -1
        }

        if (originalMusicVolume != -1) {
            try {
                audioManager?.setStreamVolume(AudioManager.STREAM_MUSIC, originalMusicVolume, 0)
                Log.d(TAG, "Restored music volume to $originalMusicVolume")
            } catch (e: Exception) {
                Log.e(TAG, "Error restoring music volume", e)
            }
            originalMusicVolume = -1
        }
        abandonAudioFocus()
    }

    private fun playRandomSong(phoneNumber: String?) {
        scope.launch {
            val state = appStateStore.stateFlow.first()
            if (state.songs.isEmpty()) {
                Log.w(TAG, "No songs selected")
                stopSelf()
                return@launch
            }

            // Determine which song to play
            val song: SongEntry = if (state.settings.shuffle) {
                state.songs.random()
            } else {
                // Progression logic: Use the saved global playlist index
                val currentIndex = state.playback.globalPlaylistIndex
                // Ensure index is valid
                val validIndex = if (currentIndex in state.songs.indices) currentIndex else 0
                val nextSong = state.songs[validIndex]

                // Advance the index for next time (loop back to 0 if at end)
                val nextIndex = (validIndex + 1) % state.songs.size
                appStateStore.update { it.copy(playback = it.playback.copy(globalPlaylistIndex = nextIndex)) }

                nextSong
            }

            Log.d(TAG, "Selected song: ${song.title} (${song.source})")

            when (song.source) {
                SongSource.SPOTIFY -> playSpotifySong(song)
                SongSource.LOCAL -> playLocalSong(song)
                SongSource.YOUTUBE_MUSIC -> {
                    Log.w(TAG, "YouTube Music playback not supported directly in service")
                    // Could try fallback to local or Spotify if available?
                }
            }
        }
    }

    private fun playSpotifySong(song: SongEntry) {
        // Spotify App Remote plays on the Spotify app (STREAM_MUSIC), so it works fine while Ring is 0.
        scope.launch {
            // connect(false) is called inside playUri if needed
            val success = spotifyPlayer.playUri(song.uri)
            if (!success) {
                Log.e(TAG, "Spotify playback failed")
                // Fallback logic could go here
            } else {
                isPlaying = true
            }
        }
    }

    private fun playLocalSong(song: SongEntry) {
        if (song.uri.isEmpty()) {
            Log.e(TAG, "Local song URI is empty")
            return
        }
        try {
            mediaPlayer = MediaPlayer().apply {
                setDataSource(song.uri)
                // Use USAGE_MEDIA to play on STREAM_MUSIC, bypassing the silenced STREAM_RING.
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                prepareAsync()
                setOnPreparedListener { mp ->
                    // Logic to start from a specific point (0 for now as Model doesn't support offsets yet)
                    mp.seekTo(0)
                    mp.start()
                    mp.isLooping = true // Ringtones should loop
                    this@RingerPlaybackService.isPlaying = true
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error playing local song", e)
        }
    }

    private fun stopPlayback() {
        if (isPlaying) {
            spotifyPlayer.pause() // Or disconnect
            mediaPlayer?.stop()
            mediaPlayer?.release()
            mediaPlayer = null
            isPlaying = false
        }
    }

    private fun buildNotification() = NotificationCompat.Builder(this, CHANNEL_ID)
        .setContentTitle("RingerSong Active")
        .setContentText("Playing ringtone...")
        .setSmallIcon(R.drawable.ic_music_note) // Ensure this exists
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .build()

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Ringer Playback",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        scope.cancel()
        restoreSystemRinger() // Safety net
        super.onDestroy()
    }
}
