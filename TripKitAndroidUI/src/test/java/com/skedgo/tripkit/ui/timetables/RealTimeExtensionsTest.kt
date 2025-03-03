package com.skedgo.tripkit.ui.timetables

import com.skedgo.tripkit.routing.RealTimeVehicle
import com.skedgo.tripkit.ui.model.TimetableEntry
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Test

class RealTimeExtensionsTest {

    @Test
    fun `realTimeArrival should return vehicle arriveAtEndStopTime if available`() {
        val vehicle: RealTimeVehicle = mockk {
            every { arriveAtEndStopTime } returns 1620000000L
        }
        val service: TimetableEntry = mockk {
            every { realTimeArrival } returns 0
            every { endTimeInSecs } returns 0
        }

        val result = realTimeArrival(service, vehicle)
        assertEquals(1620000000L, result)
    }

    @Test
    fun `realTimeArrival should return service realTimeArrival if vehicle time is unavailable`() {
        val service: TimetableEntry = mockk {
            every { realTimeArrival } returns 1620001000
            every { endTimeInSecs } returns 0
        }

        val result = realTimeArrival(service, null)
        assertEquals(1620001000L, result)
    }

    @Test
    fun `realTimeArrival should return service endTimeInSecs if no real-time values exist`() {
        val service: TimetableEntry = mockk {
            every { realTimeArrival } returns 0
            every { endTimeInSecs } returns 1620002000
        }

        val result = realTimeArrival(service, null)
        assertEquals(1620002000L, result)
    }

    @Test
    fun `realTimeArrival should return 0 if all values are missing`() {
        val service: TimetableEntry = mockk {
            every { realTimeArrival } returns 0
            every { endTimeInSecs } returns 0
        }

        val result = realTimeArrival(service, null)
        assertEquals(0, result)
    }

    @Test
    fun `realTimeDeparture should return vehicle arriveAtStartStopTime if available`() {
        val vehicle: RealTimeVehicle = mockk {
            every { arriveAtStartStopTime } returns 1619000000L
        }
        val service: TimetableEntry = mockk {
            every { realTimeDeparture } returns 0
            every { startTimeInSecs } returns 0
        }

        val result = realTimeDeparture(service, vehicle)
        assertEquals(1619000000L, result)
    }

    @Test
    fun `realTimeDeparture should return service realTimeDeparture if vehicle time is unavailable`() {
        val service: TimetableEntry = mockk {
            every { realTimeDeparture } returns 1619001000
            every { startTimeInSecs } returns 0
        }

        val result = realTimeDeparture(service, null)
        assertEquals(1619001000L, result)
    }

    @Test
    fun `realTimeDeparture should return service startTimeInSecs if no real-time values exist`() {
        val service: TimetableEntry = mockk {
            every { realTimeDeparture } returns 0
            every { startTimeInSecs } returns 1619002000
        }

        val result = realTimeDeparture(service, null)
        assertEquals(1619002000L, result)
    }

    @Test
    fun `realTimeDeparture should return 0 if all values are missing`() {
        val service: TimetableEntry = mockk {
            every { realTimeDeparture } returns 0
            every { startTimeInSecs } returns 0
        }

        val result = realTimeDeparture(service, null)
        assertEquals(0, result)
    }
}
