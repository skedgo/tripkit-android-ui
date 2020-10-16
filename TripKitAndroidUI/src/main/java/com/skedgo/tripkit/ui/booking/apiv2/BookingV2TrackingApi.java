package com.skedgo.tripkit.ui.booking.apiv2;


import io.reactivex.Completable;
import io.reactivex.Observable;
import retrofit2.http.*;

public interface BookingV2TrackingApi {
    @GET
    Observable<BookingV2LogTripResponse> logTrip(@Url String url);

    @GET("booking/v2/summary")
    Observable<BookingV2SummaryResponse> getSummary();

    @GET("booking/v2")
    Observable<BookingV2ListResponse> getBookingMonth(@Query("month") String month);

    @DELETE("booking/v2/{id}")
    Completable deleteBooking(@Path("id") String bookingId);

}
