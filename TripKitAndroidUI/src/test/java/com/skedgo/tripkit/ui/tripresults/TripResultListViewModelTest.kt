package com.skedgo.tripkit.ui.tripresults

import android.content.Context
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Observer
import com.jakewharton.rxrelay2.PublishRelay
import com.skedgo.tripkit.common.model.Query
import com.skedgo.tripkit.common.model.TransportMode
import com.skedgo.tripkit.common.model.location.Location
import com.skedgo.tripkit.data.regions.RegionService
import com.skedgo.tripkit.logging.ErrorLogger
import com.skedgo.tripkit.ui.routing.GetSortedTripGroupsWithRoutingStatus
import com.skedgo.tripkit.ui.trip.options.RoutingTimeViewModelMapper
import com.skedgo.tripkit.ui.routingresults.TripGroupRepository
import com.skedgo.tripkit.routingstatus.RoutingStatusRepository
import com.skedgo.tripkit.ui.base.MockKTest
import io.mockk.*
import io.reactivex.Completable
import io.reactivex.Observable
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import javax.inject.Provider

@RunWith(JUnit4::class)
class TripResultListViewModelTest: MockKTest() {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val context: Context = mockk(relaxed = true)
    private val tripGroupRepository: TripGroupRepository = mockk(relaxed = true)
    private val routingStatusRepository: RoutingStatusRepository = mockk(relaxed = true)
    private val tripResultViewModelProvider: Provider<TripResultViewModel> = mockk()
    private val getSortedTripGroupsProvider: Provider<GetSortedTripGroupsWithRoutingStatus> = mockk()
    private val tripResultTransportItemViewModelProvider: Provider<TripResultTransportItemViewModel> = mockk()
    private val regionService: RegionService = mockk(relaxed = true)
    private val routeService = mockk<com.skedgo.tripkit.a2brouting.RouteService>(relaxed = true)
    private val errorLogger: ErrorLogger = mockk(relaxed = true)
    private val routingTimeViewModelMapper: RoutingTimeViewModelMapper = mockk(relaxed = true)

    private lateinit var viewModel: TripResultListViewModel

    @Before
    fun setUp() {
        initRx()

        every { tripResultViewModelProvider.get() } returns mockk(relaxed = true)
        every { getSortedTripGroupsProvider.get() } returns mockk(relaxed = true)
        every { tripResultTransportItemViewModelProvider.get() } returns mockk(relaxed = true)

        viewModel = TripResultListViewModel(
            context,
            tripGroupRepository,
            dagger.Lazy { routingStatusRepository },
            tripResultViewModelProvider,
            getSortedTripGroupsProvider,
            tripResultTransportItemViewModelProvider,
            regionService,
            routeService,
            errorLogger,
            routingTimeViewModelMapper
        )
    }

    @After
    fun tearDown() {
        tearDownRx()
    }

    @Test
    fun `onStartLocationClicked sets startLocationListener true`() {
        val observer = mockk<Observer<Boolean>>(relaxed = true)
        viewModel.startLocationListener.observeForever(observer)

        viewModel.onStartLocationClicked()

        assertEquals(true, viewModel.startLocationListener.value)
        verify { observer.onChanged(true) }
    }

    @Test
    fun `setup initializes query and updates observable fields`() {
        val fromLocation = mockk<Location>(relaxed = true)
        every { fromLocation.displayName } returns "Home"

        val toLocation = mockk<Location>(relaxed = true)
        every { toLocation.displayName } returns "Office"

        val query = mockk<Query>(relaxed = true)
        every { query.fromLocation } returns fromLocation
        every { query.toLocation } returns toLocation

        viewModel.setup(
            _query = query,
            showTransportSelectionView = true,
            transportModeFilter = null,
            actionButtonHandlerFactory = mockk(relaxed = true),
            force = true,
            execute = false
        )

        assertEquals("Home", viewModel.fromName.get())
        assertEquals("From Home", viewModel.fromContentDescription.get())
        assertEquals("Office", viewModel.toName.get())
        assertEquals("Going to Office", viewModel.toContentDescription.get())
        assertTrue(viewModel.showTransportModeSelection.get())
    }

    @Test
    fun `getTransport fetches and sets transport modes`() {
        val query = mockk<Query>(relaxed = true) {
            every { fromLocation } returns mockk(relaxed = true)
            every { toLocation } returns mockk(relaxed = true)
        }

        viewModel.query = query

        val transportMode = mockk<TransportMode>(relaxed = true) {
            every { id } returns "bus"
        }

        val viewModelTransportItem = mockk<TripResultTransportItemViewModel>(relaxed = true).apply {
            every { setup(any()) } just Runs
            every { modeId.get() } returns "bus"
            every { clicked } returns PublishRelay.create<Pair<String, Boolean>>()
        }

        every { regionService.getTransportModesByLocationsAsync(any(), any()) } returns
                Observable.just(listOf(transportMode))

        every { tripResultTransportItemViewModelProvider.get() } returns viewModelTransportItem

        viewModel.setReplaceMode(emptyList())

        viewModel.setup(
            _query = query,
            showTransportSelectionView = true,
            transportModeFilter = null,
            actionButtonHandlerFactory = null,
            force = true,
            execute = false
        )

        assertTrue(viewModel.transportModes.get()?.isNotEmpty() == true)
    }

    @Test
    fun `reload clears and reloads`() {
        every { tripGroupRepository.clearPastRoutesAsync() } returns
                Observable.just(1)

        every { tripGroupRepository.addTripGroups(any(), any()) } returns
                Completable.complete()

        val query = mockk<Query>(relaxed = true) {
            every { fromLocation } returns mockk(relaxed = true)
            every { toLocation } returns mockk(relaxed = true)
            every { clone(any()) } returns this
            every { uuid() } returns "query-id"
        }

        viewModel.query = query

        viewModel.reload()

        verify { tripGroupRepository.clearPastRoutesAsync() }
    }
}
