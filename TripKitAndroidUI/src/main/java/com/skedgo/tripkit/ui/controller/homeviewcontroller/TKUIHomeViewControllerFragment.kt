package com.skedgo.tripkit.ui.controller.homeviewcontroller

import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.FrameLayout
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.google.android.gms.maps.GoogleMap
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.skedgo.geocoding.LatLng
import com.skedgo.tripkit.common.model.Location
import com.skedgo.tripkit.common.model.ScheduledStop
import com.skedgo.tripkit.ui.R
import com.skedgo.tripkit.ui.TripKitUI
import com.skedgo.tripkit.ui.controller.ViewControllerEvent
import com.skedgo.tripkit.ui.controller.ViewControllerEventBus
import com.skedgo.tripkit.ui.controller.locationsearchcontroller.TKUILocationSearchViewControllerFragment
import com.skedgo.tripkit.ui.controller.routeviewcontroller.TKUIRouteFragment
import com.skedgo.tripkit.ui.controller.timetableviewcontroller.TKUITimetableControllerFragment
import com.skedgo.tripkit.ui.controller.tripresultcontroller.TKUITripResultsFragment
import com.skedgo.tripkit.ui.controller.utils.LocationField
import com.skedgo.tripkit.ui.core.BaseFragment
import com.skedgo.tripkit.ui.core.addTo
import com.skedgo.tripkit.ui.databinding.FragmentTkuiHomeViewControllerBinding
import com.skedgo.tripkit.ui.locationpointer.LocationPointerFragment
import com.skedgo.tripkit.ui.map.home.TripKitMapFragment
import com.skedgo.tripkit.ui.search.FixedSuggestions
import io.reactivex.Completable
import io.reactivex.android.schedulers.AndroidSchedulers
import java.util.concurrent.TimeUnit

class TKUIHomeViewControllerFragment :
    BaseFragment<FragmentTkuiHomeViewControllerBinding>() {

    private val viewModel: TKUIHomeViewControllerViewModel by viewModels()

    lateinit var mapFragment: TripKitMapFragment
    lateinit var map: GoogleMap
    lateinit var locationPointerFragment: LocationPointerFragment
    lateinit var bottomSheetFragment: TKUIHomeBottomSheetFragment

    lateinit var bottomSheetBehavior: BottomSheetBehavior<FrameLayout>

    var defaultLocation: LatLng? = null
    private val fixedSuggestionsProvider = TKUIHomeViewFixedSuggestionsProvider()

    private val eventBus = ViewControllerEventBus

    private val searchCardListener =
        object : TKUILocationSearchViewControllerFragment.TKUILocationSearchViewControllerListener {
            override fun onLocationSelected(location: Location) {
                routeLocation(location)
            }

            override fun onFixedSuggestionSelected(any: Any) {
                handleFixedSuggestionAction(any)
            }

            override fun onCitySelected(location: Location) {

            }

            override fun onInfoSelected(location: Location) {

            }
        }

    override val layoutRes: Int
        get() = R.layout.fragment_tkui_home_view_controller

    override val observeAccessibility: Boolean
        get() = false

    override fun getDefaultViewForAccessibility(): View? = null

    override fun onAttach(context: Context) {
        TripKitUI.getInstance().inject(this)
        super.onAttach(context)
    }

    override fun onCreated(savedInstance: Bundle?) {
        initBinding()
        initMap()
        initViews()
        initObservers()
        handleBackPress()
    }

    private fun handleBackPress() {
        requireActivity().onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    if (viewModel.state.value?.isChooseOnMap == true) {
                        viewModel.toggleChooseOnMap(false)
                    } else if (bottomSheetFragment.childFragmentManager.backStackEntryCount > 1) {
                        bottomSheetFragment.popActiveFragment()
                    } else {
                        remove()
                        activity?.onBackPressed()
                    }
                }
            }
        )
    }

    private fun initBinding() {
        binding.lifecycleOwner = this
        binding.viewModel = viewModel
    }

    private fun initMap() {
        mapFragment = childFragmentManager.findFragmentById(R.id.mapFragment) as TripKitMapFragment
        mapFragment.getMapAsync {
            map = it
            defaultLocation?.let { moveMapToDefaultLocation(it) }
            setupLocationPointerFragment()
        }

        mapFragment.setOnInfoWindowClickListener { location ->
            if (location is ScheduledStop) {
                // Show timetable
                loadTimetable(location)
            } else {
                loadPoiDetails(location)
            }
        }

        locationPointerFragment =
            childFragmentManager.findFragmentById(R.id.locationPointerFragment) as LocationPointerFragment

    }

    private fun loadTimetable(stop: ScheduledStop) {
        val fragment = TKUITimetableControllerFragment.newInstance(
            stop,
            mapFragment,
            eventBus
        )

        val timetableFragment = bottomSheetFragment
            .childFragmentManager
            .findFragmentByTag(TKUITimetableControllerFragment.TAG)

        if (timetableFragment != null && timetableFragment is TKUITimetableControllerFragment) {
            timetableFragment.updateData(stop)
            if (!timetableFragment.isVisible) {
                bottomSheetFragment
                    .childFragmentManager
                    .popBackStackImmediate(TKUITimetableControllerFragment.TAG, 0)
            }
        } else {
            updateBottomSheetFragment(fragment, TKUITimetableControllerFragment.TAG)
        }

        if (viewModel.state.value?.isChooseOnMap == true) {
            activity?.onBackPressed()
            viewModel.toggleChooseOnMap(false)
        }

        bottomSheetBehavior.state = BottomSheetBehavior.STATE_HALF_EXPANDED
    }

    private fun loadPoiDetails(
        location: Location?,
        isRouting: Boolean = false,
        isDeparture: Boolean = false
    ) {
        /*
        location?.let {
            val fragment = PoiDetailsFragment.newInstance(it, isRouting, isDeparture)

            val poiDetailsFragment = childFragmentManager.findFragmentByTag(PoiDetailsFragment.TAG)
            if (poiDetailsFragment != null && poiDetailsFragment is PoiDetailsFragment) {
                poiDetailsFragment.updateData(it)
                if (!poiDetailsFragment.isVisible) {
                    childFragmentManager.popBackStackImmediate(PoiDetailsFragment.TAG, 0)
                }
            } else {
                replaceFragment(fragment, PoiDetailsFragment.TAG)
            }

            if (locationChooserFrame.visibility == View.VISIBLE) {
                activity?.onBackPressed()
            }
        }
        */
    }

    private fun setupLocationPointerFragment() {
        locationPointerFragment.setMap(
            map,
            object : LocationPointerFragment.LocationPointerListener {
                override fun onDone(
                    location: Location,
                    field: LocationField
                ) {
                    eventBus.publish(ViewControllerEvent.OnLocationChosen(location, field))
                }

                override fun loadPoiDetails(location: Location) {

                }

                override fun onClose() {
                    viewModel.toggleChooseOnMap(false)
                }
            })
    }

    private fun routeLocation(
        location: Location
    ) {
        val routeFragment = TKUIRouteFragment.newInstance(
            map.projection.visibleRegion.latLngBounds,
            map.cameraPosition.target,
            location
        )

        updateBottomSheetFragment(
            routeFragment, TKUIRouteFragment.TAG, BottomSheetBehavior.STATE_EXPANDED
        )
    }

    private fun initViews() {
        initBottomSheet()
        binding.testAction.setOnClickListener {
            loadSearchCardFragment()
        }
    }

    fun loadSearchCardFragment() {
        if (this::map.isInitialized) {
            val bounds = map.projection.visibleRegion.latLngBounds
            val near = map.cameraPosition.target
            fixedSuggestionsProvider.showCurrentLocation = false
            val locationSearchFragment = TKUILocationSearchViewControllerFragment
                .newInstance(bounds, near, fixedSuggestionsProvider, eventBus, searchCardListener)

            updateBottomSheetFragment(
                locationSearchFragment,
                TKUILocationSearchViewControllerFragment.TAG
            )

            bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
        }
    }

    private fun updateBottomSheetFragment(
        fragment: Fragment,
        tag: String,
        state: Int = BottomSheetBehavior.STATE_HALF_EXPANDED
    ) {
        bottomSheetFragment.update(fragment, tag)
        bottomSheetBehavior.state = state
    }

    private fun initBottomSheet() {

        bottomSheetFragment = TKUIHomeBottomSheetFragment()

        childFragmentManager
            .beginTransaction()
            .replace(R.id.standardBottomSheet, bottomSheetFragment, tag)
            .addToBackStack(tag)
            .commit()

        bottomSheetBehavior = BottomSheetBehavior.from(binding.standardBottomSheet)
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
    }

    private fun initObservers() {
        eventBus.apply {
            listen(
                ViewControllerEvent.OnLocationSuggestionSelected::class.java
            ).subscribe {
                handleFixedSuggestionAction(it.suggestion)
            }.addTo(autoDisposable)

            listen(
                ViewControllerEvent.OnCloseAction::class.java
            ).subscribe {
                bottomSheetFragment.popActiveFragment()
            }.addTo(autoDisposable)

            listen(
                ViewControllerEvent.OnChooseOnMap::class.java
            ).subscribe {
                onChooseOnMap(it.locationField)
            }.addTo(autoDisposable)

            listen(
                ViewControllerEvent.OnLocationChosen::class.java
            ).subscribe {
                viewModel.toggleChooseOnMap(false)
                routeLocation(it.location)
            }.addTo(autoDisposable)

            listen(
                ViewControllerEvent.OnGetRouteTripResults::class.java
            ).subscribe {
                getRouteTrips(it.origin, it.destination)
            }.addTo(autoDisposable)
        }


    }

    private fun getRouteTrips(
        origin: Location,
        destination: Location
    ) {
        val tripResultsFragment =
            TKUITripResultsFragment.newInstance(origin, destination, true)

        updateBottomSheetFragment(
            tripResultsFragment, TKUITripResultsFragment.TAG,
            BottomSheetBehavior.STATE_EXPANDED
        )
    }

    private fun handleFixedSuggestionAction(it: Any) {
        if (it is FixedSuggestions) {
            when (it) {
                FixedSuggestions.CURRENT_LOCATION -> {}
                FixedSuggestions.CHOOSE_ON_MAP ->
                    eventBus.publish(ViewControllerEvent.OnChooseOnMap(LocationField.NONE))

                FixedSuggestions.HOME -> {}
                FixedSuggestions.WORK -> {}
            }
        }
    }

    private fun getSuggestionLocation() {

    }

    private fun moveMapToDefaultLocation(location: LatLng) {
        Completable.complete()
            .delay(2, TimeUnit.SECONDS)
            .observeOn(AndroidSchedulers.mainThread())
            .doOnComplete {
                mapFragment.moveToLatLng(location)
            }.subscribe().addTo(autoDisposable)
    }

    private fun onChooseOnMap(locationField: LocationField = LocationField.NONE) {
        locationPointerFragment.setLocationField(locationField)
        viewModel.toggleChooseOnMap(true)
    }

    /*
    private fun switchBetweenLocationChooserAndSheet(showSheet: Boolean) {
        centerPin.visibility = if (showSheet) View.GONE else View.VISIBLE
        locationChooserFrame.visibility = if (showSheet) View.GONE else View.VISIBLE
        bottomSheet.visibility = if (showSheet) View.VISIBLE else View.GONE


        if (locationChooserFrame.visibility == View.VISIBLE) {
            // We need to set the padding on the map, otherwise the center pin doesn't match up with the correct
            // address. But we can only do that after the view is made visible and measured.
            if (locationChooserFrameHeight == 0) {
                val fragment = childFragmentManager.findFragmentById(R.id.locationChooserFragment)
                fragment?.view?.post {
                    // Adjust the center pin to have the pin point on the center of the map
                    locationChooserFrameHeight = locationChooserFrame.height
                    centerPin.y = centerPin.y - (locationChooserFrameHeight / 2)
                    mMap.setPadding(0, 0, 0, locationChooserFrameHeight)
                }
            } else {
                mMap.setPadding(0, 0, 0, locationChooserFrameHeight)
            }
        } else {
            mMap.setPadding(0, 0, 0, bottomSheet.measuredHeight)
        }
    }

     */

    companion object {

        fun load(
            activity: AppCompatActivity,
            containerId: Int,
            defaultLocation: LatLng? = null
        ) {
            activity.supportFragmentManager
                .beginTransaction()
                .replace(
                    containerId,
                    newInstance(
                        defaultLocation
                    )
                )
                .addToBackStack(null)
                .commit()
        }

        fun newInstance(
            defaultLocation: LatLng? = null
        ) = TKUIHomeViewControllerFragment().apply {
            this.defaultLocation = defaultLocation
        }
    }
}