package com.skedgo.tripkit.ui.routing

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.skedgo.tripkit.common.model.Query
import com.skedgo.tripkit.routing.TripGroup
import com.skedgo.tripkit.routingstatus.RoutingStatus
import com.skedgo.tripkit.routingstatus.RoutingStatusRepository
import com.skedgo.tripkit.routingstatus.Status
import com.skedgo.tripkit.ui.base.MockKTest
import com.skedgo.tripkit.ui.tripresults.TripResultTransportViewFilter
import io.mockk.*
import io.reactivex.Observable
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class GetSortedTripGroupsWithRoutingStatusTest: MockKTest() {

    @get:Rule
    val rule = InstantTaskExecutorRule()

    private lateinit var useCase: GetSortedTripGroupsWithRoutingStatus
    private val getSortedTripGroups: GetSortedTripGroups = mockk()
    private val routingStatusRepository: RoutingStatusRepository = mockk()

    @Before
    fun setUp() {
        initRx()
        useCase = GetSortedTripGroupsWithRoutingStatus(getSortedTripGroups, routingStatusRepository)
    }

    @After
    fun tearDown() {
        tearDownRx()
        clearAllMocks()
    }

    @Test
    fun `execute should return empty list when status is Error`() {
        val query: Query = mockk()
        val sortOrder = 1
        val filter: TripResultTransportViewFilter = mockk()
        val status = Status.Error("Network Error")

        every { query.uuid() } returns "test_uuid"
        every { routingStatusRepository.getRoutingStatus("test_uuid") } returns Observable.just(RoutingStatus("test_uuid", status))

        val testObserver = useCase.execute(query, sortOrder, filter).test()

        testObserver.assertValue(Pair(emptyList(), status))
        testObserver.assertComplete()
    }

    @Test
    fun `execute should return sorted trip groups when status is Completed`() {
        val query: Query = mockk()
        val sortOrder = 1
        val filter: TripResultTransportViewFilter = mockk()
        val status = Status.Completed()
        val tripGroups: List<TripGroup> = listOf(mockk())

        every { query.uuid() } returns "test_uuid"
        every { query.arriveBy } returns 0
        every { routingStatusRepository.getRoutingStatus("test_uuid") } returns Observable.just(RoutingStatus("test_uuid", status))
        every { getSortedTripGroups.execute("test_uuid", 0, sortOrder, filter) } returns Observable.just(tripGroups)

        val testObserver = useCase.execute(query, sortOrder, filter).test()

        testObserver.assertValue(Pair(tripGroups, status))
        testObserver.assertComplete()
    }

    @Test
    fun `execute should start with empty list and return sorted trip groups when status is InProgress`() {
        val query: Query = mockk()
        val sortOrder = 1
        val filter: TripResultTransportViewFilter = mockk()
        val status = Status.InProgress()
        val tripGroups: List<TripGroup> = listOf(mockk())

        every { query.uuid() } returns "test_uuid"
        every { query.arriveBy } returns 0
        every { routingStatusRepository.getRoutingStatus("test_uuid") } returns Observable.just(RoutingStatus("test_uuid", status))
        every { getSortedTripGroups.execute("test_uuid", query.arriveBy, sortOrder, filter) } returns Observable.just(tripGroups)

        val testObserver = useCase.execute(query, sortOrder, filter).test()

        testObserver.assertValueAt(0, Pair(emptyList(), status)) // First emission should be empty
        testObserver.assertValueAt(1, Pair(tripGroups, status)) // Second emission should contain the trip groups
        testObserver.assertComplete()
    }
}
