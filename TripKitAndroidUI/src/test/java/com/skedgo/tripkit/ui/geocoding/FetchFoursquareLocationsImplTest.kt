package com.skedgo.tripkit.ui.geocoding

import com.skedgo.tripkit.ui.core.SchedulerFactory
import com.skedgo.tripkit.ui.search.FetchLocationsParameters
import io.mockk.*
import io.reactivex.schedulers.TestScheduler
import org.junit.Before
import org.junit.Test

class FetchFoursquareLocationsImplTest {

    private lateinit var fetchFoursquareLocations: FetchFoursquareLocationsImpl
    private lateinit var mockSchedulerFactory: SchedulerFactory
    private val testScheduler = TestScheduler()

    @Before
    fun setup() {
        mockSchedulerFactory = mockk(relaxed = true)
        every { mockSchedulerFactory.ioScheduler } returns testScheduler

        fetchFoursquareLocations = FetchFoursquareLocationsImpl(mockSchedulerFactory)
    }

    @Test
    fun `getLocations should return Foursquare locations`() {
        // Given
        val mockParams = mockk<FetchLocationsParameters>(relaxed = true)
        every { mockParams.term() } returns "Test Place"
        every { mockParams.nearbyLat() } returns 37.7749
        every { mockParams.nearbyLon() } returns -122.4194

        val mockResult: FoursquareResultLocationAdapter = mockk {
            every { name } returns "Mock Place"
            every { lat } returns 37.7749
            every { lng } returns -122.4194
        }

        mockkConstructor(FoursquareGeocoder::class)
        every { anyConstructed<FoursquareGeocoder>().fromFoursquare } returns listOf(mockResult)

        // When
        val testObserver = fetchFoursquareLocations.getLocations(mockParams).test()
        testScheduler.triggerActions()

        // Then
        testObserver.assertComplete()
        testObserver.assertNoErrors()
        testObserver.assertValue { list ->
            list.isNotEmpty() &&
                list[0].name == "Mock Place" &&
                list[0].lat == 37.7749 &&
                list[0].lng == -122.4194
        }
    }

    @Test
    fun `getLocations should return empty on error`() {
        // Given
        val mockParams = mockk<FetchLocationsParameters>(relaxed = true)
        every { mockParams.term() } returns "Invalid Place"

        mockkConstructor(FoursquareGeocoder::class)
        every { anyConstructed<FoursquareGeocoder>().fromFoursquare } throws RuntimeException("API Error")

        // When
        val testObserver = fetchFoursquareLocations.getLocations(mockParams).test()
        testScheduler.triggerActions()

        // Then
        testObserver.assertComplete()
        testObserver.assertNoErrors()
        assert(testObserver.values().isEmpty())
    }
}
