package com.RingerSong.free.service

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import com.RingerSong.free.data.SongEntry
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppleMusicPlayerManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "AppleMusicPlayer"
        private const val APPLE_MUSIC_PACKAGE = "com.apple.android.music"
    }

    /**
     * Attempts to play an Apple Music track.
     *
     * UPDATE: Launching the external app via Intent (deep link) is disabled because it
     * steals focus from the incoming call screen, causing a poor user experience.
     *
     * Future implementation requires a proper SDK or API that supports background playback.
     */
    suspend fun playTrack(song: SongEntry): Boolean = withContext(Dispatchers.Main) {
        Log.w(TAG, "Apple Music playback disabled: Cannot play in background without stealing focus from incoming call.")
        // Returning false causes RingerPlaybackService to stop, which restores default ringer (if not silenced by silent.wav)
        return@withContext false
    }
}
