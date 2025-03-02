package com.skedgo.tripkit.ui.search

import android.content.Context
import android.os.Bundle
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.skedgo.tripkit.data.regions.RegionService
import com.skedgo.tripkit.logging.ErrorLogger
import com.skedgo.tripkit.ui.base.MockKTest
import com.skedgo.tripkit.ui.core.SchedulerFactory
import com.skedgo.tripkit.ui.data.places.Place
import com.skedgo.tripkit.ui.data.places.PlaceSearchRepository
import com.skedgo.tripkit.ui.database.location_history.LocationHistoryRepository
import com.skedgo.tripkit.ui.favorites.v2.data.network.FavoritesRepository
import com.squareup.picasso.Picasso
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import org.amshove.kluent.internal.assertEquals
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode

@LooperMode(LooperMode.Mode.PAUSED)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class LocationSearchViewModelTest : MockKTest() {

    private lateinit var viewModel: LocationSearchViewModel

    private val context: Context= mockk(relaxed = true)
    private val regionService: RegionService = mockk(relaxed = true)
    private val placeSearchRepository: PlaceSearchRepository = mockk(relaxed = true)
    private val fetchSuggestions: FetchSuggestions = mockk(relaxed = true)
    private val errorLogger: ErrorLogger = mockk(relaxed = true)
    private val picasso: Picasso = mockk(relaxed = true)
    private val schedulerFactory: SchedulerFactory = mockk(relaxed = true)
    private val locationHistoryRepository: LocationHistoryRepository = mockk(relaxed = true)
    private val errorViewModel: LocationSearchErrorViewModel = mockk(relaxed = true)
    private val favoritesRepository: FavoritesRepository = mockk(relaxed = true)

    @Before
    fun setUp() {
        initRx()
        viewModel = spyk(
            LocationSearchViewModel(
                context,
                regionService,
                placeSearchRepository,
                fetchSuggestions,
                errorLogger,
                picasso,
                schedulerFactory,
                locationHistoryRepository,
                errorViewModel,
                favoritesRepository
            )
        )
    }

    @After
    fun teardown() {
        tearDownRx()
        clearAllMocks()
    }

    @Test
    fun `onQueryTextChanged should update query and set showRefreshing to true`() {
        val query = "test location"

        viewModel.onQueryTextChanged(query)

        assertTrue(viewModel.showRefreshing.get())
    }

    @Test
    fun `onTextSubmit should return false when no suggestions exist`() {
        every { viewModel.googleAndTripGoSuggestions.isNotEmpty() } returns false

        val result = viewModel.onTextSubmit()

        assertFalse(result)
    }

    @Test
    fun `goBack should trigger dismiss event`() {
        val testObserver = viewModel.dismiss.test()

        viewModel.goBack()

        testObserver.assertValue(Unit)
    }

    @Test
    fun `handleArgs should correctly extract values from bundle`() {
        val bounds: LatLngBounds = mockk()
        val center: LatLng = mockk()
        val bundle: Bundle = mockk {
            every { containsKey(KEY_BOUNDS) } returns true
            every { getParcelable<LatLngBounds>(KEY_BOUNDS) } returns bounds
            every { containsKey(KEY_CENTER) } returns true
            every { getParcelable<LatLng>(KEY_CENTER) } returns center
            every { getBoolean(ARG_CAN_OPEN_TIMETABLE, false) } returns true
            every { getBoolean(ARG_WITH_CURRENT_LOCATION, false) } returns true
            every { getBoolean(ARG_WITH_DROP_PIN, false) } returns true
            every { getBoolean(ARG_SHOW_BACK_BUTTON, true) } returns false
            every { getBoolean(ARG_SHOW_SEARCH_FIELD, true) } returns false
        }

        viewModel.handleArgs(bundle)

        assertEquals(bounds, viewModel.bounds())
        assertEquals(center, viewModel.center())
        assertTrue(viewModel.showCurrentLocation)
        assertTrue(viewModel.showDropPin)
        assertTrue(viewModel.canOpenTimetable)
        assertFalse(viewModel.showBackButton.get())
        assertFalse(viewModel.showSearchBox.get())
    }
}
