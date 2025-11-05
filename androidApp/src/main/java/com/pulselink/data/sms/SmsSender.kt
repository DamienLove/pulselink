package com.pulselink.data.sms

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.telephony.SmsManager
import android.util.Log
import androidx.annotation.RequiresPermission
import com.pulselink.domain.model.Contact
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.withTimeoutOrNull

@Singleton
class SmsSender @Inject constructor(
    @ApplicationContext private val context: Context,
    private val smsManager: SmsManager
) {

    private val pendingRequests = ConcurrentHashMap<String, CompletableDeferred<Boolean>>()

    @RequiresPermission(allOf = [Manifest.permission.SEND_SMS])
    suspend fun sendAlert(message: String, contacts: List<Contact>): Int {
        var count = 0
        for (contact in contacts) {
            if (sendSms(contact.phoneNumber, message)) {
                count++
            }
        }
        return count
    }

    @RequiresPermission(Manifest.permission.SEND_SMS)
    suspend fun sendSms(
        phoneNumber: String,
        message: String,
        timeoutMillis: Long = DEFAULT_TIMEOUT_MS
    ): Boolean {
        val requestId = UUID.randomUUID().toString()
        val deferred = CompletableDeferred<Boolean>()
        pendingRequests[requestId] = deferred

        val sentIntent = Intent(ACTION_SMS_SENT).putExtra(EXTRA_REQUEST_ID, requestId)
        val deliveredIntent = Intent(ACTION_SMS_DELIVERED).putExtra(EXTRA_REQUEST_ID, requestId)

        val sentPendingIntent = PendingIntent.getBroadcast(
            context,
            requestId.hashCode(),
            sentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val deliveredPendingIntent = PendingIntent.getBroadcast(
            context,
            requestId.hashCode(),
            deliveredIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        runCatching {
            smsManager.sendTextMessage(phoneNumber, null, message, sentPendingIntent, deliveredPendingIntent)
        }.onFailure { error ->
            Log.e(TAG, "Unable to send SMS to $phoneNumber", error)
            pendingRequests.remove(requestId)?.complete(false)
        }

        val result = withTimeoutOrNull(timeoutMillis) { deferred.await() } ?: false
        pendingRequests.remove(requestId)
        return result
    }

    fun completeSmsRequest(requestId: String, success: Boolean) {
        pendingRequests.remove(requestId)?.complete(success)
    }

    companion object {
        private const val TAG = "PulseLinkSmsSender"
        const val ACTION_SMS_SENT = "com.pulselink.SMS_SENT"
        const val ACTION_SMS_DELIVERED = "com.pulselink.SMS_DELIVERED"
        const val EXTRA_REQUEST_ID = "request_id"
        private const val DEFAULT_TIMEOUT_MS = 30_000L
    }
}
