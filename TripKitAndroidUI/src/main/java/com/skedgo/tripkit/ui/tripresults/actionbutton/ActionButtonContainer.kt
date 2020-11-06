package com.skedgo.tripkit.ui.tripresults.actionbutton

import com.skedgo.tripkit.routing.Trip
import com.skedgo.tripkit.routing.TripGroup
import kotlinx.coroutines.CoroutineScope


interface ActionButtonContainer {
    fun scope(): CoroutineScope
    fun replaceTripGroup(tripGroupUuid: String, newTripGroup: TripGroup)
}