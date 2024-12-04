package com.skedgo.tripkit.ui.map.adapter

import android.view.LayoutInflater
import android.view.View
import com.google.android.gms.maps.model.Marker
import com.skedgo.tripkit.ui.R
import com.skedgo.tripkit.ui.map.SimpleCalloutView
import javax.inject.Inject

class ViewableInfoWindowAdapter @Inject constructor(private val inflater: LayoutInflater) :
    StopInfoWindowAdapter {
    private var view: SimpleCalloutView? = null

    override fun getInfoContents(marker: Marker): View {
        if (view == null) {
            view = SimpleCalloutView.create(inflater)
        }
        assert(view != null)
        view!!.setTitle(marker.title)
        view!!.setSnippet(marker.snippet)
        view!!.setRightImage(R.drawable.ic_arrow_forward)
        return view!!
    }

    override fun windowInfoHeightInPixel(marker: Marker): Int {
        return if (view != null) {
            view!!.height
        } else {
            0
        }
    }

    override fun onInfoWindowClosed(marker: Marker) {
    }

    override fun getInfoWindow(marker: Marker): View? {
        return null
    }
}