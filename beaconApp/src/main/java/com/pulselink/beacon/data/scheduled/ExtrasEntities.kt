package com.pulselink.beacon.data.scheduled

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query

@Entity(tableName = "blocked_contacts")
data class BlockedContact(
    @PrimaryKey val address: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "starred_messages")
data class StarredMessage(
    @PrimaryKey val messageId: Long,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "thread_drafts")
data class ThreadDraft(
    @PrimaryKey val threadId: Long,
    val text: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Dao
interface ExtrasDao {
    // Blocked Contacts
    @Query("SELECT * FROM blocked_contacts")
    suspend fun getAllBlockedContacts(): List<BlockedContact>

    @Query("SELECT COUNT(*) FROM blocked_contacts WHERE address = :address")
    suspend fun isBlocked(address: String): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun blockContact(contact: BlockedContact)

    @Query("DELETE FROM blocked_contacts WHERE address = :address")
    suspend fun unblockContact(address: String)

    // Starred Messages
    @Query("SELECT messageId FROM starred_messages")
    suspend fun getAllStarredMessageIds(): List<Long>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun starMessage(message: StarredMessage)

    @Query("DELETE FROM starred_messages WHERE messageId = :messageId")
    suspend fun unstarMessage(messageId: Long)

    // Drafts
    @Query("SELECT text FROM thread_drafts WHERE threadId = :threadId")
    suspend fun getDraft(threadId: Long): String?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveDraft(draft: ThreadDraft)

    @Query("DELETE FROM thread_drafts WHERE threadId = :threadId")
    suspend fun deleteDraft(threadId: Long)
}
