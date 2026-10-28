package com.skedgo.tripkit.ui.timetables

import android.content.Context
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.core.app.ApplicationProvider
import com.skedgo.TripKit
import com.skedgo.tripkit.DefaultTripPreferences
import com.skedgo.tripkit.common.model.TransportMode
import com.skedgo.tripkit.ui.tripresults.PrefsBasedTransportViewFilter
import com.skedgo.tripkit.routing.RealTimeVehicle
import com.skedgo.tripkit.ui.trip.details.viewmodel.OccupancyViewModel
import com.google.gson.Gson
import com.skedgo.tripkit.ui.R
import com.skedgo.tripkit.ui.base.MockKTest
import com.skedgo.tripkit.ui.model.TimetableEntry
import com.skedgo.tripkit.ui.trip.details.viewmodel.ServiceAlertViewModel
import com.skedgo.tripkit.ui.utils.PREF_KEY_TRANSPORT_MODE
import io.mockk.*
import org.joda.time.DateTimeZone
import org.junit.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class ServiceViewModelImplTest : MockKTest() {
    @get:Rule val rule = InstantTaskExecutorRule()
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val preferences = DefaultTripPreferences(context.getSharedPreferences("TripPreferences", 0))
    private lateinit var viewModel: ServiceViewModelImpl

    @Before fun setup() {
        initRx()
        context.getSharedPreferences("TripPreferences", 0).edit().clear().commit()
        context.getSharedPreferences(PREF_KEY_TRANSPORT_MODE, 0).edit().clear().commit()
        mockkObject(TripKit.Companion)
        every { TripKit.getInstance() } returns mockk(relaxed = true)
        val realtime = mockk<GetRealtimeText>()
        every { realtime.getWithIsOnTime(any(), any(), any()) } returns Triple("Scheduled", R.color.black1, false)
        viewModel = ServiceViewModelImpl(context, OccupancyViewModel(context),
            ServiceAlertViewModel(context), mockk(relaxed = true), mockk(relaxed = true),
            realtime, mockk(relaxed = true))
    }

    @After fun teardown() {
        viewModel.onCleared()
        unmockkAll()
        tearDownRx()
    }

    private fun bind(accessible: Boolean?, bicycle: Boolean? = true) {
        viewModel.setService("", TimetableEntry().apply {
            wheelchairAccessible = accessible
            bicycleAccessible = bicycle
            frequency = 600
        }, DateTimeZone.UTC)
    }

    @Test fun `refresh and recycled rows clear occupancy when own vehicle disappears`() {
        val entry = TimetableEntry().apply {
            frequency = 600
            realtimeVehicle = Gson().fromJson(
                """{"components":[[{"occupancy":"MANY_SEATS_AVAILABLE"}]]}""",
                RealTimeVehicle::class.java)
        }
        viewModel.setService("", entry, DateTimeZone.UTC)
        Assert.assertEquals(true, viewModel.showOccupancyInfo.value)
        entry.realtimeVehicle = null
        viewModel.setService("", entry, DateTimeZone.UTC)
        Assert.assertEquals(false, viewModel.showOccupancyInfo.value)
        Assert.assertFalse(viewModel.occupancyViewModel.hasInformation())
        bind(null)
        Assert.assertEquals(false, viewModel.showOccupancyInfo.value)
    }

    @Test fun `recycled timetable rows obey all wheelchair information states`() {
        for (information in listOf(true, false)) {
            preferences.setWheelchairPreferred(information)
            for (status in listOf(true, false, null)) {
                bind(status)
                Assert.assertEquals("information=$information status=$status",
                    status == false || information, viewModel.wheelchairIcon.value != null)
            }
        }
    }

    @Test fun `active unknown timetable row follows wheelchair mode without losing bicycle information`() {
        val filter = PrefsBasedTransportViewFilter(context)
        bind(null)
        Assert.assertNull(viewModel.wheelchairIcon.value)
        filter.setSelected(TransportMode.ID_WHEEL_CHAIR, true)
        Assert.assertNotNull(viewModel.wheelchairIcon.value)
        Assert.assertEquals(true, viewModel.showBicycleAccessible.value)
        Assert.assertTrue(preferences.isWheelchairPreferred())
        filter.setSelected(TransportMode.ID_WHEEL_CHAIR, false)
        Assert.assertNull(viewModel.wheelchairIcon.value)
        Assert.assertEquals(true, viewModel.showBicycleAccessible.value)
        bind(false)
        Assert.assertNotNull(viewModel.wheelchairIcon.value)
    }

    @Test fun `active timetable row refreshes icons on preference changes`() {
        bind(null)
        Assert.assertNull(viewModel.wheelchairIcon.value)
        Assert.assertEquals(true, viewModel.showBicycleAccessible.value)
        preferences.setWheelchairPreferred(true)
        Assert.assertNotNull(viewModel.wheelchairIcon.value)
        context.getSharedPreferences(PREF_KEY_TRANSPORT_MODE, 0).edit().putBoolean("cy_bic", false).commit()
        Assert.assertEquals(false, viewModel.showBicycleAccessible.value)
        preferences.setWheelchairPreferred(false)
        Assert.assertNull(viewModel.wheelchairIcon.value)
        bind(false, false)
        Assert.assertNotNull(viewModel.wheelchairIcon.value)
        Assert.assertEquals(false, viewModel.showBicycleAccessible.value)
    }
}
