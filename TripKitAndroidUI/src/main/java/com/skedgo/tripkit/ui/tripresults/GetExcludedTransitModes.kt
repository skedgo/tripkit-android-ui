package com.skedgo.tripkit.ui.tripresults

import com.skedgo.tripkit.TransitModeFilter
import com.skedgo.tripkit.data.tsp.RegionInfo
import com.skedgo.tripkit.ui.core.modeprefs.IsTransitModeIncludedRepository
import com.skedgo.tripkit.routing.ModeInfo

internal class GetExcludedTransitModes(
    private val isTransitModeIncludedRepository: IsTransitModeIncludedRepository
) : TransitModeFilter {

  override fun filterTransitModes(regionInfo: RegionInfo): List<ModeInfo> {
    return isTransitModeIncludedRepository.getAll()
        .filter { !it.isIncluded }
        .map { it.transitModeId }
        .toList()
        .blockingGet()
        .let { excludedModes ->
              regionInfo.transitModes().orEmpty().filter {
                  excludedModes?.contains(it.id.toString())?.not() ?: true
              }
        }
  }

}
