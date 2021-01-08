package com.skedgo.tripkit.ui.favorites

import com.skedgo.tripkit.routing.TripGroup
import com.skedgo.tripkit.ui.favorites.trips.Waypoint
import com.skedgo.tripkit.ui.routing.RoutingConfig
import io.reactivex.Observable

interface GetTripFromWaypoints {

  fun execute(config: RoutingConfig, waypoints: List<Waypoint>): Observable<TripGroup>
}