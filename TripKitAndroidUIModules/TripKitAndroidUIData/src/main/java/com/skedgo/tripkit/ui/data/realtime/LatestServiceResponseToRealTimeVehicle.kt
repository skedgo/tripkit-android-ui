package com.skedgo.tripkit.ui.data.realtime

import com.skedgo.tripkit.common.model.RealtimeAlert
import com.skedgo.tripkit.routing.RealTimeVehicle

fun LatestServiceResponse.toRealTimeVehicle() =
    (this.realtimeVehicle() ?: RealTimeVehicle())
        .apply {
          alerts = ArrayList<RealtimeAlert>(alerts().orEmpty())
          serviceTripId = serviceTripID()
          startStopCode = startStopCode()
          endStopCode = endStopCode()
          arriveAtStartStopTime = startTime() ?: 0
          arriveAtEndStopTime = endTime() ?: 0
        }