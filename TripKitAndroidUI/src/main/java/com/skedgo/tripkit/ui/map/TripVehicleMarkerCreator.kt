package com.skedgo.tripkit.ui.map

import android.content.Context
import android.content.res.Resources
import android.graphics.Color
import android.text.TextUtils
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.skedgo.tripkit.common.util.DateTimeFormats
import com.skedgo.tripkit.common.util.StringUtils.capitalizeFirst
import com.skedgo.tripkit.common.util.StringUtils.firstNonEmpty
import com.skedgo.tripkit.routing.TripSegment
import com.skedgo.tripkit.routing.VehicleMode.BUS
import com.skedgo.tripkit.ui.R
import dagger.Lazy
import javax.inject.Inject

class TripVehicleMarkerCreator @Inject internal constructor(
    private val context: Context,
    private val vehicleMarkerIconCreatorLazy: Lazy<VehicleMarkerIconCreator>
) {
    fun call(resources: Resources, segment: TripSegment): MarkerOptions {
        val vehicle = segment.realTimeVehicle
        val millis = vehicle!!.lastUpdateTime * 1000
        val time = DateTimeFormats.printTime(context, millis, segment.timeZone)
        var title: String? = null
        val snippet: String
        if (segment.mode == BUS) {
            if (TextUtils.isEmpty(segment.serviceNumber)) {
                title = "Your upcoming service" // TODO: i18n
            } else {
                if (segment.mode != null && segment.mode!!.isPublicTransport) {
                    title = capitalizeFirst(segment.mode.toString()) + " " + segment.serviceNumber
                }

                if (TextUtils.isEmpty(title)) {
                    title = "Service " + segment.serviceNumber
                }
            }

            snippet = (if (TextUtils.isEmpty(vehicle.label)
            ) "Real-time"
            else "Vehicle " + vehicle.label) + " location as at " + time
        } else {
            title = firstNonEmpty(
                segment.serviceName,
                vehicle.label,
                "Your upcoming service"
            )
            snippet = ((if (segment.mode == null
            ) "Location"
            else capitalizeFirst(segment.mode.toString()) + " location")
                + " as at " + time)
        }

        val bearing = if (vehicle.location == null
        ) 0
        else vehicle.location.bearing
        val color = if (segment.serviceColor == null || segment.serviceColor!!.color == Color.BLACK
        ) resources.getColor(R.color.v4_color)
        else segment.serviceColor!!.color

        val text = if (TextUtils.isEmpty(segment.serviceNumber)
        ) (if (segment.mode == null) "" else capitalizeFirst(segment.mode.toString()))
        else segment.serviceNumber!!

        val icon = vehicleMarkerIconCreatorLazy.get().call(bearing, color, text)
        return MarkerOptions()
            .icon(BitmapDescriptorFactory.fromBitmap(icon))
            .rotation(bearing.toFloat())
            .flat(true)
            .anchor(0.5f, 0.5f)
            .title(title)
            .snippet(snippet)
            .position(
                LatLng(
                    vehicle.location.lat,
                    vehicle.location.lon
                )
            )
            .draggable(false)
    }
}