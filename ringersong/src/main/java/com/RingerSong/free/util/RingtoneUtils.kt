package com.RingerSong.free.util

import android.content.ContentValues
import android.content.Context
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import com.RingerSong.free.R
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

object RingtoneUtils {

    private const val TAG = "RingtoneUtils"
    private const val SILENT_RINGTONE_TITLE = "RingerSong Silent"

    fun isSilentRingtoneSet(context: Context): Boolean {
        val currentUri = RingtoneManager.getActualDefaultRingtoneUri(context, RingtoneManager.TYPE_RINGTONE)
        if (currentUri == null) return false // No ringtone is technically silent, but might verify if it's ours

        // Check if the current ringtone matches our silent one
        // We can check the title via content resolver
        try {
            val ringtone = RingtoneManager.getRingtone(context, currentUri)
            val title = ringtone.getTitle(context)
            return title == SILENT_RINGTONE_TITLE
        } catch (e: Exception) {
            Log.e(TAG, "Error checking ringtone title", e)
        }
        return false
    }

    fun setSilentRingtone(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.System.canWrite(context)) {
                return false
            }
        }

        try {
            val uri = getOrCreateSilentRingtoneUri(context)
            if (uri != null) {
                RingtoneManager.setActualDefaultRingtoneUri(
                    context,
                    RingtoneManager.TYPE_RINGTONE,
                    uri
                )
                return true
            } else {
                return false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error setting silent ringtone", e)
            return false
        }
    }

    private fun getOrCreateSilentRingtoneUri(context: Context): Uri? {
        // 1. Check if it already exists in MediaStore
        val existingUri = findSilentRingtoneUri(context)
        if (existingUri != null) return existingUri

        // 2. Create it
        return createSilentRingtoneFile(context)
    }

    private fun findSilentRingtoneUri(context: Context): Uri? {
        val projection = arrayOf(MediaStore.Audio.Media._ID)
        val selection = "${MediaStore.Audio.Media.TITLE} = ?"
        val selectionArgs = arrayOf(SILENT_RINGTONE_TITLE)

        // On Android 10+ (Q), we should use MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        // But EXTERNAL_CONTENT_URI is usually fine for general queries
        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        } else {
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        }

        context.contentResolver.query(
            collection,
            projection,
            selection,
            selectionArgs,
            null
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                val id = cursor.getLong(0)
                return android.content.ContentUris.withAppendedId(collection, id)
            }
        }
        return null
    }

    private fun createSilentRingtoneFile(context: Context): Uri? {
        // Prepare values
        val values = ContentValues().apply {
            put(MediaStore.Audio.Media.DISPLAY_NAME, "silent.wav")
            put(MediaStore.Audio.Media.TITLE, SILENT_RINGTONE_TITLE)
            put(MediaStore.Audio.Media.MIME_TYPE, "audio/wav")
            put(MediaStore.Audio.Media.IS_RINGTONE, true)
            put(MediaStore.Audio.Media.IS_NOTIFICATION, false)
            put(MediaStore.Audio.Media.IS_ALARM, false)
            put(MediaStore.Audio.Media.IS_MUSIC, false)
        }

        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        } else {
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        }

        val itemUri = context.contentResolver.insert(collection, values) ?: return null

        try {
            context.contentResolver.openOutputStream(itemUri).use { os ->
                if (os == null) throw IOException("Failed to open output stream")
                // Copy from raw resource
                context.resources.openRawResource(R.raw.silent).use { input ->
                    input.copyTo(os)
                }
            }
            return itemUri
        } catch (e: Exception) {
            Log.e(TAG, "Error writing silent ringtone file", e)
            // Cleanup
            context.contentResolver.delete(itemUri, null, null)
            return null
        }
    }
}
