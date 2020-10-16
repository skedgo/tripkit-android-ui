package com.skedgo.tripkit.ui.booking.apiv2

import io.reactivex.Completable
import io.reactivex.Observable


class BookingV2TrackingService(private var trackingApi: BookingV2TrackingApi) {
    fun logTrip(logUrl: String): Observable<BookingV2LogTripResponse> {
        return trackingApi.logTrip(logUrl)
    }

    fun bookingSummary(): Observable<BookingV2SummaryResponse> {
        return trackingApi.summary
    }

    fun bookingList(month: String): Observable<BookingV2ListResponse> {
        return trackingApi.getBookingMonth(month)
    }

    fun deleteBooking(bookingId: String): Completable {
        return trackingApi.deleteBooking(bookingId)
    }
}