package com.pulselink.beacon.data.scheduled

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

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

        fun getDatabase(context: Context): BeaconDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    BeaconDatabase::class.java,
                    "beacon_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
