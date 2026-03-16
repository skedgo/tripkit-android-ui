package com.skedgo.tripkit.ui.payment

import com.skedgo.tripkit.booking.quickbooking.EphemeralKey
import com.skedgo.tripkit.booking.quickbooking.PaymentOption
import com.skedgo.tripkit.booking.quickbooking.Review
import com.skedgo.tripkit.common.util.decimalFormatWithCurrencySymbol
import com.skedgo.tripkit.common.util.factor100
import com.skedgo.tripkit.common.util.nonDecimalFormatWithCurrencySymbol
import com.skedgo.tripkit.ui.generic.transport.TransportDetails
import com.skedgo.tripkit.common.util.getCurrencySymbol

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
    var review: List<Review>?,
    var publishableApiKey: String?,
    var ephemeralKey: EphemeralKey?,
    val areInputsValid: Boolean,
    val billingEnabled: Boolean,
    val hasTickets: Boolean
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

    /**
     * Returns the numeric total (in dollars) for Stripe Payment Sheet IntentConfiguration.
     * Uses the same logic as [getTotalValue]: when [review] is present, sums review prices;
     * otherwise uses [getTotalPrice] or [total]. Caller should multiply by 100 for cents
     * and use `coerceAtLeast(50)` for Stripe's minimum amount.
     */
    fun getTotalAmountForPayment(): Double = if (review.isNullOrEmpty()) {
        if (paymentSummaryDetails.isNotEmpty()) getTotalPrice() else total
    } else {
        review!!.sumOf { it.getFormattedPrice() }
    }

    /**
     * Returns currency for Stripe Payment Sheet (ISO 4217, e.g. "aud").
     * Uses [currency] when non-blank; otherwise falls back to [review] or [paymentOptions].
     * Defaults to "aud" when no source provides a valid currency.
     */
    fun getCurrencyForPayment(): String = currency.takeIf { it.isNotBlank() }
        ?: review?.firstOrNull()?.currency?.takeIf { it.isNotBlank() }
        ?: paymentOptions?.firstOrNull()?.currency?.takeIf { it.isNotBlank() }
        ?: "aud"

    fun getTotalTickets() = (if (paymentSummaryDetails.isNotEmpty()) {
        paymentSummaryDetails.sumOf { it.breakdown ?: 0 }
    } else {
        0.0
    }).toInt()
}