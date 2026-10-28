package com.skedgo.tripkit.ui.data.realtime

import com.google.gson.Gson
import org.junit.Assert.*
import org.junit.Test

class LatestServiceResponseToRealTimeVehicleTest {
    private fun response(trip: String, actual: Boolean = false): LatestServiceResponse = Gson().fromJson(
        """{
          "serviceTripID": "$trip", "startStopCode": "3052", "startTime": 1786555000,
          ${if (actual) "\"realtimeVehicle\": {\"label\": \"own-$trip\", \"occupancy\": \"MANY_SEATS_AVAILABLE\", \"location\": {\"lat\": -35.28, \"lng\": 149.13}}," else ""}
          "realtimeVehicleAlternatives": [
            {"label": "other-bus", "occupancy": "FULL", "lastUpdate": 1786554900,
             "location": {"lat": -35.29, "lng": 149.14}}
          ],
          "realtimeAlternativeVehicle": [
            {"label": "legacy-bus", "lastUpdate": 1786554901,
             "location": {"lat": -35.30, "lng": 149.15}}
          ]
        }""", LatestServiceResponse::class.java)

    @Test fun `scheduled departure with only alternatives has no service vehicle`() {
        assertTrue(response("scheduled").toRealTimeVehicles().isEmpty())
    }

    @Test fun `actual vehicle retains its service ownership occupancy and arrival mapping`() {
        val vehicle = response("assigned", true).toRealTimeVehicles().single()
        assertEquals("own-assigned", vehicle.label)
        assertEquals("assigned", vehicle.serviceTripId)
        assertNotNull(vehicle.occupancy)
        assertEquals("3052", vehicle.startStopCode)
        assertEquals(1786555000L, vehicle.arriveAtStartStopTime)
    }

    @Test fun `same route services never acquire each others alternatives`() {
        val scheduled = response("scheduled")
        val assigned = response("assigned", true)
        val updates = listOf(scheduled, assigned).flatMap { it.toRealTimeVehicles() }
        assertEquals(listOf("assigned"), updates.map { it.serviceTripId })
        assertEquals(listOf("own-assigned"), updates.map { it.label })
    }
    @Test fun `maps retain modern and legacy alternatives without rewriting their ownership`() {
        val response = response("assigned", true)
        val alternative = response.realtimeVehicleAlternatives()!!.single()
        alternative.serviceTripId = "another-trip"
        alternative.startStopCode = "another-stop"
        val mapVehicles = response.toMapRealTimeVehicles()
        assertEquals(listOf("own-assigned", "other-bus", "legacy-bus"), mapVehicles.map { it.label })
        assertEquals("another-trip", alternative.serviceTripId)
        assertEquals("another-stop", alternative.startStopCode)
        assertNull(response.realtimeAlternativeVehicle()!!.single().serviceTripId)
        assertEquals(listOf("assigned"), response.toRealTimeVehicles().map { it.serviceTripId })
    }

}
