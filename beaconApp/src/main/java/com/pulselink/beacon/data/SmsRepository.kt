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
import android.util.LruCache
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.jvm.Volatile

class SmsRepository(private val context: Context) {

    @Volatile private var observersRegistered = false
    private val observerFlow = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    private val otpRegex = Regex("\\b\\d{4,8}\\b")

    private val addressCache = LruCache<Long, String>(ADDRESS_CACHE_SIZE)
    private val contactCache = LruCache<String, String>(CONTACT_CACHE_SIZE)

    init {
        ensureObserversRegistered()
        CoroutineScope(Dispatchers.IO).launch {
            purgeExpiredOneTimeCodes()
        }
    }

    fun changes(): SharedFlow<Unit> = observerFlow.asSharedFlow()

    suspend fun listThreads(limit: Int = 100): List<SmsThreadItem> = withContext(Dispatchers.IO) {
        if (!hasReadPerms()) return@withContext emptyList()
        ensureObserversRegistered()
        val projection = arrayOf(
            Telephony.Threads._ID,
            Telephony.Threads.SNIPPET,
            Telephony.Threads.DATE,
            Telephony.Threads.RECIPIENT_IDS,
            Telephony.Threads.READ
        )
        val cursor = runCatching {
            context.contentResolver.query(
                Telephony.Threads.CONTENT_URI,
                projection,
                null,
                null,
                "${Telephony.Threads.DATE} DESC"
            )
        }.getOrNull() ?: return@withContext listThreadsFromSms(limit)

        cursor.use { c ->
            val idIdx = c.getColumnIndexOrThrow(Telephony.Threads._ID)
            val snippetIdx = c.getColumnIndexOrThrow(Telephony.Threads.SNIPPET)
            val dateIdx = c.getColumnIndexOrThrow(Telephony.Threads.DATE)
            val readIdx = c.getColumnIndexOrThrow(Telephony.Threads.READ)
            val items = mutableListOf<SmsThreadItem>()
            var count = 0
            while (c.moveToNext() && count < limit) {
                val threadId = c.getLong(idIdx)
                val snippet = c.getString(snippetIdx) ?: ""
                val ts = c.getLong(dateIdx)
                val unread = c.getInt(readIdx) == 0
                val address = resolveThreadAddress(threadId)
                items += SmsThreadItem(
                    threadId = threadId,
                    address = address,
                    snippet = snippet,
                    timestamp = ts,
                    unread = unread
                )
                count++
            }
            if (items.isNotEmpty()) return@withContext items
        }
        return@withContext listThreadsFromSms(limit)
    }

    private fun resolveThreadAddress(threadId: Long): String {
        addressCache[threadId]?.let { return it }

        val cursor = runCatching {
            context.contentResolver.query(
                Telephony.Sms.CONTENT_URI,
                arrayOf(Telephony.Sms.ADDRESS),
                "${Telephony.Sms.THREAD_ID}=?",
                arrayOf(threadId.toString()),
                "${Telephony.Sms.DATE} DESC"
            )
        }.getOrNull() ?: return ""

        var result = ""
        cursor.use { c ->
            if (c.moveToFirst()) {
                val addr = c.getString(c.getColumnIndexOrThrow(Telephony.Sms.ADDRESS))
                result = resolveAddress(addr)
            }
        }
        if (result.isNotBlank()) {
            addressCache.put(threadId, result)
        }
        return result
    }

    private suspend fun readMmsMessages(threadId: Long, limit: Int): List<SmsMessageItem> = withContext(Dispatchers.IO) {
        if (!hasReadPerms()) return@withContext emptyList()
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
        }.getOrNull() ?: return@withContext emptyList()

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
            return@withContext items
        }
    }

    private suspend fun resolveMmsAddress(mmsId: Long): String = withContext(Dispatchers.IO) {
        if (!hasReadPerms()) return@withContext ""
        val uri = Uri.parse("content://mms/$mmsId/addr")
        val cursor = runCatching {
            context.contentResolver.query(
                uri,
                arrayOf("address", "type"),
                "type=137", // FROM
                null,
                null
            )
        }.getOrNull() ?: return@withContext ""
        cursor.use { c ->
            if (c.moveToFirst()) {
                val addr = c.getString(c.getColumnIndexOrThrow("address"))
                return@withContext resolveAddress(addr)
            }
        }
        return@withContext ""
    }

    private suspend fun readMmsParts(mmsId: Long): List<MmsPart> = withContext(Dispatchers.IO) {
        if (!hasReadPerms()) return@withContext emptyList()
        val uri = Uri.parse("content://mms/$mmsId/part")
        val cursor = runCatching {
            context.contentResolver.query(
                uri,
                arrayOf("_id", "ct", "text"),
                null,
                null,
                null
            )
        }.getOrNull() ?: return@withContext emptyList()

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
        return@withContext parts
    }

    suspend fun messagesForThread(threadId: Long, limit: Int = 200): List<SmsMessageItem> = withContext(Dispatchers.IO) {
        val projection = arrayOf(
            Telephony.Sms._ID,
            Telephony.Sms.THREAD_ID,
            Telephony.Sms.ADDRESS,
            Telephony.Sms.BODY,
            Telephony.Sms.DATE,
            Telephony.Sms.TYPE
        )
        if (!hasReadPerms()) return@withContext emptyList()
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

        // Return sorted Newest -> Oldest for reverseLayout
        return@withContext (smsItems + mmsItems).sortedByDescending { it.timestamp }.take(limit)
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

        contactCache[number]?.let { return it }

        val uri = ContactsContract.PhoneLookup.CONTENT_FILTER_URI
        val lookupUri = Uri.withAppendedPath(uri, Uri.encode(number))
        val result = runCatching {
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
                if (name.isNotBlank()) "$name \u2022 $formatted" else formatted
            } else {
                number
            }
        } ?: number

        contactCache.put(number, result)
        return result
    }

    companion object {
        private const val ADDRESS_CACHE_SIZE = 200
        private const val CONTACT_CACHE_SIZE = 500

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

    private suspend fun listThreadsFromSms(limit: Int): List<SmsThreadItem> {
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
            var count = 0
            while (c.moveToNext() && count < limit) {
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
                count++
            }
        }
        return items
    }
}
