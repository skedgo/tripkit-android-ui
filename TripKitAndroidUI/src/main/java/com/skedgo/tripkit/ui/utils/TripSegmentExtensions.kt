package com.skedgo.tripkit.ui.utils

import com.skedgo.tripkit.routing.TripSegment

const val ITEM_DEFAULT = 0
const val ITEM_DIRECTIONS = 1
const val ITEM_NEARBY = 2
const val ITEM_MODE_LOCATION = 3
const val ITEM_SERVICE = 4
const val ITEM_QUICK_BOOKING = 5
const val ITEM_EXTERNAL_BOOKING = 6

fun TripSegment.correctItemType(): Int {
    return if (this.turnByTurn != null) {
        ITEM_DIRECTIONS
    } else if (this.mode?.isPublicTransport == true && this.from != null) {
        ITEM_SERVICE
    } else if (this.modeInfo?.id == "stationary_vehicle-collect" || this.hasCarParks()) {
        ITEM_NEARBY
    }  else if (this.booking?.quickBookingsUrl != null || this.booking?.confirmation != null) {
        ITEM_QUICK_BOOKING
    } else if (this.booking?.externalActions != null && this.booking.externalActions!!.count() > 0) {
        ITEM_EXTERNAL_BOOKING
    } else {
        ITEM_DEFAULT
    }
}