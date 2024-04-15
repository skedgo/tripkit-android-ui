package com.skedgo.tripkit.ui.map

import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.skedgo.tripkit.data.database.locations.facility.FacilityLocationEntity
import com.skedgo.tripkit.ui.R
import com.skedgo.tripkit.ui.TripGoStyleKit
import io.reactivex.Single

object CreateMarkerFoFacility {
    fun execute(
        resources: Resources,
        facilityLocation: FacilityLocationEntity
    ): Single<MarkerOptions> {
        val iconSize = resources.getDimensionPixelSize(R.dimen.map_icon_size)
        val bitmap = Bitmap.createBitmap(iconSize, iconSize, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        var color = Color.GRAY

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
            iconSize - contentPadding, iconSize - contentPadding), TripGoStyleKit.ResizingBehavior.AspectFit)
        //Coles Kaleen
        return MarkerOptions()
            .title(facilityLocation.name)
            .position(LatLng(facilityLocation.lat, facilityLocation.lng))
            .draggable(false)
            .icon(MarkerIconManager.getMarkerBitmap(R.drawable.ic_facility, iconSize))
            .let { Single.just(it) }
    }
}