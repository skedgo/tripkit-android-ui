package com.skedgo.tripkit.ui.tripresults

import com.skedgo.tripkit.common.model.Region
import com.skedgo.tripkit.data.regions.RegionService
import com.skedgo.tripkit.ui.core.modeprefs.TransportModePreference
import io.reactivex.Observable
import javax.inject.Inject

open class GetTransportModePreferencesByRegion @Inject internal constructor(
        private val regionService: RegionService) {
  open fun execute(region: Region, filter: TripResultTransportViewFilter): Observable<TransportModePreference> =
      regionService.getTransportModesByRegionAsync(region)
          .flatMapIterable { it }
          .flatMap { mode ->
              Observable.just(TransportModePreference(mode.id, filter.isSelected(mode.id), filter.isMinimized(mode.id)))
          }
}
