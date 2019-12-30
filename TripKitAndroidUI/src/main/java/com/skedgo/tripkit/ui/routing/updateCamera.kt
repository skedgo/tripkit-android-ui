package com.skedgo.tripkit.ui.routing
import com.google.android.gms.maps.GoogleMap
import com.skedgo.tripkit.ui.tripresult.MapCameraUpdate

fun GoogleMap.updateCamera(mapCameraUpdate: MapCameraUpdate) = when (mapCameraUpdate) {
  is MapCameraUpdate.Anim -> animateCamera(mapCameraUpdate.value)
  is MapCameraUpdate.Move -> moveCamera(mapCameraUpdate.value)
}
