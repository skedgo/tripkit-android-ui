package com.skedgo.tripkit.ui.timetables

import android.content.Context
import com.skedgo.tripkit.common.model.stop.ScheduledStop
import com.skedgo.tripkit.common.model.stop.StopType.TRAIN
import com.skedgo.tripkit.ui.R
import com.skedgo.tripkit.ui.model.TimetableEntry
import io.mockk.every
import io.mockk.mockk
import org.joda.time.DateTimeZone
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class GetA2BTimeTest {

    private lateinit var getA2BTime: GetA2BTime
    private val context: Context = mockk(relaxed = true)
    private val getTimeRangeText: GetTimeRangeText = mockk()

    @Before
    fun setUp() {
        getA2BTime = GetA2BTime(context, getTimeRangeText)
    }

    @Test
    fun `should return formatted service title with time range`() {
        // Mock DateTimeZone
        val dateTimeZone: DateTimeZone = DateTimeZone.UTC

        // Mock TimetableEntry
        val service = mockk<TimetableEntry> {
            every { serviceNumber } returns "Bus 123"
            every { startStop } returns null
        }

        // Mock time range text
        val expectedTimeRange = "10:00 AM - 11:00 AM"
        every { getTimeRangeText.execute(dateTimeZone, 1620000000L, 1620003600L) } returns expectedTimeRange

        // Run function
        val result = getA2BTime.execute(dateTimeZone, service, 1620000000L, 1620003600L)

        // Verify
        assertEquals("Bus 123: 10:00 AM - 11:00 AM", result)
    }

    @Test
    fun `should return stop type as title if service number is empty`() {
        // Mock DateTimeZone
        val dateTimeZone: DateTimeZone = DateTimeZone.UTC

        // Mock TimetableEntry with empty service number
        val service = mockk<TimetableEntry> {
            every { serviceNumber } returns null
            every { startStop } returns mockk<ScheduledStop> {
                every { type } returns TRAIN
            }
        }

        // Mock time range text
        val expectedTimeRange = "2:00 PM - 3:00 PM"
        every { getTimeRangeText.execute(dateTimeZone, 1620050000L, 1620053600L) } returns expectedTimeRange

        // Run function
        val result = getA2BTime.execute(dateTimeZone, service, 1620050000L, 1620053600L)

        // Verify
        assertEquals("Train: 2:00 PM - 3:00 PM", result)
    }

    @Test
    fun `should return default service title if service number and stop type are null`() {
        // Mock DateTimeZone
        val dateTimeZone: DateTimeZone = DateTimeZone.UTC

        // Mock TimetableEntry with no service number and null startStop
        val service = mockk<TimetableEntry> {
            every { serviceNumber } returns null
            every { startStop } returns null
        }

        // Mock context string resource
        every { context.getString(R.string.service) } returns "Service"

        // Mock time range text
        val expectedTimeRange = "4:00 PM - 5:00 PM"
        every { getTimeRangeText.execute(dateTimeZone, 1620060000L, 1620063600L) } returns expectedTimeRange

        // Run function
        val result = getA2BTime.execute(dateTimeZone, service, 1620060000L, 1620063600L)

        // Verify
        assertEquals("Service: 4:00 PM - 5:00 PM", result)
    }
}
