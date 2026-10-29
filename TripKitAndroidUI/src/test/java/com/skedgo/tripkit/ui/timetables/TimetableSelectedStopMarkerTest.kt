package com.skedgo.tripkit.ui.timetables

import androidx.fragment.app.Fragment
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.skedgo.tripkit.ui.base.MockKTest
import com.skedgo.tripkit.ui.map.servicestop.ServiceStopMapViewModel
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.mockk.verify
import io.reactivex.Observable
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
class TimetableSelectedStopMarkerTest : MockKTest() {
    private val updates = PublishSubject.create<Pair<List<Pair<MarkerOptions, String>>, Set<String>>>()
    private val map: GoogleMap = mockk(relaxed = true)
    private val marker: Marker = mockk(relaxed = true)
    private val vm: ServiceStopMapViewModel = mockk(relaxed = true)
    private val coordinate = LatLng(-33.80411, 151.28566)
    private lateinit var contributor: TimetableMapContributor

    @Before
    fun setup() {
        initRx()
        mockkStatic(CameraUpdateFactory::class)
        every { CameraUpdateFactory.newLatLngZoom(any(), any()) } returns mockk()
        every { CameraUpdateFactory.newLatLngBounds(any(), any<Int>()) } returns mockk()
        every { marker.position } returns coordinate
        every { map.addMarker(any()) } returns marker
        every { vm.drawStops } returns updates
        every { vm.drawServiceLine } returns Observable.never()
        every { vm.realtimeVehicles } returns Observable.never()
        contributor = TimetableMapContributor(mockk<Fragment>(relaxed = true))
        contributor.viewModel = vm
        contributor.safeToUseMap(mockk(), map)
    }

    @After
    fun teardown() { contributor.cleanup(); tearDownRx(); unmockkAll() }

    private fun update(time: String, icon: BitmapDescriptor = mockk()) {
        val options = MarkerOptions().position(coordinate).title("Selected stop").snippet(time)
            .icon(icon).anchor(0.6f, 1f).infoWindowAnchor(0.6f, 0f)
        updates.onNext(listOf(options to "209592") to emptySet())
    }

    @Test
    fun `same key time changes update one existing marker without camera or coordinate changes`() {
        update("12:45")
        val icon: BitmapDescriptor = mockk()
        update("12:46", icon); update("12:51"); update("12:45")
        verify(exactly = 1) { map.addMarker(any()) }
        verify(exactly = 1) { map.animateCamera(any()) }
        verify(exactly = 0) { marker.position = any() }
        verify(exactly = 0) { marker.remove() }
        verify { marker.setIcon(icon); marker.snippet = "12:46" }
        assertThat(marker.position).isEqualTo(coordinate)
    }

    @Test
    fun `cleanup disposes marker updates and removes existing marker once`() {
        update("12:45")
        contributor.cleanup()
        assertThat(updates.hasObservers()).isFalse()
        update("12:46")
        verify(exactly = 1) { map.addMarker(any()) }
        verify(exactly = 1) { marker.remove() }
        verify(exactly = 0) { marker.snippet = "12:46" }
    }

    @Test
    fun `time updates preserve visible selected stop callout`() {
        update("12:45")
        every { marker.isInfoWindowShown } returns true
        update("12:46")
        verify(exactly = 1) { marker.showInfoWindow() }
        verify { marker.setAnchor(0.6f, 1f); marker.setInfoWindowAnchor(0.6f, 0f) }
    }
    @Test
    fun `map reattachment replaces marker observers instead of duplicating updates`() {
        update("12:45")
        contributor.safeToUseMap(mockk(), map)
        update("12:46")
        verify(exactly = 1) { marker.snippet = "12:46" }
        verify(exactly = 1) { map.addMarker(any()) }
    }

    @Test
    fun `discarded selection removal does not terminate subsequent marker updates`() {
        updates.onNext(emptyList<Pair<MarkerOptions, String>>() to setOf("never-rendered"))
        update("12:45")
        verify(exactly = 1) { map.addMarker(any()) }
    }

}
