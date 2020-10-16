package com.skedgo.tripkit.ui.booking.apiv2

import com.google.gson.annotations.JsonAdapter
import org.immutables.gson.Gson
import org.immutables.value.Value

@Gson.TypeAdapters
@Value.Immutable
@Value.Style(passAnnotations = [JsonAdapter::class])
@JsonAdapter(GsonAdaptersBookingV2LogTripResponse::class)
interface BookingV2LogTripResponse{
    fun bookingID(): String
    fun bookingURL(): String
}