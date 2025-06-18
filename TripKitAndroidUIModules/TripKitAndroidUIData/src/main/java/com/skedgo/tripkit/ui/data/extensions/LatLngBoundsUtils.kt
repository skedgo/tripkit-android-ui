package com.skedgo.tripkit.ui.data.extensions

import com.skedgo.tripkit.ui.data.places.LatLng
import com.skedgo.tripkit.ui.data.places.LatLngBounds

fun LatLngBounds.withBuffer(factor: Double = 1.5): LatLngBounds {
    val centerLat = (northeast.latitude + southwest.latitude) / 2
    val centerLng = (northeast.longitude + southwest.longitude) / 2
    val latSpan = (northeast.latitude - southwest.latitude) * factor / 2
    val lngSpan = (northeast.longitude - southwest.longitude) * factor / 2

    return LatLngBounds(
        southwest = LatLng(centerLat - latSpan, centerLng - lngSpan),
        northeast = LatLng(centerLat + latSpan, centerLng + lngSpan)
    )
}