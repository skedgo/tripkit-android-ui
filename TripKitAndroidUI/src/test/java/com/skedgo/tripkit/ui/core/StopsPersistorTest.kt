package com.skedgo.tripkit.ui.core

import android.content.Context
import com.google.gson.Gson
import com.skedgo.tripkit.common.model.stop.ScheduledStop
import com.skedgo.tripkit.common.model.stop.StopType
import com.skedgo.tripkit.data.locations.LocationsResponse
import com.skedgo.tripkit.ui.base.MockKTest
import com.skedgo.tripkit.ui.database.scheduled_stops.ScheduledStopEntity
import com.skedgo.tripkit.ui.map.ScheduledStopRepository
import com.skedgo.tripkit.ui.map.home.ApiZoomLevels
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class StopsPersistorTest : MockKTest() {

    private val context: Context = mockk(relaxed = true)
    private val repository: ScheduledStopRepository = mockk(relaxed = true)
    private lateinit var persistor: StopsPersistor

    @Before
    fun setUp() {
        persistor = StopsPersistor(context, repository, Gson())
    }

    @Test
    fun `saveStopsSync persists REGION api zoom level`() {
        val stop = ScheduledStop().apply {
            code = "STOP_1"
            type = StopType.BUS
            name = "Bus Stop"
            lat = -12.4634
            lon = 130.8456
        }
        val group = mockk<LocationsResponse.Group>()
        every { group.key } returns "10#20"
        every { group.stops } returns arrayListOf(stop)

        val insertedStops = slot<List<ScheduledStopEntity>>()
        every { repository.bulkInsertEntities(capture(insertedStops)) } returns 1
        every { repository.bulkInsertLocationEntities(any()) } returns 1

        persistor.saveStopsSync(listOf(group), ApiZoomLevels.REGION)

        verify(exactly = 1) { repository.bulkInsertEntities(any()) }
        assertTrue(insertedStops.captured.isNotEmpty())
        assertTrue(insertedStops.captured.all { it.apiZoomLevel == ApiZoomLevels.REGION })
    }
}
