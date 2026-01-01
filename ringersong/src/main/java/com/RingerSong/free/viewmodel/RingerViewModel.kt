package com.RingerSong.free.viewmodel

import android.app.Application
import android.content.ContentResolver
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.RingerSong.free.data.AppState
import com.RingerSong.free.data.AppStateStore
import com.RingerSong.free.data.ContactEntry
import com.RingerSong.free.data.SongEntry
import com.RingerSong.free.data.SpotifyRepository
import com.RingerSong.free.data.SpotifyTrack
import com.RingerSong.free.data.SpotifyArtist
import com.RingerSong.free.data.DownloadError
import com.RingerSong.free.data.DownloadResult
import com.RingerSong.free.data.resolveSongMetadata
import com.RingerSong.free.data.ThemeConfig
import com.pulselink.shared.ui.theme.SharedThemePreferences
import com.RingerSong.free.data.SongMetadata
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import okhttp3.OkHttpClient
import java.util.UUID
import java.util.concurrent.TimeUnit

data class SpotifySearchState(
    val query: String = "",
    val results: List<SpotifyTrack> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

data class AddSongsResult(
    val addedCount: Int,
    val skippedCount: Int
)

data class AuthState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

class RingerViewModel(application: Application) : AndroidViewModel(application) {
    private val store = AppStateStore(application)
    private val resolver: ContentResolver = application.contentResolver
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    val state = store.stateFlow.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        AppState()
    )

    private val _authState = MutableStateFlow(AuthState())
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    private val _currentUser = MutableStateFlow(auth.currentUser)
    val currentUser: StateFlow<com.google.firebase.auth.FirebaseUser?> = _currentUser.asStateFlow()

    private val spotifyRepository = SpotifyRepository(
        OkHttpClient.Builder()
            .callTimeout(20, TimeUnit.SECONDS)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .build()
    )

    // New API repositories for enhanced features
    private val spotifyDownloader = com.RingerSong.free.data.SpotifyDownloaderRepository(application)
    private val youtubeMusicRepo = com.RingerSong.free.data.YouTubeMusicRepository(application)
    private val truecallerRepo = com.RingerSong.free.data.TruecallerRepository(application)

    private val _searchState = MutableStateFlow(SpotifySearchState())
    val searchState: StateFlow<SpotifySearchState> = _searchState.asStateFlow()

    private val _youtubeSearchState = MutableStateFlow(SpotifySearchState())
    val youtubeSearchState: StateFlow<SpotifySearchState> = _youtubeSearchState.asStateFlow()

    init {
        auth.addAuthStateListener { firebaseAuth ->
            _currentUser.value = firebaseAuth.currentUser
            if (firebaseAuth.currentUser != null) {
                startFirestoreSync(firebaseAuth.currentUser!!.uid)
            }
        }
    }

    fun signInWithEmail(email: String, pass: String, onSuccess: () -> Unit) {
        if (email.isBlank() || pass.isBlank()) {
            _authState.update { it.copy(errorMessage = "Email and password required") }
            return
        }
        _authState.update { it.copy(isLoading = true, errorMessage = null) }
        auth.signInWithEmailAndPassword(email, pass)
            .addOnSuccessListener {
                _authState.update { it.copy(isLoading = false) }
                onSuccess()
            }
            .addOnFailureListener { e ->
                _authState.update { it.copy(isLoading = false, errorMessage = e.message) }
            }
    }

    fun signUpWithEmail(email: String, pass: String, onSuccess: () -> Unit) {
        if (email.isBlank() || pass.isBlank()) {
            _authState.update { it.copy(errorMessage = "Email and password required") }
            return
        }
        _authState.update { it.copy(isLoading = true, errorMessage = null) }
        auth.createUserWithEmailAndPassword(email, pass)
            .addOnSuccessListener {
                // Create user doc
                val user = it.user
                if (user != null) {
                    val userMap = hashMapOf(
                        "email" to user.email,
                        "createdAt" to com.google.firebase.Timestamp.now()
                    )
                    db.collection("users").document(user.uid).set(userMap)
                }
                _authState.update { it.copy(isLoading = false) }
                onSuccess()
            }
            .addOnFailureListener { e ->
                _authState.update { it.copy(isLoading = false, errorMessage = e.message) }
            }
    }

    private fun startFirestoreSync(uid: String?) {
        if (uid == null) return

        // Listen for Theme Preferences
        db.collection("users").document(uid).addSnapshotListener { snapshot, e ->
            if (e != null || snapshot == null || !snapshot.exists()) return@addSnapshotListener

            val prefs = snapshot.get("themePreferences") as? Map<*, *>
            val config = SharedThemePreferences.fromMap(prefs)
            viewModelScope.launch {
                store.update { it.copy(theme = config) }
            }
        }

        db.collection("users").document(uid).collection("ringer_playlist")
            .orderBy("addedAt", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, e ->
                if (e != null || snapshot == null) return@addSnapshotListener

                val firestoreSongs: List<SongEntry> = snapshot.documents.mapNotNull { doc ->
                    val uri = doc.getString("uri")
                    if (uri.isNullOrBlank()) return@mapNotNull null // Skip songs with invalid URIs

                    SongEntry(
                        id = doc.id,
                        title = (doc.getString("title") ?: "Unknown") + " - " + (doc.getString("artist") ?: "Unknown"),
                        uri = uri,
                        durationMs = doc.getLong("durationMs"),
                        addedAt = doc.getTimestamp("addedAt")?.toDate()?.time ?: System.currentTimeMillis()
                    )
                }

                viewModelScope.launch {
                    store.update { current ->
                        // Keep local songs, but update Spotify songs from Firestore
                        val localOnlySongs: List<SongEntry> = current.songs.filterNot { it.uri.startsWith("spotify:") }
                        val combinedSongs: List<SongEntry> = localOnlySongs + firestoreSongs

                        // Recalculate order: maintain local order for local files, append firestore ones at end if new
                        val currentOrderList: List<String> = current.songOrder
                        val combinedOrder: List<String> = currentOrderList.filter { id ->
                            combinedSongs.any { s -> s.id == id }
                        } + firestoreSongs.map { it.id }.filterNot { it in currentOrderList }

                        current.copy(songs = combinedSongs, songOrder = combinedOrder)
                    }
                }
            }
    }

    fun toggleEnabled(enabled: Boolean) {
        viewModelScope.launch {
            store.update { it.copy(settings = it.settings.copy(enabled = enabled)) }
        }
    }

    fun toggleShuffle(shuffle: Boolean) {
        viewModelScope.launch {
            store.update { it.copy(settings = it.settings.copy(shuffle = shuffle)) }
        }
    }

    fun toggleNotifications(enabled: Boolean) {
        viewModelScope.launch {
            store.update { it.copy(settings = it.settings.copy(notificationsEnabled = enabled)) }
        }
    }

    fun updateSegmentSeconds(seconds: Int) {
        viewModelScope.launch {
            store.update { it.copy(settings = it.settings.copy(segmentSeconds = seconds.coerceIn(5, 90))) }
        }
    }

    fun updateMaxSongs(maxSongs: Int) {
        viewModelScope.launch {
            store.update { it.copy(settings = it.settings.copy(maxSongs = maxSongs.coerceIn(1, 200))) }
        }
    }

    fun updateSearchQuery(query: String) {
        _searchState.update {
            it.copy(query = query, errorMessage = null)
        }
    }

    fun searchSpotify() {
        val query = _searchState.value.query.trim()
        if (query.isBlank()) return

        _searchState.update { it.copy(isLoading = true, errorMessage = null, results = emptyList()) }

        viewModelScope.launch {
            runCatching { spotifyRepository.searchTracks(query) }
                .onSuccess { results ->
                    _searchState.update { it.copy(isLoading = false, results = results) }
                }
                .onFailure { error ->
                    val msg = error.message ?: "Unknown error"
                    _searchState.update { it.copy(isLoading = false, errorMessage = msg) }
                }
        }
    }

    fun clearSearch() {
        _searchState.value = SpotifySearchState()
    }

    fun updateYouTubeSearchQuery(query: String) {
        _youtubeSearchState.update {
            it.copy(query = query, errorMessage = null)
        }
    }

    fun searchYouTubeMusic() {
        val query = _youtubeSearchState.value.query.trim()
        if (query.isBlank()) return

        _youtubeSearchState.update { it.copy(isLoading = true, errorMessage = null, results = emptyList()) }

        viewModelScope.launch {
            runCatching { youtubeMusicRepo.searchSongs(query) }
                .onSuccess { results ->
                    // Convert YouTubeSearchResult to SpotifyTrack for UI compatibility
                    val tracks = results?.mapNotNull { ytResult ->
                        SpotifyTrack(
                            id = ytResult.videoId,
                            name = ytResult.title,
                            uri = "youtube:video:${ytResult.videoId}",
                            artists = ytResult.artist?.let { listOf(SpotifyArtist(it)) } ?: emptyList(),
                            duration_ms = parseDurationToMs(ytResult.duration)
                        )
                    } ?: emptyList()
                    _youtubeSearchState.update { it.copy(isLoading = false, results = tracks) }
                }
                .onFailure { error ->
                    val msg = error.message ?: "Unknown error"
                    _youtubeSearchState.update { it.copy(isLoading = false, errorMessage = msg) }
                }
        }
    }

    fun clearYouTubeSearch() {
        _youtubeSearchState.value = SpotifySearchState()
    }

    private fun parseDurationToMs(duration: String?): Long? {
        if (duration.isNullOrBlank()) return null
        return try {
            val parts = duration.split(":")
            when (parts.size) {
                1 -> parts[0].toLong() * 1000  // seconds only
                2 -> (parts[0].toLong() * 60 + parts[1].toLong()) * 1000  // mm:ss
                3 -> (parts[0].toLong() * 3600 + parts[1].toLong() * 60 + parts[2].toLong()) * 1000  // hh:mm:ss
                else -> null
            }
        } catch (e: Exception) {
            null
        }
    }

    fun addSpotifyTrack(track: SpotifyTrack, onResult: (String) -> Unit) {
        val uid = auth.currentUser?.uid ?: run {
            onResult("Error: Please sign in to add songs")
            return
        }

        viewModelScope.launch {
            if (track.uri.isNullOrBlank()) {
                onResult("Error: Invalid track URI")
                return@launch
            }

            val current = state.value
            if (current.songs.size >= current.settings.maxSongs) {
                onResult("Playlist is full (max ${current.settings.maxSongs})")
                return@launch
            }

            if (current.songs.any { it.uri == track.uri }) {
                onResult("Song already in playlist")
                return@launch
            }

            // Download the track first (handle both Spotify and YouTube)
            onResult("Downloading ${track.name}...")
            val downloadResult = if (track.uri!!.startsWith("youtube:")) {
                val videoId = track.uri!!.substringAfterLast(":")
                youtubeMusicRepo.downloadTrack(videoId)
            } else {
                spotifyDownloader.downloadTrack(track.uri!!)
            }

            val localFilePath = when (downloadResult) {
                is DownloadResult.Success -> {
                    clearDownloadError()
                    downloadResult.filePath
                }
                is DownloadResult.Failure -> {
                    val message = mapDownloadError(downloadResult.error)
                    setDownloadError(message)
                    return@launch
                }
            }

            // Create the song entry with local file URI
            val localFileUri = "file://$localFilePath"
            val songEntry = SongEntry(
                id = track.id ?: java.util.UUID.randomUUID().toString(),
                title = "${track.name ?: "Unknown Track"} - ${track.artists?.mapNotNull { it.name }?.joinToString(", ") ?: "Unknown Artist"}",
                uri = localFileUri,
                durationMs = track.duration_ms,
                addedAt = System.currentTimeMillis()
            )

            // Update local state
            withContext(Dispatchers.IO) {
                store.update { current ->
                    val updatedSongs = current.songs + songEntry
                    val updatedOrder = current.songOrder + songEntry.id
                    current.copy(songs = updatedSongs, songOrder = updatedOrder)
                }
            }

            // Then sync to Firestore
            val trackData = mapOf(
                "spotifyId" to track.id,
                "uri" to localFileUri,
                "spotifyUri" to track.uri,
                "title" to (track.name ?: "Unknown Track"),
                "artist" to (track.artists?.mapNotNull { it.name }?.joinToString(", ") ?: "Unknown Artist"),
                "durationMs" to (track.duration_ms ?: 0L),
                "addedAt" to com.google.firebase.Timestamp.now()
            )

            db.collection("users").document(uid).collection("ringer_playlist")
                .add(trackData)
                .addOnSuccessListener {
                    onResult("Added ${track.name}")
                }
                .addOnFailureListener { e ->
                    // Remove from local state if Firestore sync fails
                    viewModelScope.launch {
                        withContext(Dispatchers.IO) {
                            store.update { current ->
                                val updatedSongs = current.songs.filter { it.id != songEntry.id }
                                val updatedOrder = current.songOrder.filter { it != songEntry.id }
                                current.copy(songs = updatedSongs, songOrder = updatedOrder)
                            }
                        }
                    }
                    onResult("Failed to sync: ${e.message}")
                }
        }
    }

    fun addSongs(uris: List<Uri>, onResult: (AddSongsResult) -> Unit) {
        if (uris.isEmpty()) return
        viewModelScope.launch {
            val current = state.value
            val existingUris = current.songs.map { it.uri }.toSet()
            val uniqueUris = uris.filterNot { it.toString() in existingUris }
            val slots = (current.settings.maxSongs - current.songs.size).coerceAtLeast(0)
            val accepted = uniqueUris.take(slots)
            val skipped = (uris.size - accepted.size).coerceAtLeast(0)

            accepted.forEach { uri ->
                runCatching {
                    resolver.takePersistableUriPermission(
                        uri,
                        android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                }
            }

            val newSongs = accepted.map { uri ->
                val metadata = resolveSongMetadata(getApplication(), uri)
                SongEntry(
                    id = UUID.randomUUID().toString(),
                    title = metadata.title,
                    uri = uri.toString(),
                    durationMs = metadata.durationMs
                )
            }

            store.update { currentState ->
                val updatedSongs = currentState.songs + newSongs
                val updatedOrder = currentState.songOrder + newSongs.map { it.id }
                currentState.copy(songs = updatedSongs, songOrder = updatedOrder)
            }

            onResult(AddSongsResult(addedCount = accepted.size, skippedCount = skipped))
        }
    }

    fun removeSong(songId: String) {
        val uid = auth.currentUser?.uid
        viewModelScope.launch {
            val current = state.value
            val song = current.songs.find { it.id == songId }

            if (song != null && song.uri.startsWith("spotify:") && uid != null) {
                // If it's a Spotify song, remove from Firestore (listener will update local)
                db.collection("users").document(uid).collection("ringer_playlist")
                    .document(songId)
                    .delete()
            } else {
                // If local song, just update local state
                store.update { current ->
                    val songs = current.songs.filterNot { it.id == songId }
                    val order = current.songOrder.filterNot { it == songId }
                    current.copy(songs = songs, songOrder = order)
                }
            }
        }
    }

    fun moveSong(fromIndex: Int, toIndex: Int) {
        viewModelScope.launch {
            store.update { current ->
                val order = current.songOrder.toMutableList()
                if (fromIndex !in order.indices || toIndex !in order.indices) {
                    return@update current
                }
                val moved = order.removeAt(fromIndex)
                order.add(toIndex, moved)
                current.copy(songOrder = order)
            }
        }
    }

    fun addOrUpdateContact(entry: ContactEntry) {
        viewModelScope.launch {
            store.update { current ->
                val existingIndex = current.contacts.indexOfFirst { it.id == entry.id }
                val updatedContacts = if (existingIndex == -1) {
                    current.contacts + entry
                } else {
                    current.contacts.mapIndexed { index, item ->
                        if (index == existingIndex) {
                            item.copy(name = entry.name)
                        } else {
                            item
                        }
                    }
                }
                current.copy(contacts = updatedContacts)
            }
        }
    }

    fun assignContactSong(contactId: String, songId: String?) {
        viewModelScope.launch {
            store.update { current ->
                val updated = current.contacts.map { contact ->
                    if (contact.id == contactId) {
                        contact.copy(assignedSongId = songId, segmentIndex = 0)
                    } else {
                        contact
                    }
                }
                current.copy(contacts = updated)
            }
        }
    }

    fun updateUrgencyThreshold(contactId: String, threshold: Int) {
        viewModelScope.launch {
            store.update { current ->
                val updated = current.contacts.map { contact ->
                    if (contact.id == contactId) {
                        contact.copy(urgencyThreshold = threshold.coerceIn(0, 10))
                    } else {
                        contact
                    }
                }
                current.copy(contacts = updated)
            }
        }
    }

    fun setUrgencyTone(contactId: String, uri: Uri) {
        viewModelScope.launch {
            runCatching {
                resolver.takePersistableUriPermission(
                    uri,
                    android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
            val metadata = resolveSongMetadata(getApplication(), uri)
            store.update { current ->
                val updated = current.contacts.map { contact ->
                    if (contact.id == contactId) {
                        contact.copy(
                            urgencyToneUri = uri.toString(),
                            urgencyToneTitle = metadata.title,
                            urgencyToneDurationMs = metadata.durationMs
                        )
                    } else {
                        contact
                    }
                }
                current.copy(contacts = updated)
            }
        }
    }

    fun clearUrgencyTone(contactId: String) {
        viewModelScope.launch {
            store.update { current ->
                val updated = current.contacts.map { contact ->
                    if (contact.id == contactId) {
                        contact.copy(
                            urgencyToneUri = null,
                            urgencyToneTitle = null,
                            urgencyToneDurationMs = null,
                            urgencyThreshold = 0
                        )
                    } else {
                        contact
                    }
                }
                current.copy(contacts = updated)
            }
        }
    }

    fun resetGlobalProgress() {
        viewModelScope.launch {
            store.update { current ->
                current.copy(playback = current.playback.copy(globalSegmentIndex = 0, globalPlaylistIndex = 0))
            }
        }
    }

    // === Spotify Downloader Methods ===
    fun downloadSpotifyTrack(spotifyUrl: String, onComplete: (String?) -> Unit) {
        viewModelScope.launch {
            when (val result = spotifyDownloader.downloadTrack(spotifyUrl)) {
                is DownloadResult.Success -> {
                    clearDownloadError()
                    onComplete(result.filePath)
                }
                is DownloadResult.Failure -> {
                    val message = mapDownloadError(result.error)
                    setDownloadError(message)
                    onComplete(null)
                }
            }
        }
    }

    fun isTrackOffline(spotifyUri: String): Boolean {
        return spotifyDownloader.getLocalFilePathFromUri(spotifyUri) != null
    }

    // === YouTube Music Methods ===
    fun fetchYouTubePlaylist(playlistId: String, onComplete: (List<com.RingerSong.free.data.YouTubeVideo>?) -> Unit) {
        viewModelScope.launch {
            val videos = youtubeMusicRepo.getPlaylistVideos(playlistId)
            onComplete(videos)
        }
    }

    fun searchYouTubeMusic(query: String, onComplete: (List<com.RingerSong.free.data.YouTubeSearchResult>?) -> Unit) {
        viewModelScope.launch {
            val results = youtubeMusicRepo.searchSongs(query)
            onComplete(results)
        }
    }

    // === Truecaller Methods ===
    fun testTruecaller(onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            val success = truecallerRepo.testConnection()
            onComplete(success)
        }
    }

    fun lookupCaller(phoneNumber: String, onComplete: (com.RingerSong.free.data.CallerInfo?) -> Unit) {
        viewModelScope.launch {
            val info = truecallerRepo.getCallerInfo(phoneNumber, "US")
            onComplete(info)
        }
    }

    private fun mapDownloadError(error: DownloadError): String {
        return when (error) {
            is DownloadError.NoApiKey -> "Download unavailable - app not configured"
            DownloadError.NetworkUnavailable -> "No internet connection"
            is DownloadError.RateLimitExceeded -> "Too many requests - try again in a few minutes"
            is DownloadError.ApiError -> {
                when (error.statusCode) {
                    in 400..499 -> "Unable to find track"
                    in 500..599 -> "Service temporarily unavailable"
                    else -> error.message ?: "Download failed"
                }
            }
            is DownloadError.AudioDownloadFailed -> error.message ?: "Audio download failed"
            is DownloadError.NoDownloadUrl -> "Download link unavailable"
            is DownloadError.UnknownError -> error.message ?: "Download failed"
        }
    }

    private fun setDownloadError(message: String) {
        viewModelScope.launch {
            store.update { it.copy(downloadError = message) }
        }
    }

    fun clearDownloadError() {
        viewModelScope.launch {
            store.update { it.copy(downloadError = null) }
        }
    }
}
