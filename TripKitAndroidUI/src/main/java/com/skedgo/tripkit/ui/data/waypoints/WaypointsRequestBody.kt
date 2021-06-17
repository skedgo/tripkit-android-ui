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
        if (value.start.isNullOrEmpty()) {
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
        } else {
            out.name("start")
            out.value(value.start)

            out.name("end")
            out.value(value.end)

            out.name("modes")
            out.beginArray()
            out.value(value.mode)
            out.endArray()

            if (!value.modeTitle.isNullOrEmpty()) {
                out.name("modeTitle")
                out.value(value.modeTitle)
            }

            if (!value.operator.isNullOrEmpty()) {
                out.name("operator")
                out.value(value.operator)
            }

            if (!value.startTime.isNullOrEmpty()) {
                out.name("startTime")
                out.value(value.startTime)
            }

            if (!value.endTime.isNullOrEmpty()) {
                out.name("endTime")
                out.value(value.endTime)
            }

            if (!value.serviceTripId.isNullOrEmpty()) {
                out.name("serviceTripId")
                out.value(value.serviceTripId)
            }

            if (!value.region.isNullOrEmpty()) {
                out.name("region")
                out.value(value.region)
            }

            if (!value.disembarkationRegion.isNullOrEmpty()) {
                out.name("disembarkationRegion")
                out.value(value.disembarkationRegion)
            }
        }
        out.endObject()
    }

    override fun read(`in`: JsonReader): Waypoint {
        TODO()
    }
}