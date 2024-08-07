package com.skedgo.tripkit.ui.core.module

import android.content.Context
import com.skedgo.tripkit.logging.ErrorLogger
import com.skedgo.tripkit.ui.timetables.GetRealtimeText
import com.skedgo.tripkit.ui.timetables.GetServiceSubTitleText
import com.skedgo.tripkit.ui.timetables.GetServiceTertiaryText
import com.skedgo.tripkit.ui.timetables.GetServiceTitleText
import com.skedgo.tripkit.ui.timetables.ServiceViewModel
import com.skedgo.tripkit.ui.timetables.ServiceViewModelImpl
import com.skedgo.tripkit.ui.trip.details.viewmodel.OccupancyViewModel
import com.skedgo.tripkit.ui.trip.details.viewmodel.ServiceAlertViewModel
import dagger.Module
import dagger.Provides

@Module
class ServiceViewModelModule {
    @Provides
    internal fun provideServiceViewModel(
        context: Context,
        occupancyViewModel: OccupancyViewModel,
        timetableEntryServiceViewModel: ServiceAlertViewModel,
        getServiceTitleText: GetServiceTitleText,
        getServiceSubTitleText: GetServiceSubTitleText,
        getServiceTertiaryText: GetServiceTertiaryText,
        getRealtimeText: GetRealtimeText,
        errorLogger: ErrorLogger
    ): ServiceViewModel = ServiceViewModelImpl(
        context,
        occupancyViewModel,
        timetableEntryServiceViewModel,
        getServiceTitleText,
        getServiceSubTitleText,
        getServiceTertiaryText,
        getRealtimeText,
        errorLogger
    )


}
