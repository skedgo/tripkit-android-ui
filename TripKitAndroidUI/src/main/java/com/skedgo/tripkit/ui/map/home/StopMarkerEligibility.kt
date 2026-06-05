package com.skedgo.tripkit.ui.map.home

import com.skedgo.tripkit.common.model.stop.ScheduledStop
import com.skedgo.tripkit.common.model.stop.StopType

/**
 * Region-only eligibility must be driven by the API fetch level (REGION vs LOCAL),
 * not by transport type. Type/parent signals are only legacy fallbacks for unknown rows.
 */
internal fun isRegionOnlyEligible(stop: ScheduledStop): Boolean {
    return when (stop.apiZoomLevel) {
        ApiZoomLevels.REGION -> true
        ApiZoomLevels.LOCAL -> false
        else -> {
            if (stop.isRegionalStop) return true
            when (stop.type) {
                StopType.TRAIN, StopType.SUBWAY, StopType.FERRY -> true
                else -> false
            }
        }
    }
}

internal fun isStopVisibleForMarkerMode(stop: ScheduledStop, mode: MarkerZoomMode): Boolean {
    return when (mode) {
        MarkerZoomMode.CITY_ONLY -> false
        MarkerZoomMode.REGION_ONLY -> isRegionOnlyEligible(stop)
        MarkerZoomMode.REGION_AND_LOCAL -> true
    }
}
