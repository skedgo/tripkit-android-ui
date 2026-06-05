package com.skedgo.tripkit.ui.map.home

import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.skedgo.tripkit.common.model.region.Region
import com.skedgo.tripkit.location.GeoPoint
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StopLoaderArgsTest {

    @Test
    fun `regional cell ids are numeric and not region name`() {
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
        assertFalse(regional.contains(regionName))
        assertTrue(regional.all { it.matches(Regex("-?\\d+#-?\\d+")) })
    }

    @Test
    fun `regional zoom covers the whole viewport with multiple cells`() {
        val region = Region().apply { name = "AU_NT_Darwin" }
        val center = GeoPoint(-12.4634, 130.8456)
        // ~0.1 degree span -> several grid cells per axis, not a single centre cell.
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

        // Previously this returned exactly one centre cell (the bug).
        assertTrue(regional.size > 1)
        assertTrue(regional.size <= StopLoaderArgs.MAX_REGIONAL_CELLS)
    }

    @Test
    fun `extremely zoomed out regional viewport falls back to single centre cell`() {
        val center = GeoPoint(-12.4634, 130.8456)
        // Huge span -> grid would exceed MAX_REGIONAL_CELLS, so we cap to the centre cell.
        val hugeSpan = LatLngBounds(
            LatLng(-20.0, 120.0),
            LatLng(-5.0, 140.0)
        )

        val regional = StopLoaderArgs.getCellIdsForRegionalLevel(center, hugeSpan)

        assertTrue(regional.size == 1)
        assertTrue(regional.all { it.matches(Regex("-?\\d+#-?\\d+")) })
    }

    @Test
    fun `local zoom requests both local grid and regional cells`() {
        val region = Region().apply { name = "AU_NT_Darwin" }
        val center = GeoPoint(-12.4634, 130.8456)
        val bounds = LatLngBounds(
            LatLng(-12.47, 130.84),
            LatLng(-12.45, 130.86)
        )

        val local = StopLoaderArgs.getCellIdsByCameraZoom(
            region = region,
            geoPoint = center,
            zoom = ZoomLevel.ZOOM_START_VALUE_FOR_LOCAL + 1.0f,
            span = bounds
        )

        assertTrue(local.isNotEmpty())
        assertTrue(local.all { it.matches(Regex("-?\\d+#-?\\d+")) })
    }
}
