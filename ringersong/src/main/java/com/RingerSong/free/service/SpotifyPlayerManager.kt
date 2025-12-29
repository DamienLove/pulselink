package com.RingerSong.free.service

import android.content.Context
import android.util.Log
import com.spotify.android.appremote.api.ConnectionParams
import com.spotify.android.appremote.api.Connector
import com.spotify.android.appremote.api.SpotifyAppRemote

class SpotifyPlayerManager(private val context: Context) {

    private var spotifyAppRemote: SpotifyAppRemote? = null

    // Spotify App Remote credentials (for playback via Spotify app)
    private val CLIENT_ID = "b846ea3c7e3440439c6a870be4de24ce"
    private val REDIRECT_URI = "com.RingerSong.free://callback"

    fun connect(onConnected: () -> Unit, onFailure: (Throwable) -> Unit) {
        if (spotifyAppRemote?.isConnected == true) {
            onConnected()
            return
        }

        val connectionParams = ConnectionParams.Builder(CLIENT_ID)
            .setRedirectUri(REDIRECT_URI)
            .showAuthView(true)
            .build()

        SpotifyAppRemote.connect(context, connectionParams, object : Connector.ConnectionListener {
            override fun onConnected(appRemote: SpotifyAppRemote) {
                spotifyAppRemote = appRemote
                Log.d("SpotifyPlayerManager", "Connected! Yay!")
                onConnected()
            }

            override fun onFailure(throwable: Throwable) {
                Log.e("SpotifyPlayerManager", throwable.message, throwable)
                onFailure(throwable)
            }
        })
    }

    fun play(uri: String) {
        spotifyAppRemote?.playerApi?.play(uri)
    }

    fun seekTo(positionMs: Long) {
        spotifyAppRemote?.playerApi?.seekTo(positionMs)
    }

    fun pause() {
        spotifyAppRemote?.playerApi?.pause()
    }

    fun disconnect() {
        SpotifyAppRemote.disconnect(spotifyAppRemote)
        spotifyAppRemote = null
    }

    fun isConnected(): Boolean {
        return spotifyAppRemote?.isConnected == true
    }
}
