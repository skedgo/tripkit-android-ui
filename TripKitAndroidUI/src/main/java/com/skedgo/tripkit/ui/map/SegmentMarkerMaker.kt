package com.skedgo.tripkit.ui.map

import android.content.Context
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.skedgo.tripkit.common.util.TripSegmentUtils
import com.skedgo.tripkit.routing.SegmentType
import com.skedgo.tripkit.routing.TripSegment
import com.skedgo.tripkit.ui.R
import javax.inject.Inject

class SegmentMarkerMaker @Inject internal constructor(
    private val context: Context,
    private val iconMaker: SegmentMarkerIconMaker
) {
    fun make(segment: TripSegment): MarkerOptions? {
        val markerPosition = TripSegmentUtils.getFirstNonNullLocation(
            segment.from,
            segment.singleLocation /* This is for Stationary segment */
        )
        return if (markerPosition != null) {
            val markerIcon = iconMaker.make(segment)
            MarkerOptions()
                .title(TripSegmentUtils.getTripSegmentAction(context, segment))
                .snippet(getSnippet(segment))
                .position(LatLng(markerPosition.lat, markerPosition.lon))
                .icon(BitmapDescriptorFactory.fromBitmap(markerIcon.first))
                .draggable(false)
                .anchor(markerIcon.second, 1.0f)
                .infoWindowAnchor(markerIcon.second, 0f)
        } else {
            null
        }
    }

    private fun getSnippet(segment: TripSegment): String? {
        return if (segment.type === SegmentType.SCHEDULED) {
            val locationName = TripSegmentUtils.getLocationName(
                TripSegmentUtils.getFirstNonNullLocation(
                    segment.from,
                    segment.singleLocation
                )
            )
            String.format(context.resources.getString(R.string.from__pattern), locationName)
        } else if (segment.type === SegmentType.UNSCHEDULED) {
            val locationName = TripSegmentUtils.getLocationName(
                TripSegmentUtils.getFirstNonNullLocation(
                    segment.to,
                    segment.singleLocation
                )
            )
            String.format(context.resources.getString(R.string.to__pattern), locationName)
        } else {
            null
        }
    }
}