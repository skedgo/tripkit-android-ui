package com.technologies.tripkituisample

import android.content.Context
import com.skedgo.tripkit.routing.Trip
import com.skedgo.tripkit.routing.getMainTripSegment
import com.skedgo.tripkit.routing.getSummarySegments
import com.skedgo.tripkit.ui.tripresult.ActionButtonViewModel
import com.skedgo.tripkit.ui.tripresults.actionbutton.ActionButton
import com.skedgo.tripkit.ui.tripresults.actionbutton.ActionButtonHandler
import javax.inject.Inject

class TripKitUISampleActionButtonHandler @Inject constructor(
    private val eventBus: AppEventBus
) : ActionButtonHandler() {

    private var actionList = mutableListOf<ActionButton>()

    override fun primaryActionClicked(trip: Trip) {
        findSegmentAndLaunchPreview(trip)
    }

    private fun findSegmentAndLaunchPreview(trip: Trip) {
        var foundSegment = segmentSearch(trip)
        if (foundSegment == null) {
            foundSegment = trip.getMainTripSegment() ?: kotlin.run {
                trip.getSummarySegments().firstOrNull()
            }
        }

        foundSegment?.let {
            eventBus.publish(
                AppEvent.OnActionClicked(
                    TripActions.Go,
                    tripSegment = it
                )
            )
        }
    }

    override suspend fun getActions(context: Context, trip: Trip): List<ActionButton> {
        actionList.clear()
        actionList.add(
            ActionButton(
                TripActions.Go.value,
                TripActions.Go.value,
                R.drawable.ic_direction,
                true
            )
        )
        actionList.add(
            ActionButton(
                TripActions.Share.value,
                TripActions.Share.value,
                R.drawable.ic_share,
                true
            )
        )
        return actionList
    }

    override fun actionClicked(
        context: Context,
        tag: String,
        trip: Trip,
        viewModel: ActionButtonViewModel
    ) {
        eventBus.publish(
            AppEvent.OnActionClicked(
                TripActions.valueOf(tag),
                actionButtonViewModel = viewModel,
                trip = trip
            )
        )
        super.actionClicked(context, tag, trip, viewModel)
    }
}

enum class TripActions(val value: String) {
    Go("Go"),
    Share("Share"),
}