package com.skedgo.tripkit.ui.timetables

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.Polyline
import com.google.android.gms.maps.model.PolylineOptions
import com.skedgo.tripkit.common.model.stop.ScheduledStop
import com.skedgo.tripkit.ui.TripKitUI
import com.skedgo.tripkit.ui.base.MockKTest
import com.skedgo.tripkit.ui.core.module.ServiceStopMapComponent
import com.skedgo.tripkit.ui.map.servicestop.ServiceStopMapViewModel
import com.skedgo.tripkit.ui.model.TimetableEntry
import com.skedgo.tripkit.ui.realtime.RealTimeViewModelFactory
import io.mockk.*
import io.reactivex.Observable
import io.reactivex.subjects.BehaviorSubject
import org.junit.*
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import javax.inject.Provider

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class TimetableMapContributorTest : MockKTest() {
    private val activity = Robolectric.buildActivity(FragmentActivity::class.java).setup()
    private val fragment = Fragment()
    private lateinit var contributor: TimetableMapContributor
    private val model: ServiceStopMapViewModel = mockk(relaxed = true)
    private val stops = BehaviorSubject.createDefault(listOf(MarkerOptions().position(LatLng(-33.8, 151.2)) to "stop") to emptySet<String>())
    private val lines = BehaviorSubject.createDefault(listOf<PolylineOptions>(PolylineOptions().add(LatLng(-33.8, 151.2), LatLng(-33.9, 151.3))))

    @Before fun setup() {
        initRx()
        activity.get().supportFragmentManager.beginTransaction().add(fragment, "service").commitNow()
        contributor = TimetableMapContributor(fragment)
        every { model.drawStops } returns stops
        every { model.drawServiceLine } returns lines
        every { model.realtimeVehicles } returns Observable.just(emptyList())
        mockkObject(TripKitUI.Companion)
        val ui: TripKitUI = mockk()
        val component: ServiceStopMapComponent = mockk()
        every { TripKitUI.getInstance() } returns ui
        every { ui.serviceStopMapComponent() } returns component
        every { component.inject(contributor) } answers {
            contributor.viewModel = model
            contributor.realTimeViewModelFactory = RealTimeViewModelFactory(Provider { mockk(relaxed = true) })
        }
    }
    @After fun teardown() {
        contributor.cleanup()
        activity.pause().stop().destroy()
        unmockkAll()
        tearDownRx()
    }
    private fun map(): GoogleMap = mockk<GoogleMap>(relaxed = true).also { map ->
        every { map.cameraPosition } returns CameraPosition.fromLatLngZoom(LatLng(-33.8, 151.2), 12f)
        every { map.addMarker(any()) } returns mockk<Marker>(relaxed = true).also {
            every { it.position } returns LatLng(-33.8, 151.2)
        }
        every { map.addPolyline(any()) } returns mockk<Polyline>(relaxed = true)
    }
    @Test fun `ready map before injection renders retained stops and route after initialize`() {
        val map = map()
        contributor.safeToUseMap(activity.get(), map)
        contributor.initialize()
        verify(exactly = 1) { map.addMarker(any()); map.addPolyline(any()) }
    }
    @Test fun `data arriving after map readiness renders and same map attachment is idempotent`() {
        stops.onNext(emptyList<Pair<MarkerOptions, String>>() to emptySet())
        lines.onNext(emptyList())
        contributor.initialize()
        val map = map()
        contributor.safeToUseMap(activity.get(), map)
        stops.onNext(listOf(MarkerOptions().position(LatLng(-33.8, 151.2)) to "stop") to emptySet())
        lines.onNext(listOf(PolylineOptions().add(LatLng(-33.8, 151.2))))
        contributor.safeToUseMap(activity.get(), map)
        verify(exactly = 1) { map.addMarker(any()); map.addPolyline(any()) }
    }
    @Test fun `replacement map gets fresh owned markers instead of updating previous map handles`() {
        contributor.initialize()
        val first = map()
        val second = map()
        contributor.safeToUseMap(activity.get(), first)
        contributor.safeToUseMap(activity.get(), second)
        verify(exactly = 1) { second.addMarker(any()); second.addPolyline(any()) }
    }
    @Test fun `cleanup and reattachment render retained service without duplicate observers`() {
        contributor.initialize()
        val map = map()
        contributor.safeToUseMap(activity.get(), map)
        contributor.cleanup()
        contributor.safeToUseMap(activity.get(), map)
        verify(exactly = 2) { map.addMarker(any()); map.addPolyline(any()) }
    }
}
