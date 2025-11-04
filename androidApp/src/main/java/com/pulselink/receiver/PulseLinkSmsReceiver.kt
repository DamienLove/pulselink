package com.pulselink.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.pulselink.data.link.ContactLinkManager
import com.pulselink.data.sms.SmsCodec
import com.pulselink.service.AlertRouter

@AndroidEntryPoint
class PulseLinkSmsReceiver : BroadcastReceiver() {

    @Inject lateinit var alertRouter: AlertRouter
    @Inject lateinit var contactLinkManager: ContactLinkManager

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return
        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        val mergedBody = messages.joinToString(separator = "") { it.messageBody }
        val body = mergedBody.trim()
        if (body.isBlank()) return
        val origin = messages.firstOrNull()?.originatingAddress.orEmpty()
        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.Default).launch {
            try {
                val parsed = SmsCodec.parse(body)
                if (parsed != null) {
                    contactLinkManager.handleInbound(parsed, origin)
                } else {
                    alertRouter.onInboundMessage(body)
                }
            } catch (error: Exception) {
                android.util.Log.e(TAG, "Failed to handle inbound SMS from $origin", error)
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        private const val TAG = "PulseLinkSmsReceiver"
    }
}
