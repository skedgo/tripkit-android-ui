package com.skedgo.tripkit.ui.tripresult

import com.google.android.gms.maps.CameraUpdate
import com.google.android.gms.maps.GoogleMap

/**
 * A view model for [GoogleMap] to know when it should
 * [GoogleMap.moveCamera] or [GoogleMap.animateCamera]
 */
sealed class MapCameraUpdate {
    class Move(val value: CameraUpdate) : MapCameraUpdate()
    class Anim(val value: CameraUpdate) : MapCameraUpdate()
}
