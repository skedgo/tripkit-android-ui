package com.skedgo.tripkit.ui.data
import com.skedgo.tripkit.common.model.Location
import com.skedgo.tripkit.common.model.ScheduledStop
import com.skedgo.tripkit.tripplanner.NonCurrentType
import com.skedgo.tripkit.routing.dateTimeZone

fun NonCurrentType.toLocation(): Location {
  val x = this
  return Location().apply {
    lat = x.lat
    lon = x.lng
    name = x.name
    address = x.address
    timeZone = x.dateTimeZone?.id
  }
}

fun Location.toNonCurrentType(isTripGoPOI: Boolean = false): NonCurrentType =
    if (isTripGoPOI || this is ScheduledStop) {
      NonCurrentType.Stop(
          lat = lat,
          lng = lon,
          name = displayName,
          address = address,
          dateTimeZone = dateTimeZone)
    } else {
      NonCurrentType.Normal(
          lat = lat,
          lng = lon,
          name = displayName,
          address = address,
          dateTimeZone = dateTimeZone)
    }
