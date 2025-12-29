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
import com.pulselink.domain.model.ArchivedThread
import com.pulselink.domain.model.Contact
import com.pulselink.domain.model.ContactMessage
import com.pulselink.domain.model.MessageStatus
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

    @Query("SELECT * FROM contacts WHERE email = :email LIMIT 1")
    suspend fun getByEmail(email: String?): Contact?

    @Query("SELECT * FROM contacts WHERE remoteDeviceId = :deviceId LIMIT 1")
    suspend fun getByRemoteDeviceId(deviceId: String): Contact?

    @Query("SELECT * FROM contacts WHERE remoteUid = :remoteUid LIMIT 1")
    suspend fun getByRemoteUid(remoteUid: String): Contact?

    @Query("SELECT * FROM contacts WHERE linkStatus = 'LINKED' ORDER BY contactOrder ASC, displayName COLLATE NOCASE")
    suspend fun getLinkedContacts(): List<Contact>

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

    @Query("SELECT COUNT(*) FROM alert_events WHERE isRead = 0")
    fun observeUnreadCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(event: AlertEvent)

    @Query("UPDATE alert_events SET isRead = 1 WHERE id IN (:ids)")
    suspend fun markAsRead(ids: List<Long>)

    @Query("DELETE FROM alert_events")
    suspend fun clear()
}

@Dao
interface ContactMessageDao {
    @Query("SELECT * FROM contact_messages WHERE contactId = :contactId ORDER BY timestamp DESC")
    fun observeForContact(contactId: Long): Flow<List<ContactMessage>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(message: ContactMessage)

    @Query("UPDATE contact_messages SET status = :status WHERE messageId = :messageId")
    suspend fun updateStatus(messageId: String, status: MessageStatus)

    @Query("DELETE FROM contact_messages WHERE contactId = :contactId")
    suspend fun clear(contactId: Long)

    @Query("SELECT COUNT(*) FROM contact_messages WHERE contactId IN (:contactIds) AND direction = 'INBOUND' AND timestamp > :since")
    suspend fun getUnreadEmergencyCount(contactIds: List<Long>, since: Long): Int
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

@Dao
interface ArchivedThreadDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(entry: ArchivedThread)

    @Query("DELETE FROM archived_threads WHERE threadId = :threadId")
    fun deleteByThreadId(threadId: Long)

    @Query("SELECT threadId FROM archived_threads")
    fun getAllIds(): List<Long>

    @Query("SELECT EXISTS(SELECT 1 FROM archived_threads WHERE threadId = :threadId)")
    fun isArchived(threadId: Long): Boolean
}

@Database(
    entities = [Contact::class, AlertEvent::class, ContactMessage::class, BlockedContact::class, ArchivedThread::class],
    version = 15,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class PulseLinkDatabase : RoomDatabase() {
    abstract fun contactDao(): ContactDao
    abstract fun alertEventDao(): AlertEventDao
    abstract fun contactMessageDao(): ContactMessageDao
    abstract fun blockedContactDao(): BlockedContactDao
    abstract fun archivedThreadDao(): ArchivedThreadDao

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

        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE contacts ADD COLUMN email TEXT")
            }
        }

        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE contacts ADD COLUMN additionalPhones TEXT")
                database.execSQL("ALTER TABLE contacts ADD COLUMN additionalEmails TEXT")
            }
        }

        val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE contacts ADD COLUMN isPrivate INTEGER NOT NULL DEFAULT 0")
            }
        }

        val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS archived_threads (
                        threadId INTEGER PRIMARY KEY,
                        archivedAt INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }

        val MIGRATION_10_11 = object : Migration(10, 11) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // No schema changes; version bump only.
            }
        }

        val MIGRATION_11_12 = object : Migration(11, 12) {
            override fun migrate(database: SupportSQLiteDatabase) {
                addColumnIfMissing(database, "contacts", "fcmToken", "TEXT")
                addColumnIfMissing(database, "contacts", "preferredChannel", "TEXT")
                addColumnIfMissing(database, "contacts", "lastSuccessfulChannel", "TEXT")
                addColumnIfMissing(database, "alert_events", "isRead", "INTEGER NOT NULL DEFAULT 0")
            }
        }

        val MIGRATION_12_13 = object : Migration(12, 13) {
            override fun migrate(database: SupportSQLiteDatabase) {
                addColumnIfMissing(database, "contacts", "isFavorite", "INTEGER NOT NULL DEFAULT 0")
                addColumnIfMissing(database, "contacts", "themeOverride", "TEXT")
                addColumnIfMissing(database, "contacts", "avatarUrl", "TEXT")
                addColumnIfMissing(database, "contacts", "remoteDisplayName", "TEXT")
            }
        }

        val MIGRATION_13_14 = object : Migration(13, 14) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("UPDATE contacts SET allowRemoteOverride = 1 WHERE allowRemoteOverride = 0")
            }
        }

        val MIGRATION_14_15 = object : Migration(14, 15) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "ALTER TABLE contact_messages ADD COLUMN messageId TEXT NOT NULL DEFAULT ''"
                )
                database.execSQL(
                    "ALTER TABLE contact_messages ADD COLUMN status TEXT NOT NULL DEFAULT 'SENT'"
                )
                database.execSQL(
                    "UPDATE contact_messages SET messageId = CASE WHEN messageId = '' THEN CAST(id AS TEXT) ELSE messageId END"
                )
                database.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_contact_messages_messageId ON contact_messages(messageId)"
                )
            }
        }

        val ALL_MIGRATIONS = arrayOf(
            MIGRATION_3_4,
            MIGRATION_4_5,
            MIGRATION_5_6,
            MIGRATION_6_7,
            MIGRATION_7_8,
            MIGRATION_8_9,
            MIGRATION_9_10,
            MIGRATION_10_11,
            MIGRATION_11_12,
            MIGRATION_12_13,
            MIGRATION_13_14,
            MIGRATION_14_15
        )

        private fun addColumnIfMissing(
            database: SupportSQLiteDatabase,
            table: String,
            column: String,
            definition: String
        ) {
            if (!columnExists(database, table, column)) {
                database.execSQL("ALTER TABLE $table ADD COLUMN $column $definition")
            }
        }

        private fun columnExists(
            database: SupportSQLiteDatabase,
            table: String,
            column: String
        ): Boolean {
            database.query("PRAGMA table_info(`$table`)").use { cursor ->
                val nameIndex = cursor.getColumnIndex("name")
                while (cursor.moveToNext()) {
                    if (cursor.getString(nameIndex).equals(column, ignoreCase = true)) {
                        return true
                    }
                }
            }
            return false
        }

    }
}
