package com.RingerSong.free.data

import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min

object ProgressionEngine {
    private const val FALLBACK_DURATION_MS = 180_000L

    fun advance(state: AppState, contactId: String?, callerKey: String?): Pair<AppState, SegmentPlay?> {
        if (!state.settings.enabled) {
            return state to null
        }

        val segmentMs = max(5, state.settings.segmentSeconds).toLong() * 1000L
        val songMap = state.songs.associateBy { it.id }

        val contact = contactId?.let { id ->
            state.contacts.firstOrNull { it.id == id }
        }

        val (callerPlayback, consecutiveCalls) =
            updateCallerTracking(state.playback, callerKey ?: contactId)

        if (contact != null && shouldTriggerUrgency(contact, consecutiveCalls)) {
            val urgencySong = SongEntry(
                id = "urgency:${contact.id}",
                title = contact.urgencyToneTitle ?: "Urgency tone",
                uri = contact.urgencyToneUri ?: return state to null,
                durationMs = contact.urgencyToneDurationMs
            )
            val (segmentPlay, _, _) = computeSegment(urgencySong, 0, segmentMs)
            val updatedPlayback = callerPlayback.copy(consecutiveCalls = 0)
            return state.copy(playback = updatedPlayback) to segmentPlay
        }

        val updatedState = state.copy(playback = callerPlayback)
        val assignedSong = contact?.assignedSongId?.let { songMap[it] }
        if (contact != null && assignedSong != null) {
            val (nextContact, segmentPlay) = advanceForSong(contact, assignedSong, segmentMs)
            val updatedContacts = updatedState.contacts.map { entry ->
                if (entry.id == nextContact.id) nextContact else entry
            }
            return updatedState.copy(contacts = updatedContacts) to segmentPlay
        }

        return advanceGlobal(updatedState, songMap, segmentMs)
    }

    private fun advanceGlobal(
        state: AppState,
        songMap: Map<String, SongEntry>,
        segmentMs: Long
    ): Pair<AppState, SegmentPlay?> {
        val songIds = if (state.settings.shuffle) {
            state.playback.shuffleOrder
        } else {
            state.songOrder
        }

        if (songIds.isEmpty()) {
            return state to null
        }

        val playlistIndex = if (state.settings.shuffle) {
            state.playback.shuffleIndex
        } else {
            state.playback.globalPlaylistIndex
        }

        val safeIndex = playlistIndex.coerceIn(songIds.indices)
        val songId = songIds[safeIndex]
        val song = songMap[songId] ?: return state to null
        val (segmentPlay, nextSegmentIndex, completed) =
            computeSegment(song, state.playback.globalSegmentIndex, segmentMs)

        val nextPlayback = if (state.settings.shuffle) {
            val nextIndex = if (completed) {
                (safeIndex + 1) % songIds.size
            } else {
                safeIndex
            }
            state.playback.copy(
                globalSegmentIndex = nextSegmentIndex,
                shuffleIndex = nextIndex
            )
        } else {
            val nextIndex = if (completed) {
                (safeIndex + 1) % songIds.size
            } else {
                safeIndex
            }
            state.playback.copy(
                globalSegmentIndex = nextSegmentIndex,
                globalPlaylistIndex = nextIndex
            )
        }

        return state.copy(playback = nextPlayback) to segmentPlay
    }

    private fun advanceForSong(
        contact: ContactEntry,
        song: SongEntry,
        segmentMs: Long
    ): Pair<ContactEntry, SegmentPlay> {
        val (segmentPlay, nextSegmentIndex, _) =
            computeSegment(song, contact.segmentIndex, segmentMs)
        val updatedContact = contact.copy(segmentIndex = nextSegmentIndex)
        return updatedContact to segmentPlay
    }

    private fun computeSegment(
        song: SongEntry,
        currentIndex: Int,
        segmentMs: Long
    ): Triple<SegmentPlay, Int, Boolean> {
        val duration = song.durationMs ?: FALLBACK_DURATION_MS
        val totalSegments = max(1, ceil(duration.toDouble() / segmentMs).toInt())
        val safeIndex = currentIndex.coerceAtLeast(0) % totalSegments
        val startMs = safeIndex * segmentMs
        val remaining = max(0, duration - startMs)
        val playDuration = min(segmentMs, remaining.coerceAtLeast(1_000L))
        val nextIndex = if (safeIndex + 1 >= totalSegments) 0 else safeIndex + 1
        val completed = nextIndex == 0
        return Triple(
            SegmentPlay(song = song, startMs = startMs, durationMs = playDuration),
            nextIndex,
            completed
        )
    }

    private fun updateCallerTracking(
        playback: PlaybackState,
        callerKey: String?
    ): Pair<PlaybackState, Int> {
        if (callerKey.isNullOrBlank()) {
            return playback.copy(lastCallerKey = null, consecutiveCalls = 0) to 0
        }
        val consecutive = if (callerKey == playback.lastCallerKey) {
            playback.consecutiveCalls + 1
        } else {
            1
        }
        val updated = playback.copy(lastCallerKey = callerKey, consecutiveCalls = consecutive)
        return updated to consecutive
    }

    private fun shouldTriggerUrgency(contact: ContactEntry, consecutiveCalls: Int): Boolean {
        val threshold = contact.urgencyThreshold
        return threshold > 0 &&
            consecutiveCalls >= threshold &&
            !contact.urgencyToneUri.isNullOrBlank()
    }
}
