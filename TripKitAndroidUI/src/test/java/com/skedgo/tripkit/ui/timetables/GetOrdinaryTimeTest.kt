package com.skedgo.tripkit.ui.timetables

import android.content.Context
import com.skedgo.tripkit.common.model.stop.StopType.FERRY
import com.skedgo.tripkit.datetime.PrintTime
import com.skedgo.tripkit.ui.R
import com.skedgo.tripkit.ui.model.TimetableEntry
import io.mockk.every
import io.mockk.mockk
import org.joda.time.DateTimeZone
import org.joda.time.tz.UTCProvider
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test

class GetOrdinaryTimeTest {

    private lateinit var getOrdinaryTime: GetOrdinaryTime
    private val context: Context = mockk()
    private val printTime: PrintTime = mockk()

    companion object {
        @BeforeClass
        @JvmStatic
        fun setupTimeZone() {
            DateTimeZone.setProvider(UTCProvider()) // Fix Joda-Time issue
        }
    }

    @Before
    fun setUp() {
        getOrdinaryTime = GetOrdinaryTime(context, printTime)
    }

    @Test
    fun `should return formatted time with default service label`() {
        // Mock TimetableEntry
        val service: TimetableEntry = mockk {
            every { serviceNumber } returns null
            every { startStop?.type } returns null
            every { realTimeDeparture } returns 12345
            every { startTimeInSecs } returns 12345
        }

        val dateTimeZone = DateTimeZone.UTC
        val departureTime = "3:15 PM"

        // Mock dependencies
        every { printTime.print(any()) } returns departureTime
        every { context.getString(R.string.service) } returns "Service"
        every {
            context.getString(
                R.string._pattern_at__pattern,
                "Service",
                departureTime
            )
        } returns "Service at 3:15 PM"

        // Execute function
        val result = getOrdinaryTime.execute(dateTimeZone, service)

        // Verify
        assertEquals("Service at 3:15 PM", result)
    }

    @Test
    fun `should return formatted time with service number`() {
        val service: TimetableEntry = mockk {
            every { serviceNumber } returns "Bus 123"
            every { startStop?.type } returns null
            every { realTimeDeparture } returns 12345
            every { startTimeInSecs } returns 12345
        }

        val dateTimeZone = DateTimeZone.UTC
        val departureTime = "12:45 PM"

        every { printTime.print(any()) } returns departureTime
        every {
            context.getString(
                R.string._pattern_at__pattern,
                "Bus 123",
                departureTime
            )
        } returns "Bus 123 at 12:45 PM"

        val result = getOrdinaryTime.execute(dateTimeZone, service)

        assertEquals("Bus 123 at 12:45 PM", result)
    }

    @Test
    fun `should return formatted time with stop type`() {
        val service: TimetableEntry = mockk {
            every { serviceNumber } returns null
            every { startStop?.type } returns FERRY
            every { realTimeDeparture } returns 12345
            every { startTimeInSecs } returns 12345
        }

        val dateTimeZone = DateTimeZone.UTC
        val departureTime = "2:30 PM"

        every { printTime.print(any()) } returns departureTime
        every {
            context.getString(
                R.string._pattern_at__pattern,
                "Ferry",
                departureTime
            )
        } returns "Ferry at 2:30 PM"

        val result = getOrdinaryTime.execute(dateTimeZone, service)

        assertEquals("Ferry at 2:30 PM", result)
    }

    @Test
    fun `should return formatted time with real-time departure`() {
        val service: TimetableEntry = mockk {
            every { serviceNumber } returns "Train 99"
            every { startStop?.type } returns null
            every { realTimeDeparture } returns 12345
            every { startTimeInSecs } returns 12345
        }

        val dateTimeZone = DateTimeZone.UTC
        val departureTime = "4:00 PM"

        every { printTime.print(any()) } returns departureTime
        every {
            context.getString(
                R.string._pattern_at__pattern,
                "Train 99",
                departureTime
            )
        } returns "Train 99 at 4:00 PM"

        val result = getOrdinaryTime.execute(dateTimeZone, service)

        assertEquals("Train 99 at 4:00 PM", result)
    }
}