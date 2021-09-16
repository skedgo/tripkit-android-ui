package com.skedgo.tripkit.ui.booking.apiv2

import com.haroldadmin.cnradapter.NetworkResponse
import com.skedgo.tripkit.ApiError
import com.skedgo.tripkit.routing.RoutingResponse

class BookingV2TrackingService(private var trackingApi: BookingV2TrackingApi) {
    suspend fun logTrip(logUrl: String): NetworkResponse<BookingV2LogTripResponse, Unit> {
        return trackingApi.logTrip(logUrl)
    }

    suspend fun bookingSummary(): NetworkResponse<BookingV2SummaryResponse, Unit> {
        return trackingApi.summary()
    }

    suspend fun bookingList(month: String): NetworkResponse<BookingV2ListResponse, Unit>{
        return trackingApi.getBookingMonth(month)
    }

    suspend fun deleteBooking(bookingId: String): NetworkResponse<Unit, Unit> {
        return trackingApi.deleteBooking(bookingId)
    }

    suspend fun getActiveBooking(mode: String? = null): NetworkResponse<BookingV2ListResponse.Booking, Unit> {
        return trackingApi.getActiveBooking(mode)
    }

    suspend fun getTripGroup(tripUrl: String): NetworkResponse<RoutingResponse, ApiError> {
        return trackingApi.getTripGroup(tripUrl, mapOf("v" to "11"))
    }
}