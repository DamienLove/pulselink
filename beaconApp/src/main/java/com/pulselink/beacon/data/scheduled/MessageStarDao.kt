package com.pulselink.beacon.data.scheduled

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface MessageStarDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(star: MessageStar)

    @Query("DELETE FROM message_stars WHERE messageId = :messageId")
    suspend fun remove(messageId: Long)

    @Query("SELECT * FROM message_stars")
    fun getAllStarred(): Flow<List<MessageStar>>

    @Query("SELECT messageId FROM message_stars WHERE threadId = :threadId")
    fun getStarredMessageIdsInThread(threadId: Long): Flow<List<Long>>

    @Query("SELECT COUNT(*) FROM message_stars WHERE threadId = :threadId")
    suspend fun getStarredCountInThread(threadId: Long): Int
}
