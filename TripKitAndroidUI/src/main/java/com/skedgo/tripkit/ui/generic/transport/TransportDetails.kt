package com.skedgo.tripkit.ui.generic.transport

import com.skedgo.tripkit.booking.quickbooking.Review
import com.skedgo.tripkit.ui.utils.getDisplayDateTimeFormatter
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
        fun parseFromReview(review: Review): TransportDetails {
            val parseFormatter: DateTimeFormatter = getISO2DateFormatter()
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
    }
}
