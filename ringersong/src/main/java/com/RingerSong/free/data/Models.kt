package com.RingerSong.free.data

import kotlinx.serialization.Serializable

@Serializable
data class AppState(
    val settings: SettingsState = SettingsState(),
    val songs: List<SongEntry> = emptyList(),
    val songOrder: List<String> = emptyList(),
    val contacts: List<ContactEntry> = emptyList(),
    val playback: PlaybackState = PlaybackState(),
    val theme: ThemeConfig? = null
)

@Serializable
data class SettingsState(
    val enabled: Boolean = true,
    val shuffle: Boolean = true,
    val segmentSeconds: Int = 30,
    val maxSongs: Int = 10,
    val notificationsEnabled: Boolean = false
)

@Serializable
data class SongEntry(
    val id: String,
    val title: String,
    val uri: String,
    val durationMs: Long? = null,
    val addedAt: Long = System.currentTimeMillis()
)

@Serializable
data class ContactEntry(
    val id: String,
    val name: String,
    val assignedSongId: String? = null,
    val segmentIndex: Int = 0,
    val urgencyThreshold: Int = 0,
    val urgencyToneUri: String? = null,
    val urgencyToneTitle: String? = null,
    val urgencyToneDurationMs: Long? = null
)

@Serializable
data class PlaybackState(
    val globalPlaylistIndex: Int = 0,
    val globalSegmentIndex: Int = 0,
    val shuffleOrder: List<String> = emptyList(),
    val shuffleIndex: Int = 0,
    val lastCallerKey: String? = null,
    val consecutiveCalls: Int = 0
)

data class SegmentPlay(
    val song: SongEntry,
    val startMs: Long,
    val durationMs: Long
)
