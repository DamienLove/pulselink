package com.pulselink.data.sms

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.pulselink.domain.repository.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton
import java.util.concurrent.atomic.AtomicBoolean

@Singleton
class SmsRelayService @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val smsSender: SmsSender,
    private val settingsRepository: SettingsRepository,
    private val smsSyncTrigger: SmsSyncTrigger
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var outboxListener: ListenerRegistration? = null
    private var userListener: ListenerRegistration? = null
    private val isStarted = AtomicBoolean(false)
    private var lastSyncRequestedAt: com.google.firebase.Timestamp? = null
    private var isFirstSnapshot = true

    fun start() {
        if (!isStarted.compareAndSet(false, true)) return

        auth.addAuthStateListener { firebaseAuth ->
            val user = firebaseAuth.currentUser
            if (user != null) {
                startListening(user.uid)
            } else {
                stopListening()
            }
        }
    }

    private fun startListening(uid: String) {
        if (outboxListener == null) {
            val outboxRef = firestore.collection("users").document(uid).collection("outbox")
            outboxListener = outboxRef.addSnapshotListener { snapshots, e ->
                if (e != null) {
                    Log.w(TAG, "Outbox listen failed.", e)
                    return@addSnapshotListener
                }

                if (snapshots != null) {
                    for (doc in snapshots.documents) {
                        val data = doc.data ?: continue
                        val address = data["address"] as? String
                        val body = data["body"] as? String
                        val lineId = data["lineId"] as? String

                        if (address != null && body != null) {
                            processMessage(doc.id, address, body, uid, lineId)
                        }
                    }
                }
            }
        }

        if (userListener == null) {
            isFirstSnapshot = true
            val userRef = firestore.collection("users").document(uid)
            userListener = userRef.addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.w(TAG, "User listen failed.", e)
                    return@addSnapshotListener
                }

                if (snapshot != null && snapshot.exists()) {
                    val syncRequestedAt = snapshot.getTimestamp("syncRequestedAt")

                    if (isFirstSnapshot) {
                        isFirstSnapshot = false
                        lastSyncRequestedAt = syncRequestedAt
                    } else if (syncRequestedAt != null && syncRequestedAt != lastSyncRequestedAt) {
                        // Timestamp changed, trigger sync
                        Log.d(TAG, "Sync requested from web at $syncRequestedAt")
                        lastSyncRequestedAt = syncRequestedAt
                        smsSyncTrigger.triggerSync()
                    }
                }
            }
        }
    }

    private fun stopListening() {
        outboxListener?.remove()
        outboxListener = null
        userListener?.remove()
        userListener = null
        lastSyncRequestedAt = null
        isFirstSnapshot = true
    }

    private fun processMessage(docId: String, address: String, body: String, uid: String, lineId: String?) {
        scope.launch {
            try {
                val settings = settingsRepository.settings.first()
                if (!settings.remoteWebAccessEnabled) {
                    return@launch
                }

                val deviceId = settingsRepository.ensureDeviceId()
                if (!lineId.isNullOrBlank() && lineId != deviceId) {
                    return@launch
                }
                // Warning: This assumes SEND_SMS permission is granted.
                // In a real app, we should check ContextCompat.checkSelfPermission
                // However, since this runs in the context of the app which (usually) has permission if it's the default SMS app
                // or requested it, we attempt it. SmsSender handles errors gracefully.
                val success = smsSender.sendSms(address, body)
                if (success) {
                    firestore.collection("users").document(uid)
                        .collection("outbox").document(docId).delete()
                } else {
                    Log.w(TAG, "Failed to send SMS via relay to $address")
                    // Mark as failed and delete from outbox to prevent infinite loops.
                    // Ideally, we would update status to 'failed', but for now, we remove it
                    // to keep the queue clear.
                    firestore.collection("users").document(uid)
                        .collection("outbox").document(docId).delete()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception during SMS relay", e)
            }
        }
    }

    companion object {
        private const val TAG = "SmsRelayService"
    }
}
