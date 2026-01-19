package com.pulselink.beacon.data.scheduled

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

import androidx.room.AutoMigration

@Database(
    entities = [
        ScheduledMessage::class,
        MessageReaction::class,
        MessageStar::class,
        BlockedNumber::class,
        ThreadDraft::class
    ],
    version = 4,
    exportSchema = true,
    autoMigrations = [
        AutoMigration(from = 1, to = 2),
        AutoMigration(from = 2, to = 3),
        AutoMigration(from = 3, to = 4)
    ]
)
abstract class BeaconDatabase : RoomDatabase() {
    abstract fun scheduledMessageDao(): ScheduledMessageDao
    abstract fun reactionDao(): MessageReactionDao
    abstract fun messageStarDao(): MessageStarDao
    abstract fun blockedNumberDao(): BlockedNumberDao
    abstract fun threadDraftDao(): ThreadDraftDao

    companion object {
        @Volatile
        private var INSTANCE: BeaconDatabase? = null

        fun getDatabase(context: Context): BeaconDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    BeaconDatabase::class.java,
                    "beacon_database"
                )
                // Fallback is still good to have if migration fails for some reason
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
