package com.skedgo.tripkit.ui.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.skedgo.tripkit.ui.database.location_history.LocationHistoryDao
import com.skedgo.tripkit.ui.database.location_history.LocationHistoryEntity

@Database(
    entities = [LocationHistoryEntity::class],
    version = 1
)
abstract class TripKitUiDatabase : RoomDatabase() {
    abstract fun locationHistoryDao(): LocationHistoryDao

    companion object {
        fun getInstance(context: Context): TripKitUiDatabase {
            return Room.databaseBuilder(
                context.applicationContext,
                TripKitUiDatabase::class.java, "tripkitui.db"
            )
                .fallbackToDestructiveMigrationFrom(1)
                .build()
        }
    }


}