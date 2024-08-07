package com.skedgo.tripkit.ui.map

import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.Canvas
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.skedgo.tripkit.data.database.locations.bikepods.BikePodLocationEntity
import com.skedgo.tripkit.ui.R
import com.skedgo.tripkit.ui.TripGoStyleKit
import io.reactivex.Single

object CreateMarkerForBikePod {
    fun execute(resources: Resources, bikePod: BikePodLocationEntity): Single<MarkerOptions> {
        val iconSize = resources.getDimensionPixelSize(R.dimen.map_icon_size)
        val bitmap = Bitmap.createBitmap(iconSize, iconSize, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val availableBikes = bikePod.bikePod.availableBikes
        val totalSpaces = bikePod.bikePod.totalSpaces
        val fraction = if (availableBikes != null && totalSpaces != null) {
            availableBikes.toFloat() / totalSpaces
        } else {
            1f
        }
        TripGoStyleKit.drawBikeShareMap(canvas, fraction, 0.toFloat(), iconSize.toFloat())
        return MarkerOptions()
            .title(bikePod.bikePod.operator.name)
            .position(LatLng(bikePod.lat, bikePod.lng))
            .draggable(false)
            .icon(BitmapDescriptorFactory.fromBitmap(bitmap))
            .let { Single.just(it) }

    }
}