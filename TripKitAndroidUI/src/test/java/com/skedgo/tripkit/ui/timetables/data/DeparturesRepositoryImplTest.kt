package com.skedgo.tripkit.ui.timetables.data

import com.skedgo.tripkit.common.model.region.Region
import com.skedgo.tripkit.data.regions.RegionService
import com.skedgo.tripkit.ui.model.DeparturesResponse
import com.skedgo.tripkit.ui.timetables.domain.DeparturesRepository
import io.mockk.*
import io.reactivex.Observable
import io.reactivex.Single
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.junit.Before
import org.junit.Test

class DeparturesRepositoryImplTest {

    private lateinit var departuresRepository: DeparturesRepository
    private val departuresApi: DeparturesApi = mockk(relaxed = true)
    private val regionService: RegionService = mockk(relaxed = true)

    private val regionName = "TestRegion"
    private val embarkationStopCodes = listOf("STOP_1", "STOP_2")
    private val disembarkationStopCodes = listOf("STOP_3")
    private val timeInSecs = 1670000000L
    private val limit = 5

    private val mockResponse: DeparturesResponse = mockk(relaxed = true)

    @Before
    fun setup() {
        departuresRepository = DeparturesRepositoryImpl(departuresApi, regionService)
    }

    @Test
    fun `getTimetableEntries should call regionService and departuresApi`() {
        val mockRegion = mockk<Region> {
            every { getURLs() } returns arrayListOf("https://api.example.com")
        }

        val requestUrl = "https://api.example.com/departures.json"
        val httpUrl = requestUrl.toHttpUrl()

        every { regionService.getRegionByNameAsync(regionName) } returns Observable.just(mockRegion)
        every { departuresApi.request(httpUrl.toString(), any()) } returns Single.just(mockResponse)

        val testObserver = departuresRepository.getTimetableEntries(
            region = regionName,
            embarkationStopCodes = embarkationStopCodes,
            disembarkationStopCodes = disembarkationStopCodes,
            timeInSecs = timeInSecs,
            limit = limit
        ).test()

        testObserver.assertComplete()
        testObserver.assertValue(mockResponse)

        verify { regionService.getRegionByNameAsync(regionName) }
        verify { departuresApi.request(httpUrl.toString(), any()) }
    }

    @Test
    fun `getTimetableEntries should fail when regionService returns empty URLs`() {
        val mockRegion = mockk<Region> {
            every { getURLs() } returns arrayListOf()
        }

        every { regionService.getRegionByNameAsync(regionName) } returns Observable.just(mockRegion)

        val testObserver = departuresRepository.getTimetableEntries(
            region = regionName,
            embarkationStopCodes = embarkationStopCodes,
            disembarkationStopCodes = disembarkationStopCodes,
            timeInSecs = timeInSecs,
            limit = limit
        ).test()

        testObserver.assertError(NoSuchElementException::class.java)

        verify { regionService.getRegionByNameAsync(regionName) }
    }
}
