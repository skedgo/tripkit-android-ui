package com.skedgo.tripkit.ui.map

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.skedgo.tripkit.data.database.locations.carpods.CarPodRepository
import com.skedgo.tripkit.location.GeoPoint
import com.skedgo.tripkit.locations.CarPod
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
class LoadCarPodByViewPortTest: MockKTest() {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private lateinit var carPodRepository: CarPodRepository
    private lateinit var getCellIdsFromViewPort: GetCellIdsFromViewPort
    private lateinit var loadCarPodByViewPort: LoadCarPodByViewPort

    @Before
    fun setup() {
        initRx()
        carPodRepository = mockk()
        getCellIdsFromViewPort = mockk()
        loadCarPodByViewPort = LoadCarPodByViewPort(carPodRepository, getCellIdsFromViewPort)
    }

    @After
    fun teardown() {
        tearDownRx()
    }

    @Test
    fun `execute should return car pods when viewport is inner`() {
        // Mock ViewPort
        val viewPort = mockk<ViewPort>(relaxed = true)
        val visibleBounds = mockk<LatLngBounds>(relaxed = true)
        val southwest = GeoPoint(10.0, 20.0)
        val northeast = GeoPoint(30.0, 40.0)
        val cellIds = listOf("cell1", "cell2")
        val carPods = listOf(mockk<CarPod>(), mockk())

        // Define behavior for mocks
        every { viewPort.isInner() } returns true
        every { viewPort.visibleBounds } returns visibleBounds
        every { visibleBounds.southwest } returns LatLng(10.0, 20.0)
        every { visibleBounds.northeast } returns LatLng(30.0, 40.0)

        every { getCellIdsFromViewPort.execute(viewPort) } returns Observable.just(cellIds)
        every {
            carPodRepository.getCarPodsByCellIdsWithinBounds(
                cellIds = cellIds,
                southwest = southwest,
                northEast = northeast
            )
        } returns Observable.just(carPods)

        // Execute and test
        val testObserver = loadCarPodByViewPort.execute(viewPort).test()

        testObserver.assertComplete()
        testObserver.assertValue(carPods)
    }

    @Test
    fun `execute should return empty list when viewport is not inner`() {
        // Mock viewport
        val viewPort = mockk<ViewPort.NotCloseEnough>()

        // Define behavior for mocks
        every { viewPort.isInner() } returns false

        // Execute and test
        val testObserver = loadCarPodByViewPort.execute(viewPort).test()

        testObserver.assertComplete()
        testObserver.assertValue(emptyList())
    }
}