package com.pulselink.beacon.ui

import com.pulselink.beacon.data.SmsMessageItem

sealed class UiSearchResultState {
    object Idle : UiSearchResultState()
    object Searching : UiSearchResultState()
    data class Contact(val threadId: Long, val address: String) : UiSearchResultState()
    data class Messages(val hits: List<SmsMessageItem>) : UiSearchResultState()
    object Empty : UiSearchResultState()
}
