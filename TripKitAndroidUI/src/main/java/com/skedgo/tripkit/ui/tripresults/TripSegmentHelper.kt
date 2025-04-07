package com.skedgo.tripkit.ui.tripresults

import android.content.Context
import android.content.res.Resources
import com.skedgo.tripkit.common.model.location.Location
import com.skedgo.tripkit.common.model.realtimealert.RealtimeAlert
import com.skedgo.tripkit.common.model.realtimealert.RealtimeAlerts.getDisplayText
import com.skedgo.tripkit.common.util.TransportModeUtils
import com.skedgo.tripkit.common.util.TripSegmentUtils
import com.skedgo.tripkit.routing.ModeInfo
import com.skedgo.tripkit.routing.TripSegment
import javax.inject.Inject

class TripSegmentHelper @Inject constructor() {
    fun getRealTimeAlertDisplayText(alert: RealtimeAlert): String? {
        return getDisplayText(alert)
    }

    fun getIconUrlForModeInfo(resources: Resources, modeInfo: ModeInfo?): String? {
        return TransportModeUtils.getIconUrlForModeInfo(resources, modeInfo)
    }

    fun getTripSegmentAction(context: Context, tripSegment: TripSegment): String? {
        return TripSegmentUtils.getTripSegmentAction(context, tripSegment)
    }

    fun getLocationName(to: Location): String? {
        return TripSegmentUtils.getLocationName(to)
    }
}