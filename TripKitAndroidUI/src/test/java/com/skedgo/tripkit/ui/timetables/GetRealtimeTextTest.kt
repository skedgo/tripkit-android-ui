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
        val nowSeconds = TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis())
        val service: TimetableEntry = mockk {
            every { realTimeStatus } returns RealTimeStatus.CAPABLE
            every { serviceTime } returns nowSeconds + 3600
            every { realtimeVehicle } returns null
            every { realTimeDeparture } returns -1
            every { startTimeInSecs } returns nowSeconds + 3600
            every { endTimeInSecs } returns -1
            every { realTimeArrival } returns -1
            every { isCancelled } returns false
        }

        val dateTimeZone = DateTimeZone.UTC

        every { context.getString(R.string.scheduled) } returns "Scheduled"

        val expectedSchedule = DateTime(TimeUnit.SECONDS.toMillis(nowSeconds + 3600))
            .toString("H:mm")

        val result = getRealtimeText.execute(dateTimeZone, service)

        assertEquals("Scheduled • $expectedSchedule" to R.color.black1, result)
    }

    @Test
    fun `should return scheduled time if real-time status is INCAPABLE`() {
        val service: TimetableEntry = mockk {
            every { realTimeStatus } returns RealTimeStatus.INCAPABLE
            every { serviceTime } returns 1617187200L
            every { realtimeVehicle } returns null
            every { realTimeDeparture } returns 1617187200
            every { startTimeInSecs } returns 1617187200L
            every { endTimeInSecs } returns -1
            every { realTimeArrival } returns -1
            every { isCancelled } returns false
        }

        val dateTimeZone = DateTimeZone.UTC
        every { context.getString(R.string.scheduled) } returns "Scheduled"

        val result = getRealtimeText.execute(dateTimeZone, service)

        assertEquals("Scheduled • 10:40" to R.color.black1, result)
    }

    @Test
    fun `should return realtime info when CAPABLE and service already started`() {
        val nowSeconds = TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis())
        val service: TimetableEntry = mockk {
            every { realTimeStatus } returns RealTimeStatus.CAPABLE
            every { serviceTime } returns nowSeconds - 60
            every { realtimeVehicle } returns null
            every { realTimeDeparture } returns (nowSeconds - 60).toInt()
            every { startTimeInSecs } returns nowSeconds - 60
            every { endTimeInSecs } returns -1
            every { realTimeArrival } returns -1
            every { isCancelled } returns false
        }

        val dateTimeZone = DateTimeZone.UTC
        every { printTime.print(any()) } returns "10:40"
        every { context.getString(R.string.on_time) } returns "On Time"

        val expectedSchedule = DateTime(TimeUnit.SECONDS.toMillis(nowSeconds - 60))
            .toString("H:mm")
        val result = getRealtimeText.execute(dateTimeZone, service)

        assertEquals("On Time • $expectedSchedule" to R.color.tripKitSuccess, result)
    }

    @Test
    fun `should keep cancelled behavior unchanged`() {
        val service: TimetableEntry = mockk {
            every { realTimeStatus } returns RealTimeStatus.CANCELLED
            every { isCancelled } returns false
            every { realtimeVehicle } returns null
            every { realTimeDeparture } returns -1
            every { startTimeInSecs } returns 1617187200L
            every { endTimeInSecs } returns -1
            every { realTimeArrival } returns -1
        }
        every { context.getString(R.string.cancelled) } returns "Cancelled"

        val result = getRealtimeText.execute(DateTimeZone.UTC, service)

        assertEquals("CANCELLED" to R.color.tripKitError, result)
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

    private fun futureService(status: RealTimeStatus = RealTimeStatus.CAPABLE) = TimetableEntry().apply {
        serviceTime = DateTime.now(DateTimeZone.UTC).plusDays(1).withTime(10, 45, 0, 0).millis / 1000
        startTimeInSecs = serviceTime
        realTimeStatus = status
    }

    private fun stubRealtimeStrings() {
        every { printTime.print(any()) } returns "10:45"
        every { context.getString(R.string.scheduled) } returns "Scheduled"
        every { context.getString(R.string.on_time) } returns "On time"
        every { context.getString(R.string.realtime_late, any()) } returns "Late"
        every { context.getString(R.string.realtime_early, any()) } returns "Early"
        every { resources.getQuantityString(com.skedgo.tripkit.common.R.plurals.str_minutes, any(), any()) } returns "mins"
    }

    private fun assertBoth(service: TimetableEntry, vehicle: RealTimeVehicle?, text: String, color: Int, onTime: Boolean = false) {
        assertEquals(text to color, getRealtimeText.execute(DateTimeZone.UTC, service, vehicle))
        assertEquals(Triple(text, color, onTime), getRealtimeText.getWithIsOnTime(DateTimeZone.UTC, service, vehicle))
    }

    @Test
    fun `capable valid carrier prediction uses realtime semantics in both presentations`() {
        stubRealtimeStrings()
        val service = futureService()
        for ((minutes, status, color) in listOf(
            Triple(4, "Late", R.color.tripKitError),
            Triple(-2, "Early", R.color.tripKitWarning),
            Triple(0, "On time", R.color.tripKitSuccess)
        )) {
            val vehicle = RealTimeVehicle().apply { arriveAtStartStopTime = service.serviceTime + minutes * 60 }
            service.realtimeVehicle = vehicle
            val time = DateTime(vehicle.arriveAtStartStopTime * 1000, DateTimeZone.UTC).toString("H:mm")
            assertBoth(service, vehicle, "$status • $time", color, minutes == 0)
        }
        assertEquals(RealTimeStatus.CAPABLE, service.realTimeStatus)
    }

    @Test
    fun `capable positive entry prediction works without a carrier`() {
        stubRealtimeStrings()
        val service = futureService().apply { realTimeDeparture = (serviceTime + 240).toInt() }
        assertBoth(service, null, "Late • 10:49", R.color.tripKitError)
    }

    @Test
    fun `capable null zero and sentinel predictions remain scheduled in both presentations`() {
        stubRealtimeStrings()
        val service = futureService()
        for (timestamp in listOf(-1L, 0L)) {
            service.realTimeDeparture = timestamp.toInt()
            service.realtimeVehicle = RealTimeVehicle().apply { arriveAtStartStopTime = timestamp }
            assertBoth(service, service.realtimeVehicle, "Scheduled • 10:45", R.color.black1)
            service.realtimeVehicle = null
            assertBoth(service, null, "Scheduled • 10:45", R.color.black1)
        }
    }

    @Test
    fun `capable prediction removed returns to scheduled and realtime status fallback is unchanged`() {
        stubRealtimeStrings()
        val service = futureService()
        service.realtimeVehicle = RealTimeVehicle().apply { arriveAtStartStopTime = service.serviceTime + 240 }
        assertBoth(service, service.realtimeVehicle, "Late • 10:49", R.color.tripKitError)
        service.realtimeVehicle = null
        assertBoth(service, null, "Scheduled • 10:45", R.color.black1)
        service.realTimeStatus = RealTimeStatus.IS_REAL_TIME
        assertBoth(service, null, "On time • 10:45", R.color.tripKitSuccess, true)
        service.realtimeVehicle = RealTimeVehicle().apply { arriveAtStartStopTime = service.serviceTime + 240 }
        assertBoth(service, service.realtimeVehicle, "Late • 10:49", R.color.tripKitError)
    }
}
