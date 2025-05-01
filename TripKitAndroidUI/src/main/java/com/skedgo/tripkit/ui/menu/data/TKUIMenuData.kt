package com.skedgo.tripkit.ui.menu.data

abstract class TKUIMenuData : TKUIMenuItem {
    /* Commented out for now. Still finding a better implementation.
    data class ActiveBooking(val vm: ActiveBookingItemViewModel) : TKUIMenuData() {
        override fun getViewType() = ACTIVE_BOOKING_VIEW_TYPE
    }

    data class TransitTicket(val vm: TransitTicketItemViewModel) : TKUIMenuData() {
        override fun getViewType() = TRANSIT_TICKET_VIEW_TYPE
    }
    */

    companion object {
        const val ACTIVE_BOOKING_VIEW_TYPE = 1
        const val TRANSIT_TICKET_VIEW_TYPE = 2
    }
}