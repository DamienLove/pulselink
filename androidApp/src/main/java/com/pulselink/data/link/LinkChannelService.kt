package com.pulselink.data.link

import android.util.Log
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.pulselink.auth.FirebaseAuthManager
import com.pulselink.domain.model.Contact
import com.pulselink.domain.model.EscalationTier
import com.pulselink.domain.model.LinkStatus
import com.pulselink.domain.repository.ContactRepository
import com.pulselink.domain.repository.SettingsRepository
import com.pulselink.data.sms.PulseLinkMessage
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.cancelChildren

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
    private val linkCodeListeners = mutableMapOf<String, ListenerRegistration>()
    private val globalListeners = mutableMapOf<String, ListenerRegistration>()
    @Volatile private var realtimeEnabled: Boolean = false
    private val _inboundMessages = MutableSharedFlow<LinkChannelPayload>(extraBufferCapacity = 64)
    val inboundMessages: SharedFlow<LinkChannelPayload> = _inboundMessages.asSharedFlow()
    @Volatile private var localDeviceId: String? = null

    suspend fun sendSignal(
        contact: Contact,
        type: String,
        body: String? = null,
        linkCode: String? = null,
        urgency: com.pulselink.domain.model.MessageUrgency? = null,
        volumeHint: com.pulselink.domain.model.VolumeHint? = null,
        tier: EscalationTier? = null,
        reason: PulseLinkMessage.AlertPrepareReason? = null,
        ready: Boolean? = null,
        senderName: String? = null,
        senderEmail: String? = null
    ): Boolean {
        if (!realtimeEnabled) return false
        start()
        authManager.ensureSignedIn()
        val senderId = localDeviceId ?: settingsRepository.ensureDeviceId().also { localDeviceId = it }
        val (_, remoteDeviceId) = ensureRemoteDeviceId(contact, linkCode, senderId)
        val targetId = remoteDeviceId ?: linkCode ?: contact.primaryEmail().orEmpty()
        if (targetId.isBlank()) return false
        val channelId = channelIdFor(senderId, targetId)
        Log.d(TAG, "sendSignal type=$type channel=$channelId receiverId=$targetId linkCode=$linkCode")
        val payload = hashMapOf(
            FIELD_ID to UUID.randomUUID().toString(),
            FIELD_SENDER_ID to senderId,
            FIELD_RECEIVER_ID to targetId,
            FIELD_TIMESTAMP to System.currentTimeMillis(),
            FIELD_TYPE to type
        )
        body?.let { payload[FIELD_BODY] = it }
        linkCode?.let { payload[FIELD_LINK_CODE] = it }
        payload[FIELD_PHONE] = contact.phoneNumber.orEmpty()
        urgency?.let { payload[FIELD_URGENCY] = it.name }
        volumeHint?.let { payload[FIELD_VOLUME] = it.name }
        tier?.let { payload[FIELD_TIER] = it.name }
        reason?.let { payload[FIELD_REASON] = it.name }
        ready?.let { payload[FIELD_READY] = it }
        senderName?.let { payload[FIELD_SENDER_NAME] = it }
        senderEmail?.let { payload[FIELD_SENDER_EMAIL] = it }

        return retry {
            firestore.collection(COLLECTION_CHANNELS)
                .document(channelId)
                .collection(COLLECTION_MESSAGES)
                .document(payload[FIELD_ID] as String)
                .set(payload)
                .await()
        }.onFailure {
            Log.w(TAG, "Failed to send realtime signal $type via $channelId", it)
        }.isSuccess
    }

    private suspend fun <T> retry(
        times: Int = 3,
        initialDelay: Long = 500,
        factor: Double = 2.0,
        block: suspend () -> T
    ): Result<T> {
        var currentDelay = initialDelay
        repeat(times - 1) {
            runCatching { return Result.success(block()) }
            delay(currentDelay)
            currentDelay = (currentDelay * factor).toLong()
        }
        return runCatching { block() }
    }

    fun start() {
        if (!realtimeEnabled) return
        if (!started.compareAndSet(false, true)) return
        scope.launch {
            authManager.ensureSignedIn()
            val deviceId = settingsRepository.ensureDeviceId()
            localDeviceId = deviceId

            // Listen for messages addressed to deviceId
            attachGlobalListener(deviceId)

            // Listen for messages addressed to email
            authManager.currentUser()?.email?.takeIf { it.isNotBlank() }?.let { email ->
                attachGlobalListener(email.trim().lowercase())
            }

            // Listen for messages addressed to phone
            authManager.currentUser()?.phoneNumber?.takeIf { it.isNotBlank() }?.let { phone ->
                attachGlobalListener(phone)
            }

            contactRepository.observeContacts().collect { contacts ->
                syncListeners(deviceId, contacts)
            }
        }
    }

    fun setRealtimeEnabled(enabled: Boolean) {
        if (enabled == realtimeEnabled) return
        realtimeEnabled = enabled
        if (enabled) {
            start()
        } else {
            stopListeners()
        }
    }

    private fun stopListeners() {
        listeners.values.forEach { it.remove() }
        listeners.clear()
        linkCodeListeners.values.forEach { it.remove() }
        linkCodeListeners.clear()
        globalListeners.values.forEach { it.remove() }
        globalListeners.clear()
        scope.coroutineContext.cancelChildren()
        started.set(false)
    }

    suspend fun sendManualMessage(
        contact: Contact,
        body: String,
        urgency: com.pulselink.domain.model.MessageUrgency,
        volumeHint: com.pulselink.domain.model.VolumeHint?
    ): Boolean {
        if (!realtimeEnabled) return false
        start()
        authManager.ensureSignedIn()
        val senderId = localDeviceId ?: settingsRepository.ensureDeviceId().also { localDeviceId = it }
        val (_, remoteDeviceId) = ensureRemoteDeviceId(contact, contact.linkCode, senderId)
        val targetId = remoteDeviceId ?: contact.linkCode ?: contact.primaryEmail().orEmpty()
        if (targetId.isBlank()) return false
        val channelId = channelIdFor(senderId, targetId)
        Log.d(TAG, "sendManualMessage channel=$channelId receiverId=$targetId linkCode=${contact.linkCode}")
            val payload = hashMapOf(
                FIELD_ID to UUID.randomUUID().toString(),
                FIELD_SENDER_ID to senderId,
                FIELD_RECEIVER_ID to targetId,
                FIELD_BODY to body,
                FIELD_TIMESTAMP to System.currentTimeMillis(),
                FIELD_TYPE to TYPE_MANUAL_MESSAGE,
                FIELD_URGENCY to urgency.name,
                FIELD_VOLUME to (volumeHint?.name ?: "")
            )
            contact.linkCode?.let { payload[FIELD_LINK_CODE] = it }
            payload[FIELD_PHONE] = contact.phoneNumber.orEmpty()
        return retry {
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

    private suspend fun ensureRemoteDeviceId(
        contact: Contact,
        linkCode: String?,
        localId: String
    ): Pair<Contact, String?> {
        val existing = contact.remoteDeviceId?.takeIf { it.isNotBlank() }
        if (existing != null) return contact to existing
        val code = linkCode ?: contact.linkCode ?: return contact to null
        val byLink = resolveRemoteDeviceIdFromLink(code, localId)
        val byEmail = contact.primaryEmail()?.let { resolveRemoteDeviceIdByEmail(it, localId) }
        val candidate = byLink ?: byEmail
        if (candidate != null) {
            val updated = contact.copy(remoteDeviceId = candidate)
            contactRepository.upsert(updated)
            return updated to candidate
        }
        return contact to null
    }

    private suspend fun resolveRemoteDeviceIdFromLink(code: String, localId: String): String? = runCatching {
        val snapshot = firestore.collection(COLLECTION_LINKS).document(code).get().await()
        val deviceIds = snapshot.get("deviceIds") as? Map<*, *>
        deviceIds?.values
            ?.mapNotNull { it as? String }
            ?.firstOrNull { it != localId }
    }.getOrNull()

    private suspend fun resolveRemoteDeviceIdByEmail(email: String, localId: String): String? = runCatching {
        val normalized = email.trim().lowercase()
        val query = firestore.collection("users")
            .whereEqualTo("emailLowercase", normalized)
            .limit(1)
            .get()
            .await()
        val doc = query.documents.firstOrNull() ?: return@runCatching null
        val deviceId = doc.getString("deviceId")
        deviceId?.takeIf { it != localId }
    }.getOrNull()

    private fun Contact.primaryEmail(): String? =
        (listOfNotNull(email) + additionalEmails).firstOrNull { !it.isNullOrBlank() }

    private fun syncListeners(localId: String, contacts: List<Contact>) {
        val desired = contacts
            // Listen for any contact where we know the remote device id so we can
            // receive link requests/accepts while the link is still pending.
            .filter { !it.remoteDeviceId.isNullOrBlank() && it.linkStatus != LinkStatus.NONE }
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

        val linkCodes = contacts.mapNotNull { it.linkCode?.takeIf { code -> code.isNotBlank() } }.toSet()
        val staleCodes = linkCodeListeners.keys - linkCodes
        staleCodes.forEach { code ->
            linkCodeListeners.remove(code)?.remove()
        }
        linkCodes.forEach { code ->
            if (!linkCodeListeners.containsKey(code)) {
                attachLinkCodeListener(code)
            }
        }
    }

    private fun attachGlobalListener(identity: String) {
        if (globalListeners.containsKey(identity)) return
        val registration = firestore.collectionGroup(COLLECTION_MESSAGES)
            .whereEqualTo(FIELD_RECEIVER_ID, identity)
            .addSnapshotListener { snapshots, error ->
                if (error != null) {
                    Log.w(TAG, "Global listener error for identity=$identity", error)
                    return@addSnapshotListener
                }
                if (snapshots == null || snapshots.isEmpty) return@addSnapshotListener
                for (change in snapshots.documentChanges) {
                    if (change.type == DocumentChange.Type.ADDED) {
                        val doc = change.document
                        Log.d(TAG, "Global listener hit identity=$identity doc=${doc.id} type=${doc.getString(FIELD_TYPE)}")
                        val payload = LinkChannelPayload(
                            id = doc.getString(FIELD_ID).orEmpty(),
                            senderId = doc.getString(FIELD_SENDER_ID).orEmpty(),
                            receiverId = doc.getString(FIELD_RECEIVER_ID).orEmpty(),
                            body = doc.getString(FIELD_BODY).orEmpty(),
                            timestamp = doc.getLong(FIELD_TIMESTAMP) ?: System.currentTimeMillis(),
                            linkCode = doc.getString(FIELD_LINK_CODE),
                            phoneNumber = doc.getString(FIELD_PHONE)?.takeIf { it.isNotBlank() },
                            urgency = doc.getString(FIELD_URGENCY)
                                ?.let { runCatching { com.pulselink.domain.model.MessageUrgency.valueOf(it) }.getOrNull() }
                                ?: com.pulselink.domain.model.MessageUrgency.STANDARD,
                            volumeHint = doc.getString(FIELD_VOLUME)
                                ?.takeIf { it.isNotBlank() }
                                ?.let { runCatching { com.pulselink.domain.model.VolumeHint.valueOf(it) }.getOrNull() },
                            type = doc.getString(FIELD_TYPE) ?: TYPE_MANUAL_MESSAGE,
                            tier = doc.getString(FIELD_TIER)
                                ?.let { runCatching { EscalationTier.valueOf(it) }.getOrNull() },
                            reason = doc.getString(FIELD_REASON)
                                ?.let { runCatching { PulseLinkMessage.AlertPrepareReason.valueOf(it) }.getOrNull() },
                            ready = doc.getBoolean(FIELD_READY),
                            senderName = doc.getString(FIELD_SENDER_NAME),
                            senderEmail = doc.getString(FIELD_SENDER_EMAIL)
                        )
                        scope.launch {
                            _inboundMessages.emit(payload)
                            doc.reference.delete()
                        }
                    }
                }
            }
        globalListeners[identity] = registration
    }

    private fun attachLinkCodeListener(linkCode: String) {
        val registration = firestore.collectionGroup(COLLECTION_MESSAGES)
            .whereEqualTo(FIELD_RECEIVER_ID, linkCode)
            .addSnapshotListener { snapshots, error ->
                if (error != null) {
                    Log.w(TAG, "LinkCode listener error for $linkCode", error)
                    return@addSnapshotListener
                }
                if (snapshots == null || snapshots.isEmpty) return@addSnapshotListener
                for (change in snapshots.documentChanges) {
                    if (change.type == DocumentChange.Type.ADDED) {
                        val doc = change.document
                        Log.d(TAG, "LinkCode listener hit code=$linkCode doc=${doc.id} type=${doc.getString(FIELD_TYPE)}")
                        val payload = LinkChannelPayload(
                            id = doc.getString(FIELD_ID).orEmpty(),
                            senderId = doc.getString(FIELD_SENDER_ID).orEmpty(),
                            receiverId = doc.getString(FIELD_RECEIVER_ID).orEmpty(),
                            body = doc.getString(FIELD_BODY).orEmpty(),
                            timestamp = doc.getLong(FIELD_TIMESTAMP) ?: System.currentTimeMillis(),
                            linkCode = doc.getString(FIELD_LINK_CODE),
                            phoneNumber = doc.getString(FIELD_PHONE)?.takeIf { it.isNotBlank() },
                            urgency = doc.getString(FIELD_URGENCY)
                                ?.let { runCatching { com.pulselink.domain.model.MessageUrgency.valueOf(it) }.getOrNull() }
                                ?: com.pulselink.domain.model.MessageUrgency.STANDARD,
                            volumeHint = doc.getString(FIELD_VOLUME)
                                ?.takeIf { it.isNotBlank() }
                                ?.let { runCatching { com.pulselink.domain.model.VolumeHint.valueOf(it) }.getOrNull() },
                            type = doc.getString(FIELD_TYPE) ?: TYPE_MANUAL_MESSAGE,
                            tier = doc.getString(FIELD_TIER)
                                ?.let { runCatching { EscalationTier.valueOf(it) }.getOrNull() },
                            reason = doc.getString(FIELD_REASON)
                                ?.let { runCatching { PulseLinkMessage.AlertPrepareReason.valueOf(it) }.getOrNull() },
                            ready = doc.getBoolean(FIELD_READY),
                            senderName = doc.getString(FIELD_SENDER_NAME),
                            senderEmail = doc.getString(FIELD_SENDER_EMAIL)
                        )
                        scope.launch {
                            _inboundMessages.emit(payload)
                            doc.reference.delete()
                        }
                    }
                }
            }
        linkCodeListeners[linkCode] = registration
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
                            senderId = doc.getString(FIELD_SENDER_ID).orEmpty(),
                            receiverId = doc.getString(FIELD_RECEIVER_ID).orEmpty(),
                            body = doc.getString(FIELD_BODY).orEmpty(),
                            timestamp = doc.getLong(FIELD_TIMESTAMP) ?: System.currentTimeMillis(),
                            linkCode = doc.getString(FIELD_LINK_CODE),
                            phoneNumber = doc.getString(FIELD_PHONE)?.takeIf { it.isNotBlank() },
                            urgency = doc.getString(FIELD_URGENCY)
                                ?.let { runCatching { com.pulselink.domain.model.MessageUrgency.valueOf(it) }.getOrNull() }
                                ?: com.pulselink.domain.model.MessageUrgency.STANDARD,
                            volumeHint = doc.getString(FIELD_VOLUME)
                                ?.takeIf { it.isNotBlank() }
                                ?.let { runCatching { com.pulselink.domain.model.VolumeHint.valueOf(it) }.getOrNull() },
                            type = doc.getString(FIELD_TYPE) ?: TYPE_MANUAL_MESSAGE,
                            tier = doc.getString(FIELD_TIER)
                                ?.let { runCatching { EscalationTier.valueOf(it) }.getOrNull() },
                            reason = doc.getString(FIELD_REASON)
                                ?.let { runCatching { PulseLinkMessage.AlertPrepareReason.valueOf(it) }.getOrNull() },
                            ready = doc.getBoolean(FIELD_READY),
                            senderName = doc.getString(FIELD_SENDER_NAME),
                            senderEmail = doc.getString(FIELD_SENDER_EMAIL)
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
        private const val COLLECTION_LINKS = "links"
        private const val FIELD_ID = "id"
        private const val FIELD_SENDER_ID = "senderId"
        private const val FIELD_RECEIVER_ID = "receiverId"
        private const val FIELD_BODY = "body"
        private const val FIELD_TIMESTAMP = "timestamp"
        private const val FIELD_TYPE = "type"
        private const val FIELD_LINK_CODE = "linkCode"
        private const val FIELD_PHONE = "phoneNumber"
        private const val FIELD_URGENCY = "urgency"
        private const val FIELD_VOLUME = "volumeHint"
        private const val FIELD_TIER = "tier"
        private const val FIELD_REASON = "reason"
        private const val FIELD_READY = "ready"
        private const val FIELD_SENDER_NAME = "senderName"
        private const val FIELD_SENDER_EMAIL = "senderEmail"
        private const val TYPE_MANUAL = "manual"
        const val TYPE_ALERT_PREPARE = "alert_prepare"
        const val TYPE_ALERT_READY = "alert_ready"
        const val TYPE_MANUAL_MESSAGE = "manual"
        const val TYPE_REMOTE_ALERT = "remote_alert"
        const val TYPE_LINK_REQUEST = "link_request"
        const val TYPE_LINK_ACCEPT = "link_accept"
    }
}

data class LinkChannelPayload(
    val id: String,
    val senderId: String,
    val receiverId: String,
    val body: String,
    val timestamp: Long,
    val linkCode: String?,
    val phoneNumber: String?,
    val urgency: com.pulselink.domain.model.MessageUrgency,
    val volumeHint: com.pulselink.domain.model.VolumeHint?,
    val type: String,
    val tier: EscalationTier?,
    val reason: PulseLinkMessage.AlertPrepareReason?,
    val ready: Boolean?,
    val senderName: String?,
    val senderEmail: String?
)
