package com.skedgo.tripkit.ui.database.scheduled_stops

import android.content.Context
import androidx.room.Database
import androidx.room.migration.Migration
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        ScheduledStopEntity::class,
        LocationEntity::class,
        ScheduledStopDownloadHistoryEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class ScheduledStopDatabase : RoomDatabase() {
    abstract fun scheduledStopDao(): ScheduledStopDao

    companion object {
        @Volatile
        private var INSTANCE: ScheduledStopDatabase? = null
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // No schema change required; keep explicit migration to avoid 1 -> 2 crash.
            }
        }

        fun getDatabase(context: Context): ScheduledStopDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ScheduledStopDatabase::class.java,
                    "scheduled_stop_database"
                )
                    .addMigrations(MIGRATION_1_2)
                    .fallbackToDestructiveMigration(true)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
