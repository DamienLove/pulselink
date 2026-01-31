package com.RingerSong.free

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.RingerSong.free.ads.CurrentActivityHolder
import com.RingerSong.free.ui.RingerSongApp
import com.RingerSong.free.viewmodel.RingerViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val sharedUriState = mutableStateOf<Uri?>(null)
    private val sharedTextState = mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        updateSharedIntent(intent)
        setContent {
            val sharedUri = remember { sharedUriState }
            val sharedText = remember { sharedTextState }
            // Use hiltViewModel() instead of manually creating via factory
            val viewModel: RingerViewModel = hiltViewModel()

            PermissionWrapper {
                RingerSongApp(
                    viewModel = viewModel,
                    sharedUri = sharedUri.value,
                    sharedText = sharedText.value,
                    onSharedConsumed = {
                        sharedUri.value = null
                        sharedText.value = null
                    }
                )
            }
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
        if (intent?.action == Intent.ACTION_SEND) {
            if (intent.type?.startsWith("audio/") == true) {
                val uri = intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
                if (uri != null) {
                    sharedUriState.value = uri
                }
            } else if (intent.type?.startsWith("text/") == true) {
                val text = intent.getStringExtra(Intent.EXTRA_TEXT)
                if (!text.isNullOrBlank()) {
                    sharedTextState.value = text
                }
            }
        }
    }
}

@Composable
fun PermissionWrapper(content: @Composable () -> Unit) {
    val context = LocalContext.current
    var permissionsState by remember { mutableStateOf(checkPermissionsState(context)) }
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                permissionsState = checkPermissionsState(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    if (permissionsState.allGranted) {
        content()
    } else {
        PermissionRequestScreen(
            state = permissionsState,
            onUpdateCheck = { permissionsState = checkPermissionsState(context) }
        )
    }
}

data class PermissionsState(
    val runtimeGranted: Boolean,
    val overlayGranted: Boolean
) {
    val allGranted: Boolean get() = runtimeGranted && overlayGranted
}

fun checkPermissionsState(context: android.content.Context): PermissionsState {
    val hasPhoneState = context.checkSelfPermission(Manifest.permission.READ_PHONE_STATE) == android.content.pm.PackageManager.PERMISSION_GRANTED
    val hasContacts = context.checkSelfPermission(Manifest.permission.READ_CONTACTS) == android.content.pm.PackageManager.PERMISSION_GRANTED

    val hasNotifications = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == android.content.pm.PackageManager.PERMISSION_GRANTED
    } else {
        true
    }

    val runtimeGranted = hasPhoneState && hasContacts && hasNotifications

    val overlayGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        Settings.canDrawOverlays(context)
    } else {
        true
    }

    return PermissionsState(runtimeGranted, overlayGranted)
}

@Composable
fun PermissionRequestScreen(
    state: PermissionsState,
    onUpdateCheck: () -> Unit
) {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { _ ->
        onUpdateCheck()
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Permissions Required",
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "To function as a Ringer, this app needs permissions to:\n\n" +
                        "• Detect Incoming Calls (Phone State)\n" +
                        "• Read Contacts (for custom ringtones)\n" +
                        "• Display over other apps (to play while locked)\n" +
                        "• Notifications (for playback controls)",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Start
            )
            Spacer(modifier = Modifier.height(32.dp))

            if (!state.runtimeGranted) {
                Button(
                    onClick = {
                        val permissions = mutableListOf(
                            Manifest.permission.READ_PHONE_STATE,
                            Manifest.permission.READ_CONTACTS
                        )
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
                        }
                        launcher.launch(permissions.toTypedArray())
                    }
                ) {
                    Text("Grant Runtime Permissions")
                }
            } else if (!state.overlayGranted) {
                Button(
                    onClick = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            val intent = Intent(
                                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                Uri.parse("package:${context.packageName}")
                            )
                            context.startActivity(intent)
                        }
                    }
                ) {
                    Text("Grant Overlay Permission")
                }
            }
        }
    }
}
