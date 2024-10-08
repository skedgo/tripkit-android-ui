package com.skedgo.tripkit.ui.tripresult

import com.skedgo.tripkit.common.model.location.Location
import com.skedgo.tripkit.common.model.region.Region
import com.skedgo.tripkit.ui.model.TimetableEntry
import com.skedgo.tripkit.routing.TripSegment
import java.util.*

sealed class WayPointTaskParam(val region: Region) {


    class ForChangingService(
        region: Region,
        val segments: ArrayList<TripSegment>,
        val prototypeSegment: TripSegment,
        val service: TimetableEntry
    ) : WayPointTaskParam(region)


    class ForChangingStop(
        region: Region,
        val segments: ArrayList<TripSegment>,
        val prototypeSegment: TripSegment,
        val waypoint: Location,
        val isGetOn: Boolean
    ) : WayPointTaskParam(region)
}