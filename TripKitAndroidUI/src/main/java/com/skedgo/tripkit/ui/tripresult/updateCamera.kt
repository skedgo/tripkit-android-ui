package com.skedgo.tripkit.ui.tripresult

import com.google.android.gms.maps.GoogleMap

fun GoogleMap.updateCamera(mapCameraUpdate: MapCameraUpdate) = when (mapCameraUpdate) {
    is MapCameraUpdate.Anim -> animateCamera(mapCameraUpdate.value)
    is MapCameraUpdate.Move -> moveCamera(mapCameraUpdate.value)
}
