package com.skedgo.tripkit.ui.database.location_history

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.skedgo.tripkit.common.model.location.Location
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
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
        // Arrange
        val mockLocation = mockk<Location>(relaxed = false)
        every { mockLocation.name } returns "Sydney"
        every { mockLocation.displayAddress } returns "Sydney, Australia"
        every { mockLocation.lat } returns -33.8688
        every { mockLocation.lon } returns 151.2093
        every { mockLocation.exact } returns true
        every { mockLocation.bearing } returns 90
        every { mockLocation.phoneNumber } returns "+61 2 1234 5678"
        every { mockLocation.url } returns "https://example.com"
        every { mockLocation.timeZone } returns "Australia/Sydney"
        every { mockLocation.popularity } returns 5
        every { mockLocation.locationClass } returns "city"
        every { mockLocation.w3w } returns "word.word.word"
        every { mockLocation.w3wInfoURL } returns "https://w3w.example.com"

        val locations = listOf(mockLocation)

        // Act
        val result = locationHistoryMapper.toEntity(locations)

        // Assert
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
    }

    @Test
    fun `toLocation should map LocationHistoryEntity to Location`() {
        // Arrange
        val mockEntity = mockk<LocationHistoryEntity>(relaxed = false)
        every { mockEntity.name } returns "Melbourne"
        every { mockEntity.address } returns "Melbourne, Australia"
        every { mockEntity.lat } returns -37.8136
        every { mockEntity.lon } returns 144.9631
        every { mockEntity.exact } returns true
        every { mockEntity.bearing } returns 180
        every { mockEntity.phone } returns "+61 3 1234 5678"
        every { mockEntity.url } returns "https://melbourne.example.com"
        every { mockEntity.timezone } returns "Australia/Melbourne" // ✅ Compare as string
        every { mockEntity.popularity } returns 7
        every { mockEntity.locationClass } returns "metro"
        every { mockEntity.w3w } returns "metro.station.city"
        every { mockEntity.wewInfoURL } returns "https://w3w.melbourne.com"

        val entities = listOf(mockEntity)

        // Act
        val result = locationHistoryMapper.toLocation(entities)

        // Assert
        assertEquals(1, result.size)
        val location = result[0]
        assertEquals("Melbourne", location.name)
        assertEquals("Melbourne, Australia", location.address)
        assertEquals(-37.8136, location.lat, 1e-6)
        assertEquals(144.9631, location.lon, 1e-6)
        assertEquals(true, location.exact)
        assertEquals(180, location.bearing)
        assertEquals("+61 3 1234 5678", location.phoneNumber)
        assertEquals("https://melbourne.example.com", location.url)
        assertEquals(7, location.popularity)
        assertEquals("metro", location.locationClass)
        assertEquals("metro.station.city", location.w3w)
        assertEquals("https://w3w.melbourne.com", location.w3wInfoURL)
    }
}
