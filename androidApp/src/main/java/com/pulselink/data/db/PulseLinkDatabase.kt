package com.pulselink.data.db

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.pulselink.domain.model.AlertEvent
import com.pulselink.domain.model.BlockedContact
import com.pulselink.domain.model.Contact
import com.pulselink.domain.model.ContactMessage
import kotlinx.coroutines.flow.Flow

@Dao
interface ContactDao {
    @Query("SELECT * FROM contacts ORDER BY contactOrder ASC, displayName COLLATE NOCASE")
    fun observeContacts(): Flow<List<Contact>>

    @Query("SELECT * FROM contacts WHERE escalationTier = :tier ORDER BY contactOrder ASC, displayName COLLATE NOCASE")
    suspend fun getByTier(tier: String): List<Contact>

    @Query("SELECT * FROM contacts WHERE id = :contactId LIMIT 1")
    suspend fun getById(contactId: Long): Contact?

    @Query("SELECT * FROM contacts WHERE linkCode = :code LIMIT 1")
    suspend fun getByLinkCode(code: String): Contact?

    @Query("SELECT * FROM contacts WHERE phoneNumber = :phone LIMIT 1")
    suspend fun getByPhone(phone: String): Contact?

    @Query("SELECT * FROM contacts WHERE remoteDeviceId = :deviceId LIMIT 1")
    suspend fun getByRemoteDeviceId(deviceId: String): Contact?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(contact: Contact)

    @Query("DELETE FROM contacts WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("UPDATE contacts SET contactOrder = :order WHERE id = :contactId")
    suspend fun updateOrder(contactId: Long, order: Int)
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

@Dao
interface ContactMessageDao {
    @Query("SELECT * FROM contact_messages WHERE contactId = :contactId ORDER BY timestamp ASC")
    fun observeForContact(contactId: Long): Flow<List<ContactMessage>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(message: ContactMessage)

    @Query("DELETE FROM contact_messages WHERE contactId = :contactId")
    suspend fun clear(contactId: Long)
}

@Dao
interface BlockedContactDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(blockedContact: BlockedContact)

    @Query(
        """
        SELECT EXISTS(
            SELECT 1 FROM blocked_contacts
            WHERE (:phoneNumber IS NOT NULL AND phoneNumber = :phoneNumber)
               OR (:linkCode IS NOT NULL AND linkCode = :linkCode)
               OR (:remoteDeviceId IS NOT NULL AND remoteDeviceId = :remoteDeviceId)
        )
        """
    )
    suspend fun isBlocked(phoneNumber: String?, linkCode: String?, remoteDeviceId: String?): Boolean

    @Query("SELECT * FROM blocked_contacts WHERE phoneNumber = :phone LIMIT 1")
    suspend fun getByPhone(phone: String): BlockedContact?

    @Query("SELECT * FROM blocked_contacts WHERE linkCode = :code LIMIT 1")
    suspend fun getByLinkCode(code: String): BlockedContact?

    @Query("DELETE FROM blocked_contacts WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("SELECT * FROM blocked_contacts ORDER BY blockedAt DESC")
    fun observeAll(): Flow<List<BlockedContact>>
}

@Database(
    entities = [Contact::class, AlertEvent::class, ContactMessage::class, BlockedContact::class],
    version = 3,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class PulseLinkDatabase : RoomDatabase() {
    abstract fun contactDao(): ContactDao
    abstract fun alertEventDao(): AlertEventDao
    abstract fun contactMessageDao(): ContactMessageDao
    abstract fun blockedContactDao(): BlockedContactDao
}
