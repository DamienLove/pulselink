package com.pulselink.beacon.data

enum class ThreadCategory {
    PERSONAL, TRANSACTIONS, PROMOTIONS
}

enum class MessageStatus {
    NONE, SENDING, SENT, FAILED
}

data class SmsThreadItem(
    val threadId: Long,
    val address: String,
    val snippet: String,
    val timestamp: Long,
    val unread: Boolean,
    val isPinned: Boolean = false,
    val isArchived: Boolean = false,
    val category: ThreadCategory = ThreadCategory.PERSONAL,
    val draftSnippet: String? = null
)

data class SmsMessageItem(
    val id: Long,
    val threadId: Long,
    val address: String,
    val body: String,
    val timestamp: Long,
    val outgoing: Boolean,
    val isMms: Boolean = false,
    val mediaParts: List<MmsPart> = emptyList(),
    val status: MessageStatus = MessageStatus.NONE
)

data class MmsPart(
    val contentType: String,
    val text: String? = null,
    val dataUri: android.net.Uri? = null
)

data class InboxState(
    val pinnedThreadIds: Set<Long> = emptySet(),
    val archivedThreadIds: Set<Long> = emptySet(),
    val delayedSendTimeout: Int = 5,
    val autoReplyEnabled: Boolean = false,
    val autoReplyMessage: String = "",
    val quickReplies: List<String> = listOf("Ok", "Yes", "No", "Thanks", "On my way!", "Can't talk now", "Call you later?"),
    val autoDeleteOtps: Boolean = false
)
