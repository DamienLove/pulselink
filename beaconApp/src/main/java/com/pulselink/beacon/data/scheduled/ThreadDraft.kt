package com.pulselink.beacon.data.scheduled

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "thread_drafts")
data class ThreadDraft(
    @PrimaryKey val threadId: Long,
    val body: String,
    val updatedAt: Long
)
