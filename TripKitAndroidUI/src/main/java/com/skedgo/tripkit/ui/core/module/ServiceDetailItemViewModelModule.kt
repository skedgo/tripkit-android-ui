package com.skedgo.tripkit.ui.core.module

import com.skedgo.tripkit.data.regions.RegionService
import com.skedgo.tripkit.datetime.PrintTime
import com.skedgo.tripkit.ui.servicedetail.GetStopTimeDisplayText
import com.skedgo.tripkit.ui.servicedetail.ServiceDetailItemViewModel
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
class ServiceDetailItemViewModelModule {
    @Provides
    internal fun provideServiceDetailItemViewModel(
        regionService: RegionService,
        printTime: PrintTime
    )
        : ServiceDetailItemViewModel =
        ServiceDetailItemViewModel(GetStopTimeDisplayText(regionService, printTime))


}
