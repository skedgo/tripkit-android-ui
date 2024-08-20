package com.skedgo.tripkit.ui.booking.apiv2

import com.haroldadmin.cnradapter.NetworkResponse
import com.skedgo.tripkit.ApiError
import com.skedgo.tripkit.routing.RoutingResponse
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.QueryMap
import retrofit2.http.Url

interface BookingV2TrackingApi {
    @GET
    suspend fun logTrip(@Url url: String): NetworkResponse<BookingV2LogTripResponse, Unit>

    @GET("booking/v2/summary")
    suspend fun summary(): NetworkResponse<BookingV2SummaryResponse, Unit>

    @GET("booking/v2")
    suspend fun getBookingMonth(@Query("month") month: String?): NetworkResponse<BookingV2ListResponse, Unit>

    @GET("booking")
    suspend fun getBookings(): NetworkResponse<BookingV2ListResponse, Unit>

    @GET("booking")
    suspend fun getBookings(
        @Query("first") first: Long,
        @Query("max") max: Int,
        @Query("valid") valid: String
    ): NetworkResponse<BookingV2ListResponse, Unit>

    @GET("booking/v2/active")
    suspend fun getActiveBooking(@Query("mode") mode: String?): NetworkResponse<BookingV2ListResponse.Booking, Unit>

    @DELETE("booking/v2/{id}")
    suspend fun deleteBooking(@Path("id") bookingId: String): NetworkResponse<Unit, Unit>

    @GET
    suspend fun getTripGroup(
        @Url tripUrl: String,
        @QueryMap config: Map<String, String>
    ): NetworkResponse<RoutingResponse, ApiError>

}