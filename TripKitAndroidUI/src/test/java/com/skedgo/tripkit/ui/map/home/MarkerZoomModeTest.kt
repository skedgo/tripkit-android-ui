package com.skedgo.tripkit.ui.map.home

import org.junit.Assert.assertEquals
import org.junit.Test

class MarkerZoomModeTest {

    @Test
    fun `at or below city threshold is CITY_ONLY`() {
        assertEquals(MarkerZoomMode.CITY_ONLY, ZoomLevel.markerModeFor(8.0f))
        assertEquals(MarkerZoomMode.CITY_ONLY, ZoomLevel.markerModeFor(5.0f))
    }

    @Test
    fun `just above city threshold is REGION_ONLY`() {
        assertEquals(MarkerZoomMode.REGION_ONLY, ZoomLevel.markerModeFor(8.01f))
    }

    @Test
    fun `at local threshold is still REGION_ONLY`() {
        assertEquals(MarkerZoomMode.REGION_ONLY, ZoomLevel.markerModeFor(13.5f))
    }

    @Test
    fun `above local threshold is REGION_AND_LOCAL`() {
        assertEquals(MarkerZoomMode.REGION_AND_LOCAL, ZoomLevel.markerModeFor(13.51f))
        assertEquals(MarkerZoomMode.REGION_AND_LOCAL, ZoomLevel.markerModeFor(18.0f))
    }
}
