package com.skedgo.tripkit.ui.database.scheduled_stops

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        ScheduledStopEntity::class,
        LocationEntity::class,
        ScheduledStopDownloadHistoryEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class ScheduledStopDatabase : RoomDatabase() {
    abstract fun scheduledStopDao(): ScheduledStopDao

    companion object {
        @Volatile
        private var INSTANCE: ScheduledStopDatabase? = null

        fun getDatabase(context: Context): ScheduledStopDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ScheduledStopDatabase::class.java,
                    "scheduled_stop_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
