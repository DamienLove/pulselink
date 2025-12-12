package com.pulselink.beacon.webrtc

import android.util.Log
import com.pulselink.beacon.messaging.FcmPushClient

class WebRtcSignaler(
    private val fcmPushClient: FcmPushClient = FcmPushClient()
) {
    suspend fun handleOffer(data: Map<String, String>) {
        Log.d(TAG, "Received WEBRTC_OFFER (stub) $data")
        // TODO: hook into Beacon P2P once pairing is defined.
    }

    suspend fun handleAnswer(data: Map<String, String>) {
        Log.d(TAG, "Received WEBRTC_ANSWER (stub) $data")
    }

    suspend fun handleIce(data: Map<String, String>) {
        Log.d(TAG, "Received WEBRTC_ICE (stub) $data")
    }

    suspend fun send(toToken: String, type: String, extras: Map<String, String>) {
        val payload = buildMap {
            put("type", type)
            putAll(extras)
        }
        fcmPushClient.send(toToken, payload)
    }

    companion object {
        private const val TAG = "BeaconWebRtcSignaler"
    }
}
