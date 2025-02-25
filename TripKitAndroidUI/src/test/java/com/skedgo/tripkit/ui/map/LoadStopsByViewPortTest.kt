package com.skedgo.tripkit.ui.map

import android.util.Pair
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.skedgo.tripkit.common.model.region.Region
import com.skedgo.tripkit.common.model.stop.ScheduledStop
import com.skedgo.tripkit.data.database.locations.freefloating.FreeFloatingLocationEntity
import com.skedgo.tripkit.data.regions.RegionService
import com.skedgo.tripkit.location.GeoPoint
import com.skedgo.tripkit.ui.base.MockKTest
import com.skedgo.tripkit.ui.map.home.GetCellIdsFromViewPort
import com.skedgo.tripkit.ui.map.home.StopLoaderArgs
import com.skedgo.tripkit.ui.map.home.ViewPort
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.verify
import io.reactivex.Observable
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class LoadStopsByViewPortTest: MockKTest() {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private lateinit var getCellIdsFromViewPort: GetCellIdsFromViewPort
    private lateinit var scheduledStopRepository: ScheduledStopRepository
    private lateinit var regionService: RegionService
    private lateinit var loadStopsByViewPort: LoadStopsByViewPort

    @Before
    fun setup() {
        initRx()
        getCellIdsFromViewPort = mockk()
        scheduledStopRepository = mockk()
        regionService = mockk()
        loadStopsByViewPort = LoadStopsByViewPort(getCellIdsFromViewPort, scheduledStopRepository, regionService)
    }

    @After
    fun teardown() {
        tearDownRx()
    }

    // TODO to fix
//    @Test
//    fun `execute should return stops when viewport is close enough`() {
//        // Mock viewport and its bounds
//        val viewPort = mockk<ViewPort.CloseEnough>(relaxed = true)
//        val visibleBounds = mockk<com.skedgo.tripkit.ui.data.places.LatLngBounds>(relaxed = true)
//        val bounds = LatLngBounds(LatLng(10.0, 20.0), LatLng(30.0, 40.0))
//        val cellIds = listOf("cell1", "cell2")
//
//        // Mock LatLng values
//        val southwest = com.skedgo.tripkit.ui.data.places.LatLng(10.0, 20.0)
//        val northeast =com.skedgo.tripkit.ui.data.places.LatLng(30.0, 40.0)
//        every { visibleBounds.southwest } returns southwest
//        every { visibleBounds.northeast } returns northeast
//
//        // Ensure viewport returns the mocked bounds
//        every { viewPort.visibleBounds } returns visibleBounds
//
//        // Mock region
//        val region = mockk<Region>(relaxed = true) {
//            every { name } returns "TestRegion"
//        }
//
//        // Mock list of stops
//        val stops = listOf(mockk<ScheduledStop>(relaxed = true), mockk(relaxed = true))
//
//        // Mock dependencies
//        every { regionService.getRegionByLocationAsync(any(), any()) } returns Observable.just(region)
//        every { getCellIdsFromViewPort.execute(any()) } returns Observable.just(cellIds)
//
//        // Mock StopLoaderArgs static method
//        mockkStatic(StopLoaderArgs::class)
//        every { StopLoaderArgs.newArgsForStopsLoader(any(), any(), any()) } returns Pair(cellIds, bounds)
//        every { StopLoaderArgs.createStopLoaderSelectionArgs(any(), any()) } returns arrayOf()
//        every { StopLoaderArgs.createStopLoaderSelection(any()) } returns ""
//
//        // Mock scheduledStopRepository to return stops
//        every { scheduledStopRepository.queryStops(any(), any(), any(), any()) } returns Observable.just(stops)
//
//        // Execute function under test
//        val testObserver = loadStopsByViewPort.execute(viewPort).test()
//
//        // Assertions
//        testObserver.assertComplete()
//        testObserver.assertValue(stops)
//
//        // Verify interactions
//        verify { regionService.getRegionByLocationAsync(any(), any()) }
//        verify { getCellIdsFromViewPort.execute(any()) }
//        verify { scheduledStopRepository.queryStops(any(), any(), any(), any()) }
//    }

    @Test
    fun `execute should return empty list when viewport is not close enough`() {
        // Mock ViewPort
        val viewPort = mockk<ViewPort.NotCloseEnough>()

        // Execute and test
        val testObserver = loadStopsByViewPort.execute(viewPort).test()

        testObserver.assertComplete()
        testObserver.assertValue(emptyList())
    }
}