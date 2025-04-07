package com.skedgo.tripkit.ui.timetables

import com.skedgo.tripkit.datetime.PrintTime
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.joda.time.DateTime
import org.joda.time.DateTimeZone
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class GetTimeRangeTextTest {

    private lateinit var getTimeRangeText: GetTimeRangeText
    private val printTime: PrintTime = mockk()

    @Before
    fun setUp() {
        getTimeRangeText = GetTimeRangeText(printTime)
    }

    @Test
    fun `should return formatted time range`() {
        val dateTimeZone = DateTimeZone.UTC
        val startTimeInSecs = 1617187200L // Mock start time
        val endTimeInSecs = 1617190800L // Mock end time

        val expectedStartTime = "12:00 PM"
        val expectedEndTime = "1:00 PM"
        val expectedResult = "$expectedStartTime - $expectedEndTime"

        every { printTime.print(DateTime(startTimeInSecs * 1000, dateTimeZone)) } returns expectedStartTime
        every { printTime.print(DateTime(endTimeInSecs * 1000, dateTimeZone)) } returns expectedEndTime

        val result = getTimeRangeText.execute(dateTimeZone, startTimeInSecs, endTimeInSecs)

        assertEquals(expectedResult, result)

        verify { printTime.print(DateTime(startTimeInSecs * 1000, dateTimeZone)) }
        verify { printTime.print(DateTime(endTimeInSecs * 1000, dateTimeZone)) }
    }
}
