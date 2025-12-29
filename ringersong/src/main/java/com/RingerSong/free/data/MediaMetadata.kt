package com.RingerSong.free.data

import android.content.ContentResolver
import android.content.Context
import android.database.Cursor
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.OpenableColumns

data class SongMetadata(
    val title: String,
    val durationMs: Long?
)

fun resolveSongMetadata(context: Context, uri: Uri): SongMetadata {
    val resolver = context.contentResolver
    val displayName = queryDisplayName(resolver, uri)
    val retriever = MediaMetadataRetriever()
    val duration = runCatching {
        retriever.setDataSource(context, uri)
        retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull()
    }.getOrNull()
    runCatching { retriever.release() }
    val title = displayName ?: uri.lastPathSegment ?: "Untitled"
    return SongMetadata(title = title, durationMs = duration)
}

private fun queryDisplayName(resolver: ContentResolver, uri: Uri): String? {
    val projection = arrayOf(OpenableColumns.DISPLAY_NAME)
    var cursor: Cursor? = null
    return try {
        cursor = resolver.query(uri, projection, null, null, null)
        if (cursor != null && cursor.moveToFirst()) {
            cursor.getString(cursor.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME))
        } else {
            null
        }
    } catch (_: Exception) {
        null
    } finally {
        cursor?.close()
    }
}
