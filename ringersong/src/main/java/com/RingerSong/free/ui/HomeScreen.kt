package com.RingerSong.free.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.ContactsContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.Slider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.RingerSong.free.R
import com.RingerSong.free.ads.AdsConfig
import com.RingerSong.free.ads.populateNativeAdView
import com.RingerSong.free.data.AppState
import com.RingerSong.free.data.ContactEntry
import com.RingerSong.free.data.SongEntry
import com.RingerSong.free.data.SpotifyTrack
import com.RingerSong.free.viewmodel.SpotifySearchState
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdView
import kotlin.math.roundToInt

@Composable
fun HomeScreen(
    state: AppState,
    searchState: SpotifySearchState,
    youtubeSearchState: SpotifySearchState,
    onOpenSettings: () -> Unit,
    onToggleEnabled: (Boolean) -> Unit,
    onToggleShuffle: (Boolean) -> Unit,
    onToggleNotifications: (Boolean) -> Unit,
    onAddSongs: (List<Uri>) -> Unit,
    onRemoveSong: (String) -> Unit,
    onMoveSong: (Int, Int) -> Unit,
    onAddOrUpdateContact: (ContactEntry) -> Unit,
    onAssignContactSong: (String, String?) -> Unit,
    onUpdateUrgencyThreshold: (String, Int) -> Unit,
    onSetUrgencyTone: (String, Uri) -> Unit,
    onClearUrgencyTone: (String) -> Unit,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    onClearSearch: () -> Unit,
    onAddSpotifyTrack: (SpotifyTrack) -> Unit,
    onYouTubeQueryChange: (String) -> Unit,
    onYouTubeSearch: () -> Unit,
    onClearYouTubeSearch: () -> Unit,
    onAddYouTubeTrack: (SpotifyTrack) -> Unit
) {
    val context = LocalContext.current
    val showContent = remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { showContent.value = true }

    val songsById = remember(state.songs) { state.songs.associateBy { it.id } }
    val orderedSongs = state.songOrder.mapNotNull { songsById[it] }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { }

    val audioPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        if (uris.isNotEmpty()) {
            onAddSongs(uris)
        }
    }

    val contactPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickContact()
    ) { uri ->
        if (uri != null) {
            resolveContact(context, uri)?.let { contact ->
                onAddOrUpdateContact(contact)
            }
        }
    }

    var activeContactForUrgency by remember { mutableStateOf<ContactEntry?>(null) }
    var activeContact by remember { mutableStateOf<ContactEntry?>(null) }

    val urgencyPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        val contact = activeContactForUrgency
        if (uri != null && contact != null) {
            onSetUrgencyTone(contact.id, uri)
        }
        activeContactForUrgency = null
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        MaterialTheme.colorScheme.surface,
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f),
                        MaterialTheme.colorScheme.background
                    )
                )
            )
    ) {
        MusicBackdrop()
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 18.dp)
                .verticalScroll(rememberScrollState())
        ) {
            TopBar(onOpenSettings)
            Spacer(modifier = Modifier.height(16.dp))

            AnimatedVisibility(
                visible = showContent.value,
                enter = fadeIn() + slideInVertically(initialOffsetY = { it / 2 })
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    StatusCard(
                        enabled = state.settings.enabled,
                        onToggle = onToggleEnabled
                    )
                    PermissionsCard(
                        context = context,
                        onRequest = {
                            permissionLauncher.launch(requiredPermissions())
                        }
                    )
                    ShuffleCard(
                        shuffle = state.settings.shuffle,
                        onToggle = onToggleShuffle
                    )
                    NotificationsCard(
                        enabled = state.settings.notificationsEnabled,
                        onToggle = onToggleNotifications
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))
            AnimatedVisibility(
                visible = showContent.value,
                enter = fadeIn() + slideInVertically(initialOffsetY = { it / 3 })
            ) {
                ContactsSection(
                    contacts = state.contacts,
                    songs = orderedSongs,
                    onAddContact = { contactPicker.launch(null) },
                    onAssignContact = { activeContact = it },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(18.dp))
            AnimatedVisibility(
                visible = showContent.value,
                enter = fadeIn() + slideInVertically(initialOffsetY = { it / 3 })
            ) {
                TracksSection(
                    songs = orderedSongs,
                    shuffle = state.settings.shuffle,
                    maxSongs = state.settings.maxSongs,
                    onAddSongs = { audioPicker.launch(arrayOf("audio/*")) },
                    onRemoveSong = onRemoveSong,
                    onMoveSong = onMoveSong,
                    onOpenZedge = { openZedge(context) }
                )
            }

            Spacer(modifier = Modifier.height(18.dp))
            AnimatedVisibility(
                visible = showContent.value,
                enter = fadeIn() + slideInVertically(initialOffsetY = { it / 3 })
            ) {
                SpotifySection(
                    state = searchState,
                    onQueryChange = onQueryChange,
                    onSearch = onSearch,
                    onClear = onClearSearch,
                    onAddTrack = onAddSpotifyTrack,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(18.dp))
            AnimatedVisibility(
                visible = showContent.value,
                enter = fadeIn() + slideInVertically(initialOffsetY = { it / 3 })
            ) {
                YouTubeMusicSection(
                    state = youtubeSearchState,
                    onQueryChange = onYouTubeQueryChange,
                    onSearch = onYouTubeSearch,
                    onClear = onClearYouTubeSearch,
                    onAddTrack = onAddYouTubeTrack,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
            AdsSection()
            Spacer(modifier = Modifier.height(18.dp))
        }

        if (activeContact != null) {
            ContactSettingsDialog(
                contact = activeContact!!,
                songs = orderedSongs,
                onDismiss = { activeContact = null },
                onAssignSong = { songId ->
                    onAssignContactSong(activeContact!!.id, songId)
                },
                onUrgencyThresholdChange = { threshold ->
                    onUpdateUrgencyThreshold(activeContact!!.id, threshold)
                },
                onPickUrgencyTone = {
                    activeContactForUrgency = activeContact
                    urgencyPicker.launch(arrayOf("audio/*"))
                },
                onClearUrgencyTone = {
                    onClearUrgencyTone(activeContact!!.id)
                },
                onSetUrgencyTone = { contactId, uri ->
                    onSetUrgencyTone(contactId, uri)
                }
            )
        }
    }
}

@Composable
private fun AdsSection() {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        NativeAdSection()
        BannerAd()
    }
}

@Composable
private fun BannerAd() {
    AndroidView(
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp),
        factory = { context ->
            AdView(context).apply {
                setAdSize(AdSize.BANNER)
                adUnitId = AdsConfig.BANNER_ID
                loadAd(AdRequest.Builder().build())
            }
        }
    )
}

@Composable
private fun NativeAdSection() {
    val context = LocalContext.current
    var nativeAd by remember { mutableStateOf<NativeAd?>(null) }

    DisposableEffect(Unit) {
        val adLoader = AdLoader.Builder(context, AdsConfig.NATIVE_ID)
            .forNativeAd { ad ->
                nativeAd?.destroy()
                nativeAd = ad
            }
            .withAdListener(object : AdListener() {
                override fun onAdFailedToLoad(error: LoadAdError) {
                    nativeAd?.destroy()
                    nativeAd = null
                }
            })
            .build()
        adLoader.loadAd(AdRequest.Builder().build())

        onDispose {
            nativeAd?.destroy()
            nativeAd = null
        }
    }

    val ad = nativeAd ?: return
    AndroidView(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(6.dp),
        factory = { ctx ->
            val view = android.view.LayoutInflater.from(ctx)
                .inflate(R.layout.native_ad_view, null) as NativeAdView
            populateNativeAdView(ad, view)
            view
        },
        update = { view ->
            populateNativeAdView(ad, view)
        }
    )
}

@Composable
private fun TopBar(onOpenSettings: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = "RingerSong",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Progressive ringers, song by song",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        IconButton(onClick = onOpenSettings) {
            Icon(
                imageVector = Icons.Filled.Settings,
                contentDescription = "Settings",
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun StatusCard(enabled: Boolean, onToggle: (Boolean) -> Unit) {
    SectionCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (enabled) "Progression Ringer On" else "Progression Ringer Off",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = if (enabled) {
                        "Incoming calls advance your track segments."
                    } else {
                        "Use the normal system ringer."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(checked = enabled, onCheckedChange = onToggle)
        }
    }
}

@Composable
private fun ShuffleCard(shuffle: Boolean, onToggle: (Boolean) -> Unit) {
    SectionCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (shuffle) "Shuffle Playlist" else "Manual Order",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = if (shuffle) {
                        "Each song plays to completion before a new one starts."
                    } else {
                        "Drag tracks to the order you want."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(checked = shuffle, onCheckedChange = onToggle)
        }
    }
}

@Composable
private fun NotificationsCard(enabled: Boolean, onToggle: (Boolean) -> Unit) {
    SectionCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Notifications",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = if (enabled) {
                        "Use progression tone for notifications too."
                    } else {
                        "Notifications stay on your normal sound."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(checked = enabled, onCheckedChange = onToggle)
        }
    }
}

@Composable
private fun PermissionsCard(context: Context, onRequest: () -> Unit) {
    val missing = requiredPermissions().filter {
        ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
    }
    if (missing.isEmpty()) return
    SectionCard(
        accent = MaterialTheme.colorScheme.tertiaryContainer,
        contentColor = MaterialTheme.colorScheme.onTertiaryContainer
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "Permissions needed",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "Allow access to phone state, contacts, and audio to progress ring segments.",
                style = MaterialTheme.typography.bodySmall
            )
            OutlinedButton(onClick = onRequest) {
                Text("Grant permissions")
            }
        }
    }
}

@Composable
private fun ContactsSection(
    contacts: List<ContactEntry>,
    songs: List<SongEntry>,
    onAddContact: () -> Unit,
    onAssignContact: (ContactEntry) -> Unit,
    modifier: Modifier = Modifier
) {
    SectionCard(modifier = modifier) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Contact Tracks",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Give each contact their own progression.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                OutlinedButton(onClick = onAddContact) {
                    Text("Add contact")
                }
            }
            if (contacts.isEmpty()) {
                Text(
                    text = "No contacts yet. Add a contact to assign a song.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    contacts.forEach { contact ->
                        ContactRow(contact, songs, onAssignContact)
                    }
                }
            }
        }
    }
}

@Composable
private fun ContactRow(
    contact: ContactEntry,
    songs: List<SongEntry>,
    onAssignContact: (ContactEntry) -> Unit
) {
    val songTitle = songs.firstOrNull { it.id == contact.assignedSongId }?.title
        ?: "Use global playlist"
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = contact.name,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold
            )
        Text(
            text = songTitle,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = urgencyLabel(contact),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        }
        TextButton(onClick = { onAssignContact(contact) }) {
            Text("Manage")
        }
    }
}

@Composable
private fun TracksSection(
    songs: List<SongEntry>,
    shuffle: Boolean,
    maxSongs: Int,
    onAddSongs: () -> Unit,
    onRemoveSong: (String) -> Unit,
    onMoveSong: (Int, Int) -> Unit,
    onOpenZedge: () -> Unit
) {
    val context = LocalContext.current
    SectionCard {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Tracks",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Up to $maxSongs songs. Add more in Settings.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (songs.isEmpty()) {
                EmptyTracksHint(onAddSongs)
            } else {
                ReorderableSongList(
                    songs = songs,
                    shuffle = shuffle,
                    onRemoveSong = onRemoveSong,
                    onMoveSong = onMoveSong
                )
                Button(
                    onClick = onAddSongs,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                ) {
                    Text("Select songs")
                }
                OutlinedButton(onClick = onOpenZedge, modifier = Modifier.fillMaxWidth()) {
                    Text("Open Zedge")
                }
                HowToAddMusicHelper()
            }
        }
    }
}

@Composable
private fun SpotifySection(
    state: SpotifySearchState,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    onClear: () -> Unit,
    onAddTrack: (SpotifyTrack) -> Unit,
    modifier: Modifier = Modifier
) {
    SectionCard(modifier = modifier) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Add from Spotify",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "Search for tracks on Spotify and add them to your progression.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            OutlinedTextField(
                value = state.query,
                onValueChange = onQueryChange,
                label = { Text("Search songs") },
                placeholder = { Text("Track or artist name") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Search
                ),
                keyboardActions = KeyboardActions(onSearch = { onSearch() }),
                modifier = Modifier.fillMaxWidth()
            )

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                val canSearch = state.query.isNotBlank() && !state.isLoading
                Button(onClick = onSearch, enabled = canSearch, modifier = Modifier.weight(1f)) {
                    Text(if (state.isLoading) "Searching..." else "Search")
                }
                OutlinedButton(
                    onClick = onClear,
                    enabled = state.query.isNotBlank() || state.results.isNotEmpty() || state.errorMessage != null,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Clear")
                }
            }

            if (state.isLoading) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    Text(
                        text = "Searching Spotify...",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (state.errorMessage != null) {
                Text(
                    text = state.errorMessage,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }

            if (!state.isLoading && state.results.isEmpty() && state.errorMessage == null && state.query.isNotBlank()) {
                Text(
                    text = "No results found.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (state.results.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    state.results.forEach { item ->
                        SpotifyResultRow(item = item, onAdd = { onAddTrack(item) })
                    }
                }
            }
        }
    }
}

@Composable
private fun YouTubeMusicSection(
    state: SpotifySearchState,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    onClear: () -> Unit,
    onAddTrack: (SpotifyTrack) -> Unit,
    modifier: Modifier = Modifier
) {
    SectionCard(modifier = modifier) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Add from YouTube Music",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "Search for tracks on YouTube Music and add them to your progression.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            OutlinedTextField(
                value = state.query,
                onValueChange = onQueryChange,
                label = { Text("Search songs") },
                placeholder = { Text("Track or artist name") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Search
                ),
                keyboardActions = KeyboardActions(onSearch = { onSearch() }),
                modifier = Modifier.fillMaxWidth()
            )

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                val canSearch = state.query.isNotBlank() && !state.isLoading
                Button(onClick = onSearch, enabled = canSearch, modifier = Modifier.weight(1f)) {
                    Text(if (state.isLoading) "Searching..." else "Search")
                }
                OutlinedButton(
                    onClick = onClear,
                    enabled = state.query.isNotBlank() || state.results.isNotEmpty() || state.errorMessage != null,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Clear")
                }
            }

            if (state.isLoading) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    Text(
                        text = "Searching YouTube Music...",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (state.errorMessage != null) {
                Text(
                    text = state.errorMessage,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }

            if (!state.isLoading && state.results.isEmpty() && state.errorMessage == null && state.query.isNotBlank()) {
                Text(
                    text = "No results found.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (state.results.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    state.results.forEach { item ->
                        SpotifyResultRow(item = item, onAdd = { onAddTrack(item) })
                    }
                }
            }
        }
    }
}

@Composable
private fun SpotifyResultRow(
    item: SpotifyTrack,
    onAdd: () -> Unit
) {
    val trackName = item.name ?: "Unknown Track"
    val artistName = item.artists?.mapNotNull { it.name }?.joinToString(", ") ?: "Unknown Artist"

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), RoundedCornerShape(14.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = trackName,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = artistName,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        TextButton(onClick = onAdd) {
            Text("Add")
        }
    }
}

@Composable
private fun EmptyTracksHint(onAddSongs: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Filled.MusicNote,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
            modifier = Modifier.size(36.dp)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "No songs selected yet.",
            style = MaterialTheme.typography.bodyMedium
        )
        Text(
            text = "Pick audio files to start the progression.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(12.dp))
        Button(onClick = onAddSongs) {
            Text("Select songs")
        }
    }
}

@Composable
private fun ReorderableSongList(
    songs: List<SongEntry>,
    shuffle: Boolean,
    onRemoveSong: (String) -> Unit,
    onMoveSong: (Int, Int) -> Unit
) {
    val itemHeight: Dp = 72.dp
    val density = androidx.compose.ui.platform.LocalDensity.current
    val itemHeightPx = with(density) { itemHeight.toPx() }
    var draggingId by remember { mutableStateOf<String?>(null) }
    var dragOffset by remember { mutableStateOf(0f) }

    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        songs.forEachIndexed { index, song ->
            val isDragging = draggingId == song.id
            val offsetY = if (isDragging) dragOffset else 0f

            SongRow(
                song = song,
                shuffle = shuffle,
                modifier = Modifier
                    .height(itemHeight)
                    .graphicsLayer { translationY = offsetY }
                    .pointerInput(shuffle, song.id) {
                        if (!shuffle) {
                            detectDragGesturesAfterLongPress(
                                onDragStart = {
                                    draggingId = song.id
                                    dragOffset = 0f
                                },
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    dragOffset += dragAmount.y
                                    val shift = (dragOffset / itemHeightPx).roundToInt()
                                    if (shift != 0) {
                                        val currentIndex = songs.indexOfFirst { it.id == song.id }
                                        val targetIndex = (currentIndex + shift)
                                            .coerceIn(0, songs.lastIndex)
                                        if (targetIndex != currentIndex) {
                                            onMoveSong(currentIndex, targetIndex)
                                            dragOffset -= shift * itemHeightPx
                                        }
                                    }
                                },
                                onDragEnd = {
                                    draggingId = null
                                    dragOffset = 0f
                                },
                                onDragCancel = {
                                    draggingId = null
                                    dragOffset = 0f
                                }
                            )
                        }
                    },
                onRemove = { onRemoveSong(song.id) }
            )
        }
    }
}

@Composable
private fun SongRow(
    song: SongEntry,
    shuffle: Boolean,
    modifier: Modifier = Modifier,
    onRemove: () -> Unit
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), RoundedCornerShape(16.dp))
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.MusicNote,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = song.title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "Progresses with each call",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (!shuffle) {
            Icon(
                imageVector = Icons.Filled.DragHandle,
                contentDescription = "Drag",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(end = 6.dp)
            )
        }
        IconButton(onClick = onRemove) {
            Icon(
                imageVector = Icons.Filled.Delete,
                contentDescription = "Remove",
                tint = MaterialTheme.colorScheme.error
            )
        }
    }
}

@Composable
private fun ContactSettingsDialog(
    contact: ContactEntry,
    songs: List<SongEntry>,
    onDismiss: () -> Unit,
    onAssignSong: (String?) -> Unit,
    onUrgencyThresholdChange: (Int) -> Unit,
    onPickUrgencyTone: () -> Unit,
    onClearUrgencyTone: () -> Unit,
    onSetUrgencyTone: (String, Uri) -> Unit
) {
    var showUrgencySongPicker by remember { mutableStateOf(false) }
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Contact settings") },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = contact.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "Progression track",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
                OutlinedButton(onClick = { onAssignSong(null) }, modifier = Modifier.fillMaxWidth()) {
                    Text("Use global playlist")
                }
                songs.forEach { song ->
                    Button(
                        onClick = { onAssignSong(song.id) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(song.title, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Urgency ringer",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = if (contact.urgencyThreshold > 0) {
                        "Play after ${contact.urgencyThreshold} consecutive calls"
                    } else {
                        "Urgency disabled"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Slider(
                    value = contact.urgencyThreshold.toFloat(),
                    valueRange = 0f..10f,
                    steps = 9,
                    enabled = true,
                    onValueChange = { onUrgencyThresholdChange(it.toInt()) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                )
                Text(
                    text = contact.urgencyToneTitle ?: "No urgency tone selected",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Button(onClick = { showUrgencySongPicker = true }, modifier = Modifier.fillMaxWidth()) {
                    Text("Select from my songs")
                }
                OutlinedButton(onClick = onPickUrgencyTone, modifier = Modifier.fillMaxWidth()) {
                    Text("Pick from device files")
                }
                if (contact.urgencyToneUri != null) {
                    OutlinedButton(onClick = onClearUrgencyTone, modifier = Modifier.fillMaxWidth()) {
                        Text("Clear urgency tone")
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )

    if (showUrgencySongPicker) {
        AlertDialog(
            onDismissRequest = { showUrgencySongPicker = false },
            title = { Text("Select urgency tone") },
            text = {
                Column(
                    modifier = Modifier
                        .heightIn(max = 400.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (songs.isEmpty()) {
                        Text(
                            text = "No songs in your list yet. Add songs first.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        songs.forEach { song ->
                            Button(
                                onClick = {
                                    song.uri?.let { uri ->
                                        onSetUrgencyTone(contact.id, Uri.parse(uri))
                                    }
                                    showUrgencySongPicker = false
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(song.title, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showUrgencySongPicker = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun SectionCard(
    modifier: Modifier = Modifier,
    accent: Color = MaterialTheme.colorScheme.surface,
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
    content: @Composable () -> Unit
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .shadow(8.dp, RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier
                .background(accent)
                .padding(18.dp)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            androidx.compose.runtime.CompositionLocalProvider(
                androidx.compose.material3.LocalContentColor provides contentColor
            ) {
                content()
            }
        }
    }
}

@Composable
private fun BoxScope.MusicBackdrop() {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height
        drawCircle(
            color = Color(0xFFB6D7F2).copy(alpha = 0.35f),
            radius = width * 0.55f,
            center = androidx.compose.ui.geometry.Offset(width * 0.85f, height * 0.1f)
        )
        drawCircle(
            color = Color(0xFF7CB2E4).copy(alpha = 0.25f),
            radius = width * 0.45f,
            center = androidx.compose.ui.geometry.Offset(width * 0.1f, height * 0.2f)
        )
    }
    Icon(
        imageVector = Icons.Filled.MusicNote,
        contentDescription = null,
        modifier = Modifier
            .size(120.dp)
            .align(Alignment.TopEnd)
            .padding(top = 20.dp, end = 12.dp)
            .alpha(0.08f),
        tint = MaterialTheme.colorScheme.primary
    )
    Icon(
        imageVector = Icons.Filled.MusicNote,
        contentDescription = null,
        modifier = Modifier
            .size(90.dp)
            .align(Alignment.BottomStart)
            .padding(bottom = 60.dp, start = 20.dp)
            .alpha(0.06f),
        tint = MaterialTheme.colorScheme.secondary
    )
}

private fun requiredPermissions(): Array<String> = arrayOf(
    Manifest.permission.READ_PHONE_STATE,
    Manifest.permission.READ_CONTACTS,
    Manifest.permission.READ_MEDIA_AUDIO,
    Manifest.permission.POST_NOTIFICATIONS
)

private fun resolveContact(context: Context, contactUri: Uri): ContactEntry? {
    val projection = arrayOf(
        ContactsContract.Contacts._ID,
        ContactsContract.Contacts.DISPLAY_NAME
    )
    context.contentResolver.query(contactUri, projection, null, null, null).use { cursor ->
        return if (cursor != null && cursor.moveToFirst()) {
            val id = cursor.getString(0)
            val name = cursor.getString(1) ?: "Unknown"
            ContactEntry(id = id, name = name)
        } else {
            null
        }
    }
}

private fun urgencyLabel(contact: ContactEntry): String {
    return when {
        contact.urgencyThreshold == 0 -> "Urgency off"
        contact.urgencyToneTitle.isNullOrBlank() -> "Urgency after ${contact.urgencyThreshold} calls - pick a tone"
        else -> "Urgency after ${contact.urgencyThreshold} calls - ${contact.urgencyToneTitle}"
    }
}

@Composable
private fun HowToAddMusicHelper() {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = "How to add music",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = "Search & add songs from Spotify or YouTube Music using the sections above. Songs will be downloaded for offline playback.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = "You can also use local audio files (MP3/WAV/M4A) by selecting them from your device.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = "Tip: Share audio files from apps like Zedge to import them here.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = "⚠️ IMPORTANT: Set your phone's default ringtone to Silent in Settings > Sounds for best results. RingerSong will still play your progressive ringer!",
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

private fun openZedge(context: Context) {
    val packageName = "net.zedge.android"
    val launchIntent = context.packageManager.getLaunchIntentForPackage(packageName)
    if (launchIntent != null) {
        context.startActivity(launchIntent)
        return
    }
    val storeIntent = Intent(
        Intent.ACTION_VIEW,
        Uri.parse("market://details?id=$packageName")
    ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    runCatching { context.startActivity(storeIntent) }.onFailure {
        val webIntent = Intent(
            Intent.ACTION_VIEW,
            Uri.parse("https://play.google.com/store/apps/details?id=$packageName")
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { context.startActivity(webIntent) }
    }
}
