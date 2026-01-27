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
class YouTubeMusicPlayerManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "YouTubeMusicPlayer"
        private const val YOUTUBE_MUSIC_PACKAGE = "com.google.android.apps.youtube.music"
    }

    /**
     * Attempts to play a YouTube Music track by launching the app with a deep link.
     * This replaces the "Import/Download" method which relied on unstable 3rd party APIs.
     */
    suspend fun playTrack(song: SongEntry): Boolean = withContext(Dispatchers.Main) {
        try {
            val uri = if (song.uri.startsWith("http")) {
                Uri.parse(song.uri)
            } else {
                // Extract video ID from uri "youtube:video:VIDEO_ID"
                val videoId = song.uri.removePrefix("youtube:video:")
                Uri.parse("https://music.youtube.com/watch?v=$videoId")
            }

            Log.d(TAG, "Attempting to launch YouTube Music with URI: $uri")

            // Check for overlay permission
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M &&
                !android.provider.Settings.canDrawOverlays(context)) {
                Log.w(TAG, "Cannot launch YouTube Music: Missing Overlay Permission")
                // We might fail here if strictly background, but let's try just in case
                // (CallStateReceiver usually ensures we are okay or starts service foreground)
            }

            val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                setPackage(YOUTUBE_MUSIC_PACKAGE)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }

            val packageManager = context.packageManager
            if (intent.resolveActivity(packageManager) != null) {
                context.startActivity(intent)
                Log.d(TAG, "YouTube Music intent launched")
                return@withContext true
            } else {
                Log.e(TAG, "YouTube Music app not installed")
                return@withContext false
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error launching YouTube Music", e)
            return@withContext false
        }
    }
}
