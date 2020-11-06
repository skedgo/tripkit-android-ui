package com.skedgo.tripkit.ui.routing

import com.skedgo.tripkit.common.model.Query
import io.reactivex.Observable
import com.skedgo.tripkit.routing.TripGroup
import com.skedgo.tripkit.routingstatus.RoutingStatusRepository
import com.skedgo.tripkit.routingstatus.Status
import com.skedgo.tripkit.ui.tripresults.TripResultTransportViewFilter
import timber.log.Timber
import javax.inject.Inject

class GetSortedTripGroupsWithRoutingStatus @Inject constructor(
    private val getSortedTripGroups: GetSortedTripGroups,
    private val routingStatusRepository: RoutingStatusRepository
) {
  fun execute(query: Query, sortOrder: Int, filter: TripResultTransportViewFilter): Observable<Pair<List<TripGroup>, Status>> =
      routingStatusRepository.getRoutingStatus(query.uuid())
          .map {
              it.status
          }
          .switchMap { status ->
            when (status) {
              is Status.Error -> Observable.just(Pair(emptyList(), status))
              is Status.InProgress -> getSortedTripGroups.execute(query.uuid(), query.arriveBy, sortOrder, filter)
                  .startWith(emptyList<TripGroup>())
                  .map {
                    Pair(it, status)
                  }
              is Status.Completed -> getSortedTripGroups.execute(query.uuid(), query.arriveBy, sortOrder, filter).map { Pair(it, status) }
            }
          }
}