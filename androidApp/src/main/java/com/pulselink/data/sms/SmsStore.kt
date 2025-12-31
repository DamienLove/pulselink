package com.pulselink.data.sms

import android.content.ContentValues
import android.content.ContentUris
import android.content.Context
import android.provider.Telephony
import android.util.Log
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import com.pulselink.BuildConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Persists SMS rows into the system Telephony provider. This must only be used
 * while the app holds the default SMS role; otherwise the inserts will fail.
 */
@Singleton
class SmsStore @Inject constructor(
    @ApplicationContext private val context: Context
){
    fun insertIncoming(address: String, body: String, timestamp: Long = System.currentTimeMillis()) {
        if (SmsCodec.isPulseLinkPayload(body)) return
        val values = ContentValues().apply {
            put(Telephony.TextBasedSmsColumns.ADDRESS, address)
            put(Telephony.TextBasedSmsColumns.BODY, body)
            put(Telephony.TextBasedSmsColumns.DATE, timestamp)
            put(Telephony.TextBasedSmsColumns.DATE_SENT, timestamp)
            put(Telephony.TextBasedSmsColumns.READ, 0)
            put(Telephony.TextBasedSmsColumns.SEEN, 0)
            put(Telephony.TextBasedSmsColumns.TYPE, Telephony.TextBasedSmsColumns.MESSAGE_TYPE_INBOX)
            runCatching {
                Telephony.Threads.getOrCreateThreadId(context, setOf(address))
            }.getOrNull()?.let { threadId ->
                put(Telephony.TextBasedSmsColumns.THREAD_ID, threadId)
            }
        }
        runCatching {
            context.contentResolver.insert(Telephony.Sms.Inbox.CONTENT_URI, values)
        }.onSuccess {
            scheduleSync()
        }.onFailure { error ->
            Log.w(TAG, "Failed to insert incoming SMS into Telephony provider", error)
        }
    }

    fun insertOutgoing(address: String, body: String, timestamp: Long = System.currentTimeMillis()) {
        if (SmsCodec.isPulseLinkPayload(body)) return
        val values = ContentValues().apply {
            put(Telephony.TextBasedSmsColumns.ADDRESS, address)
            put(Telephony.TextBasedSmsColumns.BODY, body)
            put(Telephony.TextBasedSmsColumns.DATE, timestamp)
            put(Telephony.TextBasedSmsColumns.DATE_SENT, timestamp)
            put(Telephony.TextBasedSmsColumns.READ, 1)
            put(Telephony.TextBasedSmsColumns.SEEN, 1)
            put(Telephony.TextBasedSmsColumns.TYPE, Telephony.TextBasedSmsColumns.MESSAGE_TYPE_SENT)
            put(Telephony.TextBasedSmsColumns.STATUS, Telephony.TextBasedSmsColumns.STATUS_COMPLETE)
            runCatching {
                Telephony.Threads.getOrCreateThreadId(context, setOf(address))
            }.getOrNull()?.let { threadId ->
                put(Telephony.TextBasedSmsColumns.THREAD_ID, threadId)
            }
        }
        runCatching {
            context.contentResolver.insert(Telephony.Sms.Sent.CONTENT_URI, values)
        }.onSuccess {
            scheduleSync()
        }.onFailure { error ->
            Log.w(TAG, "Failed to insert outgoing SMS into Telephony provider", error)
        }
    }

    fun insertOutgoingPending(
        address: String,
        body: String,
        timestamp: Long = System.currentTimeMillis()
    ): Long? {
        if (SmsCodec.isPulseLinkPayload(body)) return null
        val values = ContentValues().apply {
            put(Telephony.TextBasedSmsColumns.ADDRESS, address)
            put(Telephony.TextBasedSmsColumns.BODY, body)
            put(Telephony.TextBasedSmsColumns.DATE, timestamp)
            put(Telephony.TextBasedSmsColumns.DATE_SENT, timestamp)
            put(Telephony.TextBasedSmsColumns.READ, 1)
            put(Telephony.TextBasedSmsColumns.SEEN, 1)
            put(Telephony.TextBasedSmsColumns.TYPE, Telephony.TextBasedSmsColumns.MESSAGE_TYPE_OUTBOX)
            put(Telephony.TextBasedSmsColumns.STATUS, Telephony.TextBasedSmsColumns.STATUS_PENDING)
            runCatching {
                Telephony.Threads.getOrCreateThreadId(context, setOf(address))
            }.getOrNull()?.let { threadId ->
                put(Telephony.TextBasedSmsColumns.THREAD_ID, threadId)
            }
        }
        return runCatching {
            context.contentResolver.insert(Telephony.Sms.Outbox.CONTENT_URI, values)
        }.onSuccess {
            scheduleSync()
        }.onFailure { error ->
            Log.w(TAG, "Failed to insert pending SMS into Telephony provider", error)
        }.getOrNull()?.lastPathSegment?.toLongOrNull()
    }

    fun markOutgoingSent(messageId: Long) {
        updateOutgoing(
            messageId = messageId,
            type = Telephony.TextBasedSmsColumns.MESSAGE_TYPE_SENT,
            status = Telephony.TextBasedSmsColumns.STATUS_PENDING
        )
    }

    fun markOutgoingDelivered(messageId: Long) {
        updateOutgoing(
            messageId = messageId,
            type = null,
            status = Telephony.TextBasedSmsColumns.STATUS_COMPLETE
        )
    }

    private fun updateOutgoing(messageId: Long, type: Int?, status: Int?) {
        val values = ContentValues().apply {
            if (type != null) {
                put(Telephony.TextBasedSmsColumns.TYPE, type)
            }
            if (status != null) {
                put(Telephony.TextBasedSmsColumns.STATUS, status)
            }
        }
        if (values.size() == 0) return
        runCatching {
            val uri = ContentUris.withAppendedId(Telephony.Sms.CONTENT_URI, messageId)
            context.contentResolver.update(uri, values, null, null)
        }.onFailure { error ->
            Log.w(TAG, "Failed to update SMS status for $messageId", error)
        }
    }

    private fun scheduleSync() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val request = OneTimeWorkRequest.Builder(SmsSyncWorker::class.java)
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            "SmsSyncImmediate",
            ExistingWorkPolicy.APPEND_OR_REPLACE,
            request
        )
    }

    companion object {
        private const val TAG = "SmsStore"
    }
}
