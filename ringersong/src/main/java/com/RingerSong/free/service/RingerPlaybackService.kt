package com.RingerSong.free.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.provider.ContactsContract
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.RingerSong.free.R
import android.util.Log
import com.RingerSong.free.data.AppStateStore
import com.RingerSong.free.data.ProgressionEngine
import com.RingerSong.free.data.SegmentPlay
import com.RingerSong.free.data.SpotifyRemoteManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class RingerPlaybackService : Service() {
    private val scope = CoroutineScope(Dispatchers.IO)
    private var player: MediaPlayer? = null
    private var playbackJob: Job? = null
    private var stopJob: Job? = null
    private var isSpotifyPlaying = false
    private var audioManager: AudioManager? = null
    private var audioFocusRequest: AudioFocusRequest? = null
    private var originalRingerVolume: Int = -1

    companion object {
        private const val TAG = "RingerPlayback"
        const val ACTION_PLAY_SEGMENT = "com.RingerSong.free.ACTION_PLAY_SEGMENT"
        const val ACTION_STOP_PLAYBACK = "com.RingerSong.free.ACTION_STOP_PLAYBACK"
        const val EXTRA_PHONE_NUMBER = "phone_number"
        private const val NOTIFICATION_ID = 9002
        private const val CHANNEL_ID = "ringer_playback"
        private const val SPOTIFY_START_DELAY_MS = 500L // Time for Spotify to initialize playback
        private const val PREFS_NAME = "ringer_song_prefs"
        private const val KEY_MUTED_VOLUME = "muted_volume"
    }

    override fun onCreate() {
        super.onCreate()
        createChannel()
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        restoreMuteState()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "onStartCommand: action=${intent?.action}")
        when (intent?.action) {
            ACTION_PLAY_SEGMENT -> {
                val number = intent.getStringExtra(EXTRA_PHONE_NUMBER)
                Log.d(TAG, "ACTION_PLAY_SEGMENT for number: $number")
                startForeground(NOTIFICATION_ID, buildNotification("Playing your progression segment"))
                scope.launch {
                    playNextSegment(number)
                }
            }
            ACTION_STOP_PLAYBACK -> {
                Log.d(TAG, "ACTION_STOP_PLAYBACK")
                stopPlayback()
            }
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        stopPlayback()
        SpotifyRemoteManager.disconnect()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private suspend fun playNextSegment(phoneNumber: String?) {
        Log.d(TAG, "playNextSegment for phoneNumber: $phoneNumber")
        val store = AppStateStore(this)
        val contactId = phoneNumber?.let { lookupContactId(it) }
        Log.d(TAG, "Contact ID: $contactId")

        val current = store.stateFlow.first()
        Log.d(TAG, "Current state - songs: ${current.songs.size}, enabled: ${current.settings.enabled}")

        val callerKey = contactId ?: phoneNumber
        val (updated, segment) = ProgressionEngine.advance(current, contactId, callerKey)
        store.update { updated }

        if (segment == null) {
            Log.w(TAG, "No segment to play - stopping")
            stopPlayback()
            return
        }

        Log.d(TAG, "Playing segment: song=${segment.song.title}, uri=${segment.song.uri}, start=${segment.startMs}, duration=${segment.durationMs}")
        playSegment(segment)
    }

    private fun requestAudioFocus(): Boolean {
        val manager = audioManager ?: run {
            Log.e(TAG, "AudioManager is null!")
            return false
        }

        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
            .build()

        val result = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val request = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE)
                .setAudioAttributes(audioAttributes)
                .setAcceptsDelayedFocusGain(false)
                .setWillPauseWhenDucked(false)
                .build()
            audioFocusRequest = request
            manager.requestAudioFocus(request) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        } else {
            @Suppress("DEPRECATION")
            manager.requestAudioFocus(
                null,
                AudioManager.STREAM_RING,
                AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE
            ) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        }

        Log.d(TAG, "Audio focus request result: $result")
        return result
    }

    private fun abandonAudioFocus() {
        val manager = audioManager ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioFocusRequest?.let { manager.abandonAudioFocusRequest(it) }
        } else {
            @Suppress("DEPRECATION")
            manager.abandonAudioFocus(null)
        }
    }

    private fun muteRinger() {
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.MODIFY_AUDIO_SETTINGS)
            != PackageManager.PERMISSION_GRANTED) {
            Log.w(TAG, "No permission to modify audio settings")
            return
        }

        audioManager?.let { manager ->
            try {
                if (originalRingerVolume == -1) {
                    originalRingerVolume = manager.getStreamVolume(AudioManager.STREAM_RING)
                    saveMuteState(originalRingerVolume)
                }
                manager.setStreamVolume(AudioManager.STREAM_RING, 0, 0)
                Log.d(TAG, "Muted system ringer (saved volume: $originalRingerVolume)")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to mute ringer", e)
            }
        }
    }

    private fun restoreRinger() {
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.MODIFY_AUDIO_SETTINGS)
            != PackageManager.PERMISSION_GRANTED) {
            Log.w(TAG, "No permission to modify audio settings")
            return
        }

        if (originalRingerVolume != -1) {
            audioManager?.let { manager ->
                try {
                    manager.setStreamVolume(AudioManager.STREAM_RING, originalRingerVolume, 0)
                    Log.d(TAG, "Restored system ringer to $originalRingerVolume")
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to restore ringer", e)
                }
            }
            originalRingerVolume = -1
            clearMuteState()
        }
    }

    private fun saveMuteState(volume: Int) {
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putInt(KEY_MUTED_VOLUME, volume).apply()
    }

    private fun restoreMuteState() {
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val savedVolume = prefs.getInt(KEY_MUTED_VOLUME, -1)
        if (savedVolume != -1) {
            Log.d(TAG, "Found saved muted volume: $savedVolume, restoring now")
            originalRingerVolume = savedVolume
            restoreRinger()
        }
    }

    private fun clearMuteState() {
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().remove(KEY_MUTED_VOLUME).apply()
    }

    private fun playSegment(segment: SegmentPlay) {
        Log.d(TAG, "playSegment called for URI: ${segment.song.uri}")
        stopPlayback() // Ensure previous playback/mute is cleared safely

        // 1. Mute the system ringer to prevent double-audio
        muteRinger()

        // 2. Request audio focus as a backup/standard practice
        if (!requestAudioFocus()) {
            Log.w(TAG, "Failed to get audio focus, but continuing anyway")
        }

        if (segment.song.uri.startsWith("spotify:")) {
            Log.d(TAG, "Spotify URI detected")

            // PRIORITY 1: Streaming via Spotify App Remote (User's Paid Membership)
            playbackJob = scope.launch {
                var streamingSuccess = false
                runCatching {
                    Log.d(TAG, "Attempting to connect to Spotify App Remote...")
                    // Pass false for showAuthView because we are in a Service
                    val remote = SpotifyRemoteManager.connect(this@RingerPlaybackService, showAuthView = false)
                    remote.playerApi.play(segment.song.uri)
                    delay(SPOTIFY_START_DELAY_MS) // Give it a moment to start
                    remote.playerApi.seekTo(segment.startMs)
                    isSpotifyPlaying = true
                    streamingSuccess = true
                    Log.d(TAG, "Spotify App Remote playback started")
                }.onFailure { e ->
                    Log.e(TAG, "Spotify App Remote failed", e)
                }

                if (!streamingSuccess) {
                    Log.d(TAG, "Streaming failed, falling back to local file if available")
                    // PRIORITY 2: Local File (Legacy/Fallback)
                    val downloader = com.RingerSong.free.data.SpotifyDownloaderRepository(this@RingerPlaybackService)
                    val localPath = downloader.getLocalFilePathFromUri(segment.song.uri)

                    if (localPath != null) {
                         Log.d(TAG, "Found local file: $localPath")
                         withContext(Dispatchers.Main) {
                             playLocalFile(localPath, segment.startMs, segment.durationMs)
                         }
                    } else {
                        Log.e(TAG, "No local file available. Playback failed.")
                        // We must stop to restore the ringer
                        stopPlayback()
                    }
                }
            }
        } else if (segment.song.uri.startsWith("youtube:")) {
            Log.w(TAG, "YouTube URI not supported yet")
            stopPlayback()
        } else {
            Log.d(TAG, "Local file URI: ${segment.song.uri}")
            // For direct local file, we also use the main thread for consistency and callbacks
            scope.launch(Dispatchers.Main) {
                playLocalFile(segment.song.uri, segment.startMs, segment.durationMs, isUri = true)
            }
        }
    }

    // Helper to handle both file paths and URIs safely on the main thread
    private fun playLocalFile(pathOrUri: String, startMs: Long, durationMs: Long, isUri: Boolean = false) {
        val mediaPlayer = MediaPlayer()
        player = mediaPlayer
        runCatching {
            mediaPlayer.setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )

            if (isUri) {
                 mediaPlayer.setDataSource(this, Uri.parse(pathOrUri))
            } else {
                 mediaPlayer.setDataSource(pathOrUri)
            }

            // Use prepareAsync to avoid blocking Main thread, even though usually fast for local files
            mediaPlayer.setOnPreparedListener { mp ->
                Log.d(TAG, "MediaPlayer prepared")
                mp.seekTo(startMs.toInt())
                mp.start()
            }

            mediaPlayer.setOnCompletionListener {
                Log.d(TAG, "MediaPlayer completed")
                stopPlayback()
            }

            mediaPlayer.prepareAsync()

            // Schedule stop
            stopJob = scope.launch {
                delay(durationMs)
                Log.d(TAG, "Segment duration elapsed, stopping")
                stopPlayback()
            }
        }.onFailure {
            Log.e(TAG, "Failed to play local file: $pathOrUri", it)
            stopPlayback()
        }
    }

    private fun stopPlayback() {
        Log.d(TAG, "Stopping playback and restoring ringer")

        playbackJob?.cancel()
        playbackJob = null

        stopJob?.cancel()
        stopJob = null

        player?.runCatching {
            stop()
            release()
        }
        player = null

        if (isSpotifyPlaying) {
             scope.launch {
                runCatching {
                    val remote = SpotifyRemoteManager.connect(this@RingerPlaybackService, showAuthView = false)
                    remote.playerApi.pause()
                }
            }
            isSpotifyPlaying = false
        }

        abandonAudioFocus()
        restoreRinger()

        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun lookupContactId(phoneNumber: String): String? {
        val permission = ContextCompat.checkSelfPermission(
            this,
            android.Manifest.permission.READ_CONTACTS
        )
        if (permission != PackageManager.PERMISSION_GRANTED) return null
        val uri = Uri.withAppendedPath(
            ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
            Uri.encode(phoneNumber)
        )
        contentResolver.query(
            uri,
            arrayOf(ContactsContract.PhoneLookup._ID),
            null,
            null,
            null
        ).use { cursor ->
            return if (cursor != null && cursor.moveToFirst()) {
                cursor.getString(0)
            } else {
                null
            }
        }
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            "RingerSong playback",
            NotificationManager.IMPORTANCE_LOW
        )
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(channel)
    }

    private fun buildNotification(message: String): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(message)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setOngoing(true)
            .build()
    }
}
