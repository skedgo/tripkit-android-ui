package com.skedgo.tripkit.ui.favorites.v2.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.skedgo.tripkit.ui.favorites.waypoints.WaypointEntity
import com.skedgo.tripkit.ui.favorites.waypoints.WaypointsDao

/**
 * Database to handle saving favorites from server to local storage
 * This is to use an aligned data class from backend [FavoriteEntityv2]
 * to have all the favorites (home, work, trip, etc.) to use one data class
 */

const val DATABASE_TRIPS = "favorites.db"
const val DATABASE_TRIPS_VERSION = 3

@Database(
    entities = [FavoriteV2::class, WaypointEntity::class],
    version = DATABASE_TRIPS_VERSION
)
abstract class FavoritesDatabase : RoomDatabase() {

    abstract fun favoriteDao(): FavoriteDaoV2
    abstract fun waypointDao(): WaypointsDao

    companion object {
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE favorites_v2 ADD COLUMN stopName TEXT")
                database.execSQL("ALTER TABLE favorites_v2 ADD COLUMN isUserCustomName INTEGER NOT NULL DEFAULT 0")
                database.execSQL("UPDATE favorites_v2 SET stopName = name WHERE type = 'stop'")
            }
        }

        fun getInstance(context: Context): FavoritesDatabase {
            return Room.databaseBuilder(
                context.applicationContext,
                FavoritesDatabase::class.java, DATABASE_TRIPS
            )
                .addMigrations(MIGRATION_2_3)
                .build()
        }
    }
}