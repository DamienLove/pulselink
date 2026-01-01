package com.pulselink.data.sms

data class SmsThreadItem(
    val threadId: Long,
    val address: String,
    val snippet: String,
    val timestamp: Long,
    val unread: Boolean,
    val unreadCount: Int = 0,
    val isPrivate: Boolean = false,
    val isFavorite: Boolean = false,
    val isTrusted: Boolean = false,
    val trustedUrgency: com.pulselink.domain.model.MessageUrgency? = null,
    val isOtp: Boolean = false,
    val lineId: String? = null
)

data class SmsMessageItem(
    val id: Long,
    val threadId: Long,
    val address: String,
    val body: String,
    val timestamp: Long,
    val outgoing: Boolean,
    val status: SmsMessageStatus? = null
)

enum class SmsMessageStatus {
    SENDING,
    SENT,
    DELIVERED,
    RECEIVED,
    READ,
    FAILED
}
