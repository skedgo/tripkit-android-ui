package com.skedgo.tripkit.ui.servicedetail

import android.provider.Settings
import com.skedgo.TripKit
import com.skedgo.tripkit.ServiceResponse
import com.skedgo.tripkit.common.model.realtimealert.RealTimeStatus
import com.skedgo.tripkit.common.model.region.Region
import com.skedgo.tripkit.common.model.stop.ScheduledStop
import com.skedgo.tripkit.common.model.stop.ServiceStop
import com.skedgo.tripkit.data.regions.RegionService
import com.skedgo.tripkit.datetime.PrintTime
import com.skedgo.tripkit.routing.RealTimeVehicle
import com.skedgo.tripkit.servicedetail.ServiceDetailRepository
import com.skedgo.tripkit.ui.base.MockKTest
import com.skedgo.tripkit.ui.model.TimetableEntry
import com.skedgo.tripkit.ui.timetables.GetRealtimeText
import io.mockk.*
import io.reactivex.Observable
import com.google.android.gms.maps.model.MarkerOptions
import com.skedgo.tripkit.common.model.stop.StopType
import com.skedgo.tripkit.ui.map.servicestop.ServiceStopMapViewModel
import com.skedgo.tripkit.ui.map.servicestop.ServiceStopMarkerCreator
import com.skedgo.tripkit.ui.model.StopInfo
import com.skedgo.tripkit.ui.realtime.RealTimeChoreographerViewModel
import io.reactivex.Single
import io.reactivex.schedulers.TestScheduler
import io.reactivex.android.plugins.RxAndroidPlugins
import io.reactivex.Flowable
import io.reactivex.subjects.PublishSubject
import org.joda.time.DateTime
import org.joda.time.DateTimeZone
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import javax.inject.Provider

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class ServiceDetailRealtimeRowsTest : MockKTest() {
    private val context get() = RuntimeEnvironment.getApplication()
    private val timezone = DateTimeZone.UTC
    private val scheduled = DateTime.now(timezone).plusDays(1).withTime(12, 45, 0, 0).millis / 1000
    private val updates = PublishSubject.create<Pair<TimetableEntry, List<RealTimeVehicle>>>()
    private val regions: RegionService = mockk()
    private val repository: ServiceDetailRepository = mockk()
    private lateinit var viewModel: ServiceDetailViewModel
    private lateinit var selected: TimetableEntry

    @Before fun setUp() {
        initRx()
        Settings.System.putString(context.contentResolver, Settings.System.TIME_12_24, "24")
        mockkObject(TripKit.Companion)
        every { TripKit.getInstance() } returns mockk(relaxed = true)
        val region: Region = mockk(relaxed = true)
        every { region.timezone } returns timezone.id
        every { regions.getRegionByLocationAsync(any()) } returns Observable.just(region)
        every { repository.getService(any<List<String>>(), any(), any(), any(), any(), any(), any(), any()) } returns Observable.never()
        val printer: PrintTime = mockk()
        every { printer.execute(any()) } answers { Flowable.just(firstArg<DateTime>().toString("HH:mm")) }
        val formatter = GetStopTimeDisplayText(regions, printer)
        viewModel = ServiceDetailViewModel(context, regions, repository, mockk(relaxed = true),
            Provider { ServiceDetailItemViewModel(formatter) }, mockk(relaxed = true),
            GetRealtimeText(context, mockk(relaxed = true)))
        selected = entry()
        viewModel.setup(ScheduledStop().apply { code = "209592" }, selected)
        viewModel.bindRealtimeHeader(updates)
    }

    @After fun tearDown() { viewModel.onCleared(); tearDownRx(); unmockkAll() }

    private fun entry(id: String = "2146889") = TimetableEntry().apply {
        serviceTripId = id; serviceName = "Manly via North Head"; serviceDirection = serviceName
        stopCode = "209592"; startTimeInSecs = scheduled; serviceTime = scheduled
        realTimeStatus = RealTimeStatus.IS_REAL_TIME
    }
    private fun stop(code: String = "209592", time: Long = scheduled) = ServiceStop().apply {
        this.code = code; name = "Stuart St opp Cove Ave"; setDepartureSecs(time)
    }
    private fun rows(vararg stops: ServiceStop) {
        val response: ServiceResponse = mockk()
        every { response.shapes() } returns listOf(mockk {
            every { this@mockk.stops } returns stops.toList()
            every { serviceColor.color } returns android.graphics.Color.BLUE
            every { isTravelled } returns true
        })
        viewModel.processResponse(response, "209592")
    }
    private fun vehicle(time: Long = scheduled, code: String? = "209592", id: String = selected.serviceTripId!!) = RealTimeVehicle().apply {
        serviceTripId = id; startStopCode = code; arriveAtStartStopTime = time
    }
    private fun emit(vararg vehicles: RealTimeVehicle) { updates.onNext(selected to vehicles.toList()) }
    private fun row(index: Int = 0) = viewModel.items.get()!![index]

    @Test fun `route 161 row follows 1245 to 1247 to 1248 without reopening`() {
        val original = stop()
        rows(original)
        emit(vehicle())
        assertEquals("12:45", row().scheduledTime.get())
        emit(vehicle(scheduled + 120))
        assertEquals("12:47", row().scheduledTime.get())
        emit(vehicle(scheduled + 180))
        assertEquals("12:48", row().scheduledTime.get())
        assertEquals(scheduled, original.departureSecs())
        assertEquals(scheduled, selected.startTimeInSecs)
    }
    @Test fun `scheduled only preserves original arrival and departure`() {
        val original = stop().apply { arrivalTime = scheduled - 30 }
        rows(original)
        emit()
        assertEquals("12:45", row().scheduledTime.get())
        assertEquals(scheduled, row().originalDepartureSecs)
        assertEquals(scheduled - 30, row().originalArrivalSecs)
        assertNull(row().predictedTimeSecs)
    }
    @Test fun `early prediction displays effective time`() {
        rows(stop()); emit(vehicle(scheduled - 120))
        assertEquals("12:43", row().scheduledTime.get())
    }
    @Test fun `on time prediction is retained separately`() {
        rows(stop()); emit(vehicle(scheduled + 24))
        assertEquals("12:45", row().scheduledTime.get())
        assertEquals(scheduled + 24, row().predictedTimeSecs)
    }
    private fun invalid(time: Long?) {
        rows(stop()); emit(vehicle(scheduled + 120))
        assertEquals("12:47", row().scheduledTime.get())
        if (time == null) emit() else emit(vehicle(time))
        assertEquals("12:45", row().scheduledTime.get())
        assertNull(row().predictedTimeSecs)
    }
    @Test fun `null prediction falls back after realtime`() = invalid(null)
    @Test fun `zero prediction falls back after realtime`() = invalid(0)
    @Test fun `sentinel prediction falls back after realtime`() = invalid(-1)
    @Test fun `location only carrier stays scheduled`() {
        rows(stop()); emit(vehicle(-1).apply { location = com.skedgo.tripkit.common.model.location.Location() })
        assertEquals("12:45", row().scheduledTime.get())
    }
    @Test fun `wrong stop and wrong service cannot supply prediction`() {
        rows(stop()); emit(vehicle(scheduled + 120, "other"), vehicle(scheduled + 180, id = "old"))
        assertEquals("12:45", row().scheduledTime.get())
    }
    @Test fun `duplicate names and intermediate rows do not inherit selected delay`() {
        rows(stop(), stop("different", scheduled + 60))
        val originalRows = viewModel.items.get()
        emit(vehicle(scheduled + 120))
        assertEquals("12:47", row().scheduledTime.get())
        assertEquals("12:46", row(1).scheduledTime.get())
        assertSame(originalRows, viewModel.items.get())
    }
    @Test fun `parent and child codes are not aliases`() {
        rows(stop("parent"), stop("209592"))
        emit(vehicle(scheduled + 120))
        assertEquals("12:45", row().scheduledTime.get())
        assertEquals("12:47", row(1).scheduledTime.get())
        emit(vehicle(scheduled + 180, "parent"))
        assertEquals("12:45", row().scheduledTime.get())
        assertEquals("12:45", row(1).scheduledTime.get())
    }
    @Test fun `repeated stop code selects only original scheduled visit`() {
        rows(stop(time = scheduled - 600), stop(), stop(time = scheduled + 600))
        emit(vehicle(scheduled + 120))
        assertEquals("12:35", row().scheduledTime.get())
        assertEquals("12:47", row(1).scheduledTime.get())
        assertEquals("12:55", row(2).scheduledTime.get())
    }
    @Test fun `ambiguous repeated visit is left scheduled`() {
        rows(stop(), stop())
        emit(vehicle(scheduled + 120))
        assertEquals("12:45", row().scheduledTime.get())
        assertEquals("12:45", row(1).scheduledTime.get())
    }
    @Test fun `service switch rejects old emission and retains new prediction`() {
        val old = selected
        rows(stop()); emit(vehicle(scheduled + 120))
        selected = entry("new-service")
        viewModel.setup(ScheduledStop().apply { code = "209592" }, selected)
        rows(stop())
        updates.onNext(old to listOf(vehicle(scheduled + 180, id = old.serviceTripId!!)))
        assertEquals("12:45", row().scheduledTime.get())
        emit(vehicle(scheduled - 120))
        assertEquals("12:43", row().scheduledTime.get())
    }
    @Test fun `realtime arriving before service rows is applied when rows load`() {
        emit(vehicle(scheduled + 180)); rows(stop())
        assertEquals("12:48", row().scheduledTime.get())
    }
    @Test fun `entry departure prediction has same precedence as header`() {
        rows(stop()); selected.realTimeDeparture = (scheduled + 120).toInt()
        emit(vehicle(0))
        assertEquals("12:47", row().scheduledTime.get())
        assertTrue(viewModel.secondaryText.get()!!.contains("12:47"))
        emit(vehicle(scheduled + 180))
        assertEquals("12:48", row().scheduledTime.get())
        assertTrue(viewModel.secondaryText.get()!!.contains("12:48"))
        selected.realTimeDeparture = -1; emit()
        assertEquals("12:45", row().scheduledTime.get())
    }
    @Test fun `identified endpoint arrival updates independently without propagating delay`() {
        selected.endStopCode = "destination"; selected.endTimeInSecs = scheduled + 600
        val destination = stop("destination", 0).apply { arrivalTime = scheduled + 600 }
        rows(stop(), stop("intermediate", scheduled + 300), destination)
        emit(vehicle(scheduled + 120).apply { endStopCode = "destination"; arriveAtEndStopTime = scheduled + 660 })
        assertEquals("12:47", row().scheduledTime.get())
        assertEquals("12:50", row(1).scheduledTime.get())
        assertEquals("12:56", row(2).scheduledTime.get())
        assertEquals(scheduled + 600, destination.arrivalTime)
        emit(vehicle(scheduled + 180).apply { endStopCode = "wrong-end"; arriveAtEndStopTime = scheduled + 780 })
        assertEquals("12:48", row().scheduledTime.get())
        assertEquals("12:55", row(2).scheduledTime.get())
    }
    @Test fun `arrival row does not consume a departure prediction`() {
        rows(stop(time = 0).apply { arrivalTime = scheduled })
        emit(vehicle(scheduled + 120))
        assertEquals("12:45", row().scheduledTime.get())
    }
    @Test fun `end prediction does not replace a row displaying departure`() {
        selected.endStopCode = "destination"; selected.endTimeInSecs = scheduled + 600
        rows(stop("destination", scheduled + 630).apply { arrivalTime = scheduled + 600 })
        emit(vehicle().apply { endStopCode = "destination"; arriveAtEndStopTime = scheduled + 720 })
        assertEquals("12:55", row().scheduledTime.get())
    }
    @Test fun `raw identified end arrival falls back when removed`() {
        selected.endStopCode = "destination"; selected.endTimeInSecs = scheduled + 600
        rows(stop("destination", 0).apply { arrivalTime = scheduled + 600 })
        selected.realTimeArrival = (scheduled + 660).toInt(); emit()
        assertEquals("12:56", row().scheduledTime.get())
        selected.realTimeArrival = -1; emit()
        assertEquals("12:55", row().scheduledTime.get())
    }
    @Test fun `unbind rebind and clear keep one shared observer and stop row changes`() {
        rows(stop())
        var active = 0
        val counted = updates.doOnSubscribe { active++ }.doFinally { active-- }
        viewModel.bindRealtimeHeader(counted); viewModel.bindRealtimeHeader(counted)
        assertEquals(1, active)
        emit(vehicle(scheduled + 120))
        viewModel.unbindRealtimeHeader(); assertEquals(0, active)
        emit(vehicle(scheduled + 180)); assertEquals("12:47", row().scheduledTime.get())
        viewModel.bindRealtimeHeader(counted); assertEquals(1, active)
        emit(vehicle(scheduled + 180)); assertEquals("12:48", row().scheduledTime.get())
        viewModel.onCleared(); assertEquals(0, active)
        emit(vehicle(scheduled - 120)); assertEquals("12:48", row().scheduledTime.get())
    }
    @Test fun `delayed old service response cannot replace new service rows`() {
        val oldResponse = PublishSubject.create<ServiceResponse>()
        every { repository.getService(any<List<String>>(), any(), any(), any(), any(), any(), any(), any()) } returns oldResponse
        viewModel.setup(ScheduledStop().apply { code = "209592" }, selected)
        assertTrue(oldResponse.hasObservers())
        every { repository.getService(any<List<String>>(), any(), any(), any(), any(), any(), any(), any()) } returns Observable.never()
        selected = entry("new-service")
        viewModel.setup(ScheduledStop().apply { code = "209592" }, selected)
        assertFalse(oldResponse.hasObservers())
        rows(stop()); emit(vehicle(scheduled + 180))
        val old: ServiceResponse = mockk(relaxed = true)
        oldResponse.onNext(old)
        assertEquals("12:48", row().scheduledTime.get())
    }

    @Test fun `header pin and row share one producer and agree through updates and fallback`() {
        val original = stop().apply { type = StopType.BUS }
        rows(original)
        val source = PublishSubject.create<List<RealTimeVehicle>>()
        val fetch: FetchAndLoadServices = mockk()
        val text: GetStopDisplayText = mockk()
        val realtime: RealTimeChoreographerViewModel = mockk()
        val creator: ServiceStopMarkerCreator = mockk()
        var pinTime = 0L
        every { fetch.load(any(), any()) } returns Single.just(listOf(StopInfo(1, null, false, original, 0, false)) to emptyList())
        every { text.execute(any()) } answers { Observable.just(firstArg<ServiceStop>().departureSecs().toString()) }
        every { realtime.getRealTimeVehicles(any(), any()) } returns source
        every { creator.toMarkerOptions(any(), any(), any()) } answers {
            pinTime = firstArg<StopInfo>().stop.departureSecs()
            mockk<MarkerOptions>()
        }
        val map = ServiceStopMapViewModel(mockk(), fetch, regions, text)
        map.realtimeViewModel = realtime
        map.serviceStopMarkerCreator = creator
        val draw = map.drawStops.test()
        viewModel.bindRealtimeHeader(map.realtimeServiceUpdates)
        map.service.accept(selected)
        map.stop.accept(ScheduledStop().apply { code = "209592"; type = StopType.BUS })
        try {
            for ((delta, display) in listOf(0L to "12:45", 120L to "12:47", 180L to "12:48", -120L to "12:43")) {
                source.onNext(listOf(vehicle(scheduled + delta)))
                assertEquals(scheduled + delta, pinTime)
                assertEquals(display, row().scheduledTime.get())
                assertTrue(viewModel.secondaryText.get()!!.contains(display))
            }
            source.onNext(emptyList())
            assertEquals(scheduled, pinTime)
            assertEquals("12:45", row().scheduledTime.get())
            assertTrue(viewModel.secondaryText.get()!!.contains("12:45"))
            verify(exactly = 1) { realtime.getRealTimeVehicles(any(), any()) }
            draw.assertNoErrors()
        } finally { draw.dispose(); map.onCleared() }
    }
    @Test fun `queued update is ignored after service switch and row updates on main scheduler`() {
        rows(stop())
        val main = TestScheduler()
        RxAndroidPlugins.setMainThreadSchedulerHandler { main }
        viewModel.bindRealtimeHeader(updates)
        val oldRow = row()
        emit(vehicle(scheduled + 120))
        assertEquals("12:45", oldRow.scheduledTime.get())
        selected = entry("new-service")
        viewModel.setup(ScheduledStop().apply { code = "209592" }, selected)
        main.triggerActions()
        rows(stop()); main.triggerActions()
        assertEquals("12:45", oldRow.scheduledTime.get())
        assertEquals("12:45", row().scheduledTime.get())
        emit(vehicle(scheduled + 180))
        assertEquals("12:45", row().scheduledTime.get())
        main.triggerActions()
        assertEquals("12:48", row().scheduledTime.get())
    }
    @Test fun `row uses existing region timezone for prediction and fallback`() {
        val region: Region = mockk(relaxed = true)
        every { region.timezone } returns "+11:00"
        every { regions.getRegionByLocationAsync(any()) } returns Observable.just(region)
        rows(stop())
        assertEquals("23:45", row().scheduledTime.get())
        emit(vehicle(scheduled + 120))
        assertEquals("23:47", row().scheduledTime.get())
        emit()
        assertEquals("23:45", row().scheduledTime.get())
    }

}
