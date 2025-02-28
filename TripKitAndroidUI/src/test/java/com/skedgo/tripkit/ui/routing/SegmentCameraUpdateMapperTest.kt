package com.skedgo.tripkit.ui.routing

import com.google.android.gms.maps.CameraUpdate
import com.google.android.gms.maps.CameraUpdateFactory
import com.skedgo.tripkit.common.model.location.Location
import io.mockk.*
import org.junit.After
import org.junit.Before
import org.junit.Test

class SegmentCameraUpdateMapperTest {

    private lateinit var mapper: SegmentCameraUpdateMapper

    @Before
    fun setUp() {
        mapper = SegmentCameraUpdateMapper()

        mockkStatic(CameraUpdateFactory::class) // Mock Google Maps CameraUpdateFactory

        every { CameraUpdateFactory.newLatLngZoom(any(), any()) } returns mockk<CameraUpdate>()
    }

    @After
    fun tearDown() {
        clearAllMocks()
    }

    @Test
    fun `toCameraUpdate should return zoom update for single location`() {
        val location = spyk(Location(15.0, 25.0)) // Use a real instance

        val segmentCameraUpdate: SegmentCameraUpdate.HasOneLocation = mockk(relaxed = true) {
            every { this@mockk.location } returns location
        }

        val result = mapper.toCameraUpdate(segmentCameraUpdate)

        assert(result.isPresent())
    }

    @Test
    fun `toCameraUpdate should return empty for empty locations`() {
        val segmentCameraUpdate: SegmentCameraUpdate.HasEmptyLocations = mockk(relaxed = true)

        val result = mapper.toCameraUpdate(segmentCameraUpdate)

        assert(!result.isPresent())
    }
}
