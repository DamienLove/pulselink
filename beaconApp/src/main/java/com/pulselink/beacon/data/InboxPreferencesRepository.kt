package com.pulselink.beacon.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.inboxDataStore by preferencesDataStore(name = "beacon_inbox_prefs")

class InboxPreferencesRepository(private val context: Context) {

    private val pinnedKey = stringPreferencesKey("pinned_threads")
    private val archivedKey = stringPreferencesKey("archived_threads")
    private val delayedSendKey = androidx.datastore.preferences.core.intPreferencesKey("delayed_send_timeout")

    val flow: Flow<InboxState> = context.inboxDataStore.data.map { prefs ->
        val pinned = decodeSet(prefs[pinnedKey])
        val archived = decodeSet(prefs[archivedKey])
        val delayedSend = prefs[delayedSendKey] ?: 5
        InboxState(pinnedThreadIds = pinned, archivedThreadIds = archived, delayedSendTimeout = delayedSend)
    }

    suspend fun setDelayedSendTimeout(seconds: Int) {
        context.inboxDataStore.edit { prefs ->
            prefs[delayedSendKey] = seconds.coerceIn(0, 30)
        }
    }

    suspend fun togglePin(threadId: Long) {
        if (threadId <= 0) return
        context.inboxDataStore.edit { prefs ->
            val current = decodeSet(prefs[pinnedKey]).toMutableSet()
            if (current.contains(threadId)) {
                current.remove(threadId)
            } else {
                current.add(threadId)
            }
            prefs[pinnedKey] = encodeSet(current)
        }
    }

    suspend fun toggleArchive(threadId: Long) {
        if (threadId <= 0) return
        context.inboxDataStore.edit { prefs ->
            val current = decodeSet(prefs[archivedKey]).toMutableSet()
            if (current.contains(threadId)) {
                current.remove(threadId)
            } else {
                current.add(threadId)
            }
            prefs[archivedKey] = encodeSet(current)
        }
    }

    /**
     * Decodes a comma-separated string of Long IDs.
     * Note: CSV encoding has O(N) parsing overhead and is limited by max string size in Preferences.
     * For extremely large datasets (thousands of pinned/archived items), migration to Proto DataStore
     * or Room is recommended.
     */
    private fun decodeSet(raw: String?): Set<Long> {
        if (raw.isNullOrBlank()) return emptySet()
        return raw.split(",").mapNotNull { it.toLongOrNull() }.toSet()
    }

    private fun encodeSet(set: Set<Long>): String {
        return set.joinToString(",")
    }
}
