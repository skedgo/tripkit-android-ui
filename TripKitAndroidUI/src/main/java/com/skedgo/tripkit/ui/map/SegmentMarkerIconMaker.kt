package com.skedgo.tripkit.ui.map

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.util.Pair
import androidx.annotation.DrawableRes
import com.skedgo.tripkit.routing.ModeInfo
import com.skedgo.tripkit.routing.SegmentType
import com.skedgo.tripkit.routing.TripSegment
import com.skedgo.tripkit.routing.VehicleMode
import com.skedgo.tripkit.ui.R
import com.skedgo.tripkit.ui.tripresults.GetTransportIconTintStrategy
import javax.inject.Inject

class SegmentMarkerIconMaker @Inject internal constructor(
    private val context: Context,
    private val timeLabelMaker: TimeLabelMaker,
    private val getTransportIconTintStrategy: GetTransportIconTintStrategy
) {

    @SuppressLint("CheckResult")
    fun make(
        segment: TripSegment,
        remoteSegmentMarkerIcon: BitmapDrawable? = null
    ): Pair<Bitmap, Float> {
        val hasBearing =
            segment.type !== SegmentType.DEPARTURE && segment.type !== SegmentType.ARRIVAL
        val hasBearingVehicleIcon = segment.mode == VehicleMode.WALK
            || segment.mode == VehicleMode.BICYCLE
            || segment.mode == VehicleMode.CAR
            || segment.mode == VehicleMode.MOTORBIKE
        val hasTime = segment.type === SegmentType.ARRIVAL || segment.type === SegmentType.SCHEDULED

        val timezone = segment.timeZone
        val vehicleIcon = remoteSegmentMarkerIcon ?: segment.getLightTransportIcon(context)
        val modeInfo = segment.modeInfo
        if (modeInfo != null) {
            getTransportIconTintStrategy()
                .subscribe { strategy ->
                    strategy.apply(
                        remoteIconIsTemplate = modeInfo.remoteIconIsTemplate,
                        remoteIconIsBranding = modeInfo.remoteIconIsBranding,
                        serviceColor = modeInfo.color,
                        drawable = vehicleIcon
                    )
                }
        }
        return BearingMarkerIconBuilder(context, timeLabelMaker)
            .hasBearing(hasBearing)
            .bearing(segment.direction)
            .vehicleIcon(vehicleIcon)
            .vehicleIconScale(ModeInfo.MAP_LIST_SIZE_RATIO)
            .baseIcon(R.drawable.ic_map_pin_base)
            .pointerIcon(getPointerBitmapResource(segment.type))
            .hasBearingVehicleIcon(hasBearingVehicleIcon)
            .hasTime(hasTime)
            .time(segment.startTimeInSecs * 1000, timezone)
            .build()
    }

    @DrawableRes
    private fun getPointerBitmapResource(segmentType: SegmentType?): Int {
        return when (segmentType) {
            SegmentType.DEPARTURE -> R.drawable.ic_map_pin_departure
            SegmentType.ARRIVAL -> R.drawable.ic_map_pin_arrival
            else -> R.drawable.ic_map_pin_pointer
        }
    }
}
