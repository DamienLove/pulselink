package com.pulselink.data.db

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.pulselink.domain.model.AlertEvent
import com.pulselink.domain.model.BlockedContact
import com.pulselink.domain.model.Contact
import com.pulselink.domain.model.ContactMessage
import kotlinx.coroutines.flow.Flow

@Dao
interface ContactDao {
    @Query("SELECT * FROM contacts ORDER BY contactOrder ASC, displayName COLLATE NOCASE")
    fun observeContacts(): Flow<List<Contact>>

    @Query("SELECT * FROM contacts")
    suspend fun getAll(): List<Contact>

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

    @Query("SELECT * FROM contacts WHERE remoteUid = :remoteUid LIMIT 1")
    suspend fun getByRemoteUid(remoteUid: String): Contact?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(contact: Contact)

    @Query("DELETE FROM contacts WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM contacts")
    suspend fun clear()

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
    version = 6,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class PulseLinkDatabase : RoomDatabase() {
    abstract fun contactDao(): ContactDao
    abstract fun alertEventDao(): AlertEventDao
    abstract fun contactMessageDao(): ContactMessageDao
    abstract fun blockedContactDao(): BlockedContactDao

    companion object {
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE alert_events ADD COLUMN contactId INTEGER")
                database.execSQL("ALTER TABLE alert_events ADD COLUMN contactName TEXT")
                database.execSQL("ALTER TABLE alert_events ADD COLUMN isIncoming INTEGER NOT NULL DEFAULT 0")
                database.execSQL("ALTER TABLE alert_events ADD COLUMN soundKey TEXT")
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE contacts ADD COLUMN remoteUid TEXT")
            }
        }

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE contacts ADD COLUMN remoteLastSeen INTEGER")
                database.execSQL("ALTER TABLE contacts ADD COLUMN remotePresence TEXT NOT NULL DEFAULT 'UNKNOWN'")
            }
        }
    }
}
