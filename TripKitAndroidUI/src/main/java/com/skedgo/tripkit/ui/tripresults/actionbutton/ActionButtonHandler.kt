package com.skedgo.tripkit.ui.tripresults.actionbutton

import android.content.Context
import androidx.databinding.ObservableField
import androidx.lifecycle.ViewModel
import com.skedgo.tripkit.routing.Trip
import com.skedgo.tripkit.routing.TripSegment
import com.skedgo.tripkit.routing.getMainTripSegment
import com.skedgo.tripkit.ui.R
import com.skedgo.tripkit.ui.utils.ITEM_EXTERNAL_BOOKING
import com.skedgo.tripkit.ui.utils.ITEM_QUICK_BOOKING
import com.skedgo.tripkit.ui.utils.ITEM_SERVICE
import com.skedgo.tripkit.ui.utils.correctItemType

/**
 * Trip results will display individual action buttons for different segments, for example, "View timetable" for public
 * transit results, or "Ride motorbike" for motorbike results. You can customize the provided text and functionality
 * that the action button provides by inheriting from this class, creating a ViewModelProvider.Factory to create it, and providing
 * the factory to the TripResultListFragment builder.
 *
 */
open class ActionButtonHandler {
    protected fun segmentSearch(trip: Trip): TripSegment? {
        return trip.segments.find {
            val itemType = it.correctItemType()
            (itemType == ITEM_QUICK_BOOKING || itemType == ITEM_SERVICE || itemType == ITEM_EXTERNAL_BOOKING)
        }
    }
    /**
    Given a trip, provide an action string, or return NULL if the action button should not be shown.
     */
    open fun getAction(context: Context, trip: Trip): ObservableField<String>? {
        val foundSegment = segmentSearch(trip)
        val type = foundSegment?.correctItemType()
        val result = ObservableField<String>()
        if (type == ITEM_SERVICE) {
            result.set(context.getString(R.string.view_times))
            return result
        } else if (type == ITEM_QUICK_BOOKING
                || type == ITEM_EXTERNAL_BOOKING){
            result.set(foundSegment?.booking?.title)
            return result
        } else {
            val mainSegment = trip.getMainTripSegment()
            if (mainSegment != null && mainSegment.miniInstruction != null && mainSegment.miniInstruction.instruction != null) {
                result.set(mainSegment.miniInstruction.instruction)
                return result
            }
        }

        return null
    }

    open fun actionClicked(trip: Trip) : Boolean {
        return false
    }

}