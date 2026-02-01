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
class TidalPlayerManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "TidalPlayerManager"
        private const val TIDAL_PACKAGE = "com.aspiro.tidal"
    }

    /**
     * Attempts to play a Tidal track by launching the app with a deep link.
     */
    suspend fun playTrack(song: SongEntry): Boolean = withContext(Dispatchers.Main) {
        try {
            // Expected URI format: "tidal:track:12345" or raw ID if we handle it
            val trackId = if (song.uri.startsWith("tidal:track:")) {
                song.uri.removePrefix("tidal:track:")
            } else {
                song.uri // Assume it's just the ID if source is TIDAL
            }

            // Tidal Deep Link
            val uri = Uri.parse("tidal://track/$trackId")

            Log.d(TAG, "Attempting to launch Tidal with URI: $uri")

            val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                setPackage(TIDAL_PACKAGE)
            }

            val packageManager = context.packageManager
            if (intent.resolveActivity(packageManager) != null) {
                context.startActivity(intent)
                Log.d(TAG, "Tidal intent launched")
                return@withContext true
            } else {
                Log.e(TAG, "Tidal app not installed")
                return@withContext false
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error launching Tidal", e)
            return@withContext false
        }
    }
}
