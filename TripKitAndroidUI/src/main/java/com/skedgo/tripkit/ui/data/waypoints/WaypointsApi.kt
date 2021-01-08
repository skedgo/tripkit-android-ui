package com.skedgo.tripkit.ui.data.waypoints

import com.skedgo.tripkit.routing.RoutingResponse
import io.reactivex.Observable
import retrofit2.http.Body
import retrofit2.http.POST

interface WaypointsApi {

  @POST("waypoint.json")
  fun request(@Body body: WaypointsRequestBody): Observable<RoutingResponse>
}