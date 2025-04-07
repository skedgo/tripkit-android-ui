package com.skedgo.tripkit.ui.geocoding

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.skedgo.geocoding.GCSkedgoResult
import com.skedgo.geocoding.agregator.GCResultInterface
import com.skedgo.tripkit.common.model.location.Location
import com.skedgo.tripkit.ui.base.MockKTest
import com.skedgo.tripkit.ui.search.FetchLocationsParameters
import com.skedgo.tripkit.ui.utils.TransportModeSharedPreference
import io.mockk.every
import io.mockk.mockk
import io.reactivex.observers.TestObserver
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.io.IOException

@RunWith(JUnit4::class)
class FetchTripGoLocationsImplTest : MockKTest() {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private lateinit var fetchTripGoLocations: FetchTripGoLocationsImpl

    private val mockGeocoder: GeocoderLive = mockk(relaxed = true)
    private val mockTransportModeSharedPreference: TransportModeSharedPreference =
        mockk(relaxed = true)
    private val mockParameters: FetchLocationsParameters = mockk(relaxed = true)

    @Before
    fun setup() {
        initRx()
        fetchTripGoLocations =
            FetchTripGoLocationsImpl(mockGeocoder, mockTransportModeSharedPreference)
    }

    @After
    fun teardown() {
        tearDownRx()
    }

    @Test
    fun `getLocations should return results successfully`() {
        val mockLocation: Location = mockk(relaxed = true) {
            every { name } returns "Test Location"
            every { lat } returns -33.0
            every { lon } returns 151.0
            every { locationClass } returns "TestClass"
            every { popularity } returns 5
            every { modeIdentifiers } returns listOf("bus", "train")
            every { w3w } returns null
        }

        val mockResult = SkedgoResultLocationAdapter(
            mockLocation,
            GCSkedgoResult(
                mockLocation.name,
                mockLocation.lat,
                mockLocation.lon,
                mockLocation.locationClass ?: "",
                mockLocation.popularity,
                mockLocation.modeIdentifiers
            )
        )

        every { mockParameters.term() } returns "Test Query"
        every { mockParameters.nearbyLat() } returns -33.8
        every { mockParameters.nearbyLon() } returns 151.2
        every { mockGeocoder.query("Test Query") } returns listOf(mockLocation)

        val testObserver: TestObserver<List<GCResultInterface>> =
            fetchTripGoLocations.getLocations(mockParameters).test()

        testObserver.awaitTerminalEvent()
        testObserver.assertComplete()
        testObserver.assertNoErrors()
        testObserver.assertValue { it.isNotEmpty() }
    }

    @Test
    fun `getLocations should return empty list on error`() {
        every { mockParameters.term() } returns "Invalid Query"
        every { mockParameters.nearbyLat() } returns -33.8
        every { mockParameters.nearbyLon() } returns 151.2
        every { mockGeocoder.query(any()) } throws IOException("Network Error")

        val testObserver: TestObserver<List<GCResultInterface>> =
            fetchTripGoLocations.getLocations(mockParameters).test()

        testObserver.awaitTerminalEvent()
        testObserver.assertComplete()  // Ensures observable completes
        testObserver.assertNoErrors()  // Ensures error is handled properly
        assert(testObserver.values().isEmpty())
    }

    @Test
    fun `getLocations should return empty list when geocoder returns null`() {
        every { mockParameters.term() } returns "Query with Null Result"
        every { mockGeocoder.query(any()) } returns null

        val testObserver: TestObserver<List<GCResultInterface>> =
            fetchTripGoLocations.getLocations(mockParameters).test()

        testObserver.awaitTerminalEvent()
        testObserver.assertComplete()
        testObserver.assertNoErrors()
        testObserver.assertValue { it.isEmpty() } // Expecting an empty list
    }
}
