package com.skedgo.tripkit.ui.tripresults

import android.content.Context
import android.os.Parcelable
import com.skedgo.tripkit.routing.Trip

/**
 * Trip results will display individual action buttons for different segments, for example, "View timetable" for public
 * transit results, or "Ride motorbike" for motorbike results. You can customize the provided text and functionality
 * that the action button provides by inheriting from this class and providing it in the `TripResultListFragment` builder.
 */
interface ActionButtonHandler : Parcelable {
    /**
       Given a trip, provide an action string, or return NULL if the action button should not be shown.
     */
    fun getAction(context: Context, trip: Trip): String?

    fun actionClicked(trip: Trip)
}