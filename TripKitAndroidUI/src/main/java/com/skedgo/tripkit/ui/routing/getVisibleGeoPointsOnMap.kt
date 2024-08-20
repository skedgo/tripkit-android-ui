package com.skedgo.tripkit.ui.routing

import com.skedgo.tripkit.location.GeoPoint
import com.skedgo.tripkit.routing.TripSegment
import com.skedgo.tripkit.routing.Visibilities.VISIBILITY_ON_MAP

fun List<TripSegment>.getVisibleGeoPointsOnMap(): List<GeoPoint> = this
    .filter { it.isVisibleInContext(VISIBILITY_ON_MAP) }
    .flatMap {
        if (it.realTimeVehicle != null && it.realTimeVehicle.hasLocationInformation()) {
            listOf(it.from, it.singleLocation, it.realTimeVehicle.location!!)
        } else {
            listOf(it.from, it.singleLocation)
        }
    }
    .filter { it != null }
    .map { GeoPoint(it.lat, it.lon) }
