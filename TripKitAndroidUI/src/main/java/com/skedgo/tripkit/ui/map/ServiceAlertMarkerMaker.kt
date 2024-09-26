package com.skedgo.tripkit.ui.map

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.core.content.ContextCompat
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.skedgo.tripkit.common.model.realtimealert.RealtimeAlert
import com.skedgo.tripkit.ui.R
import javax.inject.Inject

class ServiceAlertMarkerMaker @Inject internal constructor(val context: Context) {

    fun make(alert: RealtimeAlert): MarkerOptions {
        val icon = if (RealtimeAlert.SEVERITY_ALERT == alert.severity())
            R.drawable.ic_alert_red_overlay
        else
            R.drawable.ic_alert_yellow_overlay
        val iconDrawable = ContextCompat.getDrawable(context, icon)!!
        iconDrawable.setBounds(0, 0, iconDrawable.intrinsicWidth, iconDrawable.intrinsicHeight)
        val iconBitmap = Bitmap.createBitmap(
            iconDrawable.intrinsicWidth,
            iconDrawable.intrinsicHeight,
            Bitmap.Config.ARGB_8888
        )
        val iconCanvas = Canvas(iconBitmap)
        iconDrawable.draw(iconCanvas)
        val location = requireNotNull(alert.location())
        return MarkerOptions()
            .title(alert.title())
            .position(LatLng(location.lat, location.lon))
            .draggable(false)
            .icon(BitmapDescriptorFactory.fromBitmap(iconBitmap))
            .anchor(0.5f, 0.5f)
    }
}