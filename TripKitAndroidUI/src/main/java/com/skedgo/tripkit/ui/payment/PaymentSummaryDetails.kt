package com.skedgo.tripkit.ui.payment

data class PaymentSummaryDetails(
        val id: Int,
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
}
