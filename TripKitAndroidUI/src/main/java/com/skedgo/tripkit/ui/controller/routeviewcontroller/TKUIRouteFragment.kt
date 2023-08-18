package com.skedgo.tripkit.ui.controller.routeviewcontroller

import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.text.Editable
import android.text.TextWatcher
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.araujo.jordan.excuseme.ExcuseMe
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.skedgo.rxtry.Failure
import com.skedgo.rxtry.Success
import com.skedgo.rxtry.Try
import com.skedgo.rxtry.toTry
import com.skedgo.tripkit.common.model.Location
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
import com.skedgo.tripkit.ui.databinding.FragmentTkuiRouteBinding
import com.skedgo.tripkit.ui.search.FixedSuggestions
import com.skedgo.tripkit.ui.search.LocationSearchFragment
import com.skedgo.tripkit.ui.utils.hideKeyboard
import com.skedgo.tripkit.ui.utils.showKeyboard
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

class TKUIRouteFragment : BaseFragment<FragmentTkuiRouteBinding>() {

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

    // Used to keep from changing the query when we're pre-filling a text field. Otherwise the observer might
    // fire when the initial destinationLocation is set.
    private var ignoreNextTextChange = false

    private var textChangedHandler = object : TextWatcher {
        override fun afterTextChanged(text: Editable?) {
            // Only pay attention if one of the EditText's has focus. When the swap button is pressed, both
            // focuses are cleared so we won't trigger a new query
            if (!ignoreNextTextChange && (binding.tieStartEdit.hasFocus() || binding.tieDestinationEdit.hasFocus())) {
                locationSearchFragment?.setQuery(text.toString(), true)

                if (text.toString().isEmpty()) {
                    setCorrectLocation(null)
                }
            } else {
                ignoreNextTextChange = false
            }

        }

        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

            if (after != count && start != 0) {
                if (binding.tieStartEdit.hasFocus()) {
                    if (viewModel.startLocation?.locationType == Location.TYPE_CURRENT_LOCATION || viewModel.startLocation?.name == "Current Location") {
                        binding.tieStartEdit.apply {
                            setText("")
                            post { requestFocus() }
                        }
                        viewModel.startLocation = null
                    }
                } else {
                    if (viewModel.destinationLocation?.locationType == Location.TYPE_CURRENT_LOCATION || viewModel.destinationLocation?.name == "Current Location") {
                        binding.tieDestinationEdit.apply {
                            setText("")
                            post { requestFocus() }
                        }
                        viewModel.destinationLocation = null
                    }
                }
            }
        }

        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

    }

    private var focusChangeListener = { v: View, hasFocus: Boolean ->
        if (v == binding.tieStartEdit && hasFocus) {
            if (viewModel.startLocation?.locationType != Location.TYPE_CURRENT_LOCATION) {
                locationSearchFragment?.setQuery(binding.tieStartEdit.text.toString(), true)
            } else {
                locationSearchFragment?.setQuery("", true)
            }
            viewModel.focusedField = TKUIRouteViewModel.FocusedField.START
        } else if (v == binding.tieDestinationEdit && hasFocus) {
            if (viewModel.destinationLocation?.locationType != Location.TYPE_CURRENT_LOCATION) {
                locationSearchFragment?.setQuery(binding.tieDestinationEdit.text.toString(), true)
            } else {
                locationSearchFragment?.setQuery("", true)
            }
            viewModel.focusedField = TKUIRouteViewModel.FocusedField.DESTINATION
        }
    }

    private val currentGeoPointAsLocation = lazy {
        userGeoPointRepository.getFirstCurrentGeoPoint()
            .toTry()
            .map<Try<Location>> { tried: Try<GeoPoint> ->
                when (tried) {
                    is Success -> {
                        val l = Location(tried.invoke().latitude, tried.invoke().longitude).also {
                            it.name = resources.getString(R.string.current_location)
                        }
                        Success(l)
                    }

                    is Failure -> Failure<Location>(tried())
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
                //eventBus.publish(TripGoEvent.LoadPoiDetails(location))
            }
        }

    override val layoutRes: Int
        get() = R.layout.fragment_tkui_route

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

        binding.tieStartEdit.setOnFocusChangeListener(null)
        binding.tieDestinationEdit.setOnFocusChangeListener(null)
        binding.tilStartEdit.setEndIconOnClickListener(null)
        binding.tilDestinationEdit.setEndIconOnClickListener(null)
        binding.tieStartEdit.setOnEditorActionListener(null)
        binding.tieDestinationEdit.setOnEditorActionListener(null)
    }

    override fun onCreated(savedInstance: Bundle?) {
        binding.lifecycleOwner = viewLifecycleOwner
        binding.viewModel = viewModel

        initSearchCard()
        initViews()
        initObservers()

        setupLocations()
    }

    override fun onResume() {
        super.onResume()

        initChangeListeners()
    }

    override fun onPause() {
        super.onPause()
        binding.tieStartEdit.removeTextChangedListener(textChangedHandler)
        binding.tieDestinationEdit.removeTextChangedListener(textChangedHandler)
    }

    private fun initChangeListeners() {
        binding.tieStartEdit.setOnFocusChangeListener(focusChangeListener)
        binding.tieDestinationEdit.setOnFocusChangeListener(focusChangeListener)
        binding.tieStartEdit.addTextChangedListener(textChangedHandler)
        binding.tieDestinationEdit.addTextChangedListener(textChangedHandler)

        binding.tilStartEdit.setEndIconOnClickListener {
            ignoreNextTextChange = true
            viewModel.startLocation = null
            toggleShowCurrentLocation()
            binding.tieStartEdit.requestFocus()
            locationSearchFragment?.setQuery("", true)
        }
        binding.tilDestinationEdit.setEndIconOnClickListener {
            ignoreNextTextChange = true
            viewModel.destinationLocation = null
            toggleShowCurrentLocation()
            binding.tieDestinationEdit.requestFocus()
            locationSearchFragment?.setQuery("", true)
        }
    }

    private fun initSearchCard() {
        locationSearchFragment = TKUILocationSearchViewControllerFragment.newInstance(
            bounds, near, suggestionProvider, searchCardListener, false
        )

        locationSearchFragment?.let {
            childFragmentManager.beginTransaction().replace(R.id.content, it).addToBackStack(null)
                .commitAllowingStateLoss()
        }

        viewModel.destinationLocation?.let {
            Handler().post {
                binding.tieStartEdit.requestFocus()
                showKeyboard(activity)
            }
        }
    }

    private fun initViews() {
        binding.tieStartEdit.setOnEditorActionListener { _, i, keyEvent ->
            if (i == EditorInfo.IME_ACTION_DONE) {
                callRouteTrips()
                true
            } else false
        }

        binding.tieDestinationEdit.setOnEditorActionListener { _, i, keyEvent ->
            if (i == EditorInfo.IME_ACTION_DONE) {
                callRouteTrips()
                true
            } else false
        }

        binding.bClose.setOnClickListener {
            eventBus.publish(ViewControllerEvent.OnCloseAction())
        }

        binding.tvRouteLabel.setOnClickListener {
            callRouteTrips()
        }
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
            clearEditFocus()
            val startText = viewModel.start.value
            val destText = viewModel.destination.value
            viewModel.setStart(destText ?: "")
            viewModel.setDestination(startText ?: "")
            if (startText.isNullOrBlank()) {
                binding.tieDestinationEdit.post { binding.tieDestinationEdit.requestFocus() }
            } else {
                binding.tieStartEdit.post { binding.tieStartEdit.requestFocus() }
            }
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
        ignoreNextTextChange = (destination != null)
        viewModel.destinationLocation = destination
        viewModel.startLocation = origin
    }

    private fun clearEditFocus() {
        // Clear the focus to prevent changing the search query just when we swap or a location is selected
        binding.tieDestinationEdit.clearFocus()
        binding.tieStartEdit.clearFocus()
    }

    private suspend fun getCurrentLocation() {
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
            binding.tieStartEdit.hasFocus() -> {
                viewModel.startLocation = location
            }

            binding.tieDestinationEdit.hasFocus() -> {
                viewModel.destinationLocation = location
            }

            binding.tieStartEdit.text.isNullOrBlank() -> {
                viewModel.startLocation = location
            }

            binding.tieDestinationEdit.text.isNullOrBlank() -> {
                viewModel.destinationLocation = location
            }

            else -> {
                viewModel.startLocation = location
            }
        }

        clearEditFocus()
    }

    fun restoreFocusedEditTextAndSetLocation(location: Location?) {
        if (viewModel.focusedField == TKUIRouteViewModel.FocusedField.START) {
            viewModel.startLocation = location
            binding.tieStartEdit.requestFocus()
        } else if (viewModel.focusedField == TKUIRouteViewModel.FocusedField.DESTINATION) {
            viewModel.destinationLocation = location
            binding.tieDestinationEdit.requestFocus()
        }
        callRouteTrips()

    }

    private fun handleFixedSuggestionAction(it: Any) {
        if (it is FixedSuggestions) {
            when (it) {
                FixedSuggestions.CURRENT_LOCATION -> {}
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

    private fun getLocationField(): LocationField = if (binding.tieStartEdit.hasFocus()) {
        LocationField.ORIGIN
    } else if (binding.tieDestinationEdit.hasFocus()) {
        LocationField.DESTINATION
    } else if (binding.tieStartEdit.text?.isNotEmpty() == true && binding.tieDestinationEdit.text.isNullOrEmpty()) {
        LocationField.DESTINATION
    } else {
        LocationField.ORIGIN
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