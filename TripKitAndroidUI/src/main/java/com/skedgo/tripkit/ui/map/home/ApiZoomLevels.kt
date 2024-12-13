package com.skedgo.tripkit.ui.map.home

import com.skedgo.tripkit.ui.map.home.ZoomLevel.INNER
import com.skedgo.tripkit.ui.map.home.ZoomLevel.OUTER


/**
 * Zoom level that is compatible with the locations.json API
 */
object ApiZoomLevels {
    /**
     * Only non-parent stops (e.g, bus) are returned at this level.
     */
    const val LOCAL: Int = 50

    /**
     * Only parent stops (e.g, train) are returned at this level.
     */
    const val REGION: Int = 1
    const val UNKNOWN: Int = 0

    /**
     * Converts zoom level defined by Google map into
     * zoom level compatible with the locations.json API.
     *
     * @param zoomLevel Zoom level defined by Google map.
     */
    fun fromMapZoomLevel(zoomLevel: ZoomLevel?): Int {
        if (zoomLevel == INNER) {
            return LOCAL
        }
        return if (zoomLevel == OUTER) {
            REGION
        } else {
            UNKNOWN
        }
    }
}
