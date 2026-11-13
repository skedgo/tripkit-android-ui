package com.skedgo.tripkit.ui.routing

import com.google.maps.android.PolyUtil
import com.skedgo.tripkit.location.GeoPoint
import com.skedgo.tripkit.routing.TripSegment
import com.skedgo.tripkit.routing.Visibilities.VISIBILITY_ON_MAP

fun List<TripSegment>.getVisibleGeoPointsOnMap(): List<GeoPoint> = this
    .filter { it.isVisibleInContext(VISIBILITY_ON_MAP) }
    .flatMap { segment ->
        val locations = if (segment.realTimeVehicle?.hasLocationInformation() == true) {
            listOf(segment.from, segment.singleLocation, segment.realTimeVehicle?.location)
        } else {
            listOf(segment.from, segment.singleLocation)
        }
        // Fit the route actually drawn, including bends beyond the endpoint rectangle. Unused
        // service extensions are deliberately excluded, just as in GetTravelledLineForTrip.
        val geometry = segment.shapes.orEmpty().filter { it.isTravelled }
            .flatMap { PolyUtil.decode(it.encodedWaypoints) } +
            segment.streets.orEmpty().flatMap { street ->
                street.encodedWaypoints()?.let(PolyUtil::decode).orEmpty()
            }
        locations.filterNotNull().map { GeoPoint(it.lat, it.lon) } +
            geometry.map { GeoPoint(it.latitude, it.longitude) }
    }
