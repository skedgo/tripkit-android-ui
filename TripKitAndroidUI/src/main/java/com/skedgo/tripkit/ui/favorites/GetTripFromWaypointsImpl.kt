package com.skedgo.tripkit.ui.favorites

import android.content.res.Resources
import com.google.gson.Gson
import com.haroldadmin.cnradapter.NetworkResponse
import com.skedgo.tripgo.sdk.agenda.data.toConfigDto
import com.skedgo.tripkit.routing.TripGroup
import com.skedgo.tripkit.ui.data.waypoints.WaypointsAdvancedRequestBody
import com.skedgo.tripkit.ui.data.waypoints.WaypointsApi
import com.skedgo.tripkit.ui.data.waypoints.WaypointsRequestBody
import com.skedgo.tripkit.ui.favorites.waypoints.Waypoint
import com.skedgo.tripkit.ui.routing.RoutingConfig
import io.reactivex.Observable
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.rx2.asFlow

internal class GetTripFromWaypointsImpl(
    private val resources: Resources,
    private val gson: Gson,
    private val waypointsApi: WaypointsApi
) : GetTripFromWaypoints {

    override fun execute(
        config: RoutingConfig,
        waypoints: List<Waypoint>
    ): Observable<GetTripFromWaypoints.WaypointResponse?> {
        val wp = waypoints.mapIndexed { index: Int, waypoint: Waypoint ->
            if (index == 0 && waypoint.start.isNullOrEmpty()) {
                waypoint.copy(time = System.currentTimeMillis().div(1000).plus(60))
            } else {
                waypoint
            }
        }
        return waypointsApi.request(WaypointsRequestBody(config.toConfigDto(), wp.toTypedArray()))
            .map { response ->
                response.processRawData(resources, gson)
                var tripGroup: TripGroup? = null
                response.tripGroupList?.let {
                    tripGroup = it.first()
                }
                GetTripFromWaypoints.WaypointResponse(tripGroup, response.errorMessage)
            }
            .subscribeOn(Schedulers.io())
    }

    override fun executeAsFlow(
        config: RoutingConfig,
        waypoints: List<Waypoint>
    ): Flow<GetTripFromWaypoints.WaypointResponse?> {
        val wp = waypoints.mapIndexed { index: Int, waypoint: Waypoint ->
            if (index == 0 && waypoint.start.isNullOrEmpty()) {
                waypoint.copy(time = System.currentTimeMillis().div(1000).plus(60))
            } else {
                waypoint
            }
        }
        return waypointsApi.request(
            WaypointsAdvancedRequestBody(
                config.toConfigDto(),
                wp.toTypedArray(),
                System.currentTimeMillis() / 1000
            )
        )
            .map { response ->
                response.processRawData(resources, gson)
                var tripGroup: TripGroup? = null
                response.tripGroupList?.let {
                    tripGroup = it.first()
                }
                GetTripFromWaypoints.WaypointResponse(tripGroup, response.errorMessage)
            }.asFlow().flowOn(Dispatchers.IO)
    }

    override suspend fun requestTripGroup(
        config: RoutingConfig,
        waypoints: List<Waypoint>
    ): TripGroup? {
        val wp = waypoints.mapIndexed { index: Int, waypoint: Waypoint ->
            if (index == 0 && waypoint.start.isNullOrEmpty()) {
                waypoint.copy(time = System.currentTimeMillis().div(1000).plus(60))
            } else {
                waypoint
            }
        }

        var tg: TripGroup? = null
        val response = waypointsApi.requestTripGroup(
            WaypointsRequestBody(
                config.toConfigDto(),
                wp.toTypedArray()
            )
        )
        if (response is NetworkResponse.Success) {
            tg = response.body.tripGroupList.first()
        }
        return tg
    }
}