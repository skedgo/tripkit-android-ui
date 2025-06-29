package com.skedgo.tripkit.ui.map.home

import android.content.Context
import android.graphics.Bitmap
import com.skedgo.tripkit.routing.ModeInfo
import com.skedgo.tripkit.routing.VehicleDrawables
import com.skedgo.tripkit.ui.R
import com.skedgo.tripkit.ui.map.BearingMarkerIconBuilder
import io.reactivex.Observable

fun Context.getFromAndToMarkerBitmap(type: Int): Bitmap {

    val builder = BearingMarkerIconBuilder(this, null)
        .hasBearing(false)
        .vehicleIconScale(ModeInfo.MAP_LIST_SIZE_RATIO)
        .baseIcon(R.drawable.ic_map_pin_base)
        .hasBearingVehicleIcon(false)
        .hasTime(false)

    return if (type == 0) {
        builder.apply {
            VehicleDrawables.createLightDrawable(
                this@getFromAndToMarkerBitmap,
                R.drawable.ic_location_on
            )
                ?.let {
                    vehicleIcon(it)
                }
            pointerIcon(R.drawable.ic_map_pin_departure)
        }.build().first

    } else {
        builder.apply {
            VehicleDrawables.createLightDrawable(
                this@getFromAndToMarkerBitmap,
                R.drawable.ic_location_on
            )
                ?.let {
                    vehicleIcon(it)
                }
            pointerIcon(R.drawable.ic_map_pin_arrival_small)
        }.build().first
    }
}

fun Observable<ViewPort>.distinctViewPortUntilChanged(
    getCellIdsFromViewPort: GetCellIdsFromViewPort
): Observable<ViewPort> {
    return this
        .flatMap { viewPort ->
            getCellIdsFromViewPort.fetch(viewPort)
                .map { viewPort to it }
        }
        .distinctUntilChanged { a, b -> a.second == b.second }
        .map { it.first }
}