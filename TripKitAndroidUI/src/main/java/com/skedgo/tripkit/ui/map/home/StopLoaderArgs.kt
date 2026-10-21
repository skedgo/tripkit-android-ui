package com.skedgo.tripkit.ui.map.home

import android.util.Pair
import com.google.android.gms.maps.model.LatLngBounds
import com.skedgo.tripkit.common.model.region.Region
import com.skedgo.tripkit.location.GeoPoint
import kotlin.math.floor

object StopLoaderArgs {
    /**
     * FIXME: Why 75? This was taken from the iOS impl.
     * If we change into something else rather than 75,
     * it might produce cell ids incompatible with locations.json API.
     *
     * @see [iOS impl](https://github.com/skedgo/tripgo-ios/blob/release-v4/Libraries/TripKit/TKCellHelper.m.L11)
     */
    const val CELLS_PER_DEGREE: Int = 75
    const val KEY_CELL_IDS: String = "cellIds"
    const val KEY_VISIBLE_BOUND: String = "visibleBounds"

    fun newArgsForStopsLoader(
        cellIds: List<String>,
        region: Region,
        visibleBounds: LatLngBounds
    ): Pair<List<String>, LatLngBounds> {
        return Pair(getCellIdsForLoading(cellIds, region), visibleBounds)
    }

    /**
     * @return A list of cell ids for regional level or local level
     * based on the zoom level of camera position
     */
    fun getCellIdsByCameraZoom(
        region: Region,
        geoPoint: GeoPoint,
        zoom: Float,
        span: LatLngBounds
    ): ArrayList<String> {
        return when (ZoomLevel.fromLevel(zoom)) {
            ZoomLevel.CITY -> {
                // City level - load regional stops for cities
                getCellIdsForRegionalLevel(region)
            }
            ZoomLevel.REGIONAL -> {
                // Regional level - load regional stops
                getCellIdsForRegionalLevel(region)
            }
            ZoomLevel.LOCAL -> {
                // Local level (>= 13.5f) - load local stops + regional for cities
                val localCellIds = getCellIdsForLocalLevel(geoPoint, span)
                localCellIds.addAll(getCellIdsForRegionalLevel(region))
                localCellIds
            }
        }
    }

    /**
     * @return A list containing region name used as cell id for regional level
     */
    fun getCellIdsForRegionalLevel(region: Region): ArrayList<String> {
        val ids = ArrayList<String>()
        ids.add(region.name.orEmpty())
        return ids
    }

    /**
     * @return A list of cell ids for local level
     */
    fun getCellIdsForLocalLevel(
        geoPoint: GeoPoint,
        span: LatLngBounds
    ): ArrayList<String> {
        val latSpan = span.northeast.latitude - span.southwest.latitude
        val lonSpan = span.northeast.longitude - span.southwest.longitude
        return getCellIdsForLocalLevel(
            geoPoint.latitude,
            geoPoint.longitude,
            latSpan,
            lonSpan
        )
    }

    fun getCellIdsForLocalLevel(
        lat: Double,
        lon: Double,
        latSpan: Double,
        lonSpan: Double
    ): ArrayList<String> {
        // A cell id is the floor of coordinate * CELLS_PER_DEGREE, so the cells covering a
        // viewport are exactly floor(edge) for each of its four edges.
        //
        // This used to truncate with toInt() and subtract 1 from the two lower bounds. Kotlin's
        // toInt() rounds toward zero, which is floor() only for positive values - south of the
        // equator it rounds the wrong way and would drop the southernmost cell, and the "- 1"
        // was compensating for that. But the compensation also applied where truncation was
        // already correct, so every request carried one extra row and one extra column beyond
        // the visible area. For a typical Sydney street-level viewport that meant 18 cells
        // requested where 10 are visible, and locations.json cost grows steeply with cell
        // count: measured 10 cells = ~11s / 308KB versus 18 cells = ~16.5s / 765KB (#25936).
        //
        // floor() is correct in both hemispheres and needs no compensation.
        val minLat = floor((lat - (latSpan / 2)) * CELLS_PER_DEGREE).toInt()
        val minLng = floor((lon - (lonSpan / 2)) * CELLS_PER_DEGREE).toInt()
        val maxLat = floor((lat + (latSpan / 2)) * CELLS_PER_DEGREE).toInt()
        val maxLng = floor((lon + (lonSpan / 2)) * CELLS_PER_DEGREE).toInt()

        val sharp = "#"
        val ids = ArrayList<String>()
        for (latitude in minLat..maxLat) {
            for (lng in minLng..maxLng) {
                ids.add(latitude.toString() + sharp + lng)
            }
        }

        return ids
    }

    /**
     * Defines proper cell ids so that the loader can load up
     * stops that should be visible at corresponding level.
     *
     *
     * If given cell ids are for the regional level, just return themselves.
     * Why? Because we expect that only parent stops are visible at this level.
     * However, if they are for the local level, should plus the cell ids for the regional level.
     * Why? Because we expect both parent stops and non-parent stops are visible at this level.
     */
    fun getCellIdsForLoading(
        cellIds: List<String>,
        region: Region
    ): ArrayList<String> {
        if (!cellIds.contains(region.name)) {
            val result = ArrayList(cellIds)
            result.addAll(getCellIdsForRegionalLevel(region))
            return result
        } else {
            return ArrayList(cellIds)
        }
    }
}
