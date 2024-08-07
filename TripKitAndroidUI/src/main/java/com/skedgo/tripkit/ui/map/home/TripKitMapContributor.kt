package com.skedgo.tripkit.ui.map.home

import android.content.Context
import android.view.View
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.Marker

/**
 * Implementations of this class will be called at certain points in a TripKitMapFragment's lifecycle,
 * allowing the implementation to add items to the map, such as markers or polylines.
 *
 */
interface TripKitMapContributor {
    /**
     * Called from the TripKitMapFragment's `onAttach()`.
     */
    fun initialize()

    /**
     * Called from `onStart()`.
     */
    fun setup()

    /**
     * Called when the map has been initialized and is safe to use.
     */
    fun safeToUseMap(context: Context, map: GoogleMap)

    /**
     * Called when an info window is clicked.
     */
    fun getInfoContents(marker: Marker): View?

    /**
     * Called either from `onStop()` or when the map is switching to another contributor.
     */
    fun cleanup()
}