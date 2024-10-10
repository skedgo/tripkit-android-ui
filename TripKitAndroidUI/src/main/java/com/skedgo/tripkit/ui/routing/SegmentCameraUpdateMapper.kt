package com.skedgo.tripkit.ui.routing

import com.google.android.gms.maps.CameraUpdate
import com.google.android.gms.maps.CameraUpdateFactory.newLatLngBounds
import com.google.android.gms.maps.CameraUpdateFactory.newLatLngZoom
import com.google.android.gms.maps.model.LatLngBounds
import com.google.maps.android.SphericalUtil
import com.skedgo.tripkit.ui.data.location.toLatLng
import com.skedgo.tripkit.ui.tripresult.CameraUpdatePadding
import com.skedgo.tripkit.ui.tripresult.OneKilometers
import com.skedgo.tripkit.ui.tripresult.ZoomOnSingleLocation
import com.skedgo.tripkit.ui.tripresult.ZoomToCoverFirstOneKilometers
import com.skedgo.tripkit.utils.OptionalCompat
import javax.inject.Inject

open class SegmentCameraUpdateMapper @Inject internal constructor() {
    open fun toCameraUpdate(segmentCameraUpdate: SegmentCameraUpdate): OptionalCompat<CameraUpdate> =
        when (segmentCameraUpdate) {
            is SegmentCameraUpdate.HasTwoLocations ->
                if (segmentCameraUpdate.getDistanceInMeters() > OneKilometers) {
                    OptionalCompat.ofNullable(
                        newLatLngZoom(
                            segmentCameraUpdate.start.toLatLng(),
                            ZoomToCoverFirstOneKilometers
                        )
                    )
                } else {
                    OptionalCompat.ofNullable(
                        newLatLngBounds(
                            LatLngBounds.builder()
                                .include(segmentCameraUpdate.start.toLatLng())
                                .include(segmentCameraUpdate.end.toLatLng())
                                .build(),
                            CameraUpdatePadding
                        )
                    )
                }
            is SegmentCameraUpdate.HasOneLocation -> OptionalCompat.ofNullable(
                newLatLngZoom(
                    segmentCameraUpdate.location.toLatLng(),
                    ZoomOnSingleLocation
                )
            )
            is SegmentCameraUpdate.HasEmptyLocations -> OptionalCompat.empty()
        }

    private fun SegmentCameraUpdate.HasTwoLocations.getDistanceInMeters(): Double =
        SphericalUtil.computeDistanceBetween(
            start.toLatLng(),
            end.toLatLng()
        )
}
