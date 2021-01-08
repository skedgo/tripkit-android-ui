package com.skedgo.tripkit.ui.favorites

import android.content.res.Resources
import com.google.gson.Gson
import com.skedgo.tripgo.sdk.agenda.data.toConfigDto
import com.skedgo.tripkit.routing.TripGroup
import com.skedgo.tripkit.ui.favorites.trips.Waypoint
import com.skedgo.tripkit.ui.routing.RoutingConfig
import io.reactivex.Observable
import io.reactivex.schedulers.Schedulers
import com.skedgo.tripkit.ui.data.waypoints.WaypointsApi
import com.skedgo.tripkit.ui.data.waypoints.WaypointsRequestBody

internal class GetTripFromWaypointsImpl(
    private val resources: Resources,
    private val gson: Gson,
    private val waypointsApi: WaypointsApi
) : GetTripFromWaypoints {

  override fun execute(config: RoutingConfig, waypoints: List<Waypoint>): Observable<TripGroup> {
    val wp = waypoints.mapIndexed { index: Int, waypoint: Waypoint ->
      if (index == 0) {
        waypoint.copy(time = System.currentTimeMillis().div(1000).plus(60))
      } else {
        waypoint
      }
    }
    return waypointsApi.request(WaypointsRequestBody(config.toConfigDto(), wp.toTypedArray()))
        .map { response ->
          response.processRawData(resources, gson)
          response.tripGroupList.first()
        }
        .subscribeOn(Schedulers.io())
  }
}