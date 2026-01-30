package com.RingerSong.free.service

import android.content.Context
import android.util.Log
import com.RingerSong.free.data.SongEntry
import com.RingerSong.free.data.YouTubeMusicRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class YouTubeMusicPlayerManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    // We instantiate the repository directly. In a larger app, this should be injected.
    private val repository = YouTubeMusicRepository(context)

    companion object {
        private const val TAG = "YouTubeMusicPlayer"
    }

    /**
     * Fetches a streamable URL for the given song using RapidAPI.
     * This allows the RingerPlaybackService to play the audio directly via MediaPlayer,
     * fulfilling the requirement for a true background ringer.
     */
    suspend fun getStreamUrl(song: SongEntry): String? = withContext(Dispatchers.IO) {
        try {
            // Extract video ID from uri "youtube:video:VIDEO_ID" or just use ID
            val videoId = if (song.uri.startsWith("youtube:video:")) {
                song.uri.removePrefix("youtube:video:")
            } else {
                song.id
            }

            Log.d(TAG, "Fetching stream URL for videoId: $videoId")

            // Use the repository to get song details which includes downloadUrl
            val details = repository.getSongDetails(videoId)

            if (details != null && !details.downloadUrl.isNullOrEmpty()) {
                Log.d(TAG, "Found stream URL: ${details.downloadUrl}")
                return@withContext details.downloadUrl
            } else {
                Log.e(TAG, "No download URL found for videoId: $videoId")
                return@withContext null
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error fetching stream URL", e)
            return@withContext null
        }
    }
}
