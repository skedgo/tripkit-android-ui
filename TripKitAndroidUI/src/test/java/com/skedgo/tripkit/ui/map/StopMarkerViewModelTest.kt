package com.skedgo.tripkit.ui.map

import android.graphics.Color
import com.skedgo.tripkit.location.GeoPoint
import com.skedgo.tripkit.routing.Trip
import com.skedgo.tripkit.routing.TripSegment
import com.skedgo.tripkit.ui.data.places.LatLng
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test

class StopMarkerViewModelTest {

    @MockK(relaxed = true)
    lateinit var trip: Trip

    @MockK(relaxed = true)
    lateinit var stop: ServiceStop

    @MockK(relaxed = true)
    lateinit var segment: TripSegment

    @Before
    fun setUp() {
        MockKAnnotations.init(this, relaxed = true)

        // Mock stop position
        every { stop.position } returns GeoPoint(37.7749, -122.4194)
        every { stop.name } returns "Mock Stop"
        every { stop.platform } returns "Platform 1"
    }

    @Test
    fun `should initialize with correct values when travelled`() {
        // Arrange
        val viewModel = StopMarkerViewModel(
            trip = trip,
            stop = stop,
            title = "Travelled Stop",
            segment = segment,
            isTravelled = true
        )

        // Act & Assert
        assertNotNull(viewModel)
        assertEquals("Mock Stop", viewModel.snippet)
        assertEquals(
            LatLng(37.7749, -122.4194).latitude,
            viewModel.position.latitude, viewModel.position.latitude
        )
        assertEquals(
            LatLng(37.7749, -122.4194).longitude,
            viewModel.position.longitude, viewModel.position.longitude
        )
        assertEquals(Color.BLACK, viewModel.strokeColor)
        assertEquals(Color.WHITE, viewModel.fillColor)
        assertEquals(1f, viewModel.alpha)
    }

    @Test
    fun `should initialize with correct values when not travelled`() {
        // Arrange
        val viewModel = StopMarkerViewModel(
            trip = trip,
            stop = stop,
            title = "Upcoming Stop",
            segment = segment,
            isTravelled = false
        )

        // Act & Assert
        assertNotNull(viewModel)
        assertEquals("Mock Stop", viewModel.snippet)
        assertEquals(
            LatLng(37.7749, -122.4194).latitude,
            viewModel.position.latitude, viewModel.position.latitude
        )
        assertEquals(
            LatLng(37.7749, -122.4194).longitude,
            viewModel.position.longitude, viewModel.position.longitude
        )
        assertEquals(Color.GRAY, viewModel.strokeColor)
        assertEquals(Color.LTGRAY, viewModel.fillColor)
        assertEquals(0.5f, viewModel.alpha)
    }

    @Test
    fun `should use platform as snippet when name is null`() {
        // Arrange
        every { stop.name } returns null
        every { stop.platform } returns "Platform 2"

        val viewModel = StopMarkerViewModel(
            trip = trip,
            stop = stop,
            title = "Platform Stop",
            segment = segment,
            isTravelled = false
        )

        // Act & Assert
        assertEquals("Platform 2", viewModel.snippet)
    }
}