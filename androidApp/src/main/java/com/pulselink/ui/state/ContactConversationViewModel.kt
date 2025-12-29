package com.pulselink.ui.state

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pulselink.data.link.ContactLinkManager
import com.pulselink.domain.model.ContactMessage
import com.pulselink.domain.model.MessageDirection
import com.pulselink.domain.model.MessageStatus
import com.pulselink.domain.model.ManualMessageResult
import com.pulselink.domain.model.isSent
import com.pulselink.domain.repository.MessageRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import kotlin.math.abs

@HiltViewModel
class ContactConversationViewModel @Inject constructor(
    private val messageRepository: MessageRepository,
    private val linkManager: ContactLinkManager,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val contactId: Long = savedStateHandle.get<Long>("contactId") ?: 0L

    private val pendingMessages = MutableStateFlow<List<ContactMessage>>(emptyList())
    private val storedMessages: Flow<List<ContactMessage>> =
        if (contactId > 0) messageRepository.observeForContact(contactId) else flowOf(emptyList())

    val messages: StateFlow<List<ContactMessage>> = combine(
        storedMessages,
        pendingMessages
    ) { stored, pending ->
        val filteredPending = pending.filter { pendingMessage ->
            stored.none { existing -> isSameMessage(existing, pendingMessage) }
        }
        (stored + filteredPending).sortedByDescending { it.timestamp }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    suspend fun sendMessage(text: String): ManualMessageResult {
        if (contactId <= 0) {
            return ManualMessageResult.Failure(ManualMessageResult.Failure.Reason.CONTACT_MISSING)
        }
        val trimmed = text.trim()
        if (trimmed.isBlank()) {
            return ManualMessageResult.Failure(ManualMessageResult.Failure.Reason.UNKNOWN)
        }
        val tempId = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()
        val optimistic = ContactMessage(
            messageId = tempId,
            contactId = contactId,
            body = trimmed,
            direction = MessageDirection.OUTBOUND,
            timestamp = now,
            status = MessageStatus.SENDING
        )
        pendingMessages.update { it + optimistic }

        val result = runCatching {
            linkManager.sendManualMessage(contactId, trimmed)
        }.getOrElse {
            pendingMessages.update { list -> list.filterNot { msg -> msg.messageId == tempId } }
            return ManualMessageResult.Failure(ManualMessageResult.Failure.Reason.UNKNOWN)
        }

        if (result is ManualMessageResult.Success) {
            pendingMessages.update { list ->
                list.map { msg ->
                    if (msg.messageId == tempId) {
                        msg.copy(
                            status = MessageStatus.SENT,
                            overrideSucceeded = result.overrideApplied
                        )
                    } else msg
                }
            }
        } else {
            pendingMessages.update { list -> list.filterNot { msg -> msg.messageId == tempId } }
        }

        return result
    }

    fun markMessagesAsRead() {
        viewModelScope.launch {
            messages.value
                .filter { msg ->
                    !msg.isSent && msg.status != MessageStatus.READ && msg.messageId.isNotBlank()
                }
                .forEach { msg ->
                    messageRepository.updateMessageStatus(msg.messageId, contactId, MessageStatus.READ)
                }
        }
    }

    private fun isSameMessage(existing: ContactMessage, pending: ContactMessage): Boolean {
        if (existing.direction != pending.direction) return false
        if (existing.body != pending.body) return false
        val delta = abs(existing.timestamp - pending.timestamp)
        return delta <= 15_000L
    }
}
