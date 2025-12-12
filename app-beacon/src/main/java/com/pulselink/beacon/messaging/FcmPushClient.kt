package com.pulselink.beacon.messaging

import android.util.Log
import com.google.firebase.functions.FirebaseFunctions
import kotlinx.coroutines.tasks.await

class FcmPushClient(
    private val functions: FirebaseFunctions = FirebaseFunctions.getInstance()
) {
    suspend fun send(token: String, data: Map<String, String>): Boolean {
        if (token.isBlank()) return false
        val payload = hashMapOf<String, Any>(
            "token" to token,
            "data" to data
        )
        return runCatching {
            functions
                .getHttpsCallable("sendDataMessage")
                .call(payload)
                .await()
            true
        }.onFailure { error ->
            Log.w(TAG, "Beacon FCM send failed", error)
        }.getOrDefault(false)
    }

    companion object {
        private const val TAG = "BeaconFcmPushClient"
    }
}
