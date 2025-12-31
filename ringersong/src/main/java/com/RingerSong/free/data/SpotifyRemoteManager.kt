package com.RingerSong.free.data

import android.content.Context
import android.util.Log
import com.spotify.android.appremote.api.ConnectionParams
import com.spotify.android.appremote.api.Connector
import com.spotify.android.appremote.api.SpotifyAppRemote
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

object SpotifyRemoteManager {
    private const val TAG = "SpotifyRemoteManager"
    // Note: This Client ID should ideally come from BuildConfig or a secure source.
    private const val CLIENT_ID = "b846ea3c7e3440439c6a870be4de24ce"
    private const val REDIRECT_URI = "com.RingerSong.free://callback"

    suspend fun connect(context: Context): SpotifyAppRemote {
        return suspendCoroutine { continuation ->
            val connectionParams = ConnectionParams.Builder(CLIENT_ID)
                .setRedirectUri(REDIRECT_URI)
                .showAuthView(true)
                .build()

            SpotifyAppRemote.connect(context, connectionParams, object : Connector.ConnectionListener {
                override fun onConnected(spotifyAppRemote: SpotifyAppRemote) {
                    Log.d(TAG, "Connected to Spotify App Remote")
                    continuation.resume(spotifyAppRemote)
                }

                override fun onFailure(throwable: Throwable) {
                    Log.e(TAG, "Failed to connect to Spotify App Remote", throwable)
                    continuation.resumeWithException(throwable)
                }
            })
        }
    }

    fun disconnect(remote: SpotifyAppRemote?) {
        remote?.let {
            if (it.isConnected) {
                SpotifyAppRemote.disconnect(it)
                Log.d(TAG, "Disconnected from Spotify App Remote")
            }
        }
    }
}
