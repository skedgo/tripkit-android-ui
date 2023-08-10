package com.skedgo.tripkit.ui.controller.locationsearchcontroller

import android.os.Bundle
import android.view.View
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.skedgo.tripkit.common.model.Location
import com.skedgo.tripkit.ui.R
import com.skedgo.tripkit.ui.controller.ViewControllerEvent
import com.skedgo.tripkit.ui.controller.ViewControllerEventBus
import com.skedgo.tripkit.ui.core.BaseFragment
import com.skedgo.tripkit.ui.databinding.FragmentTkuiLocationSearchViewControllerBinding
import com.skedgo.tripkit.ui.search.FixedSuggestionsProvider
import com.skedgo.tripkit.ui.search.LegacyLocationSearchIconProvider
import com.skedgo.tripkit.ui.search.LocationSearchFragment

class TKUILocationSearchViewControllerFragment : BaseFragment<FragmentTkuiLocationSearchViewControllerBinding>() {

    lateinit var fixedSuggestionsProvider: FixedSuggestionsProvider

    private var mapBounds: LatLngBounds? = null
    private var nearLatLng: LatLng? = null
    private var eventBus: ViewControllerEventBus? = null

    override val layoutRes: Int
        get() = R.layout.fragment_tkui_location_search_view_controller

    override val observeAccessibility: Boolean = false

    override fun getDefaultViewForAccessibility(): View? = null

    override fun onCreated(savedInstance: Bundle?) {
        initSearchFragment()
    }

    private fun initSearchFragment() {
        val locationSearchCardFragment = LocationSearchFragment.Builder()
            .withBounds(mapBounds)
            .near(nearLatLng)
            .withHint(getString(R.string.where_do_you_want_to_go_question))
            .allowDropPin()
            .withLocationSearchProvider(TKUIFavoritesSuggestionProvider())
            .showBackButton(false)
            .withLocationSearchIconProvider(TKUILocationSearchIconProvider())
            .withFixedSuggestionsProvider(fixedSuggestionsProvider)
            .build().apply {
                setOnLocationSelectedListener { location ->
                    eventBus?.publish(ViewControllerEvent.OnLocationSelected(location))
                }
                setOnFixedSuggestionSelectedListener {
                    eventBus?.publish(ViewControllerEvent.OnLocationSuggestionSelected(it))
                    /*
                    if (it is TripGoFixedSuggestionsProvider.FixedSuggestions) {
                        when (it) {
                            TripGoFixedSuggestionsProvider.FixedSuggestions.CHOOSE_ON_MAP -> {
                                eventBus.publish(
                                    TripGoEvent.SearchFragmentSelected(
                                        ClickType.CHOOSE_ON_MAP,
                                        null
                                    )
                                )
                            }
                            TripGoFixedSuggestionsProvider.FixedSuggestions.HOME -> {
                                viewModel.getLocation(Location.TYPE_HOME)
                            }
                            TripGoFixedSuggestionsProvider.FixedSuggestions.WORK -> {
                                viewModel.getLocation(Location.TYPE_WORK)
                            }
                        }
                    }
                    */
                }
                setOnCitySelectedListener {
                    eventBus?.publish(ViewControllerEvent.OnCitySelected(it))
                    /*
                    eventBus.publish(
                        TripGoEvent.ZoomToLatLng(
                            com.skedgo.geocoding.LatLng(
                                it.lat,
                                it.lon
                            )
                        )
                    )
                    */
                }
                setOnAttachFragmentListener(object :
                    LocationSearchFragment.OnAttachFragmentListener {
                    override fun onAttachFragment() {
                        //eventBus.publish(TripGoEvent.AttachBottomSheetCallback(true))
                    }
                })
                setOnInfoSelectedListener(object : LocationSearchFragment.OnInfoSelectedListener {
                    override fun onInfoSelectedListener(location: Location) {
                        //eventBus.publish(TripGoEvent.LoadPoiDetails(location))
                    }
                })
            }
        childFragmentManager
            .beginTransaction()
            .replace(R.id.content, locationSearchCardFragment, tag)
            .addToBackStack(tag)
            .commit()
    }

    companion object {

        const val TAG = "TKUILocationSearchViewControllerFragment"

        fun newInstance(
            bounds: LatLngBounds,
            near: LatLng,
            suggestionsProvider: FixedSuggestionsProvider,
            bus: ViewControllerEventBus? = null
        ): TKUILocationSearchViewControllerFragment =
            TKUILocationSearchViewControllerFragment().apply {
                mapBounds = bounds
                nearLatLng = near
                fixedSuggestionsProvider = suggestionsProvider
                eventBus = bus
            }
    }
}