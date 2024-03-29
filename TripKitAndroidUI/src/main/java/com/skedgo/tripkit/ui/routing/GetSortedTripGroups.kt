package com.skedgo.tripkit.ui.routing

import com.skedgo.tripkit.ui.routingresults.TripGroupRepository
import io.reactivex.Observable
import com.skedgo.tripkit.routing.TripGroup
import com.skedgo.tripkit.ui.tripresults.TripResultTransportViewFilter
import javax.inject.Inject
import javax.inject.Provider

open class GetSortedTripGroups @Inject internal constructor(private val tripGroupRepository: TripGroupRepository) {
  // TODO Note: This no longer actually sorts the trip groups. Here is where they should be sorted, if the design
  // changes to require it.
  open fun execute(queryId: String, arriveBy: Long, sortOrder: Int, filter: TripResultTransportViewFilter): Observable<List<TripGroup>> =
      tripGroupRepository.getTripGroupsByA2bRoutingRequestId(queryId)
}