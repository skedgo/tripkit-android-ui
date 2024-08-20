package com.skedgo.tripkit.ui.booking.apiv2

import androidx.annotation.StringDef
import com.google.gson.annotations.SerializedName
import com.skedgo.tripkit.booking.quickbooking.Fare
import com.skedgo.tripkit.common.model.BookingConfirmationAction
import com.skedgo.tripkit.common.model.BookingConfirmationNotes
import com.skedgo.tripkit.common.model.BookingConfirmationStatusValue
import com.skedgo.tripkit.common.model.BookingConfirmationStatusValue.Companion.isStatus
import com.skedgo.tripkit.common.model.BookingConfirmationStatusValue.Companion.isStatusAccepted
import com.skedgo.tripkit.routing.ModeInfo
import com.skedgo.tripkit.ui.R

data class BookingV2ListResponse(
    @SerializedName("bookings")
    val bookings: List<Booking?>? = null,
    @SerializedName("count")
    val count: Int? = null
) {
    data class Booking(
        @SerializedName("confirmation")
        val confirmation: Confirmation,
        @SerializedName("datetime")
        val datetime: String,
        @SerializedName("id")
        val id: String? = null,
        @SerializedName("index")
        val index: Int? = null,
        @SerializedName("isExternal")
        val isExternal: Boolean? = null,
        @SerializedName("mode")
        val mode: String? = null,
        @SerializedName("timeZone")
        val timeZone: String? = null,
        @SerializedName("trips")
        val trips: List<String>? = null,
        @SerializedName("tripsInfo")
        val tripsInfo: List<TripsInfo?>? = null,
        // This does NOT come from the API, but is used later
        @SerializedName("tripGroup")
        var tripGroup: String? = null,
        val relatedBookings: List<RelatedBooking>? = null,
        var isReturnTrip: Boolean = false
    ) {
        fun getPrimaryModeInfo(): ModeInfo? =
            tripsInfo?.firstOrNull()?.legs?.firstOrNull { it.modeInfo.id == mode }?.modeInfo

        data class RelatedBooking(
            val bookingId: String,
            val type: String,
            val confirmedBookingData: Booking? = null
        ) {
            @Retention(AnnotationRetention.RUNTIME)
            @StringDef(
                RelatedBookingType.RETURN,
                RelatedBookingType.OUTBOUND,
            )
            annotation class RelatedBookingType {
                companion object {
                    const val RETURN = "RETURN"
                    const val OUTBOUND = "OUTBOUND"
                }
            }
        }

        data class Confirmation(
            @SerializedName("purchase")
            val purchase: Purchase? = null,
            @SerializedName("provider")
            val provider: Provider? = null,
            @SerializedName("status")
            val status: Status? = null,
            @SerializedName("notes")
            val notes: List<BookingConfirmationNotes>? = null,
            val actions: List<BookingConfirmationAction>? = null,
            val fares: List<Fare>? = null
        ) {
            data class Purchase(
                @SerializedName("budgetPoints")
                val budgetPoints: Int? = null,
                @SerializedName("currency")
                val currency: String? = null,
                @SerializedName("date")
                val date: String? = null,
                @SerializedName("id")
                val id: String? = null,
                @SerializedName("price")
                val price: Double? = null,
                @SerializedName("productName")
                val productName: String? = null,
                @SerializedName("productType")
                val productType: String? = null,
                @SerializedName("validFromTimestamp")
                val validFrom: String? = null,
                @SerializedName("pickupWindowDuration")
                val pickupWindowDuration: Long? = null
            )

            data class Provider(
                @SerializedName("title")
                val title: String? = null
            )

            data class Status(
                @SerializedName("title")
                val title: String? = null,
                @SerializedName("subtitle")
                val subtitle: String? = null,
                @SerializedName("imageURL")
                val imageURL: String? = null,
                @SerializedName("value")
                val value: String? = null
            ) {
                fun getBackgroundColor(): Int {
                    return value?.let {
                        when {
                            it.isStatusAccepted() ||
                                it.isStatus(BookingConfirmationStatusValue.SCHEDULED) -> {
                                R.color.tripKitSuccess
                            }

                            it.isStatus(BookingConfirmationStatusValue.PROCESSING) ||
                                it.isStatus(BookingConfirmationStatusValue.STANDBY) -> {
                                R.color.tripKitWarning
                            }

                            else -> {
                                R.color.tripKitError
                            }
                        }
                    } ?: R.color.colorPrimaryDark
                }
            }
        }

        data class TripsInfo(
            @SerializedName("destination")
            val destination: Destination? = null,
            @SerializedName("legs")
            val legs: List<Leg>? = null,
            @SerializedName("origin")
            val origin: Origin? = null,
            val depart: String? = null,
            val arrive: String? = null,
        ) {
            data class Destination(
                @SerializedName("address")
                val address: String? = null,
                @SerializedName("lat")
                val lat: Double? = null,
                @SerializedName("lng")
                val lng: Double? = null,
                @SerializedName("name")
                val name: String? = null
            )

            data class Leg(
                @SerializedName("metric")
                val metric: Metric? = null,
                @SerializedName("modeInfo")
                val modeInfo: ModeInfo
            ) {
                data class Metric(
                    @SerializedName("calories")
                    val calories: Double? = null,
                    @SerializedName("carbon")
                    val carbon: Double? = null,
                    @SerializedName("currencySymbol")
                    val currencySymbol: String? = null,
                    @SerializedName("duration")
                    val duration: Int? = null,
                    @SerializedName("hassle")
                    val hassle: Double? = null,
                    @SerializedName("localCost")
                    val localCost: Double? = null,
                    @SerializedName("usdCost")
                    val usdCost: Double? = null
                )
            }

            data class Origin(
                @SerializedName("address")
                val address: String? = null,
                @SerializedName("lat")
                val lat: Double? = null,
                @SerializedName("lng")
                val lng: Double? = null,
                @SerializedName("name")
                val name: String? = null
            )
        }
    }
}