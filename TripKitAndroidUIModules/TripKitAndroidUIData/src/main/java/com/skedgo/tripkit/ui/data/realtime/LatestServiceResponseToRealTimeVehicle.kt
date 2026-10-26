package com.skedgo.tripkit.ui.data.realtime

import com.skedgo.tripkit.common.model.realtimealert.RealtimeAlert
import com.skedgo.tripkit.routing.RealTimeVehicle

/** Only the service's assigned vehicle owns the response's trip, alerts and stop times. */
fun LatestServiceResponse.toRealTimeVehicles(): List<RealTimeVehicle> =
    listOfNotNull(realtimeVehicle()?.apply {
        alerts = ArrayList<RealtimeAlert>(alerts().orEmpty())
        serviceTripId = serviceTripID()
        startStopCode = startStopCode()
        endStopCode = endStopCode()
        arriveAtStartStopTime = startTime() ?: 0
        arriveAtEndStopTime = endTime() ?: 0
    })

/** Alternatives are locations for this request's map, not vehicles assigned to its trip. */
fun LatestServiceResponse.toMapRealTimeVehicles(): List<RealTimeVehicle> =
    (toRealTimeVehicles() + realtimeVehicleAlternatives().orEmpty() +
        realtimeAlternativeVehicle().orEmpty())
        .distinctBy { vehicle ->
            vehicle.location?.let { Triple(it.lat, it.lon, vehicle.lastUpdateTime) } ?: vehicle
        }
