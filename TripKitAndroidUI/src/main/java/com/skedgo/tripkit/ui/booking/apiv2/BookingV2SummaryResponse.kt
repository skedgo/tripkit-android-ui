package com.skedgo.tripkit.ui.booking.apiv2

import com.google.gson.annotations.JsonAdapter
import org.immutables.gson.Gson
import org.immutables.value.Value


@Value.Immutable
@Gson.TypeAdapters
@Value.Style(passAnnotations = [JsonAdapter::class])
@JsonAdapter(GsonAdaptersBookingV2SummaryResponse::class)
interface BookingV2SummaryResponse{
    fun summary(): Array<BookingV2SummaryBookingMonth>
}

@Value.Immutable
@Gson.TypeAdapters
@Value.Style(passAnnotations = [JsonAdapter::class])
@JsonAdapter(GsonAdaptersBookingV2SummaryBookingMonth::class)
interface BookingV2SummaryBookingMonth {
    fun month(): String
    fun count(): Int
}