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

    suspend fun playTrack(song: SongEntry): Boolean = withContext(Dispatchers.Main) {
        try {
            val uriString = song.uri
            val uri = if (uriString.startsWith("http") || uriString.startsWith("tidal.com")) {
                 if (!uriString.startsWith("http")) {
                     Uri.parse("https://$uriString")
                 } else {
                     Uri.parse(uriString)
                 }
            } else {
                // Assume it is an ID if not a URL
                Uri.parse("https://tidal.com/browse/track/$uriString")
            }

            Log.d(TAG, "Attempting to launch Tidal with URI: $uri")

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M &&
                !android.provider.Settings.canDrawOverlays(context)) {
                Log.w(TAG, "Cannot launch Tidal: Missing Overlay Permission")
            }

            val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                setPackage(TIDAL_PACKAGE)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
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
