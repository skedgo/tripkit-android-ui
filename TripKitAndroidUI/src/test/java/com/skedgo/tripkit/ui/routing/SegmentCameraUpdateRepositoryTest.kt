package com.skedgo.tripkit.ui.routing

import com.google.maps.android.SphericalUtil
import com.jakewharton.rxrelay2.PublishRelay
import com.skedgo.tripkit.routing.SegmentType
import com.skedgo.tripkit.routing.Trip
import com.skedgo.tripkit.routing.TripSegment
import com.skedgo.tripkit.ui.base.MockKTest
import com.skedgo.tripkit.ui.routingresults.GetSelectedTrip
import io.mockk.*
import io.reactivex.Observable
import io.reactivex.observers.TestObserver
import io.reactivex.schedulers.Schedulers
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class SegmentCameraUpdateRepositoryTest: MockKTest() {

    private lateinit var repository: SegmentCameraUpdateRepository
    private val getSelectedTrip: GetSelectedTrip = mockk(relaxed = true)

    @Before
    fun setUp() {
        initRx()
        repository = SegmentCameraUpdateRepository(getSelectedTrip)
    }

    @After
    fun tearDown() {
        tearDownRx()
        clearAllMocks()
    }

    @Test
    fun `getSegmentCameraUpdate should return HasTwoLocations when segment has from and to locations`() {
        mockkStatic(SphericalUtil::class) // Mock static method for distance calculation
        every { SphericalUtil.computeDistanceBetween(any(), any()) } returns 500.0 // Mock distance

        mockkStatic(com.skedgo.tripkit.common.util.SphericalUtil::class) // Mock static method for distance calculation
        every { com.skedgo.tripkit.common.util.SphericalUtil.computeDistanceBetween(any(), any()) } returns 500.0 // Mock distance

        val tripSegment = spyk(TripSegment()) {
            every { from } returns mockk()
            every { to } returns mockk()
            every { getType() } returns SegmentType.SCHEDULED
        }

        val trip = mockk<Trip> {
            every { segmentList } returns mutableListOf(tripSegment)
        }

        every { getSelectedTrip.execute() } returns Observable.just(trip)

        // Subscribe to the observable first before emitting
        val testObserver: TestObserver<SegmentCameraUpdate> = repository.getSegmentCameraUpdate().test()

        repository.putSegment(tripSegment) // Emit after subscription

        testObserver.awaitCount(1) // Wait for at least one emission

        // Capture emitted value
        val emittedValue = testObserver.values().firstOrNull()
        println("DEBUG: Final Emitted Value = $emittedValue")

        // Verify the expected result
        testObserver.assertValue { it is SegmentCameraUpdate.HasTwoLocations }
    }

    @Test
    fun `getSegmentCameraUpdate should return HasOneLocation when segment has only one location`() {
        val tripSegment = spyk(TripSegment()) {
            every { from } returns null
            every { to } returns null
            every { singleLocation } returns mockk()
            every { getType() } returns SegmentType.SCHEDULED
        }

        val trip = mockk<Trip> {
            every { segmentList } returns mutableListOf(tripSegment)
        }

        every { getSelectedTrip.execute() } returns Observable.just(trip)

        // Subscribe before emitting
        val testObserver: TestObserver<SegmentCameraUpdate> = repository.getSegmentCameraUpdate().test()

        repository.putSegment(tripSegment) // Emit after subscription

        testObserver.awaitCount(1) // Wait for at least one emission

        // Capture emitted value
        val emittedValue = testObserver.values().firstOrNull()
        println("DEBUG: Final Emitted Value = $emittedValue")

        // Verify the expected result
        testObserver.assertValue { it is SegmentCameraUpdate.HasOneLocation }
    }

    @Test
    fun `getSegmentCameraUpdate should return HasEmptyLocations when segment has no locations`() {
        val tripSegment = spyk(TripSegment()) {
            every { from } returns null
            every { to } returns null
            every { singleLocation } returns null
            every { getType() } returns SegmentType.SCHEDULED
        }

        val trip = mockk<Trip> {
            every { segmentList } returns mutableListOf(tripSegment)
        }

        every { getSelectedTrip.execute() } returns Observable.just(trip)

        val testObserver = repository.getSegmentCameraUpdate().test() // Subscribe first
        repository.putSegment(tripSegment) // Trigger emission

        testObserver.awaitCount(1) // Ensure it emits at least 1 value
        testObserver.assertValue { it is SegmentCameraUpdate.HasEmptyLocations }
    }


    @Test
    fun `getSegmentCameraUpdate should return next segment if selected segment is STATIONARY`() {
        val stationarySegment = spyk(TripSegment()) {
            every { from } returns null
            every { to } returns null
            every { getType() } returns SegmentType.STATIONARY
        }

        val nextSegment = spyk(TripSegment()) {
            every { from } returns mockk()
            every { to } returns mockk()
            every { getType() } returns SegmentType.SCHEDULED
        }

        val trip = mockk<Trip> {
            every { segmentList } returns mutableListOf(stationarySegment, nextSegment)
        }

        every { getSelectedTrip.execute() } returns Observable.just(trip)

        // Subscribe before emitting
        val testObserver: TestObserver<SegmentCameraUpdate> = repository.getSegmentCameraUpdate().test()

        repository.putSegment(stationarySegment) // Emit after subscription

        testObserver.awaitCount(1) // Wait for at least one emission

        // Capture emitted value
        val emittedValue = testObserver.values().firstOrNull()
        println("DEBUG: Final Emitted Value = $emittedValue")

        // Verify the expected result
        testObserver.assertValue { it is SegmentCameraUpdate.HasTwoLocations }
    }
}
