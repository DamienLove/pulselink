package com.pulselink.widget

import android.content.Context
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private const val PREFS_NAME = "widget_cache"
private const val KEY_CONTACTS = "cached_contacts"
private const val KEY_THREADS = "cached_threads"
private const val KEY_CONTACTS_UPDATED_AT = "contacts_updated_at"
private const val KEY_THREADS_UPDATED_AT = "threads_updated_at"
private const val CACHE_TTL_MS = 60_000L

private val widgetJson = Json { ignoreUnknownKeys = true }

@Serializable
data class WidgetContact(
    val id: Long,
    val displayName: String
)

@Serializable
data class WidgetThread(
    val threadId: Long,
    val address: String,
    val snippet: String,
    val timestamp: Long,
    val unread: Boolean
)

object WidgetCache {
    fun hasContactsCache(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.contains(KEY_CONTACTS)
    }

    fun readContacts(context: Context): List<WidgetContact> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val raw = prefs.getString(KEY_CONTACTS, null) ?: return emptyList()
        return runCatching { widgetJson.decodeFromString<List<WidgetContact>>(raw) }
            .getOrDefault(emptyList())
    }

    fun writeContacts(context: Context, contacts: List<WidgetContact>) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val payload = widgetJson.encodeToString(contacts)
        prefs.edit()
            .putString(KEY_CONTACTS, payload)
            .putLong(KEY_CONTACTS_UPDATED_AT, System.currentTimeMillis())
            .apply()
    }

    fun hasThreadsCache(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.contains(KEY_THREADS)
    }

    fun readThreads(context: Context): List<WidgetThread> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val raw = prefs.getString(KEY_THREADS, null) ?: return emptyList()
        return runCatching { widgetJson.decodeFromString<List<WidgetThread>>(raw) }
            .getOrDefault(emptyList())
    }

    fun writeThreads(context: Context, threads: List<WidgetThread>) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val payload = widgetJson.encodeToString(threads)
        prefs.edit()
            .putString(KEY_THREADS, payload)
            .putLong(KEY_THREADS_UPDATED_AT, System.currentTimeMillis())
            .apply()
    }

    fun shouldRefreshContacts(context: Context, now: Long = System.currentTimeMillis()): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val lastUpdated = prefs.getLong(KEY_CONTACTS_UPDATED_AT, 0L)
        return now - lastUpdated > CACHE_TTL_MS
    }

    fun shouldRefreshThreads(context: Context, now: Long = System.currentTimeMillis()): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val lastUpdated = prefs.getLong(KEY_THREADS_UPDATED_AT, 0L)
        return now - lastUpdated > CACHE_TTL_MS
    }
}
