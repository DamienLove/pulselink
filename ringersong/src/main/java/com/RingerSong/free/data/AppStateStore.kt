package com.RingerSong.free.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.random.Random

private val Context.appStateDataStore: DataStore<Preferences> by preferencesDataStore(name = "ringer_state")

@Singleton
class AppStateStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private val stateKey = stringPreferencesKey("app_state_json")

    val stateFlow: Flow<AppState> = context.appStateDataStore.data
        .map { prefs ->
            val raw = prefs[stateKey]
            if (raw.isNullOrBlank()) {
                AppState()
            } else {
                runCatching { json.decodeFromString(AppState.serializer(), raw) }
                    .getOrElse { AppState() }
            }
        }
        .map { normalize(it) }
        .distinctUntilChanged()

    suspend fun update(transform: (AppState) -> AppState) {
        context.appStateDataStore.edit { prefs ->
            val current = prefs[stateKey]?.let {
                runCatching { json.decodeFromString(AppState.serializer(), it) }.getOrNull()
            } ?: AppState()
            val updated = normalize(transform(current))
            prefs[stateKey] = json.encodeToString(AppState.serializer(), updated)
        }
    }

    private fun normalize(state: AppState): AppState {
        val songIds = state.songs.map { it.id }
        val order = state.songOrder.filter { it in songIds } +
            songIds.filterNot { it in state.songOrder }
        val contacts = state.contacts.map { contact ->
            if (contact.assignedSongId != null && contact.assignedSongId !in songIds) {
                contact.copy(assignedSongId = null, segmentIndex = 0)
            } else {
                contact
            }
        }

        val filteredShuffle = state.playback.shuffleOrder.filter { it in songIds }
        val shuffleOrder = when {
            songIds.isEmpty() -> emptyList()
            filteredShuffle.size != songIds.size ->
                songIds.shuffled(Random(System.currentTimeMillis()))
            else -> filteredShuffle
        }

        val playlistIndex = when {
            songIds.isEmpty() -> 0
            state.playback.globalPlaylistIndex !in order.indices -> 0
            else -> state.playback.globalPlaylistIndex
        }

        val shuffleIndex = when {
            shuffleOrder.isEmpty() -> 0
            state.playback.shuffleIndex !in shuffleOrder.indices -> 0
            else -> state.playback.shuffleIndex
        }

        val sanitizedCalls = state.playback.consecutiveCalls.coerceAtLeast(0)
        val playback = state.playback.copy(
            globalPlaylistIndex = playlistIndex,
            shuffleOrder = shuffleOrder,
            shuffleIndex = shuffleIndex,
            lastCallerKey = if (sanitizedCalls == 0) null else state.playback.lastCallerKey,
            consecutiveCalls = sanitizedCalls
        )

        return state.copy(
            songOrder = order,
            contacts = contacts,
            playback = playback
        )
    }
}
