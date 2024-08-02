package com.skedgo.tripkit.ui.favorites

import com.skedgo.tripkit.routing.TripGroup
import com.skedgo.tripkit.ui.favorites.waypoints.Waypoint
import com.skedgo.tripkit.ui.routing.RoutingConfig
import io.reactivex.Observable
import kotlinx.coroutines.flow.Flow

interface GetTripFromWaypoints {

  data class WaypointResponse(
          val tripGroup: TripGroup? = null,
          val error: String? = ""
  )

  fun execute(config: RoutingConfig, waypoints: List<Waypoint>): Observable<WaypointResponse?>
  fun executeAsFlow(config: RoutingConfig, waypoints: List<Waypoint>): Flow<WaypointResponse?>

  suspend fun requestTripGroup(config: RoutingConfig, waypoints: List<Waypoint>): TripGroup?
}