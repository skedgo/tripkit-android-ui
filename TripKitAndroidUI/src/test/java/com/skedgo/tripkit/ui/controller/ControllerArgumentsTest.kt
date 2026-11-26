package com.skedgo.tripkit.ui.controller

import android.os.Bundle
import android.os.Parcel
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.skedgo.tripkit.common.model.location.Location
import com.skedgo.tripkit.ui.controller.locationsearchcontroller.TKUILocationSearchViewControllerFragment
import com.skedgo.tripkit.ui.controller.routeviewcontroller.TKUIRouteFragment
import com.skedgo.tripkit.ui.controller.tripresultcontroller.TKUITripResultsFragment
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Inputs must survive Android's parcel/recreate path, not just a live fragment instance. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class ControllerArgumentsTest {
    private fun restored(args: Bundle?): Bundle {
        assertNotNull("Public factory must persist inputs in arguments", args)
        val parcel = Parcel.obtain()
        return try {
            parcel.writeBundle(args)
            parcel.setDataPosition(0)
            parcel.readBundle(Location::class.java.classLoader)!!
        } finally { parcel.recycle() }
    }

    @Test fun `results inputs survive parcel recreation`() {
        val origin = Location(-27.48, 152.98).apply { name = "Origin" }
        val dest = Location(-27.47, 152.99).apply { name = "Destination" }
        val args = restored(TKUITripResultsFragment.newInstance(origin, dest, false).arguments)
        assertEquals(origin.lat, args.getParcelable<Location>("tkui_origin")!!.lat, 0.0)
        assertEquals(dest.lon, args.getParcelable<Location>("tkui_destination")!!.lon, 0.0)
        assertFalse(args.getBoolean("tkui_from_route", true))
    }

    @Test fun `route bounds and locations survive parcel recreation`() {
        val near = LatLng(-27.47, 153.02)
        val bounds = LatLngBounds(LatLng(-28.0, 152.0), LatLng(-27.0, 154.0))
        val origin = Location(-27.48, 152.98)
        val args = restored(TKUIRouteFragment.newInstance(bounds, near, origin).arguments)
        assertEquals(bounds, args.getParcelable<LatLngBounds>("tkui_bounds"))
        assertEquals(near, args.getParcelable<LatLng>("tkui_near"))
        assertEquals(origin.lat, args.getParcelable<Location>("tkui_origin")!!.lat, 0.0)
    }

    @Test fun `embedded search keeps header flag and search bounds`() {
        val near = LatLng(-27.47, 153.02)
        val bounds = LatLngBounds(LatLng(-28.0, 152.0), LatLng(-27.0, 154.0))
        val args = restored(TKUILocationSearchViewControllerFragment.newInstance(bounds, near, null, withHeaders = false).arguments)
        assertFalse(args.getBoolean("tkui_headers", true))
        assertEquals(bounds, args.getParcelable<LatLngBounds>("tkui_bounds"))
    }
    @Test fun `details group ID survives parcel recreation`() {
        val fragment = com.skedgo.tripkit.ui.controller.tripdetailsviewcontroller.TKUITripDetailsViewControllerFragment
            .newInstance("group-123", factory = null)
        assertEquals("group-123", restored(fragment.arguments).getString("tkui_group"))
    }

    @Test fun `preview trip segment and action survive parcel recreation`() {
        val fragment = com.skedgo.tripkit.ui.controller.trippreviewcontroller.TKUITripPreviewFragment
            .newInstance("group-123", "trip-456", 789L, io.mockk.mockk(relaxed = true), fromAction = true)
        val args = restored(fragment.arguments)
        assertEquals("group-123", args.getString("tkui_group"))
        assertEquals("trip-456", args.getString("tkui_trip"))
        assertEquals(789L, args.getLong("tkui_segment"))
        assertTrue(args.getBoolean("tkui_from_action"))
    }

    @Test fun `preview saves canonical selection when client UUID may change after reparse`() {
        val selected = io.mockk.mockk<com.skedgo.tripkit.routing.Trip>(relaxed = true)
        io.mockk.every { selected.saveURL } returns "canonical-selection"
        io.mockk.every { selected.tripId } returns 42L
        val fragment = com.skedgo.tripkit.ui.controller.trippreviewcontroller.TKUITripPreviewFragment()
        fragment.latestTrip = selected
        val saved = Bundle()
        fragment.onSaveInstanceState(saved)
        assertEquals("canonical-selection", saved.getString("tkui_trip_key"))
        assertEquals(42L, saved.getLong("tkui_trip_numeric"))
    }

}
