package com.skedgo.tripkit.ui.controller.tripdetailsviewcontroller

import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import com.jakewharton.rxrelay2.PublishRelay
import com.skedgo.tripkit.model.ViewTrip
import com.skedgo.tripkit.routing.TripGroup
import com.skedgo.tripkit.routing.TripSegment
import com.skedgo.tripkit.ui.R
import com.skedgo.tripkit.ui.TripKitUI
import com.skedgo.tripkit.ui.controller.ViewControllerEvent
import com.skedgo.tripkit.ui.controller.ViewControllerEventBus
import com.skedgo.tripkit.ui.controller.utils.actionhandler.TKUIActionButtonHandlerFactory
import com.skedgo.tripkit.ui.core.BaseFragment
import com.skedgo.tripkit.ui.databinding.FragmentTkuiTripDetailsViewControllerBinding
import com.skedgo.tripkit.ui.favorites.trips.FavoriteTrip
import com.skedgo.tripkit.ui.map.home.TripKitMapFragment
import com.skedgo.tripkit.ui.routingresults.TripGroupRepository
import com.skedgo.tripkit.ui.tripresult.TripResultPagerFragment
import com.skedgo.tripkit.ui.tripresult.TripSegmentListFragment
import com.skedgo.tripkit.ui.tripresults.actionbutton.ActionButtonHandlerFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.rx2.awaitFirstOrNull
import javax.inject.Inject


class TKUITripDetailsViewControllerFragment :
    BaseFragment<FragmentTkuiTripDetailsViewControllerBinding>() {

    @Inject
    lateinit var tripGroupRepository: TripGroupRepository

    @Inject
    lateinit var tkuiActionButtonHandlerFactory: TKUIActionButtonHandlerFactory

    @Inject
    lateinit var eventBus: ViewControllerEventBus

    private var trip: ViewTrip? = null
    private var tripGroupId: String? = null
    private var tripId: Long? = -1
    private var favoriteTripId: String? = null
    private var tripGroupList: List<TripGroup>? = null
    private var pagerFragment: TripResultPagerFragment? = null
    private var actionButtonHandlerFactory: ActionButtonHandlerFactory? = null

    var tripKitMapFragment: TripKitMapFragment? = null
        set(value) {
            field = value
            value?.setContributor(pagerFragment?.contributor())
        }

    var initialTripSegment: TripSegment? = null
    val initializationRelay = PublishRelay.create<Boolean>()

    override val layoutRes: Int
        get() = R.layout.fragment_tkui_trip_details_view_controller

    override val observeAccessibility: Boolean = false

    override fun getDefaultViewForAccessibility(): View? = null

    override fun onAttach(context: Context) {
        TripKitUI.getInstance().controllerComponent().inject(this)
        super.onAttach(context)
    }

    override fun clearInstances() {
        super.clearInstances()
        trip = null
        tripGroupId = null
        tripId = null
        favoriteTripId = null
        tripGroupList = null
        pagerFragment = null
        actionButtonHandlerFactory = null
        tripKitMapFragment = null
        initialTripSegment = null
    }

    override fun onCreated(savedInstance: Bundle?) {
        initPagerFragment()
    }

    fun settled() {
        tripKitMapFragment?.apply {
            setContributor(pagerFragment?.contributor())
            setShowPoiMarkers(false, null)
        }
    }

    private fun initPagerFragment() {

        if (pagerFragment == null) {

            if (actionButtonHandlerFactory == null) {
                actionButtonHandlerFactory = tkuiActionButtonHandlerFactory
            }

            val pagerFragmentBuilder = TripResultPagerFragment.Builder()
                .showCloseButton()
                .withActionButtonHandlerFactory(actionButtonHandlerFactory)

            when {
                trip != null -> {
                    pagerFragmentBuilder.withViewTrip(trip!!)
                }

                tripGroupId != null -> {
                    pagerFragmentBuilder.showSingleRoute().withTripGroupId(tripGroupId)
                        .withTripId(tripId)
                }

                favoriteTripId != null -> {
                    pagerFragmentBuilder.withFavoriteTripId(favoriteTripId)
                }
            }

            val sortedList = ArrayList<TripGroup>()
            tripGroupList?.forEach {
                if (it.uuid().equals(trip?.tripGroupUUID ?: tripGroupId)) {
                    sortedList.add(0, it)
                } else {
                    sortedList.add(it)
                }
            }
            pagerFragmentBuilder.withInitialTripGroupList(sortedList)

            pagerFragment = pagerFragmentBuilder.build()
            pagerFragment?.setOnCloseButtonListener {
                eventBus.publish(ViewControllerEvent.OnCloseAction())
            }
            pagerFragment?.setOnTripUpdatedListener { trip ->

                trip?.group?.let {
                    val list = ArrayList<TripGroup>()
                    list.add(it)
                    eventBus.publish(ViewControllerEvent.OnReportPlannedTrip(list, trip))
                }

                binding.layoutLoading.isVisible = trip == null
            }
            pagerFragment?.tripSegmentClickListener =
                object : TripSegmentListFragment.OnTripSegmentClickListener {
                    override fun tripSegmentClicked(tripSegment: TripSegment) {
                        eventBus.publish(ViewControllerEvent.OnTripSegmentClicked(tripSegment))
                    }
                }

            childFragmentManager
                .beginTransaction()
                .replace(R.id.container, pagerFragment!!)
                .addToBackStack(null)
                .commitAllowingStateLoss()

        }

        initializationRelay.accept(true)

        initialTripSegment?.let {
            tripGroupId?.let {
                viewTripSegment(it)
            }
            initialTripSegment = null
        }
    }

    private fun viewTripSegment(tripGroupId: String) {
        CoroutineScope(Dispatchers.IO).launch {
            val tripGroup = tripGroupRepository.getTripGroup(tripGroupId).awaitFirstOrNull()
            tripGroup?.trips?.first()?.segments?.first()?.let {
                eventBus.publish(ViewControllerEvent.OnTripSegmentClicked(it))
            }
        }
    }

    fun updatePagerFragmentTripGroup(tripGroup: TripGroup) {
        pagerFragment?.updatePagerFragmentTripGroup(tripGroup)
    }

    fun updateTripGroupResult(tripGroup: List<TripGroup>) {
        pagerFragment?.updateTripGroupResult(tripGroup)
    }

    companion object {
        const val TAG = "TKUITripDetailsViewControllerFragment"

        fun newInstance(
            favoriteTrip: FavoriteTrip,
            tripGroupList: List<TripGroup>? = null,
            factory: ActionButtonHandlerFactory?
        ) = TKUITripDetailsViewControllerFragment().apply {
            this.favoriteTripId = favoriteTrip.uuid
            this.tripGroupList = tripGroupList
            this.actionButtonHandlerFactory = factory
        }

        fun newInstance(
            trip: ViewTrip,
            tripGroupList: List<TripGroup>? = null,
            factory: ActionButtonHandlerFactory? = null
        ) = TKUITripDetailsViewControllerFragment().apply {
            this.trip = trip
            this.tripGroupList = tripGroupList
            this.actionButtonHandlerFactory = factory
        }

        fun newInstance(
            tripGroupId: String,
            tripGroupList: List<TripGroup>? = null,
            factory: ActionButtonHandlerFactory?
        ) = TKUITripDetailsViewControllerFragment().apply {
            this.tripGroupId = tripGroupId
            this.tripGroupList = tripGroupList
            this.actionButtonHandlerFactory = factory
        }
    }
}