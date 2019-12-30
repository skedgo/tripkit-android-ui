package com.skedgo.tripkit.ui.core.module
import android.content.Context
import com.skedgo.tripkit.data.database.DatabaseMigrator
import com.skedgo.tripkit.data.database.DbHelper
import com.skedgo.tripkit.data.database.TripKitDatabase
import com.skedgo.tripkit.data.database.timetables.ScheduledServiceRealtimeInfoDao
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
          tripKitDatabase: TripKitDatabase): DbHelper {
    return DbHelper(
            context,
            "tripkit-legacy.db",
            DatabaseMigrator(tripKitDatabase))
  }

  @Provides
  @Singleton
  internal fun tripKitDatabase(context: Context): TripKitDatabase {
    return TripKitDatabase.getInstance(context)
  }

  @Provides
  internal fun scheduledServiceRealtimeInfoDao(
          tripKitDatabase: TripKitDatabase): ScheduledServiceRealtimeInfoDao {
    return tripKitDatabase.scheduledServiceRealtimeInfoDao()
  }


  @Provides
  internal fun parentStopDao(tripKitDatabase: TripKitDatabase): ParentStopDao {
    return tripKitDatabase.parentStopDao()
  }

}