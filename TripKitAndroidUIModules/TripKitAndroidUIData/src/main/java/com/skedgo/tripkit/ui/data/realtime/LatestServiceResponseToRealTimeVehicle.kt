package com.skedgo.tripkit.ui.data.realtime

import com.skedgo.tripkit.common.model.realtimealert.RealtimeAlert
import com.skedgo.tripkit.routing.RealTimeVehicle

fun LatestServiceResponse.toRealTimeVehicles(): List<RealTimeVehicle> =
    buildList {
        realtimeVehicle()?.let(::add)
        addAll(realtimeVehicleAlternatives().orEmpty())
        addAll(realtimeAlternativeVehicle().orEmpty())
    }
        .distinctBy { vehicle ->
            vehicle.location?.let { location ->
                Triple(location.lat, location.lon, vehicle.lastUpdateTime)
            }
        }
        .map { vehicle ->
            vehicle.apply {
                alerts = ArrayList<RealtimeAlert>(alerts().orEmpty())
                serviceTripId = serviceTripID()
                startStopCode = startStopCode()
                endStopCode = endStopCode()
                arriveAtStartStopTime = startTime() ?: 0
                arriveAtEndStopTime = endTime() ?: 0
            }
        }
