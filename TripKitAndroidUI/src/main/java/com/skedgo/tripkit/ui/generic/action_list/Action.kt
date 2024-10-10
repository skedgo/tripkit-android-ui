package com.skedgo.tripkit.ui.generic.action_list

import com.skedgo.tripkit.common.model.booking.confirmation.BookingConfirmationAction

data class Action(
    val label: String,
    val textColor: Int = android.R.color.black,
    val bookingConfirmationAction: BookingConfirmationAction? = null
)
