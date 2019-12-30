package com.skedgo.tripkit.ui.map
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.skedgo.tripkit.ui.map.home.ZoomLevel

import javax.inject.Inject

class MapCameraController @Inject constructor() {
  fun moveTo(map: GoogleMap, marker: Marker) {
    val zoomLevel = computeZoomLevel(map.cameraPosition.zoom)
    map.moveCamera(CameraUpdateFactory.newLatLngZoom(marker.position, zoomLevel))
    marker.showInfoWindow()
  }

  fun moveToLatLng(map: GoogleMap, latLng: LatLng) {
    val zoomLevel = computeZoomLevel(map.cameraPosition.zoom)
    map.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, zoomLevel))
  }

  private fun computeZoomLevel(cameraZoom: Float): Float = when {
    cameraZoom > ZoomLevel.INNER.level -> cameraZoom
    else -> ZoomLevel.INNER.level.toFloat()
  }
}
