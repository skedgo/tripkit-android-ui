package com.skedgo.tripkit.ui.map.home

import com.google.android.gms.maps.model.MarkerOptions
import com.skedgo.tripkit.common.model.stop.ScheduledStop

object MapData {

    data class RegionalStopMarker(val markerOptions: MarkerOptions, val stop: ScheduledStop)

    private val regionalStops = mutableListOf<RegionalStopMarker>()

    fun getRegionalStops(): List<RegionalStopMarker> = regionalStops

    fun addRegionalStop(markerOptions: MarkerOptions, stop: ScheduledStop) {
        regionalStops.removeAll {
            it.markerOptions.title == markerOptions.title &&
            it.markerOptions.position == markerOptions.position
        }
        regionalStops.add(RegionalStopMarker(markerOptions, stop))
    }

    fun clearRegionalStops() {
        regionalStops.clear()
    }
}