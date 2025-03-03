package com.skedgo.tripkit.ui.timetables

import com.skedgo.tripkit.ui.model.TimetableEntry
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class GetServiceTertiaryTextTest {

    private lateinit var getServiceTertiaryText: GetServiceTertiaryText

    @Before
    fun setUp() {
        getServiceTertiaryText = GetServiceTertiaryText()
    }

    @Test
    fun `should return start platform and service direction if start platform is available`() {
        val service: TimetableEntry = mockk {
            every { startPlatform } returns "Platform 3"
            every { serviceDirection } returns "Northbound"
        }

        val result = getServiceTertiaryText.execute(service)

        assertEquals("Platform 3 • Northbound", result)
    }

    @Test
    fun `should return only service direction if start stop short name is blank`() {
        val service: TimetableEntry = mockk {
            every { startPlatform } returns null
            every { startStopShortName } returns ""
            every { serviceDirection } returns "Eastbound"
        }

        val result = getServiceTertiaryText.execute(service)

        assertEquals("Eastbound", result)
    }

    @Test
    fun `should return start stop short name and service direction if start platform is blank`() {
        val service: TimetableEntry = mockk {
            every { startPlatform } returns null
            every { startStopShortName } returns "Central"
            every { serviceDirection } returns "Southbound"
        }

        val result = getServiceTertiaryText.execute(service)

        assertEquals("Central • Southbound", result)
    }

    @Test
    fun `should return empty string if both start stop short name and service direction are blank`() {
        val service: TimetableEntry = mockk {
            every { startPlatform } returns null
            every { startStopShortName } returns ""
            every { serviceDirection } returns null
        }

        val result = getServiceTertiaryText.execute(service)

        assertEquals("", result)
    }
}
