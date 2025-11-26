package com.skedgo.tripkit.ui.map.adapter

import android.view.LayoutInflater
import android.view.View
import com.google.android.gms.maps.model.Marker
import com.skedgo.tripkit.ui.R
import com.skedgo.tripkit.ui.map.SimpleCalloutView

class ServiceStopInfoWindowAdapter(private val inflater: LayoutInflater) :
    SimpleInfoWindowAdapter() {

    private var showRightImage = true

    fun setShowRightImage(show: Boolean) {
        showRightImage = show
    }

    override fun getInfoContents(marker: Marker): View {
        val view = SimpleCalloutView.create(inflater)
        view.setTitle(marker.title)
        view.setSnippet(marker.snippet)
        if (showRightImage) {
            view.setRightImage(R.drawable.ic_arrow_forward)
        }
        return view
    }
}