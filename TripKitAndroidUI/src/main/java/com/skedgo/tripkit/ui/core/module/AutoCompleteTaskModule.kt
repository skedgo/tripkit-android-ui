package com.skedgo.tripkit.ui.core.module

import com.skedgo.tripkit.ui.geocoding.*
import com.skedgo.tripkit.ui.search.FetchFoursquareLocations
import com.skedgo.tripkit.ui.search.FetchGoogleLocations
import com.skedgo.tripkit.ui.search.FetchLocalLocations
import dagger.Binds
import dagger.Module

/**
 * Defines components for the search list
 */
@Module
abstract class AutoCompleteTaskModule {
  @Binds
  abstract fun filterSupportedLocations(
      impl: FilterSupportedLocationsImpl
  ): FilterSupportedLocations

  @Binds
  abstract fun fetchFoursquareLocations(
      impl: FetchFoursquareLocationsImpl
  ): FetchFoursquareLocations

  @Binds
  abstract fun fetchGoogleLocations(
      impl: FetchGoogleLocationsImpl
  ): FetchGoogleLocations

  @Binds
  abstract fun fetchLocalLocations(
      impl: FetchLocalLocationsImpl
  ): FetchLocalLocations

  @Binds
  abstract fun resultAggregator(
      impl: ResultAggregatorImpl
  ): ResultAggregator
}
