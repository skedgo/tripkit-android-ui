package com.skedgo.tripkit.ui.geocoding

import com.skedgo.tripkit.ui.data.places.LatLngBounds
import org.immutables.value.Value.Immutable
import org.immutables.value.Value.Parameter

@Immutable(builder = false)
interface CitySelectedEvent {
    @Parameter
    fun bounds(): LatLngBounds?
}