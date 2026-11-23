package com.skedgo.tripkit.ui.map

import com.skedgo.tripkit.routing.Trip
import com.skedgo.tripkit.routing.TripGroup
import com.skedgo.tripkit.ui.routingresults.TripGroupRepository
import io.mockk.every
import io.mockk.mockk
import io.reactivex.Observable
import org.junit.Assert.assertSame
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Redmine #19351: after rotation / font-size change the Trip Preview restores the map with the
 * stable numeric trip id. The map must then show that trip, not the group's display trip.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class TripResultMapViewModelSelectionTest {

    private val firstTrip = Trip().apply { tripId = 241L; uuid = "first" }
    private val selectedTrip = Trip().apply { tripId = 301L; uuid = "selected" }
    private val group = TripGroup().apply {
        uuid("group")
        addTrip(firstTrip)
        addTrip(selectedTrip)
        displayTripId = firstTrip.tripId
    }
    private val repository: TripGroupRepository = mockk {
        every { getTripGroup("group") } returns Observable.just(group)
    }

    private fun viewModel() = TripResultMapViewModel(
        mockk(relaxed = true),
        repository,
        mockk(relaxed = true),
        mockk(relaxed = true),
        mockk(relaxed = true),
        mockk(relaxed = true)
    )

    @Test
    fun `restoring with the numeric trip id selects that trip`() {
        val viewModel = viewModel()

        viewModel.setTripGroupId("group", selectedTrip.tripId, skipCamera = true)

        assertSame(selectedTrip, viewModel.selectedTrip.value)
    }

    @Test
    fun `trip key selects the trip when trip ids are zero`() {
        val parsedFirst = Trip().apply { saveURL = "https://example.test/first" }
        val parsedSelected = Trip().apply { saveURL = "https://example.test/selected" }
        val parsedGroup = TripGroup().apply { uuid("parsed"); addTrip(parsedFirst); addTrip(parsedSelected) }
        every { repository.getTripGroup("parsed") } returns Observable.just(parsedGroup)
        val viewModel = viewModel()

        viewModel.setTripGroupId("parsed", 0L, tripKey = "https://example.test/selected")

        assertSame(parsedSelected, viewModel.selectedTrip.value)
    }

    @Test
    fun `without a trip id the display trip is selected`() {
        val viewModel = viewModel()

        viewModel.setTripGroupId("group", null)

        assertSame(firstTrip, viewModel.selectedTrip.value)
    }
}
