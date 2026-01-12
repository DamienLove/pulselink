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
import com.pulselink.beacon.data.scheduled.BlockedContact
import com.pulselink.beacon.data.scheduled.StarredMessage
import com.pulselink.beacon.data.scheduled.ThreadDraft
import com.pulselink.beacon.data.scheduled.ScheduledMessage
import com.pulselink.beacon.data.scheduled.MessageStatus
import com.pulselink.beacon.worker.ScheduledMessageWorker
import com.pulselink.beacon.util.ThreadDateUtils
import com.pulselink.beacon.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

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
    private val db = BeaconDatabase.getDatabase(app)
    private val scheduledDao = db.scheduledMessageDao()
    private val extrasDao = db.extrasDao()
    private val workManager = WorkManager.getInstance(app)

    private companion object {
        const val THREAD_LIMIT = 500
        const val MESSAGE_LIMIT = 300
    }

    var threads by mutableStateOf<List<SmsThreadItem>>(emptyList())
        private set
    var uiMessages by mutableStateOf<List<ThreadUiItem>>(emptyList())
        private set
    private var rawMessages = emptyList<SmsMessageItem>()

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

    var pendingMessage by mutableStateOf<PendingMessage?>(null)
        private set

    data class PendingMessage(
        val body: String,
        val timestamp: Long,
        val targetTime: Long
    )

    private var delayedSendJob: Job? = null
    private var rawThreads: List<SmsThreadItem> = emptyList()
    private var inboxState: InboxState = InboxState()
    private var blockedContacts: Set<String> = emptySet()
    private var starredMessageIds: Set<Long> = emptySet()

    private var searchJob: Job? = null

    init {
        refreshThreads(initial = true)
        refreshExtras()

        viewModelScope.launch {
            repo.changes().collectLatest {
                refreshThreads(initial = false)
                currentThreadId?.let { refreshThread(it, refreshRead = false) }
            }
        }

        viewModelScope.launch {
            inboxPrefs.flow.collectLatest { state ->
                inboxState = state
                mergeThreads()
            }
        }
    }

    fun refreshExtras() {
        viewModelScope.launch(Dispatchers.IO) {
            val blocked = runCatching { extrasDao.getAllBlockedContacts().map { it.address }.toSet() }
                .getOrElse { emptySet() }
            val starred = runCatching { extrasDao.getAllStarredMessageIds().toSet() }
                .getOrElse { emptySet() }

            withContext(Dispatchers.Main) {
                blockedContacts = blocked
                starredMessageIds = starred
                mergeThreads() // re-merge to filter blocked
                // If in thread view, refresh to show stars
                currentThreadId?.let { refreshThread(it, refreshRead = false) }
            }
        }
    }

    fun refreshThreads(initial: Boolean = false) {
        viewModelScope.launch {
            if (initial) isLoading = true else isRefreshing = true
            rawThreads = runCatching { repo.listThreads(limit = THREAD_LIMIT) }.getOrElse { emptyList() }
            mergeThreads()
            if (initial) isLoading = false else isRefreshing = false
        }
    }

    private fun mergeThreads() {
        val merged = rawThreads.asSequence()
            .filter { !blockedContacts.contains(it.address) } // Filter blocked
            .map { thread ->
                val isPinned = inboxState.pinnedThreadIds.contains(thread.threadId)
                if (isPinned != thread.isPinned || inboxState.archivedThreadIds.contains(thread.threadId) != thread.isArchived) {
                    thread.copy(
                        isPinned = isPinned,
                        isArchived = inboxState.archivedThreadIds.contains(thread.threadId)
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
        threads = merged
    }

    // --- Actions ---

    fun blockThread(address: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                extrasDao.blockContact(BlockedContact(address, System.currentTimeMillis()))
                withContext(Dispatchers.Main) {
                    userMessage = "Blocked $address"
                    refreshExtras()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    userMessage = "Failed to block: ${e.message}"
                }
            }
        }
    }

    fun unblockThread(address: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                extrasDao.unblockContact(address)
                withContext(Dispatchers.Main) {
                    userMessage = "Unblocked $address"
                    refreshExtras()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    userMessage = "Failed to unblock: ${e.message}"
                }
            }
        }
    }

    fun starMessage(messageId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                extrasDao.starMessage(StarredMessage(messageId, System.currentTimeMillis()))
                withContext(Dispatchers.Main) {
                    refreshExtras() // This will trigger UI update
                }
            } catch (e: Exception) {
                // Silent fail or log
            }
        }
    }

    fun unstarMessage(messageId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                extrasDao.unstarMessage(messageId)
                withContext(Dispatchers.Main) {
                    refreshExtras()
                }
            } catch (e: Exception) {
                // Silent fail
            }
        }
    }

    suspend fun getDraft(threadId: Long): String? {
        return withContext(Dispatchers.IO) {
             runCatching { extrasDao.getDraft(threadId) }.getOrNull()
        }
    }

    fun saveDraft(threadId: Long, text: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                if (text.isBlank()) {
                    extrasDao.deleteDraft(threadId)
                } else {
                    extrasDao.saveDraft(ThreadDraft(threadId, text, System.currentTimeMillis()))
                }
            } catch (e: Exception) {
                // Silent fail
            }
        }
    }

    // --- Standard Methods ---

    fun togglePin(threadId: Long) {
        viewModelScope.launch {
            inboxPrefs.togglePin(threadId)
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
            rawMessages = runCatching { repo.messagesForThread(threadId, limit = MESSAGE_LIMIT) }
                .getOrElse { emptyList() }
            uiMessages = ThreadDateUtils.mapMessagesToUi(rawMessages, starredMessageIds)

            if (refreshRead) runCatching { repo.markThreadRead(threadId) }
        }
    }

    fun sendDelayedMessage(body: String, delaySeconds: Int = 5) {
        if (pendingMessage != null) {
             sendNow()
        }

        val now = System.currentTimeMillis()
        val target = now + (delaySeconds * 1000)

        pendingMessage = PendingMessage(body, now, target)

        delayedSendJob = viewModelScope.launch {
            delay(delaySeconds * 1000L)
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

        viewModelScope.launch(Dispatchers.IO) {
            val ok = runCatching { repo.sendSms(addr, body) }.getOrDefault(false)
            if (ok) {
                withContext(Dispatchers.Main) {
                    currentThreadId?.let { refreshThread(it, refreshRead = true) }
                    // Clear draft if sent
                    saveDraft(currentThreadId ?: 0L, "")
                }
            }
        }
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
                scheduledDao.insert(message.copy(status = MessageStatus.FAILED))
            } else {
                val id = scheduledDao.insert(message)
                val request = OneTimeWorkRequestBuilder<ScheduledMessageWorker>()
                    .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                    .setInputData(workDataOf("messageId" to id))
                    .build()
                workManager.enqueue(request)
            }
            // Clear draft
            saveDraft(currentThreadId ?: 0L, "")
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
