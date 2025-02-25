package com.skedgo.tripkit.ui.map

import com.google.android.gms.maps.model.MarkerOptions
import com.skedgo.tripkit.routing.TripSegment
import com.skedgo.tripkit.routing.Visibilities
import com.skedgo.tripkit.ui.base.MockKTest
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class CreateSegmentMarkersTest: MockKTest() {

    @MockK
    private lateinit var segmentMarkerMaker: SegmentMarkerMaker

    private lateinit var createSegmentMarkers: CreateSegmentMarkers

    @Before
    fun setUp() {
        initRx()
        MockKAnnotations.init(this, relaxed = true)
        createSegmentMarkers = CreateSegmentMarkers(segmentMarkerMaker)
    }

    @After
    fun teardown() {
        tearDownRx()
    }

    @Test
    fun `execute returns segment-marker pairs when segments are visible`() {
        // Arrange
        val segment1 = mockk<TripSegment>(relaxed = true) {
            every { isVisibleInContext(Visibilities.VISIBILITY_ON_MAP) } returns true
        }
        val segment2 = mockk<TripSegment>(relaxed = true) {
            every { isVisibleInContext(Visibilities.VISIBILITY_ON_MAP) } returns true
        }
        val marker1 = mockk<MarkerOptions>()
        val marker2 = mockk<MarkerOptions>()

        every { segmentMarkerMaker.make(segment1) } returns marker1
        every { segmentMarkerMaker.make(segment2) } returns marker2

        // Act
        val testObserver = createSegmentMarkers.execute(listOf(segment1, segment2)).test()

        // Assert
        testObserver.assertValue(listOf(segment1 to marker1, segment2 to marker2))
        testObserver.assertComplete()
    }

    @Test
    fun `execute returns empty list when all segments are invisible`() {
        // Arrange
        val segment1 = mockk<TripSegment>(relaxed = true) {
            every { isVisibleInContext(Visibilities.VISIBILITY_ON_MAP) } returns false
        }
        val segment2 = mockk<TripSegment>(relaxed = true) {
            every { isVisibleInContext(Visibilities.VISIBILITY_ON_MAP) } returns false
        }

        // Act
        val testObserver = createSegmentMarkers.execute(listOf(segment1, segment2)).test()

        // Assert
        testObserver.assertValue(emptyList())
        testObserver.assertComplete()
    }

    @Test
    fun `execute filters out segments where MarkerOptions is null`() {
        // Arrange
        val segment1 = mockk<TripSegment>(relaxed = true) {
            every { isVisibleInContext(Visibilities.VISIBILITY_ON_MAP) } returns true
        }
        val segment2 = mockk<TripSegment>(relaxed = true) {
            every { isVisibleInContext(Visibilities.VISIBILITY_ON_MAP) } returns true
        }
        val marker1 = mockk<MarkerOptions>()

        every { segmentMarkerMaker.make(segment1) } returns marker1
        every { segmentMarkerMaker.make(segment2) } returns null // This should be filtered out

        // Act
        val testObserver = createSegmentMarkers.execute(listOf(segment1, segment2)).test()

        // Assert
        testObserver.assertValue(listOf(segment1 to marker1)) // segment2 should be filtered out
        testObserver.assertComplete()
    }
}