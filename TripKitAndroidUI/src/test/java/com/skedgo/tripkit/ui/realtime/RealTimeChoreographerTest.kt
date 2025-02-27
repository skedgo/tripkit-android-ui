package com.skedgo.tripkit.ui.realtime

import com.skedgo.tripkit.common.agenda.IRealTimeElement
import com.skedgo.tripkit.common.model.region.Region
import com.skedgo.tripkit.routing.RealTimeVehicle
import com.skedgo.tripkit.ui.base.MockKTest
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.reactivex.Single
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class RealTimeChoreographerTest: MockKTest() {

    private lateinit var realTimeChoreographer: RealTimeChoreographer
    private val realTimeRepository: RealTimeRepository = mockk()

    @Before
    fun setUp() {
        initRx()
        realTimeChoreographer = RealTimeChoreographer(realTimeRepository)
    }

    @After
    fun tearDown() {
        tearDownRx()
        clearAllMocks()
    }

    @Test
    fun `getRealTimeResults should return real-time vehicle updates`() {
        val region = mockk<Region>(relaxed = true)
        val elements = listOf(mockk<IRealTimeElement>(relaxed = true))
        val vehicles = listOf(mockk<RealTimeVehicle>(relaxed = true))

        every { region.name } returns "test_region"
        every { realTimeRepository.getUpdates("test_region", elements) } returns Single.just(vehicles)

        val testObserver = realTimeChoreographer.getRealTimeResults(region, elements).test()

        testObserver.assertValue(vehicles)
        testObserver.dispose()
    }

    @Test
    fun `getRealTimeResults should return empty observable on error`() {
        val region = mockk<Region>(relaxed = true)
        val elements = listOf(mockk<IRealTimeElement>(relaxed = true))

        every { region.name } returns "test_region"
        every { realTimeRepository.getUpdates("test_region", elements) } returns Single.error(Exception("Network error"))

        val testObserver = realTimeChoreographer.getRealTimeResults(region, elements)
            .test()

        testObserver.assertNoValues() // No items should be emitted
        testObserver.assertNoErrors() // Ensure that no errors are emitted downstream
        testObserver.dispose()
    }


    @Test
    fun `getRealTimeResultsFromCleanElements should call getRealTimeResults with cleaned elements`() {
        val region = mockk<Region>(relaxed = true)
        val element1 = mockk<IRealTimeElement>(relaxed = true) {
            every { serviceTripId } returns "1"
            every { startStopCode } returns "A"
            every { endStopCode } returns "B"
        }
        val element2 = mockk<IRealTimeElement>(relaxed = true) {
            every { serviceTripId } returns "1"
            every { startStopCode } returns "A"
            every { endStopCode } returns "B"
        }
        val elements = listOf(element1, null, element2)
        val cleanedElements = listOf(element1)
        val vehicles = listOf(mockk<RealTimeVehicle>(relaxed = true))

        every { region.name } returns "test_region"
        every { realTimeRepository.getUpdates("test_region", cleanedElements) } returns Single.just(vehicles)

        val testObserver = realTimeChoreographer.getRealTimeResultsFromCleanElements(region, elements).test()

        testObserver.assertValue(vehicles)
        testObserver.dispose()
    }
}
