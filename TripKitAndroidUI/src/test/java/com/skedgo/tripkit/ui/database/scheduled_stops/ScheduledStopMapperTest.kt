package com.skedgo.tripkit.ui.database.scheduled_stops

import com.google.gson.Gson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class ScheduledStopMapperTest {

    private lateinit var mapper: ScheduledStopMapper

    @Before
    fun setUp() {
        mapper = ScheduledStopMapper(Gson())
    }

    private fun entity(isParent: Int, apiZoomLevel: Int = 0): ScheduledStopWithLocation =
        ScheduledStopWithLocation(
            scheduledStop = ScheduledStopEntity(
                code = "STOP_1",
                cellCode = "1#1",
                stopType = null,
                shortName = "Stop",
                services = null,
                parentId = null,
                isParent = isParent,
                apiZoomLevel = apiZoomLevel,
                modeInfo = null,
                filter = null
            ),
            location = null
        )

    @Test
    fun `isParent 1 maps to isRegionalStop true`() {
        val stop = mapper.mapToDomain(entity(isParent = 1))
        assertTrue(stop.isRegionalStop)
    }

    @Test
    fun `isParent 0 maps to isRegionalStop false`() {
        val stop = mapper.mapToDomain(entity(isParent = 0))
        assertFalse(stop.isRegionalStop)
    }

    @Test
    fun `apiZoomLevel maps to domain`() {
        val stop = mapper.mapToDomain(entity(isParent = 0, apiZoomLevel = 1))
        assertEquals(1, stop.apiZoomLevel)
    }
}
