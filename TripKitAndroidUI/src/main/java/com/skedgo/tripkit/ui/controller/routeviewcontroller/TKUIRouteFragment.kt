package com.skedgo.tripkit.ui.controller.routeviewcontroller

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.Toast
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.araujo.jordan.excuseme.ExcuseMe
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.skedgo.rxtry.Failure
import com.skedgo.rxtry.Success
import com.skedgo.rxtry.Try
import com.skedgo.rxtry.toTry
import com.skedgo.tripkit.common.model.location.Location
import com.skedgo.tripkit.location.GeoPoint
import com.skedgo.tripkit.location.UserGeoPointRepository
import com.skedgo.tripkit.ui.R
import com.skedgo.tripkit.ui.TripKitUI
import com.skedgo.tripkit.ui.controller.ControllerDataProvider
import com.skedgo.tripkit.ui.controller.ViewControllerEvent
import com.skedgo.tripkit.ui.controller.ViewControllerEventBus
import com.skedgo.tripkit.ui.controller.homeviewcontroller.TKUIHomeViewFixedSuggestionsProvider
import com.skedgo.tripkit.ui.controller.locationsearchcontroller.TKUILocationSearchViewControllerFragment
import com.skedgo.tripkit.ui.controller.utils.LocationField
import com.skedgo.tripkit.ui.core.BaseFragment
import com.skedgo.tripkit.ui.core.addTo
import com.skedgo.tripkit.ui.databinding.FragmentTkuiRouteComposeBinding
import com.skedgo.tripkit.ui.search.FixedSuggestions
import com.skedgo.tripkit.ui.utils.showConfirmationPopUpDialog
import com.skedgo.tripkit.checkIfLocationProviderIsEnabled
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

class TKUIRouteFragment : BaseFragment<FragmentTkuiRouteComposeBinding>() {

    @Inject
    lateinit var userGeoPointRepository: UserGeoPointRepository

    @Inject
    lateinit var eventBus: ViewControllerEventBus

    private val viewModel: TKUIRouteViewModel by viewModels()

    lateinit var bounds: LatLngBounds
    lateinit var near: LatLng

    var origin: Location? = null
    var destination: Location? = null

    private var locationSearchFragment: TKUILocationSearchViewControllerFragment? = null

    private var suggestionProvider: TKUIHomeViewFixedSuggestionsProvider? =
        ControllerDataProvider.suggestionProvider

    private val currentGeoPointAsLocation = lazy {
        userGeoPointRepository.getFirstCurrentGeoPoint()
            .toTry()
            .map<Try<Location>> { tried: Try<GeoPoint> ->
                when (tried) {
                    is Success -> {
                        val l = Location(
                            tried.invoke().latitude,
                            tried.invoke().longitude
                        ).also {
                            it.name = resources.getString(R.string.current_location)
                        }
                        Success(l)
                    }

                    is Failure -> Failure<Location>(tried())
                    else -> null
                }
            }
            .subscribeOn(Schedulers.io())
    }

    private var searchCardListener =
        object : TKUILocationSearchViewControllerFragment.TKUILocationSearchViewControllerListener {
            override fun onLocationSelected(location: Location) {
                setCorrectLocation(location)
                callRouteTrips()
            }

            override fun onFixedSuggestionSelected(any: Any) {
                handleFixedSuggestionAction(any)
            }

            override fun onCitySelected(location: Location) {}

            override fun onInfoSelected(location: Location) {
                eventBus.publish(ViewControllerEvent.OnViewPoiDetails(location))
            }
        }

    override val layoutRes: Int
        get() = R.layout.fragment_tkui_route_compose

    override val observeAccessibility: Boolean = false

    override fun getDefaultViewForAccessibility(): View? = null

    override fun onAttach(context: Context) {
        TripKitUI.getInstance().controllerComponent().inject(this)
        super.onAttach(context)
    }

    override fun clearInstances() {
        origin = null
        destination = null
        locationSearchFragment = null
        suggestionProvider = null
    }

    override fun onCreated(savedInstance: Bundle?) {
        initSearchCard()
        setupRouteCompose()
        initObservers()
        setupLocations()
    }

    override fun onResume() {
        super.onResume()
        // BaseFragment can reuse binding.root on back stack return.
        // Re-attach a fresh composition for the compose header shell.
        setupRouteCompose()
    }

    private fun initSearchCard() {
        locationSearchFragment = TKUILocationSearchViewControllerFragment.newInstance(
            bounds, near, suggestionProvider, searchCardListener, false
        )

        locationSearchFragment?.updateSuggestionProviderCurrentLocation(true)

        locationSearchFragment?.let {
            childFragmentManager.beginTransaction().replace(R.id.content, it).addToBackStack(null)
                .commitAllowingStateLoss()
        }

    }

    private fun setupRouteCompose() {
        val composeView = binding.routeCompose

        // In a reused-view setup, clear stale composition before setting fresh content.
        composeView.disposeComposition()
        composeView.setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
        composeView.setContent {
            TKUIRouteCardCompose(
                viewModel = viewModel,
                onClose = { eventBus.publish(ViewControllerEvent.OnCloseAction()) },
                onConfirm = { callRouteTrips() },
                onStartChange = { text ->
                    val hasCurrentLocation =
                        viewModel.startLocation?.locationType == Location.TYPE_CURRENT_LOCATION ||
                            viewModel.startLocation?.name == getString(R.string.current_location)
                    if (text.isEmpty() || hasCurrentLocation) {
                        viewModel.startLocation = null
                        toggleShowCurrentLocation()
                    }
                    viewModel.setStart(text)
                    locationSearchFragment?.setQuery(text, true)
                },
                onDestinationChange = { text ->
                    val hasCurrentLocation =
                        viewModel.destinationLocation?.locationType == Location.TYPE_CURRENT_LOCATION ||
                            viewModel.destinationLocation?.name == getString(R.string.current_location)
                    if (text.isEmpty() || hasCurrentLocation) {
                        viewModel.destinationLocation = null
                        toggleShowCurrentLocation()
                    }
                    viewModel.setDestination(text)
                    locationSearchFragment?.setQuery(text, true)
                },
                onStartFocused = {
                    val query = if (viewModel.startLocation?.locationType == Location.TYPE_CURRENT_LOCATION) {
                        ""
                    } else {
                        viewModel.start.value.orEmpty()
                    }
                    locationSearchFragment?.setQuery(query, true)
                },
                onDestinationFocused = {
                    val query = if (viewModel.destinationLocation?.locationType == Location.TYPE_CURRENT_LOCATION) {
                        ""
                    } else {
                        viewModel.destination.value.orEmpty()
                    }
                    locationSearchFragment?.setQuery(query, true)
                },
                onSwap = { viewModel.swap() }
            )
        }
        composeView.requestLayout()
    }

    private fun callRouteTrips() {
        if (viewModel.bothLocationsAreValid()) {
            eventBus.publish(
                ViewControllerEvent.OnGetRouteTripResults(
                    viewModel.startLocation!!,
                    viewModel.destinationLocation!!
                )
            )
        }
    }

    private fun initObservers() {
        viewModel.swap.observeOn(AndroidSchedulers.mainThread()).subscribe {
            val startText = viewModel.start.value
            val destText = viewModel.destination.value
            viewModel.setStart(destText ?: "")
            viewModel.setDestination(startText ?: "")
            viewModel.swapLocations()
        }.addTo(autoDisposable)

        eventBus.apply {
            listen(
                ViewControllerEvent.OnLocationChosen::class.java
            ).subscribe {
                setCorrectLocation(it.location)
                callRouteTrips()
            }.addTo(autoDisposable)
        }
    }

    fun setupData(bounds: LatLngBounds, near: LatLng, dest: Location?, start: Location? = null) {
        this.bounds = bounds
        this.near = near
        this.destination = dest
        this.origin = start

        setupLocations()
    }

    private fun setupLocations() {
        viewModel.destinationLocation = destination
        viewModel.startLocation = origin
        toggleShowCurrentLocation()
    }

    private suspend fun getCurrentLocation() {
        // First check if device location is enabled
        if (!requireContext().checkIfLocationProviderIsEnabled()) {
            requireContext().showConfirmationPopUpDialog(
                title = getString(R.string.location_services_required),
                message = getString(R.string.device_location_is_turned_off),
                positiveLabel = getString(R.string.settings),
                positiveCallback = {
                    val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                    startActivity(intent)
                }
            )
            return
        }

        if (ExcuseMe.couldYouGive(this)
                .permissionFor(android.Manifest.permission.ACCESS_FINE_LOCATION)
        ) {
            currentGeoPointAsLocation.value
                .subscribe({
                    when (it) {
                        is Success -> {
                            setCurrentLocation(it.invoke())
                        }

                        is Failure -> {
                            Toast.makeText(
                                requireContext(),
                                R.string.could_not_determine_your_current_location_dot,
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }, { Timber.e(it) }).addTo(autoDisposable)
        } else {
            clearCurrentLocationSelection()
            Toast.makeText(
                requireContext(),
                R.string.could_not_determine_your_current_location_dot,
                Toast.LENGTH_SHORT
            ).show()
        }

    }

    private fun setCurrentLocation(location: Location) {
        if (viewModel.startLocation?.locationType == Location.TYPE_CURRENT_LOCATION) {
            location.locationType = Location.TYPE_CURRENT_LOCATION
            viewModel.startLocation = location
        } else if (viewModel.destinationLocation?.locationType == Location.TYPE_CURRENT_LOCATION) {
            viewModel.destinationLocation = location
        }

        callRouteTrips()
    }

    private fun toggleShowCurrentLocation() {
        suggestionProvider?.showCurrentLocation =
            !(viewModel.startLocation?.locationType == Location.TYPE_CURRENT_LOCATION
                || viewModel.destinationLocation?.locationType == Location.TYPE_CURRENT_LOCATION)
    }

    private fun setCorrectLocation(location: Location?) {
        when {
            viewModel.focusedField == TKUIRouteViewModel.FocusedField.START -> {
                viewModel.startLocation = location
            }

            viewModel.focusedField == TKUIRouteViewModel.FocusedField.DESTINATION -> {
                viewModel.destinationLocation = location
            }

            viewModel.start.value.isNullOrBlank() -> {
                viewModel.startLocation = location
            }

            viewModel.destination.value.isNullOrBlank() -> {
                viewModel.destinationLocation = location
            }

            else -> {
                viewModel.startLocation = location
            }
        }
        toggleShowCurrentLocation()
    }

    fun restoreFocusedEditTextAndSetLocation(location: Location?) {
        if (viewModel.focusedField == TKUIRouteViewModel.FocusedField.START) {
            viewModel.startLocation = location
        } else if (viewModel.focusedField == TKUIRouteViewModel.FocusedField.DESTINATION) {
            viewModel.destinationLocation = location
        }
        callRouteTrips()

    }

    private fun handleFixedSuggestionAction(it: Any) {
        if (it is FixedSuggestions) {
            when (it) {
                FixedSuggestions.CURRENT_LOCATION -> {
                    val fixedLocation = Location().apply {
                        locationType = Location.TYPE_CURRENT_LOCATION
                        name = getString(R.string.current_location)
                        lat = 0.0
                        lon = 0.0
                    }
                    locationSearchFragment?.let { fragment ->
                        if (fragment.isAdded && fragment.isVisible) {
                            fragment.setQuery("") // To reset the list
                        }
                    }
                    setCorrectLocation(fixedLocation)
                    lifecycleScope.launch {
                        getCurrentLocation()
                    }
                }
                FixedSuggestions.CHOOSE_ON_MAP ->
                    eventBus.publish(ViewControllerEvent.OnChooseOnMap(getLocationField()))

                FixedSuggestions.HOME -> {
                    val home = ControllerDataProvider.favoriteProvider?.getHome()
                    if (home != null) {
                        setCorrectLocation(home)
                        callRouteTrips()
                    }
                }

                FixedSuggestions.WORK -> {
                    val work = ControllerDataProvider.favoriteProvider?.getWork()
                    if (work != null) {
                        setCorrectLocation(work)
                        callRouteTrips()
                    }
                }
            }
        }
    }

    private fun clearCurrentLocationSelection() {
        val currentLocationLabel = getString(R.string.current_location)
        if (viewModel.startLocation?.locationType == Location.TYPE_CURRENT_LOCATION ||
            viewModel.startLocation?.name == currentLocationLabel
        ) {
            viewModel.startLocation = null
        }
        if (viewModel.destinationLocation?.locationType == Location.TYPE_CURRENT_LOCATION ||
            viewModel.destinationLocation?.name == currentLocationLabel
        ) {
            viewModel.destinationLocation = null
        }
        toggleShowCurrentLocation()
    }

    fun getLocationField(): LocationField = when (viewModel.focusedField) {
        TKUIRouteViewModel.FocusedField.START -> LocationField.ORIGIN
        TKUIRouteViewModel.FocusedField.DESTINATION -> LocationField.DESTINATION
        else -> if (viewModel.start.value?.isNotEmpty() == true && viewModel.destination.value.isNullOrBlank()) {
            LocationField.DESTINATION
        } else {
            LocationField.ORIGIN
        }
    }

    companion object {

        const val TAG = "TKUIRouteFragment"
        fun newInstance(
            bounds: LatLngBounds,
            near: LatLng,
            origin: Location? = null,
            destination: Location? = null
        ): TKUIRouteFragment =
            TKUIRouteFragment().apply {
                this.bounds = bounds
                this.near = near
                this.origin = origin
                this.destination = destination
            }
    }
}