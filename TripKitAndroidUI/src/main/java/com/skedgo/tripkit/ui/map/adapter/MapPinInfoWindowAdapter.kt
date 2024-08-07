package com.skedgo.tripkit.ui.map.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import com.google.android.gms.maps.model.Marker
import com.skedgo.tripkit.ui.R

class MapPinInfoWindowAdapter(private val context: Context) : StopInfoWindowAdapter {
    val view: View by lazy {
        LayoutInflater.from(context).inflate(R.layout.map_pin_info_window, null, false)
    }

    @SuppressLint("InflateParams")
    override fun getInfoWindow(marker: Marker): View {
        view.findViewById<TextView>(R.id.txtTitle).text = marker.title
        view.findViewById<TextView>(R.id.txtSubtitle).text = marker.title
        return view
    }

    override fun getInfoContents(marker: Marker): View? {
        return null
    }

    override fun onInfoWindowClosed(marker: Marker) {}

    override fun windowInfoHeightInPixel(marker: Marker): Int = view.height
}