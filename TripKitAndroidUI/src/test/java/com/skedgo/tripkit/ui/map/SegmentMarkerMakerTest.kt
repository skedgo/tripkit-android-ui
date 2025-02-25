package com.skedgo.tripkit.ui.map

import android.content.Context
import android.content.res.Resources
import android.graphics.Bitmap
import android.util.Pair
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.skedgo.tripkit.common.model.location.Location
import com.skedgo.tripkit.common.util.TripSegmentUtils
import com.skedgo.tripkit.routing.SegmentType
import com.skedgo.tripkit.routing.TripSegment
import com.skedgo.tripkit.ui.R
import com.skedgo.tripkit.ui.data.places.LatLng
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class SegmentMarkerMakerTest {

    @MockK(relaxed = true)
    lateinit var context: Context

    @MockK(relaxed = true)
    lateinit var iconMaker: SegmentMarkerIconMaker

    @MockK(relaxed = true)
    lateinit var tripSegment: TripSegment

    @MockK(relaxed = true)
    lateinit var resources: Resources

    @MockK(relaxed = true)
    lateinit var bitmap: Bitmap

    @MockK(relaxed = true)
    lateinit var location: Location// ✅ Mocked Location

    private lateinit var segmentMarkerMaker: SegmentMarkerMaker

    @Before
    fun setUp() {
        MockKAnnotations.init(this, relaxed = true)

        // Initialize class with mocks
        segmentMarkerMaker = SegmentMarkerMaker(context, iconMaker)

        // Mock context resources
        every { context.resources } returns resources
        every { resources.getString(R.string.from__pattern) } returns "From: %s"
        every { resources.getString(R.string.to__pattern) } returns "To: %s"

        // Mock Location properties (Fix for the error)
        every { location.lat } returns 37.7749
        every { location.lon } returns -122.4194
        every { location.address } returns "San Francisco, CA"

        // Mock `tripSegment` properties including `getSingleLocation()` and `getAction()`
        tripSegment = mockk {
            every { getType() } returns SegmentType.SCHEDULED
            every { action } returns "Board Bus 123" // Fix: Mocked correctly
            every { from } returns location
            every { to } returns location
            every { singleLocation } returns null
            every { timeZone } returns "America/Los_Angeles"
        }

        // Mock iconMaker response
        every { iconMaker.make(tripSegment) } returns Pair(bitmap, 0.5f)

        // Mock BitmapDescriptorFactory to avoid Google Maps dependency
        mockkStatic(BitmapDescriptorFactory::class)
        every { BitmapDescriptorFactory.fromBitmap(any()) } returns mockk()
    }

    @Test
    fun `make should return MarkerOptions when location is valid`() {
        // Act
        val result = segmentMarkerMaker.make(tripSegment)

        // Assert
        assertNotNull(result)
        assertEquals("From: San Francisco, CA", result?.snippet)
        assertEquals("Board Bus 123", result?.title)
        assertEquals(LatLng(37.7749, -122.4194).latitude, result?.position?.latitude)
        assertEquals(LatLng(37.7749, -122.4194).longitude, result?.position?.longitude)
        verify { iconMaker.make(tripSegment) }
    }

    @Test
    fun `make should return null when location is invalid`() {
        // Arrange
        every { tripSegment.from } returns null
        every { tripSegment.singleLocation } returns null

        // Act
        val result = segmentMarkerMaker.make(tripSegment)

        // Assert
        assertNull(result)
        verify(exactly = 0) { iconMaker.make(tripSegment) }
    }

    @Test
    fun `make should return correct snippet for scheduled segments`() {
        // Act
        val result = segmentMarkerMaker.make(tripSegment)

        // Assert
        assertNotNull(result)
        assertEquals("From: San Francisco, CA", result?.snippet) // ✅ Updated expected value
    }

    @Test
    fun `make should return correct snippet for unscheduled segments`() {
        // Arrange
        every { tripSegment.getType() } returns SegmentType.UNSCHEDULED

        // Act
        val result = segmentMarkerMaker.make(tripSegment)

        // Assert
        assertNotNull(result)
        assertEquals("To: San Francisco, CA", result?.snippet)
        assertEquals(TripSegmentUtils.getTripSegmentAction(context, tripSegment), result?.title)
    }
}