package com.skedgo.tripkit.ui.booking.apiv2

import com.haroldadmin.cnradapter.NetworkResponse
import com.skedgo.tripkit.ApiError
import com.skedgo.tripkit.routing.RoutingResponse
import io.reactivex.Completable
import io.reactivex.Observable
import kotlinx.coroutines.flow.Flow
import retrofit2.http.*
import java.util.*

interface BookingV2TrackingApi {
    @GET
    suspend fun logTrip(@Url url: String): NetworkResponse<BookingV2LogTripResponse, Unit>

    @GET("booking/v2/summary")
    suspend fun summary(): NetworkResponse<BookingV2SummaryResponse, Unit>

    @GET("booking/v2")
    suspend fun getBookingMonth(@Query("month") month: String): NetworkResponse<BookingV2ListResponse, Unit>

    @GET("booking/v2/active")
    suspend fun getActiveBooking(@Query("mode") mode: String?): NetworkResponse<BookingV2ListResponse.Booking, Unit>

    @DELETE("booking/v2/{id}")
    suspend fun deleteBooking(@Path("id") bookingId: String): NetworkResponse<Unit, Unit>

    @GET
    suspend fun getTripGroup(@Url tripUrl: String, @QueryMap config: Map<String, String>): NetworkResponse<RoutingResponse, ApiError>

}