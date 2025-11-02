package com.pulselink.data.sms

import android.Manifest
import android.telephony.SmsManager
import android.util.Log
import androidx.annotation.RequiresPermission
import com.pulselink.domain.model.Contact
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SmsSender @Inject constructor(
    private val smsManager: SmsManager
) {

    @RequiresPermission(allOf = [Manifest.permission.SEND_SMS])
    fun sendAlert(message: String, contacts: List<Contact>): Int {
        var count = 0
        contacts.forEach { contact ->
            if (sendSms(contact.phoneNumber, message)) {
                count++
            }
        }
        return count
    }

    @RequiresPermission(Manifest.permission.SEND_SMS)
    fun sendSms(phoneNumber: String, message: String): Boolean {
        return runCatching {
            smsManager.sendTextMessage(phoneNumber, null, message, null, null)
        }.onFailure { error ->
            Log.e(TAG, "Unable to send SMS to $phoneNumber", error)
        }.isSuccess
    }

    companion object {
        private const val TAG = "PulseLinkSmsSender"
    }
}
