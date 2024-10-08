package com.skedgo.tripkit.ui.controller.timetableviewcontroller

import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.skedgo.TripKit
import com.skedgo.tripkit.common.model.stop.ScheduledStop
import com.skedgo.tripkit.ui.R
import com.skedgo.tripkit.ui.TripKitUI
import com.skedgo.tripkit.ui.controller.ViewControllerEvent
import com.skedgo.tripkit.ui.controller.ViewControllerEventBus
import com.skedgo.tripkit.ui.core.BaseFragment
import com.skedgo.tripkit.ui.databinding.FragmentTkuiTimetableControllerBinding
import com.skedgo.tripkit.ui.map.home.TripKitMapFragment
import com.skedgo.tripkit.ui.model.TimetableEntry
import com.skedgo.tripkit.ui.servicedetail.ServiceDetailFragment
import com.skedgo.tripkit.ui.timetables.TimetableFragment
import com.skedgo.tripkit.ui.timetables.TimetableMapContributor
import javax.inject.Inject

class TKUITimetableControllerFragment : BaseFragment<FragmentTkuiTimetableControllerBinding>() {

    @Inject
    lateinit var eventBus: ViewControllerEventBus

    private lateinit var stop: ScheduledStop

    private var serviceDetailsFragment: ServiceDetailFragment? = null
    private var mapFragment: TripKitMapFragment? = null
        set(value) {
            field = value
            value?.setContributor(serviceDetailsFragment?.contributor())
        }

    private var timetableFragment: TimetableFragment? = null

    override val layoutRes: Int
        get() = R.layout.fragment_tkui_timetable_controller

    override val observeAccessibility: Boolean = false

    override fun getDefaultViewForAccessibility(): View? = null

    private val backPressedCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            if (popServiceFragment()) {
                // Remove the callback so that the next time the user presses back, the HomeFragment will
                // take care of it.
                this.remove()
            }
        }
    }

    override fun onAttach(context: Context) {
        TripKitUI.getInstance().controllerComponent().inject(this)
        super.onAttach(context)
    }

    override fun onCreated(savedInstance: Bundle?) {}

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        //setupTimeTableFragment()
    }

    override fun onResume() {
        super.onResume()
        setupTimeTableFragment()
    }

    override fun onAttachFragment(childFragment: Fragment) {
        super.onAttachFragment(childFragment)
        if (childFragment is ServiceDetailFragment) {
            mapFragment?.setContributor(childFragment.contributor())
        }
    }

    override fun clearInstances() {
        super.clearInstances()
        if (timetableFragment != null) {
            timetableFragment?.clearInstances()
            timetableFragment = null
        }
        serviceDetailsFragment = null
        mapFragment = null
    }

    private fun setupTimeTableFragment() {
        val timetableFragmentBuilder = TimetableFragment.Builder()
            .withStop(stop)
            .withButton(getString(R.string.go), R.layout.layout_go_button)

        val globalConfigs = TripKit.getInstance().configs()
        if (!globalConfigs.hideFavorites()) {
            timetableFragmentBuilder.withButton(
                getString(R.string.favorites),
                R.layout.layout_favorite_button
            )
        }

        timetableFragmentBuilder.withButton(getString(R.string.share), R.layout.layout_share_button)
            .showCloseButton()


        timetableFragment = timetableFragmentBuilder.build()

        timetableFragment?.apply {

            setOnCloseButtonListener {
                eventBus.publish(ViewControllerEvent.OnCloseAction())
            }

            addOnTimetableEntrySelectedListener { entry, stop, l ->
                loadDetails(entry, stop, l)
            }

            setOnTripKitButtonClickListener { tag, scheduledStop ->
                when (tag) {
                    R.id.goButton -> {
                        eventBus.publish(
                            ViewControllerEvent.OnRouteFromCurrentLocation(
                                scheduledStop
                            )
                        )
                    }

                    R.id.shareButton -> {
                        timetableFragment?.showShareDialog()
                    }

                    R.id.favoriteButton -> {

                    }
                }
            }

            this@TKUITimetableControllerFragment.childFragmentManager
                .beginTransaction()
                .replace(R.id.content, this)
                .addToBackStack(null)
                .commitAllowingStateLoss()
        }
    }

    private fun popServiceFragment(): Boolean {
        childFragmentManager.findFragmentById(R.id.content)?.let { fragment ->
            if (fragment is ServiceDetailFragment) {
                childFragmentManager.popBackStackImmediate()
                return true
            }
        }
        return false
    }

    private fun loadDetails(
        timetableEntry: TimetableEntry,
        scheduledStop: ScheduledStop,
        time: Long
    ) {

        mapFragment?.setShowPoiMarkers(false, null)

        serviceDetailsFragment = ServiceDetailFragment.Builder()
            .withStop(scheduledStop)
            .showCloseButton()
            .withTimetableEntry(timetableEntry).build().apply {
                setOnCloseButtonListener {
                    popServiceFragment()
                    resetMapState()
                }
            }

        // We need to manually pop the backstack when necessary, as the HomeFragment won't know to do it.
        requireActivity().onBackPressedDispatcher.addCallback(this, backPressedCallback)

        childFragmentManager
            .beginTransaction()
            .replace(R.id.content, serviceDetailsFragment!!)
            .addToBackStack(null)
            .commitAllowingStateLoss()

        eventBus.publish(
            ViewControllerEvent.OnUpdateBottomSheetState(BottomSheetBehavior.STATE_HALF_EXPANDED)
        )
    }

    private fun resetMapState() {
        mapFragment?.setShowPoiMarkers(true, emptyList())
        val contributor = serviceDetailsFragment?.contributor()
        if (contributor is TimetableMapContributor) {
            contributor.getMapPreviousPosition().let {
                it?.let { mapFragment?.moveToCameraPosition(it) }
            }
        }
    }

    fun updateData(stop: ScheduledStop) {
        timetableFragment?.updateStop(stop)
    }

    companion object {

        const val TAG = "TKUITimetableControllerFragment"

        fun newInstance(
            stop: ScheduledStop,
            mapFragment: TripKitMapFragment,
        ): TKUITimetableControllerFragment =
            TKUITimetableControllerFragment().apply {
                this.stop = stop
                this.mapFragment = mapFragment
            }
    }
}