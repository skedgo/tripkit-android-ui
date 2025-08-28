package com.skedgo.tripkit.ui.map.home

import com.google.android.gms.maps.model.MarkerOptions
import com.skedgo.tripkit.common.model.stop.ScheduledStop
import com.skedgo.tripkit.ui.map.StopPOILocation
import com.skedgo.tripkit.ui.map.getStopDisplayName

object MapData {

    private val regionalStops = mutableListOf<MarkerOptions>()

    fun getRegionalStops() = regionalStops

    fun addRegionalStop(stop: MarkerOptions) {
        regionalStops.removeAll { it.title == stop.title && it.position == stop.position }
        regionalStops.add(stop)
    }

    fun clearRegionalStops() {
        regionalStops.clear()
    }
}