package com.skedgo.tripkit.ui.geocoding

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.skedgo.tripkit.ui.base.MockKTest
import com.skedgo.tripkit.ui.data.places.GooglePlacePrediction
import com.skedgo.tripkit.ui.data.places.LatLng
import com.skedgo.tripkit.ui.data.places.LatLngBounds
import com.skedgo.tripkit.ui.data.places.PlaceSearchRepository
import io.mockk.every
import io.mockk.mockk
import io.reactivex.Observable
import org.amshove.kluent.internal.assertEquals
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class GoogleGeocoderLiveTest: MockKTest() {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private lateinit var googleGeocoderLive: GoogleGeocoderLive
    private val placeSearchRepository: PlaceSearchRepository = mockk()

    @Before
    fun setup() {
        initRx()

        val mockPlaceWithoutLocation = mockk<GooglePlacePrediction>(relaxed = true)

        val mockBounds = LatLngBounds(
            LatLng(-33.2, 150.8),
            LatLng(-31.8, 152.2)
        )

        every { placeSearchRepository.searchForPlaces(any(), any()) } returns
            Observable.just(mockPlaceWithoutLocation)

        googleGeocoderLive = GoogleGeocoderLive(placeSearchRepository)
    }

    @After
    fun teardown() {
        tearDownRx()
    }

    @Test
    fun `test query returns expected results`() {
        val locationName = "Test Location"
        val maxResult = 5
        val swLat = -33.0
        val swLon = 151.0
        val neLat = -32.0
        val neLon = 152.0

        val mockPrediction: GooglePlacePrediction = mockk(relaxed = true)
        val mockObservable: Observable<GooglePlacePrediction> = Observable.just(mockPrediction)

        val latLngBounds = LatLngBounds(
            LatLng(swLat - 0.2, swLon - 0.2),
            LatLng(neLat + 0.2, neLon + 0.2)
        )

        every { placeSearchRepository.searchForPlaces(locationName, latLngBounds) } returns mockObservable

        val result = googleGeocoderLive.query(locationName, maxResult, swLat, swLon, neLat, neLon)
            .blockingIterable()
            .toList()

        assertEquals(1, result.size)
    }

}