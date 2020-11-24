package com.skedgo.tripkit.ui.booking.apiv2

import com.google.gson.annotations.SerializedName
import com.skedgo.tripkit.routing.ModeInfo

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
        var tripGroup: String? = null
    ) {
        data class Confirmation(
            @SerializedName("purchase")
            val purchase: Purchase? = null
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
                val productType: String? = null
            )
        }

        data class TripsInfo(
                @SerializedName("destination")
            val destination: Destination? = null,
                @SerializedName("legs")
            val legs: List<Leg>? = null,
                @SerializedName("origin")
            val origin: Origin? = null
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