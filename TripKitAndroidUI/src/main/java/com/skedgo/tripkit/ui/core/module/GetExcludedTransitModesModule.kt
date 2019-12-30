package com.skedgo.tripkit.ui.core.module

import com.skedgo.tripkit.TransitModeFilter
import com.skedgo.tripkit.ui.core.modeprefs.IsTransitModeIncludedRepository
import com.skedgo.tripkit.ui.tripresults.GetExcludedTransitModes
import dagger.Module
import dagger.Provides

@Module
class GetExcludedTransitModesModule {
  @Provides
  internal fun excludedTransitModesAdapter(
      isTransitModeIncludedRepository: IsTransitModeIncludedRepository
  ): TransitModeFilter =
      GetExcludedTransitModes(isTransitModeIncludedRepository)
}