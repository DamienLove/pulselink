package com.pulselink.data.sms

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.SetOptions
import com.pulselink.data.contacts.DeviceContactsRepository
import com.pulselink.BuildConfig
import com.pulselink.domain.repository.SettingsRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import android.os.Build
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await

@HiltWorker
class SmsSyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val smsRepository: SmsRepository,
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val settingsRepository: SettingsRepository,
    private val deviceContactsRepository: DeviceContactsRepository
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        if (!BuildConfig.PREMIUM_FEATURES) {
            return Result.success()
        }

        val user = auth.currentUser
        if (user == null) {
            return Result.failure()
        }

        return try {
            val settings = settingsRepository.settings.first()
            val deviceId = settingsRepository.ensureDeviceId()
            val phoneNumber = settings.devicePhoneNumber
                ?: settingsRepository.getLastKnownPhone()
            val lineId = deviceId

            val userRef = firestore.collection("users").document(user.uid)
            val lineRef = userRef.collection("lines").document(lineId)
            val deviceRef = userRef.collection("devices").document(deviceId)
            val linePayload = mutableMapOf<String, Any>(
                "primaryDeviceId" to deviceId,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            phoneNumber?.takeIf { it.isNotBlank() }?.let { linePayload["phoneNumber"] = it }
            lineRef.set(linePayload, SetOptions.merge()).await()

            val devicePayload = mutableMapOf<String, Any>(
                "lineId" to lineId,
                "isPrimary" to true,
                "lastSeen" to FieldValue.serverTimestamp(),
                "deviceName" to "${Build.MANUFACTURER} ${Build.MODEL}".trim()
            )
            phoneNumber?.takeIf { it.isNotBlank() }?.let { devicePayload["phoneNumber"] = it }
            deviceRef.set(devicePayload, SetOptions.merge()).await()

            // Only sync messages if Remote Web Access is enabled by the user
            if (settings.remoteWebAccessEnabled) {
                val threads = smsRepository.listThreads(limit = 20)
                val legacyThreadsRef = userRef.collection("synced_threads")
                val lineThreadsRef = lineRef.collection("threads")

                for (thread in threads) {
                    val threadDoc = legacyThreadsRef.document(thread.threadId.toString())
                    val lineThreadDoc = lineThreadsRef.document(thread.threadId.toString())
                    val threadData = mapOf(
                        "address" to thread.address,
                        "snippet" to thread.snippet,
                        "date" to thread.timestamp,
                        "unread" to thread.unread,
                        "unreadCount" to thread.unreadCount,
                        "isFavorite" to thread.isFavorite,
                        "isPrivate" to thread.isPrivate,
                        "isTrusted" to thread.isTrusted
                    )
                    // Write thread data
                    threadDoc.set(threadData, SetOptions.merge()).await()
                    lineThreadDoc.set(threadData, SetOptions.merge()).await()

                    // Sync messages
                    val messages = smsRepository.messagesForThread(thread.threadId, limit = 50)
                    val messagesRef = threadDoc.collection("messages")
                    val lineMessagesRef = lineThreadDoc.collection("messages")

                    val batch = firestore.batch()
                    val lineBatch = firestore.batch()
                    var batchCount = 0

                    for (msg in messages) {
                        val msgDoc = messagesRef.document(msg.id.toString())
                        val lineMsgDoc = lineMessagesRef.document(msg.id.toString())
                        val msgData = mapOf(
                            "body" to msg.body,
                            "date" to msg.timestamp,
                            "type" to (if (msg.outgoing) 2 else 1)
                        )
                        batch.set(msgDoc, msgData, SetOptions.merge())
                        lineBatch.set(lineMsgDoc, msgData, SetOptions.merge())
                        batchCount++
                    }
                    if (batchCount > 0) {
                        batch.commit().await()
                        lineBatch.commit().await()
                    }
                }
            }

            if (deviceContactsRepository.hasContactsPermission() && settings.remoteWebAccessEnabled) {
                syncDeviceContacts(user.uid)
            }
            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.retry()
        }
    }

    private suspend fun syncDeviceContacts(userId: String) {
        val contacts = deviceContactsRepository.listPhoneContacts(limit = 500)
        val contactsRef = firestore.collection("users")
            .document(userId)
            .collection("deviceContacts")
        val existing = runCatching { contactsRef.get().await() }.getOrNull()
        val existingIds = existing?.documents?.map { it.id } ?: emptyList()
        val desiredIds = HashSet<String>()
        var batch = firestore.batch()
        var batchCount = 0

        suspend fun commitBatch() {
            if (batchCount == 0) return
            batch.commit().await()
            batch = firestore.batch()
            batchCount = 0
        }

        contacts.forEach { contact ->
            val normalized = normalizePhone(contact.phoneNumber)
            val docId = if (normalized.isNotBlank()) normalized else "id_${contact.id}"
            desiredIds.add(docId)
            val payload = mapOf(
                "displayName" to contact.displayName,
                "phoneNumber" to contact.phoneNumber,
                "normalizedPhone" to normalized,
                "contactId" to contact.id,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            batch.set(contactsRef.document(docId), payload, SetOptions.merge())
            batchCount++
            if (batchCount >= 450) {
                commitBatch()
            }
        }

        existingIds.filterNot { desiredIds.contains(it) }.forEach { docId ->
            batch.delete(contactsRef.document(docId))
            batchCount++
            if (batchCount >= 450) {
                commitBatch()
            }
        }

        commitBatch()
    }

    private fun normalizePhone(input: String?): String {
        if (input.isNullOrBlank()) return ""
        val digits = buildString {
            input.forEach { ch ->
                if (ch.isDigit()) append(ch)
            }
        }
        return if (input.trim().startsWith("+")) "+$digits" else digits
    }
}
