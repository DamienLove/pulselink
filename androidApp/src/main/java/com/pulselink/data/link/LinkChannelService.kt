package com.pulselink.data.link

import android.util.Log
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.pulselink.auth.FirebaseAuthManager
import com.pulselink.domain.model.Contact
import com.pulselink.domain.model.LinkStatus
import com.pulselink.domain.repository.ContactRepository
import com.pulselink.domain.repository.SettingsRepository
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@Singleton
class LinkChannelService @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val settingsRepository: SettingsRepository,
    private val contactRepository: ContactRepository,
    private val authManager: FirebaseAuthManager
) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val started = AtomicBoolean(false)
    private val listeners = mutableMapOf<String, ListenerRegistration>()
    private val _inboundMessages = MutableSharedFlow<LinkChannelPayload>(extraBufferCapacity = 64)
    val inboundMessages: SharedFlow<LinkChannelPayload> = _inboundMessages.asSharedFlow()
    @Volatile private var localDeviceId: String? = null

    fun start() {
        if (!started.compareAndSet(false, true)) return
        scope.launch {
            authManager.ensureSignedIn()
            val deviceId = settingsRepository.ensureDeviceId()
            localDeviceId = deviceId
            contactRepository.observeContacts().collect { contacts ->
                syncListeners(deviceId, contacts)
            }
        }
    }

    suspend fun sendManualMessage(contact: Contact, body: String): Boolean {
        start()
        authManager.ensureSignedIn()
        val remoteDeviceId = contact.remoteDeviceId ?: return false
        val senderId = localDeviceId ?: settingsRepository.ensureDeviceId().also { localDeviceId = it }
        val channelId = channelIdFor(senderId, remoteDeviceId)
        val payload = hashMapOf(
            FIELD_ID to UUID.randomUUID().toString(),
            FIELD_SENDER_ID to senderId,
            FIELD_RECEIVER_ID to remoteDeviceId,
            FIELD_CONTACT_ID to contact.id,
            FIELD_BODY to body,
            FIELD_TIMESTAMP to System.currentTimeMillis(),
            FIELD_TYPE to TYPE_MANUAL
        )
        return runCatching {
            firestore.collection(COLLECTION_CHANNELS)
                .document(channelId)
                .collection(COLLECTION_MESSAGES)
                .document(payload[FIELD_ID] as String)
                .set(payload)
                .await()
        }.onFailure {
            Log.w(TAG, "Failed to send realtime message via $channelId", it)
        }.isSuccess
    }

    private fun syncListeners(localId: String, contacts: List<Contact>) {
        val desired = contacts
            .filter { it.linkStatus == LinkStatus.LINKED && !it.remoteDeviceId.isNullOrBlank() }
            .associate { contact ->
                val channelId = channelIdFor(localId, contact.remoteDeviceId!!)
                channelId to contact.id
            }

        val stale = listeners.keys - desired.keys
        stale.forEach { channelId ->
            listeners.remove(channelId)?.remove()
        }

        desired.forEach { (channelId, contactId) ->
            if (!listeners.containsKey(channelId)) {
                attachListener(channelId, contactId, localId)
            }
        }
    }

    private fun attachListener(channelId: String, contactId: Long, localId: String) {
        val registration = firestore.collection(COLLECTION_CHANNELS)
            .document(channelId)
            .collection(COLLECTION_MESSAGES)
            .whereEqualTo(FIELD_RECEIVER_ID, localId)
            .addSnapshotListener { snapshots, error ->
                if (error != null) {
                    Log.w(TAG, "Listener error for $channelId", error)
                    return@addSnapshotListener
                }
                if (snapshots == null || snapshots.isEmpty) return@addSnapshotListener
                for (change in snapshots.documentChanges) {
                    if (change.type == DocumentChange.Type.ADDED) {
                        val doc = change.document
                        val payload = LinkChannelPayload(
                            id = doc.getString(FIELD_ID).orEmpty(),
                            contactId = doc.getLong(FIELD_CONTACT_ID)?.toLong() ?: contactId,
                            senderId = doc.getString(FIELD_SENDER_ID).orEmpty(),
                            receiverId = doc.getString(FIELD_RECEIVER_ID).orEmpty(),
                            body = doc.getString(FIELD_BODY).orEmpty(),
                            timestamp = doc.getLong(FIELD_TIMESTAMP) ?: System.currentTimeMillis()
                        )
                        scope.launch {
                            _inboundMessages.emit(payload)
                            doc.reference.delete()
                        }
                    }
                }
            }
        listeners[channelId] = registration
    }

    private fun channelIdFor(a: String, b: String): String =
        listOf(a, b).sorted().joinToString("_")

    companion object {
        private const val TAG = "LinkChannelService"
        private const val COLLECTION_CHANNELS = "linkChannels"
        private const val COLLECTION_MESSAGES = "messages"
        private const val FIELD_ID = "id"
        private const val FIELD_SENDER_ID = "senderId"
        private const val FIELD_RECEIVER_ID = "receiverId"
        private const val FIELD_CONTACT_ID = "contactId"
        private const val FIELD_BODY = "body"
        private const val FIELD_TIMESTAMP = "timestamp"
        private const val FIELD_TYPE = "type"
        private const val TYPE_MANUAL = "manual"
    }
}

data class LinkChannelPayload(
    val id: String,
    val contactId: Long,
    val senderId: String,
    val receiverId: String,
    val body: String,
    val timestamp: Long
)
