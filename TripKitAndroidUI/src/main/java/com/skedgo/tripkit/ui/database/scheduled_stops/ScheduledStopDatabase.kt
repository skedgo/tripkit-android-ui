package com.skedgo.tripkit.ui.database.scheduled_stops

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        ScheduledStopEntity::class,
        LocationEntity::class,
        ScheduledStopDownloadHistoryEntity::class
    ],
    version = 2,
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
                )
                    .addMigrations(MIGRATION_1_2)
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "ALTER TABLE scheduled_stops ADD COLUMN apiZoomLevel INTEGER NOT NULL DEFAULT 0"
                )
            }
        }
    }
}
