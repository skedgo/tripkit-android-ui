package com.skedgo.tripkit.ui.data.realtime

import com.google.gson.annotations.JsonAdapter
import com.skedgo.tripkit.common.model.RealtimeAlert
import org.immutables.gson.Gson
import org.immutables.value.Value
import com.skedgo.tripkit.routing.RealTimeVehicle

@Value.Immutable
@Gson.TypeAdapters
@Value.Style(passAnnotations = [JsonAdapter::class])
@JsonAdapter(GsonAdaptersLatestServiceResponse::class)
interface LatestServiceResponse {
  fun serviceTripID(): String
  fun startStopCode(): String?
  fun endStopCode(): String?

  fun startTime(): Long? = 0
  fun endTime(): Long? = 0
  fun lastUpdate(): Long?
  fun realtimeVehicle(): RealTimeVehicle?
  fun realtimeAlternativeVehicle(): List<RealTimeVehicle>?
  fun alerts(): List<RealtimeAlert>?
}