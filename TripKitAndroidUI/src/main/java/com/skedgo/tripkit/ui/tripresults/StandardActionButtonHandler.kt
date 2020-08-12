package com.skedgo.tripkit.ui.tripresults

import android.content.Context
import android.util.Log
import com.skedgo.tripkit.routing.Trip
import kotlinx.android.parcel.Parcelize
import com.skedgo.tripkit.routing.TripSegment
import com.skedgo.tripkit.routing.getMainTripSegment
import com.skedgo.tripkit.ui.R
import com.skedgo.tripkit.ui.utils.ITEM_EXTERNAL_BOOKING
import com.skedgo.tripkit.ui.utils.ITEM_QUICK_BOOKING
import com.skedgo.tripkit.ui.utils.ITEM_SERVICE
import com.skedgo.tripkit.ui.utils.correctItemType

@Parcelize
open class StandardActionButtonHandler : ActionButtonHandler {
    protected fun segmentSearch(trip: Trip): TripSegment? {
        return trip.segments.find {
            val itemType = it.correctItemType()
            (itemType == ITEM_QUICK_BOOKING || itemType == ITEM_SERVICE || itemType == ITEM_EXTERNAL_BOOKING)
            }
    }

    override fun getAction(context: Context, trip: Trip): String? {
        val foundSegment = segmentSearch(trip)
        val type = foundSegment?.correctItemType()

        if (type == ITEM_SERVICE) {
            return context.getString(R.string.view_times)
        } else if (type == ITEM_QUICK_BOOKING
                || type == ITEM_EXTERNAL_BOOKING){
            return foundSegment?.booking?.title
        } else {
            val mainSegment = trip.getMainTripSegment()
            if (mainSegment != null && mainSegment.miniInstruction != null && mainSegment.miniInstruction.instruction != null) {
                return mainSegment.miniInstruction.instruction
            }
        }

        return null
    }

    override fun actionClicked(trip: Trip) {

    }

}