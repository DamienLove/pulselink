package com.pulselink.data.db

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RoomDatabase
import com.pulselink.domain.model.AlertEvent
import com.pulselink.domain.model.Contact
import kotlinx.coroutines.flow.Flow

@Dao
interface ContactDao {
    @Query("SELECT * FROM contacts ORDER BY displayName")
    fun observeContacts(): Flow<List<Contact>>

    @Query("SELECT * FROM contacts WHERE escalationTier = :tier ORDER BY displayName")
    suspend fun getByTier(tier: String): List<Contact>

    @Query("SELECT * FROM contacts WHERE id = :contactId LIMIT 1")
    suspend fun getById(contactId: Long): Contact?

    @Query("SELECT * FROM contacts WHERE linkCode = :code LIMIT 1")
    suspend fun getByLinkCode(code: String): Contact?

    @Query("SELECT * FROM contacts WHERE phoneNumber = :phone LIMIT 1")
    suspend fun getByPhone(phone: String): Contact?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(contact: Contact)

    @Query("DELETE FROM contacts WHERE id = :id")
    suspend fun deleteById(id: Long)
}

@Dao
interface AlertEventDao {
    @Query("SELECT * FROM alert_events ORDER BY timestamp DESC LIMIT :limit")
    fun observeRecent(limit: Int): Flow<List<AlertEvent>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(event: AlertEvent)

    @Query("DELETE FROM alert_events")
    suspend fun clear()
}

@Database(
    entities = [Contact::class, AlertEvent::class],
    version = 1,
    exportSchema = true
)
abstract class PulseLinkDatabase : RoomDatabase() {
    abstract fun contactDao(): ContactDao
    abstract fun alertEventDao(): AlertEventDao
}
