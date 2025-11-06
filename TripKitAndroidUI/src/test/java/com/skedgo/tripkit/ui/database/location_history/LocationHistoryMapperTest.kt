package com.skedgo.tripkit.ui.database.location_history

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.skedgo.tripkit.common.model.location.Location
import com.skedgo.tripkit.common.model.stop.ScheduledStop
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class LocationHistoryMapperTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private lateinit var locationHistoryMapper: LocationHistoryMapper

    @Before
    fun setUp() {
        locationHistoryMapper = LocationHistoryMapper()
    }

    @Test
    fun `toEntity should map Location to LocationHistoryEntity`() {
        val location = Location().apply {
            name = "Sydney"
            address = "Sydney, Australia"
            lat = -33.8688
            lon = 151.2093
            exact = true
            bearing = 90
            phoneNumber = "+61 2 1234 5678"
            url = "https://example.com"
            timeZone = "Australia/Sydney"
            popularity = 5
            locationClass = "city"
            w3w = "word.word.word"
            w3wInfoURL = "https://w3w.example.com"
            locationType = Location.TYPE_HISTORY
        }

        val result = locationHistoryMapper.toEntity(listOf(location))

        assertEquals(1, result.size)
        val entity = result[0]
        assertEquals("Sydney", entity.name)
        assertEquals("Sydney, Australia", entity.address)
        assertEquals(-33.8688, entity.lat, 1e-6)
        assertEquals(151.2093, entity.lon, 1e-6)
        assertEquals(true, entity.exact)
        assertEquals(90, entity.bearing)
        assertEquals("+61 2 1234 5678", entity.phone)
        assertEquals("https://example.com", entity.url)
        assertEquals("Australia/Sydney", entity.timezone)
        assertEquals(5, entity.popularity)
        assertEquals("city", entity.locationClass)
        assertEquals("word.word.word", entity.w3w)
        assertEquals("https://w3w.example.com", entity.wewInfoURL)
        assertEquals(Location.TYPE_HISTORY, entity.locationType)
        assertNotNull(entity.locationJson)
        assertTrue(entity.locationJson!!.contains("Sydney"))
    }

    @Test
    fun `toLocation should map LocationHistoryEntity to ScheduledStop`() {
        val originalStop = ScheduledStop().apply {
            name = "Town Hall Station"
            address = "Sydney NSW"
            lat = -33.8731
            lon = 151.2060
            exact = true
            bearing = 0
            phoneNumber = "+61 2 0000 0000"
            url = "https://transportnsw.info"
            timeZone = "Australia/Sydney"
            popularity = 10
            locationClass = "stop"
            w3w = "town.hall.station"
            w3wInfoURL = "https://w3w.example.com/townhall"
            locationType = Location.TYPE_SCHEDULED_STOP
            stopId = 12345L
            code = "200020"
            shortName = "Town Hall"
        }

        val entity = locationHistoryMapper.toEntity(listOf(originalStop)).first()
        val restored = locationHistoryMapper.toLocation(listOf(entity)).first()

        assertTrue(restored is ScheduledStop)
        val restoredStop = restored as ScheduledStop
        assertEquals(Location.TYPE_SCHEDULED_STOP, restoredStop.locationType)
        assertEquals("Town Hall Station", restoredStop.name)
        assertEquals("Sydney NSW", restoredStop.address)
        assertEquals(-33.8731, restoredStop.lat, 1e-6)
        assertEquals(151.2060, restoredStop.lon, 1e-6)
        assertEquals(12345L, restoredStop.stopId)
        assertEquals("200020", restoredStop.code)
        assertEquals("Town Hall", restoredStop.shortName)
    }
}
