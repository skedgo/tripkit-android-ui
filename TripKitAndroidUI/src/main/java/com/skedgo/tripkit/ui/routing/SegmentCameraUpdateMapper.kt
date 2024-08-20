package com.skedgo.tripkit.ui.routing

import com.gojuno.koptional.None
import com.gojuno.koptional.Optional
import com.gojuno.koptional.Some
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
import javax.inject.Inject

open class SegmentCameraUpdateMapper @Inject internal constructor() {
    open fun toCameraUpdate(segmentCameraUpdate: SegmentCameraUpdate): Optional<CameraUpdate> =
        when (segmentCameraUpdate) {
            is SegmentCameraUpdate.HasTwoLocations ->
                if (segmentCameraUpdate.getDistanceInMeters() > OneKilometers) {
                    Some(
                        newLatLngZoom(
                            segmentCameraUpdate.start.toLatLng(),
                            ZoomToCoverFirstOneKilometers
                        )
                    )
                } else {
                    Some(
                        newLatLngBounds(
                            LatLngBounds.builder()
                                .include(segmentCameraUpdate.start.toLatLng())
                                .include(segmentCameraUpdate.end.toLatLng())
                                .build(),
                            CameraUpdatePadding
                        )
                    )
                }
            is SegmentCameraUpdate.HasOneLocation -> Some(
                newLatLngZoom(
                    segmentCameraUpdate.location.toLatLng(),
                    ZoomOnSingleLocation
                )
            )
            is SegmentCameraUpdate.HasEmptyLocations -> None
        }

    private fun SegmentCameraUpdate.HasTwoLocations.getDistanceInMeters(): Double =
        SphericalUtil.computeDistanceBetween(
            start.toLatLng(),
            end.toLatLng()
        )
}
