package com.pulselink.beacon.data.scheduled

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface MessageReactionDao {
    @Query("SELECT * FROM message_reactions WHERE messageId IN (:messageIds)")
    fun getReactionsForMessages(messageIds: List<Long>): Flow<List<MessageReaction>>

    @Insert
    suspend fun insert(reaction: MessageReaction)

    @Query("DELETE FROM message_reactions WHERE messageId = :messageId AND emoji = :emoji")
    suspend fun removeReaction(messageId: Long, emoji: String)
}
