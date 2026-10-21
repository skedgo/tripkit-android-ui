package com.skedgo.tripkit.ui.tripresults

import android.content.Context
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.core.app.ApplicationProvider
import com.skedgo.TripKit
import com.skedgo.tripkit.common.model.location.Location
import com.skedgo.tripkit.datetime.PrintTime
import com.skedgo.tripkit.routing.Trip
import com.skedgo.tripkit.routing.TripGroup
import com.skedgo.tripkit.routing.TripSegment
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkAll
import org.joda.time.DateTime
import org.joda.time.DateTimeZone
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], qualifiers = "en-rAU")
class TripResultTimeBindingTest {
    @get:Rule val instantTaskExecutorRule = InstantTaskExecutorRule()
    private lateinit var context: Context
    private lateinit var viewModel: TripResultViewModel

    @Before fun setup() {
        context = ApplicationProvider.getApplicationContext()
        net.danlew.android.joda.JodaTimeAndroid.init(context)
        mockkObject(TripKit.Companion)
        mockkObject(com.skedgo.tripkit.ui.TripKitUI.Companion)
        every { com.skedgo.tripkit.ui.TripKitUI.getInstance() } returns mockk(relaxed = true)
        every { TripKit.getInstance() } returns mockk(relaxed = true)
        val printTime = mockk<PrintTime>()
        every { printTime.printLocalTime(any()) } answers { firstArg<org.joda.time.LocalTime>().toString("HH:mm") }
        viewModel = TripResultViewModel(context, mockk(relaxed = true), printTime,
            context.resources, mockk(relaxed = true))
    }

    @After fun teardown() = unmockkAll()

    private fun trip(departure: String, query: String = "2026-09-26T06:36", zone: String = "Australia/Darwin"): Trip {
        val tz = DateTimeZone.forID(zone)
        val start = DateTime.parse(departure).withZoneRetainFields(tz)
        return Trip().apply {
            uuid = departure
            tripId = start.millis
            startTimeInSecs = start.millis / 1000
            endTimeInSecs = start.plusMinutes(52).millis / 1000
            queryTime = DateTime.parse(query).withZoneRetainFields(tz).millis / 1000
            queryIsLeaveAfter = true
            segmentList = arrayListOf(TripSegment().apply {
                serviceTripId = "450"
                from = Location().apply { timeZone = zone }
                to = Location().apply { timeZone = zone }
            })
        }
    }

    private fun bind(vararg trips: Trip): List<TripResultTripViewModel> {
        val group = TripGroup().apply { trips.forEach { addTrip(it) } }
        viewModel.setTripGroup(context, group, TripGroupClassifier.Classification.NONE)
        return viewModel.tripResults.value!!
    }

    @Test fun `same day and overnight arrival retain time only title`() {
        val row = bind(trip("2026-09-26T07:03")).single()
        assertEquals("07:03 - 07:55", row.title.value)
        assertNull(row.departureDayLabel.value)
        assertEquals("23:40 - 00:32", bind(trip("2026-09-26T23:40")).single().title.value)
    }

    @Test fun `later departures in a group each show their departure weekday`() {
        val rows = bind(trip("2026-09-28T07:03"), trip("2026-09-29T07:03"), trip("2026-09-30T07:03"))
        assertEquals("Mon, 07:03 - 07:55", rows[0].title.value)
        assertEquals("Tue, 07:03 - 07:55", rows[1].title.value)
        assertEquals(listOf("+2 days", "+3 days"), rows.map { it.departureDayLabel.value })
        viewModel.toggleShowMore()
        assertEquals(listOf("Mon, 07:03 - 07:55", "Tue, 07:03 - 07:55", "Wed, 07:03 - 07:55"),
            viewModel.tripResults.value!!.map { it.title.value })
        assertEquals(listOf("+2 days", "+3 days", "+4 days"), viewModel.tripResults.value!!.map { it.departureDayLabel.value })
    }

    @Test fun `next day across year boundary shows weekday`() {
        val row = bind(trip("2027-01-01T07:03", "2026-12-31T23:36")).single()
        assertEquals("Fri, 07:03 - 07:55", row.title.value)
        assertEquals("+1 day", row.departureDayLabel.value)
    }

    @Test fun `month boundary uses calendar days`() {
        assertEquals("+2 days", bind(trip("2026-10-01T07:03", "2026-09-29T06:36")).single().departureDayLabel.value)
    }

    @Test fun `spring and autumn DST days are not elapsed 24 hour intervals`() {
        val spring = bind(trip("2026-03-09T00:10", "2026-03-07T23:50", "America/New_York")).single()
        assertEquals("Mon, 00:10 - 01:02", spring.title.value)
        assertEquals("+2 days", spring.departureDayLabel.value)
        val autumn = bind(trip("2026-11-02T00:10", "2026-10-31T23:50", "America/New_York")).single()
        assertEquals("+2 days", autumn.departureDayLabel.value)
    }

    @Test fun `origin calendar is used even when device date differs`() {
        val previous = DateTimeZone.getDefault()
        try {
            DateTimeZone.setDefault(DateTimeZone.forID("America/Los_Angeles"))
            val row = bind(trip("2026-09-27T00:10", "2026-09-26T23:50")).single()
            assertEquals("Sun, 00:10 - 01:02", row.title.value)
            assertEquals("+1 day", row.departureDayLabel.value)
        } finally { DateTimeZone.setDefault(previous) }
    }

    @Test fun `arrival by and missing query or origin timezone keep original presentation`() {
        val arrivalBy = trip("2026-09-28T07:03").apply { queryIsLeaveAfter = false }
        val missingQuery = trip("2026-09-28T07:03").apply { queryTime = 0 }
        val missingZone = trip("2026-09-28T07:03").apply { from!!.timeZone = null }
        listOf(arrivalBy, missingQuery).forEach {
            val row = bind(it).single()
            assertEquals("07:03 - 07:55", row.title.value)
            assertNull(row.departureDayLabel.value)
        }
        val noZoneRow = bind(missingZone).single()
        assertFalse(noZoneRow.title.value!!.contains(","))
        assertNull(noZoneRow.departureDayLabel.value)
    }

    @Test fun `requested date refresh changes labels and result diff`() {
        val t = trip("2026-09-28T07:03")
        val group = TripGroup().apply { addTrip(t) }
        viewModel.setTripGroup(context, group, null)
        val previous = viewModel
        val printTime = mockk<PrintTime>()
        every { printTime.printLocalTime(any()) } answers { firstArg<org.joda.time.LocalTime>().toString("HH:mm") }
        viewModel = TripResultViewModel(context, mockk(relaxed = true), printTime, context.resources, mockk(relaxed = true))
        t.queryTime = DateTime.parse("2026-09-28T06:36+09:30").millis / 1000
        viewModel.setTripGroup(context, group, null)
        assertNull(viewModel.tripResults.value!!.single().departureDayLabel.value)
        assertEquals("07:03 - 07:55", viewModel.tripResults.value!!.single().title.value)
        assertFalse(GroupDiffCallback.areContentsTheSame(previous, viewModel))
    }

    @Test fun `production binding clears date badge when rebound and retains click emission`() {
        val themed = android.view.ContextThemeWrapper(context, com.google.android.material.R.style.Theme_MaterialComponents_DayNight_NoActionBar)
        val binding = com.skedgo.tripkit.ui.databinding.TripResultListItemTripBinding.inflate(android.view.LayoutInflater.from(themed))
        val owner = object : androidx.lifecycle.LifecycleOwner {
            val registry = androidx.lifecycle.LifecycleRegistry(this)
            override val lifecycle: androidx.lifecycle.Lifecycle get() = registry
        }
        owner.registry.currentState = androidx.lifecycle.Lifecycle.State.RESUMED
        binding.lifecycleOwner = owner
        val laterTrip = trip("2026-09-28T07:03")
        val row = bind(laterTrip).single()
        val flow = kotlinx.coroutines.flow.MutableSharedFlow<Trip>(replay = 1)
        row.clickFlow = flow
        binding.viewModel = row
        binding.executePendingBindings()
        assertEquals("+2 days", binding.departureDayLabel.text.toString())
        assertEquals(android.view.View.VISIBLE, binding.departureDayLabel.visibility)
        binding.root.performClick()
        org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()
        assertEquals(laterTrip, flow.replayCache.single())
        binding.viewModel = bind(trip("2026-09-26T07:03")).single()
        binding.executePendingBindings()
        assertEquals(android.view.View.GONE, binding.departureDayLabel.visibility)
        assertEquals("07:03 - 07:55", binding.title.text.toString())
    }

    @Test fun `duration style later departure also carries weekday and day offset`() {
        val flexibleTrip = trip("2026-09-28T07:03").apply {
            segmentList.first().serviceTripId = null
        }
        val row = bind(flexibleTrip).single()
        org.junit.Assert.assertTrue(row.title.value!!.startsWith("Mon, "))
        assertEquals("+2 days", row.departureDayLabel.value)
    }
}
