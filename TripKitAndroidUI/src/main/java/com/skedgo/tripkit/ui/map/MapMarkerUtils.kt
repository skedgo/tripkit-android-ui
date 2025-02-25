package com.skedgo.tripkit.ui.map

import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.Bitmap.Config.ARGB_8888
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Paint.Cap.ROUND
import android.graphics.Paint.Style.FILL_AND_STROKE
import androidx.annotation.DimenRes
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.skedgo.tripkit.common.model.region.Region.City

object MapMarkerUtils {
    private val circlePaint = Paint().apply {
        isAntiAlias = true
        isFilterBitmap = true
        style = Paint.Style.FILL_AND_STROKE
        strokeCap = Paint.Cap.ROUND
    }

    fun createStopMarkerIcon(
        diameter: Int,
        strokeColor: Int,
        fillColor: Int,
        showOutline: Boolean
    ): Bitmap {
        var adjustedDiameter = diameter
        val icon = Bitmap.createBitmap(adjustedDiameter, adjustedDiameter, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(icon)
        val circleCenter = adjustedDiameter / 2

        if (showOutline) {
            circlePaint.color = Color.BLACK
            canvas.drawCircle(circleCenter.toFloat(), circleCenter.toFloat(), (adjustedDiameter / 2).toFloat(), circlePaint)
            adjustedDiameter = (adjustedDiameter * 0.9).toInt()
        }

        circlePaint.color = strokeColor
        canvas.drawCircle(circleCenter.toFloat(), circleCenter.toFloat(), (adjustedDiameter / 2).toFloat(), circlePaint)

        val innerDiameter = (adjustedDiameter * 0.7).toInt()
        circlePaint.color = fillColor
        canvas.drawCircle(circleCenter.toFloat(), circleCenter.toFloat(), (innerDiameter / 2).toFloat(), circlePaint)

        return icon
    }

    /**
     * This replaces the old solution that used a transparent bitmap contained in the drawable/ folder.
     * The advantage is that this creates a bitmap that looks more consistent over various DPIs.
     */
    fun createTransparentSquaredIcon(res: Resources, @DimenRes sizeResId: Int): BitmapDescriptor {
        val size = res.getDimensionPixelSize(sizeResId)
        return BitmapDescriptorFactory.fromBitmap(
            Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        )
    }

    fun createCityMarker(city: City, cityIcon: BitmapDescriptor?): MarkerOptions? {
        return cityIcon?.let {
            MarkerOptions()
                .title(city.name)
                .position(LatLng(city.lat, city.lon))
                .icon(it)
                .draggable(false)
        }
    }
}
