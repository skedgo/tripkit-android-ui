package com.skedgo.tripkit.ui.trip.options

import android.content.res.Resources
import android.text.format.DateFormat
import com.skedgo.tripkit.ui.R
import com.skedgo.tripkit.ui.trip.ArriveBy
import com.skedgo.tripkit.ui.trip.LeaveAfter
import com.skedgo.tripkit.ui.trip.Now
import com.skedgo.tripkit.ui.utils.SystemTimeFormatManager
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.unmockkObject
import io.mockk.unmockkStatic
import io.reactivex.Single
import io.reactivex.observers.TestObserver
import org.amshove.kluent.internal.assertEquals
import org.joda.time.DateTime
import org.joda.time.DateTimeZone
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.*

class RoutingTimeViewModelMapperTest {

    private lateinit var mapper: RoutingTimeViewModelMapper
    private val resources: Resources = mockk()

    @Before
    fun setUp() {
        // Mock SystemTimeFormatManager
        mockkObject(SystemTimeFormatManager)
        every { SystemTimeFormatManager.getTimeFormatPattern() } returns "h:mm a"
        
        // Mock DateFormat.is24HourFormat for SystemTimeFormatManager internal usage
        mockkStatic(DateFormat::class)
        every { DateFormat.is24HourFormat(any()) } returns false
        
        mapper = RoutingTimeViewModelMapper(resources)
    }
    
    @After
    fun tearDown() {
        unmockkObject(SystemTimeFormatManager)
        unmockkStatic(DateFormat::class)
    }

    @Test
    fun `toText returns correct text for Now`() {
        every { resources.getString(R.string.leave_now) } returns "Leave Now"

        val testObserver = TestObserver<String>()
        mapper.toText(Now).subscribe(testObserver)

        testObserver.assertValue("Leave Now")
    }

    @Test
    fun `toText returns formatted text for LeaveAfter`() {
        val testDateTime = DateTime(2025, 3, 8, 10, 30, DateTimeZone.UTC)
        val formattedTime = testDateTime.format()
        every { resources.getString(R.string.leave) } returns "Leave"

        val testObserver = TestObserver<String>()
        mapper.toText(LeaveAfter(testDateTime)).subscribe(testObserver)

        testObserver.assertValue("Leave $formattedTime")
    }

    @Test
    fun `toText returns formatted text for ArriveBy`() {
        val testDateTime = DateTime(2025, 3, 8, 18, 45, DateTimeZone.UTC)
        val formattedTime = testDateTime.format()
        every { resources.getString(R.string.arrive) } returns "Arrive"

        val testObserver = TestObserver<String>()
        mapper.toText(ArriveBy(testDateTime)).subscribe(testObserver)

        testObserver.assertValue("Arrive $formattedTime")
    }

    @Test
    fun `should format leave after time correctly`() {
        val dateTime = DateTime(2023, 1, 1, 14, 30, DateTimeZone.UTC)
        val leaveAfter = LeaveAfter(dateTime)
        
        // Mock the string resource for "Leave"
        every { resources.getString(R.string.leave) } returns "Leave"

        val result = mapper.toText(leaveAfter).blockingGet()

        assertEquals("Leave Jan 01, 2:30 PM", result)
    }

    // Helper function to match the ViewModel's formatting
    private fun DateTime.format(): String {
        // Use SystemTimeFormatManager singleton instead of requiring Context parameter
        val timePattern = SystemTimeFormatManager.getTimeFormatPattern()
        val datePattern = "MMM dd, $timePattern"
        val simpleDateFormat = SimpleDateFormat(datePattern, Locale.US)
        simpleDateFormat.timeZone = zone.toTimeZone()
        return simpleDateFormat.format(Date(millis))
    }
}
