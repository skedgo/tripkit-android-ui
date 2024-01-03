package com.skedgo.tripkit.ui.trippreview

import androidx.lifecycle.MutableLiveData
import com.skedgo.tripkit.routing.Trip
import com.skedgo.tripkit.routing.TripGroup
import com.skedgo.tripkit.routing.TripSegment
import com.skedgo.tripkit.ui.model.TimetableEntry
import kotlinx.coroutines.CoroutineScope

interface TripPreviewPagerListener {
    fun onServiceActionButtonClicked(_tripSegment: TripSegment?, action: String?)

    @Deprecated("Keep the logic on the [TimetableFragment] and use [viewTimetableEntry] instead")
    fun onTimetableEntryClicked(
        segment: TripSegment?,
        scope: CoroutineScope,
        entry: TimetableEntry
    )

    fun viewTimetableEntry(
        tripGroup: TripGroup,
        trip: Trip,
        segment: TripSegment
    )

    fun reportPlannedTrip(trip: Trip?, tripGroups: List<TripGroup>)
    fun onBottomSheetResize(): MutableLiveData<Int>
    fun onRestartHomePage()

    @Deprecated("UnusedClass")
    fun onExternalActionButtonClicked(action: String?)

    fun onToggleBottomSheetDrag(isDraggable: Boolean)
    fun getCurrentPagerItemType(): Int
    fun getLatestTrip(): Trip?
    fun showUpdateLoader(show: Boolean, message: String)
}