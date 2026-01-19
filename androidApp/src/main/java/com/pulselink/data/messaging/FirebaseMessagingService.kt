package com.pulselink.data.messaging

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.pulselink.data.link.LinkChannelService
import com.pulselink.data.sms.SmsRelayService
import com.pulselink.domain.repository.SettingsRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class PulseLinkFirebaseMessagingService : FirebaseMessagingService() {

    @Inject lateinit var linkChannelService: LinkChannelService
    @Inject lateinit var smsRelayService: SmsRelayService
    @Inject lateinit var settingsRepository: SettingsRepository
    @Inject lateinit var firestore: FirebaseFirestore
    @Inject lateinit var auth: FirebaseAuth

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "New FCM token received")
        updateToken(token)
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        Log.d(TAG, "Message received from: ${message.from}")

        val type = message.data["type"]
        if (type == "SMS_RELAY") {
            Log.d(TAG, "Received SMS_RELAY trigger")
            // Ensure the relay service is listening. This is idempotent.
            // If the app was dead, PulseLinkApp.onCreate() calls this too, but
            // explicit call here guarantees it for all entry points.
            smsRelayService.start()
            return
        }

        // Ensure LinkChannelService is running to process the message via Firestore listeners
        linkChannelService.start()

        val messageId = message.data["messageId"]
        val channelId = message.data["channelId"]
        if (!messageId.isNullOrBlank() && !channelId.isNullOrBlank()) {
            scope.launch {
                firestore.collection("linkChannels")
                    .document(channelId)
                    .collection("messages")
                    .document(messageId)
                    .update("status", "DELIVERED")
                    .addOnFailureListener { e ->
                        Log.w(TAG, "Failed to mark message delivered", e)
                    }
            }
        }
    }

    private fun updateToken(token: String) {
        scope.launch {
            val deviceId = settingsRepository.ensureDeviceId()

            // We store the token associated with the device ID.
            // Even if user is not signed in, we might want to store it if we track devices anonymously,
            // but usually we want to associate with user.
            // For now, we update if we have a device ID.

            val deviceData = hashMapOf(
                "fcmToken" to token,
                "updatedAt" to System.currentTimeMillis()
            )
            auth.currentUser?.uid?.let { deviceData["uid"] = it }

            firestore.collection("devices")
                .document(deviceId)
                .set(deviceData, SetOptions.merge())
                .addOnFailureListener { e ->
                    Log.w(TAG, "Failed to update FCM token", e)
                }
        }
    }

    companion object {
        private const val TAG = "PulseLinkFCM"
    }
}
