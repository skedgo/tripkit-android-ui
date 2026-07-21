package com.skedgo.tripkit.ui.tripresult.v2

import android.view.View
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.skedgo.tripkit.common.model.location.Location
import com.skedgo.tripkit.routing.TripGroup
import com.skedgo.tripkit.ui.tripresult.TripResultMapContributor
import com.skedgo.tripkit.ui.tripresult.TripSegmentListFragment
import com.skedgo.tripkit.ui.tripresult.TripSegmentListFragment.OnTripSegmentClickListener
import com.skedgo.tripkit.ui.tripresults.actionbutton.ActionButtonHandlerFactory
import io.reactivex.subjects.PublishSubject

class TripGroupsPagerAdapter(
    fragment: Fragment,
    private val tripResultMapContributor: TripResultMapContributor
) : FragmentStateAdapter(fragment) {

    var tripGroups: List<TripGroup> = emptyList()
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    var tripIds = mutableMapOf<String, Long>()

    private var actionButtonHandlerFactory: ActionButtonHandlerFactory? = null
    private var showCloseButton = false

    var closeListener: View.OnClickListener? = null
    var listener: TripSegmentListFragment.OnTripKitButtonClickListener? = null
    var segmentClickListener: OnTripSegmentClickListener? = null

    private val updateStream = PublishSubject.create<Unit>()

    private var queryFromLocation: Location? = null
    private var queryToLocation: Location? = null

    var tripAlertChangeValidator: (() -> Boolean)? = null

    fun setShowCloseButton(showCloseButton: Boolean) {
        this.showCloseButton = showCloseButton
    }

    fun setActionButtonHandlerFactory(actionButtonHandlerFactory: ActionButtonHandlerFactory?) {
        this.actionButtonHandlerFactory = actionButtonHandlerFactory
    }

    fun setQueryLocations(from: Location?, to: Location?) {
        queryFromLocation = from
        queryToLocation = to
    }

    override fun getItemCount(): Int = tripGroups.size

    override fun createFragment(position: Int): Fragment {
        val tripGroup = tripGroups[position]
        val tripId = tripIds[tripGroup.uuid()]
        return TripSegmentListFragment.Builder()
            .withTripGroupId(tripGroup.uuid())
            .withTripId(tripId ?: tripGroup.displayTripId)
            .withTripGroup(tripGroup)
            .withActionButtonHandlerFactory(actionButtonHandlerFactory)
            .withMapContributor(tripResultMapContributor)
            .showCloseButton(showCloseButton)
            .withUpdateStream(updateStream)
            .withQueryLocations(queryFromLocation, queryToLocation)
            .build().apply {
                this.position = position
                listener?.let { setOnTripKitButtonClickListener(it) }
                onCloseButtonListener = closeListener
                segmentClickListener?.let { setOnTripSegmentClickListener(it) }
                tripAlertChangeValidator?.let { setTripAlertChangeValidator(it) }
            }
    }
}
