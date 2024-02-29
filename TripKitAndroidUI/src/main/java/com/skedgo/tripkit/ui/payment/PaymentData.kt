package com.skedgo.tripkit.ui.payment

import com.skedgo.tripkit.booking.quickbooking.EphemeralKey
import com.skedgo.tripkit.booking.quickbooking.PaymentOption
import com.skedgo.tripkit.booking.quickbooking.Review
import com.skedgo.tripkit.common.util.decimalFormatWithCurrencySymbol
import com.skedgo.tripkit.common.util.factor100
import com.skedgo.tripkit.common.util.nonDecimalFormatWithCurrencySymbol
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
    val review: List<Review>?,
    val publishableApiKey: String?,
    val ephemeralKey: EphemeralKey?,
    val areInputsValid: Boolean,
    val billingEnabled: Boolean
) {
    fun getTotalValue(): String {
        var total = 0.0
        if (review.isNullOrEmpty()) {
            total = this.total
        }
        review?.forEach {
            total += it.getFormattedPrice()
        }

        return if (total.factor100()) {
            total.toInt().nonDecimalFormatWithCurrencySymbol(currency.getCurrencySymbol())
        } else {
            total.decimalFormatWithCurrencySymbol(currency.getCurrencySymbol())
        }

    }

    fun getTotalPrice() = if (paymentSummaryDetails.isNotEmpty()) {
        paymentSummaryDetails.sumOf { it.getConvertedPrice() * (it.breakdown?.toDouble() ?: 0.0) }
    } else {
        0.0
    }

    fun getTotalTickets() = (if (paymentSummaryDetails.isNotEmpty()) {
        paymentSummaryDetails.sumOf { it.breakdown ?: 0 }
    } else {
        0.0
    }).toInt()
}