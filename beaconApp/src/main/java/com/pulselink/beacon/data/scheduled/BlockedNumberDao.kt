package com.pulselink.beacon.data.scheduled

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface BlockedNumberDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun block(number: BlockedNumber)

    @Query("DELETE FROM blocked_numbers WHERE normalizedNumber = :normalizedNumber")
    suspend fun unblock(normalizedNumber: String)

    @Query("SELECT * FROM blocked_numbers")
    fun getAllBlocked(): Flow<List<BlockedNumber>>

    @Query("SELECT COUNT(*) FROM blocked_numbers WHERE normalizedNumber = :number")
    suspend fun isBlocked(number: String): Int
}
