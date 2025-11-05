package com.pulselink.receiver

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.SmsManager
import android.util.Log
import com.pulselink.data.sms.SmsSender
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class SmsSendReceiver : BroadcastReceiver() {

    @Inject lateinit var smsSender: SmsSender

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        val requestId = intent.getStringExtra(SmsSender.EXTRA_REQUEST_ID) ?: return
        val success = resultCode == Activity.RESULT_OK

        if (!success) {
            val reason = when (resultCode) {
                SmsManager.RESULT_ERROR_GENERIC_FAILURE -> "Generic failure"
                SmsManager.RESULT_ERROR_NO_SERVICE -> "No service"
                SmsManager.RESULT_ERROR_NULL_PDU -> "Null PDU"
                SmsManager.RESULT_ERROR_RADIO_OFF -> "Radio off"
                else -> "Unknown error code $resultCode"
            }
            Log.w(TAG, "SMS action $action failed ($reason) for request $requestId")
        } else {
            Log.d(TAG, "SMS action $action succeeded for request $requestId")
        }

        if (action == SmsSender.ACTION_SMS_SENT) {
            smsSender.completeSmsRequest(requestId, success)
        } else if (action == SmsSender.ACTION_SMS_DELIVERED && success) {
            smsSender.completeSmsRequest(requestId, true)
        }
    }

    companion object {
        private const val TAG = "SmsSendReceiver"
    }
}
