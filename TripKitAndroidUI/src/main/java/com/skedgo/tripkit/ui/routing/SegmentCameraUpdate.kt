package com.skedgo.tripkit.ui.routing

import com.skedgo.tripkit.common.model.Location

sealed class SegmentCameraUpdate {
    abstract fun tripSegmentId(): Long

    data class HasTwoLocations(
        val id: Long,
        val start: Location,
        val end: Location
    ) : SegmentCameraUpdate() {
        override fun tripSegmentId(): Long = id
    }

    data class HasOneLocation(
        val id: Long,
        val location: Location
    ) : SegmentCameraUpdate() {
        override fun tripSegmentId(): Long = id
    }

    data class HasEmptyLocations(val id: Long) : SegmentCameraUpdate() {
        override fun tripSegmentId(): Long = id
    }
}
