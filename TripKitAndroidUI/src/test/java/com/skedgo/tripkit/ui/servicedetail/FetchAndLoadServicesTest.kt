package com.skedgo.tripkit.ui.servicedetail

import com.skedgo.tripkit.common.model.stop.ScheduledStop
import com.skedgo.tripkit.ui.model.StopInfo
import com.skedgo.tripkit.ui.model.TimetableEntry
import com.skedgo.tripkit.ui.timetables.ServiceRepository
import com.skedgo.tripkit.ui.utils.ServiceLineOverlayTask
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.reactivex.Completable
import io.reactivex.Single
import io.reactivex.observers.TestObserver
import org.junit.Before
import org.junit.Test

class FetchAndLoadServicesTest {

    private val serviceRepository: ServiceRepository = mockk()
    private lateinit var fetchAndLoadServices: FetchAndLoadServices

    private val service: TimetableEntry = mockk()
    private val stop: ScheduledStop = mockk()
    private val stopInfoList: List<StopInfo> = mockk()
    private val serviceLineInfoList: List<ServiceLineOverlayTask.ServiceLineInfo> = mockk()

    @Before
    fun setup() {
        fetchAndLoadServices = FetchAndLoadServices(serviceRepository)

        // Mock repository responses
        every { serviceRepository.fetchServices(service, stop) } returns Completable.complete()
        every { serviceRepository.loadServices(service, stop) } returns Single.just(
            Pair(
                stopInfoList,
                serviceLineInfoList
            )
        )
        every { serviceRepository.fetchAndLoadServices(service, stop) } returns Single.just(
            Pair(
                stopInfoList,
                serviceLineInfoList
            )
        )
    }

    @Test
    fun `execute should call fetchServices and loadServices`() {
        val testObserver: TestObserver<Pair<List<StopInfo>, List<ServiceLineOverlayTask.ServiceLineInfo>>> =
            fetchAndLoadServices.execute(service, stop).test()

        testObserver.assertComplete()
        testObserver.assertValue { it.first == stopInfoList && it.second == serviceLineInfoList }

        verify { serviceRepository.fetchServices(service, stop) }
        verify { serviceRepository.loadServices(service, stop) }
    }

    @Test
    fun `load should call fetchAndLoadServices`() {
        val testObserver: TestObserver<Pair<List<StopInfo>, List<ServiceLineOverlayTask.ServiceLineInfo>>> =
            fetchAndLoadServices.load(service, stop).test()

        testObserver.assertComplete()
        testObserver.assertValue { it.first == stopInfoList && it.second == serviceLineInfoList }

        verify { serviceRepository.fetchAndLoadServices(service, stop) }
    }
}
