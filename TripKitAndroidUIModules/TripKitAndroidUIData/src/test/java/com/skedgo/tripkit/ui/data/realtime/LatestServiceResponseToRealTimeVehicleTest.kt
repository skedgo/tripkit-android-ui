package com.skedgo.tripkit.ui.data.realtime

import com.google.gson.Gson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LatestServiceResponseToRealTimeVehicleTest {

    @Test
    fun `converts plural realtime vehicle alternatives returned by latest endpoint`() {
        val response = Gson().fromJson(
            """
            {
              "serviceTripID": "service-trip-id",
              "realtimeVehicleAlternatives": [
                {
                  "lastUpdate": 1786554900,
                  "location": { "lat": 52.63, "lng": -1.13, "bearing": 10 }
                },
                {
                  "lastUpdate": 1786554901,
                  "location": { "lat": 52.59, "lng": -1.15, "bearing": 20 }
                }
              ]
            }
            """.trimIndent(),
            LatestServiceResponse::class.java
        )

        val vehicles = response.toRealTimeVehicles()

        assertEquals(2, vehicles.size)
        assertTrue(vehicles.all { it.serviceTripId == "service-trip-id" })
        assertEquals(52.63, vehicles[0].location.lat, 0.0)
        assertEquals(52.59, vehicles[1].location.lat, 0.0)
    }
}
