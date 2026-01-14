package com.pulselink.beacon.receiver

import android.content.BroadcastReceiver
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Telephony
import android.telephony.SmsMessage
import com.pulselink.beacon.data.MessageNotificationPreferences
import com.pulselink.beacon.data.SmsSyncManager
import com.pulselink.beacon.notifications.MessageNotificationManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Receives inbound SMS when Beacon is set as the default SMS app.
 * Persists the message into the system Telephony provider so it
 * becomes visible to the inbox/thread UI.
 */
class SmsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_DELIVER_ACTION &&
            intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION
        ) return

        val msgs = Telephony.Sms.Intents.getMessagesFromIntent(intent) ?: return
        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.Default).launch {
            try {
                msgs.forEach { sms ->
                    writeToInbox(context, sms)
                }
                val origin = msgs.firstOrNull()?.displayOriginatingAddress.orEmpty()
                val body = msgs.joinToString(separator = "") { it.displayMessageBody }.trim()
                if (origin.isNotBlank() && body.isNotBlank()) {
                    val timestamp = msgs.maxOfOrNull { it.timestampMillis } ?: System.currentTimeMillis()

                    runCatching {
                        SmsSyncManager.getInstance(context).syncIncoming(origin, body, timestamp)
                    }

                    val settings = MessageNotificationPreferences(context).getConfig()
                    val threadId = runCatching {
                        Telephony.Threads.getOrCreateThreadId(context, origin)
                    }.getOrNull()
                    MessageNotificationManager.notifyIncoming(
                        context = context,
                        address = origin,
                        body = body,
                        timestamp = timestamp,
                        settings = settings,
                        threadId = threadId
                    )
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun writeToInbox(context: Context, sms: SmsMessage) {
        val values = ContentValues().apply {
            put(Telephony.Sms.ADDRESS, sms.displayOriginatingAddress)
            put(Telephony.Sms.BODY, sms.displayMessageBody)
            put(Telephony.Sms.DATE, sms.timestampMillis)
            put(Telephony.Sms.READ, 0)
            put(Telephony.Sms.SEEN, 0)
            put(Telephony.Sms.TYPE, Telephony.Sms.MESSAGE_TYPE_INBOX)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                val threadId = Telephony.Threads.getOrCreateThreadId(
                    context,
                    sms.displayOriginatingAddress
                )
                put(Telephony.Sms.THREAD_ID, threadId)
            }
        }
        context.contentResolver.insert(Telephony.Sms.Inbox.CONTENT_URI, values)
    }
}
