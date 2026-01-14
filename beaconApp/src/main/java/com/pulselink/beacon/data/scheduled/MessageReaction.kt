package com.pulselink.beacon.data.scheduled

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Index

@Entity(
    tableName = "message_reactions",
    indices = [Index(value = ["messageId"])]
)
data class MessageReaction(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val messageId: Long,
    val emoji: String,
    val timestamp: Long = System.currentTimeMillis()
)
