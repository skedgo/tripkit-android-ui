package com.skedgo.tripkit.ui.realtime

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.skedgo.tripkit.common.agenda.IRealTimeElement
import com.skedgo.tripkit.common.model.region.Region
import com.skedgo.tripkit.routing.RealTimeVehicle
import com.skedgo.tripkit.ui.base.MockKTest
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.reactivex.Observable
import io.reactivex.schedulers.Schedulers
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class RealTimeChoreographerViewModelTest : MockKTest() {

    @get:Rule
    val rule = InstantTaskExecutorRule()

    private lateinit var viewModel: RealTimeChoreographerViewModel
    private val realTimeChoreographer: RealTimeChoreographer = mockk()

    @Before
    fun setUp() {
        initRx()
        viewModel = RealTimeChoreographerViewModel(realTimeChoreographer)
    }

    @After
    fun tearDown() {
        tearDownRx()
        clearAllMocks()
    }

    @Test
    fun `getRealTimeVehicles should emit real-time vehicle updates`() {
        val region = mockk<Region>(relaxed = true)
        val elements = listOf(mockk<IRealTimeElement>(relaxed = true))
        val vehicles = listOf(mockk<RealTimeVehicle>(relaxed = true))

        every {
            realTimeChoreographer.getRealTimeResultsFromCleanElements(
                region,
                elements
            )
        } returns Observable.just(vehicles)

        val testObserver = viewModel.getRealTimeVehicles(region, elements).test()

        testObserver.assertValue(vehicles)
        testObserver.assertComplete()
        testObserver.dispose()
    }

    @Test
    fun `realTimeVehicleObservable should emit correct vehicle`() {
        val service = mockk<IRealTimeElement>(relaxed = true)
        every { service.serviceTripId } returns "1"

        val vehicle1 =
            mockk<RealTimeVehicle>(relaxed = true) { every { serviceTripId } returns "1" }
        val vehicle2 =
            mockk<RealTimeVehicle>(relaxed = true) { every { serviceTripId } returns "2" }
        val vehicles = listOf(vehicle1, vehicle2)
        val region = mockk<Region>(relaxed = true)

        every {
            realTimeChoreographer.getRealTimeResultsFromCleanElements(
                region,
                listOf(service)
            )
        } returns Observable.just(vehicles)

        // Subscribe to realTimeVehicleObservable BEFORE triggering getRealTimeVehicles
        val testObserver = viewModel.realTimeVehicleObservable(service).test()

        // Now trigger the emission
        viewModel.getRealTimeVehicles(region, listOf(service))
            .subscribeOn(Schedulers.trampoline())
            .subscribe()

        testObserver.assertValue(vehicle1) // Ensure correct vehicle is emitted
        testObserver.assertNoErrors()
        testObserver.dispose()
    }
}