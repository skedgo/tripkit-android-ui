package com.skedgo.tripkit.camera

import org.joda.time.DateTime

data class CachedMapCameraPosition(
    val cachingDateTime: DateTime,
    val mapCameraPosition: MapCameraPosition
)
