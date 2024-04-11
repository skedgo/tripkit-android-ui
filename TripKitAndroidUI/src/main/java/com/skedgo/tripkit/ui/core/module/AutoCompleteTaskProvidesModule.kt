package com.skedgo.tripkit.ui.core.module

import com.skedgo.tripkit.ui.geocoding.FetchTripGoLocationsImpl
import com.skedgo.tripkit.ui.geocoding.GeocoderLive
import com.skedgo.tripkit.ui.search.FetchTripGoLocations
import com.skedgo.tripkit.ui.utils.TransportModeSharedPreference
import dagger.Module
import dagger.Provides

@Module
class AutoCompleteTaskProvidesModule {
    @Provides
    fun provideTripGoLocations(
        geocoderLive: GeocoderLive,
        transportModeSharedPreference: TransportModeSharedPreference
    ): FetchTripGoLocations {
        return FetchTripGoLocationsImpl(geocoderLive, transportModeSharedPreference)
    }
}