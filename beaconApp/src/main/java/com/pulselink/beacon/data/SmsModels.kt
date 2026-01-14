package com.pulselink.beacon.data

enum class ThreadCategory {
    PERSONAL, TRANSACTIONS, PROMOTIONS
}

data class SmsThreadItem(
    val threadId: Long,
    val address: String,
    val snippet: String,
    val timestamp: Long,
    val unread: Boolean,
    val isPinned: Boolean = false,
    val isArchived: Boolean = false,
    val category: ThreadCategory = ThreadCategory.PERSONAL
)

data class SmsMessageItem(
    val id: Long,
    val threadId: Long,
    val address: String,
    val body: String,
    val timestamp: Long,
    val outgoing: Boolean,
    val isMms: Boolean = false,
    val mediaParts: List<MmsPart> = emptyList()
)

data class MmsPart(
    val contentType: String,
    val text: String? = null,
    val dataUri: android.net.Uri? = null
)

data class InboxState(
    val pinnedThreadIds: Set<Long> = emptySet(),
    val archivedThreadIds: Set<Long> = emptySet(),
    val delayedSendTimeout: Int = 5
)
