package com.skedgo.tripkit.ui.data.waypoints

import com.haroldadmin.cnradapter.NetworkResponse
import com.skedgo.tripkit.ApiError
import com.skedgo.tripkit.routing.RoutingResponse
import io.reactivex.Observable
import retrofit2.http.Body
import retrofit2.http.POST

interface WaypointsApi {

    @POST("waypoint.json")
    fun request(@Body body: WaypointsRequestBody): Observable<RoutingResponse>

    @POST("waypoint.json")
    fun request(@Body body: WaypointsAdvancedRequestBody): Observable<RoutingResponse>

    @POST("waypoint.json")
    suspend fun requestTripGroup(@Body body: WaypointsRequestBody): NetworkResponse<RoutingResponse, ApiError>
}