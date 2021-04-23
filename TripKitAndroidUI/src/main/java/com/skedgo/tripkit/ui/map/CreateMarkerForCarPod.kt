package com.skedgo.tripkit.ui.map

import android.content.res.Resources
import android.graphics.*
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.skedgo.tripkit.locations.CarPod
import com.skedgo.tripkit.ui.R
import com.skedgo.tripkit.ui.TripGoStyleKit
import com.skedgo.tripkit.ui.TripGoStyleKit.ResizingBehavior
import com.squareup.picasso.Picasso
import io.reactivex.Single

object CreateMarkerForCarPod {

    fun execute(resources: Resources, picasso: Picasso, carPod: CarPod): Single<MarkerOptions> {
        val iconSize = resources.getDimensionPixelSize(R.dimen.map_icon_size)
        val bitmap = Bitmap.createBitmap(iconSize, iconSize, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val colorEntity = carPod.modeInfo?.color
        var color = Color.GRAY
        if (colorEntity?.red != null && colorEntity.green != null && colorEntity.blue != null) {
            colorEntity.let {
                color = Color.argb(255, it.red!!, it.green!!, it.blue!!)
            }
        }

        val paint = Paint()
        paint.color = color
        paint.style = Paint.Style.FILL
        val fillCenter = (iconSize * 1.0f / 2)
        canvas.drawCircle(fillCenter, fillCenter, fillCenter, paint)

        paint.color = Color.WHITE
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = resources.getDimensionPixelSize(R.dimen.map_icon_stroke).toFloat()
        val strokeCenter = (iconSize * 0.95f / 2)
        canvas.drawCircle(fillCenter, fillCenter, strokeCenter, paint)

        val contentPadding = resources.getDimensionPixelSize(R.dimen.map_icon_padding).toFloat()
        TripGoStyleKit.drawIconcarshare(canvas, RectF(contentPadding, contentPadding,
                iconSize - contentPadding, iconSize - contentPadding), ResizingBehavior.AspectFit)
        return MarkerOptions()
                .title(carPod.name)
                .position(LatLng(carPod.lat, carPod.lng))
                .draggable(false)
                .snippet(carPod.address)
                .icon(BitmapDescriptorFactory.fromBitmap(bitmap))
                .let { Single.just(it) }
    }
}