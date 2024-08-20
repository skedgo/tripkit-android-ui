package com.skedgo.tripkit.ui.map

import com.skedgo.tripkit.location.GeoPoint
import org.joda.time.DateTime

data class ServiceStop(
    val code: String,
    val position: GeoPoint,
    val name: String? = null,
    val platform: String? = null,
    val departureDateTime: DateTime? = null,
    val arrivalDateTime: DateTime? = null,
    val isWheelchairAccessible: Boolean? = null,
    val isBicycleAccessible: Boolean? = null
)
