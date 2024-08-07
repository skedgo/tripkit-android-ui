package com.skedgo.tripkit.ui.favorites.trips

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [FavoriteTripEntity::class], version = 1)
abstract class FavoriteTripsDataBase : RoomDatabase() {

    abstract fun favoriteTripsDao(): FavoriteTripsDao

    companion object {
        fun getInstance(context: Context): FavoriteTripsDataBase {
            return Room.databaseBuilder(
                context.applicationContext,
                FavoriteTripsDataBase::class.java, "favorite-trips.db"
            )
                .build()
        }
    }
}