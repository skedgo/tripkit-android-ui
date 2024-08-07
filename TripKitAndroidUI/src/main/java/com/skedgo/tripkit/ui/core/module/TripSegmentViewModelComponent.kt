package com.skedgo.tripkit.ui.core.module

import com.skedgo.tripkit.ui.tripresults.TripSegmentViewModel
import dagger.Subcomponent

@Subcomponent(modules = [TransportationIconTintStrategyModule::class])
interface TripSegmentViewModelComponent {
    fun inject(view: TripSegmentViewModel)
}