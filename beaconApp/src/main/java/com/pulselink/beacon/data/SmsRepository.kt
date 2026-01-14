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
import java.util.regex.Pattern
import kotlin.jvm.Volatile

class SmsRepository(private val context: Context) {

    @Volatile private var observersRegistered = false
    private val observerFlow = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    private val otpRegex = Regex("\\b\\d{4,8}\\b")

    companion object {
        private const val ADDRESS_CACHE_SIZE = 1000 // Increased cache
        private const val CONTACT_CACHE_SIZE = 2000 // Increased cache

        // Static caches to survive ViewModel recreation
        // These hold only Strings, which are safe from Context leaks.
        private val addressCache = LruCache<Long, String>(ADDRESS_CACHE_SIZE)
        private val contactCache = LruCache<String, String>(CONTACT_CACHE_SIZE)

        // Clear caches on memory warning or low memory if needed
        fun clearCaches() {
            addressCache.evictAll()
            contactCache.evictAll()
        }

        // Classification Regex
        private val NUMERIC_REGEX = Regex("[^0-9]")
        private val TRANSACTION_KEYWORDS = listOf(
            "otp", "code", "bank", "debit", "credit", "acct", "txn", "verify",
            "password", "login", "auth", "bill", "invoice", "due", "paid"
        )
        private val PROMOTION_KEYWORDS = listOf(
            "offer", "sale", "save", "discount", "buy", "deal", "coupon",
            "% off", "limited time", "cashback", "flat"
        )
        private val TRANSACTION_PATTERN = Pattern.compile(
            "\\b(${TRANSACTION_KEYWORDS.joinToString("|")})\\b",
            Pattern.CASE_INSENSITIVE
        )
        private val PROMOTION_PATTERN = Pattern.compile(
            "\\b(${PROMOTION_KEYWORDS.joinToString("|")})\\b",
            Pattern.CASE_INSENSITIVE
        )

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
        // Optimized Projection
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

        val rawThreads = mutableListOf<RawThreadData>()
        cursor.use { c ->
            val idIdx = c.getColumnIndexOrThrow(Telephony.Threads._ID)
            val snippetIdx = c.getColumnIndexOrThrow(Telephony.Threads.SNIPPET)
            val dateIdx = c.getColumnIndexOrThrow(Telephony.Threads.DATE)
            val readIdx = c.getColumnIndexOrThrow(Telephony.Threads.READ)
            val recipIdx = c.getColumnIndexOrThrow(Telephony.Threads.RECIPIENT_IDS)
            var count = 0
            while (c.moveToNext() && count < limit) {
                val threadId = c.getLong(idIdx)
                var snippet = c.getString(snippetIdx) ?: ""

                // Optimization: Fallback logic removed to prevent N+1 queries.
                // Blank snippets are handled in UI (e.g. "Media" or hidden) to prioritize load speed.

                rawThreads.add(RawThreadData(
                    id = threadId,
                    snippet = snippet,
                    timestamp = c.getLong(dateIdx),
                    unread = c.getInt(readIdx) == 0,
                    recipientIds = c.getString(recipIdx) ?: ""
                ))
                count++
            }
        }

        if (rawThreads.isEmpty()) return@withContext listThreadsFromSms(limit)

        // Bulk resolve
        val addressMap = resolveAddressesForThreads(rawThreads)

        return@withContext rawThreads.map { raw ->
            val finalName = addressMap[raw.id] ?: "Unknown"
            SmsThreadItem(
                threadId = raw.id,
                address = finalName,
                snippet = raw.snippet,
                timestamp = raw.timestamp,
                unread = raw.unread,
                category = classifyThread(finalName, raw.snippet)
            )
        }
    }

    private data class RawThreadData(
        val id: Long,
        val snippet: String,
        val timestamp: Long,
        val unread: Boolean,
        val recipientIds: String
    )

    private fun classifyThread(address: String, snippet: String): ThreadCategory {
        val body = snippet.lowercase()
        val hasSpace = address.contains(" ")

        val hasLetters = address.any { it.isLetter() }
        val isContactName = hasSpace && hasLetters
        // Check if it's a raw phone number (digits and common punctuation only)
        val isNumericNumber = !hasLetters && address.any { it.isDigit() } && address.length >= 3

        if (isContactName || isNumericNumber) {
            return ThreadCategory.PERSONAL
        }

        if (TRANSACTION_PATTERN.matcher(body).find()) return ThreadCategory.TRANSACTIONS
        if (PROMOTION_PATTERN.matcher(body).find()) return ThreadCategory.PROMOTIONS

        return ThreadCategory.PERSONAL
    }

    private fun resolveAddressesForThreads(threads: List<RawThreadData>): Map<Long, String> {
        val result = mutableMapOf<Long, String>()
        val threadsToResolve = mutableListOf<RawThreadData>()

        // 1. Check cache first
        threads.forEach { thread ->
            val cached = addressCache[thread.id]
            if (cached != null) {
                result[thread.id] = cached
            } else {
                threadsToResolve.add(thread)
            }
        }

        if (threadsToResolve.isEmpty()) return result

        val threadToRecipients = mutableMapOf<Long, List<Long>>()
        val neededRecipients = mutableSetOf<Long>()

        threadsToResolve.forEach { thread ->
            val ids = thread.recipientIds.split(" ").mapNotNull { it.toLongOrNull() }
            if (ids.isNotEmpty()) {
                threadToRecipients[thread.id] = ids
                neededRecipients.addAll(ids)
            }
        }

        // 2. Resolve Recipient IDs to Raw Numbers (Canonical Addresses)
        val recipientIdToNumber = mutableMapOf<Long, String>()
        if (neededRecipients.isNotEmpty()) {
            // Increased chunk size for better speed
            neededRecipients.chunked(100).forEach { chunk ->
                val q = chunk.joinToString(",")
                val c = runCatching {
                    context.contentResolver.query(
                        Uri.parse("content://mms-sms/canonical-addresses"),
                        arrayOf("_id", "address"),
                        "_id IN ($q)",
                        null,
                        null
                    )
                }.getOrNull()
                c?.use {
                    while (it.moveToNext()) {
                        recipientIdToNumber[it.getLong(0)] = it.getString(1)
                    }
                }
            }
        }

        // 3. Resolve Numbers to Contact Names (Bulk)
        val numberToName = mutableMapOf<String, String>()
        val numbersToLookup = recipientIdToNumber.values.toSet().filter { contactCache[it] == null }

        if (numbersToLookup.isNotEmpty()) {
            // Chunk size 100 is safe (SQLite limit 900 params)
             numbersToLookup.chunked(100).forEach { batch ->
                 val cursor = runCatching {
                     context.contentResolver.query(
                         ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                         arrayOf(ContactsContract.CommonDataKinds.Phone.NUMBER, ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME),
                         "${ContactsContract.CommonDataKinds.Phone.NUMBER} IN (${batch.joinToString(",") { "?" }})",
                         batch.toTypedArray(),
                         null
                     )
                 }.getOrNull()

                 cursor?.use { c ->
                     val numIdx = c.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                     val nameIdx = c.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                     while (c.moveToNext()) {
                         val num = c.getString(numIdx) ?: continue
                         val name = c.getString(nameIdx) ?: continue
                         if (name.isNotBlank()) {
                             val formatted = if (name != num) "$name \u2022 $num" else num
                             numberToName[num] = formatted
                             // Normalize for matches
                             val norm = num.replace(" ", "").replace("-", "")
                             if (norm != num) numberToName[norm] = formatted
                         }
                     }
                 }
             }
        }

        // Assemble results
        threadsToResolve.forEach { thread ->
            val recips = threadToRecipients[thread.id]
            if (recips != null) {
                val names = recips.mapNotNull { rid ->
                    val rawNum = recipientIdToNumber[rid] ?: return@mapNotNull null
                    // Check cache
                    contactCache[rawNum]?.let { return@mapNotNull it }

                    // Check bulk result
                    val bulkName = numberToName[rawNum]
                        ?: numberToName[rawNum.replace(" ", "").replace("-", "")]

                    if (bulkName != null) {
                        contactCache.put(rawNum, bulkName)
                        return@mapNotNull bulkName
                    }

                    // Store raw number in cache if no contact found, to avoid repeated lookups
                    contactCache.put(rawNum, rawNum)
                    rawNum
                }
                val finalStr = names.joinToString(", ")
                addressCache.put(thread.id, finalStr)
                result[thread.id] = finalStr
            }
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

    private fun getLastMessageSnippet(threadId: Long): String {
        // Fast query for just the body of the latest message
        return runCatching {
            context.contentResolver.query(
                Telephony.Sms.CONTENT_URI,
                arrayOf(Telephony.Sms.BODY),
                "${Telephony.Sms.THREAD_ID}=?",
                arrayOf(threadId.toString()),
                "${Telephony.Sms.DATE} DESC LIMIT 1"
            )?.use { c ->
                if (c.moveToFirst()) c.getString(0) else ""
            }
        }.getOrNull() ?: ""
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

    suspend fun messagesForThread(threadId: Long, limit: Int = 300): List<SmsMessageItem> = withContext(Dispatchers.IO) {
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

            runCatching {
                SmsSyncManager.getInstance(context).syncOutgoing(address, body)
            }

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

    fun markThreadUnread(threadId: Long) {
        if (!hasWritePerms()) return
        val values = android.content.ContentValues().apply {
            put(Telephony.Sms.READ, 0)
            put(Telephony.Sms.SEEN, 0)
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

    fun markThreadsRead(threadIds: List<Long>) {
        if (!hasWritePerms() || threadIds.isEmpty()) return
        val values = android.content.ContentValues().apply {
            put(Telephony.Sms.READ, 1)
            put(Telephony.Sms.SEEN, 1)
        }
        threadIds.chunked(900).forEach { chunk ->
            val placeholders = chunk.joinToString(",") { "?" }
            val args = chunk.map { it.toString() }.toTypedArray()
            context.contentResolver.update(
                Telephony.Sms.CONTENT_URI,
                values,
                "${Telephony.Sms.THREAD_ID} IN ($placeholders)",
                args
            )
            context.contentResolver.update(
                Telephony.Threads.CONTENT_URI,
                values,
                "${Telephony.Threads._ID} IN ($placeholders)",
                args
            )
        }
        observerFlow.tryEmit(Unit)
    }

    fun markThreadsUnread(threadIds: List<Long>) {
        if (!hasWritePerms() || threadIds.isEmpty()) return
        val values = android.content.ContentValues().apply {
            put(Telephony.Sms.READ, 0)
            put(Telephony.Sms.SEEN, 0)
        }
        threadIds.chunked(900).forEach { chunk ->
            val placeholders = chunk.joinToString(",") { "?" }
            val args = chunk.map { it.toString() }.toTypedArray()
            context.contentResolver.update(
                Telephony.Sms.CONTENT_URI,
                values,
                "${Telephony.Sms.THREAD_ID} IN ($placeholders)",
                args
            )
            context.contentResolver.update(
                Telephony.Threads.CONTENT_URI,
                values,
                "${Telephony.Threads._ID} IN ($placeholders)",
                args
            )
        }
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

    fun deleteThreads(threadIds: List<Long>) {
        if (!hasWritePerms() || threadIds.isEmpty()) return
        threadIds.chunked(900).forEach { chunk ->
            val placeholders = chunk.joinToString(",") { "?" }
            val args = chunk.map { it.toString() }.toTypedArray()
            context.contentResolver.delete(
                Telephony.Sms.CONTENT_URI,
                "${Telephony.Sms.THREAD_ID} IN ($placeholders)",
                args
            )
        }
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

        val rawItems = mutableListOf<RawSmsThreadData>()
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

                rawItems.add(RawSmsThreadData(
                    threadId = threadId,
                    rawAddress = c.getString(addrIdx) ?: "",
                    body = c.getString(bodyIdx) ?: "",
                    timestamp = c.getLong(dateIdx),
                    unread = c.getInt(readIdx) == 0
                ))
                count++
            }
        }

        if (rawItems.isEmpty()) return emptyList()

        // Batch resolve addresses
        val resolvedMap = resolveAddressesForStrings(rawItems.map { it.rawAddress })

        return rawItems.map { raw ->
            val finalAddress = resolvedMap[raw.rawAddress] ?: raw.rawAddress
            SmsThreadItem(
                threadId = raw.threadId,
                address = finalAddress,
                snippet = raw.body,
                timestamp = raw.timestamp,
                unread = raw.unread,
                category = classifyThread(finalAddress, raw.body)
            )
        }
    }

    private data class RawSmsThreadData(
        val threadId: Long,
        val rawAddress: String,
        val body: String,
        val timestamp: Long,
        val unread: Boolean
    )

    private fun resolveAddressesForStrings(rawAddresses: List<String>): Map<String, String> {
        val result = mutableMapOf<String, String>()
        val toLookup = mutableSetOf<String>()

        // 1. Check Cache
        rawAddresses.forEach { raw ->
            if (raw.isBlank()) return@forEach
            val cached = contactCache[raw]
            if (cached != null) {
                result[raw] = cached
            } else {
                toLookup.add(raw)
            }
        }

        if (toLookup.isEmpty()) return result

        // 2. Batch Lookup
        toLookup.chunked(100).forEach { batch ->
            // Sanitize batch for SQL (single quotes escaping not strictly needed with placeholders but good to be careful)
            // ContactsContract.CommonDataKinds.Phone.NUMBER lookup
            val cursor = runCatching {
                context.contentResolver.query(
                    ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                    arrayOf(ContactsContract.CommonDataKinds.Phone.NUMBER, ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME),
                    "${ContactsContract.CommonDataKinds.Phone.NUMBER} IN (${batch.joinToString(",") { "?" }})",
                    batch.toTypedArray(),
                    null
                )
            }.getOrNull()

            cursor?.use { c ->
                val numIdx = c.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                val nameIdx = c.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                while (c.moveToNext()) {
                    val num = c.getString(numIdx) ?: continue
                    val name = c.getString(nameIdx) ?: continue
                    if (name.isNotBlank()) {
                        val formatted = if (name != num) "$name \u2022 $num" else num

                        // Try to match back to our lookup set (normalization might be needed)
                        // Simple exact match first
                        if (toLookup.contains(num)) {
                            result[num] = formatted
                            contactCache.put(num, formatted)
                        }

                        // Try stripped match
                        val stripped = num.replace(" ", "").replace("-", "")
                        batch.forEach { pending ->
                             if (pending.replace(" ", "").replace("-", "") == stripped) {
                                 result[pending] = formatted
                                 contactCache.put(pending, formatted)
                             }
                        }
                    }
                }
            }
        }

        // Fill remaining with raw
        toLookup.forEach { raw ->
            if (!result.containsKey(raw)) {
                contactCache.put(raw, raw)
                result[raw] = raw
            }
        }

        return result
    }
}
