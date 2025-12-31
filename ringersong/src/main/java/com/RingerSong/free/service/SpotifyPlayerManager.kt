package com.RingerSong.free.service

import android.content.Context
import android.util.Log
import com.RingerSong.free.BuildConfig
import com.spotify.android.appremote.api.ConnectionParams
import com.spotify.android.appremote.api.Connector
import com.spotify.android.appremote.api.SpotifyAppRemote
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

@Singleton
class SpotifyPlayerManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private var spotifyAppRemote: SpotifyAppRemote? = null

    // Get Client ID from BuildConfig
    private val CLIENT_ID = BuildConfig.SPOTIFY_CLIENT_ID
    private val REDIRECT_URI = "${BuildConfig.REDIRECT_SCHEME}://${BuildConfig.REDIRECT_HOST}"

    companion object {
        private const val TAG = "SpotifyPlayerManager"
    }

    suspend fun connect(): Boolean = suspendCancellableCoroutine { continuation ->
        if (spotifyAppRemote?.isConnected == true) {
            continuation.resume(true)
            return@suspendCancellableCoroutine
        }

        if (CLIENT_ID == "YOUR_CLIENT_ID_PLACEHOLDER" || CLIENT_ID.isEmpty()) {
             Log.e(TAG, "Spotify Client ID is not configured. Add spotify.client.id to local.properties")
             continuation.resume(false)
             return@suspendCancellableCoroutine
        }

        val connectionParams = ConnectionParams.Builder(CLIENT_ID)
            .setRedirectUri(REDIRECT_URI)
            .showAuthView(true)
            .build()

        SpotifyAppRemote.connect(context, connectionParams, object : Connector.ConnectionListener {
            override fun onConnected(appRemote: SpotifyAppRemote) {
                spotifyAppRemote = appRemote
                Log.d(TAG, "Connected to Spotify")
                continuation.resume(true)
            }

            override fun onFailure(throwable: Throwable) {
                Log.e(TAG, "Failed to connect to Spotify", throwable)
                continuation.resume(false)
            }
        })
    }

    suspend fun playUri(uri: String): Boolean {
        if (spotifyAppRemote?.isConnected != true) {
            if (!connect()) return false
        }

        return try {
            spotifyAppRemote?.playerApi?.play(uri)
            Log.d(TAG, "Sent play command for $uri")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error playing URI", e)
            false
        }
    }

    fun pause() {
        spotifyAppRemote?.playerApi?.pause()
    }

    fun disconnect() {
        SpotifyAppRemote.disconnect(spotifyAppRemote)
        spotifyAppRemote = null
    }
}
