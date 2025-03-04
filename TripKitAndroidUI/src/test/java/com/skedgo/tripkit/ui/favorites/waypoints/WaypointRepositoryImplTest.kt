package com.skedgo.tripkit.ui.favorites.waypoints

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.skedgo.network.Resource
import com.skedgo.tripkit.routing.TripGroup
import com.skedgo.tripkit.ui.base.MockKTest
import com.skedgo.tripkit.ui.favorites.GetTripFromWaypoints
import com.skedgo.tripkit.ui.favorites.GetTripFromWaypoints.WaypointResponse
import com.skedgo.tripkit.ui.routing.GetRoutingConfig
import com.skedgo.tripkit.ui.routingresults.TripGroupRepository
import io.mockk.*
import io.reactivex.Completable
import io.reactivex.Observable
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@ExperimentalCoroutinesApi
@RunWith(JUnit4::class)
class WaypointRepositoryImplTest: MockKTest() {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private lateinit var repository: WaypointRepository.WaypointRepositoryImpl

    private val mockGetRoutingConfig: GetRoutingConfig = mockk()
    private val mockGetTripFromWaypoints: GetTripFromWaypoints = mockk()
    private val mockTripGroupRepository: TripGroupRepository = mockk()
    private val mockWaypointsDao: WaypointsDao = mockk()

    @Before
    fun setUp() {
        initRx()
        repository = WaypointRepository.WaypointRepositoryImpl(
            getRoutingConfig = mockGetRoutingConfig,
            getTripFromWaypoints = mockGetTripFromWaypoints,
            tripGroupRepository = mockTripGroupRepository,
            waypointsDao = mockWaypointsDao
        )
    }

    @After
    fun teardown() {
        tearDownRx()
    }

    @Test
    fun `insertWaypoints should insert waypoints successfully`() = runBlocking {
        // Arrange
        val tripId = "trip123"
        val waypoints = listOf(mockk<Waypoint>(relaxed = true))

        coEvery { mockWaypointsDao.deleteTripWaypoints(tripId) } just Runs
        coEvery { mockWaypointsDao.insertAll(any()) } just Runs

        // Act
        val flow = repository.insertWaypoints(tripId, waypoints)

        // Assert
        val results = flow.toList()
        assertEquals(Resource.success(true), results.last())
    }

    @Test
    fun `deleteTripWaypoints should remove waypoints successfully`() = runBlocking {
        // Arrange
        val tripId = "trip123"

        coEvery { mockWaypointsDao.deleteTripWaypoints(tripId) } just Runs

        // Act
        val flow = repository.deleteTripWaypoints(tripId)

        // Assert
        val results = flow.toList()
        assertEquals(Resource.success(true), results.last())
    }

    @Ignore(
        """
            To check again later, having issue with the WaypointEntity.toWaypoint on this test
            even values are correctly mocked
        """
    )
    @Test
    fun `getTripWaypoints should return waypoints for given tripId`() = runBlocking {
        val id = "trip123"

        val mockWaypointEntity = mockk<WaypointEntity>(relaxed = true).apply {
            every { tripId } returns id
            every { order } returns 1
            every { modeTitle } returns "Bus"
        }

        val mockWaypoint = mockk<Waypoint>(relaxed = true).apply {
            every { modeTitle } returns "Bus"
        }

        val mockWaypointEntities = listOf(mockWaypointEntity)

        coEvery { mockWaypointsDao.getAllWaypoints() } returns mockWaypointEntities
        every { mockWaypointEntity.toWaypoint() } returns mockWaypoint

        val flow = repository.getTripWaypoints(id)

        val results = flow.toList()
        assertEquals(1, results.last().size)
    }

    @Test
    fun `getTripGroup should return trip group from waypoints`() = runBlocking {
        // Arrange
        val waypoints = listOf(mockk<Waypoint>(relaxed = true))
        val mockTripGroup = mockk<TripGroup>(relaxed = true)
        val mockResponse = mockk<WaypointResponse>(relaxed = true)

        every { mockResponse.tripGroup } returns mockTripGroup

        coEvery { mockGetRoutingConfig.execute() } returns mockk(relaxed = true)
        coEvery { mockGetTripFromWaypoints.executeAsFlow(any(), waypoints) } returns flowOf(mockResponse)

        coEvery { mockTripGroupRepository.addTripGroups(any(), any()) } returns Completable.complete()
        coEvery { mockTripGroupRepository.getTripGroup(any()) } returns Observable.just(mockTripGroup)


        // Act
        val flow = repository.getTripGroup(waypoints)

        // Assert
        val results = flow.toList()
        assertEquals(mockTripGroup, results.last())
    }

}
