package com.RingerSong.free.data

import android.content.Context
import android.util.Base64
import com.RingerSong.free.BuildConfig
import com.google.gson.Gson
import com.spotify.android.appremote.api.ConnectionParams
import com.spotify.android.appremote.api.Connector
import com.spotify.android.appremote.api.SpotifyAppRemote
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

// Spotify API credentials for search functionality
private val CLIENT_ID = BuildConfig.SPOTIFY_CLIENT_ID
private val CLIENT_SECRET = BuildConfig.SPOTIFY_CLIENT_SECRET
private const val REDIRECT_URI = "com.RingerSong.free://callback"

data class SpotifyTrack(
    val id: String?,
    val name: String?,
    val uri: String?,
    val artists: List<SpotifyArtist>?,
    val duration_ms: Long?
)

data class SpotifyArtist(
    val name: String?
)

data class SpotifySearchResponse(
    val tracks: SpotifyTracksItems
)

data class SpotifyTracksItems(
    val items: List<SpotifyTrack>
)

private data class TokenResponse(
    val access_token: String,
    val token_type: String,
    val expires_in: Int
)

class SpotifyRepository(
    private val client: OkHttpClient
) {
    private val gson = Gson()
    private var accessToken: String? = null
    private var tokenExpiration: Long = 0

    suspend fun searchTracks(query: String): List<SpotifyTrack> = withContext(Dispatchers.IO) {
        ensureToken()
        val token = accessToken ?: throw IOException("Could not authenticate with Spotify")

        val request = Request.Builder()
            .url("https://api.spotify.com/v1/search?q=${query.replace(" ", "%20")}&type=track&limit=20")
            .header("Authorization", "Bearer $token")
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("Spotify Search Failed: ${response.code}")
            val body = response.body?.string() ?: return@withContext emptyList()
            val searchResult = gson.fromJson(body, SpotifySearchResponse::class.java)
            searchResult.tracks.items
        }
    }

    private fun ensureToken() {
        if (accessToken != null && System.currentTimeMillis() < tokenExpiration) return

        if (CLIENT_ID == "YOUR_CLIENT_ID_HERE") {
             // For prototype purposes, we can't really throw here without breaking the app for the user immediately.
             // We will throw a helpful error.
             throw IOException("Please set your Spotify CLIENT_ID and CLIENT_SECRET in SpotifyRepository.kt")
        }

        val auth = Base64.encodeToString("$CLIENT_ID:$CLIENT_SECRET".toByteArray(), Base64.NO_WRAP)
        val request = Request.Builder()
            .url("https://accounts.spotify.com/api/token")
            .header("Authorization", "Basic $auth")
            .post(FormBody.Builder().add("grant_type", "client_credentials").build())
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("Auth Failed: ${response.code}")
            val body = response.body?.string() ?: throw IOException("Empty auth response")
            val tokenData = gson.fromJson(body, TokenResponse::class.java)
            accessToken = tokenData.access_token
            tokenExpiration = System.currentTimeMillis() + (tokenData.expires_in * 1000) - 60000
        }
    }
}

object SpotifyRemoteManager {
    private var spotifyAppRemote: SpotifyAppRemote? = null

    suspend fun connect(context: Context): SpotifyAppRemote = suspendCancellableCoroutine { cont ->
        if (spotifyAppRemote?.isConnected == true) {
            cont.resume(spotifyAppRemote!!)
            return@suspendCancellableCoroutine
        }

        val connectionParams = ConnectionParams.Builder(CLIENT_ID)
            .setRedirectUri(REDIRECT_URI)
            .showAuthView(true)
            .build()

        SpotifyAppRemote.connect(context, connectionParams, object : Connector.ConnectionListener {
            override fun onConnected(appRemote: SpotifyAppRemote) {
                spotifyAppRemote = appRemote
                cont.resume(appRemote)
            }

            override fun onFailure(throwable: Throwable) {
                cont.resumeWithException(throwable)
            }
        })
    }

    fun disconnect() {
        spotifyAppRemote?.let {
            SpotifyAppRemote.disconnect(it)
        }
        spotifyAppRemote = null
    }
}
