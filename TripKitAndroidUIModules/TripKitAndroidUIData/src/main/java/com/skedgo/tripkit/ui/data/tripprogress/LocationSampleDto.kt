package com.skedgo.tripkit.ui.data.tripprogress
import com.skedgo.tripkit.location.LocationSample

internal fun LocationSample.toLocationSampleDto(): LocationSampleDto =
    LocationSampleDto.Builder().apply {
      timestamp(timestamp)
      latitude(latitude)
      longitude(longitude)
      bearing(bearing)
      speed(speed)
    }.build()