package com.pulselink.beacon.data.scheduled

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ThreadDraftDao {
    @Query("SELECT * FROM thread_drafts")
    fun getAllDrafts(): Flow<List<ThreadDraft>>

    @Query("SELECT * FROM thread_drafts WHERE threadId = :threadId")
    suspend fun getDraft(threadId: Long): ThreadDraft?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDraft(draft: ThreadDraft)

    @Query("DELETE FROM thread_drafts WHERE threadId = :threadId")
    suspend fun deleteDraft(threadId: Long)
}
