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
import com.RingerSong.free.data.SpotifyDownloaderRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class RingerPlaybackService : Service() {

    @Inject lateinit var appStateStore: AppStateStore
    @Inject lateinit var spotifyPlayer: SpotifyPlayerManager
    @Inject lateinit var appleMusicPlayer: AppleMusicPlayerManager

    private val scope = CoroutineScope(Dispatchers.Main + Job())
    private var mediaPlayer: MediaPlayer? = null
    private var audioManager: AudioManager? = null
    private var originalRingerVolume = -1
    private var originalMusicVolume = -1
    private var isPlaying = false
    private var playbackJob: Job? = null
    private var audioFocusRequest: AudioFocusRequest? = null

    companion object {
        const val ACTION_PLAY_SEGMENT = "com.RingerSong.action.PLAY_SEGMENT"
        const val ACTION_STOP_PLAYBACK = "com.RingerSong.action.STOP_PLAYBACK"
        const val EXTRA_PHONE_NUMBER = "com.RingerSong.extra.PHONE_NUMBER"
        const val EXTRA_ORIGINAL_VOLUME = "com.RingerSong.extra.ORIGINAL_VOLUME"
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
                val passedOriginalVolume = intent.getIntExtra(EXTRA_ORIGINAL_VOLUME, -1)

                Log.d(TAG, "Starting playback for $phoneNumber. Passed vol: $passedOriginalVolume")

                startForeground(NOTIFICATION_ID, buildNotification())
                silenceSystemRinger(passedOriginalVolume)
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

    private fun silenceSystemRinger(passedVolume: Int = -1) {
        audioManager?.let { am ->
            // Only capture if we haven't already (to prevent capturing 0)
            if (originalRingerVolume == -1) {
                if (passedVolume != -1) {
                    originalRingerVolume = passedVolume
                    Log.d(TAG, "Using passed original ringer volume: $originalRingerVolume")
                } else {
                    originalRingerVolume = am.getStreamVolume(AudioManager.STREAM_RING)
                    Log.d(TAG, "Captured original ringer volume: $originalRingerVolume")
                }

                // Sync Music Volume to Ringer Volume (so user can hear the music even if media is muted)
                syncMusicVolumeToRingerLevel(am, originalRingerVolume)
            } else {
                Log.d(TAG, "Already captured ringer volume: $originalRingerVolume")
            }

            // We set STREAM_RING to 0 to silence the default ringer.
            try {
                // Check if it's already 0 (silenced by Receiver)
                if (am.getStreamVolume(AudioManager.STREAM_RING) != 0) {
                    am.setStreamVolume(AudioManager.STREAM_RING, 0, 0)
                    Log.d(TAG, "Silenced system ringer")
                }

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
                audioFocusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE)
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .build()
                    )
                    .setAcceptsDelayedFocusGain(false)
                    .build()
                audioFocusRequest?.let { am.requestAudioFocus(it) }
            } else {
                @Suppress("DEPRECATION")
                am.requestAudioFocus(
                    null,
                    AudioManager.STREAM_MUSIC,
                    AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE
                )
            }
        }
    }

    private fun abandonAudioFocus() {
         audioManager?.let { am ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                audioFocusRequest?.let {
                    am.abandonAudioFocusRequest(it)
                    audioFocusRequest = null
                }
            } else {
                @Suppress("DEPRECATION")
                am.abandonAudioFocus(null)
            }
        }
    }

    private fun restoreSystemRinger() {
        try {
            if (originalRingerVolume != -1) {
                audioManager?.setStreamVolume(AudioManager.STREAM_RING, originalRingerVolume, 0)
                Log.d(TAG, "Restored system ringer to $originalRingerVolume")
                originalRingerVolume = -1
            }

            if (originalMusicVolume != -1) {
                audioManager?.setStreamVolume(AudioManager.STREAM_MUSIC, originalMusicVolume, 0)
                Log.d(TAG, "Restored music volume to $originalMusicVolume")
                originalMusicVolume = -1
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error restoring ringer/music volume", e)
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

            // Use ProgressionEngine to get the correct segment to play
            val (updatedState, segmentPlay) = com.RingerSong.free.data.ProgressionEngine.advance(
                state,
                contactId = null, // Could look up by phoneNumber if needed
                callerKey = phoneNumber
            )

            // Save the updated state (includes incremented segment index)
            appStateStore.update { updatedState }

            if (segmentPlay == null) {
                Log.w(TAG, "No segment to play")
                stopSelf()
                return@launch
            }

            val song = segmentPlay.song
            Log.d(TAG, "Playing segment: ${song.title} from ${segmentPlay.startMs}ms for ${segmentPlay.durationMs}ms")

            when (song.source) {
                SongSource.SPOTIFY -> playSpotifySong(song, segmentPlay.startMs, segmentPlay.durationMs)
                SongSource.LOCAL -> playLocalSong(song, segmentPlay.startMs, segmentPlay.durationMs)
                SongSource.YOUTUBE_MUSIC -> playYouTubeSong(song, segmentPlay.startMs, segmentPlay.durationMs)
                SongSource.APPLE_MUSIC -> playAppleMusicSong(song, segmentPlay.startMs, segmentPlay.durationMs)
                else -> {
                    Log.w(TAG, "Unsupported song source: ${song.source}")
                    stopSelf()
                }
            }
        }
    }

    private suspend fun playSpotifySong(song: SongEntry, startMs: Long, durationMs: Long) {
        Log.d(TAG, "Attempting to stream Spotify track: ${song.title} (${song.uri})")

        // Try streaming using Spotify App Remote
        val streamSuccess = spotifyPlayer.playUri(song.uri, startMs)

        if (streamSuccess) {
            Log.d(TAG, "Spotify streaming started")
            isPlaying = true

            // Schedule stop
            playbackJob = scope.launch {
                delay(durationMs)
                Log.d(TAG, "Segment playback complete after ${durationMs}ms")
                stopPlayback()
                restoreSystemRinger()
                stopForeground(true)
                stopSelf()
            }
            return
        }

        Log.e(TAG, "Spotify streaming failed: ${song.title} (${song.uri})")
        stopPlayback()
        restoreSystemRinger()
        stopForeground(true)
        stopSelf()
    }

    private fun playLocalSong(song: SongEntry, startMs: Long, durationMs: Long) {
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
                    // Seek to the start of this segment
                    mp.seekTo(startMs.toInt())
                    mp.start()
                    // Don't loop - we want to play only this segment
                    mp.isLooping = false
                    this@RingerPlaybackService.isPlaying = true

                    // Schedule stop after durationMs with cancellable job
                    playbackJob = scope.launch {
                        delay(durationMs)
                        Log.d(TAG, "Segment playback complete after ${durationMs}ms")
                        stopPlayback()
                        restoreSystemRinger()
                        stopForeground(true)
                        stopSelf()
                    }
                }
                setOnCompletionListener {
                    Log.d(TAG, "MediaPlayer completed playback")
                    stopPlayback()
                    restoreSystemRinger()
                    stopForeground(true)
                    stopSelf()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error playing local song", e)
        }
    }

    private suspend fun playYouTubeSong(song: SongEntry, startMs: Long, durationMs: Long) {
        // Extract the video ID from the URI (format: "youtube:video:VIDEO_ID")
        val videoId = song.uri.removePrefix("youtube:video:")
        val youtubeMusicRepo = com.RingerSong.free.data.YouTubeMusicRepository(this)

        Log.d(TAG, "Attempting to stream YouTube track: $videoId")

        // Fetch stream URL via Repository (RapidAPI)
        // This keeps it "streaming" (no local file import) but allows us to use MediaPlayer
        // which gives us start/stop control, unlike launching the external app.
        val details = youtubeMusicRepo.getSongDetails(videoId)
        val streamUrl = details?.downloadUrl

        if (!streamUrl.isNullOrBlank()) {
            Log.d(TAG, "Streaming YouTube URL: $streamUrl")
            // Stream the URL using MediaPlayer
            playLocalSong(song.copy(uri = streamUrl), startMs, durationMs)
            return
        }

        Log.e(TAG, "YouTube track stream unavailable: $videoId")
        stopSelf()
    }

    private suspend fun playAppleMusicSong(song: SongEntry, startMs: Long, durationMs: Long) {
        Log.d(TAG, "Attempting to play Apple Music song: ${song.title}")
        val success = appleMusicPlayer.playTrack(song)
        if (success) {
            // Since we just launched the app, we can't easily control duration/stop.
            // But we can schedule a stopSelf to cleanup the service.
            // The Apple Music app will continue playing.
            // This is "best effort".
            isPlaying = true
            playbackJob = scope.launch {
                delay(durationMs)
                Log.d(TAG, "Apple Music segment duration passed")
                // We cannot stop Apple Music, but we stop our service and restore ringer
                stopPlayback()
                restoreSystemRinger()
                stopForeground(true)
                stopSelf()
            }
        } else {
            Log.e(TAG, "Failed to launch Apple Music")
            stopSelf()
        }
    }

    private fun stopPlayback() {
        Log.d(TAG, "Executing stopPlayback")

        // Cancel any pending stop/cleanup jobs
        playbackJob?.cancel()
        playbackJob = null

        // Ensure we pause Spotify explicitly - simplified logic
        spotifyPlayer.pause()

        // Attempt to stop external players (like Apple Music) by sending a media pause key event
        // This is a workaround since we don't control the external app directly.
        try {
            audioManager?.let { am ->
                val eventTime = android.os.SystemClock.uptimeMillis()
                val downEvent = android.view.KeyEvent(eventTime, eventTime, android.view.KeyEvent.ACTION_DOWN, android.view.KeyEvent.KEYCODE_MEDIA_PAUSE, 0)
                val upEvent = android.view.KeyEvent(eventTime, eventTime, android.view.KeyEvent.ACTION_UP, android.view.KeyEvent.KEYCODE_MEDIA_PAUSE, 0)

                am.dispatchMediaKeyEvent(downEvent)
                am.dispatchMediaKeyEvent(upEvent)
                Log.d(TAG, "Dispatched MEDIA_PAUSE key event to stop external players")
            }
        } catch (e: Exception) {
             Log.e(TAG, "Failed to dispatch media key event", e)
        }

        // Cleanup media player
        if (mediaPlayer != null) {
            mediaPlayer?.stop()
            mediaPlayer?.release()
            mediaPlayer = null
        }

        isPlaying = false
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
        Log.d(TAG, "Service Destroyed")
        stopPlayback() // Ensure cleanup happens even if destroyed unexpectedly
        scope.cancel()
        restoreSystemRinger() // Safety net
        super.onDestroy()
    }
}
