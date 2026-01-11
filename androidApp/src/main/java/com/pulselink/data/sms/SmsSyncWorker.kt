package com.pulselink.data.sms

import android.content.Context
import android.os.Build
import android.provider.Telephony
import androidx.core.content.ContextCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.pulselink.BuildConfig
import com.pulselink.data.contacts.DeviceContactsRepository
import com.pulselink.domain.repository.SettingsRepository
import com.pulselink.util.splitSmsDisplayAddress
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
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
        val settings = settingsRepository.settings.first()
        val isPremium = BuildConfig.PREMIUM_FEATURES || settings.premiumUnlocked
        val isPro = settings.proUnlocked
        val hasReadSms = hasSmsPermission()

        val user = auth.currentUser ?: run {
            // No signed-in user; log and stop gracefully
            writeDiagnostics("unknown", settingsRepository.ensureDeviceId(), "unknown", 0, 0, hasReadSms, "no user")
            return Result.success()
        }

        return try {
            val userRef = firestore.collection("users").document(user.uid)
            val deviceId = settingsRepository.ensureDeviceId()

            // Sync subscription status to allow Web client to unlock features
            val status = when {
                isPremium -> "premium"
                isPro -> "pro"
                else -> "free"
            }
            userRef.set(
                mapOf(
                    "subscriptionStatus" to status,
                    "remoteWebAccessEnabled" to settings.remoteWebAccessEnabled
                ),
                SetOptions.merge()
            ).await()

            // If the user hasn't enabled remote web access, there's nothing to sync.
            if (!settings.remoteWebAccessEnabled) {
                writeDiagnostics(user.uid, deviceId, status, 0, 0, hasReadSms, "remoteWebAccess off")
                return Result.success()
            }
            if (!hasReadSms) {
                writeDiagnostics(user.uid, deviceId, status, 0, 0, hasReadSms, "READ_SMS missing")
                return Result.success()
            }

            val phoneNumber = settings.devicePhoneNumber ?: settingsRepository.getLastKnownPhone()
            val lineId = deviceId

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

            var syncedThreads = 0
            var syncedMessages = 0
            val threads = smsRepository.listThreads(limit = 50)
            val lineThreadsRef = lineRef.collection("threads")

            // Identify which threads are currently in Firestore so we can delete stale ones
            val existingThreadsSnapshot = lineThreadsRef.get().await()
            val existingThreadIds = existingThreadsSnapshot.documents.map { it.id }.toMutableSet()

            for (thread in threads) {
                val threadIdStr = thread.threadId.toString()
                existingThreadIds.remove(threadIdStr) // Mark as "present"

                val lineThreadDoc = lineThreadsRef.document(threadIdStr)

                val (namePart, numberPart) = splitSmsDisplayAddress(thread.address)
                val displayName = if (numberPart != null && namePart.isNotBlank()) namePart else ""
                val phone = numberPart?.takeIf { it.isNotBlank() } ?: namePart

                val threadData = mapOf(
                    "address" to thread.address,
                    "display_name" to displayName,
                    "phone_number" to phone,
                    "snippet" to thread.snippet,
                    "date" to thread.timestamp,
                    "unread" to thread.unread,
                    "unreadCount" to thread.unreadCount,
                    "isFavorite" to thread.isFavorite,
                    "isPrivate" to thread.isPrivate,
                    "isTrusted" to thread.isTrusted
                )
                lineThreadDoc.set(threadData, SetOptions.merge()).await()
                syncedThreads++

                val messages = smsRepository.messagesForThread(thread.threadId, limit = 200)
                val lineMessagesRef = lineThreadDoc.collection("messages")
                val lineBatch = firestore.batch()
                var batchCount = 0

                for (msg in messages) {
                    val lineMsgDoc = lineMessagesRef.document(msg.id.toString())
                    val msgData = mapOf(
                        "body" to msg.body,
                        "date" to msg.timestamp,
                        "type" to (if (msg.outgoing) 2 else 1)
                    )
                    lineBatch.set(lineMsgDoc, msgData, SetOptions.merge())
                    batchCount++
                    syncedMessages++
                    if (batchCount >= 450) {
                        lineBatch.commit().await()
                        batchCount = 0
                    }
                }
                if (batchCount > 0) {
                    lineBatch.commit().await()
                }
            }

            // Clean up threads that no longer exist locally (or fell out of the top 50)
            if (existingThreadIds.isNotEmpty()) {
                val deleteBatch = firestore.batch()
                var deleteCount = 0
                for (oldId in existingThreadIds) {
                    deleteBatch.delete(lineThreadsRef.document(oldId))
                    deleteCount++
                    if (deleteCount >= 450) {
                        deleteBatch.commit().await()
                        deleteBatch = firestore.batch()
                        deleteCount = 0
                    }
                }
                if (deleteCount > 0) {
                    deleteBatch.commit().await()
                }
            }

            // Mark last sync for the line
            lineRef.set(mapOf("lastSyncAt" to FieldValue.serverTimestamp()), SetOptions.merge()).await()

            writeDiagnostics(
                user.uid,
                deviceId,
                status,
                syncedThreads,
                syncedMessages,
                hasReadSms,
                "ok"
            )

            if (deviceContactsRepository.hasContactsPermission() && settings.remoteWebAccessEnabled) {
                syncDeviceContacts(user.uid)
            }
            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            runCatching {
                writeDiagnostics(
                    auth.currentUser?.uid ?: "unknown",
                    settingsRepository.ensureDeviceId(),
                    "unknown",
                    0,
                    0,
                    hasSmsPermission(),
                    "error: ${e.message}"
                )
            }
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

    private fun hasSmsPermission(): Boolean {
        val ctx = applicationContext
        val perms = listOf(
            android.Manifest.permission.READ_SMS,
            android.Manifest.permission.RECEIVE_SMS,
            android.Manifest.permission.SEND_SMS
        )
        return perms.all { perm ->
            ContextCompat.checkSelfPermission(ctx, perm) == android.content.pm.PackageManager.PERMISSION_GRANTED
        }
    }

    private suspend fun writeDiagnostics(
        userId: String,
        deviceId: String,
        subscriptionStatus: String,
        threadCount: Int,
        messageCount: Int,
        hasReadSms: Boolean,
        status: String
    ) {
        runCatching {
            val doc = firestore.collection("users").document(userId)
                .collection("syncDiagnostics")
                .document("latest")
            val payload = mapOf(
                "deviceId" to deviceId,
                "subscriptionStatus" to subscriptionStatus,
                "threadCount" to threadCount,
                "messageCount" to messageCount,
                "hasReadSms" to hasReadSms,
                "status" to status,
                "timestamp" to FieldValue.serverTimestamp(),
                "appVersion" to BuildConfig.VERSION_NAME
            )
            doc.set(payload, SetOptions.merge()).await()
        }
    }
}
