package com.pulselink.beacon.ui

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.pulselink.beacon.data.SmsMessageItem
import com.pulselink.beacon.data.SmsRepository
import com.pulselink.beacon.data.SmsThreadItem
import com.pulselink.beacon.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed class SearchResultState {
    object Idle : SearchResultState()
    object Searching : SearchResultState()
    data class Contact(val threadId: Long, val address: String) : SearchResultState()
    data class Messages(val hits: List<SmsMessageItem>) : SearchResultState()
    object Empty : SearchResultState()
}

class SmsViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = SmsRepository(app.applicationContext)
    private companion object {
        const val THREAD_LIMIT = 200
        const val MESSAGE_LIMIT = 200
    }

    var threads by mutableStateOf<List<SmsThreadItem>>(emptyList())
        private set
    var messages by mutableStateOf<List<SmsMessageItem>>(emptyList())
        private set
    var currentThreadId by mutableStateOf<Long?>(null)
        private set
    var currentAddress by mutableStateOf("")
        private set
    var searchState: SearchResultState by mutableStateOf(SearchResultState.Idle)
        private set
    var currentFilter by mutableStateOf(InboxFilter.ALL)
        private set

    // Track inbox state locally to pass to sync methods
    private var inboxState = InboxState()

    init {
        // Collect inbox state and trigger refresh
        viewModelScope.launch {
            repo.inboxStateFlow.collectLatest { state ->
                inboxState = state
                refreshThreads()
            }
        }

        // Listen for DB changes
        viewModelScope.launch {
            repo.changes().collectLatest {
                refreshThreads()
                currentThreadId?.let { refreshThread(it, refreshRead = false) }
            }
        }
    }

    fun setFilter(filter: InboxFilter) {
        currentFilter = filter
        refreshThreads()
    }

    fun refreshThreads() {
        threads = if (currentFilter == InboxFilter.ARCHIVED) {
            runCatching { repo.listArchivedThreads(limit = THREAD_LIMIT, state = inboxState) }.getOrElse { emptyList() }
        } else {
            runCatching { repo.listInboxThreads(limit = THREAD_LIMIT, state = inboxState) }.getOrElse { emptyList() }
        }
    }

    fun openThread(threadId: Long, address: String) {
                // Handle new conversations
        if (threadId == 0L && !address.isNullOrBlank()) {
            currentThreadId = 0L
            currentAddress = address
            messages = emptyList()
            return
        }

        currentThreadId = threadId
        currentAddress = address
        refreshThread(threadId, refreshRead = true)
    }

    private fun refreshThread(threadId: Long, refreshRead: Boolean) {
        messages = runCatching { repo.messagesForThread(threadId, limit = MESSAGE_LIMIT) }
            .getOrElse { emptyList() }
        if (refreshRead) runCatching { repo.markThreadRead(threadId) }
    }

    fun sendMessage(body: String): Boolean {
        val addr = currentAddress.ifBlank { messages.lastOrNull()?.address.orEmpty() }
        if (addr.isBlank()) return false
        val ok = runCatching { repo.sendSms(addr, body) }.getOrDefault(false)
        if (ok) currentThreadId?.let { refreshThread(it, refreshRead = true) }
        return ok
    }

    fun deleteThread(threadId: Long) {
        repo.deleteThread(threadId)
        if (currentThreadId == threadId) {
            currentThreadId = null
            messages = emptyList()
        }
        refreshThreads()
    }

    fun togglePin(threadId: Long) {
        viewModelScope.launch {
            repo.togglePin(threadId)
            // The repo change flow will trigger refreshThreads, but it might be delayed.
            // Since togglePin updates DataStore and we collect it in repo, it should trigger changes() flow.
        }
    }

    fun toggleArchive(threadId: Long) {
        viewModelScope.launch {
            repo.toggleArchive(threadId)
        }
    }

    fun search(query: String) {
        if (query.isBlank()) {
            searchState = SearchResultState.Idle
            return
        }
        searchState = SearchResultState.Searching
        viewModelScope.launch(Dispatchers.IO) {
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

            // Premium hook: route through Gemini when available; fallback to local search otherwise
            val hits = if (BuildConfig.PREMIUM_SEARCH) {
                repo.searchMessages(query) // placeholder until Gemini backend is wired
            } else {
                repo.searchMessages(query)
            }
            withContext(Dispatchers.Main) {
                searchState = if (hits.isEmpty()) SearchResultState.Empty else SearchResultState.Messages(hits)
            }
        }
    }

    fun clearSearch() {
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
