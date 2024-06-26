package com.skedgo.tripkit.ui.tripresults.actionbutton

import android.content.Context
import androidx.annotation.DrawableRes
import androidx.databinding.ObservableField
import com.skedgo.tripkit.routing.Trip
import com.skedgo.tripkit.routing.TripSegment
import com.skedgo.tripkit.routing.getMainTripSegment
import com.skedgo.tripkit.ui.tripresult.ActionButtonViewModel
import com.skedgo.tripkit.ui.utils.ITEM_EXTERNAL_BOOKING
import com.skedgo.tripkit.ui.utils.ITEM_QUICK_BOOKING
import com.skedgo.tripkit.ui.utils.ITEM_SERVICE
import com.skedgo.tripkit.ui.utils.correctItemType

data class ActionButton(
    val text: String,
    val tag: String,
    @DrawableRes val icon: Int,
    val isPrimary: Boolean,
    val useIconTint: Boolean = true
)

/**
 * Trip results will display individual action buttons for different segments, for example, "View timetable" for public
 * transit results, or "Ride motorbike" for motorbike results. You can customize the provided text and functionality
 * that the action button provides by inheriting from this class, creating a ViewModelProvider.Factory to create it, and providing
 * the factory to the TripResultListFragment builder.
 *
 * Trip details can also show multiple actions, such as "Add to favorites" or "Route". Those should be provided by overriding
 * `getActions()`
 */
open class ActionButtonHandler {

    companion object {
        const val ACTION_TAG_GO = "go"
        const val ACTION_TAG_SHARE = "share"
        const val ACTION_TAG_FAVORITE = "favorite"
        const val ACTION_TAG_REPORT = "report"
        const val ACTION_TAG_ALERT = "alert"
        const val ACTION_EXTERNAL_SHOW_TICKET = "showTicket"
    }

    var container: ActionButtonContainer? = null
    protected fun segmentSearch(trip: Trip): TripSegment? {
        return trip.segments.find {
            val itemType = it.correctItemType()
            (itemType == ITEM_QUICK_BOOKING || itemType == ITEM_EXTERNAL_BOOKING)
        }
    }

    /**
    Given a trip, provide an action string, or return NULL if the action button should not be shown.
     */
    open fun getPrimaryAction(context: Context, trip: Trip): ObservableField<String>? {
        val foundSegment = segmentSearch(trip)
        val type = foundSegment?.correctItemType()
        val result = ObservableField<String>()
        if (type == ITEM_SERVICE) {
            // TODO: More & Less button to expand the list of extra services
//            result.set(context.getString(R.string.view_times))
            return result
        } else if (type == ITEM_QUICK_BOOKING
            || type == ITEM_EXTERNAL_BOOKING
        ) {
            result.set(foundSegment.booking?.title)
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

    /**
     * Given a trip, return a list of actions that can be taken. The default implementation lists nothing.
     */
    open suspend fun getActions(context: Context, trip: Trip): List<ActionButton> {
        return listOf<ActionButton>()
    }

    open fun primaryActionClicked(trip: Trip) {

    }

    open fun actionClicked(
        context: Context,
        tag: String,
        trip: Trip,
        viewModel: ActionButtonViewModel
    ) {
    }

    // Interim solution, should find a better workaround for this one
    open fun handleCustomAction(tag: String, data: Any) {}
}