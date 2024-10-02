package com.skedgo.tripkit.ui.favorites.waypoints

import com.skedgo.tripkit.routing.TripSegment
import com.skedgo.tripkit.ui.favorites.trips.getModeForWayPoint
import com.skedgo.tripkit.ui.model.TimetableEntry
import timber.log.Timber

data class Waypoint(
    val lat: Double? = 0.0,
    val lng: Double? = 0.0,
    val mode: String?,
    val modes: List<String>? = null,
    val modeTitle: String? = null,
    val time: Long = 0,
    val start: String? = null,
    val end: String? = null,
    val startTime: String? = null,
    val endTime: String? = null,
    val serviceTripId: String? = null,
    val operator: String? = null,
    val region: String? = null,
    val disembarkationRegion: String? = null,
    val vehicleUUID: String? = null,
    val order: Int = 0
) {
    companion object {
        fun parseFromTimetableEntryAndSegment(
            segment: TripSegment,
            entry: TimetableEntry
        ): Waypoint {
            var endTime = entry.endTimeInSecs
            if (endTime <= 0) {
                val toAdd = segment.endTimeInSecs - segment.startTimeInSecs
                endTime = entry.serviceTime + toAdd
            }
            return Waypoint(
                mode = entry.modeInfo?.id,
                modes = entry.modeInfo?.id?.run { listOf(this) },
                start = entry.stopCode ?: segment.startStopCode,
                end = entry.endStopCode ?: segment.endStopCode,
                startTime = entry.serviceTime.toString(),
                endTime = endTime.toString(),
                serviceTripId = entry.serviceTripId,
                operator = entry.operator,
                region = segment.from?.region,
                disembarkationRegion = segment.to?.region
            )
        }

        fun parseFromSegment(segment: TripSegment, order: Int = 0): Waypoint? {
            try {
                val mode = segment.getModeForWayPoint()
                var vehicleUUID: String? = null
                segment.realTimeVehicle?.id?.let {
                    vehicleUUID = it.toString()
                }

                return mode.first?.let {
                    Waypoint(
                        start = segment.from?.coordinateString,
                        end = segment.to?.coordinateString,
                        mode = it,
                        modes = listOf(it),
                        vehicleUUID = vehicleUUID,
                        order = order
                    )
                }
            } catch (e: Exception) {
                Timber.e(e)
            }

            return null
        }
    }
}