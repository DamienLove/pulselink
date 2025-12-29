package com.pulselink.ui.state

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pulselink.BuildConfig
import com.pulselink.data.ai.AiAssistantRepository
import com.pulselink.data.ai.AiComposeAction
import com.pulselink.data.sms.RemoteSmsRepository
import com.pulselink.data.sms.SmsLineRepository
import com.pulselink.data.sms.SmsMessageItem
import com.pulselink.data.sms.SmsMessageStatus
import com.pulselink.data.sms.SmsOutboxService
import com.pulselink.data.sms.SmsRepository
import com.pulselink.data.sms.SmsSender
import com.pulselink.data.sms.SmsThreadItem
import com.pulselink.domain.model.Contact
import com.pulselink.domain.model.LineInboxMode
import com.pulselink.domain.model.SmsLine
import com.pulselink.domain.model.ThemePreferences
import com.pulselink.domain.repository.ContactRepository
import com.pulselink.domain.repository.SettingsRepository
import com.pulselink.util.normalizeSmsAddress
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlin.math.abs

sealed class SearchResultState {
    object Idle : SearchResultState()
    object Searching : SearchResultState()
    data class Contact(val threadId: Long, val address: String) : SearchResultState()
    data class Messages(val hits: List<SmsMessageItem>) : SearchResultState()
    object Empty : SearchResultState()
}

@HiltViewModel
class SmsInboxViewModel @Inject constructor(
    private val smsRepository: SmsRepository,
    private val remoteSmsRepository: RemoteSmsRepository,
    private val smsLineRepository: SmsLineRepository,
    private val settingsRepository: SettingsRepository,
    private val contactRepository: ContactRepository
) : ViewModel() {
    private companion object {
        const val THREAD_LIMIT = Int.MAX_VALUE
    }
    private val localThreads = MutableStateFlow<List<SmsThreadItem>>(emptyList())
    private val localArchived = MutableStateFlow<List<SmsThreadItem>>(emptyList())
    private val remoteThreads = MutableStateFlow<List<SmsThreadItem>>(emptyList())
    private val _threads = MutableStateFlow<List<SmsThreadItem>>(emptyList())
    val threads: StateFlow<List<SmsThreadItem>> = _threads
    private val _archived = MutableStateFlow<List<SmsThreadItem>>(emptyList())
    val archived: StateFlow<List<SmsThreadItem>> = _archived
    private val _searchState = MutableStateFlow<SearchResultState>(SearchResultState.Idle)
    val searchState: StateFlow<SearchResultState> = _searchState
    private val _lines = MutableStateFlow<List<SmsLine>>(emptyList())
    val lines: StateFlow<List<SmsLine>> = _lines
    private val _activeLineId = MutableStateFlow<String?>(null)
    val activeLineId: StateFlow<String?> = _activeLineId
    private val _inboxMode = MutableStateFlow(LineInboxMode.COMBINED)
    val inboxMode: StateFlow<LineInboxMode> = _inboxMode
    private val _isDatabaseBusy = MutableStateFlow(false)
    val isDatabaseBusy: StateFlow<Boolean> = _isDatabaseBusy
    private val deviceLineId = MutableStateFlow("")
    private var refreshJob: Job? = null
    private var lastRefreshAt = 0L

    fun refresh(force: Boolean = false) {
        refreshJob?.cancel()
        refreshJob = viewModelScope.launch {
            val now = System.currentTimeMillis()
            if (!force && now - lastRefreshAt < 1500L) return@launch
            lastRefreshAt = now
            val deviceId = deviceLineId.value.ifBlank {
                settingsRepository.ensureDeviceId().also { deviceLineId.value = it }
            }
            val (threads, archived) = withContext(Dispatchers.IO) {
                smsRepository.listThreadsAndArchived(
                    limit = THREAD_LIMIT,
                    fromSmsOnly = false,
                    forceRefresh = force
                )
            }
            if (!force &&
                threads.isEmpty() &&
                archived.isEmpty() &&
                (localThreads.value.isNotEmpty() || localArchived.value.isNotEmpty())
            ) {
                return@launch
            }
            localThreads.value = threads.map { it.copy(lineId = deviceId) }
            localArchived.value = archived.map { it.copy(lineId = deviceId) }
        }
    }

    fun importAllMessages() {
        viewModelScope.launch {
            runDatabaseAction {
                val deviceId = deviceLineId.value.ifBlank {
                    settingsRepository.ensureDeviceId().also { deviceLineId.value = it }
                }
                val (threads, archived) = withContext(Dispatchers.IO) {
                    smsRepository.listThreadsAndArchived(
                        limit = THREAD_LIMIT,
                        fromSmsOnly = true,
                        forceRefresh = true
                    )
                }
                localThreads.value = threads.map { it.copy(lineId = deviceId) }
                localArchived.value = archived.map { it.copy(lineId = deviceId) }
            }
        }
    }

    fun search(query: String) {
        if (query.isBlank()) {
            _searchState.value = SearchResultState.Idle
            return
        }
        _searchState.value = SearchResultState.Searching
        viewModelScope.launch(Dispatchers.IO) {
            val direct = _threads.value.firstOrNull {
                it.address.contains(query, ignoreCase = true) ||
                    it.snippet.contains(query, ignoreCase = true)
            }
            if (direct != null) {
                withContext(Dispatchers.Main) {
                    _searchState.value = SearchResultState.Contact(direct.threadId, direct.address)
                }
                return@launch
            }

            val hits = smsRepository.searchMessages(query)
            withContext(Dispatchers.Main) {
                _searchState.value = if (hits.isEmpty()) SearchResultState.Empty else SearchResultState.Messages(hits)
            }
        }
    }

    fun clearSearch() {
        _searchState.value = SearchResultState.Idle
    }

    fun archive(threadId: Long) {
        viewModelScope.launch {
            runDatabaseAction {
                withContext(Dispatchers.IO) {
                    smsRepository.markThreadRead(threadId)
                    smsRepository.archiveThread(threadId)
                }
                refresh(force = true)
            }
        }
    }

    fun unarchive(threadId: Long) {
        viewModelScope.launch {
            runDatabaseAction {
                withContext(Dispatchers.IO) {
                    smsRepository.unarchiveThread(threadId)
                }
                refresh(force = true)
            }
        }
    }

    fun delete(threadId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            runDatabaseAction {
                smsRepository.deleteThread(threadId)
                refresh()
            }
        }
    }

    private suspend fun runDatabaseAction(block: suspend () -> Unit) {
        _isDatabaseBusy.value = true
        try {
            block()
        } finally {
            _isDatabaseBusy.value = false
        }
    }

    init {
        viewModelScope.launch {
            deviceLineId.value = settingsRepository.ensureDeviceId()
        }

        settingsRepository.settings
            .onEach { settings ->
                _inboxMode.value = settings.lineInboxMode
                _activeLineId.value = settings.activeLineId ?: settings.deviceId.ifBlank { deviceLineId.value }
                if (settings.deviceId.isNotBlank() && deviceLineId.value != settings.deviceId) {
                    deviceLineId.value = settings.deviceId
                }
            }
            .launchIn(viewModelScope)

        smsLineRepository.observeLines()
            .onEach { lines -> _lines.value = lines.filter { line -> !line.disabled } }
            .launchIn(viewModelScope)

        combine(_lines, _inboxMode, _activeLineId, deviceLineId) { lines, mode, active, deviceId ->
            val lineIds = when (mode) {
                LineInboxMode.COMBINED -> lines.map { it.id }.filter { it.isNotBlank() && it != deviceId }
                LineInboxMode.PER_LINE -> listOfNotNull(active).filter { it.isNotBlank() && it != deviceId }
            }
            lineIds
        }
            .flatMapLatest { lineIds ->
                if (lineIds.isEmpty()) flowOf(emptyList()) else remoteSmsRepository.observeThreads(lineIds)
            }
            .onEach { remoteThreads.value = it }
            .launchIn(viewModelScope)

        combine(
            localThreads,
            localArchived,
            remoteThreads,
            _inboxMode,
            _activeLineId,
            deviceLineId
) { values ->
            val local = values[0] as List<SmsThreadItem>
            val archived = values[1] as List<SmsThreadItem>
            val remote = values[2] as List<SmsThreadItem>
            val mode = values[3] as LineInboxMode
            val activeLine = values[4] as String?
            val deviceId = values[5] as String
            val active = activeLine ?: deviceId
            val visibleThreads = when (mode) {
                LineInboxMode.COMBINED -> (local + remote).sortedByDescending { it.timestamp }
                LineInboxMode.PER_LINE -> if (active == deviceId) local else remote.filter { it.lineId == active }
            }
            val visibleArchived = if (mode == LineInboxMode.PER_LINE && active != deviceId) {
                emptyList()
            } else {
                archived
            }
            Pair(visibleThreads, visibleArchived)
        }
            .onEach { (visibleThreads, visibleArchived) ->
                _threads.value = visibleThreads
                _archived.value = visibleArchived
            }
            .launchIn(viewModelScope)

        smsRepository.changes()
            .debounce(400)
            .onEach { refresh(force = true) }
            .launchIn(viewModelScope)

        contactRepository.observeContacts()
            .debounce(300)
            .onEach { refresh(force = true) }
            .launchIn(viewModelScope)
    }
}

@HiltViewModel
class SmsThreadViewModel @Inject constructor(
    private val smsRepository: SmsRepository,
    private val contactRepository: ContactRepository,
    private val smsSender: SmsSender,
    private val aiAssistantRepository: AiAssistantRepository,
    private val settingsRepository: SettingsRepository,
    private val remoteSmsRepository: RemoteSmsRepository,
    private val smsOutboxService: SmsOutboxService
) : ViewModel() {
    private companion object {
        const val MESSAGE_LIMIT = Int.MAX_VALUE
        const val PENDING_MATCH_WINDOW_MS = 20_000L
    }
    private val _messages = MutableStateFlow<List<SmsMessageItem>>(emptyList())
    val messages: StateFlow<List<SmsMessageItem>> = _messages
    private val _contact = MutableStateFlow<Contact?>(null)
    val contact: StateFlow<Contact?> = _contact
    private val _isArchived = MutableStateFlow(false)
    val isArchived: StateFlow<Boolean> = _isArchived
    private val _summaryState = MutableStateFlow<AiSummaryState>(AiSummaryState.Idle)
    val summaryState: StateFlow<AiSummaryState> = _summaryState
    private val _composeState = MutableStateFlow<AiComposeState>(AiComposeState.Idle)
    val composeState: StateFlow<AiComposeState> = _composeState
    private val _isDatabaseBusy = MutableStateFlow(false)
    val isDatabaseBusy: StateFlow<Boolean> = _isDatabaseBusy
    private var activeThreadId: Long? = null
    private var activeAddress: String = ""
    private var activeLineId: String? = null
    private var deviceLineId: String = ""
    private var lastSummaryMessageId: Long? = null
    private var remoteMessagesJob: Job? = null
    private var localMessagesJob: Job? = null
    private val pendingMessages = mutableListOf<SmsMessageItem>()
    private val pendingLock = Any()
    private var loadedMessages: List<SmsMessageItem> = emptyList()

    fun load(threadId: Long, address: String, lineId: String? = null) {
        activeThreadId = threadId.takeIf { it > 0 }
        activeAddress = address
        resetMessages()
        viewModelScope.launch {
            deviceLineId = settingsRepository.ensureDeviceId()
            activeLineId = lineId ?: deviceLineId
            if (activeAddress.isNotBlank()) {
                _contact.value = withContext(Dispatchers.IO) {
                    contactRepository.getByPhone(activeAddress)
                }
            }
            remoteMessagesJob?.cancel()
            localMessagesJob?.cancel()
            if (!activeLineId.isNullOrBlank() && activeLineId != deviceLineId && activeThreadId != null) {
                remoteMessagesJob = remoteSmsRepository
                    .observeMessages(activeLineId!!, activeThreadId!!)
                    .onEach { updateMessages(it) }
                    .launchIn(viewModelScope)
                _isArchived.value = false
            } else {
                refreshMessages()
                localMessagesJob = smsRepository.changes()
                    .onEach { refreshMessages() }
                    .launchIn(viewModelScope)
            }
        }
    }

    private suspend fun refreshMessages() {
        val result = withContext(Dispatchers.IO) {
            if (activeThreadId == null && activeAddress.isNotBlank()) {
                activeThreadId = smsRepository.resolveThreadIdForAddress(activeAddress)
            }
            val msgs = when {
                activeThreadId != null -> smsRepository.messagesForThread(activeThreadId!!, limit = MESSAGE_LIMIT)
                activeAddress.isNotBlank() -> smsRepository.messagesForAddress(activeAddress, limit = MESSAGE_LIMIT)
                else -> emptyList()
            }
            val contact = msgs.firstOrNull()?.address?.let { contactRepository.getByPhone(it) }
            val archived = activeThreadId?.let { smsRepository.isThreadArchived(it) } ?: false
            Triple(msgs, contact, archived)
        }
        val msgs = result.first
        val contact = result.second
        val archived = result.third
        activeThreadId?.let { threadId ->
            withContext(Dispatchers.IO) { smsRepository.markThreadRead(threadId) }
        }
        updateMessages(msgs)
        _contact.value = contact
        _isArchived.value = archived
    }

    fun toggleArchive() {
        viewModelScope.launch {
            runDatabaseAction {
                if (activeThreadId == null && activeAddress.isNotBlank()) {
                    activeThreadId = smsRepository.resolveThreadIdForAddress(activeAddress)
                }
                val threadId = activeThreadId ?: return@runDatabaseAction
                val updatedArchived = withContext(Dispatchers.IO) {
                    if (_isArchived.value) {
                        smsRepository.unarchiveThread(threadId)
                    } else {
                        smsRepository.archiveThread(threadId)
                    }
                    smsRepository.isThreadArchived(threadId)
                }
                _isArchived.value = updatedArchived
            }
        }
    }

    fun setContactTheme(theme: ThemePreferences?) {
        viewModelScope.launch(Dispatchers.IO) {
            runDatabaseAction {
                _contact.value?.let { currentContact ->
                    val updated = currentContact.copy(themeOverride = theme)
                    contactRepository.upsert(updated)
                    withContext(Dispatchers.Main) {
                        _contact.value = updated
                    }
                }
            }
        }
    }

    private suspend fun runDatabaseAction(block: suspend () -> Unit) {
        _isDatabaseBusy.value = true
        try {
            block()
        } finally {
            _isDatabaseBusy.value = false
        }
    }

    fun sendMessage(address: String, body: String, lineId: String? = null) {
        viewModelScope.launch {
            // Handle multi-recipient (Broadcast) by splitting on common delimiters
            val targetAddress = address.ifBlank { activeAddress }
            if (targetAddress.isBlank()) return@launch
            activeAddress = targetAddress
            val sendTimestamp = System.currentTimeMillis()
            val pendingId = -sendTimestamp
            addPendingMessage(
                SmsMessageItem(
                    id = pendingId,
                    threadId = activeThreadId ?: pendingId,
                    address = targetAddress,
                    body = body,
                    timestamp = sendTimestamp,
                    outgoing = true,
                    status = SmsMessageStatus.SENDING
                )
            )
            val destinations = targetAddress.split(';', ',')
                .map { it.trim() }
                .filter { it.isNotBlank() }
            val resolvedLineId = lineId ?: activeLineId ?: deviceLineId
            val isRemoteLine = resolvedLineId.isNotBlank() && resolvedLineId != deviceLineId
            var anySuccess = false

            withContext(Dispatchers.IO) {
                destinations.forEach { dest ->
                    val rawNumber = normalizeSmsAddress(dest)
                    if (rawNumber.isNotBlank()) {
                        try {
                            if (isRemoteLine) {
                                smsOutboxService.queueMessage(rawNumber, body, resolvedLineId, activeThreadId)
                                anySuccess = true
                            } else {
                                // awaitResult = false to parallelize if multiple
                                val sent = smsSender.sendSms(rawNumber, body, awaitResult = false)
                                anySuccess = anySuccess || sent
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }
            }
            if (!isRemoteLine) {
                refreshMessages()
            }
            if (!anySuccess) {
                markPendingFailed(pendingId)
            }
        }
    }

    private fun updateMessages(messages: List<SmsMessageItem>) {
        withPendingLock {
            loadedMessages = messages
            _messages.value = mergeMessages(messages)
        }
        val latestMessageId = messages.firstOrNull()?.id
        if (latestMessageId != lastSummaryMessageId) {
            lastSummaryMessageId = null
            if (_summaryState.value !is AiSummaryState.Loading) {
                _summaryState.value = AiSummaryState.Idle
            }
        }
    }

    private fun resetMessages() {
        withPendingLock {
            pendingMessages.clear()
            loadedMessages = emptyList()
            _messages.value = emptyList()
        }
    }

    private fun addPendingMessage(message: SmsMessageItem) {
        withPendingLock {
            pendingMessages.add(message)
            _messages.value = mergeMessages(loadedMessages)
        }
    }

    private fun markPendingFailed(pendingId: Long) {
        withPendingLock {
            pendingMessages.replaceAll { msg ->
                if (msg.id == pendingId) msg.copy(status = SmsMessageStatus.FAILED) else msg
            }
            _messages.value = mergeMessages(loadedMessages)
        }
    }

    private fun mergeMessages(messages: List<SmsMessageItem>): List<SmsMessageItem> {
        if (pendingMessages.isEmpty()) return messages
        val pendingRemaining = pendingMessages.filterNot { pending ->
            messages.any { loaded -> isSameMessage(loaded, pending) }
        }
        pendingMessages.clear()
        pendingMessages.addAll(pendingRemaining)
        return (messages + pendingRemaining).sortedByDescending { it.timestamp }
    }

    private fun isSameMessage(loaded: SmsMessageItem, pending: SmsMessageItem): Boolean {
        if (loaded.outgoing != pending.outgoing) return false
        if (loaded.body != pending.body) return false
        val timeDelta = abs(loaded.timestamp - pending.timestamp)
        if (timeDelta > PENDING_MATCH_WINDOW_MS) return false
        val loadedAddress = normalizeSmsAddress(loaded.address)
        val pendingAddress = normalizeSmsAddress(pending.address)
        if (loadedAddress.isNotBlank() && pendingAddress.isNotBlank()) {
            return loadedAddress == pendingAddress
        }
        return true
    }

    private fun <T> withPendingLock(block: () -> T): T = synchronized(pendingLock) {
        block()
    }

    fun requestSummary(force: Boolean = false) {
        viewModelScope.launch {
            val settings = settingsRepository.settings.first()
            val premium = BuildConfig.PREMIUM_FEATURES || settings.premiumUnlocked
            if (!premium || !settings.aiSummariesEnabled) return@launch

            val messages = loadedMessages
            if (messages.isEmpty()) return@launch
            val latestMessageId = messages.firstOrNull()?.id
            if (!force && latestMessageId != null && latestMessageId == lastSummaryMessageId) {
                return@launch
            }
            _summaryState.value = AiSummaryState.Loading
            val slices = messages.take(20).reversed().map { msg ->
                val sender = if (msg.outgoing) "Me" else "Them"
                "$sender: ${msg.body}"
            }
            val contactName = _contact.value?.displayName
            val summary = runCatching {
                withTimeout(12_000L) {
                    aiAssistantRepository.summarizeThread(slices, contactName)
                }
            }.getOrElse { error ->
                _summaryState.value = AiSummaryState.Error(error.message ?: "Summary failed.")
                return@launch
            }
            lastSummaryMessageId = latestMessageId
            _summaryState.value = AiSummaryState.Success(summary)
        }
    }

    fun clearSummary() {
        _summaryState.value = AiSummaryState.Idle
    }

    fun requestCompose(
        action: AiComposeAction,
        draft: String?,
        lastMessage: String?
    ) {
        viewModelScope.launch {
            val settings = settingsRepository.settings.first()
            val premium = BuildConfig.PREMIUM_FEATURES || settings.premiumUnlocked
            if (!premium || !settings.aiComposeEnabled) return@launch

            val resolvedDraft = draft?.trim()
            if (action != AiComposeAction.REPLY && resolvedDraft.isNullOrBlank()) {
                _composeState.value = AiComposeState.Error("Type a draft first.")
                return@launch
            }
            _composeState.value = AiComposeState.Loading
            val suggestion = runCatching {
                withTimeout(12_000L) {
                    aiAssistantRepository.composeSuggestion(
                        action = action,
                        draft = resolvedDraft,
                        lastMessage = lastMessage
                    )
                }
            }.getOrElse { error ->
                _composeState.value = AiComposeState.Error(error.message ?: "AI assist failed.")
                return@launch
            }
            _composeState.value = AiComposeState.Suggestion(suggestion)
        }
    }

    fun clearCompose() {
        _composeState.value = AiComposeState.Idle
    }
}

