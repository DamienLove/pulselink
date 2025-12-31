package com.RingerSong.free.data

import android.content.Context
import android.util.Log
import com.RingerSong.free.BuildConfig
import com.RingerSong.free.util.NetworkUtils
import com.RingerSong.free.util.RetryUtils
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

data class SpotifyDownloadResponse(
    val success: Boolean,
    val data: SpotifyDownloadData?
)

data class SpotifyDownloadData(
    @SerializedName("downloadUrl")
    val downloadUrl: String?,
    val title: String?,
    val artist: String?,
    val album: String?,
    val cover: String?
)

class SpotifyDownloaderRepository(private val context: Context) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val gson = Gson()

    companion object {
        private const val TAG = "SpotifyDownloader"
    }

    suspend fun downloadTrack(spotifyUrl: String): DownloadResult = withContext(Dispatchers.IO) {
        Log.d(TAG, "Starting download for: $spotifyUrl")

        if (!NetworkUtils.isNetworkAvailable(context)) {
            return@withContext DownloadResult.Failure(DownloadError.NetworkUnavailable)
        }

        if (BuildConfig.RAPIDAPI_KEY.isEmpty()) {
            Log.e(TAG, "RapidAPI key not configured")
            return@withContext DownloadResult.Failure(DownloadError.NoApiKey())
        }

        val encodedUrl = URLEncoder.encode(spotifyUrl, "UTF-8")
        val apiUrl = "https://${BuildConfig.RAPIDAPI_SPOTIFY_HOST}/downloadSong?songId=$encodedUrl"

        RetryUtils.retryDownload {
            try {
                val request = Request.Builder()
                    .url(apiUrl)
                    .get()
                    .addHeader("x-rapidapi-key", BuildConfig.RAPIDAPI_KEY)
                    .addHeader("x-rapidapi-host", BuildConfig.RAPIDAPI_SPOTIFY_HOST)
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
                    return@retryDownload DownloadResult.Failure(error)
                }

                if (responseBody == null) {
                    Log.e(TAG, "Empty API response")
                    return@retryDownload DownloadResult.Failure(
                        DownloadError.UnknownError("Empty API response")
                    )
                }

                val downloadResponse = gson.fromJson(responseBody, SpotifyDownloadResponse::class.java)

                val audioUrl = downloadResponse.data?.downloadUrl
                if (!downloadResponse.success || audioUrl.isNullOrBlank()) {
                    Log.e(TAG, "No download URL in response")
                    return@retryDownload DownloadResult.Failure(
                        DownloadError.NoDownloadUrl("Missing download URL")
                    )
                }

                val audioRequest = Request.Builder().url(audioUrl).build()
                val audioResponse = client.newCall(audioRequest).execute()

                if (!audioResponse.isSuccessful) {
                    Log.e(TAG, "Audio download failed: ${audioResponse.code}")
                    return@retryDownload DownloadResult.Failure(
                        DownloadError.AudioDownloadFailed(audioResponse.code, audioResponse.message)
                    )
                }

                val musicDir = File(context.getExternalFilesDir(null), "RingerSongs")
                if (!musicDir.exists()) musicDir.mkdirs()

                val trackId = extractTrackId(spotifyUrl)
                val localFile = File(musicDir, "$trackId.mp3")

                audioResponse.body?.byteStream()?.use { input ->
                    FileOutputStream(localFile).use { output ->
                        input.copyTo(output)
                    }
                }

                Log.d(TAG, "Downloaded: ${localFile.absolutePath}")
                DownloadResult.Success(localFile.absolutePath)
            } catch (e: Exception) {
                Log.e(TAG, "Download error", e)
                DownloadResult.Failure(DownloadError.UnknownError(e.message))
            }
        }
    }

    fun isTrackDownloaded(spotifyUrl: String): Boolean {
        val trackId = extractTrackId(spotifyUrl)
        val musicDir = File(context.getExternalFilesDir(null), "RingerSongs")
        return File(musicDir, "$trackId.mp3").exists()
    }

    fun getLocalFilePath(spotifyUrl: String): String? {
        val trackId = extractTrackId(spotifyUrl)
        val musicDir = File(context.getExternalFilesDir(null), "RingerSongs")
        val file = File(musicDir, "$trackId.mp3")
        return if (file.exists()) file.absolutePath else null
    }

    fun getLocalFilePathFromUri(spotifyUri: String): String? {
        val trackId = spotifyUri.substringAfterLast(":")
        val musicDir = File(context.getExternalFilesDir(null), "RingerSongs")
        val file = File(musicDir, "$trackId.mp3")
        return if (file.exists()) file.absolutePath else null
    }

    fun deleteTrack(spotifyUrl: String): Boolean {
        val trackId = extractTrackId(spotifyUrl)
        val musicDir = File(context.getExternalFilesDir(null), "RingerSongs")
        val file = File(musicDir, "$trackId.mp3")
        return if (file.exists()) file.delete() else false
    }

    private fun extractTrackId(spotifyUrlOrUri: String): String {
        return spotifyUrlOrUri
            .substringAfterLast("/")
            .substringAfterLast(":")
            .substringBefore("?")
    }
}
