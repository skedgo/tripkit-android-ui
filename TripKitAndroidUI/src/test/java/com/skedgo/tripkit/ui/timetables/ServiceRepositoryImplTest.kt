package com.skedgo.tripkit.ui.timetables

import android.content.ContentResolver
import android.content.Context
import android.database.Cursor
import android.net.Uri
import com.skedgo.tripkit.common.model.stop.ScheduledStop
import com.skedgo.tripkit.ui.model.StopInfo
import com.skedgo.tripkit.ui.model.TimetableEntry
import com.skedgo.tripkit.ui.provider.ServiceStopsProvider
import com.skedgo.tripkit.ui.utils.ServiceLineOverlayTask
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.verify
import io.reactivex.Completable
import io.reactivex.Single
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class ServiceRepositoryImplTest {

    private lateinit var serviceRepository: ServiceRepositoryImpl
    private val context: Context = mockk(relaxed = true)
    private val fetchService: FetchService = mockk()
    private val contentResolver: ContentResolver = mockk()
    private val cursor: Cursor = mockk()

    private val service: TimetableEntry = mockk()
    private val stop: ScheduledStop = mockk()

    @Before
    fun setUp() {
        ServiceStopsProvider.STOPS_BY_SERVICE_URI = Uri.parse("content://com.skedgo.tripkit.service_stops")
        every { context.contentResolver } returns contentResolver
        serviceRepository = ServiceRepositoryImpl(context, fetchService)
    }

    @Test
    fun `fetchServices should call fetchService execute`() {
        every { fetchService.execute(service, stop) } returns Completable.complete()

        val testObserver = serviceRepository.fetchServices(service, stop).test()

        testObserver.assertComplete()
        verify { fetchService.execute(service, stop) }
    }

    @Test
    fun `fetchAndLoadServices should call fetchService executeWithResponse`() {
        val expectedData: Pair<List<StopInfo>, List<ServiceLineOverlayTask.ServiceLineInfo>> =
            Pair(emptyList(), emptyList())

        every { fetchService.executeWithResponse(service, stop) } returns Single.just(expectedData)

        val testObserver = serviceRepository.fetchAndLoadServices(service, stop).test()

        testObserver.assertValue(expectedData)
        verify { fetchService.executeWithResponse(service, stop) }
    }

    @Test
    fun `loadServices should query content resolver and return data from LoadServiceTask`() {
        val expectedData: Pair<List<StopInfo>, List<ServiceLineOverlayTask.ServiceLineInfo>> =
            Pair(emptyList(), emptyList())

        every { service.startTimeInSecs } returns 1617187200L
        every { service.serviceTripId } returns "trip_123"
        every { contentResolver.query(any(), any(), any(), any(), any()) } returns cursor
        every { cursor.close() } just Runs

        mockkConstructor(LoadServiceTask::class)
        every { anyConstructed<LoadServiceTask>().call() } returns expectedData

        val testObserver = serviceRepository.loadServices(service, stop).test()

        // Make sure the observable actually emits the expected value
        testObserver.assertValue {
            it.first.isEmpty() && it.second.isEmpty()
        }

        verify { contentResolver.query(any(), any(), any(), any(), any()) }
        verify { cursor.close() }
        verify { anyConstructed<LoadServiceTask>().call() }
    }

}
