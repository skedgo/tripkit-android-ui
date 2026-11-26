package com.skedgo.tripkit.ui.tripresult

import com.skedgo.tripkit.common.model.location.Location
import com.skedgo.tripkit.routing.TripSegment
import io.mockk.every
import io.mockk.mockk
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class TripMapReadinessTest {
    @Test fun `segment focus before map readiness is retained without accessing an uninitialized map`() {
        val segment = mockk<TripSegment>(relaxed = true)
        every { segment.from } returns Location(-33.87, 151.20)
        every { segment.to } returns Location(-33.89, 151.27)
        TripResultMapContributor().focusTripLine(segment)
    }

    @Test fun `segment with no geometry or locations is a safe no-op`() {
        val segment = mockk<TripSegment>(relaxed = true)
        every { segment.from } returns null
        every { segment.to } returns null
        every { segment.singleLocation } returns null
        TripResultMapContributor().focusTripLine(segment)
    }
    @org.junit.After fun tearDown() = io.mockk.unmockkAll()

    private fun segment(id: Long, latitude: Double): TripSegment {
        val segment = mockk<TripSegment>(relaxed = true)
        every { segment.segmentId } returns id
        every { segment.from } returns Location(latitude, 151.20)
        every { segment.to } returns Location(latitude + 0.01, 151.27)
        every { segment.singleLocation } returns null
        every { segment.isVisibleInContext(any()) } returns true
        every { segment.shapes } returns arrayListOf()
        every { segment.streets } returns arrayListOf()
        return segment
    }

    private fun ready(contributor: TripResultMapContributor, segments: List<TripSegment>): MutableList<com.google.android.gms.maps.model.LatLngBounds> {
        val context = androidx.test.core.app.ApplicationProvider.getApplicationContext<android.content.Context>()
        val host = android.widget.FrameLayout(context).apply { layout(0, 0, 1080, 2000) }
        contributor.mapViewportView = host
        val trip = mockk<com.skedgo.tripkit.routing.Trip>(relaxed = true)
        every { trip.segmentList } returns ArrayList(segments)
        every { trip.uuid } returns "selected"
        every { trip.saveURL } returns "selected"
        contributor.viewModel = mockk(relaxed = true)
        every { contributor.viewModel.selectedTrip } returns com.jakewharton.rxrelay2.BehaviorRelay.createDefault(trip)
        val map = mockk<com.google.android.gms.maps.GoogleMap>(relaxed = true)
        val projection = mockk<com.google.android.gms.maps.Projection>()
        every { map.projection } returns projection
        val sw = com.google.android.gms.maps.model.LatLng(-35.0, 150.0)
        val ne = com.google.android.gms.maps.model.LatLng(-30.0, 155.0)
        every { projection.visibleRegion } returns com.google.android.gms.maps.model.VisibleRegion(sw,
            com.google.android.gms.maps.model.LatLng(-35.0, 155.0), com.google.android.gms.maps.model.LatLng(-30.0, 150.0), ne,
            com.google.android.gms.maps.model.LatLngBounds(sw, ne))
        every { map.cameraPosition } returns com.google.android.gms.maps.model.CameraPosition.fromLatLngZoom(com.google.android.gms.maps.model.LatLng(-32.5, 152.5), 10f)
        every { projection.toScreenLocation(any()) } answers {
            val point = firstArg<com.google.android.gms.maps.model.LatLng>()
            android.graphics.Point(((point.longitude - 150) / 5 * 1080).toInt(), ((-30 - point.latitude) / 5 * 2000).toInt())
        }
        listOf("context" to context, "map" to map, "isSafeToUse" to true).forEach { (name, value) ->
            contributor.javaClass.getDeclaredField(name).apply { isAccessible = true }.set(contributor, value)
        }
        io.mockk.mockkStatic(com.google.android.gms.maps.CameraUpdateFactory::class)
        val fits = mutableListOf<com.google.android.gms.maps.model.LatLngBounds>()
        every { com.google.android.gms.maps.CameraUpdateFactory.newLatLngBounds(any(), any<Int>()) } answers {
            fits.add(firstArg()); mockk(relaxed = true)
        }
        return fits
    }

    @Test fun `initial details camera includes route bends and does not refit unchanged layout`() {
        val segment = segment(1, -33.9)
        val street = mockk<com.skedgo.tripkit.common.model.Street>()
        val bend = com.google.android.gms.maps.model.LatLng(-34.2, 151.4)
        every { street.encodedWaypoints() } returns com.google.maps.android.PolyUtil.encode(listOf(bend))
        every { segment.streets } returns arrayListOf(street)
        val contributor = TripResultMapContributor()
        val fits = ready(contributor, listOf(segment))
        contributor.onViewportChanged()
        org.junit.Assert.assertTrue(fits.single().contains(bend))
        contributor.onViewportChanged()
        org.junit.Assert.assertEquals(1, fits.size)
    }

    @Test fun `latest focus wins when rapid selection precedes map readiness`() {
        val first = segment(1, -33.9)
        val last = segment(2, -31.0)
        val contributor = TripResultMapContributor()
        contributor.focusTripLine(first)
        contributor.focusTripLine(last)
        val fits = ready(contributor, listOf(first, last))
        contributor.onViewportChanged()
        org.junit.Assert.assertTrue(fits.single().contains(com.google.android.gms.maps.model.LatLng(-31.0, 151.20)))
        org.junit.Assert.assertFalse(fits.single().contains(com.google.android.gms.maps.model.LatLng(-33.9, 151.20)))
        contributor.onViewportChanged()
        org.junit.Assert.assertEquals(1, fits.size)
    }

    @Test fun `whole trip updates cannot overwrite focused segment and missing geometry keeps camera`() {
        val first = segment(1, -33.9)
        val last = segment(2, -31.0)
        val empty = segment(3, -32.0)
        every { empty.from } returns null
        every { empty.to } returns null
        val contributor = TripResultMapContributor()
        val fits = ready(contributor, listOf(first, last, empty))
        contributor.focusTripLine(first)
        contributor.focusTripLine(last)
        contributor.onViewportChanged()
        org.junit.Assert.assertEquals(2, fits.size)
        org.junit.Assert.assertFalse(fits.last().contains(com.google.android.gms.maps.model.LatLng(-33.9, 151.20)))
        contributor.focusTripLine(empty)
        org.junit.Assert.assertEquals(2, fits.size)
    }

    @Test fun `public segment focus still works without a preselected trip`() {
        val segment = segment(1, -33.9)
        val contributor = TripResultMapContributor()
        val fits = ready(contributor, listOf(segment))
        every { contributor.viewModel.selectedTrip } returns com.jakewharton.rxrelay2.BehaviorRelay.create()
        contributor.focusTripLine(segment)
        org.junit.Assert.assertEquals(1, fits.size)
        io.mockk.verify(exactly = 1) { com.google.android.gms.maps.CameraUpdateFactory.newLatLngBounds(any(), 50) }
    }

}
