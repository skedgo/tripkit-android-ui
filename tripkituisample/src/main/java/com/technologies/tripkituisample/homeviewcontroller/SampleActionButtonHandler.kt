package com.technologies.tripkituisample.homeviewcontroller

import android.content.Context
import android.widget.Toast
import com.skedgo.tripkit.routing.Trip
import com.skedgo.tripkit.ui.controller.ViewControllerEventBus
import com.skedgo.tripkit.ui.controller.utils.actionhandler.TKUIActionButtonHandler
import com.skedgo.tripkit.ui.tripresult.ActionButtonViewModel
import com.skedgo.tripkit.ui.tripresults.actionbutton.ActionButton
import com.technologies.tripkituisample.R
import javax.inject.Inject


open class SampleActionButtonHandler @Inject constructor(
    eventBus: ViewControllerEventBus
) : TKUIActionButtonHandler(eventBus) {

    override fun findSegmentAndLaunchPreview(trip: Trip, fromListOverviewAction: Boolean) {
        //can add additional actions here then super.findSegmentAndLaunchPreview will trigger the eventBus
        super.findSegmentAndLaunchPreview(trip, fromListOverviewAction)
    }

    override suspend fun getActions(context: Context, trip: Trip): List<ActionButton> {
        //we have default actions that you can use from super.getActions(context, trip)
        //which are "Go" primary action, "Favorite", "Share"
        //and "Report" (if config.showReportProblemOnTripAction is true)
        val defaultAction = super.getActions(context, trip).toMutableList()
        defaultAction.add(
            ActionButton(
                "Custom 1",
                ACTION_TAG_CUSTOM_ACTION_1,
                R.drawable.ic_sample_action_1,
                false,
            )
        )
        return defaultAction
    }

    override fun actionClicked(context: Context, tag: String, trip: Trip, viewModel: ActionButtonViewModel) {
        //you can override action clicks here or use the default super.actionClicked(context, tag, trip, viewModel)
        Toast.makeText(context, "action clicked: $tag for trip ${trip.id}", Toast.LENGTH_LONG).show()
        when (tag) {
            //for example, creating a custom call when Favorite action is clicked
            ACTION_TAG_FAVORITE -> favoriteCustomFunction(trip, viewModel)
            ACTION_TAG_CUSTOM_ACTION_1 -> {
                Toast.makeText(context, "custom action 1 clicked", Toast.LENGTH_LONG).show()
            }
            else -> super.actionClicked(context, tag, trip, viewModel)
        }
    }

    private fun favoriteCustomFunction(trip: Trip, viewModel: ActionButtonViewModel) {

    }

    //default favorite action or ACTION_TAG_FAVORITE calls this function when clicked
    override fun handleFavoriteClick(trip: Trip, viewModel: ActionButtonViewModel) {
        super.handleFavoriteClick(trip, viewModel)
    }

    //default favorite action or ACTION_TAG_FAVORITE calls this function to check if trip is in favorites
    override fun isTripFavorite(trip: Trip): Boolean {
        return super.isTripFavorite(trip)
    }

    companion object {
        const val ACTION_TAG_CUSTOM_ACTION_1 = "custom_action_1"
    }
}