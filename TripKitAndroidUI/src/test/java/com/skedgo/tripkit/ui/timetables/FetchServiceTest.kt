package com.skedgo.tripkit.ui.timetables

import android.content.ContentResolver
import android.content.Context
import android.content.res.Resources
import android.text.TextUtils
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.PolyUtil
import com.skedgo.tripkit.common.model.location.Location
import com.skedgo.tripkit.common.model.region.Region
import com.skedgo.tripkit.common.model.stop.ScheduledStop
import com.skedgo.tripkit.data.regions.RegionService
import com.skedgo.tripkit.ui.TripKitUI
import com.skedgo.tripkit.ui.base.MockKTest
import com.skedgo.tripkit.ui.model.StopInfo
import com.skedgo.tripkit.ui.model.TimetableEntry
import com.skedgo.tripkit.ui.utils.ServiceLineOverlayTask.ServiceLineInfo
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.reactivex.Observable
import io.reactivex.observers.TestObserver
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class FetchServiceTest : MockKTest() {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private lateinit var fetchService: FetchService
    private val context: Context = mockk(relaxed = true)
    private val getModeAccessibility: GetModeAccessibility = mockk(relaxed = true)
    private val timetableEntry: TimetableEntry = mockk(relaxed = true)
    private val stop: Location = mockk(relaxed = true)
    private val scheduledStop: ScheduledStop = mockk(relaxed = true)
    private val contentResolver: ContentResolver = mockk(relaxed = true)

    @Before
    fun setup() {
        initRx()
        // Mock Context and ContentResolver
        every { context.contentResolver } returns contentResolver

        // Mock TripKitUI and its RegionService
        val mockRegionService: RegionService = mockk(relaxed = true)
        val mockRegion: Region = mockk(relaxed = true) {
            every { name } returns "Region Name"
        }

        mockkObject(TripKitUI) // Mock TripKitUI singleton
        every { TripKitUI.getInstance().regionService() } returns mockRegionService
        every { mockRegionService.getRegionByLocationAsync(any()) } returns Observable.just(
            mockRegion
        )

        mockkStatic(TextUtils::class)
        every { TextUtils.isEmpty(any()) } answers { firstArg<CharSequence?>().isNullOrEmpty() }


        fetchService = FetchService(context, getModeAccessibility)
    }

    @After
    fun teardown() {
        tearDownRx()
    }

    @Test
    fun `execute should complete successfully`() {
        // Mock Context Resources
        every { context.resources } returns mockk<Resources> {
            every { getString(any()) } returns "Mocked String"
        }

        every { context.getString(any()) } returns "Mocked String"

        every { timetableEntry.serviceTripId } returns "serviceTrip123"
        every { timetableEntry.serviceTime } returns 123456789L

        every { scheduledStop.code } returns "stop123"
        every { scheduledStop.hasChildren() } returns false

        val mockRegion: Region = mockk(relaxed = true) {
            every { name } returns "Region Name"
        }

        every {
            TripKitUI.getInstance().regionService().getRegionByLocationAsync(any())
        } returns Observable.just(mockRegion)

        mockkStatic(PolyUtil::class)
        every { PolyUtil.decode(any()) } returns listOf(
            LatLng(37.7749, -122.4194),
            LatLng(37.7750, -122.4195)
        )

        val testObserver: TestObserver<Pair<List<StopInfo>, List<ServiceLineInfo>>> =
            fetchService.executeWithResponse(timetableEntry, scheduledStop).test()

        testObserver.assertComplete()
        testObserver.assertNoErrors()
    }


}