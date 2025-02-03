package com.skedgo.tripkit.ui.map

import com.google.android.gms.maps.model.MarkerOptions
import com.skedgo.tripkit.common.model.location.Location
import com.skedgo.tripkit.ui.data.places.LatLng
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class TripLocationMarkerCreatorTest {

    private lateinit var tripLocationMarkerCreator: TripLocationMarkerCreator

    @Before
    fun setUp() {
        tripLocationMarkerCreator = TripLocationMarkerCreator()
    }

    @Test
    fun `call should create MarkerOptions with correct title and snippet when location has name and address`() {
        // Arrange
        val location = mockk<Location>(relaxed = true)
        every { location.name } returns "Test Location"
        every { location.address } returns "123 Test Street"
        every { location.lat } returns 37.7749
        every { location.lon } returns -122.4194

        // Act
        val result: MarkerOptions = tripLocationMarkerCreator.call(location)

        // Assert
        assertNotNull(result)
        assertEquals("Test Location", result.title)
        assertEquals("123 Test Street", result.snippet)
        assertEquals(
            LatLng(37.7749, -122.4194).latitude,
            result.position.latitude,
            result.position.latitude
        )
        assertEquals(
            LatLng(37.7749, -122.4194).longitude,
            result.position.longitude,
            result.position.longitude
        )
    }

    @Test
    fun `call should use address as title when name is empty`() {
        // Arrange
        val location = mockk<Location>(relaxed = true)
        every { location.name } returns null
        every { location.address } returns "456 Another Street"
        every { location.lat } returns 40.7128
        every { location.lon } returns -74.0060

        // Act
        val result: MarkerOptions = tripLocationMarkerCreator.call(location)

        // Assert
        assertNotNull(result)
        assertEquals("456 Another Street", result.title)
        assertEquals(null, result.snippet)
        assertEquals(
            LatLng(40.7128, -74.0060).latitude,
            result.position.latitude,
            result.position.latitude,
        )
        assertEquals(
            LatLng(40.7128, -74.0060).longitude,
            result.position.longitude,
            result.position.longitude
        )
    }

    @Test
    fun `call should use coordinate string as title when name and address are empty`() {
        // Arrange
        val location = mockk<Location>(relaxed = true)
        every { location.name } returns null
        every { location.address } returns null
        every { location.coordinateString } returns "(40.7128, -74.0060)"
        every { location.lat } returns 40.7128
        every { location.lon } returns -74.0060

        // Act
        val result: MarkerOptions = tripLocationMarkerCreator.call(location)

        // Assert
        assertNotNull(result)
        assertEquals("(40.7128, -74.0060)", result.title)
        assertEquals(null, result.snippet)
        assertEquals(
            LatLng(40.7128, -74.0060).latitude,
            result.position.latitude,
            result.position.latitude
        )
        assertEquals(
            LatLng(40.7128, -74.0060).longitude,
            result.position.longitude,
            result.position.longitude,
        )
    }
}