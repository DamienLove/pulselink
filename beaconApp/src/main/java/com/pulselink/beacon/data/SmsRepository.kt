package com.pulselink.beacon.data

import android.content.Context
import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.os.Build
import android.app.role.RoleManager
import android.provider.ContactsContract
import android.provider.Telephony
import androidx.core.content.ContextCompat
import com.pulselink.beacon.data.MmsPart
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlin.jvm.Volatile

class SmsRepository(private val context: Context) {

    @Volatile private var observersRegistered = false
    private val observerFlow = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    private val otpRegex = Regex("\\b\\d{4,8}\\b")
    private val inboxPrefs = InboxPreferencesRepository(context)

    // Expose flow directly to be collected by ViewModel scope
    val inboxStateFlow = inboxPrefs.flow

    init {
        ensureObserversRegistered()
        purgeExpiredOneTimeCodes()
    }

    suspend fun togglePin(threadId: Long) {
        inboxPrefs.togglePin(threadId)
        observerFlow.tryEmit(Unit)
    }

    suspend fun toggleArchive(threadId: Long) {
        inboxPrefs.toggleArchive(threadId)
        observerFlow.tryEmit(Unit)
    }

    fun changes(): SharedFlow<Unit> = observerFlow.asSharedFlow()

    fun listInboxThreads(limit: Int = 50, state: InboxState): List<SmsThreadItem> {
        if (!hasReadPerms()) return emptyList()
        ensureObserversRegistered()

        val items = mutableListOf<SmsThreadItem>()

        // 1. Fetch Pinned Threads explicitly
        val pinnedIds = state.pinnedThreadIds.filter { it > 0 }
        if (pinnedIds.isNotEmpty()) {
            items.addAll(fetchSpecificThreads(pinnedIds, state))
        }

        // 2. Fetch Regular Threads (Unarchived)
        val archivedIds = state.archivedThreadIds.filter { it > 0 }
        val canUseSqlFilter = archivedIds.size < SQLITE_MAX_ARGS_SAFE

        val selection: String?
        val selectionArgs: Array<String>?

        if (canUseSqlFilter && archivedIds.isNotEmpty()) {
            selection = "${Telephony.Threads._ID} NOT IN (${archivedIds.joinToString(",") { "?" }})"
            selectionArgs = archivedIds.map { it.toString() }.toTypedArray()
        } else {
            selection = null
            selectionArgs = null
        }

        items.addAll(fetchThreads(limit, selection, selectionArgs, state, excludeIds = items.map { it.threadId }.toSet()))

        return items.sortedWith(
            compareByDescending<SmsThreadItem> { it.isPinned }
                .thenByDescending { it.timestamp }
        )
    }

    fun listArchivedThreads(limit: Int = 50, state: InboxState): List<SmsThreadItem> {
        if (!hasReadPerms()) return emptyList()
        ensureObserversRegistered()
        val archivedIds = state.archivedThreadIds.filter { it > 0 }
        if (archivedIds.isEmpty()) return emptyList()

        return fetchSpecificThreads(archivedIds, state)
            .sortedByDescending { it.timestamp }
            .take(limit)
    }

    private fun fetchSpecificThreads(ids: List<Long>, state: InboxState): List<SmsThreadItem> {
        if (ids.isEmpty()) return emptyList()
        val chunks = ids.chunked(SQLITE_MAX_ARGS_SAFE)
        val items = mutableListOf<SmsThreadItem>()

        val projection = arrayOf(
            Telephony.Threads._ID,
            Telephony.Threads.SNIPPET,
            Telephony.Threads.DATE,
            Telephony.Threads.RECIPIENT_IDS,
            Telephony.Threads.READ
        )

        for (chunk in chunks) {
            val selection = "${Telephony.Threads._ID} IN (${chunk.joinToString { "?" }})"
            val selectionArgs = chunk.map { it.toString() }.toTypedArray()

            runCatching {
                context.contentResolver.query(Telephony.Threads.CONTENT_URI, projection, selection, selectionArgs, "${Telephony.Threads.DATE} DESC")
            }.getOrElse {
                android.util.Log.e("SmsRepository", "Error fetching specific threads", it)
                null
            }?.use { c ->
                val idIdx = c.getColumnIndexOrThrow(Telephony.Threads._ID)
                val snippetIdx = c.getColumnIndexOrThrow(Telephony.Threads.SNIPPET)
                val dateIdx = c.getColumnIndexOrThrow(Telephony.Threads.DATE)
                val readIdx = c.getColumnIndexOrThrow(Telephony.Threads.READ)
                while (c.moveToNext()) {
                    val threadId = c.getLong(idIdx)
                    val isArchived = state.archivedThreadIds.contains(threadId)
                    val isPinned = state.pinnedThreadIds.contains(threadId)

                    items += SmsThreadItem(
                        threadId = threadId,
                        address = resolveThreadAddress(threadId),
                        snippet = c.getString(snippetIdx) ?: "",
                        timestamp = c.getLong(dateIdx),
                        unread = c.getInt(readIdx) == 0,
                        isPinned = isPinned,
                        isArchived = isArchived
                    )
                }
            }
        }
        return items
    }

    private fun fetchThreads(limit: Int, selection: String?, selectionArgs: Array<String>?, state: InboxState, excludeIds: Set<Long>): List<SmsThreadItem> {
        val projection = arrayOf(
            Telephony.Threads._ID,
            Telephony.Threads.SNIPPET,
            Telephony.Threads.DATE,
            Telephony.Threads.RECIPIENT_IDS,
            Telephony.Threads.READ
        )
        val items = mutableListOf<SmsThreadItem>()

        runCatching {
            context.contentResolver.query(
                Telephony.Threads.CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                "${Telephony.Threads.DATE} DESC"
            )
        }.getOrElse {
            android.util.Log.e("SmsRepository", "Error fetching threads", it)
            null
        }?.use { c ->
            val idIdx = c.getColumnIndexOrThrow(Telephony.Threads._ID)
            val snippetIdx = c.getColumnIndexOrThrow(Telephony.Threads.SNIPPET)
            val dateIdx = c.getColumnIndexOrThrow(Telephony.Threads.DATE)
            val readIdx = c.getColumnIndexOrThrow(Telephony.Threads.READ)

            // Cap fetch
            var count = 0
            val fetchBuffer = if (selection == null) FETCH_BUFFER_UNFILTERED else limit + FETCH_BUFFER_MARGIN

            while (c.moveToNext() && count < fetchBuffer) {
                val threadId = c.getLong(idIdx)
                if (excludeIds.contains(threadId)) continue

                // Double check archive status if we couldn't filter in SQL
                val isArchived = state.archivedThreadIds.contains(threadId)
                if (selection == null && isArchived) continue

                val isPinned = state.pinnedThreadIds.contains(threadId)

                items += SmsThreadItem(
                    threadId = threadId,
                    address = resolveThreadAddress(threadId),
                    snippet = c.getString(snippetIdx) ?: "",
                    timestamp = c.getLong(dateIdx),
                    unread = c.getInt(readIdx) == 0,
                    isPinned = isPinned,
                    isArchived = isArchived
                )
                count++
            }
        }
        return items
    }

    private fun resolveThreadAddress(threadId: Long): String {
        val cursor = runCatching {
            context.contentResolver.query(
                Telephony.Sms.CONTENT_URI,
                arrayOf(Telephony.Sms.ADDRESS),
                "${Telephony.Sms.THREAD_ID}=?",
                arrayOf(threadId.toString()),
                "${Telephony.Sms.DATE} DESC"
            )
        }.getOrNull() ?: return ""

        cursor.use { c ->
            if (c.moveToFirst()) {
                val addr = c.getString(c.getColumnIndexOrThrow(Telephony.Sms.ADDRESS))
                return resolveAddress(addr)
            }
        }
        return ""
    }

    private fun readMmsMessages(threadId: Long, limit: Int): List<SmsMessageItem> {
        if (!hasReadPerms()) return emptyList()
        val projection = arrayOf(
            Telephony.Mms._ID,
            Telephony.Mms.THREAD_ID,
            Telephony.Mms.DATE,
            Telephony.Mms.MESSAGE_BOX
        )
        val cursor = runCatching {
            context.contentResolver.query(
                Telephony.Mms.CONTENT_URI,
                projection,
                "${Telephony.Mms.THREAD_ID}=?",
                arrayOf(threadId.toString()),
                "${Telephony.Mms.DATE} DESC"
            )
        }.getOrNull() ?: return emptyList()

        cursor.use { c ->
            val idIdx = c.getColumnIndexOrThrow(Telephony.Mms._ID)
            val dateIdx = c.getColumnIndexOrThrow(Telephony.Mms.DATE)
            val boxIdx = c.getColumnIndexOrThrow(Telephony.Mms.MESSAGE_BOX)
            val items = mutableListOf<SmsMessageItem>()
            var count = 0
            while (c.moveToNext() && count < limit) {
                val mmsId = c.getLong(idIdx)
                val address = resolveMmsAddress(mmsId)
                val parts = readMmsParts(mmsId)
                val textPart = parts.firstOrNull { it.text != null }?.text ?: "[MMS]"
                val ts = c.getLong(dateIdx) * 1000 // Mms dates are in seconds
                val msgBox = c.getInt(boxIdx)
                val outgoing = msgBox == Telephony.Mms.MESSAGE_BOX_SENT || msgBox == Telephony.Mms.MESSAGE_BOX_OUTBOX
                items += SmsMessageItem(
                    id = -mmsId, // avoid collision with SMS ids
                    threadId = threadId,
                    address = address,
                    body = textPart,
                    timestamp = ts,
                    outgoing = outgoing,
                    isMms = true,
                    mediaParts = parts
                )
                count++
            }
            return items
        }
    }

    private fun resolveMmsAddress(mmsId: Long): String {
        if (!hasReadPerms()) return ""
        val uri = Uri.parse("content://mms/$mmsId/addr")
        val cursor = runCatching {
            context.contentResolver.query(
                uri,
                arrayOf("address", "type"),
                "type=137", // FROM
                null,
                null
            )
        }.getOrNull() ?: return ""
        cursor.use { c ->
            if (c.moveToFirst()) {
                val addr = c.getString(c.getColumnIndexOrThrow("address"))
                return resolveAddress(addr)
            }
        }
        return ""
    }

    private fun readMmsParts(mmsId: Long): List<MmsPart> {
        if (!hasReadPerms()) return emptyList()
        val uri = Uri.parse("content://mms/$mmsId/part")
        val cursor = runCatching {
            context.contentResolver.query(
                uri,
                arrayOf("_id", "ct", "text"),
                null,
                null,
                null
            )
        }.getOrNull() ?: return emptyList()

        val parts = mutableListOf<MmsPart>()
        cursor.use { c ->
            val idIdx = c.getColumnIndexOrThrow("_id")
            val ctIdx = c.getColumnIndexOrThrow("ct")
            val textIdx = c.getColumnIndexOrThrow("text")
            while (c.moveToNext()) {
                val partId = c.getString(idIdx)
                val contentType = c.getString(ctIdx) ?: ""
                val text = c.getString(textIdx)
                val dataUri = Uri.parse("content://mms/part/$partId")
                parts += MmsPart(
                    contentType = contentType,
                    text = text,
                    dataUri = if (text == null) dataUri else null
                )
            }
        }
        return parts
    }

    fun messagesForThread(threadId: Long, limit: Int = 200): List<SmsMessageItem> {
        val projection = arrayOf(
            Telephony.Sms._ID,
            Telephony.Sms.THREAD_ID,
            Telephony.Sms.ADDRESS,
            Telephony.Sms.BODY,
            Telephony.Sms.DATE,
            Telephony.Sms.TYPE
        )
        if (!hasReadPerms()) return emptyList()
        ensureObserversRegistered()
        val smsCursor = runCatching {
            context.contentResolver.query(
                Telephony.Sms.CONTENT_URI,
                projection,
                "${Telephony.Sms.THREAD_ID}=?",
                arrayOf(threadId.toString()),
                "${Telephony.Sms.DATE} DESC"
            )
        }.getOrNull()

        val smsItems = smsCursor?.use { c ->
            val idIdx = c.getColumnIndexOrThrow(Telephony.Sms._ID)
            val threadIdx = c.getColumnIndexOrThrow(Telephony.Sms.THREAD_ID)
            val addrIdx = c.getColumnIndexOrThrow(Telephony.Sms.ADDRESS)
            val bodyIdx = c.getColumnIndexOrThrow(Telephony.Sms.BODY)
            val dateIdx = c.getColumnIndexOrThrow(Telephony.Sms.DATE)
            val typeIdx = c.getColumnIndexOrThrow(Telephony.Sms.TYPE)
            val items = mutableListOf<SmsMessageItem>()
            var count = 0
            while (c.moveToNext() && count < limit) {
                val id = c.getLong(idIdx)
                val addr = resolveAddress(c.getString(addrIdx))
                val body = c.getString(bodyIdx) ?: ""
                val ts = c.getLong(dateIdx)
                val type = c.getInt(typeIdx)
                val outgoing = type == Telephony.Sms.MESSAGE_TYPE_SENT || type == Telephony.Sms.MESSAGE_TYPE_OUTBOX
                items += SmsMessageItem(
                    id = id,
                    threadId = c.getLong(threadIdx),
                    address = addr,
                    body = body,
                    timestamp = ts,
                    outgoing = outgoing,
                    isMms = false
                )
                count++
            }
            items
        } ?: emptyList()

        val mmsItems = readMmsMessages(threadId, limit)

        return (smsItems + mmsItems).sortedBy { it.timestamp }.take(limit)
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
            Telephony.Sms.TYPE
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
            val hits = mutableListOf<SmsMessageItem>()
            var count = 0
            while (c.moveToNext() && count < limit) {
                val outgoing = c.getInt(typeIdx) == Telephony.Sms.MESSAGE_TYPE_SENT || c.getInt(typeIdx) == Telephony.Sms.MESSAGE_TYPE_OUTBOX
                hits += SmsMessageItem(
                    id = c.getLong(idIdx),
                    threadId = c.getLong(threadIdx),
                    address = resolveAddress(c.getString(addrIdx)),
                    body = c.getString(bodyIdx) ?: "",
                    timestamp = c.getLong(dateIdx),
                    outgoing = outgoing
                )
                count++
            }
            return hits
        }
    }

    fun sendSms(address: String, body: String): Boolean {
        if (!hasSendPerms()) return false
        return runCatching {
            val smsManager = android.telephony.SmsManager.getDefault()
            smsManager.sendTextMessage(address, null, body, null, null)
            true
        }.getOrDefault(false)
    }

    fun purgeExpiredOneTimeCodes(expiryMillis: Long = 10 * 60 * 1000L) {
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
            while (c.moveToNext() && staleIds.size < 80) {
                val addr = c.getString(addrIdx) ?: ""
                val body = c.getString(bodyIdx) ?: ""
                val shortCode = addr.length <= 8
                val looksOtp = shortCode && (body.contains("code", true) || body.contains("login", true) || body.contains("otp", true)) && otpRegex.containsMatchIn(body)
                if (looksOtp) {
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
            observerFlow.tryEmit(Unit)
        }
    }

    fun markThreadRead(threadId: Long) {
        if (!hasWritePerms()) return
        val values = android.content.ContentValues().apply {
            put(Telephony.Sms.READ, 1)
            put(Telephony.Sms.SEEN, 1)
        }
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
        observerFlow.tryEmit(Unit)
    }

    fun deleteThread(threadId: Long) {
        if (!hasWritePerms()) return
        context.contentResolver.delete(
            Telephony.Sms.CONTENT_URI,
            "${Telephony.Sms.THREAD_ID}=?",
            arrayOf(threadId.toString())
        )
        observerFlow.tryEmit(Unit)
    }

    private fun resolveAddress(raw: String?): String {
        if (!hasReadPerms()) return raw.orEmpty()
        val number = raw?.trim().orEmpty()
        if (number.isBlank()) return ""
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
                return if (name.isNotBlank()) "$name \u2022 $formatted" else formatted
            }
        }
        return number
    }

    companion object {
        private const val SQLITE_MAX_ARGS_SAFE = 900
        private const val FETCH_BUFFER_UNFILTERED = 500
        private const val FETCH_BUFFER_MARGIN = 20

        private fun isDefaultSmsApp(context: Context): Boolean {
            val roleHeld = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                context.getSystemService(RoleManager::class.java)
                    ?.isRoleHeld(RoleManager.ROLE_SMS) == true
            } else {
                false
            }
            val telephonyDefault = Telephony.Sms.getDefaultSmsPackage(context) == context.packageName
            return roleHeld || telephonyDefault
        }

        private fun hasReadSmsPermission(context: Context): Boolean {
            return ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.READ_SMS
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        }

        private fun hasSendSmsPermission(context: Context): Boolean {
            return ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.SEND_SMS
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        }
    }

    private fun hasReadPerms(): Boolean = isDefaultSmsApp(context) || hasReadSmsPermission(context)
    private fun hasSendPerms(): Boolean = isDefaultSmsApp(context) || hasSendSmsPermission(context)
    private fun hasWritePerms(): Boolean = isDefaultSmsApp(context)

    private fun ensureObserversRegistered() {
        if (observersRegistered || !hasReadPerms()) return
        val handler = Handler(Looper.getMainLooper())
        val observer = object : ContentObserver(handler) {
            override fun onChange(selfChange: Boolean) {
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

    private fun listThreadsFromSms(limit: Int): List<SmsThreadItem> {
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

        val items = mutableListOf<SmsThreadItem>()
        val seenThreads = HashSet<Long>()
        cursor.use { c ->
            val threadIdx = c.getColumnIndexOrThrow(Telephony.Sms.THREAD_ID)
            val addrIdx = c.getColumnIndexOrThrow(Telephony.Sms.ADDRESS)
            val bodyIdx = c.getColumnIndexOrThrow(Telephony.Sms.BODY)
            val dateIdx = c.getColumnIndexOrThrow(Telephony.Sms.DATE)
            val readIdx = c.getColumnIndexOrThrow(Telephony.Sms.READ)
            // No strict limit in loop, we filter and sort later
            while (c.moveToNext() && items.size < limit * 2) {
                val threadId = c.getLong(threadIdx)
                if (!seenThreads.add(threadId)) continue
                val body = c.getString(bodyIdx) ?: ""
                val ts = c.getLong(dateIdx)
                val unread = c.getInt(readIdx) == 0
                val address = resolveAddress(c.getString(addrIdx))
                items += SmsThreadItem(
                    threadId = threadId,
                    address = address,
                    snippet = body,
                    timestamp = ts,
                    unread = unread
                )
            }
        }
        return items
    }
}

