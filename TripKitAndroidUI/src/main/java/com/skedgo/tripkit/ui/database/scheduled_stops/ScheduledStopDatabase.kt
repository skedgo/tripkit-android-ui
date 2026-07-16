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
    version = 4,
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

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    DELETE FROM locations
                    WHERE scheduledStopCode IS NOT NULL
                    AND id NOT IN (
                        SELECT MAX(id)
                        FROM locations
                        WHERE scheduledStopCode IS NOT NULL
                        GROUP BY scheduledStopCode
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    DELETE FROM locations
                    WHERE scheduledStopCode IS NULL
                    OR scheduledStopCode NOT IN (SELECT code FROM scheduled_stops)
                    """.trimIndent()
                )
                db.execSQL("DROP INDEX IF EXISTS index_locations_scheduledStopCode")
                db.execSQL(
                    "CREATE UNIQUE INDEX index_locations_scheduledStopCode ON locations(scheduledStopCode)"
                )
            }
        }

        fun getDatabase(context: Context): ScheduledStopDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ScheduledStopDatabase::class.java,
                    "scheduled_stop_database"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_3_4)
                    .fallbackToDestructiveMigration(true)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
