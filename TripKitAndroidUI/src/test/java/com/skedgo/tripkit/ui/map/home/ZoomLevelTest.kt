package com.skedgo.tripkit.ui.map.home

import org.junit.Assert.assertEquals
import org.junit.Test

class ZoomLevelTest {

    @Test
    fun `zoom bands are mutually exclusive at every boundary`() {
        assertEquals(ZoomLevel.CITY, ZoomLevel.fromLevel(8.0f))
        assertEquals(ZoomLevel.CITY, ZoomLevel.fromLevel(8.099f))
        assertEquals(ZoomLevel.REGIONAL, ZoomLevel.fromLevel(8.1f))
        assertEquals(ZoomLevel.REGIONAL, ZoomLevel.fromLevel(13.499f))
        assertEquals(ZoomLevel.LOCAL, ZoomLevel.fromLevel(13.5f))
        assertEquals(ZoomLevel.LOCAL, ZoomLevel.fromLevel(16.0f))
    }
}
