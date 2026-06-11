package com.skedgo.tripkit.ui.tripresult

import android.content.Context
import android.content.res.Resources
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.core.content.ContextCompat
import com.skedgo.tripkit.common.model.realtimealert.RealtimeAlert
import com.skedgo.tripkit.datetime.PrintTime
import com.skedgo.tripkit.routing.Occupancy
import com.skedgo.tripkit.routing.RealTimeVehicle
import com.skedgo.tripkit.routing.TripSegment
import com.skedgo.tripkit.ui.R
import com.skedgo.tripkit.ui.base.MockKTest
import com.skedgo.tripkit.ui.trip.details.viewmodel.OccupancyViewModel
import com.skedgo.tripkit.ui.tripresults.GetTransportIconTintStrategy
import com.skedgo.tripkit.ui.tripresults.TripSegmentHelper
import com.skedgo.tripkit.ui.utils.TransportModeSharedPreference
import io.mockk.MockKAnnotations
import io.mockk.Runs
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.spyk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner

@RunWith(MockitoJUnitRunner::class)
class TripSegmentItemViewModelTest : MockKTest() {

    private lateinit var context: Context

    @MockK
    private lateinit var occupancyViewModel: OccupancyViewModel

    @MockK
    private lateinit var getTransportIconTintStrategy: GetTransportIconTintStrategy

    @MockK
    private lateinit var tripSegmentHelper: TripSegmentHelper

    @MockK
    private lateinit var printTime: PrintTime

    @MockK(relaxed = true)
    private lateinit var transportModeSharedPreference: TransportModeSharedPreference

    private lateinit var viewModel: TripSegmentItemViewModel

    @get:Rule
    val rule = InstantTaskExecutorRule()

    private val mockTripSegment = mockk<TripSegment>(relaxed = true)

    @Before
    fun setup() {
        MockKAnnotations.init(this, relaxUnitFun = true)

        val mockResources = mockk<Resources>().apply {
            every { getColor(any()) } returns mockk()
        }

        context = mockk<Context>().apply {
            every { resources } returns mockResources
            every { getColor(any()) } returns mockk()
        }

        every { mockTripSegment.realTimeVehicle = any() } just Runs

        viewModel = TripSegmentItemViewModel(
            context,
            getTransportIconTintStrategy,
            tripSegmentHelper,
            printTime,
            occupancyViewModel,
            transportModeSharedPreference
        )

    }

    @Test
    fun `initOccupancy - should call setOccupancy if TripSegment realTimeVehicle is not null`() {
        val realTimeVehicle = mockk<RealTimeVehicle>()
        mockTripSegment.realTimeVehicle = realTimeVehicle
        viewModel.initOccupancy(mockTripSegment)

        verify { occupancyViewModel.setOccupancy(mockTripSegment.realTimeVehicle!!, any()) }
    }

    @Test
    fun `initOccupancy - should not call setOccupancy if TripSegment realTimeVehicle is null`() {
        every { mockTripSegment.realTimeVehicle } returns null
        viewModel.initOccupancy(mockTripSegment)
        verify(exactly = 0) { occupancyViewModel.setOccupancy(any(), any()) }
    }

    @Test
    fun `updateAlertStateForViewType - shows deduplicated alerts for moving segment`() {
        val firstAlert = mockk<RealtimeAlert>()
        val duplicateFirstAlert = mockk<RealtimeAlert>()
        val secondAlert = mockk<RealtimeAlert>()
        every { firstAlert.title() } returns "Weekday track closure"
        every { duplicateFirstAlert.title() } returns "Weekday track closure"
        every { secondAlert.title() } returns "Other disruption"

        viewModel.updateAlertStateForViewType(
            TripSegmentItemViewModel.SegmentViewType.MOVING,
            listOf(firstAlert, duplicateFirstAlert, secondAlert)
        )

        assertTrue(viewModel.showAlerts.value == true)
        assertEquals(2, viewModel.alerts.value?.size)
        assertEquals(firstAlert, viewModel.alerts.value?.first())
        assertEquals(secondAlert, viewModel.alerts.value?.get(1))
    }

    @Test
    fun `updateAlertStateForViewType - hides alerts for stationary bridge segment`() {
        val alert = mockk<RealtimeAlert>()
        every { alert.title() } returns "Weekday track closure"

        viewModel.updateAlertStateForViewType(
            TripSegmentItemViewModel.SegmentViewType.STATIONARY_BRIDGE,
            listOf(alert)
        )

        assertFalse(viewModel.showAlerts.value == true)
        assertTrue(viewModel.alerts.value?.isEmpty() == true)
    }

}