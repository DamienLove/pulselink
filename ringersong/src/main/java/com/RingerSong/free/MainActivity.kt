package com.RingerSong.free

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.lifecycle.viewmodel.compose.viewModel
import com.RingerSong.free.ads.CurrentActivityHolder
import com.RingerSong.free.ui.RingerSongApp
import com.RingerSong.free.viewmodel.RingerViewModel
import com.RingerSong.free.viewmodel.RingerViewModelFactory

class MainActivity : ComponentActivity() {
    private val sharedUriState = mutableStateOf<Uri?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        updateSharedIntent(intent)
        setContent {
            val sharedUri = remember { sharedUriState }
            val viewModel: RingerViewModel = viewModel(
                factory = RingerViewModelFactory(application)
            )
            RingerSongApp(
                viewModel = viewModel,
                sharedUri = sharedUri.value,
                onSharedConsumed = { sharedUri.value = null }
            )
        }
    }

    override fun onResume() {
        super.onResume()
        CurrentActivityHolder.activity = this
    }

    override fun onPause() {
        CurrentActivityHolder.activity = null
        super.onPause()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        updateSharedIntent(intent)
    }

    private fun updateSharedIntent(intent: Intent?) {
        if (intent?.action == Intent.ACTION_SEND && intent.type?.startsWith("audio/") == true) {
            val uri = intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
            if (uri != null) {
                sharedUriState.value = uri
            }
        }
    }
}
