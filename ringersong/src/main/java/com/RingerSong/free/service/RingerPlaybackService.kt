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
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class RingerPlaybackService : Service() {
    private val scope = CoroutineScope(Dispatchers.IO)
    private var player: MediaPlayer? = null
    private var stopJob: Job? = null
    private var isSpotifyPlaying = false
    private var audioManager: AudioManager? = null
    private var audioFocusRequest: AudioFocusRequest? = null
    private var originalRingerVolume: Int? = null
    private var spotifyRemote: com.spotify.android.appremote.api.SpotifyAppRemote? = null

    companion object {
        private const val TAG = "RingerPlayback"
        const val ACTION_PLAY_SEGMENT = "com.RingerSong.free.ACTION_PLAY_SEGMENT"
        const val ACTION_STOP_PLAYBACK = "com.RingerSong.free.ACTION_STOP_PLAYBACK"
        const val EXTRA_PHONE_NUMBER = "phone_number"
        private const val NOTIFICATION_ID = 9002
        private const val CHANNEL_ID = "ringer_playback"
    }

    override fun onCreate() {
        super.onCreate()
        createChannel()
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
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
        scope.cancel()
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

        // Silence default ringer
        try {
            // Only save the volume if we haven't already saved it (prevents overwriting with 0)
            if (originalRingerVolume == null) {
                originalRingerVolume = manager.getStreamVolume(AudioManager.STREAM_RING)
            }
            // Check for permission if necessary or just attempt it safely
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M || android.provider.Settings.System.canWrite(this)) {
                manager.setStreamVolume(AudioManager.STREAM_RING, 0, 0)
                Log.d(TAG, "Silenced default ringer (saved volume: $originalRingerVolume)")
            } else {
                Log.w(TAG, "Cannot silence ringer: Missing MODIFY_AUDIO_SETTINGS permission")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to silence ringer", e)
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

        // Restore ringer volume
        originalRingerVolume?.let { volume ->
            try {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M || android.provider.Settings.System.canWrite(this)) {
                    manager.setStreamVolume(AudioManager.STREAM_RING, volume, 0)
                    Log.d(TAG, "Restored ringer volume to $volume")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to restore ringer volume", e)
            }
            originalRingerVolume = null
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioFocusRequest?.let { manager.abandonAudioFocusRequest(it) }
        } else {
            @Suppress("DEPRECATION")
            manager.abandonAudioFocus(null)
        }
    }

    private fun playSegment(segment: SegmentPlay) {
        Log.d(TAG, "playSegment called for URI: ${segment.song.uri}")
        stopPlayback()

        // Request audio focus to try to duck/stop the system ringtone
        if (!requestAudioFocus()) {
            Log.w(TAG, "Failed to get audio focus, but continuing anyway")
        }

        if (segment.song.uri.startsWith("spotify:")) {
            Log.d(TAG, "Spotify URI detected")

            // PRIORITY 1: Try Spotify App Remote (Streaming) as requested by user
            // "activate users paid memberships... as the ringer"

            val attemptSpotifyRemote = suspend {
                runCatching {
                    val remote = SpotifyRemoteManager.connect(this@RingerPlaybackService)
                    spotifyRemote = remote // Keep reference for cleanup
                    remote.playerApi.play(segment.song.uri)
                    delay(500)
                    remote.playerApi.seekTo(segment.startMs)
                    isSpotifyPlaying = true
                    Log.d(TAG, "Spotify App Remote playback started")

                    // Enforce duration for Spotify
                    stopJob = scope.launch {
                        delay(segment.durationMs)
                        stopPlayback()
                    }
                    true
                }.getOrElse {
                     Log.e(TAG, "Spotify App Remote failed", it)
                     false
                }
            }

            scope.launch {
                // Try remote first
                if (attemptSpotifyRemote()) {
                    return@launch
                }

                // PRIORITY 2: Fallback to downloaded offline file
                val downloader = com.RingerSong.free.data.SpotifyDownloaderRepository(this@RingerPlaybackService)
                val localPath = downloader.getLocalFilePathFromUri(segment.song.uri)

                if (localPath != null) {
                    Log.d(TAG, "Found local file: $localPath")
                    playLocalFile(localPath, segment.startMs, segment.durationMs)
                } else {
                     Log.w(TAG, "No Spotify Remote and no local file - giving up")
                     stopPlayback()
                }
            }
        } else if (segment.song.uri.startsWith("youtube:")) {
            Log.w(TAG, "YouTube URI not supported yet")
            // YouTube Music - will need implementation
            stopPlayback()
        } else {
            Log.d(TAG, "Local file URI: ${segment.song.uri}")
            val uri = Uri.parse(segment.song.uri)
            val mediaPlayer = MediaPlayer()
            player = mediaPlayer
            runCatching {
                mediaPlayer.setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                mediaPlayer.setDataSource(this, uri)
                Log.d(TAG, "Preparing MediaPlayer")
                mediaPlayer.prepare()
                mediaPlayer.seekTo(segment.startMs.toInt())
                mediaPlayer.start()
                Log.d(TAG, "MediaPlayer started successfully")
            }.onFailure { e ->
                Log.e(TAG, "MediaPlayer failed", e)
                stopPlayback()
                return
            }

            mediaPlayer.setOnCompletionListener {
                Log.d(TAG, "MediaPlayer completed")
                stopPlayback()
            }
        }

        // Only set the default stopJob if we're NOT using Spotify Remote (which sets its own)
        // or if we fall through to local playback
        if (!segment.song.uri.startsWith("spotify:") && !segment.song.uri.startsWith("youtube:")) {
             stopJob = scope.launch {
                delay(segment.durationMs)
                Log.d(TAG, "Segment duration elapsed, stopping")
                stopPlayback()
            }
        }
    }

    private fun playLocalFile(filePath: String, startMs: Long, durationMs: Long) {
        val mediaPlayer = MediaPlayer()
        player = mediaPlayer
        runCatching {
            mediaPlayer.setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            mediaPlayer.setDataSource(filePath)
            mediaPlayer.prepare()
            mediaPlayer.seekTo(startMs.toInt())
            mediaPlayer.start()

            mediaPlayer.setOnCompletionListener {
                stopPlayback()
            }

            stopJob = scope.launch {
                delay(durationMs)
                stopPlayback()
            }
        }.onFailure {
            Log.e(TAG, "Failed to play local file: $filePath", it)
            stopPlayback()
        }
    }

    private fun stopPlayback() {
        abandonAudioFocus()
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
                    // Use existing connection if available, otherwise reconnect
                    val remote = spotifyRemote ?: SpotifyRemoteManager.connect(this@RingerPlaybackService)
                    remote.playerApi.pause()
                    SpotifyRemoteManager.disconnect(remote)
                }.onFailure {
                    Log.e(TAG, "Failed to pause/disconnect Spotify", it)
                }
                // Always clear state
                spotifyRemote = null
                isSpotifyPlaying = false
            }
        } else {
             // Ensure cleanup even if not playing but connected
             spotifyRemote?.let {
                 SpotifyRemoteManager.disconnect(it)
                 spotifyRemote = null
             }
        }

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
