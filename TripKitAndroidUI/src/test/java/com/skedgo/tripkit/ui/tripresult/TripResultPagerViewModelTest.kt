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
}