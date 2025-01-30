package com.skedgo.tripkit.ui.map

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.skedgo.tripkit.data.database.locations.bikepods.BikePodLocationEntity
import com.skedgo.tripkit.data.database.locations.bikepods.BikePodRepository
import com.skedgo.tripkit.location.GeoPoint
import com.skedgo.tripkit.ui.base.MockKTest
import com.skedgo.tripkit.ui.data.places.LatLng
import com.skedgo.tripkit.ui.data.places.LatLngBounds
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
class LoadBikePodsByViewPortTest: MockKTest() {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private lateinit var bikePodRepository: BikePodRepository
    private lateinit var getCellIdsFromViewPort: GetCellIdsFromViewPort
    private lateinit var loadBikePodsByViewPort: LoadBikePodsByViewPort

    @Before
    fun setup() {
        initRx()
        bikePodRepository = mockk()
        getCellIdsFromViewPort = mockk()
        loadBikePodsByViewPort = LoadBikePodsByViewPort(bikePodRepository, getCellIdsFromViewPort)
    }

    @Test
    fun `execute should return bike pods when viewport is CloseEnough`() {
        val viewPort = mockk<ViewPort.CloseEnough>()
        val bounds = mockk<LatLngBounds>()
        val southwest = GeoPoint(10.0, 20.0)
        val northeast = GeoPoint(30.0, 40.0)
        val cellIds = listOf("cell1", "cell2")
        val bikePods = listOf(mockk<BikePodLocationEntity>(), mockk())

        every { viewPort.visibleBounds } returns bounds
        every { bounds.southwest } returns LatLng(10.0, 20.0)
        every { bounds.northeast } returns LatLng(30.0, 40.0)
        every { getCellIdsFromViewPort.execute(viewPort) } returns Observable.just(cellIds)
        every { bikePodRepository.getBikePodsWithinBounds(cellIds, southwest, northeast) } returns Observable.just(bikePods)

        val testObserver = loadBikePodsByViewPort.execute(viewPort).test()

        testObserver.assertComplete()
        testObserver.assertValue(bikePods)
    }

    @Test
    fun `execute should return empty list when viewport is not CloseEnough`() {
        val viewPort = mockk<ViewPort.NotCloseEnough>()

        val testObserver = loadBikePodsByViewPort.execute(viewPort).test()

        testObserver.assertComplete()
        testObserver.assertValue(emptyList())
    }
}