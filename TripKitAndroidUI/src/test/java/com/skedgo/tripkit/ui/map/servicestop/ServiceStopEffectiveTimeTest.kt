package com.skedgo.tripkit.ui.map.servicestop

import com.google.android.gms.maps.model.MarkerOptions
import com.skedgo.tripkit.common.model.region.Region
import com.skedgo.tripkit.common.model.stop.ScheduledStop
import com.skedgo.tripkit.common.model.stop.ServiceStop
import com.skedgo.tripkit.common.model.stop.StopType
import com.skedgo.tripkit.data.regions.RegionService
import com.skedgo.tripkit.routing.RealTimeVehicle
import com.skedgo.tripkit.ui.base.MockKTest
import com.skedgo.tripkit.ui.model.StopInfo
import com.skedgo.tripkit.ui.model.TimetableEntry
import com.skedgo.tripkit.ui.realtime.RealTimeChoreographerViewModel
import com.skedgo.tripkit.ui.servicedetail.FetchAndLoadServices
import com.skedgo.tripkit.ui.servicedetail.GetStopDisplayText
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll
import io.mockk.verify
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.subjects.PublishSubject
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class ServiceStopEffectiveTimeTest : MockKTest() {
    private val scheduled = 45900L // 12:45 UTC
    private val regionService: RegionService = mockk()
    private val fetch: FetchAndLoadServices = mockk()
    private val text: GetStopDisplayText = mockk()
    private val realtime: RealTimeChoreographerViewModel = mockk()
    private val creator: ServiceStopMarkerCreator = mockk()
    private val source = PublishSubject.create<List<RealTimeVehicle>>()
    private val drawn = mutableListOf<Pair<String, Long>>()
    private lateinit var vm: ServiceStopMapViewModel
    private val selected = ScheduledStop().apply { code = "209592"; type = StopType.BUS }
    private val service = TimetableEntry().apply { serviceTripId = "selected"; stopCode = "209592"; serviceTime = scheduled; startTimeInSecs = scheduled }
    private val stop = ServiceStop().apply { code = "209592"; type = StopType.BUS; setDepartureSecs(scheduled); lat = -33.8; lon = 151.2 }
    private val stops = mutableListOf(StopInfo(1, null, false, stop, 0, false))

    @Before
    fun setup() {
        initRx()
        val region: Region = mockk()
        every { region.timezone } returns "UTC"
        every { regionService.getRegionByLocationAsync(any()) } returns Observable.just(region)
        every { fetch.load(any(), any()) } answers { Single.just(stops.toList() to emptyList()) }
        every { text.execute(any()) } answers { Observable.just(firstArg<ServiceStop>().departureSecs().toString()) }
        every { realtime.getRealTimeVehicles(any(), any()) } returns source
        every { creator.toMarkerOptions(any(), any(), any()) } answers {
            val info = firstArg<StopInfo>()
            drawn += info.stop.code to info.stop.departureSecs()
            mockk<MarkerOptions>()
        }
        vm = ServiceStopMapViewModel(mockk(), fetch, regionService, text)
        vm.realtimeViewModel = realtime
        vm.serviceStopMarkerCreator = creator
    }

    @After
    fun teardown() { vm.onCleared(); tearDownRx(); unmockkAll() }

    private fun vehicle(time: Long, code: String? = selected.code, id: String = service.serviceTripId!!) = RealTimeVehicle().apply {
        serviceTripId = id; startStopCode = code; arriveAtStartStopTime = time
    }
    private fun select(entry: TimetableEntry = service, selection: ScheduledStop = selected) {
        vm.service.accept(entry); vm.stop.accept(selection)
    }

    @Test
    fun `selected marker uses 12 46 prediction instead of 12 45 schedule`() {
        service.realtimeVehicle = vehicle(scheduled + 60)
        val observer = vm.drawStops.test()
        select()
        observer.assertNoErrors()
        assertThat(drawn.last().second).isEqualTo(scheduled + 60)
        assertThat(stop.departureSecs()).isEqualTo(scheduled)
    }
    @Test
    fun `scheduled only retains scheduled pin`() {
        vm.drawStops.test(); select()
        assertThat(drawn.last().second).isEqualTo(scheduled)
    }

    @Test
    fun `delayed early and on time predictions use selected departure`() {
        vm.drawStops.test(); select()
        for (prediction in listOf(scheduled + 360, scheduled - 180, scheduled + 25)) {
            source.onNext(listOf(vehicle(prediction)))
            assertThat(drawn.last().second).isEqualTo(prediction)
        }
        assertThat(stop.departureSecs()).isEqualTo(scheduled)
    }

    @Test
    fun `null zero and sentinel prediction retain scheduled time`() {
        vm.drawStops.test(); select()
        for (prediction in listOf(0L, -1L)) {
            source.onNext(listOf(vehicle(prediction)))
            assertThat(drawn.last().second).isEqualTo(scheduled)
        }
        source.onNext(emptyList())
        assertThat(drawn.last().second).isEqualTo(scheduled)
    }

    @Test
    fun `same service A to B updates same key once and ignores unchanged poll`() {
        val observer = vm.drawStops.test(); select()
        source.onNext(listOf(vehicle(scheduled + 360)))
        source.onNext(listOf(vehicle(scheduled + 240)))
        source.onNext(listOf(vehicle(scheduled + 240)))
        assertThat(drawn.map { it.second }).containsExactly(scheduled, scheduled + 360, scheduled + 240)
        assertThat(drawn.map { it.first }.toSet()).containsExactly(selected.code)
        observer.assertValueCount(3)
        assertThat(observer.values().drop(1).all { it.second.isEmpty() }).isTrue()
    }

    @Test
    fun `prediction disappearance restores original scheduled departure`() {
        vm.drawStops.test(); select()
        source.onNext(listOf(vehicle(scheduled + 360)))
        source.onNext(emptyList())
        assertThat(drawn.map { it.second }).containsExactly(scheduled, scheduled + 360, scheduled)
        assertThat(stop.departureSecs()).isEqualTo(scheduled)
    }

    @Test
    fun `wrong service or origin stop prediction cannot update selected pin`() {
        vm.drawStops.test(); select()
        source.onNext(listOf(vehicle(scheduled + 360, "origin")))
        source.onNext(listOf(vehicle(scheduled + 360, id = "another")))
        assertThat(drawn).containsExactly(selected.code!! to scheduled)
    }

    @Test
    fun `prediction without location reaches selected pin independently of vehicle map`() {
        val map = vm.realtimeVehicles.test()
        vm.drawStops.test(); select()
        source.onNext(listOf(vehicle(scheduled + 360)))
        assertThat(drawn.last().second).isEqualTo(scheduled + 360)
        assertThat(map.values().all { it.isEmpty() }).isTrue()
        verify(exactly = 1) { realtime.getRealTimeVehicles(any(), any()) }
    }

    @Test
    fun `matching raw entry departure is used when carrier prediction is invalid`() {
        service.realTimeDeparture = (scheduled + 240).toInt()
        service.realtimeVehicle = vehicle(0)
        vm.drawStops.test(); select()
        assertThat(drawn.last().second).isEqualTo(scheduled + 240)
    }

    @Test
    fun `same service different selected stop uses its own time not origin`() {
        vm.drawStops.test(); select()
        val otherStop = ServiceStop().apply { code = "other-stop"; type = StopType.BUS; setDepartureSecs(scheduled + 600) }
        stops.clear(); stops += StopInfo(2, null, false, otherStop, 0, false)
        val otherEntry = TimetableEntry().apply { serviceTripId = service.serviceTripId; stopCode = "other-stop"; startTimeInSecs = scheduled + 600 }
        val selection = ScheduledStop().apply { code = "other-stop"; type = StopType.BUS }
        select(otherEntry, selection)
        source.onNext(listOf(vehicle(scheduled + 360)))
        assertThat(drawn.last()).isEqualTo("other-stop" to scheduled + 600)
        source.onNext(listOf(vehicle(scheduled + 660, "other-stop")))
        assertThat(drawn.last()).isEqualTo("other-stop" to scheduled + 660)
    }

    @Test
    fun `service switch ignores previous service carrier`() {
        vm.drawStops.test(); select()
        val other = TimetableEntry().apply { serviceTripId = "other-service"; stopCode = selected.code }
        select(other)
        source.onNext(listOf(vehicle(scheduled + 360)))
        assertThat(drawn.last().second).isEqualTo(scheduled)
        source.onNext(listOf(vehicle(scheduled + 120, id = "other-service")))
        assertThat(drawn.last().second).isEqualTo(scheduled + 120)
    }

    @Test
    fun `resolved child stop is matched by code and route dots stay unchanged`() {
        val dot = ServiceStop().apply { code = "origin"; setDepartureSecs(scheduled - 180) }
        stops.add(0, StopInfo(2, null, false, dot, 0, false))
        val parent = ScheduledStop().apply { code = "parent"; children = arrayListOf(selected) }
        vm.drawStops.test(); select(service, parent)
        source.onNext(listOf(vehicle(scheduled + 360)))
        assertThat(drawn.filter { it.first == "origin" }).containsExactly("origin" to scheduled - 180)
        assertThat(drawn.last()).isEqualTo(selected.code to scheduled + 360)
    }

    @Test
    fun `dispose and reopen disconnect old observer and restore current prediction`() {
        val observer = vm.drawStops.test(); select()
        source.onNext(listOf(vehicle(scheduled + 60)))
        observer.dispose()
        val count = drawn.size
        source.onNext(listOf(vehicle(scheduled + 360)))
        assertThat(drawn).hasSize(count)
        assertThat(source.hasObservers()).isFalse()
        service.realtimeVehicle = vehicle(scheduled + 120)
        val reopened = vm.drawStops.test()
        assertThat(drawn.last().second).isEqualTo(scheduled + 120)
        reopened.dispose()
    }

    @Test
    fun `marker keeps selected coordinates type and region timezone`() {
        vm.drawStops.test(); select()
        source.onNext(listOf(vehicle(scheduled + 60)))
        verify { creator.toMarkerOptions(match {
            it.stop.code == selected.code && it.stop.lat == stop.lat && it.stop.lon == stop.lon &&
                it.stop.type == StopType.BUS && it.realTimeStatus == null && it.stop.departureSecs() == scheduled + 60
        }, (scheduled + 60).toString(), "UTC") }
    }

    @Test
    fun `location only vehicle remains scheduled and coexists with selected pin`() {
        val map = vm.realtimeVehicles.test()
        vm.drawStops.test(); select()
        val tracked = vehicle(0).apply { location = com.skedgo.tripkit.common.model.location.Location(-33.81, 151.21) }
        source.onNext(listOf(tracked))
        map.assertValueAt(1) { it.single() === tracked }
        assertThat(drawn).containsExactly(selected.code!! to scheduled)
        verify(exactly = 1) { realtime.getRealTimeVehicles(any(), any()) }
    }

    @Test
    fun `prediction arriving during initial asynchronous marker creation does not lose route dots`() {
        val delay = PublishSubject.create<String>()
        var calls = 0
        every { text.execute(any()) } answers {
            if (calls++ == 0) delay else Observable.just(firstArg<ServiceStop>().departureSecs().toString())
        }
        val dot = ServiceStop().apply { code = "origin"; setDepartureSecs(scheduled - 180) }
        stops.add(0, StopInfo(2, null, false, dot, 0, false))
        val observer = vm.drawStops.test(); select()
        source.onNext(listOf(vehicle(scheduled + 60)))
        delay.onNext("12:42")
        observer.assertNoErrors().assertValueCount(2)
        assertThat(observer.values().first().first.map { it.second }).containsExactlyInAnyOrder("origin", selected.code)
        assertThat(observer.values().last().first.map { it.second }).containsExactly(selected.code)
        assertThat(drawn.last().second).isEqualTo(scheduled + 60)
    }

    @Test
    fun `queued previous selection marker update is rejected on main thread`() {
        val main = io.reactivex.schedulers.TestScheduler()
        io.reactivex.android.plugins.RxAndroidPlugins.setMainThreadSchedulerHandler { main }
        vm.onCleared()
        vm = ServiceStopMapViewModel(mockk(), fetch, regionService, text).apply {
            realtimeViewModel = realtime; serviceStopMarkerCreator = creator
        }
        val observer = vm.drawStops.test(); select()
        source.onNext(listOf(vehicle(scheduled + 360)))
        val next = TimetableEntry().apply { serviceTripId = "next"; stopCode = selected.code }
        select(next)
        main.triggerActions()
        observer.assertNoErrors().assertValueCount(1)
        source.onNext(listOf(vehicle(scheduled + 240, id = "next")))
        main.triggerActions()
        observer.assertValueCount(2)
        assertThat(drawn.last().second).isEqualTo(scheduled + 240)
    }

    @Test
    fun `marker waits for delayed region when display text completes first`() {
        val delayedRegion = PublishSubject.create<Region>()
        every { regionService.getRegionByLocationAsync(any()) } returns delayedRegion
        val observer = vm.drawStops.test()
        select()
        observer.assertNoErrors().assertNoValues()
        val region = mockk<Region>()
        every { region.timezone } returns "UTC"
        delayedRegion.onNext(region)
        observer.assertNoErrors().assertValueCount(1)
        assertThat(drawn).containsExactly(selected.code!! to scheduled)
    }

}
