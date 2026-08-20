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
}
