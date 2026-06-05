package com.skedgo.tripkit.ui.map.home

import com.skedgo.tripkit.common.model.stop.ScheduledStop
import com.skedgo.tripkit.common.model.stop.StopType
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StopMarkerEligibilityTest {

    @Test
    fun `REGION api level bus stop is visible in REGION_ONLY`() {
        val stop = ScheduledStop().apply {
            apiZoomLevel = ApiZoomLevels.REGION
            type = StopType.BUS
            isRegionalStop = false
        }
        assertTrue(isStopVisibleForMarkerMode(stop, MarkerZoomMode.REGION_ONLY))
    }

    @Test
    fun `LOCAL api level stop is hidden in REGION_ONLY`() {
        val stop = ScheduledStop().apply {
            apiZoomLevel = ApiZoomLevels.LOCAL
            type = StopType.BUS
            isRegionalStop = false
        }
        assertFalse(isStopVisibleForMarkerMode(stop, MarkerZoomMode.REGION_ONLY))
    }

    @Test
    fun `REGION_AND_LOCAL mode shows both region and local bus stops`() {
        val regionStop = ScheduledStop().apply {
            apiZoomLevel = ApiZoomLevels.REGION
            type = StopType.BUS
        }
        val localStop = ScheduledStop().apply {
            apiZoomLevel = ApiZoomLevels.LOCAL
            type = StopType.BUS
        }

        assertTrue(isStopVisibleForMarkerMode(regionStop, MarkerZoomMode.REGION_AND_LOCAL))
        assertTrue(isStopVisibleForMarkerMode(localStop, MarkerZoomMode.REGION_AND_LOCAL))
    }

    @Test
    fun `CITY_ONLY mode hides all stop markers`() {
        val stop = ScheduledStop().apply {
            apiZoomLevel = ApiZoomLevels.REGION
            type = StopType.BUS
        }
        assertFalse(isStopVisibleForMarkerMode(stop, MarkerZoomMode.CITY_ONLY))
    }

    @Test
    fun `legacy unknown-level row uses fallback`() {
        val fallbackRegional = ScheduledStop().apply {
            apiZoomLevel = 0
            isRegionalStop = true
            type = StopType.BUS
        }
        val fallbackNonRegional = ScheduledStop().apply {
            apiZoomLevel = 0
            isRegionalStop = false
            type = StopType.BUS
        }

        assertTrue(isStopVisibleForMarkerMode(fallbackRegional, MarkerZoomMode.REGION_ONLY))
        assertFalse(isStopVisibleForMarkerMode(fallbackNonRegional, MarkerZoomMode.REGION_ONLY))
    }
}
