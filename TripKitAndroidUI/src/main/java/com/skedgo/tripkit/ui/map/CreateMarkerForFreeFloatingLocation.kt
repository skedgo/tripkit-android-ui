package com.skedgo.tripkit.ui.map

import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.skedgo.tripkit.data.database.locations.freefloating.FreeFloatingLocationEntity
import com.skedgo.tripkit.ui.R
import io.reactivex.Single

object CreateMarkerForFreeFloatingLocation {
    fun execute(
        resources: Resources,
        freeFloatingLocation: FreeFloatingLocationEntity
    ): Single<MarkerOptions> {
        val iconSize = resources.getDimensionPixelSize(R.dimen.free_floating_map_icon_size)
        val bitmap = Bitmap.createBitmap(iconSize, iconSize, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val colorEntity = freeFloatingLocation.modeInfo?.color
        var color = Color.GRAY
        colorEntity?.let {
            color = Color.argb(255, it.red, it.green, it.blue)
        }

        val paint = Paint()
        paint.color = color
        paint.style = Paint.Style.FILL
        val center = (iconSize * 1.0f / 2)
        canvas.drawCircle(center, center, center, paint)

        return MarkerOptions()
            .title(freeFloatingLocation.vehicle.operator.name)
            .position(LatLng(freeFloatingLocation.lat, freeFloatingLocation.lng))
            .draggable(false)
            .icon(BitmapDescriptorFactory.fromBitmap(bitmap))
            .let { Single.just(it) }

    }
}