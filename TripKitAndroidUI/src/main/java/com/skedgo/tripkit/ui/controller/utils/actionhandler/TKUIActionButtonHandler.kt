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
    private val eventBus: ViewControllerEventBus,
    private val favoriteTripsRepository: FavoriteTripsRepository
) : ActionButtonHandler() {

    private var actionList = mutableListOf<ActionButton>()
    private var favouriteText = ""
    private var unfavouriteText = ""

    override fun primaryActionClicked(trip: Trip) {
        findSegmentAndLaunchPreview(trip, true)
    }

    private fun findSegmentAndLaunchPreview(trip: Trip, fromListOverviewAction: Boolean = false) {
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
        var favText = if (favoriteTripsRepository.isFavoriteTrip(trip.uuid())) {
            unfavouriteText
        } else {
            favouriteText
        }
        actionList.add(
            ActionButton(
                context.getString(R.string.go),
                "go",
                R.drawable.ic_directions,
                true
            )
        )
        actionList.add(ActionButton(favText, "favorite", R.drawable.ic_bookmark, false))
        actionList.add(
            ActionButton(
                context.getString(R.string.share),
                "share",
                R.drawable.ic_share,
                false
            )
        )

        val globalConfigs = TripKit.getInstance().configs()

        if (globalConfigs.showReportProblemOnTripAction()) {
            actionList.add(
                ActionButton(
                    context.getString(R.string.report_problem),
                    "report",
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
            "share" -> eventBus.publish(ViewControllerEvent.OnShareTrip(trip))
            "go" -> findSegmentAndLaunchPreview(trip)
            "favorite" -> handleFavoriteClick(trip, viewModel)
            "report" -> eventBus.publish(ViewControllerEvent.OnLaunchReportingTripBug(trip))
            else -> super.actionClicked(context, tag, trip, viewModel)
        }
    }

    private fun handleFavoriteClick(trip: Trip, viewModel: ActionButtonViewModel) {
        container?.scope()?.launch {
            withContext(Dispatchers.IO) {
                if (favoriteTripsRepository.isFavoriteTrip(trip.uuid())) {
                    favoriteTripsRepository.deleteFavoriteTrip(trip.toFavoriteTrip())
                    viewModel.title.set(favouriteText)
                } else {
                    favoriteTripsRepository.saveFavoriteTrip(trip.toFavoriteTrip())
                    viewModel.title.set(unfavouriteText)
                }
            }
        }
    }

}