package com.skedgo.tripkit.ui.base

import com.skedgo.tripkit.routing.ModeInfo
import com.skedgo.tripkit.routing.SegmentType
import com.skedgo.tripkit.routing.ServiceColor
import com.skedgo.tripkit.routing.TripSegment

class TripSegmentMock {

    companion object {

        val tripSegmentScheduled = TripSegment().apply {
            type = SegmentType.SCHEDULED
            modeInfo = createModeInfo()
        }

        val tripSegmentArrival = TripSegment().apply {
            type = SegmentType.ARRIVAL
            modeInfo = createModeInfo()
        }

        val tripSegmentDeparture = TripSegment().apply {
            type = SegmentType.DEPARTURE
            modeInfo = createModeInfo()
        }

        fun getTripSegments(): List<TripSegment> =
            listOf(tripSegmentScheduled, tripSegmentArrival, tripSegmentDeparture)

        fun createModeInfo(): ModeInfo =
            ModeInfo().apply {
                alternativeText = "Taxi"
                localIconName = "taxi"
                remoteIconName = "taxi"
                remoteDarkIconName = "taxi"
                description = "Taxi"
                id = "ps_tax"
                color = ServiceColor(22, 33, 44)
            }

    }

}