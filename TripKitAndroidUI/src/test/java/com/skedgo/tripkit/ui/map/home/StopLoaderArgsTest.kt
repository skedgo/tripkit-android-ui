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
}
