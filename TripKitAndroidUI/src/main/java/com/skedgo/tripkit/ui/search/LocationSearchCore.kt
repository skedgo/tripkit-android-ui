package com.skedgo.tripkit.ui.search

import android.os.Bundle
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.skedgo.tripkit.common.model.location.Location

/**
 * Shared location-search fragment construction/wiring used by both:
 * - TripGo/WL wrappers
 * - TKUI controller wrappers
 */
data class LocationSearchCoreConfig(
    val bounds: LatLngBounds? = null,
    val near: LatLng? = null,
    val initialQuery: String? = null,
    val hint: String? = null,
    val canOpenTimetable: Boolean = true,
    val withCurrentLocation: Boolean = false,
    val withDropPin: Boolean = false,
    val showBackButton: Boolean = true,
    val showSearchField: Boolean = true,
    val locationSearchIconProvider: LocationSearchIconProvider? = null,
    val fixedSuggestionsProvider: FixedSuggestionsProvider? = null,
    val searchProvider: LocationSearchProvider? = null
)

data class LocationSearchCoreListeners(
    val onLocationSelected: ((Location) -> Unit)? = null,
    val onFixedSuggestionSelected: ((Any) -> Unit)? = null,
    val onCitySelected: ((Location) -> Unit)? = null,
    val onAttachFragment: (() -> Unit)? = null,
    val onInfoClick: ((Location) -> Unit)? = null,
    val onSuggestionActionClick: ((Location) -> Unit)? = null
)

object LocationSearchCore {
    fun createFragment(
        config: LocationSearchCoreConfig,
        listeners: LocationSearchCoreListeners = LocationSearchCoreListeners()
    ): LocationSearchFragment {
        val args = Bundle().apply {
            putParcelable(KEY_BOUNDS, config.bounds)
            putParcelable(KEY_CENTER, config.near)
            putString(ARG_QUERY_HINT, config.hint)
            putString(ARG_INITIAL_QUERY, config.initialQuery)
            putBoolean(ARG_CAN_OPEN_TIMETABLE, config.canOpenTimetable)
            putBoolean(ARG_WITH_CURRENT_LOCATION, config.withCurrentLocation)
            putBoolean(ARG_WITH_DROP_PIN, config.withDropPin)
            putBoolean(ARG_SHOW_BACK_BUTTON, config.showBackButton)
            putBoolean(ARG_SHOW_SEARCH_FIELD, config.showSearchField)
        }

        return LocationSearchFragment().apply {
            arguments = args
            searchSuggestionProvider = config.searchProvider
            locationSearchIconProvider = config.locationSearchIconProvider
            fixedSuggestionsProvider = config.fixedSuggestionsProvider

            listeners.onLocationSelected?.let { callback ->
                setOnLocationSelectedListener { callback(it) }
            }
            listeners.onFixedSuggestionSelected?.let { callback ->
                setOnFixedSuggestionSelectedListener { callback(it) }
            }
            listeners.onCitySelected?.let { callback ->
                setOnCitySelectedListener { callback(it) }
            }
            listeners.onAttachFragment?.let { callback ->
                setOnAttachFragmentListener(object : LocationSearchFragment.OnAttachFragmentListener {
                    override fun onAttachFragment() = callback()
                })
            }
            if (listeners.onInfoClick != null || listeners.onSuggestionActionClick != null) {
                setOnItemActionClickListener(object : LocationSearchFragment.OnItemActionClickListener {
                    override fun onInfoClick(location: Location) {
                        listeners.onInfoClick?.invoke(location)
                    }

                    override fun onSuggestionActionClick(location: Location) {
                        listeners.onSuggestionActionClick?.invoke(location)
                    }
                })
            }
        }
    }
}
