package com.pulselink.beacon.ui

import com.pulselink.beacon.data.SmsMessageItem

data class Reaction(
    val emoji: String,
    val senderAddress: String,
    val isMe: Boolean
)

sealed class ThreadUiItem {
    data class Message(val message: SmsMessageItem, val reactions: List<Reaction> = emptyList()) : ThreadUiItem()
    data class DateHeader(val date: String) : ThreadUiItem()
}
