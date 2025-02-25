package com.skedgo.tripkit.ui.poidetails

import android.content.Context
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.skedgo.TripKit
import com.skedgo.tripkit.Configs
import com.skedgo.tripkit.LocationInfoService
import com.skedgo.tripkit.common.model.location.Location
import com.skedgo.tripkit.ui.R
import com.skedgo.tripkit.ui.base.MockKTest
import com.skedgo.tripkit.ui.data.places.PlaceSearchRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.reactivex.Observable
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestRule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class PoiDetailsViewModelTest : MockKTest() {

    @get:Rule
    var rule: TestRule = InstantTaskExecutorRule()

    private lateinit var viewModel: PoiDetailsViewModel
    private val locationInfoService: LocationInfoService = mockk()
    private val placeSearchRepository: PlaceSearchRepository = mockk()
    private val context: Context = mockk(relaxed = true)
    private val tripKit: TripKit = mockk()
    private val configs: Configs = mockk()

    @Before
    fun setup() {
        initRx()
        // Mock TripKit instance
        mockkObject(TripKit.Companion)
        every { TripKit.getInstance() } returns tripKit
        every { tripKit.configs() } returns configs
        every { configs.hideFavorites() } returns false

        viewModel = PoiDetailsViewModel(locationInfoService, placeSearchRepository)
    }

    @After
    fun teardown() {
        tearDownRx()
    }

    @Test
    fun `setFavorite should update favorite text`() {
        every { context.getString(R.string.remove_favourite) } returns "Remove Favourite"
        every { context.getString(R.string.favourite) } returns "Favourite"

        viewModel.setFavorite(context, true)
        assert(viewModel.favoriteText.get() == "Remove Favourite")

        viewModel.setFavorite(context, false)
        assert(viewModel.favoriteText.get() == "Favourite")
    }

    @Test
    fun `start should update fields correctly`() {
        val location: Location = mockk(relaxed = true)
        every { location.displayName } returns "Test Location"
        every { location.address } returns "123 Test St"
        every { location.url } returns "http://example.com"
        every { location.locationType } returns Location.TYPE_UNKNOWN
        every { location.withExternalApp } returns false

        every { locationInfoService.getLocationInfoAsync(any()) } returns Observable.just(mockk {
            every { details()?.w3w() } returns "word1.word2.word3"
        })

        viewModel.start(context, location)

        assert(viewModel.locationTitle.get() == "Test Location")
        assert(viewModel.address.get() == "123 Test St")
        assert(viewModel.website.get() == "http://example.com")
        assert(viewModel.type.get() == Location.TYPE_UNKNOWN)
        assert(viewModel.withExternalApp.get() == false)
        assert(viewModel.what3words.get() == "word1.word2.word3")
        assert(viewModel.showWhat3words.get())
    }
}