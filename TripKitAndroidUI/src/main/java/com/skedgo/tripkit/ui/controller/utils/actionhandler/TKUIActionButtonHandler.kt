package com.skedgo.tripkit.ui.controller.utils.actionhandler

import android.content.Context
import com.skedgo.TripKit
import com.skedgo.tripkit.ui.favorites.trips.FavoriteTripsRepository
import com.skedgo.tripkit.ui.favorites.trips.toFavoriteTrip
import com.skedgo.tripkit.routing.Trip
import com.skedgo.tripkit.routing.getMainTripSegment
import com.skedgo.tripkit.routing.getSummarySegments
import com.skedgo.tripkit.ui.R
import com.skedgo.tripkit.ui.controller.ViewControllerEvent
import com.skedgo.tripkit.ui.controller.ViewControllerEventBus
import com.skedgo.tripkit.ui.tripresult.ActionButtonViewModel
import com.skedgo.tripkit.ui.tripresults.actionbutton.ActionButton
import com.skedgo.tripkit.ui.tripresults.actionbutton.ActionButtonHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject


open class TKUIActionButtonHandler @Inject constructor(
    val eventBus: ViewControllerEventBus
) : ActionButtonHandler() {

    /*
    @Inject
    lateinit var eventBus: ViewControllerEventBus
    */

    private var actionList = mutableListOf<ActionButton>()
    private var favouriteText = ""
    private var unfavouriteText = ""

    override fun primaryActionClicked(trip: Trip) {
        findSegmentAndLaunchPreview(trip, true)
    }

    open fun findSegmentAndLaunchPreview(trip: Trip, fromListOverviewAction: Boolean = false) {
        var foundSegment = segmentSearch(trip)
        if (foundSegment == null) {
            foundSegment = trip.getMainTripSegment() ?: kotlin.run {
                trip.getSummarySegments().firstOrNull()
            }
        }

        foundSegment?.let {
            eventBus.publish(
                ViewControllerEvent.OnTripPrimaryActionClick(
                    it,
                    fromListOverviewAction
                )
            )
        }
    }

    override suspend fun getActions(context: Context, trip: Trip): List<ActionButton> {
        actionList.clear()
        favouriteText = context.getString(R.string.favourite)
        unfavouriteText = context.getString(R.string.remove_favourite)
        val favText = if (isTripFavorite(trip)) {
            unfavouriteText
        } else {
            favouriteText
        }
        actionList.add(
            ActionButton(
                context.getString(R.string.go),
                ACTION_TAG_GO,
                R.drawable.ic_directions,
                true
            )
        )
        actionList.add(ActionButton(favText, ACTION_TAG_FAVORITE, R.drawable.ic_bookmark, false))
        actionList.add(
            ActionButton(
                context.getString(R.string.share),
                ACTION_TAG_SHARE,
                R.drawable.ic_share,
                false
            )
        )

        val globalConfigs = TripKit.getInstance().configs()

        if (globalConfigs.showReportProblemOnTripAction()) {
            actionList.add(
                ActionButton(
                    context.getString(R.string.report_problem),
                    ACTION_TAG_REPORT,
                    R.drawable.ic_action_warning,
                    false
                )
            )
        }
        return actionList
    }


    override fun actionClicked(
        context: Context,
        tag: String,
        trip: Trip,
        viewModel: ActionButtonViewModel
    ) {
        when (tag) {
            ACTION_TAG_SHARE -> eventBus.publish(ViewControllerEvent.OnShareTrip(trip))
            ACTION_TAG_GO -> findSegmentAndLaunchPreview(trip)
            ACTION_TAG_FAVORITE -> handleFavoriteClick(trip, viewModel)
            ACTION_TAG_REPORT -> eventBus.publish(ViewControllerEvent.OnLaunchReportingTripBug(trip))
            else -> super.actionClicked(context, tag, trip, viewModel)
        }
    }

    open fun handleFavoriteClick(trip: Trip, viewModel: ActionButtonViewModel) {}

    open fun isTripFavorite(trip: Trip): Boolean {
        return false
    }

    companion object {
        const val ACTION_TAG_SHARE = "share"
        const val ACTION_TAG_GO = "go"
        const val ACTION_TAG_FAVORITE = "favorite"
        const val ACTION_TAG_REPORT = "report"
    }

}