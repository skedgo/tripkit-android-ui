package com.skedgo.tripkit.ui.core.module

import android.content.Context
import com.skedgo.tripkit.ui.timetables.*
import com.skedgo.tripkit.ui.trip.details.viewmodel.OccupancyViewModel
import com.skedgo.tripkit.ui.trip.details.viewmodel.ServiceAlertViewModel
import dagger.Module
import dagger.Provides
import com.skedgo.tripkit.logging.ErrorLogger

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
  ): ServiceViewModel = ServiceViewModelImpl(context,
          occupancyViewModel,
          timetableEntryServiceViewModel,
          getServiceTitleText,
          getServiceSubTitleText,
          getServiceTertiaryText,
          getRealtimeText,
          errorLogger)


}
