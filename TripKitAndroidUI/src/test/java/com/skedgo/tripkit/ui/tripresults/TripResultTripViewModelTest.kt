package com.skedgo.tripkit.ui.tripresults

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.skedgo.tripkit.routing.Trip
import com.skedgo.tripkit.routing.TripSegment
import com.skedgo.tripkit.ui.base.MockKTest
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.*
import org.amshove.kluent.internal.assertFalse
import org.junit.*
import org.junit.Assert.assertTrue
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@ExperimentalCoroutinesApi
@RunWith(JUnit4::class)
class TripResultTripViewModelTest: MockKTest() {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private lateinit var viewModel: TripResultTripViewModel

    @Before
    fun setup() {
        initDispatchers()
        viewModel = TripResultTripViewModel()
    }

    @After
    fun tearDown() {
        tearDownDispatchers()
    }

    @Test
    fun `setting trip triggers checkQuickBooking with quickBookingSegment null`() = runTest {
        val trip = mockk<Trip>(relaxed = true) {
            every { quickBookingSegment } returns null
        }

        viewModel.trip = trip

        assertFalse(viewModel.hasQuickBooking.value ?: true)
    }

    @Test
    fun `setting trip triggers checkQuickBooking with quickBookingSegment present`() = runTest {
        val quickSegment = mockk<TripSegment>()
        val trip = mockk<Trip>(relaxed = true) {
            every { quickBookingSegment } returns quickSegment
        }

        viewModel.trip = trip

        assertTrue(viewModel.hasQuickBooking.value ?: false)
    }

    @Test
    fun `onItemClicked emits trip`() = runTest {
        val quickSegment = mockk<TripSegment>()
        val trip = mockk<Trip>(relaxed = true) {
            every { quickBookingSegment } returns quickSegment
        }
        val clickFlow = spyk(MutableSharedFlow<Trip>(replay = 1))
        viewModel.trip = trip
        viewModel.clickFlow = clickFlow

        viewModel.onItemClicked()
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { clickFlow.emit(trip) }
    }

    @Test
    fun `onQuickBookingActionClicked emits quickBookingSegment`() = runTest {
        val quickSegment = mockk<TripSegment>()
        val trip = mockk<Trip>(relaxed = true) {
            every { quickBookingSegment } returns quickSegment
        }
        val quickBookingFlow = spyk(MutableSharedFlow<TripSegment>(replay = 1))

        viewModel.trip = trip
        viewModel.quickBookingActionClickFlow = quickBookingFlow

        viewModel.onQuickBookingActionClicked()
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { quickBookingFlow.emit(quickSegment) }
    }
}
