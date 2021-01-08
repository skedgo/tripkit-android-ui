package com.skedgo.tripkit.ui.data.waypoints

import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter
import com.skedgo.tripkit.ui.data.ConfigDto
import com.skedgo.tripkit.ui.favorites.trips.Waypoint

class WaypointsRequestBody(val config: ConfigDto, val waypoints: Array<Waypoint>)

class WaypointsAdapter : TypeAdapter<Waypoint>() {
  override fun write(out: JsonWriter, value: Waypoint) {
    out.beginObject()
    out.name("lat")
    out.value(value.lat)

    out.name("lng")
    out.value(value.lng)
    value.mode?.let {
      out.name("mode")
      out.value(value.mode)
    }

    if (value.time > 0) {
      out.name("time")
      out.value(value.time)
    }
    out.endObject()
  }

  override fun read(`in`: JsonReader): Waypoint {
    TODO()
  }
}