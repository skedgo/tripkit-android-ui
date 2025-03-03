package com.skedgo.tripkit.ui.map.servicestop

import android.content.Context
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.google.android.gms.maps.model.MarkerOptions
import com.jakewharton.rxrelay2.PublishRelay
import com.skedgo.tripkit.common.model.realtimealert.RealTimeStatus
import com.skedgo.tripkit.common.model.region.Region
import com.skedgo.tripkit.common.model.stop.ScheduledStop
import com.skedgo.tripkit.data.regions.RegionService
import com.skedgo.tripkit.routing.RealTimeVehicle
import com.skedgo.tripkit.ui.base.MockKTest
import com.skedgo.tripkit.ui.model.StopInfo
import com.skedgo.tripkit.ui.model.TimetableEntry
import com.skedgo.tripkit.ui.realtime.RealTimeChoreographerViewModel
import com.skedgo.tripkit.ui.servicedetail.FetchAndLoadServices
import com.skedgo.tripkit.ui.servicedetail.GetStopDisplayText
import com.skedgo.tripkit.ui.utils.ServiceLineOverlayTask
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import io.reactivex.Observable
import io.reactivex.Single
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class ServiceStopMapViewModelTest: MockKTest() {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private lateinit var viewModel: ServiceStopMapViewModel

    @MockK(relaxed = true)
    private lateinit var context: Context

    @MockK(relaxed = true)
    private lateinit var fetchAndLoadServices: FetchAndLoadServices

    @MockK(relaxed = true)
    private lateinit var regionService: RegionService

    @MockK(relaxed = true)
    private lateinit var getStopDisplayText: GetStopDisplayText

    @MockK(relaxed = true)
    private lateinit var realtimeViewModel: RealTimeChoreographerViewModel

    @MockK(relaxed = true)
    private lateinit var serviceStopMarkerCreator: ServiceStopMarkerCreator

    @Before
    fun setUp() {
        initRx()
        MockKAnnotations.init(this, relaxed = true)
        viewModel = ServiceStopMapViewModel(
            context,
            fetchAndLoadServices,
            regionService,
            getStopDisplayText
        )
        viewModel.realtimeViewModel = realtimeViewModel
        viewModel.serviceStopMarkerCreator = serviceStopMarkerCreator
    }

    @After
    fun teardown() {
        tearDownRx()
    }

    @Test
    fun `test stopRealtimeUpdates triggers relay`() {
        val relay = spyk(PublishRelay.create<Unit>())
        val relayField = ServiceStopMapViewModel::class.java.getDeclaredField("stopRealtimeRelay")
        relayField.isAccessible = true
        relayField.set(viewModel, relay)

        viewModel.stopRealtimeUpdates()

        verify { relay.accept(Unit) }
    }

    @Test
    fun `test getStopForService returns correct stop`() {
        val childStop = ScheduledStop().apply { code = "123" }
        val parentStop = ScheduledStop().apply {
            code = "456"
            children = listOf(childStop)
        }
        val service = TimetableEntry().apply { stopCode = "123" }

        val result = viewModel.getStopForService(parentStop, service)

        assertEquals(childStop, result)
    }

    @Test
    fun `test drawStops transformation`() {
        val stopInfo = mockk<StopInfo>(relaxed = true)
        val markerOptions = mockk<MarkerOptions>(relaxed = true)
        val region = mockk<Region>(relaxed = true)
        val stop = mockk<ScheduledStop>(relaxed = true)
        val service = mockk<ServiceLineOverlayTask.ServiceLineInfo>(relaxed = true)
        val result = Pair(listOf(stopInfo), listOf(service))
        every { fetchAndLoadServices.load(any(), any()) } returns
            Single.just(result)

        every { getStopDisplayText.execute(any()) } returns Observable.just("Stop Name")
        every {
            serviceStopMarkerCreator.toMarkerOptions(
                any(),
                any(),
                any()
            )
        } returns markerOptions
        every { regionService.getRegionByLocationAsync(any()) } returns Observable.just(region)

        val testObserver = viewModel.drawStops.test()
        testObserver.assertNoErrors()
    }

    @Test
    fun `test realtimeVehicle observable`() {
        val service = mockk<TimetableEntry>(relaxed = true)
        val realTimeVehicle = mockk<RealTimeVehicle>(relaxed = true)

        every { service.realTimeStatus } returns RealTimeStatus.IS_REAL_TIME
        every { realtimeViewModel.realTimeVehicleObservable(service) } returns Observable.just(
            realTimeVehicle
        )

        viewModel.service.accept(service)

        val testObserver = viewModel.realtimeVehicle.test()
        testObserver.assertNoErrors()
        verify { realtimeViewModel.realTimeVehicleObservable(service) }
    }
}
