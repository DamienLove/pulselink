package com.pulselink.beacon.data.scheduled

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "message_stars")
data class MessageStar(
    @PrimaryKey val messageId: Long,
    val threadId: Long, // Denormalized for easier querying of threads with starred messages
    val timestamp: Long = System.currentTimeMillis()
)
