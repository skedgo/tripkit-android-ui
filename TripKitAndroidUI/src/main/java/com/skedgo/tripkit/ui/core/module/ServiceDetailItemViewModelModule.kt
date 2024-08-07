package com.skedgo.tripkit.ui.core.module

import com.skedgo.tripkit.data.regions.RegionService
import com.skedgo.tripkit.datetime.PrintTime
import com.skedgo.tripkit.ui.servicedetail.GetStopTimeDisplayText
import com.skedgo.tripkit.ui.servicedetail.ServiceDetailItemViewModel
import dagger.Module
import dagger.Provides

@Module
class ServiceDetailItemViewModelModule {
    @Provides
    internal fun provideServiceDetailItemViewModel(
        regionService: RegionService,
        printTime: PrintTime
    )
        : ServiceDetailItemViewModel =
        ServiceDetailItemViewModel(GetStopTimeDisplayText(regionService, printTime))


}
