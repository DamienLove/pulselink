package com.pulselink.beacon.sms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.util.Log

class BeaconSmsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return
        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        val mergedBody = messages.joinToString(separator = "") { it.messageBody }
        val body = mergedBody.trim()
        val origin = messages.firstOrNull()?.originatingAddress.orEmpty()
        if (body.isBlank()) return
        val parsed = SmsCodec.parse(body)
        if (parsed != null) {
            Log.i(TAG, "Beacon received PulseLink message from $origin tier=${(parsed as? PulseLinkMessage.ManualMessage)?.tier ?: EscalationTier.EMERGENCY}")
            // TODO: bridge to safety app (via FCM or local alert) when linking is available.
        }
    }

    companion object {
        private const val TAG = "BeaconSmsReceiver"
    }
}
