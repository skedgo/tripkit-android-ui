package com.skedgo.tripkit.ui.trip.options

import android.content.res.Resources
import com.skedgo.tripkit.ui.R
import com.skedgo.tripkit.ui.trip.ArriveBy
import com.skedgo.tripkit.ui.trip.LeaveAfter
import com.skedgo.tripkit.ui.trip.Now
import io.mockk.every
import io.mockk.mockk
import io.reactivex.Single
import io.reactivex.observers.TestObserver
import org.joda.time.DateTime
import org.joda.time.DateTimeZone
import org.junit.Before
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.*

class RoutingTimeViewModelMapperTest {

    private lateinit var mapper: RoutingTimeViewModelMapper
    private val resources: Resources = mockk()

    @Before
    fun setUp() {
        mapper = RoutingTimeViewModelMapper(resources)
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

    // Helper function to match the ViewModel's formatting
    private fun DateTime.format(): String {
        val simpleDateFormat = SimpleDateFormat("MMM dd, h:mm a", Locale.US)
        simpleDateFormat.timeZone = zone.toTimeZone()
        return simpleDateFormat.format(Date(millis))
    }
}
