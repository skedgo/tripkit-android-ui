package com.skedgo.tripkit.ui.geocoding

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.skedgo.geocoding.agregator.GCResultInterface
import com.skedgo.tripkit.ui.base.MockKTest
import com.skedgo.tripkit.ui.data.places.GooglePlacePrediction
import com.skedgo.tripkit.ui.data.places.LatLng
import com.skedgo.tripkit.ui.data.places.LatLngBounds
import com.skedgo.tripkit.ui.data.places.Place
import com.skedgo.tripkit.ui.data.places.PlaceSearchRepository
import com.skedgo.tripkit.ui.search.FetchLocationsParameters
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.reactivex.Observable
import io.reactivex.observers.TestObserver
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class FetchGoogleLocationsImplTest : MockKTest() {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private lateinit var fetchGoogleLocations: FetchGoogleLocationsImpl
    private val mockPlaceSearchRepository: PlaceSearchRepository = mockk(relaxed = true)
    private val mockParameters: FetchLocationsParameters = mockk(relaxed = true)

    @Before
    fun setup() {
        initRx()
        fetchGoogleLocations = FetchGoogleLocationsImpl(mockPlaceSearchRepository)
    }

    @After
    fun teardown() {
        tearDownRx()
    }

    @Test
    fun `getLocations should return Google results successfully`() {
        val mockPlaceWithoutLocation = mockk<GooglePlacePrediction>(relaxed = true)

        every { mockParameters.term() } returns "Test Query"
        every { mockParameters.southwestLat() } returns -33.0
        every { mockParameters.southwestLon() } returns 151.0
        every { mockParameters.northeastLat() } returns -32.0
        every { mockParameters.northeastLon() } returns 152.0

        val mockBounds = LatLngBounds(
            LatLng(-33.2, 150.8),
            LatLng(-31.8, 152.2)
        )

        every { mockPlaceSearchRepository.searchForPlaces("Test Query", any()) } returns
            Observable.just(mockPlaceWithoutLocation)

        val testObserver: TestObserver<List<GCResultInterface>> =
            fetchGoogleLocations.getLocations(mockParameters).test()

        testObserver.awaitTerminalEvent()
        testObserver.assertComplete()
        testObserver.assertNoErrors()
        testObserver.assertValue { it.isNotEmpty() }
    }

    @Test
    fun `getLocations should throw exception on error`() {
        every { mockParameters.term() } returns "Invalid Query"
        every { mockParameters.southwestLat() } returns -33.0
        every { mockParameters.southwestLon() } returns 151.0
        every { mockParameters.northeastLat() } returns -32.0
        every { mockParameters.northeastLon() } returns 152.0

        // Mock GoogleGeocoderLive constructor
        mockkConstructor(GoogleGeocoderLive::class)
        every {
            anyConstructed<GoogleGeocoderLive>().query(
                any(), any(), any(), any(), any(), any()
            )
        } returns Observable.error<Place.WithoutLocation>(Exception("API Error"))
            .onErrorReturnItem(mockk(relaxed = true))

        every { mockPlaceSearchRepository.searchForPlaces(any(), any()) } returns
            Observable.error(Exception("API Error"))

        val testObserver: TestObserver<List<GCResultInterface>> =
            fetchGoogleLocations.getLocations(mockParameters).test()

        testObserver.awaitTerminalEvent()
        assert(testObserver.errors().isNotEmpty())
        assert(testObserver.values().isEmpty())
    }

}
