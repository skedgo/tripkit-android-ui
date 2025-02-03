package com.skedgo.tripkit.ui.map

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.skedgo.tripkit.data.database.locations.freefloating.FreeFloatingLocationEntity
import com.skedgo.tripkit.data.database.locations.freefloating.FreeFloatingRepository
import com.skedgo.tripkit.location.GeoPoint
import com.skedgo.tripkit.ui.base.MockKTest
import com.skedgo.tripkit.ui.data.places.LatLng
import com.skedgo.tripkit.ui.data.places.LatLngBounds
import com.skedgo.tripkit.ui.map.home.GetCellIdsFromViewPort
import com.skedgo.tripkit.ui.map.home.ViewPort
import io.mockk.every
import io.mockk.mockk
import io.reactivex.Observable
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class LoadFreeFloatingVehiclesByViewPortTest: MockKTest() {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private lateinit var freeFloatingRepository: FreeFloatingRepository
    private lateinit var getCellIdsFromViewPort: GetCellIdsFromViewPort
    private lateinit var loadFreeFloatingVehiclesByViewPort: LoadFreeFloatingVehiclesByViewPort

    @Before
    fun setup() {
        initRx()
        freeFloatingRepository = mockk()
        getCellIdsFromViewPort = mockk()
        loadFreeFloatingVehiclesByViewPort = LoadFreeFloatingVehiclesByViewPort(freeFloatingRepository, getCellIdsFromViewPort)
    }

    @After
    fun teardown() {
        tearDownRx()
    }

    @Test
    fun `execute should return free floating locations when viewport is close enough`() {
        // Mock ViewPort
        val viewPort = mockk<ViewPort.CloseEnough>(relaxed = true)
        val visibleBounds = mockk<LatLngBounds>(relaxed = true)
        val southwest = GeoPoint(10.0, 20.0)
        val northeast = GeoPoint(30.0, 40.0)
        val cellIds = listOf("cell1", "cell2")
        val freeFloatingLocations = listOf(mockk<FreeFloatingLocationEntity>(), mockk())

        // Define behavior for mocks
        every { viewPort.visibleBounds } returns visibleBounds
        every { visibleBounds.southwest } returns LatLng(10.0, 20.0)
        every { visibleBounds.northeast } returns LatLng(30.0, 40.0)

        every { getCellIdsFromViewPort.execute(viewPort) } returns Observable.just(cellIds)
        every {
            freeFloatingRepository.getFreeFloatingLocationsWithinBounds(
                cellIds = cellIds,
                southwest = southwest,
                northEast = northeast
            )
        } returns Observable.just(freeFloatingLocations)

        // Execute and test
        val testObserver = loadFreeFloatingVehiclesByViewPort.execute(viewPort).test()

        testObserver.assertComplete()
        testObserver.assertValue(freeFloatingLocations)
    }

    @Test
    fun `execute should return empty list when viewport is not close enough`() {
        // Mock ViewPort
        val viewPort = mockk<ViewPort.NotCloseEnough>()

        // Define behavior for mocks
        every { viewPort.isInner() } returns false

        // Execute and test
        val testObserver = loadFreeFloatingVehiclesByViewPort.execute(viewPort).test()

        testObserver.assertComplete()
        testObserver.assertValue(emptyList())
    }
}