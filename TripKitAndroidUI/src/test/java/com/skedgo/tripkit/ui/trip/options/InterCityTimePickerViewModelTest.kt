package com.skedgo.tripkit.ui.trip.options

import android.content.Context
import android.os.Bundle
import com.skedgo.tripkit.common.model.time.TimeTag
import com.skedgo.tripkit.time.GetNow
import com.skedgo.tripkit.ui.R
import com.squareup.otto.Bus
import io.mockk.*
import org.joda.time.DateTime
import org.joda.time.DateTimeZone
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class InterCityTimePickerViewModelTest {

    private lateinit var viewModel: InterCityTimePickerViewModel
    private val context: Context = mockk(relaxed = true)
    private val eventBus: Bus = mockk(relaxed = true)
    private val getNow: GetNow = mockk()

    @Before
    fun setup() {
        every { getNow.execute() } returns DateTime.now(DateTimeZone.UTC)
        every { context.getString(R.string.leave_at) } returns "Leave At"
        every { context.getString(R.string.arrive_by) } returns "Arrive By"
        every { context.getString(R.string.done) } returns "Done"

        viewModel = InterCityTimePickerViewModel(context, eventBus, getNow, "UTC")
    }

    @Test
    fun `handleArguments - initializes values correctly`() {
        val bundle = mockk<Bundle>(relaxed = true)
        every { bundle.containsKey(InterCityTimePickerViewModel.ARG_TIME_IN_MILLIS) } returns true
        every { bundle.getLong(InterCityTimePickerViewModel.ARG_TIME_IN_MILLIS) } returns 1680000000000L
        every { bundle.containsKey(InterCityTimePickerViewModel.ARG_TIME_TYPE) } returns true
        every { bundle.getInt(InterCityTimePickerViewModel.ARG_TIME_TYPE) } returns TimeTag.TIME_TYPE_LEAVE_AFTER

        every { bundle.containsKey(InterCityTimePickerViewModel.ARG_DEPARTURE_TIMEZONE) } returns true
        every { bundle.getString(InterCityTimePickerViewModel.ARG_DEPARTURE_TIMEZONE) } returns "UTC"

        every { bundle.containsKey(InterCityTimePickerViewModel.ARG_ARRIVAL_TIMEZONE) } returns true
        every { bundle.getString(InterCityTimePickerViewModel.ARG_ARRIVAL_TIMEZONE) } returns "UTC"

        viewModel.handleArguments(bundle)

        assertEquals(1680000000000L, viewModel.timeMillis)
        assertTrue(viewModel.isLeaveAfter().get())
    }

    @Test
    fun `leaveNow - returns correct TimeTag`() {
        val timeTag = viewModel.leaveNow()
        assertNotNull(timeTag)
        assertEquals(TimeTag.TIME_TYPE_LEAVE_AFTER, timeTag.type)
    }

    @Test
    fun `done - returns correct TimeTag`() {
        val bundle = mockk<Bundle>(relaxed = true)

        every { bundle.containsKey(InterCityTimePickerViewModel.ARG_TIME_IN_MILLIS) } returns true
        every { bundle.getLong(InterCityTimePickerViewModel.ARG_TIME_IN_MILLIS) } returns 1680000000000L

        every { bundle.containsKey(InterCityTimePickerViewModel.ARG_DEPARTURE_TIMEZONE) } returns true
        every { bundle.getString(InterCityTimePickerViewModel.ARG_DEPARTURE_TIMEZONE) } returns "UTC"

        every { bundle.containsKey(InterCityTimePickerViewModel.ARG_ARRIVAL_TIMEZONE) } returns true
        every { bundle.getString(InterCityTimePickerViewModel.ARG_ARRIVAL_TIMEZONE) } returns "UTC"

        viewModel.handleArguments(bundle) // Ensure timezones are set before calling initValues()

        viewModel.updateTime(10, 30)
        val timeTag = viewModel.done()

        assertNotNull(timeTag)
        assertEquals(TimeTag.TIME_TYPE_LEAVE_AFTER, timeTag.type)
    }

    @Test
    fun `observable values update correctly`() {
        assertEquals("Leave At", viewModel.leaveAtLabel().get())
        assertEquals("Arrive By", viewModel.arriveByLabel().get())
        assertEquals(R.string.done, viewModel.positiveActionLabel().get())
    }
}
