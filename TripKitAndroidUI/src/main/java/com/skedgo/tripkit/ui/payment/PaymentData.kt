package com.skedgo.tripkit.ui.payment

import android.graphics.drawable.Drawable
import com.skedgo.tripkit.ui.generic.transport.TransportDetails

data class PaymentData(
        val modeTitle: String,
        val modeIcon: String?,
        val modeDarkVehicleIcon: Int?,
        val paymentSummaryDetails: List<PaymentSummaryDetails>,
        val transportDetails: TransportDetails,
        val total: Double,
        val currency: String,
){
    fun getTotalValue(): String{
        return String.format("%s%.2f", currency, total)
    }
}