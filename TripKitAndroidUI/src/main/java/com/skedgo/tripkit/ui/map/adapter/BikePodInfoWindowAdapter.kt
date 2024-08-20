package com.skedgo.tripkit.ui.map.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import com.google.android.gms.maps.model.Marker
import com.skedgo.tripkit.ui.R
import com.skedgo.tripkit.ui.map.BikePodPOILocation

class BikePodInfoWindowAdapter(private val context: Context) : StopInfoWindowAdapter {
    val view: View by lazy {
        LayoutInflater.from(context).inflate(R.layout.bike_share_info_window, null, false)
    }

    @SuppressLint("InflateParams")
    override fun getInfoWindow(marker: Marker): View {
        val bikePodLocationEntity = (marker.tag as BikePodPOILocation).bikePodEntity
        view.findViewById<TextView>(R.id.operatorName).text =
            bikePodLocationEntity.bikePod.operator.name
        view.findViewById<TextView>(R.id.availableBikes).text =
            bikePodLocationEntity.bikePod.availableBikes.toString()
        view.findViewById<TextView>(R.id.batteryLevel).text =
            (bikePodLocationEntity.bikePod.totalSpaces!! - bikePodLocationEntity.bikePod.availableBikes!!).toString()
        return view
    }

    override fun getInfoContents(marker: Marker): View? {
        return null
    }

    override fun onInfoWindowClosed(marker: Marker) {}

    override fun windowInfoHeightInPixel(marker: Marker): Int = view.height
}