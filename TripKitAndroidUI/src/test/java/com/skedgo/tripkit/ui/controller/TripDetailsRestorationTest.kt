package com.skedgo.tripkit.ui.controller

import com.skedgo.tripkit.common.model.Query
import com.skedgo.tripkit.common.model.location.Location
import com.skedgo.tripkit.model.ViewTrip
import com.skedgo.tripkit.ui.controller.tripdetailsviewcontroller.TKUITripDetailsViewControllerFragment
import com.skedgo.tripkit.ui.tripresult.FromRoutes
import com.skedgo.tripkit.ui.tripresult.SingleTrip
import com.skedgo.tripkit.ui.tripresult.TripResultPagerFragment
import io.mockk.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class TripDetailsRestorationTest {
    @Test fun `restored routes pager receives the original query identity locations and selection`() {
        val query = Query().apply {
            fromLocation = Location(-27.47, 152.98)
            toLocation = Location(-27.48, 152.99)
        }
        val controller = TKUITripDetailsViewControllerFragment.newInstance(ViewTrip(query, "selected-group", 7L, 2))
        val pager = mockk<TripResultPagerFragment>(relaxed = true)
        controller.configureRestoredPager(pager)
        verify { pager.setArgs(match { it is FromRoutes && it.tripGroupId == "selected-group" &&
            it.tripId == 7L && it.sortOrder == 2 && it.requestId == query.uuid() && it.arriveBy == query.arriveBy }) }
        verify { pager.setQueryLocations(query.fromLocation, query.toLocation) }
    }

    @Test fun `restored single-group pager receives its query arguments`() {
        val controller = TKUITripDetailsViewControllerFragment.newInstance("selected-group", factory = null)
        val pager = mockk<TripResultPagerFragment>(relaxed = true)
        controller.configureRestoredPager(pager)
        verify { pager.setArgs(match { it is SingleTrip && it.tripGroupId == "selected-group" }) }
    }

    @Test fun `restored favorite pager keeps the favorite identity`() {
        val favorite = mockk<com.skedgo.tripkit.ui.favorites.trips.FavoriteTrip>(relaxed = true)
        every { favorite.uuid } returns "favorite-id"
        val controller = TKUITripDetailsViewControllerFragment.newInstance(favorite, factory = null)
        val pager = mockk<TripResultPagerFragment>(relaxed = true)
        controller.configureRestoredPager(pager)
        verify { pager.setArgs(match { it is com.skedgo.tripkit.ui.tripresult.FavoriteTrip && it.favoriteTripId == "favorite-id" }) }
    }
}
