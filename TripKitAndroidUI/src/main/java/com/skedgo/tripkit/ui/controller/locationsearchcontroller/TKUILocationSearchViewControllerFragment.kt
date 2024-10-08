package com.skedgo.tripkit.ui.controller.locationsearchcontroller

import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.skedgo.TripKit
import com.skedgo.tripkit.common.model.location.Location
import com.skedgo.tripkit.common.model.stop.ScheduledStop
import com.skedgo.tripkit.ui.R
import com.skedgo.tripkit.ui.TripKitUI
import com.skedgo.tripkit.ui.controller.ControllerDataProvider
import com.skedgo.tripkit.ui.controller.ViewControllerEvent
import com.skedgo.tripkit.ui.controller.ViewControllerEventBus
import com.skedgo.tripkit.ui.controller.homeviewcontroller.TKUIHomeViewFixedSuggestionsProvider
import com.skedgo.tripkit.ui.core.BaseFragment
import com.skedgo.tripkit.ui.databinding.FragmentTkuiLocationSearchViewControllerBinding
import com.skedgo.tripkit.ui.search.FixedSuggestionsProvider
import com.skedgo.tripkit.ui.search.LocationSearchFragment
import javax.inject.Inject

class TKUILocationSearchViewControllerFragment :
    BaseFragment<FragmentTkuiLocationSearchViewControllerBinding>() {

    @Inject
    lateinit var eventBus: ViewControllerEventBus

    private val viewModel: TKUILocationSearchViewControllerViewModel by viewModels()

    private var listener: TKUILocationSearchViewControllerListener? = null
    private var fixedSuggestionsProvider: FixedSuggestionsProvider? = null

    private var mapBounds: LatLngBounds? = null
    private var nearLatLng: LatLng? = null
    private var withHeaders: Boolean = true

    private var locationSearchFragment: LocationSearchFragment? = null

    override val layoutRes: Int
        get() = R.layout.fragment_tkui_location_search_view_controller

    override val observeAccessibility: Boolean = false

    override fun getDefaultViewForAccessibility(): View? = null

    override fun onAttach(context: Context) {
        TripKitUI.getInstance().controllerComponent().inject(this)
        super.onAttach(context)
    }

    override fun clearInstances() {
        listener = null
        fixedSuggestionsProvider = null
        mapBounds = null
        if (locationSearchFragment != null) {
            locationSearchFragment?.unregisterListeners()
            locationSearchFragment = null
        }
    }

    override fun onCreated(savedInstance: Bundle?) {
        binding.lifecycleOwner = viewLifecycleOwner
        binding.viewModel = viewModel
        viewModel.setWithHeaders(withHeaders)
        initViews()
        initSearchFragment()
        updateSuggestionProviderCurrentLocation(false)
        checkConfigs()
    }

    fun updateSuggestionProviderCurrentLocation(show: Boolean) {
        if (fixedSuggestionsProvider is TKUIHomeViewFixedSuggestionsProvider) {
            (fixedSuggestionsProvider as TKUIHomeViewFixedSuggestionsProvider)
                .showCurrentLocation = show
        }
    }

    private fun checkConfigs() {
        val globalConfigs = TripKit.getInstance().configs()
        if (fixedSuggestionsProvider is TKUIHomeViewFixedSuggestionsProvider) {
            (fixedSuggestionsProvider as TKUIHomeViewFixedSuggestionsProvider)
                .hideFavorites = globalConfigs.hideFavorites()
        }
    }

    private fun initViews() {
        binding.bClose.setOnClickListener {
            eventBus.publish(ViewControllerEvent.OnCloseAction())
        }
    }

    private fun initSearchFragment() {
        val locationSearchFragmentBuilder = LocationSearchFragment.Builder()
            .withBounds(mapBounds)
            .near(nearLatLng)
            .withHint(getString(R.string.where_do_you_want_to_go_question))
            .allowDropPin()
            .showBackButton(false)
            .showSearchField(withHeaders)
            .withLocationSearchIconProvider(TKUILocationSearchIconProvider())

        fixedSuggestionsProvider?.let {
            locationSearchFragmentBuilder.withFixedSuggestionsProvider(it)
        }

        ControllerDataProvider.favoriteProvider?.let {
            locationSearchFragmentBuilder.withLocationSearchProvider(it)
        } ?: kotlin.run {
            locationSearchFragmentBuilder.withLocationSearchProvider(TKUIFavoritesSuggestionProvider())
        }

        locationSearchFragment = locationSearchFragmentBuilder
            .build().apply {
                setOnLocationSelectedListener { location ->
                    listener?.onLocationSelected(location)
                }
                setOnFixedSuggestionSelectedListener {
                    listener?.onFixedSuggestionSelected(it)
                }
                setOnCitySelectedListener {
                    listener?.onCitySelected(it)
                }
                setOnAttachFragmentListener(object :
                    LocationSearchFragment.OnAttachFragmentListener {
                    override fun onAttachFragment() {}
                })
                setOnItemActionClickListener(object :
                    LocationSearchFragment.OnItemActionClickListener {
                    override fun onInfoClick(location: Location) {
                        listener?.onInfoSelected(location)
                    }

                    override fun onSuggestionActionClick(location: Location) {
                        // Right now, we only have scenario for Timetable entry so adding this validation
                        // to check if the location is a ScheduledStop
                        if (location is ScheduledStop) {
                            //TODO update to handle timetable click on controller
                        }
                    }
                })
            }

        locationSearchFragment?.let {
            childFragmentManager
                .beginTransaction()
                .replace(R.id.content, it, tag)
                .addToBackStack(tag)
                .commit()
        }
    }

    fun setQuery(query: String, isRouting: Boolean = false) {
        locationSearchFragment?.setQuery(query, isRouting)
    }

    companion object {

        const val TAG = "TKUILocationSearchViewControllerFragment"

        fun newInstance(
            bounds: LatLngBounds,
            near: LatLng,
            suggestionsProvider: FixedSuggestionsProvider?,
            eventListener: TKUILocationSearchViewControllerListener? = null,
            withHeaders: Boolean = true
        ): TKUILocationSearchViewControllerFragment =
            TKUILocationSearchViewControllerFragment().apply {
                this.mapBounds = bounds
                this.nearLatLng = near
                this.fixedSuggestionsProvider = suggestionsProvider
                this.listener = eventListener
                this.withHeaders = withHeaders
            }
    }

    interface TKUILocationSearchViewControllerListener {
        fun onLocationSelected(location: Location)
        fun onFixedSuggestionSelected(any: Any)
        fun onCitySelected(location: Location)
        fun onInfoSelected(location: Location)
    }
}