package com.skedgo.tripkit.ui.controller.homeviewcontroller

import android.app.Activity
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.maps.GoogleMap
import com.skedgo.geocoding.LatLng
import com.skedgo.tripkit.common.model.Location
import com.skedgo.tripkit.ui.R
import com.skedgo.tripkit.ui.core.BaseFragment
import com.skedgo.tripkit.ui.core.addTo
import com.skedgo.tripkit.ui.databinding.FragmentTripkitUiHomeViewControllerBinding
import com.skedgo.tripkit.ui.map.home.TripKitMapFragment
import com.skedgo.tripkit.ui.search.LocationSearchFragment
import io.reactivex.Completable
import io.reactivex.android.schedulers.AndroidSchedulers
import java.util.concurrent.TimeUnit

class TripKitUIHomeViewControllerFragment :
    BaseFragment<FragmentTripkitUiHomeViewControllerBinding>() {

    lateinit var mapFragment: TripKitMapFragment
    lateinit var map: GoogleMap

    var defaultLocation: LatLng? = null

    override val layoutRes: Int
        get() = R.layout.fragment_tripkit_ui_home_view_controller

    override val observeAccessibility: Boolean
        get() = false

    override fun getDefaultViewForAccessibility(): View? = null

    override fun onCreated(savedInstance: Bundle?) {
        initMap()
        initViews()
    }

    private fun initMap() {
        mapFragment = childFragmentManager.findFragmentById(R.id.mapFragment) as TripKitMapFragment
        mapFragment.getMapAsync {
            map = it
            defaultLocation?.let { moveMapToDefaultLocation(it) }
        }
    }

    private fun initViews() {
        binding.testAction.setOnClickListener {
            /*
            if (this::map.isInitialized) {
                val bounds = map.projection.visibleRegion.latLngBounds
                val near = map.cameraPosition.target
                val locationSearchCardFragment = LocationSearchFragment.Builder()
                    .withBounds(bounds)
                    .near(near)
                    .withHint(getString(R.string.where_do_you_want_to_go_question))
                    .allowDropPin()
                    .withLocationSearchProvider(tripGoFavoritesSuggestionProvider)
                    .showBackButton(false)
                    .withLocationSearchIconProvider(TripGoLocationSearchIconProvider())
                    .withFixedSuggestionsProvider(tripGoFixedSuggestionsProvider)
                    .build().apply {
                        setOnLocationSelectedListener { location ->
                            eventBus.publish(
                                TripGoEvent.SearchFragmentSelected(
                                    ClickType.LOCATION,
                                    location
                                )
                            )
                        }
                        setOnFixedSuggestionSelectedListener {
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
                        }
                        setOnCitySelectedListener {
                            eventBus.publish(
                                TripGoEvent.ZoomToLatLng(
                                    com.skedgo.geocoding.LatLng(
                                        it.lat,
                                        it.lon
                                    )
                                )
                            )
                        }
                        setOnAttachFragmentListener(object :
                            LocationSearchFragment.OnAttachFragmentListener {
                            override fun onAttachFragment() {
                                eventBus.publish(TripGoEvent.AttachBottomSheetCallback(true))
                            }
                        })
                        setOnInfoSelectedListener(object : LocationSearchFragment.OnInfoSelectedListener {
                            override fun onInfoSelectedListener(location: Location) {
                                eventBus.publish(TripGoEvent.LoadPoiDetails(location))
                            }
                        })
                    }
                childFragmentManager
                    .beginTransaction()
                    .replace(R.id.standardBottomSheet, fragment, tag)
                    .addToBackStack(tag)
                    .commit()
            }
            */
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
                    newInstance(defaultLocation)
                )
                .addToBackStack(null)
                .commit()
        }

        fun newInstance(defaultLocation: LatLng? = null) =
            TripKitUIHomeViewControllerFragment().apply {
                this.defaultLocation = defaultLocation
            }
    }
}