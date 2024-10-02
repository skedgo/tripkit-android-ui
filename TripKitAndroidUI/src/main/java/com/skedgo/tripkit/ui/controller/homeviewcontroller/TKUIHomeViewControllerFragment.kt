package com.skedgo.tripkit.ui.controller.homeviewcontroller

import android.Manifest
import android.content.Context
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.viewModels
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.lifecycleScope
import com.araujo.jordan.excuseme.ExcuseMe
import com.bumptech.glide.load.resource.bitmap.CircleCrop
import com.bumptech.glide.request.RequestOptions
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.skedgo.geocoding.LatLng
import com.skedgo.rxtry.Failure
import com.skedgo.rxtry.Success
import com.skedgo.tripkit.common.model.location.Location
import com.skedgo.tripkit.common.model.stop.ScheduledStop
import com.skedgo.tripkit.model.ViewTrip
import com.skedgo.tripkit.routing.Trip
import com.skedgo.tripkit.routing.TripGroup
import com.skedgo.tripkit.routing.TripSegment
import com.skedgo.tripkit.ui.GlideApp
import com.skedgo.tripkit.ui.R
import com.skedgo.tripkit.ui.TripKitUI
import com.skedgo.tripkit.ui.controller.ControllerDataProvider
import com.skedgo.tripkit.ui.controller.ViewControllerEvent
import com.skedgo.tripkit.ui.controller.ViewControllerEventBus
import com.skedgo.tripkit.ui.controller.locationsearchcontroller.TKUIFavoritesSuggestionProvider
import com.skedgo.tripkit.ui.controller.locationsearchcontroller.TKUILocationSearchViewControllerFragment
import com.skedgo.tripkit.ui.controller.poidetails.TKUIPoiDetailsFragment
import com.skedgo.tripkit.ui.controller.routeviewcontroller.TKUIRouteFragment
import com.skedgo.tripkit.ui.controller.timetableviewcontroller.TKUITimetableControllerFragment
import com.skedgo.tripkit.ui.controller.tripdetailsviewcontroller.TKUITripDetailsViewControllerFragment
import com.skedgo.tripkit.ui.controller.trippreviewcontroller.TKUITripPreviewFragment
import com.skedgo.tripkit.ui.controller.tripresultcontroller.TKUITripResultsFragment
import com.skedgo.tripkit.ui.controller.utils.LocationField
import com.skedgo.tripkit.ui.controller.utils.actionhandler.TKUIActionButtonHandlerFactory
import com.skedgo.tripkit.ui.core.BaseFragment
import com.skedgo.tripkit.ui.core.addTo
import com.skedgo.tripkit.ui.core.module.ViewModelFactory
import com.skedgo.tripkit.ui.databinding.FragmentTkuiHomeViewControllerBinding
import com.skedgo.tripkit.ui.dialog.UpdateModalDialog
import com.skedgo.tripkit.ui.locationpointer.LocationPointerFragment
import com.skedgo.tripkit.ui.map.home.TripKitMapFragment
import com.skedgo.tripkit.ui.model.TimetableEntry
import com.skedgo.tripkit.ui.payment.PaymentData
import com.skedgo.tripkit.ui.search.FixedSuggestions
import com.skedgo.tripkit.ui.trippreview.TripPreviewHeaderFragment
import com.skedgo.tripkit.ui.trippreview.TripPreviewPagerListener
import com.skedgo.tripkit.ui.utils.deFocusAndHideKeyboard
import com.skedgo.tripkit.ui.utils.hideKeyboard
import com.skedgo.tripkit.ui.utils.isPermissionGranted
import com.skedgo.tripkit.ui.utils.replaceFragment
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.subjects.PublishSubject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import kotlin.math.max
import kotlin.math.roundToInt

class TKUIHomeViewControllerFragment :
    BaseFragment<FragmentTkuiHomeViewControllerBinding>() {

    @Inject
    lateinit var viewModelFactory: ViewModelFactory

    @Inject
    lateinit var eventBus: ViewControllerEventBus

    private val viewModel: TKUIHomeViewControllerViewModel by viewModels { viewModelFactory }

    lateinit var mapFragment: TripKitMapFragment
    lateinit var map: GoogleMap
    lateinit var locationPointerFragment: LocationPointerFragment
    lateinit var bottomSheetFragment: TKUIHomeBottomSheetFragment
    lateinit var bottomSheetBehavior: BottomSheetBehavior<FrameLayout>

    private var bottomSheetVisibilityCallback: ((Int) -> Unit)? = null
    private var maxSheetHeight = 0
    var defaultLocation: LatLng? = null

    private var fixedSuggestionsProvider = TKUIHomeViewFixedSuggestionsProvider()

    private var tripSegmentOnPreview: TripSegment? = null
    private var tripGroupOnPreview: TripGroup? = null
    private var updateModalDialog: UpdateModalDialog? = null
    private val bottomSheetOffset = MutableLiveData(0)

    private var showMyLocationButtonWithoutPermission = false

    private var onBackPressOnEmptyBottomSheetCallback: ((OnBackPressedCallback) -> Unit)? = null

    /**
     * Listener for TKUILocationSearchViewControllerFragment actions
     */
    private val searchCardListener =
        object : TKUILocationSearchViewControllerFragment.TKUILocationSearchViewControllerListener {
            override fun onLocationSelected(location: Location) {
                routeLocation(location)
            }

            override fun onFixedSuggestionSelected(any: Any) {
                handleSuggestionAction(any)
            }

            override fun onCitySelected(location: Location) {
                handleCitySelected(location)
            }

            override fun onInfoSelected(location: Location) {
                loadPoiDetails(location)
            }
        }

    override val layoutRes: Int
        get() = R.layout.fragment_tkui_home_view_controller

    override val observeAccessibility: Boolean
        get() = false

    override fun getDefaultViewForAccessibility(): View? = null

    override fun onAttach(context: Context) {
        TripKitUI.getInstance().controllerComponent().inject(this)
        super.onAttach(context)
    }

    override fun onCreated(savedInstance: Bundle?) {

        binding.mainLayout.post {
            maxSheetHeight = binding.mainLayout.height
        }

        ControllerDataProvider.suggestionProvider = fixedSuggestionsProvider

        initBinding()
        initViews()
        initMap()
        handleBackPress()
    }

    override fun onResume() {
        super.onResume()
        initObservers()
    }

    private fun handleBackPress() {
        requireActivity().onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    if (viewModel.state.value?.isChooseOnMap == true) {
                        viewModel.toggleChooseOnMap(false)
                    } else if (bottomSheetFragment.childFragmentManager.backStackEntryCount > 0) {
                        bottomSheetFragment.popActiveFragment()
                    } else {
                        onBackPressOnEmptyBottomSheetCallback?.invoke(this)
                    }
                }
            }
        )
    }

    private fun initBinding() {
        binding.lifecycleOwner = viewLifecycleOwner
        binding.viewModel = viewModel
    }

    /**
     * Initialize map (@see com.skedgo.tripkit.ui.map.home.TripKitMapFragment)
     * Check and set if default location is available
     * Check and set my location button visibility
     */
    private fun initMap() {
        mapFragment = childFragmentManager.findFragmentById(R.id.mapFragment) as TripKitMapFragment
        mapFragment.getMapAsync {
            map = it
            bottomSheetBehavior.let { bottomSheet ->
                // We need to always show the Google logo
                map.setPadding(0, 0, 0, bottomSheet.peekHeight)
            }

            if (requireContext().isPermissionGranted(Manifest.permission.ACCESS_FINE_LOCATION)) {
                mapFragment.animateToMyLocation()
                viewModel.setMyLocationButtonVisible(true)
            } else {
                viewModel.setMyLocationButtonVisible(showMyLocationButtonWithoutPermission)
                defaultLocation?.let { location ->
                    moveMapToDefaultLocation(location)
                }
            }
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

        setupPinLocationListener()
    }

    private fun setupPinLocationListener() {
        mapFragment.pinLocationSelectedListener = { pinnedLocation, type ->
            if (mapFragment.pinnedOriginLocation == null) {
                checkLocationPermission { granted ->
                    if (granted) {
                        viewModel.getUserGeoPoint {
                            when (it) {
                                is Success -> {
                                    val currentLocation = it.invoke()
                                    mapFragment.addOriginDestinationMarker(0, currentLocation)
                                    mapFragment.addOriginDestinationMarker(1, pinnedLocation)
                                    routeLocation(currentLocation, pinnedLocation)
                                }
                                is Failure -> routeLocation(
                                    pinnedLocation
                                )
                            }
                        }
                    } else {
                        routeLocation(pinnedLocation)
                    }
                }
            } else {
                mapFragment.addOriginDestinationMarker(type, pinnedLocation)

                mapFragment.pinnedOriginLocation?.let { originLocation ->
                    mapFragment.pinnedDepartureLocation?.let { departureLocation ->
                        routeLocation(
                            originLocation,
                            departureLocation
                        )
                    }
                }
            }

            mapFragment.updatePinForType()
        }

        mapFragment.enablePinLocationOnClick = true
    }


    private fun setMapPadding(offset: Float) {
        val peekHeight = bottomSheetBehavior.peekHeight
        val maxPadding = maxSheetHeight - peekHeight
        val calculatedPadding = (offset * maxPadding).roundToInt() + peekHeight
        map.setPadding(0, 0, 0, max(calculatedPadding, peekHeight))
    }

    private fun loadTimetable(stop: ScheduledStop) {
        val fragment = TKUITimetableControllerFragment.newInstance(
            stop,
            mapFragment
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

        location?.let {
            val fragmentByTag = bottomSheetFragment.getFragmentByTag(TKUIPoiDetailsFragment.TAG)

            if (fragmentByTag != null && fragmentByTag is TKUIPoiDetailsFragment && fragmentByTag.isVisible) {
                fragmentByTag.updateData(it)
            } else {
                val fragment = TKUIPoiDetailsFragment
                    .newInstance(it, isRouting, isDeparture)

                updateBottomSheetFragment(
                    fragment, TKUIPoiDetailsFragment.TAG, BottomSheetBehavior.STATE_HALF_EXPANDED
                )
            }
        }
    }

    private var isFromChooseOnMap = false

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
                    isFromChooseOnMap = true
                    viewModel.toggleChooseOnMap(false)
                    eventBus.publish(ViewControllerEvent.OnViewPoiDetails(location))
                }

                override fun onClose() {
                    viewModel.toggleChooseOnMap(false)
                }
            })
    }

    private fun routeLocation(
        origin: Location,
        destination: Location? = null
    ) {
        val routeFragment = TKUIRouteFragment.newInstance(
            map.projection.visibleRegion.latLngBounds,
            map.cameraPosition.target,
            origin,
            destination
        )

        updateBottomSheetFragment(
            routeFragment, TKUIRouteFragment.TAG, BottomSheetBehavior.STATE_EXPANDED
        )
    }

    private fun initViews() {
        initBottomSheet()
        binding.ivMyLocation.setOnClickListener {
            mapFragment.animateToMyLocation()
        }

        val requestOptions = RequestOptions().transform(CircleCrop())
        GlideApp.with(requireContext())
            .load(R.drawable.ic_my_location)
            .apply(requestOptions)
            .into(binding.ivMyLocation)
    }

    fun loadSearchCardFragment() {
        if (this::map.isInitialized) {
            val bounds = map.projection.visibleRegion.latLngBounds
            val near = map.cameraPosition.target
            fixedSuggestionsProvider.showCurrentLocation = false
            val locationSearchFragment = TKUILocationSearchViewControllerFragment
                .newInstance(bounds, near, fixedSuggestionsProvider, searchCardListener)

            updateBottomSheetFragment(
                locationSearchFragment,
                TKUILocationSearchViewControllerFragment.TAG,
                BottomSheetBehavior.STATE_EXPANDED
            )
        }
    }

    private fun updateBottomSheetFragment(
        fragment: Fragment,
        tag: String,
        state: Int = BottomSheetBehavior.STATE_HALF_EXPANDED
    ) {
        if (fragment !is TKUIPoiDetailsFragment) {
            isFromChooseOnMap = false
        }

        bottomSheetBehavior.peekHeight =
            resources.getDimensionPixelSize(R.dimen.bottom_sheet_peek_height)
        binding.standardBottomSheet.visibility = View.VISIBLE
        bottomSheetFragment.update(fragment, tag)
        bottomSheetBehavior.state = state
        mapFragment.enablePinLocationOnClick =
            tag == TKUILocationSearchViewControllerFragment.TAG || tag == TKUIRouteFragment.TAG

        updateFabMyLocationAnchor(binding.standardBottomSheet.id, Gravity.TOP or Gravity.END)
    }

    private fun initBottomSheet() {

        bottomSheetFragment = TKUIHomeBottomSheetFragment.newInstance(object :
            TKUIHomeBottomSheetFragment.TKUIHomeBottomSheetListener {
            override fun refreshMap() {
                mapFragment.refreshMap(map)
                bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
                binding.standardBottomSheet.visibility = View.GONE
            }

            override fun removePinnedLocationMarker() {
                mapFragment.removePinnedLocationMarker()
            }

            override fun reloadMapMarkers() {
                mapFragment.setShowPoiMarkers(true, emptyList())

                Observable.timer(500, TimeUnit.MILLISECONDS)
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe {
                        map.animateCamera(CameraUpdateFactory.zoomTo(16.0f))
                    }
                    .addTo(autoDisposable)
            }

            override fun onFragmentPopped() {
                val searchFragment = bottomSheetFragment
                    .getFragmentByTag(
                        TKUILocationSearchViewControllerFragment.TAG
                    )
                if (searchFragment?.isVisible == true) {
                    (searchFragment as TKUILocationSearchViewControllerFragment)
                        .updateSuggestionProviderCurrentLocation(false)
                }

                mapFragment.enablePinLocationOnClick = when {
                    bottomSheetFragment.childFragmentManager.backStackEntryCount <= 1 -> true
                    bottomSheetFragment.getFragmentByTag(
                        TKUIRouteFragment.TAG
                    )?.isVisible == true -> true
                    else -> false
                }
            }
        })

        childFragmentManager
            .beginTransaction()
            .replace(R.id.standardBottomSheet, bottomSheetFragment, tag)
            .addToBackStack(tag)
            .commit()

        bottomSheetBehavior = BottomSheetBehavior.from(binding.standardBottomSheet)
        bottomSheetBehavior.isFitToContents = false
        bottomSheetBehavior.isDraggable = true
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
        binding.standardBottomSheet.let { frameLayout ->
            frameLayout.layoutParams = frameLayout.layoutParams.apply {
                height = ViewGroup.LayoutParams.MATCH_PARENT
            }
        }

        binding.standardBottomSheet.visibility = View.GONE

        addBottomSheetCallback()
    }

    private fun addBottomSheetCallback() {
        bottomSheetBehavior.addBottomSheetCallback(object :
            BottomSheetBehavior.BottomSheetCallback() {
            override fun onStateChanged(bottomSheet: View, newState: Int) {

            }

            override fun onSlide(bottomSheet: View, slideOffset: Float) {
                lifecycleScope.launch {
                    bottomSheetOffset.value = (binding.mainLayout.bottom) - bottomSheet.top
                }

                requireContext().deFocusAndHideKeyboard(
                    requireActivity().currentFocus
                        ?: view?.rootView
                )

                if (this@TKUIHomeViewControllerFragment::map.isInitialized) {
                    val currentCenter = map.cameraPosition.target
                    if (bottomSheetBehavior.state == BottomSheetBehavior.STATE_DRAGGING) {
                        setMapPadding(slideOffset)
                        map.moveCamera(CameraUpdateFactory.newLatLng(currentCenter))
                    } else if (bottomSheetBehavior.state == BottomSheetBehavior.STATE_SETTLING) {
                        setMapPadding(slideOffset)
                        map.moveCamera(CameraUpdateFactory.newLatLng(currentCenter))
                        hideKeyboard(requireContext(), bottomSheet)
                    }
                }
            }
        })
    }

    private fun initObservers() {
        eventBus.apply {
            listen(
                ViewControllerEvent.OnLocationSuggestionSelected::class.java
            ).subscribe {
                handleSuggestionAction(it.suggestion)
            }.addTo(autoDisposable)

            listen(
                ViewControllerEvent.OnCloseAction::class.java
            ).subscribe {
                handleCloseAction()
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
                if (bottomSheetFragment
                        .getFragmentByTag(
                            TKUILocationSearchViewControllerFragment.TAG
                        )?.isVisible == true || bottomSheetFragment
                        .getFragmentByTag(
                            TKUIPoiDetailsFragment.TAG
                        )?.isVisible == true
                ) {
                    routeLocation(it.location)
                }
            }.addTo(autoDisposable)

            listen(
                ViewControllerEvent.OnGetRouteTripResults::class.java
            ).subscribe {
                getRouteTrips(it.origin, it.destination)
            }.addTo(autoDisposable)

            listen(
                ViewControllerEvent.OnRouteFromCurrentLocation::class.java
            ).subscribe {
                routeFromCurrentLocation(it.location)
            }.addTo(autoDisposable)

            listen(
                ViewControllerEvent.OnViewTrip::class.java
            ).subscribe {
                loadTrip(it.viewTrip, it.tripGroupList)
            }.addTo(autoDisposable)

            listen(
                ViewControllerEvent.OnBottomSheetFragmentCountUpdate::class.java
            ).subscribe {
                if (it.count > 0) {
                    bottomSheetVisibilityCallback?.invoke(1)
                } else {
                    updateFabMyLocationAnchor(
                        binding.mapFragmentParent.id,
                        Gravity.BOTTOM or Gravity.END
                    )
                    bottomSheetBehavior.peekHeight = 0
                    bottomSheetVisibilityCallback?.invoke(0)
                }
            }.addTo(autoDisposable)

            listen(ViewControllerEvent.OnTripSegmentClicked::class.java)
                .subscribe {
                    loadTripPreview(it.tripSegment, it.tripSegment.segmentId)
                }.addTo(autoDisposable)

            listen(ViewControllerEvent.OnTripSegmentDataSetChange::class.java)
                .subscribe {
                    reloadTripPreview(it.tripSegment, it.trip)
                }.addTo(autoDisposable)

            listen(ViewControllerEvent.OnTripPrimaryActionClick::class.java)
                .subscribe {
                    loadTripPreview(it.tripSegment, it.tripSegment.segmentId)
                }.addTo(autoDisposable)

            listen(ViewControllerEvent.OnViewPoiDetails::class.java)
                .subscribe {
                    val currentBottomSheetFragment =
                        bottomSheetFragment.getFragmentByTag(TKUIRouteFragment.TAG)
                    if (currentBottomSheetFragment is TKUIRouteFragment && currentBottomSheetFragment.isVisible) {
                        loadPoiDetails(
                            it.location,
                            true,
                            currentBottomSheetFragment.getLocationField() == LocationField.ORIGIN
                        )
                    } else {
                        loadPoiDetails(it.location)
                    }
                }.addTo(autoDisposable)

            listen(ViewControllerEvent.OnUpdateBottomSheetState::class.java)
                .subscribe {
                    bottomSheetBehavior.state = it.state
                }.addTo(autoDisposable)
        }

    }

    private fun updateFabMyLocationAnchor(anchorId: Int, anchorGravity: Int) {
        val layoutParams = binding.ivMyLocation.layoutParams as CoordinatorLayout.LayoutParams
        if (layoutParams.anchorId == anchorId) {
            return
        }
        layoutParams.anchorId = anchorId
        layoutParams.anchorGravity = anchorGravity
        binding.ivMyLocation.layoutParams = layoutParams
    }

    private fun handleCloseAction() {

        if (isFromChooseOnMap) {
            isFromChooseOnMap = false
            viewModel.toggleChooseOnMap(true)
        }

        bottomSheetFragment.popActiveFragment()

        if (childFragmentManager.findFragmentByTag(TripPreviewHeaderFragment.TAG) != null &&
            binding.topSheet.isVisible
        ) {
            bottomSheetBehavior.expandedOffset = 0
            binding.topSheet.visibility = View.GONE
            childFragmentManager.popBackStack(
                TripPreviewHeaderFragment.TAG,
                FragmentManager.POP_BACK_STACK_INCLUSIVE
            )
        }
    }

    private fun routeFromCurrentLocation(destination: Location?) {
        if (destination == null) {
            return
        }

        checkLocationPermission { granted ->
            if (granted) {
                viewModel.getUserGeoPoint {
                    when (it) {
                        is Success -> getRouteTrips(it.invoke(), destination, false)
                        is Failure -> Timber.d(getString(R.string.could_not_get_location))
                    }
                }
            }
        }
    }

    private fun checkLocationPermission(callback: (Boolean) -> Unit) {
        ExcuseMe.couldYouGive(this)
            .permissionFor(Manifest.permission.ACCESS_FINE_LOCATION) {
                callback.invoke(it.granted.contains(Manifest.permission.ACCESS_FINE_LOCATION))
            }
    }

    private fun getRouteTrips(
        origin: Location,
        destination: Location,
        fromRouteCard: Boolean = true
    ) {
        val tripResultsFragment =
            TKUITripResultsFragment.newInstance(
                origin,
                destination,
                fromRouteCard
            )


        updateBottomSheetFragment(
            tripResultsFragment, TKUITripResultsFragment.TAG,
            BottomSheetBehavior.STATE_EXPANDED
        )
    }

    private fun handleSuggestionAction(action: Any) {
        if (action is FixedSuggestions) {
            viewModel.handleFixedSuggestionAction(action)
        }
    }

    private fun handleCitySelected(location: Location) {
        bottomSheetFragment.popActiveFragment()
        mapFragment.moveToLatLng(
            LatLng(location.lat, location.lon)
        )
        if (bottomSheetBehavior.state != BottomSheetBehavior.STATE_HALF_EXPANDED) {
            bottomSheetBehavior.state = BottomSheetBehavior.STATE_HALF_EXPANDED
        }
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

    private fun loadTrip(trip: ViewTrip, tripGroupList: List<TripGroup>) {
        val fragment = TKUITripDetailsViewControllerFragment.newInstance(
            trip,
            tripGroupList
        )
        setupTripFragment(fragment, null)
    }

    private fun setupTripFragment(
        fragment: TKUITripDetailsViewControllerFragment,
        segment: TripSegment?
    ) {
        fragment.tripKitMapFragment = mapFragment
        fragment.initialTripSegment = segment

        updateBottomSheetFragment(
            fragment,
            TKUITripDetailsViewControllerFragment.TAG
        )

        if (bottomSheetBehavior.state == BottomSheetBehavior.STATE_HALF_EXPANDED) {
            fragment.initializationRelay.observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    fragment.settled()
                }.addTo(autoDisposable)
        }

        // We only want to set the map contributor after the fragment has settled into the half-expanded state, otherwise it messes
        // up the camera animation zooming in on the first selected trip in the pager.
        val bottomSheetCallback = object : BottomSheetBehavior.BottomSheetCallback() {
            override fun onStateChanged(bottomSheet: View, newState: Int) {
                if (newState == BottomSheetBehavior.STATE_HALF_EXPANDED) {
                    fragment.settled()
                }
                bottomSheetBehavior.removeBottomSheetCallback(this)
            }

            override fun onSlide(bottomSheet: View, slideOffset: Float) {}
        }
        bottomSheetBehavior.addBottomSheetCallback(bottomSheetCallback)
    }

    private fun loadTripPreview(
        tripSegment: TripSegment,
        segmentId: Long,
        fromTripAction: Boolean = false
    ) {
        val pageIndexStream = PublishSubject.create<Pair<Long, String>>()
        val paymentDataStream = PublishSubject.create<PaymentData>()
        val ticketActionStream = PublishSubject.create<String>()

        tripSegmentOnPreview = tripSegment

        val headerFragment =
            TripPreviewHeaderFragment.newInstance(
                pageIndexStream,
                tripSegment.trip?.hideExactTimes == true ||
                    tripSegment.trip?.segmentList?.any { it.isHideExactTimes } ?: false
            )
        replaceFragment(headerFragment, TripPreviewHeaderFragment.TAG, R.id.topSheet, false)

        val fragment = TKUITripPreviewFragment.newInstance(
            tripSegment.trip?.group!!.uuid(),
            tripSegment.trip!!.uuid, segmentId,
            initTripPreviewPagerFragmentListener(tripSegment),
            fromTripAction,
            pageIndexStream,
            paymentDataStream,
            ticketActionStream
        ) {
            headerFragment.setHeaderItems(it)
            binding.topSheet.visibility = View.VISIBLE
            mapFragment.setSettingsButtonVisibility(false)
        }

        fragment.setOnCloseButtonListener { handleCloseAction() }
        updateBottomSheetFragment(fragment, TKUITripPreviewFragment.TAG)

        binding.topSheet.addOnLayoutChangeListener { view, _, _, _, _, _, _, _, _ ->
            if (view.isVisible) {
                bottomSheetBehavior.expandedOffset = binding.topSheet.height + 100
            }
        }
    }

    //TODO breakdown the function and move all the logic to the viewmodel
    private fun initTripPreviewPagerFragmentListener(tripSegment: TripSegment): TripPreviewPagerListener {
        return object : TripPreviewPagerListener {
            override fun onServiceActionButtonClicked(
                _tripSegment: TripSegment?,
                action: String?
            ) {
            }

            override fun onTimetableEntryClicked(
                tripSegment: TripSegment?,
                scope: CoroutineScope,
                entry: TimetableEntry
            ) {
                // Replaced by [viewTimetableEntry] and [showUpdateLoader]
            }

            override fun viewTimetableEntry(
                tripGroup: TripGroup,
                trip: Trip,
                segment: TripSegment
            ) {
                eventBus.publish(
                    ViewControllerEvent.OnTripSegmentDataSetChange(
                        trip,
                        segment
                    )
                )
                tripGroupOnPreview = tripGroup
                reloadTrip(tripGroup)
            }

            override fun reportPlannedTrip(trip: Trip?, tripGroups: List<TripGroup>) {
                trip?.let {
                    eventBus.publish(ViewControllerEvent.OnReportPlannedTrip(tripGroups, it))
                }
            }

            override fun onBottomSheetResize(): MutableLiveData<Int> {
                return bottomSheetOffset
            }

            override fun onRestartHomePage() {}

            override fun onExternalActionButtonClicked(action: String?) {}

            override fun onToggleBottomSheetDrag(isDraggable: Boolean) {
                bottomSheetBehavior.isDraggable = isDraggable
                if (!isDraggable) {
                    bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
                }
            }

            override fun getCurrentPagerItemType(): Int {
                val fragment = childFragmentManager.findFragmentByTag(TKUITripPreviewFragment.TAG)
                if (fragment?.isVisible == true) {
                    return (fragment as TKUITripPreviewFragment).getCurrentPagerItemType()
                }
                return -1
            }

            override fun getLatestTrip(): Trip? {
                val fragment = childFragmentManager.findFragmentByTag(TKUITripPreviewFragment.TAG)
                if (fragment?.isVisible == true) {
                    return (fragment as TKUITripPreviewFragment).latestTrip
                }
                return null
            }

            override fun showUpdateLoader(show: Boolean, message: String) {
                if (updateModalDialog != null) {
                    updateModalDialog?.dismissAllowingStateLoss()
                    updateModalDialog = null
                }
                if (show) {
                    updateModalDialog = UpdateModalDialog.newInstance(message)
                    updateModalDialog?.show(childFragmentManager, null)
                }
            }
        }
    }

    private fun reloadTrip(tripGroup: TripGroup) {
        val fragment =
            bottomSheetFragment.getFragmentByTag(TKUITripDetailsViewControllerFragment.TAG)
        if (fragment != null && fragment is TKUITripDetailsViewControllerFragment) {
            val list = ArrayList<TripGroup>()
            list.add(tripGroup)
            fragment.updateTripGroupResult(list)
            fragment.updatePagerFragmentTripGroup(tripGroup)
        }
    }

    private fun reloadTripPreview(tripSegment: TripSegment, trip: Trip) {
        tripSegmentOnPreview = tripSegment
        val fragment = bottomSheetFragment.getFragmentByTag(TKUITripPreviewFragment.TAG)
        if (fragment?.isVisible == true) {
            val tripPreviewPagerFragment = fragment as TKUITripPreviewFragment
            tripPreviewPagerFragment.setTripSegment(tripSegment, trip.segmentList)
            tripPreviewPagerFragment.updateTripSegment(trip.segmentList)
            tripPreviewPagerFragment.updateListener(initTripPreviewPagerFragmentListener(tripSegment))
        }
    }

    companion object {
        fun load(
            activity: AppCompatActivity,
            containerId: Int,
            defaultLocation: LatLng? = null,
            favoriteSuggestionProvider: TKUIFavoritesSuggestionProvider? = null,
            actionButtonHandlerFactory: TKUIActionButtonHandlerFactory? = null,
            showMyLocationButtonWithoutPermission: Boolean = false,
            bottomSheetVisibilityCallback: ((Int) -> Unit)? = null,
            onBackPressOnEmptyBottomSheetCallback: ((OnBackPressedCallback) -> Unit)? = null,
        ): TKUIHomeViewControllerFragment {

            ControllerDataProvider.favoriteProvider = favoriteSuggestionProvider
            ControllerDataProvider.actionButtonHandlerFactory = actionButtonHandlerFactory

            val fragment =
                newInstance(
                    defaultLocation = defaultLocation,
                    bottomSheetVisibilityCallback = bottomSheetVisibilityCallback,
                    showMyLocationButtonWithoutPermission = showMyLocationButtonWithoutPermission,
                    onBackPressOnEmptyBottomSheetCallback = onBackPressOnEmptyBottomSheetCallback,
                )

            activity.supportFragmentManager
                .beginTransaction()
                .replace(
                    containerId,
                    fragment
                )
                .addToBackStack(null)
                .commit()

            return fragment
        }

        /**
         * Create [TKUIHomeViewControllerFragment] instance
         *
         * @param defaultLocation to set map default location after it loads
         * @param bottomSheetVisibilityCallback Callback to detect if [TKUIHomeViewControllerFragment]
         * bottom sheet is hidden (0) or visible (1)
         * @param showMyLocationButtonWithoutPermission when true, show my location button even if
         * [Manifest.permission.ACCESS_FINE_LOCATION] is not yet granted and permission request will be asked
         * once the button is clicked.Will hide the button if false.
         * @param onBackPressOnEmptyBottomSheetCallback callback to be triggered when user clicked
         * back button and [TKUIHomeViewControllerFragment]'s [TKUIHomeBottomSheetFragment] is empty so the action
         * can be handled on the parent activity/fragment. Including [OnBackPressedCallback] in the callback
         * for the parent to remove([OnBackPressedCallback.remove] or not (i.e. you want to close parent activity
         * on backpress when [TKUIHomeBottomSheetFragment] is empty)
         * @return [TKUIHomeViewControllerFragment] new instance
         */
        fun newInstance(
            defaultLocation: LatLng? = null,
            bottomSheetVisibilityCallback: ((Int) -> Unit)? = null,
            showMyLocationButtonWithoutPermission: Boolean = false,
            onBackPressOnEmptyBottomSheetCallback: ((OnBackPressedCallback) -> Unit)? = null,
        ) = TKUIHomeViewControllerFragment().apply {
            this.defaultLocation = defaultLocation
            this.bottomSheetVisibilityCallback = bottomSheetVisibilityCallback
            this.showMyLocationButtonWithoutPermission = showMyLocationButtonWithoutPermission
            this.onBackPressOnEmptyBottomSheetCallback = onBackPressOnEmptyBottomSheetCallback
        }
    }
}