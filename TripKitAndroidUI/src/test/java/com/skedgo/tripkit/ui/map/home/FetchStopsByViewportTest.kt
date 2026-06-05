package com.skedgo.tripkit.ui.map.home

import com.skedgo.tripkit.common.model.region.Region
import com.skedgo.tripkit.data.locations.StopsFetcher
import com.skedgo.tripkit.data.regions.RegionService
import com.skedgo.tripkit.ui.base.MockKTest
import com.skedgo.tripkit.ui.data.places.LatLng
import com.skedgo.tripkit.ui.data.places.LatLngBounds
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.reactivex.Observable
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class FetchStopsByViewportTest : MockKTest() {

    private val getCellIdsFromViewPort: GetCellIdsFromViewPort = mockk()
    private val regionService: RegionService = mockk()
    private val stopsFetcher: StopsFetcher = mockk()

    private val region: Region = mockk(relaxed = true)

    private lateinit var fetchStopsByViewport: FetchStopsByViewport

    @Before
    fun setUp() {
        initRx()
        fetchStopsByViewport = FetchStopsByViewport(getCellIdsFromViewPort, regionService, stopsFetcher)
        every { regionService.getRegionByLocationAsync(any(), any()) } returns Observable.just(region)
        every { getCellIdsFromViewPort.fetch(any()) } returns Observable.just(listOf("1#1"))
        every { stopsFetcher.fetchAsync(any(), any(), any()) } returns Observable.just(emptyList())
    }

    @After
    fun tearDown() {
        tearDownRx()
    }

    private fun viewPort(zoom: Float): ViewPort.CloseEnough =
        ViewPort.CloseEnough(zoom, LatLngBounds(LatLng(-33.9, 151.1), LatLng(-33.8, 151.3)))

    @Test
    fun `CITY_ONLY does not call stopsFetcher nor region lookup`() {
        fetchStopsByViewport.fetch(viewPort(7.0f)).test().assertComplete()

        verify(exactly = 0) { stopsFetcher.fetchAsync(any(), any(), any()) }
        verify(exactly = 0) { regionService.getRegionByLocationAsync(any(), any()) }
    }

    @Test
    fun `REGION_ONLY fetches only REGION level`() {
        val levels = mutableListOf<Int>()
        every { stopsFetcher.fetchAsync(any(), any(), capture(levels)) } returns Observable.just(emptyList())

        fetchStopsByViewport.fetch(viewPort(10.0f)).test().assertComplete()

        assertEquals(listOf(ApiZoomLevels.REGION), levels)
    }

    @Test
    fun `REGION_AND_LOCAL fetches REGION and LOCAL levels`() {
        val levels = mutableListOf<Int>()
        every { stopsFetcher.fetchAsync(any(), any(), capture(levels)) } returns Observable.just(emptyList())

        fetchStopsByViewport.fetch(viewPort(15.0f)).test().assertComplete()

        assertEquals(2, levels.size)
        assertTrue(levels.contains(ApiZoomLevels.REGION))
        assertTrue(levels.contains(ApiZoomLevels.LOCAL))
    }
}
