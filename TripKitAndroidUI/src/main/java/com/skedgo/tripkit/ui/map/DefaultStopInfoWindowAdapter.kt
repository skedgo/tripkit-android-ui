package com.skedgo.tripkit.ui.map
import android.view.View
import com.google.android.gms.maps.model.Marker
import com.skedgo.tripkit.ui.map.adapter.StopInfoWindowAdapter
import com.skedgo.tripkit.ui.map.adapter.ViewableInfoWindowAdapter
import javax.inject.Inject

class DefaultStopInfoWindowAdapter @Inject constructor(
    private val viewableInfoWindowAdapter: ViewableInfoWindowAdapter) : StopInfoWindowAdapter {

  override fun getInfoContents(marker: Marker?): View {
    return viewableInfoWindowAdapter.getInfoContents(marker)
  }

  override fun getInfoWindow(marker: Marker?): View? {
    return viewableInfoWindowAdapter.getInfoWindow(marker)
  }

  override fun onInfoWindowClosed(marker: Marker) {
  }

  override fun windowInfoHeightInPixel(marker: Marker): Int {
    return viewableInfoWindowAdapter.windowInfoHeightInPixel(marker)
  }

}