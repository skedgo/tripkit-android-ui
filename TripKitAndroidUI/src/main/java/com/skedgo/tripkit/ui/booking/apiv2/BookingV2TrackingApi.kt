package com.skedgo.tripkit.ui.booking.apiv2

import com.haroldadmin.cnradapter.NetworkResponse
import io.reactivex.Completable
import io.reactivex.Observable
import retrofit2.http.*

interface BookingV2TrackingApi {
    @GET
    suspend fun logTrip(@Url url: String): NetworkResponse<BookingV2LogTripResponse, Unit>

    @GET("booking/v2/summary")
    suspend fun summary(): NetworkResponse<BookingV2SummaryResponse, Unit>

    @GET("booking/v2")
    suspend fun getBookingMonth(@Query("month") month: String): NetworkResponse<BookingV2ListResponse, Unit>

    @DELETE("booking/v2/{id}")
    suspend fun deleteBooking(@Path("id") bookingId: String): NetworkResponse<Unit, Unit>
}