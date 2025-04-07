package com.skedgo.tripkit.ui.favorites

import android.content.res.Resources
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.google.gson.Gson
import com.haroldadmin.cnradapter.NetworkResponse
import com.skedgo.tripkit.ApiError
import com.skedgo.tripkit.routing.RoutingResponse
import com.skedgo.tripkit.routing.TripGroup
import com.skedgo.tripkit.ui.base.MockKTest
import com.skedgo.tripkit.ui.data.waypoints.WaypointsAdvancedRequestBody
import com.skedgo.tripkit.ui.data.waypoints.WaypointsApi
import com.skedgo.tripkit.ui.data.waypoints.WaypointsRequestBody
import com.skedgo.tripkit.ui.favorites.waypoints.Waypoint
import com.skedgo.tripkit.ui.routing.RoutingConfig
import io.mockk.*
import io.reactivex.Observable
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class GetTripFromWaypointsImplTest: MockKTest() {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private lateinit var getTripFromWaypoints: GetTripFromWaypointsImpl

    private val mockResources: Resources = mockk(relaxed = true)
    private val mockGson: Gson = mockk(relaxed = true)
    private val mockWaypointsApi: WaypointsApi = mockk()

    private val mockRoutingConfig: RoutingConfig = mockk(relaxed = true)
    private val mockTripGroup: TripGroup = mockk(relaxed = true)
    private val mockWaypoints = listOf(mockk<Waypoint>(relaxed = true))

    @Before
    fun setUp() {
        initRx()
        getTripFromWaypoints = GetTripFromWaypointsImpl(
            resources = mockResources,
            gson = mockGson,
            waypointsApi = mockWaypointsApi
        )

        // Mock successful API response
        every { mockWaypointsApi.request(any<WaypointsRequestBody>()) } returns
            Observable.just(mockk(relaxed = true) {
                every { tripGroupList } returns arrayListOf(mockTripGroup)
                every { errorMessage } returns null
            })

        val routingResponse: RoutingResponse = mockk(relaxed = true) {
            every { tripGroupList } returns arrayListOf(mockTripGroup)
            every { errorMessage } returns null
        }

        every { mockWaypointsApi.request(any<WaypointsAdvancedRequestBody>()) } returns
            Observable.just(routingResponse)

        val networkResponse: NetworkResponse<RoutingResponse, ApiError> = NetworkResponse.Success(
            routingResponse, null, 200
        )

        coEvery { mockWaypointsApi.requestTripGroup(any()) } returns networkResponse
    }

    fun teardown() {
        tearDownRx()
    }

    @Test
    fun `execute should return WaypointResponse when API is successful`() {
        // Act
        val testObserver = getTripFromWaypoints.execute(mockRoutingConfig, mockWaypoints).test()

        // Assert
        testObserver.assertComplete()
        testObserver.assertNoErrors()
        testObserver.assertValue { response ->
            response != null && response.tripGroup == mockTripGroup && response.error == null
        }
    }

    @Test
    fun `executeAsFlow should return WaypointResponse when API is successful`() = runBlocking {
        // Act
        val result = getTripFromWaypoints.executeAsFlow(mockRoutingConfig, mockWaypoints).toList()

        // Assert
        assertEquals(1, result.size) // Ensure at least one item is emitted
        val response = result.first()
        assertEquals(mockTripGroup, response?.tripGroup)
        assertNull(response?.error)
    }

    @Test
    fun `requestTripGroup should return TripGroup when API is successful`() = runBlocking {
        // Act
        val result = getTripFromWaypoints.requestTripGroup(mockRoutingConfig, mockWaypoints)

        // Assert
        assertNotNull(result)
        assertEquals(mockTripGroup, result)
    }

    @Test
    fun `requestTripGroup should return null when API fails`() = runBlocking {
        // Mock failure response
        coEvery { mockWaypointsApi.requestTripGroup(any()) } returns NetworkResponse.ServerError(
            body = null, code = 500
        )

        // Act
        val result = getTripFromWaypoints.requestTripGroup(mockRoutingConfig, mockWaypoints)

        // Assert
        assertNull(result)
    }
}
