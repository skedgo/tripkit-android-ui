package com.skedgo.tripkit.ui.favorites.trips

import com.skedgo.tripkit.routing.SegmentType
import com.skedgo.tripkit.routing.Trip
import com.skedgo.tripkit.routing.TripSegment

fun Trip.toWaypoints(): List<Waypoint> {
  val waypoints = segments
      .filter { listOf(SegmentType.STATIONARY, SegmentType.DEPARTURE, SegmentType.ARRIVAL).contains(it.type).not() }
      .map { segment ->
        val mode = segment.getModeForWayPoint()
        Waypoint(segment.from.lat, segment.from.lon, mode.first, mode.second)
      }
  return waypoints.plus(
      segments.last().let { Waypoint(it.to.lat, it.to.lon, null, null) }
  )
}

internal fun TripSegment.getModeForWayPoint(): Pair<String, String> {
  if (this.modeInfo!!.modeCompat.isPublicTransport) {
//          segment.modeInfo!!.id
    return this.transportModeId!! to this.modeInfo!!.alternativeText
  } else {
    return this.transportModeId!! to this.modeInfo!!.alternativeText
  }
}