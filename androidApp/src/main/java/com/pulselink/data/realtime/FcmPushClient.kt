package com.pulselink.data.realtime

import com.google.firebase.functions.FirebaseFunctions
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.tasks.await
import android.util.Log

@Singleton
class FcmPushClient @Inject constructor(
    private val functions: FirebaseFunctions
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
            Log.w(TAG, "FCM data message send failed", error)
        }.getOrDefault(false)
    }

    companion object {
        private const val TAG = "FcmPushClient"
    }
}
