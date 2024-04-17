package com.skedgo.tripkit.ui.tripresult

import android.content.Context
import android.content.res.Resources
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.core.content.ContextCompat
import com.skedgo.tripkit.datetime.PrintTime
import com.skedgo.tripkit.routing.Occupancy
import com.skedgo.tripkit.routing.RealTimeVehicle
import com.skedgo.tripkit.routing.TripSegment
import com.skedgo.tripkit.ui.R
import com.skedgo.tripkit.ui.base.MockKTest
import com.skedgo.tripkit.ui.trip.details.viewmodel.OccupancyViewModel
import com.skedgo.tripkit.ui.tripresults.GetTransportIconTintStrategy
import com.skedgo.tripkit.ui.tripresults.TripSegmentHelper
import io.mockk.MockKAnnotations
import io.mockk.Runs
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.spyk
import io.mockk.verify
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner

@RunWith(MockitoJUnitRunner::class)
class TripSegmentItemViewModelTest: MockKTest() {

    private lateinit var context: Context

    @MockK
    private lateinit var occupancyViewModel: OccupancyViewModel

    @MockK
    private lateinit var getTransportIconTintStrategy: GetTransportIconTintStrategy

    @MockK
    private lateinit var tripSegmentHelper: TripSegmentHelper

    @MockK
    private lateinit var printTime: PrintTime

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
            occupancyViewModel
        )

    }

    @Test
    fun `initOccupancy - should call setOccupancy if TripSegment realTimeVehicle is not null`() {
        val realTimeVehicle = mockk<RealTimeVehicle>()
        mockTripSegment.realTimeVehicle = realTimeVehicle
        viewModel.initOccupancy(mockTripSegment)

        verify { occupancyViewModel.setOccupancy(mockTripSegment.realTimeVehicle, any()) }
    }

    @Test
    fun `initOccupancy - should not call setOccupancy if TripSegment realTimeVehicle is null`() {
        every { mockTripSegment.realTimeVehicle } returns null
        viewModel.initOccupancy(mockTripSegment)
        verify(exactly = 0) { occupancyViewModel.setOccupancy(any(), any()) }
    }

}