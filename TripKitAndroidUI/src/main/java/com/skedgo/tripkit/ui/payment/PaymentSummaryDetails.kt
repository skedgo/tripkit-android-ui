package com.skedgo.tripkit.ui.payment

import com.skedgo.tripkit.booking.quickbooking.Ticket
import com.skedgo.tripkit.ui.R
import com.skedgo.tripkit.ui.utils.getCurrencySymbol

data class PaymentSummaryDetails(
    val id: String,
    val icon: Int,
    val description: String,
    val breakdown: Long? = null,
    val price: Double? = null,
    val currency: String? = null,
) {
    fun getDetailedDescription(): String {
        return if (breakdown != null) {
            "$breakdown x $description"
        } else {
            description
        }
    }

    fun getConvertedPrice(): Double {
        return (price ?: 0.0) / 100.0 // Why is it divided by 100? - Leng
//        return (price ?: 0.0)
    }

    fun getTotal(): String {
        if ((breakdown ?: 0L).toDouble() * getConvertedPrice() > 0) {
            return String.format(
                "%s%.2f", currency?.getCurrencySymbol(), (breakdown
                    ?: 0L).toDouble() * getConvertedPrice()
            )
        }
        return "FREE"
    }

    companion object {
        fun parseTicket(ticket: Ticket): PaymentSummaryDetails {
            return PaymentSummaryDetails(
                ticket.id,
                R.drawable.ic_person,
                ticket.name,
                ticket.value,
                ticket.price,
                ticket.currency
            )
        }
    }
}
