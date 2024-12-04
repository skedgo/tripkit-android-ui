package com.skedgo.tripkit.ui.map.adapter

import android.view.View
import com.google.android.gms.maps.GoogleMap.InfoWindowAdapter
import com.google.android.gms.maps.model.Marker

open class SimpleInfoWindowAdapter : InfoWindowAdapter {
    override fun getInfoWindow(marker: Marker): View? {
        // Fall back to default implementation.
        return null
    }

    override fun getInfoContents(marker: Marker): View? {
        return null
    }
}