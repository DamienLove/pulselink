package com.pulselink.beacon.data

import android.content.Context
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce

class SmsSyncManager(private val context: Context) {

    private val auth by lazy { FirebaseAuth.getInstance() }
    private val db by lazy { FirebaseFirestore.getInstance() }
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val repository = SmsRepository(context)

    // Flag to prevent multiple concurrent syncs
    private val isSyncing = AtomicBoolean(false)
    private var isPremium = false

    fun startMonitoring() {
        auth.addAuthStateListener { firebaseAuth ->
            val user = firebaseAuth.currentUser
            if (user != null) {
                // Listen for premium status
                db.collection("users").document(user.uid)
                    .addSnapshotListener { snapshot, e ->
                        if (e != null) {
                            Log.w("SmsSyncManager", "Listen failed.", e)
                            return@addSnapshotListener
                        }
                        if (snapshot != null && snapshot.exists()) {
                            val data = snapshot.data ?: emptyMap()
                            val subStatus = data["subscriptionStatus"] as? String
                            val unlocked = data["premiumUnlocked"] == true
                            val history = data["hasPremiumHistory"] == true
                            val remoteEnabled = data["remoteWebAccessEnabled"] == true

                            val newIsPremium = (subStatus == "premium" || unlocked || history) && remoteEnabled

                            if (newIsPremium && !isPremium) {
                                // Just became premium/enabled
                                scope.launch {
                                    performFullSync(user.uid)
                                }
                            }
                            isPremium = newIsPremium
                        }
                    }
            } else {
                isPremium = false
            }
        }

        // Listen for SMS database changes (incoming or outgoing from any app)
        scope.launch {
            repository.changes()
                .debounce(1000L) // Debounce to prevent thrashing on rapid updates
                .collectLatest {
                    val user = auth.currentUser
                    if (isPremium && user != null) {
                        syncRecent(user.uid)
                    }
                }
        }
    }

    private suspend fun performFullSync(uid: String) {
        if (isSyncing.getAndSet(true)) return
        try {
            Log.d("SmsSyncManager", "Starting full sync for $uid")
            val threads = repository.listThreads(limit = 100)

            // Sync threads
            threads.forEach { thread ->
                syncThread(uid, thread)
                // Sync messages for each thread
                val messages = repository.messagesForThread(thread.threadId, limit = 50)
                messages.forEach { msg ->
                    syncMessage(uid, msg)
                }
            }
            Log.d("SmsSyncManager", "Full sync complete")
        } catch (e: Exception) {
            Log.e("SmsSyncManager", "Error during full sync", e)
        } finally {
            isSyncing.set(false)
        }
    }

    private suspend fun syncRecent(uid: String) {
        try {
            // Sync top 10 most recent threads to catch any new activity
            val threads = repository.listThreads(limit = 10)
            threads.forEach { thread ->
                syncThread(uid, thread)
                // Sync latest messages for this thread
                val messages = repository.messagesForThread(thread.threadId, limit = 10)
                messages.forEach { msg ->
                    syncMessage(uid, msg)
                }
            }
        } catch (e: Exception) {
            Log.e("SmsSyncManager", "Error syncing recent", e)
        }
    }

    fun syncIncoming(address: String, body: String, timestamp: Long) {
        val user = auth.currentUser ?: return
        if (!isPremium) return

        scope.launch {
            try {
                // Try to find the thread by address or sync latest message
                // We add a small delay to ensure the system has written the message to the DB
                // so repository.messagesForThread returns the new message with the correct ID.
                kotlinx.coroutines.delay(1000)

                val threads = repository.listThreads(limit = 20)
                val thread = threads.firstOrNull { matchThread(it.address, address) }

                if (thread != null) {
                    syncThread(user.uid, thread)

                    // Attempt to find the exact message we just received
                    val latestMessages = repository.messagesForThread(thread.threadId, limit = 5)
                    val match = latestMessages.firstOrNull {
                         // Check within 10 seconds and matching body
                         Math.abs(it.timestamp - timestamp) < 10000 && it.body == body
                    }

                    if (match != null) {
                        syncMessage(user.uid, match)
                    } else {
                        // Fallback if we can't find it (unlikely with delay)
                        val msg = SmsMessageItem(
                            id = timestamp, // Temporary ID
                            threadId = thread.threadId,
                            address = address,
                            body = body,
                            timestamp = timestamp,
                            outgoing = false
                        )
                        syncMessage(user.uid, msg)
                    }
                }

            } catch (e: Exception) {
                Log.e("SmsSyncManager", "Error syncing incoming", e)
            }
        }
    }

    fun syncOutgoing(address: String, body: String) {
        val user = auth.currentUser ?: return
        if (!isPremium) return

        scope.launch {
             try {
                // Wait for system write
                kotlinx.coroutines.delay(1000)

                val threads = repository.listThreads(limit = 20)
                val thread = threads.firstOrNull { matchThread(it.address, address) }

                if (thread != null) {
                    syncThread(user.uid, thread)
                    val latestMessages = repository.messagesForThread(thread.threadId, limit = 5)
                    val match = latestMessages.firstOrNull {
                         it.outgoing && it.body == body
                    }

                    if (match != null) {
                         syncMessage(user.uid, match)
                    } else {
                        val msg = SmsMessageItem(
                            id = System.currentTimeMillis(),
                            threadId = thread.threadId,
                            address = address,
                            body = body,
                            timestamp = System.currentTimeMillis(),
                            outgoing = true
                        )
                        syncMessage(user.uid, msg)
                    }
                }
            } catch (e: Exception) {
                Log.e("SmsSyncManager", "Error syncing outgoing", e)
            }
        }
    }

    private suspend fun syncThread(uid: String, thread: SmsThreadItem) {
        val data = hashMapOf(
            "address" to thread.address,
            "snippet" to thread.snippet,
            "date" to thread.timestamp,
            "unread" to thread.unread,
            "archived" to thread.isArchived,
            "pinned" to thread.isPinned
        )
        db.collection("users").document(uid)
            .collection("synced_threads").document(thread.threadId.toString())
            .set(data, SetOptions.merge())
            .await()
    }

    private suspend fun syncMessage(uid: String, msg: SmsMessageItem) {
        val data = hashMapOf(
            "body" to msg.body,
            "date" to msg.timestamp,
            "type" to (if (msg.outgoing) 2 else 1),
            "read" to true,
            "address" to msg.address
        )
        db.collection("users").document(uid)
            .collection("synced_threads").document(msg.threadId.toString())
            .collection("messages").document(msg.id.toString())
            .set(data, SetOptions.merge())
            .await()
    }

    private fun matchThread(threadAddress: String, targetAddress: String): Boolean {
        val cleanedIt = cleanNumber(threadAddress)
        val cleanedTarget = cleanNumber(targetAddress)

        return if (cleanedTarget.isNotEmpty() && cleanedIt.isNotEmpty()) {
            cleanedIt.contains(cleanedTarget) || cleanedTarget.contains(cleanedIt)
        } else {
            threadAddress.contains(targetAddress, ignoreCase = true)
        }
    }

    private fun cleanNumber(num: String): String {
        return num.replace("[^0-9]".toRegex(), "")
    }

    companion object {
        @Volatile
        private var INSTANCE: SmsSyncManager? = null

        fun getInstance(context: Context): SmsSyncManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: SmsSyncManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}
