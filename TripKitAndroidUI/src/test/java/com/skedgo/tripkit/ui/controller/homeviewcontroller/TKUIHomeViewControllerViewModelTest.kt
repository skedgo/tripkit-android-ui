package com.skedgo.tripkit.ui.controller.homeviewcontroller

import android.content.Context
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Observer
import com.skedgo.rxtry.Failure
import com.skedgo.rxtry.Success
import com.skedgo.rxtry.Try
import com.skedgo.tripkit.common.model.Location
import com.skedgo.tripkit.location.GeoPoint
import com.skedgo.tripkit.location.UserGeoPointRepository
import com.skedgo.tripkit.ui.R
import com.skedgo.tripkit.ui.base.MockKTest
import com.skedgo.tripkit.ui.controller.ControllerDataProvider
import com.skedgo.tripkit.ui.controller.ViewControllerEvent
import com.skedgo.tripkit.ui.controller.ViewControllerEventBus
import com.skedgo.tripkit.ui.controller.locationsearchcontroller.TKUIFavoritesSuggestionProvider
import com.skedgo.tripkit.ui.controller.utils.LocationField
import com.skedgo.tripkit.ui.search.FixedSuggestions
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.verify
import io.reactivex.Observable
import io.reactivex.observers.TestObserver
import org.amshove.kluent.`should be instance of`
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner
import java.lang.RuntimeException

@RunWith(MockitoJUnitRunner::class)
class TKUIHomeViewControllerViewModelTest: MockKTest() {

    private lateinit var viewModel: TKUIHomeViewControllerViewModel

    @MockK
    private lateinit var context: Context

    @MockK(relaxed = true)
    private lateinit var eventBus: ViewControllerEventBus

    @MockK(relaxed = true)
    private lateinit var userGeoPointRepository: UserGeoPointRepository

    @MockK
    private lateinit var favoriteProvider: TKUIFavoritesSuggestionProvider

    @MockK(relaxed = true)
    private lateinit var myLocationButtonObserver: Observer<Boolean>

    @MockK(relaxed = true)
    private lateinit var stateObserver: Observer<TKUIHomeViewControllerUIState>

    @get:Rule
    val rule = InstantTaskExecutorRule()

    @Before
    fun setup() {
        MockKAnnotations.init(this)
        initRx()

        viewModel = TKUIHomeViewControllerViewModel(
            context, eventBus, userGeoPointRepository
        )

        viewModel.myLocationButtonVisible.observeForever(myLocationButtonObserver)
        viewModel.state.observeForever(stateObserver)

        ControllerDataProvider.favoriteProvider = favoriteProvider

        every { context.resources.getString(R.string.current_location) } returns "Current Location"
    }

    @After
    fun tearDown() {
        tearDownRx()
    }

    @Test
    fun `getUserGeoPoint GeoPoint success and user location is returned`() {
        val geoPoint = GeoPoint(1.0, 2.0)
        every { userGeoPointRepository.getFirstCurrentGeoPoint() } returns Observable.just(geoPoint)

        val locationSubscriber = TestObserver<Try<Location>>()

        viewModel.getUserGeoPointObservable().subscribe(locationSubscriber)

        locationSubscriber.apply {
            assertComplete()
            assertNoErrors()
            val result = values().first()
            result as Success
            assertEquals(1.0, result.invoke().lat, 0.0)
            assertEquals(2.0, result.invoke().lon, 0.0)
            locationSubscriber.dispose()
        }
    }

    @Test
    fun `getUserGeoPoint GeoPoint returns an exception so try should return failed`() {
        val exception = RuntimeException("error")
        every { userGeoPointRepository.getFirstCurrentGeoPoint() } returns Observable.error(exception)

        val locationSubscriber = TestObserver<Try<Location>>()

        viewModel.getUserGeoPointObservable().subscribe(locationSubscriber)

        locationSubscriber.apply {
            assertComplete()
            val result = values().first()
            result.`should be instance of`<Failure<Throwable>>()
            locationSubscriber.dispose()
        }
    }

    @Test
    fun `setMyLocationButtonVisible should update myLocationButtonVisible`() {
        val isVisible = true
        viewModel.setMyLocationButtonVisible(isVisible)

        verify { myLocationButtonObserver.onChanged(isVisible) }
    }

    @Test
    fun `toggleChooseOnMap should update state and set isChooseOnMap value`() {
        val isShow = true
        viewModel.toggleChooseOnMap(isShow)

        verify { stateObserver.onChanged(match { it.isChooseOnMap == isShow }) }
    }

    @Test
    fun `handleFixedSuggestionAction with CURRENT_LOCATION does nothing`() {
        // Call the function with CURRENT_LOCATION
        viewModel.handleFixedSuggestionAction(FixedSuggestions.CURRENT_LOCATION)

        // Verify that no interactions with the eventBus happen
        verify(exactly = 0) { eventBus.publish(any()) }
    }

    @Test
    fun `handleFixedSuggestionAction CHOOSE_ON_MAP publishes OnChooseOnMap event`() {
        // Call the function with CHOOSE_ON_MAP
        viewModel.handleFixedSuggestionAction(FixedSuggestions.CHOOSE_ON_MAP)

        // Verify that the correct event is published
        verify { eventBus.publish(ViewControllerEvent.OnChooseOnMap(LocationField.NONE)) }
    }

    @Test
    fun `handleFixedSuggestionAction HOME publishes OnLocationChosen event with home location`() {
        val homeLocation = Location(1.0, 2.0)
        every { ControllerDataProvider.getFavoritesHome() } returns homeLocation

        // Call the function with HOME
        viewModel.handleFixedSuggestionAction(FixedSuggestions.HOME)

        // Verify that the correct event is published
        verify { eventBus.publish(ViewControllerEvent.OnLocationChosen(homeLocation, LocationField.NONE)) }
    }

    @Test
    fun `handleFixedSuggestionAction WORK publishes OnLocationChosen event with work location`() {
        val workLocation = Location(1.0, 2.0)
        every { ControllerDataProvider.getFavoritesWork() } returns workLocation

        // Call the function with WORK
        viewModel.handleFixedSuggestionAction(FixedSuggestions.WORK)

        // Verify that the correct event is published
        verify { eventBus.publish(ViewControllerEvent.OnLocationChosen(workLocation, LocationField.NONE)) }
    }
}