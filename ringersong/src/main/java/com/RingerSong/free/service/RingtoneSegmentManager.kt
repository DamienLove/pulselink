package com.RingerSong.free.service

import android.content.ContentValues
import android.content.Context
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaCodec
import android.media.MediaMuxer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.provider.Settings
import android.util.Log
import com.RingerSong.free.data.AppStateStore
import com.RingerSong.free.data.ProgressionEngine
import com.RingerSong.free.data.SongEntry
import com.RingerSong.free.data.SongSource
import com.RingerSong.free.data.YouTubeMusicRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.first
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RingtoneSegmentManager @Inject constructor(
    private val context: Context,
    private val appStateStore: AppStateStore
) {
    companion object {
        private const val TAG = "RingtoneSegmentManager"
        private const val RINGTONE_DIR = "Ringtones"
    }

    suspend fun setRingtoneForIncomingCall(phoneNumber: String?) = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Setting ringtone for incoming call from: $phoneNumber")

            // Check if we have WRITE_SETTINGS permission
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (!Settings.System.canWrite(context)) {
                    Log.e(TAG, "No WRITE_SETTINGS permission")
                    return@withContext
                }
            }

            // Get current state
            val state = appStateStore.stateFlow.first()

            if (!state.settings.enabled || state.songs.isEmpty()) {
                Log.w(TAG, "RingerSong disabled or no songs")
                return@withContext
            }

            // Use ProgressionEngine to get the next segment
            val (updatedState, segmentPlay) = ProgressionEngine.advance(
                state,
                contactId = null,
                callerKey = phoneNumber
            )

            if (segmentPlay == null) {
                Log.w(TAG, "No segment to play")
                return@withContext
            }

            // Save updated state
            appStateStore.update { updatedState }

            val song = segmentPlay.song
            Log.d(TAG, "Playing segment: ${song.title} from ${segmentPlay.startMs}ms for ${segmentPlay.durationMs}ms")

            // Get the actual file path
            val sourceFilePath = when (song.source) {
                SongSource.YOUTUBE_MUSIC -> {
                    val videoId = song.uri.removePrefix("youtube:video:")
                    val youtubeMusicRepo = YouTubeMusicRepository(context)
                    youtubeMusicRepo.getLocalFilePath(videoId)
                }
                SongSource.LOCAL -> {
                    if (song.uri.startsWith("content://")) {
                        // Copy content URI to temporary file first
                        copyContentUriToFile(Uri.parse(song.uri))
                    } else {
                        song.uri.removePrefix("file://")
                    }
                }
                SongSource.SPOTIFY -> {
                    Log.w(TAG, "Spotify streaming not supported for ringtones")
                    null
                }
                else -> {
                    Log.w(TAG, "Unsupported song source: ${song.source}")
                    null
                }
            }

            if (sourceFilePath == null) {
                Log.e(TAG, "Could not get source file path for ${song.source}")
                return@withContext
            }

            // Extract the segment and set as ringtone
            val segmentFile = extractAudioSegment(
                sourceFilePath,
                segmentPlay.startMs,
                segmentPlay.durationMs
            )

            if (segmentFile != null) {
                setAsSystemRingtone(segmentFile, song.title)
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error setting ringtone", e)
        }
    }

    private fun copyContentUriToFile(uri: Uri): String? {
        return try {
            val tempFile = File(context.cacheDir, "temp_${System.currentTimeMillis()}.mp3")
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(tempFile).use { output ->
                    input.copyTo(output)
                }
            }
            tempFile.absolutePath
        } catch (e: Exception) {
            Log.e(TAG, "Error copying content URI to file", e)
            null
        }
    }

    private fun extractAudioSegment(
        sourceFilePath: String,
        startMs: Long,
        durationMs: Long
    ): File? {
        return try {
            val sourceFile = File(sourceFilePath)
            if (!sourceFile.exists()) {
                Log.e(TAG, "Source file does not exist: $sourceFilePath")
                return null
            }

            // Create output file
            val ringtoneDir = File(context.getExternalFilesDir(null), RINGTONE_DIR)
            if (!ringtoneDir.exists()) ringtoneDir.mkdirs()

            val outputFile = File(ringtoneDir, "current_segment_${System.currentTimeMillis()}.mp3")

            // For now, use a simple approach: copy the whole file
            // TODO: Implement proper segment extraction with FFmpeg in future version
            FileInputStream(sourceFile).use { input ->
                FileOutputStream(outputFile).use { output ->
                    input.copyTo(output)
                }
            }
            Log.d(TAG, "Copied file for ringtone (segment extraction will be added in future update)")

            outputFile
        } catch (e: Exception) {
            Log.e(TAG, "Error extracting audio segment", e)
            null
        }
    }


    private fun setAsSystemRingtone(file: File, title: String) {
        try {
            Log.d(TAG, "Setting system ringtone: ${file.absolutePath}")

            // Add to MediaStore
            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.DATA, file.absolutePath)
                put(MediaStore.MediaColumns.TITLE, "RingerSong - $title")
                put(MediaStore.MediaColumns.DISPLAY_NAME, file.name)
                put(MediaStore.MediaColumns.MIME_TYPE, "audio/mpeg")
                put(MediaStore.MediaColumns.SIZE, file.length())
                put(MediaStore.Audio.Media.IS_RINGTONE, true)
                put(MediaStore.Audio.Media.IS_NOTIFICATION, false)
                put(MediaStore.Audio.Media.IS_ALARM, false)
                put(MediaStore.Audio.Media.IS_MUSIC, false)
            }

            val uri = MediaStore.Audio.Media.getContentUriForPath(file.absolutePath)
            if (uri == null) {
                Log.e(TAG, "Could not get content URI for path")
                return
            }

            // Delete any existing entry for this file
            context.contentResolver.delete(
                uri,
                "${MediaStore.MediaColumns.DATA}=?",
                arrayOf(file.absolutePath)
            )

            // Insert new entry
            val newUri = context.contentResolver.insert(uri, values)
            if (newUri == null) {
                Log.e(TAG, "Failed to insert into MediaStore")
                return
            }

            // Set as ringtone
            RingtoneManager.setActualDefaultRingtoneUri(
                context,
                RingtoneManager.TYPE_RINGTONE,
                newUri
            )

            Log.d(TAG, "Successfully set ringtone: $newUri")

        } catch (e: Exception) {
            Log.e(TAG, "Error setting system ringtone", e)
        }
    }

    fun cleanupOldSegments() {
        try {
            val ringtoneDir = File(context.getExternalFilesDir(null), RINGTONE_DIR)
            if (!ringtoneDir.exists()) return

            val files = ringtoneDir.listFiles() ?: return
            val cutoffTime = System.currentTimeMillis() - (24 * 60 * 60 * 1000) // 24 hours ago

            files.filter { it.lastModified() < cutoffTime }.forEach { file ->
                file.delete()
                Log.d(TAG, "Deleted old segment: ${file.name}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error cleaning up old segments", e)
        }
    }
}
