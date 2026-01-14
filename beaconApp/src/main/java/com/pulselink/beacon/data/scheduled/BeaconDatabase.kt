package com.pulselink.beacon.data.scheduled

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        ScheduledMessage::class,
        BlockedContact::class,
        StarredMessage::class,
        ThreadDraft::class
    ],
    version = 2,
    exportSchema = true
)
abstract class BeaconDatabase : RoomDatabase() {
    abstract fun scheduledMessageDao(): ScheduledMessageDao
    abstract fun extrasDao(): ExtrasDao

    companion object {
        @Volatile
        private var INSTANCE: BeaconDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("CREATE TABLE IF NOT EXISTS `blocked_contacts` (`address` TEXT NOT NULL, `timestamp` INTEGER NOT NULL, PRIMARY KEY(`address`))")
                database.execSQL("CREATE TABLE IF NOT EXISTS `starred_messages` (`messageId` INTEGER NOT NULL, `timestamp` INTEGER NOT NULL, PRIMARY KEY(`messageId`))")
                database.execSQL("CREATE TABLE IF NOT EXISTS `thread_drafts` (`threadId` INTEGER NOT NULL, `text` TEXT NOT NULL, `timestamp` INTEGER NOT NULL, PRIMARY KEY(`threadId`))")
            }
        }

        fun getDatabase(context: Context): BeaconDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    BeaconDatabase::class.java,
                    "beacon_database"
                )
                .addMigrations(MIGRATION_1_2)
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
