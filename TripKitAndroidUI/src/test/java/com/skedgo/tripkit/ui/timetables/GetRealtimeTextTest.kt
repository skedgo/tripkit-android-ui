package com.skedgo.tripkit.ui.timetables

import android.content.Context
import android.content.res.Resources
import android.text.format.DateFormat
import com.skedgo.tripkit.common.model.realtimealert.RealTimeStatus
import com.skedgo.tripkit.datetime.PrintTime
import com.skedgo.tripkit.routing.RealTimeVehicle
import com.skedgo.tripkit.ui.R
import com.skedgo.tripkit.ui.model.TimetableEntry
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import org.joda.time.DateTime
import org.joda.time.DateTimeZone
import org.joda.time.tz.UTCProvider
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test
import java.util.concurrent.TimeUnit

class GetRealtimeTextTest {

    private lateinit var getRealtimeText: GetRealtimeText
    private val context: Context = mockk()
    private val resources: Resources = mockk()
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
        mockkStatic(DateFormat::class)
        every { DateFormat.is24HourFormat(any()) } returns true
        every { context.resources } returns resources
        every { resources.getBoolean(R.bool.is_right_to_left) } returns false // Mock getBoolean()
        getRealtimeText = GetRealtimeText(context, printTime)
    }

    @After
    fun tearDown() {
        unmockkStatic(DateFormat::class)
    }

    @Test
    fun `should return scheduled time if real-time status is null`() {
        val service: TimetableEntry = mockk {
            every { realTimeStatus } returns null
            every { serviceTime } returns 1617187200L // Mock timestamp
            every { realtimeVehicle } returns null
            every { realTimeDeparture } returns 1617187200
            every { startTimeInSecs } returns 1617187200L
            every { endTimeInSecs } returns -1
            every { realTimeArrival } returns -1
            every { isCancelled } returns false
        }

        val dateTimeZone = DateTimeZone.UTC
        val formattedTime = "10:40"

        every { printTime.print(any()) } returns formattedTime
        every { context.getString(R.string.scheduled) } returns "Scheduled"
        every { context.getString(R.string.scheduled) } returns "Scheduled"

        val result = getRealtimeText.execute(dateTimeZone, service)

        assertEquals("Scheduled • 10:40" to R.color.black1, result)
    }

    @Test
    fun `should return no real-time available when real-time status is CAPABLE`() {
        val service: TimetableEntry = mockk {
            every { realTimeStatus } returns RealTimeStatus.CAPABLE
            every { realtimeVehicle } returns null
            every { realTimeDeparture } returns -1
            every { startTimeInSecs } returns 1617187200L
            every { endTimeInSecs } returns -1
            every { realTimeArrival } returns -1
            every { isCancelled } returns false
        }

        val dateTimeZone = DateTimeZone.UTC

        every { context.getString(R.string.no_realtime_available) } returns "No Real-Time Available"

        val result = getRealtimeText.execute(dateTimeZone, service)

        assertEquals("No Real-Time Available" to R.color.black1, result)
    }

    @Test
    fun `should return on-time status if departure matches service time`() {
        val service: TimetableEntry = mockk {
            every { realTimeStatus } returns RealTimeStatus.IS_REAL_TIME
            every { serviceTime } returns 1617187200L
            every { realtimeVehicle } returns null
            every { realTimeDeparture } returns -1
            every { startTimeInSecs } returns 1617187200L
            every { endTimeInSecs } returns -1
            every { realTimeArrival } returns -1
            every { isCancelled } returns false
        }

        val dateTimeZone = DateTimeZone.UTC
        val formattedTime = "10:40"

        every { printTime.print(any()) } returns formattedTime
        every { context.getString(R.string.on_time) } returns "On Time"

        val result = getRealtimeText.execute(dateTimeZone, service)

        assertEquals("On Time • 10:40" to R.color.tripKitSuccess, result)
    }

    @Test
    fun `should return early status if real-time departure is before scheduled time`() {
        val service: TimetableEntry = mockk {
            every { realTimeStatus } returns RealTimeStatus.IS_REAL_TIME
            every { serviceTime } returns 1617187300L
            every { realtimeVehicle } returns null
            every { realTimeDeparture } returns -1
            every { startTimeInSecs } returns 1617187200L
            every { endTimeInSecs } returns -1
            every { realTimeArrival } returns -1
            every { isCancelled } returns false

        }

        val dateTimeZone = DateTimeZone.UTC
        val formattedTime = "10:40"
        val earlyMinutes = "5 min early"

        every { printTime.print(any()) } returns formattedTime
        every { context.getString(R.string.realtime_early, any()) } returns earlyMinutes
        every {
            resources.getQuantityString(com.skedgo.tripkit.common.R.plurals.str_minutes, any(), any())
        } returns "mins"

        val result = getRealtimeText.execute(dateTimeZone, service)

        assertEquals("5 min early • 10:40" to R.color.tripKitWarning, result)
    }

    @Test
    fun `should return late status if real-time departure is after scheduled time`() {
        val service: TimetableEntry = mockk {
            every { realTimeStatus } returns RealTimeStatus.IS_REAL_TIME
            every { serviceTime } returns 1617187000L
            every { realtimeVehicle } returns null
            every { realTimeDeparture } returns 1617187100
            every { startTimeInSecs } returns 1617187200L
            every { endTimeInSecs } returns -1
            every { realTimeArrival } returns -1
            every { isCancelled } returns false
        }

        val dateTimeZone = DateTimeZone.UTC
        val formattedTime = "10:38"
        val lateMinutes = "10 min late"

        every { printTime.print(any()) } returns formattedTime
        every { context.getString(R.string.realtime_late, any()) } returns lateMinutes
        every {
            resources.getQuantityString(com.skedgo.tripkit.common.R.plurals.str_minutes, any(), any())
        } returns "mins"

        val result = getRealtimeText.execute(dateTimeZone, service)

        assertEquals("10 min late • 10:38" to R.color.tripKitError, result)
    }
}
