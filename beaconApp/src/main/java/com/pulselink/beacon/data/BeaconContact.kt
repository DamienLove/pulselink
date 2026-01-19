package com.pulselink.beacon.data

import android.net.Uri

data class BeaconContact(
    val id: Long,
    val lookupKey: String,
    val displayName: String,
    val phoneNumber: String,
    val photoUri: String?
)
