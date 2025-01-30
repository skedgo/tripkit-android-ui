package com.skedgo.tripkit.ui.map

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.skedgo.tripkit.parkingspots.ParkingRepository
import com.skedgo.tripkit.parkingspots.models.OffStreetParking
import com.skedgo.tripkit.ui.base.MockKTest
import com.skedgo.tripkit.ui.map.home.GetCellIdsFromViewPort
import com.skedgo.tripkit.ui.map.home.ViewPort
import io.mockk.every
import io.mockk.mockk
import io.reactivex.Observable
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class LoadCarParksByViewPortTest: MockKTest() {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private lateinit var parkingRepository: ParkingRepository
    private lateinit var getCellIdsFromViewPort: GetCellIdsFromViewPort
    private lateinit var loadCarParksByViewPort: LoadCarParksByViewPort

    @Before
    fun setup() {
        parkingRepository = mockk()
        getCellIdsFromViewPort = mockk()
        loadCarParksByViewPort = LoadCarParksByViewPort(parkingRepository, getCellIdsFromViewPort)
    }

    @Test
    fun `execute should return car parks when viewport is CloseEnough`() {
        // Mock viewport
        val viewPort = mockk<ViewPort.CloseEnough>()
        val cellIds = listOf("cell1", "cell2")
        val carParks = listOf(mockk<OffStreetParking>(), mockk())

        // Define behavior for mocks
        every { getCellIdsFromViewPort.execute(viewPort) } returns Observable.just(cellIds)
        every { parkingRepository.getByCellIds(cellIds) } returns Observable.just(carParks)

        // Execute and test
        val testObserver = loadCarParksByViewPort.execute(viewPort).test()

        testObserver.assertComplete()
        testObserver.assertValue(carParks)
    }

    @Test
    fun `execute should return empty list when viewport is not CloseEnough`() {
        // Mock different viewport type
        val viewPort = mockk<ViewPort.NotCloseEnough>()

        // Execute and test
        val testObserver = loadCarParksByViewPort.execute(viewPort).test()

        testObserver.assertComplete()
        testObserver.assertValue(emptyList())
    }
}