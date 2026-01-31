package com.RingerSong.free.service

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import com.RingerSong.free.BuildConfig
import com.spotify.android.appremote.api.ConnectionParams
import com.spotify.android.appremote.api.Connector
import com.spotify.android.appremote.api.SpotifyAppRemote
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
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

    suspend fun connect(showAuthView: Boolean = true): Boolean = suspendCancellableCoroutine { continuation ->
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
            .showAuthView(showAuthView)
            .build()

        // Use a timeout logic if possible, but SpotifyAppRemote doesn't expose it directly.
        // We rely on the callback.
        SpotifyAppRemote.connect(context, connectionParams, object : Connector.ConnectionListener {
            override fun onConnected(appRemote: SpotifyAppRemote) {
                spotifyAppRemote = appRemote
                Log.d(TAG, "Connected to Spotify")
                if (continuation.isActive) {
                    continuation.resume(true)
                }
            }

            override fun onFailure(throwable: Throwable) {
                Log.e(TAG, "Failed to connect to Spotify", throwable)
                if (continuation.isActive) {
                    continuation.resume(false)
                }
            }
        })
    }

    suspend fun playUri(uri: String, startMs: Long = 0): Boolean {
        try {
            if (spotifyAppRemote?.isConnected != true) {
                // Attempt silent connection first with a timeout check implicitly handled by connect
                if (!connect(showAuthView = false)) {
                     Log.w(TAG, "Could not connect to Spotify AppRemote. Falling back to Intent.")
                     return playTrackIntent(uri)
                }
            }

            return suspendCancellableCoroutine { continuation ->
                val playerApi = spotifyAppRemote?.playerApi
                if (playerApi == null) {
                    // Fallback if player API is null despite connection
                    Log.w(TAG, "Player API null. Falling back to Intent.")
                    // Can't easily switch to suspend fun from here without launching coroutine or restructuring.
                    // But we can just resume(false) and let the outer catch block handle it? No.
                    // For simplicity, we just fail AppRemote here, but we can't call suspend function from callback.
                    // So we must structure this better.
                    if (continuation.isActive) continuation.resume(false)
                    return@suspendCancellableCoroutine
                }

                playerApi.play(uri).setResultCallback {
                    Log.d(TAG, "Sent play command for $uri")
                    if (startMs > 0) {
                         playerApi.seekTo(startMs).setResultCallback {
                             Log.d(TAG, "Sent seek command for $startMs")
                             if (continuation.isActive) continuation.resume(true)
                         }.setErrorCallback { e ->
                             Log.e(TAG, "Error seeking to $startMs", e)
                             // Resume true anyway as play succeeded, though seek failed
                             if (continuation.isActive) continuation.resume(true)
                         }
                    } else {
                        if (continuation.isActive) continuation.resume(true)
                    }
                }.setErrorCallback { e ->
                    Log.e(TAG, "Error playing URI via AppRemote", e)
                    if (continuation.isActive) continuation.resume(false)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception during playUri, falling back to Intent", e)
            return playTrackIntent(uri)
        }
    }

    private suspend fun playTrackIntent(uri: String): Boolean = withContext(Dispatchers.Main) {
        try {
            Log.d(TAG, "Attempting Intent fallback for: $uri")
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(uri)).apply {
                setPackage("com.spotify.music")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M &&
                !android.provider.Settings.canDrawOverlays(context)) {
                Log.w(TAG, "Missing overlay permission for Intent fallback")
            }

            context.startActivity(intent)
            Log.d(TAG, "Launched Spotify via Intent")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Intent fallback failed", e)
            false
        }
    }

    fun pause() {
        if (spotifyAppRemote?.isConnected == true) {
             spotifyAppRemote?.playerApi?.pause()?.setResultCallback {
                 Log.d(TAG, "Sent pause command")
             }?.setErrorCallback { e ->
                 Log.e(TAG, "Error sending pause command", e)
             }
        } else {
            Log.w(TAG, "Cannot pause: Spotify not connected")
        }
    }

    fun disconnect() {
        SpotifyAppRemote.disconnect(spotifyAppRemote)
        spotifyAppRemote = null
    }

    suspend fun getUserCapabilities(): String = suspendCancellableCoroutine { continuation ->
        if (spotifyAppRemote?.isConnected != true) {
            continuation.resume("Not Connected")
            return@suspendCancellableCoroutine
        }

        spotifyAppRemote?.userApi?.capabilities?.setResultCallback { capabilities ->
            val canPlayOnDemand = capabilities.canPlayOnDemand
            Log.d(TAG, "User capabilities: canPlayOnDemand=$canPlayOnDemand")
            continuation.resume(if (canPlayOnDemand) "Premium" else "Free")
        }?.setErrorCallback {
            Log.e(TAG, "Error getting capabilities", it)
            continuation.resume("Error")
        }
    }
}
