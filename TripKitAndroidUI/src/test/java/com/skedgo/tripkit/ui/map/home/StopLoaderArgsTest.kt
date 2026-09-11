package com.skedgo.tripkit.ui.map.home

import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.skedgo.tripkit.common.model.region.Region
import com.skedgo.tripkit.location.GeoPoint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StopLoaderArgsTest {

    @Test
    fun `regional cell ids include region name`() {
        val regionName = "AU_NT_Darwin"
        val region = Region().apply { name = regionName }
        val center = GeoPoint(-12.4634, 130.8456)
        val bounds = LatLngBounds(
            LatLng(-12.50, 130.80),
            LatLng(-12.40, 130.90)
        )

        val regional = StopLoaderArgs.getCellIdsByCameraZoom(
            region = region,
            geoPoint = center,
            zoom = ZoomLevel.ZOOM_START_VALUE_TO_SHOW_REGIONAL + 0.5f,
            span = bounds
        )

        assertTrue(regional.isNotEmpty())
        assertEquals(listOf(regionName), regional)
    }

    @Test
    fun `regional boundary loads only regional cell`() {
        val regionName = "GB_ENG_Leicester"
        val region = Region().apply { name = regionName }
        val center = GeoPoint(52.6369, -1.1398)
        val bounds = LatLngBounds(
            LatLng(52.50, -1.30),
            LatLng(52.75, -1.00)
        )

        val regional = StopLoaderArgs.getCellIdsByCameraZoom(
            region = region,
            geoPoint = center,
            zoom = ZoomLevel.ZOOM_START_VALUE_TO_SHOW_REGIONAL,
            span = bounds
        )

        assertEquals(listOf(regionName), regional)
    }

    @Test
    fun `local boundary loads local and regional cells`() {
        val regionName = "GB_ENG_Leicester"
        val region = Region().apply { name = regionName }
        val center = GeoPoint(52.6369, -1.1398)
        val bounds = LatLngBounds(
            LatLng(52.62, -1.16),
            LatLng(52.65, -1.12)
        )

        val local = StopLoaderArgs.getCellIdsByCameraZoom(
            region = region,
            geoPoint = center,
            zoom = ZoomLevel.ZOOM_START_VALUE_FOR_LOCAL,
            span = bounds
        )

        assertTrue(local.contains(regionName))
        assertFalse(local.all { it == regionName })
        assertTrue(local.any { it.contains('#') })
    }

    // region #25936 - the local cell set must cover the viewport and nothing more.

    private fun cellFor(lat: Double, lon: Double): String {
        val c = StopLoaderArgs.CELLS_PER_DEGREE
        return "${Math.floor(lat * c).toInt()}#${Math.floor(lon * c).toInt()}"
    }

    /**
     * Every corner of the viewport must resolve to a cell we actually requested, otherwise
     * stops along that edge would never load.
     */
    private fun assertCoversCorners(lat: Double, lon: Double, latSpan: Double, lonSpan: Double) {
        val cells = StopLoaderArgs.getCellIdsForLocalLevel(lat, lon, latSpan, lonSpan)
        for (la in listOf(lat - latSpan / 2, lat + latSpan / 2)) {
            for (lo in listOf(lon - lonSpan / 2, lon + lonSpan / 2)) {
                assertTrue("missing corner cell ${cellFor(la, lo)} in $cells", cells.contains(cellFor(la, lo)))
            }
        }
    }

    @Test
    fun `local cells cover exactly the visible viewport in the southern hemisphere`() {
        // The Sydney street-level viewport measured on device for #25936.
        val lat = -33.8763916043141
        val lon = 151.20698997750878
        val latSpan = 0.019416189719976273
        val lonSpan = 0.0436975434422493

        val cells = StopLoaderArgs.getCellIdsForLocalLevel(lat, lon, latSpan, lonSpan)

        // 2 rows x 5 columns. Before the fix this returned 18 (3 rows x 6 columns) because the
        // lower bounds were truncated with toInt() and then had 1 subtracted.
        assertEquals(10, cells.size)
        assertCoversCorners(lat, lon, latSpan, lonSpan)

        // The extra row/column the old arithmetic added must be gone.
        assertFalse(cells.any { it.startsWith("-2540#") })
        assertFalse(cells.any { it.endsWith("#11337") })
    }

    @Test
    fun `local cells cover exactly the visible viewport in the northern hemisphere`() {
        val lat = 51.5074
        val lon = -0.1278
        val latSpan = 0.02
        val lonSpan = 0.04

        val cells = StopLoaderArgs.getCellIdsForLocalLevel(lat, lon, latSpan, lonSpan)

        assertCoversCorners(lat, lon, latSpan, lonSpan)
        // No row or column beyond the ones the corners need.
        val rows = cells.map { it.substringBefore('#').toInt() }.distinct()
        val cols = cells.map { it.substringAfter('#').toInt() }.distinct()
        assertEquals(
            Math.floor((lat + latSpan / 2) * StopLoaderArgs.CELLS_PER_DEGREE).toInt() -
                Math.floor((lat - latSpan / 2) * StopLoaderArgs.CELLS_PER_DEGREE).toInt() + 1,
            rows.size
        )
        assertEquals(
            Math.floor((lon + lonSpan / 2) * StopLoaderArgs.CELLS_PER_DEGREE).toInt() -
                Math.floor((lon - lonSpan / 2) * StopLoaderArgs.CELLS_PER_DEGREE).toInt() + 1,
            cols.size
        )
    }

    @Test
    fun `local cells cover the viewport across the equator and the prime meridian`() {
        assertCoversCorners(0.004, 0.004, 0.02, 0.02)
        assertCoversCorners(-0.004, -0.004, 0.02, 0.02)
    }

    // endregion
}
