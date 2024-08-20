package com.skedgo.tripkit.ui.map.adapter

import android.view.LayoutInflater
import android.view.View

import com.google.android.gms.maps.model.Marker
import com.skedgo.tripkit.ui.map.SimpleCalloutView

import javax.inject.Inject

class NoActionWindowAdapter @Inject internal constructor(
    private val inflater: LayoutInflater
) : SimpleInfoWindowAdapter() {
    override fun getInfoContents(marker: Marker): View? =
        SimpleCalloutView.create(inflater).apply {
            setTitle(marker.title)
            setSnippet(marker.snippet)
        }
}
