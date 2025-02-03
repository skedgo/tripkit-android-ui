package com.skedgo.tripkit.ui.map

import android.content.Context
import android.graphics.Bitmap
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.skedgo.tripkit.location.GeoPoint
import com.skedgo.tripkit.routing.Trip
import com.skedgo.tripkit.routing.TripSegment
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockkStatic
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class SegmentStopMarkerMakerTest {

    @MockK(relaxed = true)
    lateinit var context: Context

    @MockK(relaxed = true)
    lateinit var trip: Trip

    @MockK(relaxed = true)
    lateinit var stop: ServiceStop

    @MockK(relaxed = true)
    lateinit var segment: TripSegment

    @MockK(relaxed = true)
    lateinit var bitmap: Bitmap

    @MockK(relaxed = true)
    lateinit var bitmapDescriptor: BitmapDescriptor

    private lateinit var segmentStopMarkerMaker: SegmentStopMarkerMaker

    @Before
    fun setUp() {
        MockKAnnotations.init(this, relaxed = true)

        // Initialize SegmentStopMarkerMaker with mock context
        segmentStopMarkerMaker = SegmentStopMarkerMaker(context)

        // Mock stop position
        every { stop.position } returns GeoPoint(37.7749, -122.4194)
        every { stop.name } returns "Mock Stop"
        every { stop.platform } returns "Platform 1"

        // Mock context resources
        every { context.resources.getDimensionPixelSize(any()) } returns 20

        // Mock BitmapDescriptorFactory to avoid real object creation
        mockkStatic(BitmapDescriptorFactory::class)
        every { BitmapDescriptorFactory.fromBitmap(any()) } returns bitmapDescriptor

        // Mock MapMarkerUtils
        mockkStatic(MapMarkerUtils::class)
        every { MapMarkerUtils.createStopMarkerIcon(any(), any(), any(), any()) } returns bitmap
    }

    @Test
    fun `make should return valid MarkerOptions`() {
        // Arrange
        val stopMarkerViewModel = StopMarkerViewModel(
            trip = trip,
            stop = stop,
            title = "Test Stop",
            segment = segment,
            isTravelled = false
        )

        // Act
        val markerOptions = segmentStopMarkerMaker.make(stopMarkerViewModel)

        // Assert
        assertNotNull(markerOptions)
        assertEquals("Test Stop", markerOptions.title)
        assertEquals("Mock Stop", markerOptions.snippet)
        assertEquals(LatLng(37.7749, -122.4194), markerOptions.position)
        assertEquals(0.5f, markerOptions.anchorU)
        assertEquals(0.5f, markerOptions.anchorV)
        assertEquals(0.5f, markerOptions.infoWindowAnchorU)
        assertEquals(0f, markerOptions.infoWindowAnchorV)
        assertEquals(0.5f, markerOptions.alpha)
    }
}