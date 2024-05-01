package com.skedgo.tripkit.ui.map

import android.content.res.Resources
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.skedgo.tripkit.parkingspots.models.Parking
import com.skedgo.tripkit.ui.R
import io.reactivex.Single

object CreateMarkerForParking {
    fun execute(
        resources: Resources,
        parking: Parking
    ): Single<MarkerOptions> {
        val markerOptions = MarkerOptions()
            .title(parking.name)
            .position(LatLng(parking.location.latitude, parking.location.longitude))
            .draggable(false)

        val iconRes = R.drawable.ic_map_stop_parking
        val icon: BitmapDescriptor
        if (iconRes == 0) {
            icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_YELLOW)
        } else {
            icon = BitmapDescriptorFactory.fromResource(iconRes)
        }
        markerOptions.icon(icon)

        return Single.just(markerOptions)
    }
}
