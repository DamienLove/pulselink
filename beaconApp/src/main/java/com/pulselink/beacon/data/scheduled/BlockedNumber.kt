package com.pulselink.beacon.data.scheduled

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "blocked_numbers")
data class BlockedNumber(
    @PrimaryKey val normalizedNumber: String, // E.164 or simple normalized
    val originalNumber: String,
    val timestamp: Long = System.currentTimeMillis()
)
