package com.pulselink.beacon.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.pulselink.beacon.data.SmsRepository
import com.pulselink.beacon.data.InboxPreferencesRepository
import kotlinx.coroutines.flow.first

class OtpCleanupWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val repo = SmsRepository(applicationContext)
        val prefs = InboxPreferencesRepository(applicationContext)

        val state = prefs.flow.first()
        if (!state.autoDeleteOtps) {
            return Result.success()
        }

        val contentResolver = applicationContext.contentResolver
        val uri = android.provider.Telephony.Sms.CONTENT_URI
        val projection = arrayOf(android.provider.Telephony.Sms._ID, android.provider.Telephony.Sms.BODY)

        val oneDayAgo = System.currentTimeMillis() - 24 * 60 * 60 * 1000

        // Optimized query: Look for messages older than 24h AND containing keywords
        // (body LIKE '%otp%' OR body LIKE '%code%' ...)
        val keywords = listOf("otp", "verification", "code", "password")
        val keywordSelection = keywords.joinToString(" OR ") { "${android.provider.Telephony.Sms.BODY} LIKE ?" }
        val selection = "${android.provider.Telephony.Sms.DATE} < ? AND ($keywordSelection)"

        val selectionArgs = mutableListOf<String>()
        selectionArgs.add(oneDayAgo.toString())
        keywords.forEach { selectionArgs.add("%$it%") }

        contentResolver.query(uri, projection, selection, selectionArgs.toTypedArray(), null)?.use { cursor ->
            val idIdx = cursor.getColumnIndex(android.provider.Telephony.Sms._ID)
            val bodyIdx = cursor.getColumnIndex(android.provider.Telephony.Sms.BODY)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idIdx)
                val body = cursor.getString(bodyIdx)

                // Double check locally to be strict (SQL LIKE is case insensitive usually but good to be sure)
                if (isOtpMessage(body)) {
                    repo.deleteMessage(id)
                }
            }
        }

        return Result.success()
    }

    private fun isOtpMessage(body: String): Boolean {
        // Strict regex check before deletion
        val regex = Regex("(?<!\\d)\\d{4,8}(?!\\d)") // 4-8 digit code
        if (!regex.containsMatchIn(body)) return false

        val keywords = listOf("otp", "verification", "code", "password", "pin", "login")
        return keywords.any { body.contains(it, ignoreCase = true) }
    }
}
