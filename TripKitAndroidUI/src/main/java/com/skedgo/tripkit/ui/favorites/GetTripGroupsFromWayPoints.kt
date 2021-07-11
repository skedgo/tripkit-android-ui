package com.skedgo.tripkit.ui.favorites

import com.skedgo.tripkit.routing.TripGroup
import com.skedgo.tripkit.routing.startDateTime
import com.skedgo.tripkit.ui.favorites.trips.FavoriteTripsRepository
import com.skedgo.tripkit.ui.routing.GetRoutingConfig
import com.skedgo.tripkit.ui.routingresults.TripGroupRepository
import io.reactivex.Observable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.rx2.*
import kotlinx.coroutines.withContext
import javax.inject.Inject

open class GetTripGroupsFromWayPoints @Inject constructor(
        private val getTripFromWaypoints: GetTripFromWaypoints,
        private val favoriteTripsRepository: FavoriteTripsRepository,
        private val tripGroupRepository: TripGroupRepository,
        private val getRoutingConfig: GetRoutingConfig
) {

    open fun execute(favoriteTripId: String): Observable<TripGroup> {
        return favoriteTripsRepository.getAllFavoriteTrips().map {
            it.first { it.uuid == favoriteTripId }
        }.flatMapLatest { favoriteTrip ->
            val tripGroupId = favoriteTrip.tripGroupId
            if (tripGroupId == null) {
                emptyFlow<TripGroup>()
            } else {
                val group = tripGroupRepository.getTripGroup(tripGroupId)
                        .takeWhile { it.displayTrip!!.startDateTime.isAfterNow }
                        .awaitFirstOrNull()
                flow {
                    if (group == null) {
                        val config = getRoutingConfig.execute()
                        val waypointResponse = getTripFromWaypoints.execute(config, favoriteTrip.waypoints).awaitFirstOrNull()
                        waypointResponse?.let { response ->
                            withContext(Dispatchers.IO) {
                                response.tripGroup?.let { tg ->
                                    tripGroupRepository.addTripGroups(tg.uuid(), listOf(tg)).await()
                                    favoriteTripsRepository.updateFavoriteTrip(favoriteTrip = favoriteTrip.copy(tripGroupId = tg.uuid()))
                                    emit(tripGroupRepository.getTripGroup(tg.uuid()).awaitFirst())
                                }
                            }
                        }
                    } else {
                        emit(group)
                    }
                }
            }
        }.asObservable()
    }
}