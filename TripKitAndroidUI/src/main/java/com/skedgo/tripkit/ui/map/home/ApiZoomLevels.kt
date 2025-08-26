package com.skedgo.tripkit.ui.map.home

import com.skedgo.tripkit.ui.map.home.ZoomLevel.INNER
import com.skedgo.tripkit.ui.map.home.ZoomLevel.REGIONAL
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
        return when (zoomLevel) {
            INNER -> LOCAL
            REGIONAL, OUTER -> REGION
            else -> UNKNOWN
        }
    }

    /**
     * Determines if a zoom level should load both region and local levels
     * This is used for the hybrid approach in zoom range 13.0f - 15.1f
     */
    fun shouldLoadBothLevels(zoom: Float): Boolean {
        return zoom >= 13.0f && zoom < 15.2f
    }
}
