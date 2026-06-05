package com.skedgo.tripkit.ui.map.home

import com.skedgo.tripkit.common.model.stop.ScheduledStop
import com.skedgo.tripkit.ui.map.StopPOILocation
import com.skedgo.tripkit.ui.map.adapter.StopInfoWindowAdapter
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Test

class MarkerDiffKeyTest {

    @Test
    fun `stop marker diff key includes api zoom level`() {
        val stop = ScheduledStop().apply {
            code = "STOP_1"
            apiZoomLevel = ApiZoomLevels.REGION
        }
        val poi = StopPOILocation(stop, mockk<StopInfoWindowAdapter>(relaxed = true))

        assertEquals("STOP_1:1", markerDiffKey(poi))
    }
}
