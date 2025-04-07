package com.skedgo.tripkit.ui.geocoding

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.google.gson.Gson
import com.skedgo.tripkit.common.model.location.Location
import com.skedgo.tripkit.ui.utils.HttpUtils
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.spyk
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import java.io.IOException

class GeocoderTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    @Test
    fun `test query with valid response`() {
        val geocoder = spyk(Geocoder(), recordPrivateCalls = true)

        mockkStatic(HttpUtils::class)
        val mockResponse = """
            {
                "query": "San Francisco",
                "choices": [
                    {"latitude": 37.7749, "longitude": -122.4194, "name": "San Francisco"}
                ]
            }
        """.trimIndent()
        every { HttpUtils.get(any(), any()) } returns mockResponse

        val mockGson = mockk<Gson>(relaxed = true)
        val geocodeResponse = GeocodeResponse(
            query = "San Francisco",
            choiceList = listOf(Location().apply {
                lat = 37.7749
                lon = -122.4194
                name = "San Francisco"
            })
        )
        every { mockGson.fromJson(mockResponse, GeocodeResponse::class.java) } returns geocodeResponse

        geocoder.mGson = mockGson

        val result = geocoder.query("San Francisco")

        assertEquals(1, result?.size)
        assertEquals("San Francisco", result?.get(0)?.name)
        assertEquals(37.7749, result?.get(0)?.lat ?: 0.0, 0.0001)
        assertEquals(-122.4194, result?.get(0)?.lon ?: 0.0, 0.0001)
    }

    @Test(expected = IOException::class)
    fun `test query throws IOException`() {
        val geocoder = Geocoder()
        mockkStatic(HttpUtils::class)
        every { HttpUtils.get(any(), any()) } throws IOException("Network Error")

        geocoder.query("San Francisco")
    }

    @Test
    fun `test setting near latitude and longitude`() {
        val geocoder = Geocoder()
        geocoder.setNearLatitude(37.7749)
        geocoder.setNearLongitude(-122.4194)

        assertEquals(37.7749, geocoder.nearLatitude, 0.0001)
        assertEquals(-122.4194, geocoder.nearLongitude, 0.0001)
    }
}
