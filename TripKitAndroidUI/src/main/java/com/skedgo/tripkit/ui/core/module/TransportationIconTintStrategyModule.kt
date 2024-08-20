package com.skedgo.tripkit.ui.core.module

import android.content.res.Resources
import com.skedgo.tripkit.ui.tripresults.GetTransportIconTintStrategy
import dagger.Module
import dagger.Provides

@Module
class TransportationIconTintStrategyModule {
    @Provides
    fun transportationIconTintStrategy(resources: Resources)
        : GetTransportIconTintStrategy = GetTransportIconTintStrategy(resources)
}