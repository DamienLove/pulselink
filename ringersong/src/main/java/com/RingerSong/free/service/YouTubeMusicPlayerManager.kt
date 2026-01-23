package com.RingerSong.free.service

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
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
        private const val YOUTUBE_PACKAGE = "com.google.android.youtube"
    }

    suspend fun playVideo(videoId: String): Boolean = withContext(Dispatchers.Main) {
        try {
            val uri = Uri.parse("https://music.youtube.com/watch?v=$videoId")
            Log.d(TAG, "Attempting to launch YouTube Music with URI: $uri")

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M &&
                !android.provider.Settings.canDrawOverlays(context)) {
                Log.w(TAG, "Cannot launch YouTube Music: Missing Overlay Permission")
                return@withContext false
            }

            val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }

            val packageManager = context.packageManager

            // Try YouTube Music first
            intent.setPackage(YOUTUBE_MUSIC_PACKAGE)
            if (intent.resolveActivity(packageManager) != null) {
                context.startActivity(intent)
                Log.d(TAG, "YouTube Music launched")
                return@withContext true
            }

            // Fallback to standard YouTube
            intent.setPackage(YOUTUBE_PACKAGE)
            if (intent.resolveActivity(packageManager) != null) {
                Log.d(TAG, "YouTube Music not installed, falling back to standard YouTube")
                context.startActivity(intent)
                return@withContext true
            }

            // Fallback to generic browser/handler
            intent.setPackage(null)
             if (intent.resolveActivity(packageManager) != null) {
                Log.d(TAG, "YouTube apps not installed, falling back to browser")
                context.startActivity(intent)
                return@withContext true
            }

            Log.e(TAG, "No app found to handle YouTube URI")
            return@withContext false

        } catch (e: Exception) {
            Log.e(TAG, "Error launching YouTube Music", e)
            return@withContext false
        }
    }

    fun stop() {
        // Cannot stop external app directly. Service handles focus stealing.
    }
}
