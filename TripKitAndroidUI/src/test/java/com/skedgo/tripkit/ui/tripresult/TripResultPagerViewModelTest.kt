package com.skedgo.tripkit.ui.tripresult

import android.content.Context
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.MutableLiveData
import com.skedgo.tripkit.common.model.region.Region
import com.skedgo.tripkit.logging.ErrorLogger
import com.skedgo.tripkit.routing.Trip
import com.skedgo.tripkit.routing.TripGroup
import com.skedgo.tripkit.ui.base.MockKTest
import com.skedgo.tripkit.ui.core.SchedulerFactory
import com.skedgo.tripkit.ui.favorites.v2.data.network.FavoritesRepository
import com.skedgo.tripkit.ui.favorites.waypoints.WaypointRepository
import com.skedgo.tripkit.ui.routing.GetSortedTripGroups
import com.skedgo.tripkit.ui.routingresults.*
import com.skedgo.tripkit.ui.tripprogress.UpdateTripProgressWithUserLocation
import com.skedgo.tripkit.ui.tripresult.*
import io.mockk.*
import io.reactivex.Observable
import io.reactivex.schedulers.Schedulers
import org.junit.*
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class TripResultPagerViewModelTest: MockKTest() {

    @get:Rule
    val rule = InstantTaskExecutorRule()

    private lateinit var viewModel: TripResultPagerViewModel

    private val mockContext: Context = mockk(relaxed = true)
    private val getSortedTripGroups: GetSortedTripGroups = mockk(relaxed = true)
    private val trackViewingTrip: TrackViewingTrip = mockk(relaxed = true)
    private val errorLogger: ErrorLogger = mockk(relaxed = true)
    private val selectedTripGroupRepository: SelectedTripGroupRepository = mockk(relaxed = true)
    private val updateTripProgress: UpdateTripProgressWithUserLocation = mockk(relaxed = true)
    private val tripGroupRepository: TripGroupRepository = mockk(relaxed = true)
    private val fetchingRealtimeStatusRepository: FetchingRealtimeStatusRepository = mockk(relaxed = true)
    private val schedulers: SchedulerFactory = mockk(relaxed = true)
    private val waypointsRepository: WaypointRepository = mockk(relaxed = true)
    private val favoritesRepository: FavoritesRepository = mockk(relaxed = true)

    @Before
    fun setup() {
        initRx()
        MockKAnnotations.init(this)

        viewModel = TripResultPagerViewModel(
            context = mockContext,
            getSortedTripGroups = getSortedTripGroups,
            trackViewingTrip = trackViewingTrip,
            errorLogger = errorLogger,
            selectedTripGroupRepository = selectedTripGroupRepository,
            updateTripProgress = updateTripProgress,
            tripGroupRepository = tripGroupRepository,
            fetchingRealtimeStatusRepository = fetchingRealtimeStatusRepository,
            schedulers = schedulers,
            waypointsRepository = waypointsRepository,
            favoritesRepository = favoritesRepository
        )
    }

    @After
    fun tearDown() {
        tearDownRx()
        unmockkAll()
    }

    @Test
    fun `getSortedTripGroups FromRoutes returns sorted trip groups and updates tripGroups`() {
        val tripGroupId = "trip-group-id"
        val tripId = 123L
        val sortOrder = 1
        val requestId = "request-id"
        val arriveBy = 123456L

        val dummyTripGroups = listOf(mockk<TripGroup>(relaxed = true), mockk<TripGroup>(relaxed = true))

        val args = FromRoutes(
            tripGroupId = tripGroupId,
            tripId = tripId,
            sortOrder = sortOrder,
            requestId = requestId,
            arriveBy = arriveBy
        )

        every {
            getSortedTripGroups.execute(
                requestId,
                arriveBy,
                sortOrder,
                any()
            )
        } returns Observable.just(dummyTripGroups)
        val observer = viewModel.observeTripGroups().test()

        val testObserver = viewModel.getSortedTripGroups(args, emptyList()).test()

        testObserver.assertComplete()
        testObserver.assertValue(Unit)

        observer.assertValue(dummyTripGroups)

        val currentGroups = viewModel.tripGroupsBinding.get()
        Assert.assertEquals(dummyTripGroups, currentGroups)

        Assert.assertEquals(false, viewModel.isLoading.get())

        verify(exactly = 1) {
            getSortedTripGroups.execute(
                requestId,
                arriveBy,
                sortOrder,
                any()
            )
        }
    }
    @Test
    fun `favorite recreation restores the displayed stored trip instead of rerouting its waypoints`() {
        val group = TripGroup().apply { uuid("stored-group") }
        val trip = Trip().apply { uuid = "selected-favorite"; tripId = 123L }
        group.setTrips(arrayListOf(trip))
        group.displayTripId = trip.tripId
        val state = mockk<android.os.Bundle>(relaxed = true)
        every { state.getString(ARG_TRIP_GROUP_ID) } returns group.uuid()
        every { state.getLong("selectedTripId", -1L) } returns trip.tripId
        every { selectedTripGroupRepository.getSelectedTripGroup() } returns Observable.just(group)
        every { favoritesRepository.getFavoriteById(any()) } returns kotlinx.coroutines.flow.flowOf(null)
        every { waypointsRepository.getTripGroup(any()) } returns kotlinx.coroutines.flow.flowOf(null)

        viewModel.onCreate(state)
        val groups = viewModel.observeTripGroups().test()
        viewModel.getSortedTripGroups(FavoriteTrip("original-favorite"), emptyList()).test()

        groups.assertValue(listOf(group))
        Assert.assertSame(trip, viewModel.defaultTrip)
        verify(exactly = 0) { favoritesRepository.getFavoriteById(any()) }
        verify(exactly = 0) { waypointsRepository.getTripGroup(any()) }
    }

    @Test
    fun `route list restoration applies the saved selected trip before publishing database results`() {
        val group = TripGroup().apply { uuid("routed-group") }
        val first = Trip().apply { uuid = "default-journey"; tripId = 1L }
        val selected = Trip().apply { uuid = "favorited-journey"; tripId = 2L }
        group.setTrips(arrayListOf(first, selected))
        group.displayTripId = first.tripId
        val state = mockk<android.os.Bundle>(relaxed = true)
        every { state.getString(ARG_TRIP_GROUP_ID) } returns group.uuid()
        every { state.getLong("selectedTripId", -1L) } returns selected.tripId
        every { getSortedTripGroups.execute(any(), any(), any(), any()) } returns Observable.just(listOf(group))
        viewModel.onCreate(state)
        var publishedTrip: Trip? = null
        val subscription = viewModel.observeTripGroups().subscribe { publishedTrip = it.single().displayTrip }
        viewModel.getSortedTripGroups(FromRoutes(group.uuid(), first.tripId, 1, "request", 0L), emptyList()).test()
        subscription.dispose()
        Assert.assertSame(selected, publishedTrip)
        Assert.assertSame(selected, viewModel.defaultTrip)
    }

    @Test
    fun `resuming a resolved favorite reuses its trip instead of routing the pattern again`() {
        val group = TripGroup().apply { uuid("resolved-group") }
        val trip = Trip().apply { uuid = "resolved-trip"; tripId = 123L }
        group.setTrips(arrayListOf(trip))
        group.displayTripId = trip.tripId
        every { favoritesRepository.getFavoriteById(any()) } returns kotlinx.coroutines.flow.flowOf(null)
        every { waypointsRepository.getTripGroup(any()) } returns kotlinx.coroutines.flow.flowOf(group)
        every { selectedTripGroupRepository.getSelectedTripGroup() } returns Observable.just(group)
        val args = FavoriteTrip("persisted-pattern")
        viewModel.getSortedTripGroups(args, emptyList()).blockingFirst()
        viewModel.getSortedTripGroups(args, emptyList()).blockingFirst()
        verify(exactly = 1) { waypointsRepository.getTripGroup(any()) }
        verify(exactly = 1) { selectedTripGroupRepository.setSelectedTripGroupId(group.uuid()) }
        Assert.assertSame(trip, viewModel.defaultTrip)
    }

    @Test
    fun `opening a different favorite still resolves its own pattern`() {
        val first = TripGroup().apply { uuid("first-group") }
        first.setTrips(arrayListOf(Trip().apply { tripId = 123L }))
        val second = TripGroup().apply { uuid("second-group") }
        val selected = Trip().apply { tripId = 456L }
        second.setTrips(arrayListOf(selected))
        every { favoritesRepository.getFavoriteById(any()) } returns kotlinx.coroutines.flow.flowOf(null)
        every { waypointsRepository.getTripGroup(any()) } returnsMany listOf(
            kotlinx.coroutines.flow.flowOf(first), kotlinx.coroutines.flow.flowOf(second)
        )
        every { selectedTripGroupRepository.getSelectedTripGroup() } returns Observable.just(first)
        viewModel.getSortedTripGroups(FavoriteTrip("first-pattern"), emptyList()).blockingFirst()
        viewModel.getSortedTripGroups(FavoriteTrip("first-pattern"), emptyList()).blockingFirst()
        viewModel.getSortedTripGroups(FavoriteTrip("different-pattern"), emptyList()).blockingFirst()
        verify(exactly = 2) { waypointsRepository.getTripGroup(any()) }
        verify(exactly = 1) { favoritesRepository.getFavoriteById("different-pattern") }
        Assert.assertSame(selected, viewModel.defaultTrip)
    }

}