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
import com.pulselink.beacon.data.scheduled.BeaconDatabase
import com.pulselink.beacon.data.scheduled.ScheduledMessage
import com.pulselink.beacon.data.scheduled.MessageStatus
import com.pulselink.beacon.worker.ScheduledMessageWorker
import com.pulselink.beacon.BuildConfig
import kotlinx.coroutines.Dispatchers
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
    private val scheduledDao = BeaconDatabase.getDatabase(app).scheduledMessageDao()
    private val workManager = WorkManager.getInstance(app)

    private companion object {
        const val THREAD_LIMIT = 100
        const val MESSAGE_LIMIT = 300
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
    var isLoading by mutableStateOf(true)
        private set
    var isRefreshing by mutableStateOf(false)
        private set

    init {
        refreshThreads(initial = true)
        viewModelScope.launch {
            repo.changes().collectLatest {
                refreshThreads(initial = false)
                currentThreadId?.let { refreshThread(it, refreshRead = false) }
            }
        }
    }

    fun refreshThreads(initial: Boolean = false) {
        viewModelScope.launch {
            if (initial) isLoading = true else isRefreshing = true
            threads = runCatching { repo.listThreads(limit = THREAD_LIMIT) }.getOrElse { emptyList() }
            if (initial) isLoading = false else isRefreshing = false
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
        viewModelScope.launch {
            messages = runCatching { repo.messagesForThread(threadId, limit = MESSAGE_LIMIT) }
                .getOrElse { emptyList() }
            if (refreshRead) runCatching { repo.markThreadRead(threadId) }
        }
    }

    fun sendMessage(body: String) {
        val addr = currentAddress.ifBlank { messages.lastOrNull()?.address.orEmpty() }
        if (addr.isBlank()) return

        viewModelScope.launch(Dispatchers.IO) {
            val ok = runCatching { repo.sendSms(addr, body) }.getOrDefault(false)
            if (ok) {
                withContext(Dispatchers.Main) {
                    currentThreadId?.let { refreshThread(it, refreshRead = true) }
                }
            }
        }
    }

    fun scheduleMessage(body: String, scheduledTime: Long) {
        val addr = currentAddress.ifBlank { messages.lastOrNull()?.address.orEmpty() }
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
        }
    }

    fun deleteThread(threadId: Long) {
        repo.deleteThread(threadId)
        if (currentThreadId == threadId) {
            currentThreadId = null
            messages = emptyList()
        }
        refreshThreads()
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
