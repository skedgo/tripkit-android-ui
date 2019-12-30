package com.skedgo.tripkit.ui.map.adapter

import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import com.google.android.gms.maps.model.Marker
import com.skedgo.tripkit.ui.R
import javax.inject.Inject

class CityInfoWindowAdapter @Inject internal constructor(
    private val inflater: LayoutInflater
) : SimpleInfoWindowAdapter() {
  override fun getInfoContents(marker: Marker): View? {
    val view = inflater.inflate(R.layout.city_callout, null, false) as TextView
    view.text = marker.title
    return view
  }
}
