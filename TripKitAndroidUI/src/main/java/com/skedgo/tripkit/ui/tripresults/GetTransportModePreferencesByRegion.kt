package com.skedgo.tripkit.ui.tripresults

import com.skedgo.tripkit.common.model.Region
import com.skedgo.tripkit.common.model.TransportMode
import com.skedgo.tripkit.data.regions.RegionService
import com.skedgo.tripkit.ui.core.modeprefs.TransportModePreference
import com.skedgo.tripkit.ui.routingresults.IsModeIncludedInTripsRepository
import com.skedgo.tripkit.ui.routingresults.IsModeMinimizedRepository
import io.reactivex.Observable
import io.reactivex.functions.Function3
import javax.inject.Inject

open class GetTransportModePreferencesByRegion @Inject internal constructor(
        private val regionService: RegionService,
        private val isModeMinimizedRepository: IsModeMinimizedRepository,
        private val isModeIncludedInTripsRepository: IsModeIncludedInTripsRepository
) {
  open fun execute(region: Region): Observable<TransportModePreference> =
      regionService.getTransportModesByRegionAsync(region)
          .flatMapIterable { it }
          .flatMap {
            Observable.combineLatest(
                Observable.just(it),
                isModeIncludedInTripsRepository.isModeIncludedForRouting(it.id).toObservable(),
                isModeMinimizedRepository.isModeMinimized(it.id).toObservable(),
             Function3<TransportMode, Boolean, Boolean, TransportModePreference> { mode: TransportMode, isIncluded: Boolean, isMinimized: Boolean ->
              TransportModePreference(mode.id, isIncluded, isMinimized)
            })
          }
}
