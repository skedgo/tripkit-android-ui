package com.skedgo.tripkit.ui.core.module

import android.content.Context
import com.skedgo.tripkit.data.database.DatabaseMigrator
import com.skedgo.tripkit.data.database.DbHelper
import com.skedgo.tripkit.data.database.TripKitDatabase
import com.skedgo.tripkit.data.database.timetables.ScheduledServiceRealtimeInfoDao
import com.skedgo.tripkit.ui.database.TripKitUiDatabase
import com.skedgo.tripkit.ui.database.location_history.LocationHistoryDao
import dagger.Module
import dagger.Provides
import skedgo.tripgo.data.timetables.ParentStopDao
import javax.inject.Singleton

@Module
class DbHelperModule {
    @Provides
    @Singleton
    internal fun dbHelper(
        context: Context,
        tripKitDatabase: TripKitDatabase
    ): DbHelper {
        return DbHelper(
            context,
            // Although this is called "tripkit-legacy.db", the tables come from the original TripGo, and were imported
            // to be used by TripKitUI customers.
            "tripkit-legacy.db",
            DatabaseMigrator(tripKitDatabase)
        )
    }

    @Provides
    @Singleton
    internal fun tripKitDatabase(context: Context): TripKitDatabase {
        return TripKitDatabase.getInstance(context)
    }

    @Provides
    @Singleton
    internal fun tripKitUiDatabase(context: Context): TripKitUiDatabase {
        return TripKitUiDatabase.getInstance(context)
    }

    @Provides
    internal fun locationHistoryDao(tripKitUiDatabase: TripKitUiDatabase): LocationHistoryDao {
        return tripKitUiDatabase.locationHistoryDao()
    }

    @Provides
    internal fun scheduledServiceRealtimeInfoDao(
        tripKitDatabase: TripKitDatabase
    ): ScheduledServiceRealtimeInfoDao {
        return tripKitDatabase.scheduledServiceRealtimeInfoDao()
    }


    @Provides
    internal fun parentStopDao(tripKitDatabase: TripKitDatabase): ParentStopDao {
        return tripKitDatabase.parentStopDao()
    }


}