package com.RingerSong.free.data

import android.content.Context
import com.spotify.android.appremote.api.SpotifyAppRemote
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner

@RunWith(MockitoJUnitRunner::class)
class SpotifyRemoteManagerTest {

    @Mock
    private lateinit var mockContext: Context

    @Mock
    private lateinit var mockRemote: SpotifyAppRemote

    @Test
    fun `test disconnect handles null safely`() {
        // Should not throw exception
        SpotifyRemoteManager.disconnect(null)
    }
}
