package com.skedgo.tripkit.ui.favorites.trips

import com.skedgo.tripkit.routing.TripSegment
import com.skedgo.tripkit.ui.model.TimetableEntry
import timber.log.Timber

data class Waypoint(
    val lat: Double? = 0.0,
    val lng: Double? = 0.0,
    val mode: String?,
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
    val vehicleUUID: String? = null
) {
    companion object {
        fun parseFromTimetableEntryAndSegment(segment: TripSegment, entry: TimetableEntry): Waypoint {
            var endTime = entry.endTimeInSecs
            if (endTime <= 0) {
                val toAdd = segment.endTimeInSecs - segment.startTimeInSecs
                endTime = entry.serviceTime + toAdd
            }
            return Waypoint(
                mode = entry.modeInfo?.id,
                start = entry.stopCode ?: segment.startStopCode,
                end = entry.endStopCode ?: segment.endStopCode,
                startTime = entry.serviceTime.toString(),
                endTime = endTime.toString(),
                serviceTripId = entry.serviceTripId,
                operator = entry.operator,
                region = segment.from.region,
                disembarkationRegion = segment.to.region
            )
        }

        fun parseFromSegment(segment: TripSegment): Waypoint? {
            try {
                val mode = segment.getModeForWayPoint()
                var vehicleUUID: String? = null
                segment.realTimeVehicle?.id?.let {
                    vehicleUUID = it.toString()
                }

                return mode.first?.let {
                    Waypoint(
                        start = segment.from.coordinateString,
                        end = segment.to.coordinateString,
                        mode = it,
                        vehicleUUID = vehicleUUID
                    )
                }
            } catch (e: Exception) {
                Timber.e(e)
            }

            return null
        }
    }
}