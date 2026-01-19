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
     * Attempts to play an Apple Music track by launching the app with a deep link.
     * Note: This is not a true background player like Spotify App Remote.
     * It relies on the Apple Music app handling the intent.
     * For a "Ringer" this is a best-effort fallback if streaming API is unavailable.
     */
    suspend fun playTrack(song: SongEntry): Boolean = withContext(Dispatchers.Main) {
        try {
            // Apple Music deep link format: https://music.apple.com/us/song/<id> or https://music.apple.com/us/album/<name>/<albumId>?i=<songId>
            val uriString = song.uri
            val uri = if (uriString.startsWith("http")) {
                Uri.parse(uriString)
            } else if (uriString.all { it.isDigit() }) {
                // If it's just an ID (numeric), try to construct a direct song link
                // Note: This is a guess. Apple Music IDs usually require album context or 'i=' param.
                // Fallback to searching/playing by ID if possible via URL scheme
                Uri.parse("https://music.apple.com/us/song/$uriString")
            } else {
                // Assume it's a URI we can parse directly (or a different ID format)
                Uri.parse(uriString)
            }

            Log.d(TAG, "Attempting to launch Apple Music with URI: $uri")

            // Check for overlay permission which is needed to start activity from background on Android 10+
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M &&
                !android.provider.Settings.canDrawOverlays(context)) {
                Log.w(TAG, "Cannot launch Apple Music: Missing Overlay Permission")
                return@withContext false
            }

            val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                setPackage(APPLE_MUSIC_PACKAGE)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }

            // Verify if Apple Music is installed
            val packageManager = context.packageManager
            if (intent.resolveActivity(packageManager) != null) {
                context.startActivity(intent)
                // We return true because we successfully launched the intent.
                // However, we can't guarantee playback started immediately without user interaction
                // unless Apple Music supports auto-play on deep link (which it usually does).
                Log.d(TAG, "Apple Music intent launched")
                return@withContext true
            } else {
                Log.e(TAG, "Apple Music not installed")
                return@withContext false
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error launching Apple Music", e)
            return@withContext false
        }
    }
}
