package com.skedgo.tripkit.ui.map.home

import android.util.Pair
import com.google.android.gms.maps.model.LatLngBounds
import com.skedgo.tripkit.common.model.region.Region
import com.skedgo.tripkit.location.GeoPoint
import com.skedgo.tripkit.ui.map.home.ZoomLevel.Companion.ZOOM_START_VALUE_FOR_LOCAL
import com.skedgo.tripkit.ui.map.home.ZoomLevel.Companion.ZOOM_START_VALUE_TO_SHOW_REGIONAL
import com.skedgo.tripkit.ui.map.home.ZoomLevel.Companion.ZOOM_VALUE_TO_SHOW_CITIES

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

    /**
     * Upper bound on how many regional grid cells we are willing to request for a single
     * viewport. At very far (near-city) zoom the visible span covers a huge number of
     * [CELLS_PER_DEGREE] cells; rather than send an enormous /satapp/locations.json body
     * (the #25753 concern), we fall back to the single centre cell beyond this limit.
     * The cap is generous enough to cover normal regional viewing (roughly zoom >= 9.5).
     */
    const val MAX_REGIONAL_CELLS: Int = 2500

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
        return when {
            zoom <= ZOOM_VALUE_TO_SHOW_CITIES -> {
                // City level - load regional stops for cities
                getCellIdsForRegionalLevel(geoPoint)
            }
            zoom > ZOOM_START_VALUE_TO_SHOW_REGIONAL && zoom <= ZOOM_START_VALUE_FOR_LOCAL -> {
                // Regional level - load regional (parent) stops across the WHOLE visible
                // viewport, not just the centre cell. Requesting only the centre cell made
                // a single marker appear in the middle of the screen when zoomed out.
                getCellIdsForRegionalLevel(geoPoint, span)
            }
            else -> {
                // Local level (> 15.0f) - load local stops + regional for cities
                val localCellIds = getCellIdsForLocalLevel(geoPoint, span)
                localCellIds.addAll(getCellIdsForRegionalLevel(geoPoint))
                localCellIds
            }
        }
    }

    /**
     * @return A list containing a single numeric regional cell id (lat#lng) for the
     * centre point. Used as a cheap fallback and for the parent-stop request while zoomed in.
     */
    fun getCellIdsForRegionalLevel(geoPoint: GeoPoint): ArrayList<String> {
        val ids = ArrayList<String>()
        val latCell = (geoPoint.latitude * CELLS_PER_DEGREE).toInt()
        val lngCell = (geoPoint.longitude * CELLS_PER_DEGREE).toInt()
        ids.add("$latCell#$lngCell")
        return ids
    }

    /**
     * @return Numeric regional cell ids (lat#lng) covering the visible [span].
     *
     * Regional and local levels share the same [CELLS_PER_DEGREE] grid; the only
     * difference at fetch time is the API level flag (parent vs non-parent stops).
     * When the viewport is so far out that the grid would exceed [MAX_REGIONAL_CELLS],
     * we fall back to the single centre cell to keep the request body small (#25753).
     */
    fun getCellIdsForRegionalLevel(
        geoPoint: GeoPoint,
        span: LatLngBounds
    ): ArrayList<String> {
        val cells = getCellIdsForLocalLevel(geoPoint, span)
        return if (cells.size > MAX_REGIONAL_CELLS) {
            getCellIdsForRegionalLevel(geoPoint)
        } else {
            cells
        }
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
        val minLat = ((lat - (latSpan / 2)) * CELLS_PER_DEGREE - 1).toInt()
        val minLng = ((lon - (lonSpan / 2)) * CELLS_PER_DEGREE - 1).toInt()
        val maxLat = ((lat + (latSpan / 2)) * CELLS_PER_DEGREE).toInt()
        val maxLng = ((lon + (lonSpan / 2)) * CELLS_PER_DEGREE).toInt()

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
        return ArrayList(cellIds)
    }
}
