package com.pulselink.beacon.messaging

import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.pulselink.beacon.webrtc.WebRtcSignaler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class BeaconFcmService : FirebaseMessagingService() {

    private val webRtcSignaler: WebRtcSignaler by lazy { WebRtcSignaler() }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "Beacon FCM token: $token")
        // TODO: send token to paired safety app when linking is available.
    }

    override fun onMessageReceived(message: RemoteMessage) {
        val data = message.data
        if (data.isNullOrEmpty()) return
        val type = data["type"].orEmpty()
        scope.launch {
            runCatching {
                when (type) {
                    "WEBRTC_OFFER" -> webRtcSignaler.handleOffer(data)
                    "WEBRTC_ANSWER" -> webRtcSignaler.handleAnswer(data)
                    "WEBRTC_ICE" -> webRtcSignaler.handleIce(data)
                    else -> Log.d(TAG, "Unhandled FCM type=$type")
                }
            }.onFailure { error ->
                Log.e(TAG, "Failed to handle Beacon FCM type=$type", error)
            }
        }
    }

    companion object {
        private const val TAG = "BeaconFcmService"
    }
}
