package com.skedgo.tripkit.ui.geocoding

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.skedgo.tripkit.data.connectivity.ConnectivityService
import com.skedgo.tripkit.ui.base.MockKTest
import com.skedgo.tripkit.ui.data.places.Place
import com.skedgo.tripkit.ui.search.*
import io.mockk.*
import io.reactivex.Observable
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import javax.inject.Provider

@RunWith(JUnit4::class)
class AutoCompleteTaskTest: MockKTest() {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private lateinit var autoCompleteTask: AutoCompleteTask

    private val mockFilterSupportedLocations: FilterSupportedLocations = mockk()
    private val mockFetchLocalLocations: FetchLocalLocations = mockk()
    private val mockFetchGoogleLocations: FetchGoogleLocations = mockk()
    private val mockFetchFoursquareLocations: FetchFoursquareLocations = mockk()
    private val mockFetchTripGoLocations: FetchTripGoLocations = mockk()
    private val mockConnectivityService: ConnectivityService = mockk()
    private val mockResultAggregator: ResultAggregator = mockk()

    private val mockFetchLocalLocationsProvider: Provider<FetchLocalLocations> = mockk()
    private val mockFetchGoogleLocationsProvider: Provider<FetchGoogleLocations> = mockk()
    private val mockFetchFoursquareLocationsProvider: Provider<FetchFoursquareLocations> = mockk()
    private val mockFetchTripGoLocationsProvider: Provider<FetchTripGoLocations> = mockk()

    private val mockFetchLocationsParameters: FetchLocationsParameters = mockk()
    private val mockResults: List<Place> = listOf(mockk(), mockk()) // Ensure these match HasResults.suggestions

    @Before
    fun setUp() {
        every { mockFetchLocalLocationsProvider.get() } returns mockFetchLocalLocations
        every { mockFetchGoogleLocationsProvider.get() } returns mockFetchGoogleLocations
        every { mockFetchFoursquareLocationsProvider.get() } returns mockFetchFoursquareLocations
        every { mockFetchTripGoLocationsProvider.get() } returns mockFetchTripGoLocations

        autoCompleteTask = AutoCompleteTask(
            mockFilterSupportedLocations,
            mockFetchLocalLocationsProvider,
            mockFetchFoursquareLocationsProvider,
            mockFetchGoogleLocationsProvider,
            mockFetchTripGoLocationsProvider,
            mockConnectivityService,
            mockResultAggregator
        )
    }

    @Test
    fun `query should return HasResults when network is connected`() {
        // Given
        every { mockConnectivityService.isNetworkConnected } returns true
        every { mockFetchLocationsParameters.term() } returns "Test Query"

        every { mockFetchLocalLocations.getLocations(mockFetchLocationsParameters) } returns Observable.just(emptyList())
        every { mockFetchGoogleLocations.getLocations(mockFetchLocationsParameters) } returns Observable.just(emptyList())
        every { mockFetchFoursquareLocations.getLocations(mockFetchLocationsParameters) } returns Observable.just(emptyList())
        every { mockFetchTripGoLocations.getLocations(mockFetchLocationsParameters) } returns Observable.just(emptyList())

        every { mockResultAggregator.aggregate(mockFetchLocationsParameters, any()) } returns mockResults

        // When
        val testObserver = autoCompleteTask.query(mockFetchLocationsParameters).test()

        // Then
        testObserver.assertComplete()
        testObserver.assertValueAt(0) { result ->
            result is HasResults && result.suggestions == mockResults
        }
    }

    @Test
    fun `query should return NoConnection when network is not connected`() {
        // Given
        every { mockConnectivityService.isNetworkConnected } returns false
        every { mockFetchLocationsParameters.term() } returns "Test Query"

        // When
        val testObserver = autoCompleteTask.query(mockFetchLocationsParameters).test()

        // Then
        testObserver.assertComplete()
        testObserver.assertValue(NoConnection)
    }

    @Test
    fun `query should return NoResult when search term has no matches`() {
        // Given
        every { mockConnectivityService.isNetworkConnected } returns true
        every { mockFetchLocationsParameters.term() } returns "Unknown Query"

        every { mockFetchLocalLocations.getLocations(mockFetchLocationsParameters) } returns Observable.just(emptyList())
        every { mockFetchGoogleLocations.getLocations(mockFetchLocationsParameters) } returns Observable.just(emptyList())
        every { mockFetchFoursquareLocations.getLocations(mockFetchLocationsParameters) } returns Observable.just(emptyList())
        every { mockFetchTripGoLocations.getLocations(mockFetchLocationsParameters) } returns Observable.just(emptyList())

        every { mockResultAggregator.aggregate(mockFetchLocationsParameters, any()) } returns emptyList()

        // When
        val testObserver = autoCompleteTask.query(mockFetchLocationsParameters).test()

        // Then
        testObserver.assertComplete()
        testObserver.assertValue(NoResult("Unknown Query"))
    }
}
