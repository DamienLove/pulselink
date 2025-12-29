package com.pulselink.data.sms

import android.content.Context
import android.provider.Telephony
import androidx.core.content.ContextCompat
import android.content.pm.PackageManager
import com.pulselink.data.db.ArchivedThreadDao
import com.pulselink.data.db.ContactDao
import com.pulselink.domain.model.ArchivedThread
import com.pulselink.domain.model.EscalationTier
import com.pulselink.domain.model.MessageUrgency
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.os.Build
import kotlin.jvm.Volatile
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import android.provider.ContactsContract
import android.net.Uri
import android.app.role.RoleManager
import android.util.LruCache
import com.pulselink.util.formatSmsDisplayName
import com.pulselink.util.stripSmsDisplayName

@Singleton
class SmsRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val archivedThreadDao: ArchivedThreadDao,
    private val contactDao: ContactDao
) {

    @Volatile private var observersRegistered = false
    private val observerFlow = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    private val addressCache = LruCache<String, String>(500)
    private val canonicalAddressCache = LruCache<String, String>(1000)

    private data class ThreadCacheEntry(
        val limit: Int,
        val updatedAt: Long,
        val threads: List<SmsThreadItem>,
        val archived: List<SmsThreadItem>
    )

    @Volatile private var threadsCache: ThreadCacheEntry? = null
    @Volatile private var smsOnlyCache: ThreadCacheEntry? = null
    private val CACHE_TTL_MS = 300_000L
    private val CANONICAL_URI = Uri.parse("content://mms-sms/canonical-addresses")

    init {
        ensureObserversRegistered()
    }

    fun changes(): SharedFlow<Unit> = observerFlow.asSharedFlow()

    suspend fun listThreads(
        limit: Int = 50,
        includeArchived: Boolean = false,
        onlyArchived: Boolean = false
    ): List<SmsThreadItem> {
        if (!hasReadPerms()) return emptyList()
        ensureObserversRegistered()
        val archivedIds = loadArchivedIds().toSet()
        val fastThreads = listThreadsFromThreads(limit, archivedIds, includeArchived, onlyArchived)
        if (fastThreads.isNotEmpty()) {
            return fastThreads
        }
        return listThreadsFromSms(limit, archivedIds, includeArchived, onlyArchived)
    }

    suspend fun listThreadsFromSmsOnly(
        limit: Int = 50,
        includeArchived: Boolean = false,
        onlyArchived: Boolean = false
    ): List<SmsThreadItem> {
        if (!hasReadPerms()) return emptyList()
        ensureObserversRegistered()
        val archivedIds = loadArchivedIds().toSet()
        return listThreadsFromSms(limit, archivedIds, includeArchived, onlyArchived)
    }

    suspend fun listArchivedThreads(limit: Int = 50): List<SmsThreadItem> =
        listThreads(limit = limit, includeArchived = true, onlyArchived = true)

    suspend fun listThreadsAndArchived(
        limit: Int = 50,
        fromSmsOnly: Boolean = false,
        forceRefresh: Boolean = false
    ): Pair<List<SmsThreadItem>, List<SmsThreadItem>> {
        val now = System.currentTimeMillis()
        val cache = if (fromSmsOnly) smsOnlyCache else threadsCache
        if (cache != null) {
            if (!hasReadPerms()) {
                return cache.threads to cache.archived
            }
            if (!forceRefresh && cache.limit == limit && now - cache.updatedAt < CACHE_TTL_MS) {
                return cache.threads to cache.archived
            }
        }
        if (!hasReadPerms()) return emptyList<SmsThreadItem>() to emptyList()
        val archivedIds = loadArchivedIds().toSet()
        val allThreads = if (fromSmsOnly) {
            listThreadsFromSms(limit, archivedIds, includeArchived = true, onlyArchived = false)
        } else {
            val fast = listThreadsFromThreads(limit, archivedIds, includeArchived = true, onlyArchived = false)
            if (fast.isNotEmpty()) fast else listThreadsFromSms(limit, archivedIds, includeArchived = true, onlyArchived = false)
        }
        val archived = allThreads.filter { archivedIds.contains(it.threadId) }
        val active = allThreads.filterNot { archivedIds.contains(it.threadId) }
        val entry = ThreadCacheEntry(limit = limit, updatedAt = now, threads = active, archived = archived)
        if (fromSmsOnly) {
            smsOnlyCache = entry
        } else {
            threadsCache = entry
        }
        return active to archived
    }

    fun searchMessages(query: String, limit: Int = 40): List<SmsMessageItem> {
        if (!hasReadPerms() || query.isBlank()) return emptyList()
        val pattern = "%${query.trim()}%"
        val projection = arrayOf(
            Telephony.Sms._ID,
            Telephony.Sms.THREAD_ID,
            Telephony.Sms.ADDRESS,
            Telephony.Sms.BODY,
            Telephony.Sms.DATE,
            Telephony.Sms.TYPE,
            Telephony.Sms.STATUS,
            Telephony.Sms.READ
        )
        val cursor = runCatching {
            context.contentResolver.query(
                Telephony.Sms.CONTENT_URI,
                projection,
                "${Telephony.Sms.BODY} LIKE ?",
                arrayOf(pattern),
                "${Telephony.Sms.DATE} DESC"
            )
        }.getOrNull() ?: return emptyList()

        cursor.use { c ->
            val idIdx = c.getColumnIndexOrThrow(Telephony.Sms._ID)
            val threadIdx = c.getColumnIndexOrThrow(Telephony.Sms.THREAD_ID)
            val addrIdx = c.getColumnIndexOrThrow(Telephony.Sms.ADDRESS)
            val bodyIdx = c.getColumnIndexOrThrow(Telephony.Sms.BODY)
            val dateIdx = c.getColumnIndexOrThrow(Telephony.Sms.DATE)
            val typeIdx = c.getColumnIndexOrThrow(Telephony.Sms.TYPE)
            val statusIdx = c.getColumnIndexOrThrow(Telephony.Sms.STATUS)
            val readIdx = c.getColumnIndexOrThrow(Telephony.Sms.READ)
            val hits = mutableListOf<SmsMessageItem>()
            var count = 0
            while (c.moveToNext() && count < limit) {
                val body = c.getString(bodyIdx) ?: ""
                if (SmsCodec.isPulseLinkPayload(body)) {
                    count++
                    continue
                }
                val outgoing = c.getInt(typeIdx) == Telephony.Sms.MESSAGE_TYPE_SENT ||
                    c.getInt(typeIdx) == Telephony.Sms.MESSAGE_TYPE_OUTBOX ||
                    c.getInt(typeIdx) == Telephony.Sms.MESSAGE_TYPE_QUEUED
                val statusValue = c.getInt(statusIdx)
                val isRead = c.getInt(readIdx) != 0
                hits += SmsMessageItem(
                    id = c.getLong(idIdx),
                    threadId = c.getLong(threadIdx),
                    address = resolveAddress(c.getString(addrIdx)),
                    body = body,
                    timestamp = c.getLong(dateIdx),
                    outgoing = outgoing,
                    status = resolveMessageStatus(c.getInt(typeIdx), statusValue, isRead)
                )
                count++
            }
            return hits
        }
    }

    fun messagesForThread(threadId: Long, limit: Int = 200): List<SmsMessageItem> {
        if (!hasReadPerms()) return emptyList()
        ensureObserversRegistered()
        val projection = arrayOf(
            Telephony.Sms._ID,
            Telephony.Sms.THREAD_ID,
            Telephony.Sms.ADDRESS,
            Telephony.Sms.BODY,
            Telephony.Sms.DATE,
            Telephony.Sms.TYPE,
            Telephony.Sms.STATUS,
            Telephony.Sms.READ
        )
        val cursor = runCatching {
            context.contentResolver.query(
                Telephony.Sms.CONTENT_URI,
                projection,
                "${Telephony.Sms.THREAD_ID}=?",
                arrayOf(threadId.toString()),
                "${Telephony.Sms.DATE} DESC"
            )
        }.getOrNull()
        cursor ?: return emptyList()
        cursor.use { c ->
            val idIdx = c.getColumnIndexOrThrow(Telephony.Sms._ID)
            val threadIdx = c.getColumnIndexOrThrow(Telephony.Sms.THREAD_ID)
            val addrIdx = c.getColumnIndexOrThrow(Telephony.Sms.ADDRESS)
            val bodyIdx = c.getColumnIndexOrThrow(Telephony.Sms.BODY)
            val dateIdx = c.getColumnIndexOrThrow(Telephony.Sms.DATE)
            val typeIdx = c.getColumnIndexOrThrow(Telephony.Sms.TYPE)
            val statusIdx = c.getColumnIndexOrThrow(Telephony.Sms.STATUS)
            val readIdx = c.getColumnIndexOrThrow(Telephony.Sms.READ)
            val items = mutableListOf<SmsMessageItem>()
            var count = 0
            while (c.moveToNext() && count < limit) {
                val id = c.getLong(idIdx)
                val addr = resolveAddress(c.getString(addrIdx))
                val body = c.getString(bodyIdx) ?: ""
                if (SmsCodec.isPulseLinkPayload(body)) {
                    count++
                    continue
                }
                val ts = c.getLong(dateIdx)
                val type = c.getInt(typeIdx)
                val outgoing = type == Telephony.Sms.MESSAGE_TYPE_SENT ||
                    type == Telephony.Sms.MESSAGE_TYPE_OUTBOX ||
                    type == Telephony.Sms.MESSAGE_TYPE_QUEUED
                val statusValue = c.getInt(statusIdx)
                val isRead = c.getInt(readIdx) != 0
                items += SmsMessageItem(
                    id = id,
                    threadId = c.getLong(threadIdx),
                    address = addr,
                    body = body,
                    timestamp = ts,
                    outgoing = outgoing,
                    status = resolveMessageStatus(type, statusValue, isRead)
                )
                count++
            }
            return items.sortedByDescending { it.timestamp }
        }
    }

    fun messagesForAddress(address: String, limit: Int = 200): List<SmsMessageItem> {
        if (!hasReadPerms()) return emptyList()
        val projection = arrayOf(
            Telephony.Sms._ID,
            Telephony.Sms.THREAD_ID,
            Telephony.Sms.ADDRESS,
            Telephony.Sms.BODY,
            Telephony.Sms.DATE,
            Telephony.Sms.TYPE,
            Telephony.Sms.STATUS,
            Telephony.Sms.READ
        )
        val candidates = addressCandidates(address)
        if (candidates.isEmpty()) return emptyList()
        val items = mutableListOf<SmsMessageItem>()
        candidates.forEach { candidate ->
            val cursor = runCatching {
                context.contentResolver.query(
                    Telephony.Sms.CONTENT_URI,
                    projection,
                    "${Telephony.Sms.ADDRESS}=?",
                    arrayOf(candidate),
                    "${Telephony.Sms.DATE} DESC"
                )
            }.getOrNull() ?: return@forEach
            cursor.use { c ->
                val idIdx = c.getColumnIndexOrThrow(Telephony.Sms._ID)
                val threadIdx = c.getColumnIndexOrThrow(Telephony.Sms.THREAD_ID)
                val addrIdx = c.getColumnIndexOrThrow(Telephony.Sms.ADDRESS)
                val bodyIdx = c.getColumnIndexOrThrow(Telephony.Sms.BODY)
                val dateIdx = c.getColumnIndexOrThrow(Telephony.Sms.DATE)
                val typeIdx = c.getColumnIndexOrThrow(Telephony.Sms.TYPE)
                val statusIdx = c.getColumnIndexOrThrow(Telephony.Sms.STATUS)
                val readIdx = c.getColumnIndexOrThrow(Telephony.Sms.READ)
                var count = 0
                while (c.moveToNext() && count < limit) {
                    val body = c.getString(bodyIdx) ?: ""
                    if (SmsCodec.isPulseLinkPayload(body)) {
                        count++
                        continue
                    }
                    val outgoing = c.getInt(typeIdx) == Telephony.Sms.MESSAGE_TYPE_SENT ||
                        c.getInt(typeIdx) == Telephony.Sms.MESSAGE_TYPE_OUTBOX ||
                        c.getInt(typeIdx) == Telephony.Sms.MESSAGE_TYPE_QUEUED
                    val statusValue = c.getInt(statusIdx)
                    val isRead = c.getInt(readIdx) != 0
                    items += SmsMessageItem(
                        id = c.getLong(idIdx),
                        threadId = c.getLong(threadIdx),
                        address = resolveAddress(c.getString(addrIdx)),
                        body = body,
                        timestamp = c.getLong(dateIdx),
                        outgoing = outgoing,
                        status = resolveMessageStatus(c.getInt(typeIdx), statusValue, isRead)
                    )
                    count++
                }
            }
        }
        return items.distinctBy { it.id }.sortedByDescending { it.timestamp }.take(limit)
    }

    private fun resolveMessageStatus(type: Int, status: Int, isRead: Boolean): SmsMessageStatus? {
        val outgoing = type == Telephony.Sms.MESSAGE_TYPE_SENT ||
            type == Telephony.Sms.MESSAGE_TYPE_OUTBOX ||
            type == Telephony.Sms.MESSAGE_TYPE_QUEUED
        return if (outgoing) {
            when (type) {
                Telephony.Sms.MESSAGE_TYPE_OUTBOX,
                Telephony.Sms.MESSAGE_TYPE_QUEUED -> SmsMessageStatus.SENDING
                Telephony.Sms.MESSAGE_TYPE_FAILED -> SmsMessageStatus.FAILED
                Telephony.Sms.MESSAGE_TYPE_SENT -> when (status) {
                    Telephony.TextBasedSmsColumns.STATUS_COMPLETE -> SmsMessageStatus.DELIVERED
                    Telephony.TextBasedSmsColumns.STATUS_FAILED -> SmsMessageStatus.FAILED
                    else -> SmsMessageStatus.SENT
                }
                else -> SmsMessageStatus.SENT
            }
        } else {
            if (isRead) SmsMessageStatus.READ else SmsMessageStatus.RECEIVED    
        }
    }

    fun resolveThreadIdForAddress(address: String): Long? {
        if (!hasReadPerms()) return null
        val projection = arrayOf(
            Telephony.Sms.THREAD_ID,
            Telephony.Sms.ADDRESS,
            Telephony.Sms.DATE
        )
        val candidates = addressCandidates(address)
        candidates.forEach { candidate ->
            val cursor = runCatching {
                context.contentResolver.query(
                    Telephony.Sms.CONTENT_URI,
                    projection,
                    "${Telephony.Sms.ADDRESS}=?",
                    arrayOf(candidate),
                    "${Telephony.Sms.DATE} DESC"
                )
            }.getOrNull() ?: return@forEach
            cursor.use { c ->
                if (c.moveToFirst()) {
                    val idx = c.getColumnIndexOrThrow(Telephony.Sms.THREAD_ID)
                    return c.getLong(idx)
                }
            }
        }
        return null
    }

    fun purgeExpiredOtpMessages(expiryMillis: Long) {
        if (!hasWritePerms()) return
        val cutoff = System.currentTimeMillis() - expiryMillis
        val projection = arrayOf(
            Telephony.Sms._ID,
            Telephony.Sms.ADDRESS,
            Telephony.Sms.BODY,
            Telephony.Sms.DATE
        )
        val cursor = runCatching {
            context.contentResolver.query(
                Telephony.Sms.CONTENT_URI,
                projection,
                "${Telephony.Sms.DATE}<?",
                arrayOf(cutoff.toString()),
                "${Telephony.Sms.DATE} ASC"
            )
        }.getOrNull() ?: return

        val staleIds = mutableListOf<Long>()
        cursor.use { c ->
            val idIdx = c.getColumnIndexOrThrow(Telephony.Sms._ID)
            val addrIdx = c.getColumnIndexOrThrow(Telephony.Sms.ADDRESS)
            val bodyIdx = c.getColumnIndexOrThrow(Telephony.Sms.BODY)
            while (c.moveToNext() && staleIds.size < 200) {
                val addr = c.getString(addrIdx) ?: ""
                val body = c.getString(bodyIdx) ?: ""
                if (OtpHelper.isOtpMessage(addr, body)) {
                    staleIds += c.getLong(idIdx)
                }
            }
        }

        if (staleIds.isNotEmpty()) {
            staleIds.forEach { id ->
                context.contentResolver.delete(
                    Telephony.Sms.CONTENT_URI,
                    "${Telephony.Sms._ID}=?",
                    arrayOf(id.toString())
                )
            }
            invalidateThreadCaches()
            observerFlow.tryEmit(Unit)
        }
    }

    private fun resolveAddress(raw: String?): String {
        if (!hasReadPerms()) return raw.orEmpty()
        val number = raw?.trim().orEmpty()
        if (number.isBlank()) return ""
        val cached = addressCache.get(number)
        if (cached != null) return cached
        val uri = ContactsContract.PhoneLookup.CONTENT_FILTER_URI
        val lookupUri = Uri.withAppendedPath(uri, Uri.encode(number))
        runCatching {
            context.contentResolver.query(
                lookupUri,
                arrayOf(ContactsContract.PhoneLookup.DISPLAY_NAME, ContactsContract.PhoneLookup.NUMBER),
                null,
                null,
                null
            )
        }.getOrNull()?.use { c ->
            if (c.moveToFirst()) {
                val nameIdx = c.getColumnIndexOrThrow(ContactsContract.PhoneLookup.DISPLAY_NAME)
                val numIdx = c.getColumnIndexOrThrow(ContactsContract.PhoneLookup.NUMBER)
                val name = c.getString(nameIdx) ?: ""
                val formatted = c.getString(numIdx) ?: number
                val resolved = if (name.isNotBlank()) formatSmsDisplayName(name, formatted) else formatted
                addressCache.put(number, resolved)
                return resolved
            }
        }
        addressCache.put(number, number)
        return number
    }

    private fun addressCandidates(raw: String): List<String> {
        val trimmed = raw.trim()
        if (trimmed.isBlank()) return emptyList()
        val parsedNumber = stripSmsDisplayName(trimmed).trim()
        val normalized = normalizePhone(parsedNumber)
        val noPlus = normalized.removePrefix("+")
        return listOf(trimmed, parsedNumber, normalized, noPlus)
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinct()
    }

    private fun normalizePhone(input: String): String {
        if (input.isBlank()) return ""
        val digits = buildString {
            input.forEach { ch ->
                if (ch.isDigit()) append(ch)
            }
        }
        return if (input.startsWith("+")) "+$digits" else digits
    }

    fun markThreadRead(threadId: Long): Boolean {
        if (!hasWritePerms()) return false
        val values = android.content.ContentValues().apply {
            put(Telephony.Sms.READ, 1)
            put(Telephony.Sms.SEEN, 1)
        }
        return runCatching {
            context.contentResolver.update(
                Telephony.Sms.CONTENT_URI,
                values,
                "${Telephony.Sms.THREAD_ID}=?",
                arrayOf(threadId.toString())
            )
            context.contentResolver.update(
                Telephony.Threads.CONTENT_URI,
                values,
                "${Telephony.Threads._ID}=?",
                arrayOf(threadId.toString())
            )
            invalidateThreadCaches()
            observerFlow.tryEmit(Unit)
            true
        }.getOrDefault(false)
    }

    fun archiveThread(threadId: Long): Boolean {
        return runCatching {
            archivedThreadDao.insert(ArchivedThread(threadId = threadId, archivedAt = System.currentTimeMillis()))
            invalidateThreadCaches()
            observerFlow.tryEmit(Unit)
            true
        }.getOrDefault(false)
    }

    fun unarchiveThread(threadId: Long): Boolean {
        return runCatching {
            archivedThreadDao.deleteByThreadId(threadId)
            invalidateThreadCaches()
            observerFlow.tryEmit(Unit)
            true
        }.getOrDefault(false)
    }

    fun isThreadArchived(threadId: Long): Boolean {
        return runCatching { archivedThreadDao.isArchived(threadId) }.getOrDefault(false)
    }

    fun deleteThread(threadId: Long): Boolean {
        if (!hasWritePerms()) return false
        return runCatching {
            context.contentResolver.delete(
                Telephony.Sms.CONTENT_URI,
                "${Telephony.Sms.THREAD_ID}=?",
                arrayOf(threadId.toString())
            )
            archivedThreadDao.deleteByThreadId(threadId)
            invalidateThreadCaches()
            observerFlow.tryEmit(Unit)
            true
        }.getOrDefault(false)
    }

    companion object {
        private fun isDefaultSmsApp(context: Context): Boolean {
            return runCatching {
                val roleHeld = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    context.getSystemService(RoleManager::class.java)
                        ?.isRoleHeld(RoleManager.ROLE_SMS) == true
                } else {
                    false
                }
                val telephonyDefault =
                    Telephony.Sms.getDefaultSmsPackage(context) == context.packageName
                roleHeld || telephonyDefault
            }.getOrDefault(false)
        }

        private fun hasReadSmsPermission(context: Context): Boolean {
            return ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.READ_SMS
            ) == PackageManager.PERMISSION_GRANTED
        }

        private fun hasWriteSmsPermission(context: Context): Boolean {
            return isDefaultSmsApp(context)
        }
    }

    private fun hasReadPerms(): Boolean = isDefaultSmsApp(context) || hasReadSmsPermission(context)
    private fun hasWritePerms(): Boolean = hasWriteSmsPermission(context)

    private fun ensureObserversRegistered() {
        if (observersRegistered || !hasReadPerms()) return
        val handler = Handler(Looper.getMainLooper())
        val observer = object : ContentObserver(handler) {
            override fun onChange(selfChange: Boolean) {
                invalidateThreadCaches()
                observerFlow.tryEmit(Unit)
            }
        }
        val registered = runCatching {
            context.contentResolver.registerContentObserver(
                Telephony.Sms.CONTENT_URI,
                true,
                observer
            )
            context.contentResolver.registerContentObserver(
                Telephony.Threads.CONTENT_URI,
                true,
                observer
            )
            true
        }.getOrDefault(false)
        observersRegistered = registered
    }

    private fun loadArchivedIds(): List<Long> {
        return runCatching { archivedThreadDao.getAllIds() }.getOrDefault(emptyList())
    }

    private fun invalidateThreadCaches() {
        threadsCache = null
        smsOnlyCache = null
    }

    private suspend fun listThreadsFromThreads(
        limit: Int,
        archivedIds: Set<Long>,
        includeArchived: Boolean,
        onlyArchived: Boolean
    ): List<SmsThreadItem> {
        if (!hasReadPerms()) return emptyList()
        val threadProjection = arrayOf(
            Telephony.Threads._ID,
            Telephony.Threads.DATE,
            Telephony.Threads.READ,
            Telephony.Threads.SNIPPET,
            Telephony.Threads.RECIPIENT_IDS
        )
        val threadCursor = runCatching {
            context.contentResolver.query(
                Telephony.Threads.CONTENT_URI,
                threadProjection,
                null,
                null,
                "${Telephony.Threads.DATE} DESC"
            )
        }.getOrNull() ?: return emptyList()

        data class ThreadRow(
            val threadId: Long,
            val timestamp: Long,
            val unread: Boolean,
            val snippet: String,
            val recipientIds: String
        )

        val rows = mutableListOf<ThreadRow>()
        val recipientIdSet = linkedSetOf<Long>()
        threadCursor.use { c ->
            val idIdx = c.getColumnIndexOrThrow(Telephony.Threads._ID)
            val readIdx = c.getColumnIndexOrThrow(Telephony.Threads.READ)
            val dateIdx = c.getColumnIndexOrThrow(Telephony.Threads.DATE)
            val snippetIdx = c.getColumnIndexOrThrow(Telephony.Threads.SNIPPET)
            val recipientsIdx = c.getColumnIndexOrThrow(Telephony.Threads.RECIPIENT_IDS)
            while (c.moveToNext() && rows.size < limit) {
                val threadId = c.getLong(idIdx)
                val unread = c.getInt(readIdx) == 0
                val snippet = c.getString(snippetIdx) ?: ""
                val recipientIds = c.getString(recipientsIdx).orEmpty()
                rows += ThreadRow(
                    threadId = threadId,
                    timestamp = c.getLong(dateIdx),
                    unread = unread,
                    snippet = snippet,
                    recipientIds = recipientIds
                )
                recipientIds.split(' ')
                    .asSequence()
                    .map { it.trim() }
                    .filter { it.isNotEmpty() }
                    .mapNotNull { it.toLongOrNull() }
                    .forEach { recipientIdSet.add(it) }
            }
        }
        if (rows.isEmpty()) return emptyList()

        if (!loadCanonicalAddresses(recipientIdSet)) {
            return emptyList()
        }
        val items = mutableListOf<SmsThreadItem>()
        rows.forEach { row ->
            if (SmsCodec.isPulseLinkPayload(row.snippet)) return@forEach
            val isArchived = archivedIds.contains(row.threadId)
            if (onlyArchived && !isArchived) return@forEach
            if (!includeArchived && isArchived) return@forEach
            val recipients = row.recipientIds.split(' ')
                .mapNotNull { id ->
                    val trimmed = id.trim()
                    if (trimmed.isEmpty()) return@mapNotNull null
                    canonicalAddressCache.get(trimmed)?.takeIf { it.isNotBlank() }
                }
            if (recipients.isEmpty()) return@forEach
            val displayAddress = if (recipients.isEmpty()) "" else {
                recipients.joinToString(", ") { resolveAddress(it) }
            }
            val primaryRecipient = recipients.firstOrNull().orEmpty()
            val phone = if (primaryRecipient.isNotBlank()) primaryRecipient else stripSmsDisplayName(displayAddress)
            val normalized = normalizePhone(phone)
            val contact = contactDao.getByPhone(phone)
                ?: contactDao.getByPhone(normalized)
            val trustedUrgency = contact?.let {
                when {
                    OtpHelper.isUrgentBody(row.snippet) -> MessageUrgency.URGENT
                    it.escalationTier == EscalationTier.EMERGENCY -> MessageUrgency.EMERGENCY
                    else -> MessageUrgency.STANDARD
                }
            }
            items += SmsThreadItem(
                threadId = row.threadId,
                address = displayAddress,
                snippet = row.snippet,
                timestamp = row.timestamp,
                unread = row.unread,
                isPrivate = contact?.isPrivate == true,
                isFavorite = contact?.isFavorite == true,
                isTrusted = contact != null,
                trustedUrgency = trustedUrgency,
                isOtp = OtpHelper.isOtpMessage(phone, row.snippet)
            )
        }
        return items.sortedByDescending { it.timestamp }
    }

    private fun loadCanonicalAddresses(recipientIds: Set<Long>): Boolean {
        if (!hasReadPerms()) return false
        if (recipientIds.isEmpty()) return true
        val missing = recipientIds.filter { canonicalAddressCache.get(it.toString()) == null }
        if (missing.isEmpty()) return true
        val projection = arrayOf("_id", "address")
        var loadedAny = false
        missing.chunked(100).forEach { chunk ->
            val selection = "_id IN (${chunk.joinToString(",") { "?" }})"
            val args = chunk.map { it.toString() }.toTypedArray()
            val cursor = runCatching {
                context.contentResolver.query(
                    CANONICAL_URI,
                    projection,
                    selection,
                    args,
                    null
                )
            }.getOrNull() ?: return@forEach
            cursor.use { c ->
                val idIdx = c.getColumnIndexOrThrow("_id")
                val addrIdx = c.getColumnIndexOrThrow("address")
                while (c.moveToNext()) {
                    val id = c.getLong(idIdx)
                    val addr = c.getString(addrIdx).orEmpty()
                    canonicalAddressCache.put(id.toString(), addr)
                    if (addr.isNotBlank()) {
                        loadedAny = true
                    }
                }
            }
        }
        return loadedAny || recipientIds.any {
            canonicalAddressCache.get(it.toString())?.isNotBlank() == true
        }
    }

    private suspend fun listThreadsFromSms(
        limit: Int,
        archivedIds: Set<Long>,
        includeArchived: Boolean,
        onlyArchived: Boolean
    ): List<SmsThreadItem> {
        if (!hasReadPerms()) return emptyList()
        val projection = arrayOf(
            Telephony.Sms._ID,
            Telephony.Sms.THREAD_ID,
            Telephony.Sms.ADDRESS,
            Telephony.Sms.BODY,
            Telephony.Sms.DATE,
            Telephony.Sms.READ
        )
        val cursor = runCatching {
            context.contentResolver.query(
                Telephony.Sms.CONTENT_URI,
                projection,
                null,
                null,
                "${Telephony.Sms.DATE} DESC"
            )
        }.getOrNull() ?: return emptyList()

        cursor.use { c ->
            val threadIdx = c.getColumnIndexOrThrow(Telephony.Sms.THREAD_ID)
            val addrIdx = c.getColumnIndexOrThrow(Telephony.Sms.ADDRESS)
            val bodyIdx = c.getColumnIndexOrThrow(Telephony.Sms.BODY)
            val dateIdx = c.getColumnIndexOrThrow(Telephony.Sms.DATE)
            val readIdx = c.getColumnIndexOrThrow(Telephony.Sms.READ)
            val seenThreads = HashSet<Long>()
            val items = mutableListOf<SmsThreadItem>()
            var count = 0
            while (c.moveToNext() && count < limit) {
                val threadId = c.getLong(threadIdx)
                val body = c.getString(bodyIdx) ?: ""
                if (SmsCodec.isPulseLinkPayload(body)) {
                    continue
                }
                if (!seenThreads.add(threadId)) continue
                val ts = c.getLong(dateIdx)
                val unread = c.getInt(readIdx) == 0
                val address = resolveAddress(c.getString(addrIdx))
                val isArchived = archivedIds.contains(threadId)

                if (onlyArchived && !isArchived) {
                    // skip
                } else if (!includeArchived && isArchived) {
                    // skip
                } else {
                    val phone = stripSmsDisplayName(address)
                    val normalized = normalizePhone(phone)
                    val contact = contactDao.getByPhone(phone)
                        ?: contactDao.getByPhone(normalized)
                    val trustedUrgency = contact?.let {
                        when {
                            OtpHelper.isUrgentBody(body) -> MessageUrgency.URGENT
                            it.escalationTier == EscalationTier.EMERGENCY -> MessageUrgency.EMERGENCY
                            else -> MessageUrgency.STANDARD
                        }
                    }
                    items += SmsThreadItem(
                        threadId = threadId,
                        address = address,
                        snippet = body,
                        timestamp = ts,
                        unread = unread,
                        isPrivate = contact?.isPrivate == true,
                        isFavorite = contact?.isFavorite == true,
                        isTrusted = contact != null,
                        trustedUrgency = trustedUrgency,
                        isOtp = OtpHelper.isOtpMessage(phone, body)
                    )
                }
                count++
            }
            return items
        }
    }
}
