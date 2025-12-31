package com.pulselink.data.sms

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

@Singleton
class RemoteSmsRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) {
    fun observeThreads(lineIds: List<String>): Flow<List<SmsThreadItem>> = callbackFlow {
        if (lineIds.isEmpty()) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }
        val snapshotsByLine = mutableMapOf<String, List<SmsThreadItem>>()
        val listeners = mutableListOf<ListenerRegistration>()

        fun clearListeners() {
            listeners.forEach { it.remove() }
            listeners.clear()
            snapshotsByLine.clear()
        }

        val authListener = FirebaseAuth.AuthStateListener { state ->
            clearListeners()
            val user = state.currentUser
            if (user == null) {
                trySend(emptyList())
                return@AuthStateListener
            }
            val userRef = firestore.collection("users").document(user.uid)
            lineIds.forEach { lineId ->
                val ref = userRef.collection("lines").document(lineId).collection("threads")
                val listener = ref.addSnapshotListener { snapshot, _ ->
                    val threads = snapshot?.documents?.mapNotNull { doc ->
                        val threadId = doc.id.toLongOrNull() ?: return@mapNotNull null
                        val address = doc.getString("address") ?: return@mapNotNull null
                        val unread = doc.getBoolean("unread") == true
                        val rawUnreadCount = doc.getLong("unreadCount") ?: 0L
                        val unreadCount = if (unread && rawUnreadCount == 0L) 1 else rawUnreadCount.toInt()
                        SmsThreadItem(
                            threadId = threadId,
                            address = address,
                            snippet = doc.getString("snippet").orEmpty(),
                            timestamp = doc.getLong("date") ?: 0L,
                            unread = unread,
                            unreadCount = unreadCount,
                            isPrivate = doc.getBoolean("isPrivate") == true,
                            isFavorite = doc.getBoolean("isFavorite") == true,
                            isTrusted = doc.getBoolean("isTrusted") == true,
                            lineId = lineId
                        )
                    } ?: emptyList()
                    snapshotsByLine[lineId] = threads
                    val merged = snapshotsByLine.values.flatten()
                        .sortedByDescending { it.timestamp }
                    trySend(merged)
                }
                listeners.add(listener)
            }
        }
        auth.addAuthStateListener(authListener)
        authListener.onAuthStateChanged(auth)

        awaitClose {
            clearListeners()
            auth.removeAuthStateListener(authListener)
        }
    }

    fun observeMessages(lineId: String, threadId: Long): Flow<List<SmsMessageItem>> = callbackFlow {
        val user = auth.currentUser
        if (user == null || lineId.isBlank()) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }
        val ref = firestore.collection("users").document(user.uid)
            .collection("lines").document(lineId)
            .collection("threads").document(threadId.toString())
            .collection("messages")
        val listener = ref.addSnapshotListener { snapshot, _ ->
            val messages = snapshot?.documents?.mapNotNull { doc ->
                val msgId = doc.id.toLongOrNull() ?: return@mapNotNull null
                val body = doc.getString("body") ?: return@mapNotNull null
                val ts = doc.getLong("date") ?: 0L
                val type = doc.getLong("type")?.toInt() ?: 1
                val outgoing = type == 2
                SmsMessageItem(
                    id = msgId,
                    threadId = threadId,
                    address = "",
                    body = body,
                    timestamp = ts,
                    outgoing = outgoing,
                    status = if (outgoing) SmsMessageStatus.SENT else SmsMessageStatus.RECEIVED
                )
            } ?: emptyList()
            trySend(messages.sortedByDescending { it.timestamp })
        }
        awaitClose { listener.remove() }
    }
}
