package com.skedgo.tripkit.ui.generic.transport

import com.skedgo.tripkit.booking.quickbooking.Review
import com.skedgo.tripkit.ui.booking.apiv2.BookingV2ListResponse
import com.skedgo.tripkit.ui.utils.getDisplayDateTimeFormatter
import com.skedgo.tripkit.ui.utils.getDisplayTimeFormatter
import com.skedgo.tripkit.ui.utils.getISO2DateFormatter
import com.skedgo.tripkit.ui.utils.getISODateFormatter
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat
import org.joda.time.format.DateTimeFormatter

data class TransportDetails(
        val pickUpAddress: String?,
        val pickUpTime: String?,
        val dropOffAddress: String?,
        val dropOffTime: String?
) {
    companion object {

        private val parseFormatter: DateTimeFormatter = getISO2DateFormatter()

        fun parseFromReview(review: Review): TransportDetails {
            val formatter: DateTimeFormatter = getDisplayDateTimeFormatter()
            val dtDepart: DateTime = parseFormatter.parseDateTime(review.depart)
            val dtArrive: DateTime = parseFormatter.parseDateTime(review.arrive)

            return TransportDetails(
                    pickUpAddress = review.origin.address,
                    pickUpTime = dtDepart.toString(formatter),
                    dropOffAddress = review.destination.address,
                    dropOffTime = dtArrive.toString(formatter)
            )
        }

        fun parseFromTripsInfo(tripsInfo: BookingV2ListResponse.Booking.TripsInfo): TransportDetails {
            val formatter: DateTimeFormatter = getDisplayTimeFormatter()
            val dtDepart: DateTime = parseFormatter.parseDateTime(tripsInfo.depart?.substringBefore('['))

            return TransportDetails(
                pickUpAddress = tripsInfo.origin?.address,
                pickUpTime = "Requested time ${dtDepart.toString(formatter)}",
                dropOffAddress = tripsInfo.destination?.address,
                dropOffTime = null
            )
        }
    }
}
