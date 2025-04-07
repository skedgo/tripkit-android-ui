package com.skedgo.tripkit.ui.geocoding
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.skedgo.geocoding.agregator.*
import com.skedgo.tripkit.ui.search.FetchLocationsParameters
import io.mockk.*
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class ResultAggregatorImplTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private lateinit var resultAggregator: ResultAggregatorImpl
    private val mockAggregator: MultiSourceGeocodingAggregator<GCResultInterface> = mockk()

    @Before
    fun setup() {
        resultAggregator = spyk(ResultAggregatorImpl())

        mockkObject(MultiSourceGeocodingAggregator)
        every { MultiSourceGeocodingAggregator.getInstance<GCResultInterface>() } returns mockAggregator
    }

    @Ignore("To look at the error on a later date")
    @Test
    fun `test aggregate returns expected places`() {
        val mockFetchLocationsParameters: FetchLocationsParameters = mockk(relaxed = true) {
            every { northeastLat() } returns 151.0
            every { northeastLon() } returns 152.0
            every { southwestLat() } returns -33.0
            every { southwestLon() } returns -32.0
            every { nearbyLat() } returns -33.5
            every { nearbyLon() } returns 151.5
            every { term() } returns "Test Query"
        }

        val mockGCResult: GCResultInterface = mockk(relaxed = true) {
            every { name } returns "Test Place"
            every { lat } returns -33.9
            every { lng } returns 151.2
        }

        val mockMGAResult: MGAResultInterface<GCResultInterface> = mockk(relaxed = true) {
            every { result } returns mockGCResult
        }

        val mockResults = listOf(mockMGAResult)
        val mockNestedResults: List<List<GCResultInterface>> = listOf(listOf(mockGCResult))

        every { mockAggregator.aggregate(any(), any()) } returns mockResults

        // Execute method
        val actualResults = resultAggregator.aggregate(mockFetchLocationsParameters, mockNestedResults)

        // Assertions
        assertEquals(1, actualResults.size)
    }
}
