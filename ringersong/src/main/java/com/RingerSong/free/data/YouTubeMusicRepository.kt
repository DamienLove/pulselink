package com.RingerSong.free.data

import android.content.Context
import android.util.Log
import com.RingerSong.free.BuildConfig
import com.RingerSong.free.util.NetworkUtils
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

data class YouTubePlaylistResponse(
    val success: Boolean?,
    val data: YouTubePlaylistData?
)

data class YouTubePlaylistData(
    val videos: List<YouTubeVideo>?,
    val playlistId: String?,
    val title: String?
)

data class YouTubeSongResponse(
    val success: Boolean?,
    val data: YouTubeSongData?
)

data class YouTubeSongData(
    val videoId: String?,
    val title: String?,
    val artist: String?,
    val album: String?,
    val duration: String?,
    val thumbnail: String?,
    val thumbnails: List<YouTubeThumbnail>?,
    val downloadUrl: String?
)

data class YouTubeSearchResponse(
    val success: Boolean?,
    val data: YouTubeSearchData?
)

data class YouTubeSearchData(
    val results: List<YouTubeSearchResult>?
)

data class YouTubeSearchResult(
    val videoId: String?,
    val title: String?,
    val artist: String?,
    val duration: String?,
    val thumbnail: String?,
    val type: String?
)

data class YouTubeArtistSearchResponse(
    val success: Boolean?,
    val data: YouTubeArtistSearchData?
)

data class YouTubeArtistSearchData(
    val artists: List<YouTubeArtist>?
)

data class YouTubeArtist(
    val browseId: String?,
    val name: String?,
    val thumbnail: String?,
    val subscribers: String?
)

data class YouTubeVideo(
    val videoId: String?,
    val title: String?,
    val artist: String?,
    val duration: String?,
    val thumbnail: String?,
    @SerializedName("thumbnails")
    val thumbnailList: List<YouTubeThumbnail>?
)

data class YouTubeThumbnail(
    val url: String?,
    val width: Int?,
    val height: Int?
)

class YouTubeMusicRepository(private val context: Context) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val gson = Gson()

    companion object {
        private const val TAG = "YouTubeMusic"
    }

    suspend fun getPlaylistVideos(playlistId: String): List<YouTubeVideo>? = withContext(Dispatchers.IO) {
        try {
            val apiUrl = "https://${BuildConfig.RAPIDAPI_YOUTUBE_HOST}/get-playlist-videos?playlistId=${URLEncoder.encode(playlistId, "UTF-8")}"
            val response = makeRequest(apiUrl) ?: return@withContext null
            val playlistResponse = gson.fromJson(response, YouTubePlaylistResponse::class.java)
            Log.d(TAG, "Fetched ${playlistResponse.data?.videos?.size ?: 0} videos")
            return@withContext playlistResponse.data?.videos
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching playlist", e)
            return@withContext null
        }
    }

    suspend fun getSongDetails(videoId: String): YouTubeSongData? = withContext(Dispatchers.IO) {
        try {
            val apiUrl = "https://${BuildConfig.RAPIDAPI_YOUTUBE_HOST}/get-song?videoId=${URLEncoder.encode(videoId, "UTF-8")}"
            val response = makeRequest(apiUrl) ?: return@withContext null
            val songResponse = gson.fromJson(response, YouTubeSongResponse::class.java)
            Log.d(TAG, "Fetched song: ${songResponse.data?.title}")
            return@withContext songResponse.data
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching song", e)
            return@withContext null
        }
    }

    suspend fun searchSongs(query: String): List<YouTubeSearchResult>? = withContext(Dispatchers.IO) {
        try {
            val apiUrl = "https://${BuildConfig.RAPIDAPI_YOUTUBE_HOST}/search?q=${URLEncoder.encode(query, "UTF-8")}"
            val response = makeRequest(apiUrl) ?: return@withContext null
            val searchResponse = gson.fromJson(response, YouTubeSearchResponse::class.java)
            Log.d(TAG, "Search found ${searchResponse.data?.results?.size ?: 0} results")
            return@withContext searchResponse.data?.results
        } catch (e: Exception) {
            Log.e(TAG, "Error searching", e)
            return@withContext null
        }
    }

    suspend fun searchArtists(query: String): List<YouTubeArtist>? = withContext(Dispatchers.IO) {
        try {
            val apiUrl = "https://${BuildConfig.RAPIDAPI_YOUTUBE_HOST}/search-artists?q=${URLEncoder.encode(query, "UTF-8")}"
            val response = makeRequest(apiUrl) ?: return@withContext null
            val artistResponse = gson.fromJson(response, YouTubeArtistSearchResponse::class.java)
            Log.d(TAG, "Found ${artistResponse.data?.artists?.size ?: 0} artists")
            return@withContext artistResponse.data?.artists
        } catch (e: Exception) {
            Log.e(TAG, "Error searching artists", e)
            return@withContext null
        }
    }

    private fun makeRequest(url: String): String? {
        try {
            if (BuildConfig.RAPIDAPI_KEY.isEmpty()) {
                Log.e(TAG, "RapidAPI key not configured")
                return null
            }

            Log.d(TAG, "Making request to: $url")
            val request = Request.Builder()
                .url(url)
                .get()
                .addHeader("x-rapidapi-key", BuildConfig.RAPIDAPI_KEY)
                .addHeader("x-rapidapi-host", BuildConfig.RAPIDAPI_YOUTUBE_HOST)
                .build()

            val response = client.newCall(request).execute()
            val body = response.body?.string()

            if (!response.isSuccessful) {
                Log.e(TAG, "API failed: ${response.code} - ${response.message}")
                Log.e(TAG, "Response body: $body")
                return null
            }

            Log.d(TAG, "API success: $body")
            return body
        } catch (e: Exception) {
            Log.e(TAG, "Request error for URL: $url", e)
            return null
        }
    }

    fun convertSearchResultToSongEntry(result: YouTubeSearchResult): SongEntry? {
        if (result.videoId == null || result.title == null) return null
        val titleWithArtist = if (result.artist != null) {
            "${result.title} - ${result.artist}"
        } else {
            result.title
        }
        return SongEntry(
            id = result.videoId,
            title = titleWithArtist,
            uri = "youtube:video:${result.videoId}",
            durationMs = parseDurationToMs(result.duration),
            addedAt = System.currentTimeMillis()
        )
    }

    private fun parseDurationToMs(duration: String?): Long {
        if (duration == null) return 0L
        return try {
            val parts = duration.split(":")
            when (parts.size) {
                3 -> (parts[0].toLong() * 3600 + parts[1].toLong() * 60 + parts[2].toLong()) * 1000
                2 -> (parts[0].toLong() * 60 + parts[1].toLong()) * 1000
                else -> 0L
            }
        } catch (e: Exception) {
            0L
        }
    }

    suspend fun downloadTrack(videoId: String): DownloadResult = withContext(Dispatchers.IO) {
        Log.d(TAG, "Starting YouTube download for: $videoId")

        if (!NetworkUtils.isNetworkAvailable(context)) {
            return@withContext DownloadResult.Failure(DownloadError.NetworkUnavailable)
        }

        if (BuildConfig.RAPIDAPI_KEY.isEmpty()) {
            Log.e(TAG, "RapidAPI key not configured")
            return@withContext DownloadResult.Failure(DownloadError.NoApiKey())
        }

        try {
            val apiUrl = "https://${BuildConfig.RAPIDAPI_YOUTUBE_HOST}/get-song?videoId=${URLEncoder.encode(videoId, "UTF-8")}"
            val request = Request.Builder()
                .url(apiUrl)
                .get()
                .addHeader("x-rapidapi-key", BuildConfig.RAPIDAPI_KEY)
                .addHeader("x-rapidapi-host", BuildConfig.RAPIDAPI_YOUTUBE_HOST)
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string()

            if (!response.isSuccessful) {
                Log.e(TAG, "API failed: ${response.code}")
                val error = if (response.code == 429) {
                    DownloadError.RateLimitExceeded(response.message)
                } else {
                    DownloadError.ApiError(response.code, response.message)
                }
                return@withContext DownloadResult.Failure(error)
            }

            if (responseBody == null) {
                Log.e(TAG, "Empty API response for video: $videoId")
                return@withContext DownloadResult.Failure(
                    DownloadError.ApiError(response.code, "Empty body")
                )
            }

            val songResponse = gson.fromJson(responseBody, YouTubeSongResponse::class.java)
            val downloadUrl = songResponse.data?.downloadUrl
            if (downloadUrl.isNullOrBlank()) {
                Log.e(TAG, "No download URL for video: $videoId")
                return@withContext DownloadResult.Failure(
                    DownloadError.NoDownloadUrl("Missing download URL")
                )
            }

            val audioRequest = Request.Builder().url(downloadUrl).build()
            val audioResponse = client.newCall(audioRequest).execute()

            if (!audioResponse.isSuccessful) {
                Log.e(TAG, "Audio download failed: ${audioResponse.code}")
                return@withContext DownloadResult.Failure(
                    DownloadError.AudioDownloadFailed(audioResponse.code, audioResponse.message)
                )
            }

            val musicDir = java.io.File(context.getExternalFilesDir(null), "RingerSongs")
            if (!musicDir.exists()) musicDir.mkdirs()

            val localFile = java.io.File(musicDir, "$videoId.mp3")

            audioResponse.body?.byteStream()?.use { input ->
                java.io.FileOutputStream(localFile).use { output ->
                    input.copyTo(output)
                }
            }

            Log.d(TAG, "Downloaded YouTube track: ${localFile.absolutePath}")
            return@withContext DownloadResult.Success(localFile.absolutePath)
        } catch (e: Exception) {
            Log.e(TAG, "YouTube download error", e)
            return@withContext DownloadResult.Failure(
                DownloadError.UnknownError(e.message)
            )
        }
    }

    fun isTrackDownloaded(videoId: String): Boolean {
        val musicDir = java.io.File(context.getExternalFilesDir(null), "RingerSongs")
        return java.io.File(musicDir, "$videoId.mp3").exists()
    }

    fun getLocalFilePath(videoId: String): String? {
        val musicDir = java.io.File(context.getExternalFilesDir(null), "RingerSongs")
        val file = java.io.File(musicDir, "$videoId.mp3")
        return if (file.exists()) file.absolutePath else null
    }
}
