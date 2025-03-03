package com.skedgo.tripkit.ui.timetables

import com.skedgo.tripkit.datetime.PrintTime
import com.skedgo.tripkit.routing.RealTimeVehicle
import com.skedgo.tripkit.ui.model.TimetableEntry
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.joda.time.DateTimeZone
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class GetServiceTitleTextTest {

    private lateinit var getServiceTitleText: GetServiceTitleText
    private val getFrequencyText: GetFrequencyText = mockk()
    private val getA2BTime: GetA2BTime = mockk()
    private val getOrdinaryTime: GetOrdinaryTime = mockk()

    @Before
    fun setUp() {
        getServiceTitleText = GetServiceTitleText(getFrequencyText, getA2BTime, getOrdinaryTime)
    }

    @Test
    fun `should return frequency text if service is frequency-based`() {
        val service: TimetableEntry = mockk {
            every { isFrequencyBased } returns true
            every { realTimeDeparture } returns -1
            every { startTimeInSecs } returns -1
            every { realTimeArrival } returns -1
            every { endTimeInSecs } returns -1
        }
        val dateTimeZone = DateTimeZone.UTC
        val expectedText = "Every 15 minutes"

        every { getFrequencyText.execute(service) } returns expectedText

        val result = getServiceTitleText.execute(service, dateTimeZone)

        assertEquals(expectedText, result)
        verify { getFrequencyText.execute(service) }
    }

    @Test
    fun `should return A2B time if endTimeInSecs is not zero`() {
        val service: TimetableEntry = mockk {
            every { isFrequencyBased } returns false
            every { realTimeDeparture } returns -1
            every { startTimeInSecs } returns -1
            every { realTimeArrival } returns -1
            every { endTimeInSecs } returns -1
        }
        val vehicle: RealTimeVehicle? = null
        val dateTimeZone = DateTimeZone.UTC
        val expectedText = "12:30 PM - 1:15 PM"
        
        every { realTimeDeparture(service, vehicle) } returns 1617187200L
        every { realTimeArrival(service, vehicle) } returns 1617190800L // Non-zero endTime
        every { getA2BTime.execute(dateTimeZone, service, 1617187200L, 1617190800L) } returns expectedText

        val result = getServiceTitleText.execute(service, dateTimeZone, vehicle)

        assertEquals(expectedText, result)
        verify { getA2BTime.execute(dateTimeZone, service, 1617187200L, 1617190800L) }
    }

    @Test
    fun `should return ordinary time if endTimeInSecs is zero`() {
        val service: TimetableEntry = mockk {
            every { isFrequencyBased } returns false
            every { realTimeDeparture } returns -1
            every { startTimeInSecs } returns -1
            every { realTimeArrival } returns -1
            every { endTimeInSecs } returns -1
        }
        val vehicle: RealTimeVehicle? = null
        val dateTimeZone = DateTimeZone.UTC
        val expectedText = "Departing at 12:45 PM"

        every { realTimeDeparture(service, vehicle) } returns 1617187200L
        every { realTimeArrival(service, vehicle) } returns 0L // Zero endTime
        every { getOrdinaryTime.execute(dateTimeZone, service, vehicle) } returns expectedText

        val result = getServiceTitleText.execute(service, dateTimeZone, vehicle)

        assertEquals(expectedText, result)
        verify { getOrdinaryTime.execute(dateTimeZone, service, vehicle) }
    }
}
