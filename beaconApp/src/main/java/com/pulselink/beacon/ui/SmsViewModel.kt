package com.pulselink.beacon.ui

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.pulselink.beacon.data.SmsMessageItem
import com.pulselink.beacon.data.SmsRepository
import com.pulselink.beacon.data.SmsThreadItem
import com.pulselink.beacon.data.InboxPreferencesRepository
import com.pulselink.beacon.data.InboxState
import com.pulselink.beacon.data.scheduled.BeaconDatabase
import com.pulselink.beacon.data.scheduled.ScheduledMessage
import com.pulselink.beacon.data.scheduled.MessageReaction
import com.pulselink.beacon.data.scheduled.MessageStar
import com.pulselink.beacon.data.scheduled.BlockedNumber
import com.pulselink.beacon.data.scheduled.MessageStatus as ScheduledStatus
import com.pulselink.beacon.data.MessageStatus
import com.pulselink.beacon.data.scheduled.ThreadDraft
import com.pulselink.beacon.worker.ScheduledMessageWorker
import com.pulselink.beacon.util.ThreadDateUtils
import com.pulselink.beacon.BuildConfig
import com.pulselink.beacon.data.BeaconContact
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit
import kotlin.math.abs

sealed class SearchResultState {
    object Idle : SearchResultState()
    object Searching : SearchResultState()
    data class Contact(val threadId: Long, val address: String) : SearchResultState()
    data class Messages(val hits: List<SmsMessageItem>) : SearchResultState()
    object Empty : SearchResultState()
}

class SmsViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = SmsRepository(app.applicationContext)
    private val inboxPrefs = InboxPreferencesRepository(app.applicationContext)
    private val scheduledDao = BeaconDatabase.getDatabase(app).scheduledMessageDao()
    private val reactionDao = BeaconDatabase.getDatabase(app).reactionDao()
    private val starDao = BeaconDatabase.getDatabase(app).messageStarDao()
    private val blockedDao = BeaconDatabase.getDatabase(app).blockedNumberDao()
    private val draftDao = BeaconDatabase.getDatabase(app).threadDraftDao()
    private val workManager = WorkManager.getInstance(app)

    private companion object {
        const val THREAD_LIMIT = 100
        const val MESSAGE_LIMIT = 300
    }

    var threads by mutableStateOf<List<SmsThreadItem>>(emptyList())
        private set

    var contacts by mutableStateOf<List<BeaconContact>>(emptyList())
        private set
    var filteredContacts by mutableStateOf<List<BeaconContact>>(emptyList())
        private set

    // Grouped threads for UI (Map<Header, List<Thread>>)
    var filteredGroupedThreads by mutableStateOf<Map<String, List<SmsThreadItem>>>(emptyMap())
        private set

    // Changed to hold UiItems for display
    var uiMessages by mutableStateOf<List<ThreadUiItem>>(emptyList())
        private set

    var reactions by mutableStateOf<Map<Long, List<MessageReaction>>>(emptyMap())
        private set

    var starredMessageIds by mutableStateOf<Set<Long>>(emptySet())
        private set

    // Store threads that have starred messages for filtering
    private var threadsWithStars: Set<Long> = emptySet()

    private var blockedNumbers: Set<String> = emptySet()
    var blockedNumbersList by mutableStateOf<List<BlockedNumber>>(emptyList())
        private set

    var scheduledMessages by mutableStateOf<List<ScheduledMessage>>(emptyList())
        private set

    var draftsMap by mutableStateOf<Map<Long, ThreadDraft>>(emptyMap())
        private set

    var isDraftsLoaded by mutableStateOf(false)
        private set

    // Keep raw messages for internal logic
    private var rawMessages = emptyList<SmsMessageItem>()

    // Pending messages (Optimistic UI)
    private val pendingOutgoingMessages = mutableListOf<SmsMessageItem>()

    var currentThreadId by mutableStateOf<Long?>(null)
        private set
    var currentAddress by mutableStateOf("")
        private set
    var searchState: SearchResultState by mutableStateOf(SearchResultState.Idle)
        private set
    var isLoading by mutableStateOf(true)
        private set
    var isRefreshing by mutableStateOf(false)
        private set

    var selectionMode by mutableStateOf(false)
        private set
    var selectedThreadIds by mutableStateOf(setOf<Long>())
        private set

    var userMessage by mutableStateOf<String?>(null)
        private set

    // Delayed Send
    var pendingMessage by mutableStateOf<PendingMessage?>(null)
        private set

    data class PendingMessage(
        val body: String,
        val timestamp: Long,
        val targetTime: Long
    )

    private var delayedSendJob: Job? = null
    private var reactionJob: Job? = null

    // Internal holder for raw threads before merging preferences
    private var rawThreads: List<SmsThreadItem> = emptyList()
    private var inboxState: InboxState = InboxState()

    val delayedSendTimeout: Int get() = inboxState.delayedSendTimeout
    val autoReplyEnabled: Boolean get() = inboxState.autoReplyEnabled
    val autoReplyMessage: String get() = inboxState.autoReplyMessage
    val quickReplies: List<String> get() = inboxState.quickReplies
    val autoDeleteOtps: Boolean get() = inboxState.autoDeleteOtps

    // Filtered state
    var filteredThreads by mutableStateOf<List<SmsThreadItem>>(emptyList())
        private set
    var currentFilter by mutableStateOf(InboxFilter.ALL)
        private set
    var currentSearchText by mutableStateOf("")
        private set

    private var searchJob: Job? = null

    init {
        refreshThreads(initial = true)

        viewModelScope.launch {
            repo.changes().collectLatest {
                // Use suspend functions to ensure cancellation if new updates arrive
                // This prevents stacking parallel DB queries during rapid updates
                isRefreshing = true
                val newThreads = fetchThreadsSuspend()
                rawThreads = newThreads
                mergeThreads()
                isRefreshing = false

                val threadId = currentThreadId
                if (threadId != null) {
                    refreshThreadSuspend(threadId, refreshRead = false)
                }
            }
        }

        viewModelScope.launch {
            inboxPrefs.flow.collectLatest { state ->
                inboxState = state
                mergeThreads()
            }
        }

        viewModelScope.launch {
            starDao.getAllStarred().collectLatest { stars ->
                starredMessageIds = stars.map { it.messageId }.toSet()
                threadsWithStars = stars.map { it.threadId }.toSet()
                // Refresh list if filter is STARRED
                if (currentFilter == InboxFilter.STARRED) updateFilteredList()
            }
        }

        viewModelScope.launch {
            blockedDao.getAllBlocked().collectLatest { blocked ->
                blockedNumbersList = blocked
                blockedNumbers = blocked.map { it.normalizedNumber }.toSet()
                mergeThreads() // Re-filter blocked threads
            }
        }

        viewModelScope.launch {
            scheduledDao.getAllPending().collectLatest { scheduled ->
                scheduledMessages = scheduled
            }
        }

        viewModelScope.launch {
            draftDao.getAllDrafts().collectLatest { drafts ->
                draftsMap = drafts.associateBy { it.threadId }
                isDraftsLoaded = true
                mergeThreads()
            }
        }
    }

    fun updateFilter(filter: InboxFilter) {
        currentFilter = filter
        if (filter == InboxFilter.CONTACTS && contacts.isEmpty()) {
            loadContacts()
        }
        updateFilteredList()
    }

    fun loadContacts() {
        viewModelScope.launch {
            contacts = repo.getContacts()
            updateFilteredList()
        }
    }

    fun updateSearchText(text: String) {
        currentSearchText = text
        updateFilteredList()
        search(text) // Trigger full search logic too
    }

    fun refreshThreads(initial: Boolean = false) {
        viewModelScope.launch {
            if (initial) isLoading = true else isRefreshing = true
            val newThreads = fetchThreadsSuspend()
            rawThreads = newThreads
            mergeThreads()
            if (initial) isLoading = false else isRefreshing = false
        }
    }

    private suspend fun fetchThreadsSuspend(): List<SmsThreadItem> {
        return runCatching { repo.listThreads(limit = THREAD_LIMIT) }.getOrElse { emptyList() }
    }

    private fun mergeThreads() {
        viewModelScope.launch(Dispatchers.Default) {
            // Optimized sorting and filtering blocked
            val merged = rawThreads.asSequence()
                .filter { !blockedNumbers.contains(it.address) }
                .map { thread ->
                    val isPinned = inboxState.pinnedThreadIds.contains(thread.threadId)
                    val isArchived = inboxState.archivedThreadIds.contains(thread.threadId)
                    val draft = draftsMap[thread.threadId]

                    // Calculate effective timestamp (max of thread or draft)
                    // If we have a draft that is newer than the thread timestamp, use it.
                    val effectiveTimestamp = if (draft != null && draft.timestamp > thread.timestamp) {
                        draft.timestamp
                    } else {
                        thread.timestamp
                    }

                    // Always create copy if draft exists or other state changed
                    if (isPinned != thread.isPinned ||
                        isArchived != thread.isArchived ||
                        draft?.body != thread.draftSnippet ||
                        effectiveTimestamp != thread.timestamp
                    ) {
                        thread.copy(
                            isPinned = isPinned,
                            isArchived = isArchived,
                            draftSnippet = draft?.body,
                            timestamp = effectiveTimestamp
                        )
                    } else {
                        thread
                    }
                }
                .sortedWith(
                    compareByDescending<SmsThreadItem> { it.isPinned }
                        .thenByDescending { it.timestamp }
                )
                .toList()

            withContext(Dispatchers.Main) {
                threads = merged
            }
            updateFilteredList()
        }
    }

    private fun updateFilteredList() {
        viewModelScope.launch(Dispatchers.Default) {
            val list = threads
            val filter = currentFilter
            val search = currentSearchText

            val result = if (search.isNotBlank()) {
                // If searching, show all non-archived matching items in the main list
                list.filter {
                    !it.isArchived && (it.address.contains(search, true) || it.snippet.contains(search, true))
                }
            } else {
                 when (filter) {
                    InboxFilter.ALL -> list.filter { !it.isArchived }
                    InboxFilter.READ -> list.filter { !it.unread && !it.isArchived }
                    InboxFilter.UNREAD -> list.filter { it.unread && !it.isArchived }
                    InboxFilter.STARRED -> list.filter { threadsWithStars.contains(it.threadId) && !it.isArchived }
                    InboxFilter.PERSONAL -> list.filter { it.category == ThreadCategory.PERSONAL && !it.isArchived }
                    InboxFilter.TRANSACTIONS -> list.filter { it.category == ThreadCategory.TRANSACTIONS && !it.isArchived }
                    InboxFilter.PROMOTIONS -> list.filter { it.category == ThreadCategory.PROMOTIONS && !it.isArchived }
                    InboxFilter.ARCHIVED -> list.filter { it.isArchived }
                    InboxFilter.CONTACTS -> emptyList()
                }
            }

            // Calculate groups
            val now = System.currentTimeMillis()
            val calendar = java.util.Calendar.getInstance()
            calendar.timeInMillis = now
            calendar.set(java.util.Calendar.HOUR_OF_DAY, 0)
            calendar.set(java.util.Calendar.MINUTE, 0)
            calendar.set(java.util.Calendar.SECOND, 0)
            calendar.set(java.util.Calendar.MILLISECOND, 0)
            val startOfToday = calendar.timeInMillis

            calendar.add(java.util.Calendar.DAY_OF_YEAR, -1)
            val startOfYesterday = calendar.timeInMillis

            val groups = result.groupBy { item ->
                val t = item.timestamp
                when {
                    t >= startOfToday -> "Today"
                    t >= startOfYesterday -> "Yesterday"
                    now - t < 7 * android.text.format.DateUtils.DAY_IN_MILLIS -> "This Week"
                    now - t < 30 * android.text.format.DateUtils.DAY_IN_MILLIS -> "This Month"
                    else -> "Older"
                }
            }

            val currentContacts = contacts
            val fContacts = if (search.isNotBlank()) {
                currentContacts.filter {
                    it.displayName.contains(search, true) || it.phoneNumber.contains(search)
                }
            } else {
                currentContacts
            }

            withContext(Dispatchers.Main) {
                filteredThreads = result
                filteredGroupedThreads = groups
                filteredContacts = fContacts
            }
        }
    }

    fun togglePin(threadId: Long) {
        viewModelScope.launch {
            inboxPrefs.togglePin(threadId)
        }
    }

    fun toggleStar(messageId: Long, threadId: Long) {
        viewModelScope.launch {
            if (starredMessageIds.contains(messageId)) {
                starDao.remove(messageId)
            } else {
                starDao.insert(MessageStar(messageId = messageId, threadId = threadId))
            }
        }
    }

    fun blockNumber(address: String) {
        viewModelScope.launch {
            blockedDao.block(BlockedNumber(normalizedNumber = address, originalNumber = address))
        }
    }

    fun unblockNumber(address: String) {
        viewModelScope.launch {
            blockedDao.unblock(address)
        }
    }

    fun toggleArchive(threadId: Long) {
        viewModelScope.launch {
            inboxPrefs.toggleArchive(threadId)
        }
    }

    fun markAsUnread(threadId: Long) {
        viewModelScope.launch {
            repo.markThreadUnread(threadId)
        }
    }

    fun toggleSelection(threadId: Long) {
        val current = selectedThreadIds
        if (current.contains(threadId)) {
            selectedThreadIds = current - threadId
            if (selectedThreadIds.isEmpty()) {
                selectionMode = false
            }
        } else {
            selectedThreadIds = current + threadId
            selectionMode = true
        }
    }

    fun clearSelection() {
        selectedThreadIds = emptySet()
        selectionMode = false
    }

    fun archiveSelected() {
        val ids = selectedThreadIds.toList()
        viewModelScope.launch {
            val currentThreads = threads
            val toArchive = currentThreads.filter { it.threadId in ids && !it.isArchived }.map { it.threadId }
            toArchive.forEach { inboxPrefs.toggleArchive(it) }

            userMessage = "${toArchive.size} threads archived"
            clearSelection()
        }
    }

    fun markAllRead() {
        viewModelScope.launch {
            val unreadIds = threads.filter { it.unread }.map { it.threadId }
            if (unreadIds.isNotEmpty()) {
                repo.markThreadsRead(unreadIds)
                userMessage = "Marked ${unreadIds.size} threads as read"
            }
        }
    }

    fun pinSelected() {
        val ids = selectedThreadIds.toList()
        viewModelScope.launch {
            val currentThreads = threads
            val toPin = currentThreads.filter { it.threadId in ids && !it.isPinned }.map { it.threadId }
            toPin.forEach { inboxPrefs.togglePin(it) }

            userMessage = "${toPin.size} threads pinned"
            clearSelection()
        }
    }

    fun deleteSelected() {
        val ids = selectedThreadIds.toList()
        viewModelScope.launch {
            repo.deleteThreads(ids)
            userMessage = "${ids.size} threads deleted"
            clearSelection()
            refreshThreads()
        }
    }

    fun markSelectedRead() {
        val ids = selectedThreadIds.toList()
        viewModelScope.launch {
            repo.markThreadsRead(ids)
            userMessage = "Marked ${ids.size} threads as read"
            clearSelection()
        }
    }

    fun markSelectedUnread() {
        val ids = selectedThreadIds.toList()
        viewModelScope.launch {
            repo.markThreadsUnread(ids)
            userMessage = "Marked ${ids.size} threads as unread"
            clearSelection()
        }
    }

    fun clearUserMessage() {
        userMessage = null
    }

    fun openThread(threadId: Long, address: String) {
        if (threadId == 0L && !address.isNullOrBlank()) {
            currentThreadId = 0L
            currentAddress = address
            rawMessages = emptyList()
            uiMessages = emptyList()
            return
        }

        currentThreadId = threadId
        currentAddress = address
        refreshThread(threadId, refreshRead = true)
    }

    private fun refreshThread(threadId: Long, refreshRead: Boolean) {
        viewModelScope.launch {
            refreshThreadSuspend(threadId, refreshRead)
        }
    }

    private suspend fun refreshThreadSuspend(threadId: Long, refreshRead: Boolean) {
        val dbMessages = runCatching { repo.messagesForThread(threadId, limit = MESSAGE_LIMIT) }
            .getOrElse { emptyList() }

        // Deduplicate pending messages: remove if found in DB
        pendingOutgoingMessages.removeAll { pending ->
            // Check if any DB message matches (body + similar timestamp)
            dbMessages.any { db ->
                db.outgoing && db.body == pending.body && abs(db.timestamp - pending.timestamp) < 10000
            }
        }

        // Merge pending messages for this thread
        val pendingForThread = pendingOutgoingMessages.filter {
            if (threadId == 0L) it.address == currentAddress else it.threadId == threadId
        }

        rawMessages = (pendingForThread + dbMessages).sortedByDescending { it.timestamp }

        // Transform for UI (Group by Date)
        uiMessages = ThreadDateUtils.mapMessagesToUi(rawMessages)

        // Load reactions
        reactionJob?.cancel()
        val ids = rawMessages.map { it.id }
        if (ids.isNotEmpty()) {
            reactionJob = viewModelScope.launch {
                reactionDao.getReactionsForMessages(ids).collectLatest { list ->
                    reactions = list.groupBy { it.messageId }
                }
            }
        } else {
            reactions = emptyMap()
        }

        if (refreshRead) runCatching { repo.markThreadRead(threadId) }
    }

    fun addReaction(messageId: Long, emoji: String) {
        viewModelScope.launch(Dispatchers.IO) {
            // Toggle local reaction
            val current = reactions[messageId]?.find { it.emoji == emoji }
            if (current != null) {
                reactionDao.removeReaction(messageId, emoji)
            } else {
                reactionDao.insert(MessageReaction(messageId = messageId, emoji = emoji))

                // Send "Tapback" SMS text
                // "Liked a message", "Loved...", etc.
                // Standard: "Liked " + original text (truncated)
                val originalMsg = rawMessages.find { it.id == messageId }
                if (originalMsg != null && !originalMsg.outgoing) {
                    // Only send Tapback if we are reacting to SOMEONE ELSE'S message
                    // Reacting to our own message locally is fine, but sending "Liked my own message" is weird.
                    val prefix = when(emoji) {
                        "👍" -> "Liked"
                        "❤️" -> "Loved"
                        "😂" -> "Laughed at"
                        "😮" -> "Questioned" // Or Surprised
                        "😢" -> "Emphasized" // Or Sad
                        "👎" -> "Disliked"
                        else -> "Reacted $emoji to"
                    }
                    val snippet = if (originalMsg.body.length > 20) originalMsg.body.take(20) + "..." else originalMsg.body
                    val tapbackBody = "$prefix \"$snippet\""

                    // Send it
                    // NOTE: In a real app we might want to delay this or confirm.
                    val ok = runCatching { repo.sendSms(originalMsg.address, tapbackBody) }.getOrDefault(false)
                    if (ok) {
                         withContext(Dispatchers.Main) {
                             currentThreadId?.let { refreshThread(it, refreshRead = true) }
                         }
                    }
                }
            }
        }
    }

    fun setDelayedSendTimeout(seconds: Int) {
        viewModelScope.launch {
            inboxPrefs.setDelayedSendTimeout(seconds)
        }
    }

    fun setAutoReplyEnabled(enabled: Boolean) {
        viewModelScope.launch {
            inboxPrefs.setAutoReplyEnabled(enabled)
        }
    }

    fun setAutoReplyMessage(message: String) {
        viewModelScope.launch {
            inboxPrefs.setAutoReplyMessage(message)
        }
    }

    fun updateQuickReplies(replies: List<String>) {
        viewModelScope.launch {
            inboxPrefs.setQuickReplies(replies)
        }
    }

    fun setAutoDeleteOtps(enabled: Boolean) {
        viewModelScope.launch {
            inboxPrefs.setAutoDeleteOtps(enabled)
            if (enabled) {
                // Enqueue periodic worker
                val request = androidx.work.PeriodicWorkRequestBuilder<com.pulselink.beacon.worker.OtpCleanupWorker>(
                    24, java.util.concurrent.TimeUnit.HOURS
                ).build()
                workManager.enqueueUniquePeriodicWork(
                    "OtpCleanup",
                    androidx.work.ExistingPeriodicWorkPolicy.KEEP,
                    request
                )
            } else {
                workManager.cancelUniqueWork("OtpCleanup")
            }
        }
    }

    fun sendDelayedMessage(body: String) {
        val delaySeconds = delayedSendTimeout
        if (delaySeconds <= 0) {
            sendMessage(body)
            return
        }

        // If there is already a pending message, send it immediately before starting the new one
        if (pendingMessage != null) {
             sendNow()
        }

        val now = System.currentTimeMillis()
        val target = now + (delaySeconds * 1000)

        pendingMessage = PendingMessage(body, now, target)

        delayedSendJob = viewModelScope.launch {
            delay(delaySeconds * 1000L)
            // Re-check pendingMessage to ensure we are sending the correct one
            // (Though sendNow clears it, so strictly speaking this job should be cancelled if sendNow was called externally)
            if (pendingMessage?.timestamp == now) {
                sendMessage(body)
                pendingMessage = null
            }
        }
    }

    fun cancelDelayedMessage() {
        delayedSendJob?.cancel()
        pendingMessage = null
    }

    fun sendNow() {
        val msg = pendingMessage ?: return
        delayedSendJob?.cancel()
        sendMessage(msg.body)
        pendingMessage = null
    }

    fun sendMessage(body: String) {
        val addr = currentAddress.ifBlank { rawMessages.lastOrNull()?.address.orEmpty() }
        if (addr.isBlank()) return

        // Optimistic UI
        val tempId = System.currentTimeMillis()
        val pending = SmsMessageItem(
            id = tempId,
            threadId = currentThreadId ?: 0L,
            address = addr,
            body = body,
            timestamp = System.currentTimeMillis(),
            outgoing = true,
            status = MessageStatus.SENDING
        )
        pendingOutgoingMessages.add(0, pending)

        // Immediate UI Update
        currentThreadId?.let {
            deleteDraft(it)
            refreshThread(it, refreshRead = false) // Don't mark read triggered by self-send
        }

        viewModelScope.launch(Dispatchers.IO) {
            val ok = runCatching { repo.sendSms(addr, body) }.getOrDefault(false)
            if (ok) {
                // Success: The ContentObserver will trigger refreshThread, which will eventually find the real message
                // and remove the pending one.
                withContext(Dispatchers.Main) {
                    // We could mark it as SENT here if we wanted to keep it until DB update
                    // But relying on DB update is safer for consistency.
                    // Trigger refresh just in case observer is slow?
                    // currentThreadId?.let { refreshThread(it, refreshRead = true) }
                }
            } else {
                withContext(Dispatchers.Main) {
                    // Mark failed
                    val index = pendingOutgoingMessages.indexOfFirst { it.id == tempId }
                    if (index != -1) {
                        pendingOutgoingMessages[index] = pending.copy(status = MessageStatus.FAILED)
                        currentThreadId?.let { refreshThread(it, refreshRead = false) }
                    }
                }
            }
        }
    }

    fun saveDraft(threadId: Long, body: String) {
        if (threadId <= 0) return
        if (body.isBlank()) {
            deleteDraft(threadId)
            return
        }
        viewModelScope.launch {
            draftDao.insertDraft(ThreadDraft(threadId, body, System.currentTimeMillis()))
        }
    }

    fun deleteDraft(threadId: Long) {
        viewModelScope.launch {
            draftDao.deleteDraft(threadId)
        }
    }

    fun getDraftForThread(threadId: Long): String? {
        return draftsMap[threadId]
    }

    fun scheduleMessage(body: String, scheduledTime: Long) {
        val addr = currentAddress.ifBlank { rawMessages.lastOrNull()?.address.orEmpty() }
        if (addr.isBlank()) return

        viewModelScope.launch(Dispatchers.IO) {
            val message = ScheduledMessage(
                address = addr,
                body = body,
                scheduledTimeMillis = scheduledTime
            )

            val delay = scheduledTime - System.currentTimeMillis()

            if (delay <= 0) {
                scheduledDao.insert(message.copy(status = ScheduledStatus.FAILED))
            } else {
                val id = scheduledDao.insert(message)
                val request = OneTimeWorkRequestBuilder<ScheduledMessageWorker>()
                    .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                    .setInputData(workDataOf("messageId" to id))
                    .build()
                workManager.enqueue(request)
            }
        }
    }

    fun cancelScheduledMessage(message: ScheduledMessage) {
        viewModelScope.launch(Dispatchers.IO) {
            scheduledDao.delete(message)
            // Cancel worker if needed (usually handled by worker check)
        }
    }

    fun deleteThread(threadId: Long) {
        repo.deleteThread(threadId)
        if (currentThreadId == threadId) {
            currentThreadId = null
            rawMessages = emptyList()
            uiMessages = emptyList()
        }
        refreshThreads()
    }

    fun search(query: String) {
        searchJob?.cancel()
        if (query.isBlank()) {
            searchState = SearchResultState.Idle
            return
        }

        // Debounce
        searchJob = viewModelScope.launch(Dispatchers.IO) {
            delay(300)
            withContext(Dispatchers.Main) { searchState = SearchResultState.Searching }

            val direct = threads.firstOrNull {
                it.address.contains(query, ignoreCase = true)
                        || it.snippet.contains(query, ignoreCase = true)
            }
            if (direct != null) {
                withContext(Dispatchers.Main) {
                    searchState = SearchResultState.Contact(direct.threadId, direct.address)
                }
                return@launch
            }

            val hits = repo.searchMessages(query)
            withContext(Dispatchers.Main) {
                searchState = if (hits.isEmpty()) SearchResultState.Empty else SearchResultState.Messages(hits)
            }
        }
    }

    fun clearSearch() {
        searchJob?.cancel()
        searchState = SearchResultState.Idle
    }

    companion object {
        fun factory(app: Application) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return SmsViewModel(app) as T
            }
        }
    }
}
