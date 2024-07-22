package com.skedgo.tripkit.ui.favorites.v2.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * Database to handle saving favorites from server to local storage
 * This is to use an aligned data class from backend [FavoriteEntityv2]
 * to have all the favorites (home, work, trip, etc.) to use one data class
 */

const val DATABASE_TRIPS = "favorites.db"
const val DATABASE_TRIPS_VERSION = 2

@Database(
    entities = [FavoriteV2::class],
    version = DATABASE_TRIPS_VERSION
)
abstract class FavoritesDatabase : RoomDatabase() {

    abstract fun favoriteDao(): FavoriteDaoV2

    companion object {
        fun getInstance(context: Context): FavoritesDatabase {
            return Room.databaseBuilder(
                context.applicationContext,
                FavoritesDatabase::class.java, DATABASE_TRIPS
            ).build()
        }
    }
}