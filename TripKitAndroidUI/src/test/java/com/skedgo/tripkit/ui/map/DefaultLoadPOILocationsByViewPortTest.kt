package com.skedgo.tripkit.ui.map

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.skedgo.tripkit.common.model.stop.ScheduledStop
import com.skedgo.tripkit.data.database.locations.bikepods.BikePodLocationEntity
import com.skedgo.tripkit.data.database.locations.facility.FacilityLocationEntity
import com.skedgo.tripkit.data.database.locations.freefloating.FreeFloatingLocationEntity
import com.skedgo.tripkit.locations.CarPod
import com.skedgo.tripkit.parkingspots.models.OffStreetParking
import com.skedgo.tripkit.ui.base.MockKTest
import com.skedgo.tripkit.ui.map.adapter.StopInfoWindowAdapter
import com.skedgo.tripkit.ui.map.home.ViewPort
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.reactivex.Observable
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class DefaultLoadPOILocationsByViewPortTest: MockKTest() {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    // Mock dependencies
    private val stopInfoWindowAdapter: StopInfoWindowAdapter = mockk()
    private val loadStopsByViewPort: LoadStopsByViewPort = mockk()
    private val loadBikePodsByViewPort: LoadBikePodsByViewPort = mockk()
    private val loadFreeFloatingVehiclesByViewPort: LoadFreeFloatingVehiclesByViewPort = mockk()
    private val loadCarPodByViewPort: LoadCarPodByViewPort = mockk()
    private val loadFacilitiesByViewPort: LoadFacilitiesByViewPort = mockk()
    private val loadCarParksByViewPort: LoadCarParksByViewPort = mockk()

    private lateinit var defaultLoadPOILocationsByViewPort: DefaultLoadPOILocationsByViewPort

    @Before
    fun setUp() {
        initRx()
        defaultLoadPOILocationsByViewPort = DefaultLoadPOILocationsByViewPort(
            stopInfoWindowAdapter,
            loadStopsByViewPort,
            loadBikePodsByViewPort,
            loadFreeFloatingVehiclesByViewPort,
            loadCarPodByViewPort,
            loadFacilitiesByViewPort,
            loadCarParksByViewPort
        )
    }

    @After
    fun teardown() {
        tearDownRx()
    }

    @Test
    fun `execute returns combined list of POI locations`() {
        // Arrange: Mock data
        val viewPort = mockk<ViewPort>()

        // Mock entities
        val bikePodEntity = mockk<BikePodLocationEntity>(relaxed = true)
        val freeFloatingEntity = mockk<FreeFloatingLocationEntity>(relaxed = true)
        val stopEntity = mockk<ScheduledStop>(relaxed = true)
        val carPodEntity = mockk<CarPod>(relaxed = true)
        val facilityEntity = mockk<FacilityLocationEntity>(relaxed = true)
        val carParkEntity = mockk<OffStreetParking>(relaxed = true)

        // Ensure each mock entity returns a valid identifier (fixes NullPointerException)
        every { bikePodEntity.identifier } returns "bikePod_1"
        every { freeFloatingEntity.identifier } returns "freeFloating_1"
        every { stopEntity.id } returns "stop_1"
        every { carPodEntity.id } returns "carPod_1"
        every { facilityEntity.identifier } returns "facility_1"
        every { carParkEntity.id } returns "carPark_1"

        // Mock lists returned by data sources
        every { loadBikePodsByViewPort.execute(viewPort) } returns Observable.just(listOf(bikePodEntity))
        every { loadFreeFloatingVehiclesByViewPort.execute(viewPort) } returns Observable.just(listOf(freeFloatingEntity))
        every { loadStopsByViewPort.execute(viewPort) } returns Observable.just(listOf(stopEntity))
        every { loadCarPodByViewPort.execute(viewPort) } returns Observable.just(listOf(carPodEntity))
        every { loadFacilitiesByViewPort.execute(viewPort) } returns Observable.just(listOf(facilityEntity))
        every { loadCarParksByViewPort.execute(viewPort) } returns Observable.just(listOf(carParkEntity))

        // Act
        val testObserver = defaultLoadPOILocationsByViewPort.execute(viewPort).test()

        // Assert: Verify the final combined list
        testObserver.assertComplete()
        testObserver.assertValue { result ->
            result.size == 6 // One POI from each category
                && result.any { it is BikePodPOILocation }
                && result.any { it is FreeFloatingVehiclePOILocation }
                && result.any { it is StopPOILocation }
                && result.any { it is CarPodPOILocation }
                && result.any { it is FacilityPOILocation }
                && result.any { it is CarParkPOILocation }
        }

        // Verify that each data source was called once
        verify(exactly = 1) { loadBikePodsByViewPort.execute(viewPort) }
        verify(exactly = 1) { loadFreeFloatingVehiclesByViewPort.execute(viewPort) }
        verify(exactly = 1) { loadStopsByViewPort.execute(viewPort) }
        verify(exactly = 1) { loadCarPodByViewPort.execute(viewPort) }
        verify(exactly = 1) { loadFacilitiesByViewPort.execute(viewPort) }
        verify(exactly = 1) { loadCarParksByViewPort.execute(viewPort) }
    }

}