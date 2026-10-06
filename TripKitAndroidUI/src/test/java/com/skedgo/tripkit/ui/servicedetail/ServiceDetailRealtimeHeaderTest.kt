package com.skedgo.tripkit.ui.servicedetail

import android.provider.Settings
import androidx.core.content.ContextCompat
import com.skedgo.TripKit
import com.skedgo.tripkit.common.model.realtimealert.RealTimeStatus
import com.skedgo.tripkit.common.model.region.Region
import com.skedgo.tripkit.common.model.stop.ScheduledStop
import com.skedgo.tripkit.data.regions.RegionService
import com.skedgo.tripkit.routing.RealTimeVehicle
import com.skedgo.tripkit.servicedetail.ServiceDetailRepository
import com.skedgo.tripkit.ui.R
import com.skedgo.tripkit.ui.base.MockKTest
import com.skedgo.tripkit.ui.model.TimetableEntry
import com.skedgo.tripkit.ui.timetables.GetRealtimeText
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkAll
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import io.reactivex.schedulers.TestScheduler
import io.reactivex.android.plugins.RxAndroidPlugins
import org.joda.time.DateTime
import org.joda.time.DateTimeZone
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class ServiceDetailRealtimeHeaderTest : MockKTest() {
    private val context get() = RuntimeEnvironment.getApplication()
    private lateinit var viewModel: ServiceDetailViewModel
    private val regionService: RegionService = mockk()
    private val repository: ServiceDetailRepository = mockk()
    private val stop = ScheduledStop().apply { code = "209592" }
    private val scheduled = DateTime.now(DateTimeZone.UTC).plusDays(1).withTime(10, 45, 0, 0).millis / 1000
    private val updates = PublishSubject.create<Pair<TimetableEntry, List<RealTimeVehicle>>>()

    @Before
    fun setUp() {
        initRx()
        Settings.System.putString(context.contentResolver, Settings.System.TIME_12_24, "24")
        mockkObject(TripKit.Companion)
        every { TripKit.getInstance() } returns mockk(relaxed = true)
        val region: Region = mockk(relaxed = true)
        every { region.timezone } returns "UTC"
        every { regionService.getRegionByLocationAsync(any()) } returns Observable.just(region)
        every { repository.getService(any<List<String>>(), any(), any(), any(), any(), any(), any(), any()) } returns Observable.never()
        viewModel = ServiceDetailViewModel(context, regionService, repository, mockk(relaxed = true),
            mockk(), mockk(relaxed = true), GetRealtimeText(context, mockk(relaxed = true)))
    }

    @After
    fun tearDown() {
        viewModel.onCleared()
        tearDownRx()
        unmockkAll()
    }

    private fun entry() = TimetableEntry().apply {
        serviceTripId = "2146986"
        serviceName = "Manly via North Head"
        serviceDirection = serviceName
        stopCode = stop.code
        startTimeInSecs = scheduled
        serviceTime = scheduled
        realTimeStatus = RealTimeStatus.IS_REAL_TIME
        realtimeVehicle = vehicle(6)
    }

    private fun vehicle(minutes: Int) = RealTimeVehicle().apply {
        serviceTripId = "2146986"
        startStopCode = stop.code
        arriveAtStartStopTime = scheduled + minutes * 60
    }

    private fun open(selected: TimetableEntry) {
        viewModel.setup(stop, selected)
        viewModel.bindRealtimeHeader(updates)
    }

    private fun emit(selected: TimetableEntry, vararg vehicles: RealTimeVehicle) {
        updates.onNext(selected to vehicles.toList())
    }

    @Test
    fun `same service prediction changes header without reopening detail`() {
        val selected = entry()
        open(selected)
        assertTrue(viewModel.secondaryText.get().orEmpty().contains("10:51"))
        assertEquals(ContextCompat.getColor(context, R.color.tripKitError), viewModel.secondaryTextColor.get())

        emit(selected, vehicle(4))

        assertTrue("Expected live header 10:49, was ${viewModel.secondaryText.get()}",
            viewModel.secondaryText.get().orEmpty().contains("10:49"))
        assertEquals(scheduled, selected.serviceTime)
        assertEquals(scheduled + 360, selected.realtimeVehicle!!.arriveAtStartStopTime)
    }

    @Test
    fun `scheduled detail becomes realtime when prediction arrives without reopening`() {
        val selected = entry().apply {
            realTimeStatus = RealTimeStatus.CAPABLE
            realtimeVehicle = null
        }
        open(selected)
        assertEquals("Scheduled • 10:45", viewModel.secondaryText.get())

        emit(selected, vehicle(4))

        assertEquals("4 mins late • 10:49", viewModel.secondaryText.get())
        assertEquals(ContextCompat.getColor(context, R.color.tripKitError), viewModel.secondaryTextColor.get())
    }

    @Test
    fun `same id late prediction transitions through on time and early colors`() {
        val selected = entry()
        open(selected)
        emit(selected, vehicle(0))
        assertEquals("On time • 10:45", viewModel.secondaryText.get())
        assertEquals(ContextCompat.getColor(context, R.color.tripKitSuccess), viewModel.secondaryTextColor.get())
        emit(selected, vehicle(-2))
        assertEquals("2 mins early • 10:43", viewModel.secondaryText.get())
        assertEquals(ContextCompat.getColor(context, R.color.tripKitWarning), viewModel.secondaryTextColor.get())
    }

    @Test
    fun `future capable without prediction stays scheduled including location only vehicle`() {
        val selected = entry().apply { realTimeStatus = RealTimeStatus.CAPABLE; realtimeVehicle = null }
        open(selected)
        emit(selected)
        assertEquals("Scheduled • 10:45", viewModel.secondaryText.get())
        emit(selected, vehicle(0).apply { arriveAtStartStopTime = -1 })
        assertEquals("Scheduled • 10:45", viewModel.secondaryText.get())
        assertEquals(ContextCompat.getColor(context, R.color.black1), viewModel.secondaryTextColor.get())
    }

    @Test
    fun `missing or invalid prediction uses existing realtime fallback instead of old vehicle`() {
        val selected = entry()
        open(selected)
        emit(selected)
        assertEquals("On time • 10:45", viewModel.secondaryText.get())
        emit(selected, vehicle(0).apply { arriveAtStartStopTime = -1 })
        assertEquals("On time • 10:45", viewModel.secondaryText.get())
        selected.realTimeDeparture = (scheduled + 120).toInt()
        emit(selected)
        assertEquals("2 mins late • 10:47", viewModel.secondaryText.get())
    }

    @Test
    fun `cancelled service stays cancelled after live prediction`() {
        val selected = entry().apply { isCancelled = true }
        open(selected)
        emit(selected, vehicle(0))
        assertEquals("CANCELLED", viewModel.secondaryText.get())
        assertEquals(ContextCompat.getColor(context, R.color.tripKitError), viewModel.secondaryTextColor.get())
    }

    @Test
    fun `old service and wrong stop vehicle cannot change new selection header`() {
        val old = entry()
        open(old)
        val selected = entry().apply { serviceTripId = "new-service" }
        open(selected)
        val initial = viewModel.secondaryText.get()
        emit(old, vehicle(-2))
        assertEquals(initial, viewModel.secondaryText.get())
        emit(selected, vehicle(-2), vehicle(4).apply {
            serviceTripId = "new-service"; startStopCode = "other-stop"
        })
        assertEquals("On time • 10:45", viewModel.secondaryText.get())
    }

    @Test
    fun `unbind stops mutations and rebind replaces subscription`() {
        val selected = entry()
        open(selected)
        var active = 0
        val countedUpdates = updates.doOnSubscribe { active++ }.doFinally { active-- }
        viewModel.bindRealtimeHeader(countedUpdates)
        viewModel.bindRealtimeHeader(countedUpdates)
        assertEquals(1, active)
        assertTrue(updates.hasObservers())
        viewModel.unbindRealtimeHeader()
        assertEquals(0, active)
        assertTrue(!updates.hasObservers())
        emit(selected, vehicle(-2))
        assertTrue(viewModel.secondaryText.get().orEmpty().contains("10:51"))
        viewModel.bindRealtimeHeader(updates)
        emit(selected, vehicle(4))
        assertEquals("4 mins late • 10:49", viewModel.secondaryText.get())
        viewModel.onCleared()
        assertTrue(!updates.hasObservers())
        emit(selected, vehicle(-2))
        assertEquals("4 mins late • 10:49", viewModel.secondaryText.get())
    }

    @Test
    fun `queued old selection updates are ignored on main thread`() {
        val old = entry()
        open(old)
        val main = TestScheduler()
        RxAndroidPlugins.setMainThreadSchedulerHandler { main }
        viewModel.bindRealtimeHeader(updates)
        emit(old, vehicle(-2))
        val selected = entry().apply { serviceTripId = "new-service" }
        viewModel.setup(stop, selected)
        main.triggerActions()
        assertTrue(viewModel.secondaryText.get().orEmpty().contains("10:51"))
        emit(selected, vehicle(4).apply { serviceTripId = "new-service" })
        assertTrue(viewModel.secondaryText.get().orEmpty().contains("10:51"))
        main.triggerActions()
        assertEquals("4 mins late • 10:49", viewModel.secondaryText.get())
    }

    @Test
    fun `prediction arriving before timezone resolution is retained`() {
        val regions = PublishSubject.create<Region>()
        every { regionService.getRegionByLocationAsync(any()) } returns regions
        val selected = entry()
        open(selected)
        emit(selected, vehicle(4))
        val region: Region = mockk(relaxed = true)
        every { region.timezone } returns "UTC"
        regions.onNext(region)
        assertEquals("4 mins late • 10:49", viewModel.secondaryText.get())
    }
}
