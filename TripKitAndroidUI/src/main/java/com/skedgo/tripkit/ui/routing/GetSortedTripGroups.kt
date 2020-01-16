package com.skedgo.tripkit.ui.routing

import com.skedgo.tripkit.settings.GetLeastRecentlyUsedRegion
import com.skedgo.tripkit.ui.routingresults.TripGroupRepository
import com.skedgo.tripkit.ui.tripresults.GetTransportModePreferencesByRegion
import io.reactivex.Observable
import com.skedgo.tripkit.routing.TripGroup
import com.skedgo.tripkit.ui.tripresults.TripResultTransportViewFilter
import javax.inject.Inject
import javax.inject.Provider

open class GetSortedTripGroups @Inject internal constructor(
        private val tripGroupRepository: TripGroupRepository,
        private val sorterProvider: Provider<TripGroupsSorter>,
        private val getLeastRecentlyUsedRegion: GetLeastRecentlyUsedRegion,
        private val getTransportModePreferencesByRegion: GetTransportModePreferencesByRegion
) {
  open fun execute(queryId: String, arriveBy: Long, sortOrder: Int, filter: TripResultTransportViewFilter): Observable<List<TripGroup>> =
      tripGroupRepository.getTripGroupsByA2bRoutingRequestId(queryId)
          .switchMap { groups ->
            getLeastRecentlyUsedRegion.execute()
                .flatMap { getTransportModePreferencesByRegion.execute(it, filter) }
                .toList()
                .map { Pair(groups, it) }.toObservable()
          }
          .map {
            val groups = it.first
            val modePreferences = it.second
            val sorter = sorterProvider.get()
            val list = groups.toMutableList()
            sorter.updateTripGroupVisibilities(
                list,
                modePreferences
            )

            // Must sort after mutating the groups like above.
            // Otherwise, will wind up following issues:
            // [1] https://redmine.buzzhives.com/issues/4447
            // [2] https://redmine.buzzhives.com/issues/4305
            sorter.sort(sortOrder, list, arriveBy > 0)
            list.toList()
          }
}