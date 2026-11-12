package com.skedgo.tripkit.ui.model

import com.google.gson.Gson
import com.skedgo.tripkit.common.model.stop.ScheduledStop
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class DeparturesResponseTest {

    private fun wheelchairAccessible(trip: Boolean?, boarding: Boolean?, alighting: Boolean? = null): Boolean? {
        val entry = TimetableEntry().apply {
            wheelchairAccessible = trip
            endStopCode = "end"
        }
        val response = DeparturesResponse().apply {
            embarkationStopList = listOf(
                DeparturesResponse.ServicesResponse("start", listOf(entry), boarding)
            )
            stopList = listOf(ScheduledStop().apply {
                code = "end"
                wheelchairAccessible = alighting
            })
        }
        response.processEmbarkationStopList()
        return entry.wheelchairAccessible
    }

    @Test fun `an accessible trip stays accessible at a stop with no information`() {
        assertEquals(true, wheelchairAccessible(trip = true, boarding = null))
    }

    @Test fun `an accessible stop makes a trip with no information accessible`() {
        assertEquals(true, wheelchairAccessible(trip = null, boarding = true))
    }

    @Test fun `an inaccessible boarding stop overrides an accessible trip`() {
        assertEquals(false, wheelchairAccessible(trip = true, boarding = false))
    }

    @Test fun `an inaccessible alighting stop overrides an accessible trip`() {
        assertEquals(false, wheelchairAccessible(trip = true, boarding = true, alighting = false))
    }

    @Test fun `an inaccessible trip stays inaccessible at an accessible stop`() {
        assertEquals(false, wheelchairAccessible(trip = false, boarding = true))
    }

    @Test fun `nothing known stays unknown`() {
        assertEquals(null, wheelchairAccessible(trip = null, boarding = null))
    }

    @Test fun `alighting stop nested under a parent station is found`() {
        val entry = TimetableEntry().apply {
            wheelchairAccessible = true
            endStopCode = "platform"
        }
        val response = DeparturesResponse().apply {
            embarkationStopList = listOf(
                DeparturesResponse.ServicesResponse("start", listOf(entry), null)
            )
            stopList = listOf(ScheduledStop().apply {
                code = "station"
                children = listOf(ScheduledStop().apply {
                    code = "platform"
                    wheelchairAccessible = false
                })
            })
        }
        response.processEmbarkationStopList()
        assertEquals(false, entry.wheelchairAccessible)
    }

    @Test fun `the boarding stop value is read from the departures response`() {
        val response = Gson().fromJson(
            """{"embarkationStops":[{"stopCode":"start","wheelchairAccessible":false,"services":[{"wheelchairAccessible":true}]}]}""",
            DeparturesResponse::class.java
        )
        response.processEmbarkationStopList()
        assertEquals(false, response.serviceList!!.single().wheelchairAccessible)
    }
}
