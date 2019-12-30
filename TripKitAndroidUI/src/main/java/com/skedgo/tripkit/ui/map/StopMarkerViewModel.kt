package com.skedgo.tripkit.ui.map

import android.graphics.Color
import com.google.android.gms.maps.model.LatLng
import com.skedgo.tripkit.routing.Trip
import com.skedgo.tripkit.routing.TripSegment

data class StopMarkerViewModel(
        val trip: Trip,
        val stop: ServiceStop,
        val title: String?,
        val segment: TripSegment,
        val isTravelled: Boolean
) {
  val snippet: String? = stop.name ?: stop.platform
  val position = LatLng(stop.position.latitude, stop.position.longitude)
  val strokeColor: Int
    get() = when {
      isTravelled -> Color.BLACK
      else -> Color.GRAY
    }
  val fillColor: Int
    get() = when {
      isTravelled -> Color.WHITE
      else -> Color.LTGRAY
    }
  val alpha: Float
    get() = when {
      isTravelled -> 1f
      else -> 0.5f
    }
}