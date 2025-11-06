package com.skedgo.tripkit.ui.map

import android.graphics.Bitmap
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.skedgo.tripkit.common.model.location.Location
import javax.inject.Inject

class TripLocationMarkerCreator @Inject constructor() {
    fun call(location: Location, customIcon: Bitmap? = null): MarkerOptions {
        var title: String? = location.name
        var snippet: String? = null

        if (title.isNullOrEmpty()) {
            title = location.address
            if (title.isNullOrEmpty()) {
                title = location.coordinateString
            }
        } else {
            snippet = location.address
        }

        val markerPosition = LatLng(location.lat, location.lon)
        val markerOption = MarkerOptions()
            .title(title)
            .snippet(snippet)
            .draggable(false)
            .position(markerPosition)
        customIcon?.let {
            markerOption.icon(BitmapDescriptorFactory.fromBitmap(it))
        }
        return markerOption
    }
}
