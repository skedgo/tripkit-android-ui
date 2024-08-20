package com.skedgo.tripkit.ui.core.module

import com.skedgo.tripkit.data.database.TripKitDatabase
import com.skedgo.tripkit.data.database.timetables.ServiceAlertsDao
import dagger.Module
import dagger.Provides

@Module
class ServiceAlertDataModule {
    @Provides
    internal fun serviceAlertsDao(tripKitDatabase: TripKitDatabase): ServiceAlertsDao {
        return tripKitDatabase.serviceAlertsDao()
    }
}