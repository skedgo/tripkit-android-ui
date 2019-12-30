package com.skedgo.tripkit.ui.routing

import com.skedgo.tripkit.common.model.Query
import io.reactivex.Observable
import com.skedgo.tripkit.routing.TripGroup
import com.skedgo.tripkit.routingstatus.RoutingStatusRepository
import com.skedgo.tripkit.routingstatus.Status
import javax.inject.Inject

class GetSortedTripGroupsWithRoutingStatus @Inject constructor(
    private val getSortedTripGroups: GetSortedTripGroups,
    private val routingStatusRepository: RoutingStatusRepository
) {
  fun execute(query: Query, sortOrder: Int): Observable<Pair<List<TripGroup>, Status>> =
      routingStatusRepository.getRoutingStatus(query.uuid())
          .map { it.status }
          .switchMap { status ->
            when (status) {
              is Status.Error -> Observable.just(Pair(emptyList(), status))
              is Status.InProgress -> getSortedTripGroups.execute(query.uuid(), query.arriveBy, sortOrder)
                  .startWith(emptyList<TripGroup>())
                  .map {
                    Pair(it, status)
                  }
              is Status.Completed -> getSortedTripGroups.execute(query.uuid(), query.arriveBy, sortOrder).map { Pair(it, status) }
            }
          }
}