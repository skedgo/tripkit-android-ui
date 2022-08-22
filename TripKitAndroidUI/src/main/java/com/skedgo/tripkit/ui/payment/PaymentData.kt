package com.skedgo.tripkit.ui.payment

import com.skedgo.tripkit.booking.quickbooking.PaymentOption
import com.skedgo.tripkit.booking.quickbooking.Review
import com.skedgo.tripkit.ui.generic.transport.TransportDetails
import com.skedgo.tripkit.ui.utils.getCurrencySymbol

data class PaymentData(
        val drtFragmentHashCode: Int,
        val modeTitle: String,
        val modeIcon: String?,
        val modeDarkVehicleIcon: Int?,
        val paymentSummaryDetails: List<PaymentSummaryDetails>,
        val transportDetails: TransportDetails,
        val total: Double,
        val currency: String,
        val paymentOptions: List<PaymentOption>?,
        val review: List<Review>?
) {
    fun getTotalValue(): String {
        return String.format("%s%.2f", currency.getCurrencySymbol(), total)
    }



}