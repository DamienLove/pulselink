package com.pulselink.beacon.ui

import com.pulselink.beacon.data.SmsMessageItem

sealed class BeaconSearchResultState {
    object Idle : BeaconSearchResultState()
    object Searching : BeaconSearchResultState()
    data class Contact(val threadId: Long, val address: String) : BeaconSearchResultState()
    data class Messages(val hits: List<SmsMessageItem>) : BeaconSearchResultState()
    object Empty : BeaconSearchResultState()
}
