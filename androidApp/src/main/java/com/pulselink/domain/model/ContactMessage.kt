package com.pulselink.domain.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "contact_messages")
data class ContactMessage(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val contactId: Long,
    val body: String,
    val direction: MessageDirection,
    val timestamp: Long = System.currentTimeMillis(),
    val overrideSucceeded: Boolean = false
)

enum class MessageDirection {
    INBOUND,
    OUTBOUND
}
