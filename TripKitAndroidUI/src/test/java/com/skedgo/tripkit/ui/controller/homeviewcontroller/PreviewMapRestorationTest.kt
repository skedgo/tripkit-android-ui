package com.skedgo.tripkit.ui.controller.homeviewcontroller

import com.skedgo.tripkit.routing.Trip
import com.skedgo.tripkit.routing.TripGroup
import com.skedgo.tripkit.ui.controller.ViewControllerEventBus
import com.skedgo.tripkit.ui.map.home.TripKitMapContributor
import com.skedgo.tripkit.ui.map.home.TripKitMapFragment
import com.skedgo.tripkit.ui.trippreview.TripPreviewPagerListener
import com.skedgo.tripkit.ui.tripresult.TripResultMapContributor
import io.mockk.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class PreviewMapRestorationTest {
    private lateinit var home: TKUIHomeViewControllerFragment
    private lateinit var listener: TripPreviewPagerListener
    private lateinit var map: TripKitMapFragment
    private lateinit var trip: Trip
    private lateinit var group: TripGroup

    @Before fun setUp() {
        mockkConstructor(TripResultMapContributor::class)
        every { anyConstructed<TripResultMapContributor>().initialize() } just Runs
        every { anyConstructed<TripResultMapContributor>().setTripGroupId(any(), any(), any(), any()) } just Runs
        map = mockk(relaxed = true)
        var active: TripKitMapContributor? = null
        every { map.getContributor() } answers { active }
        every { map.setContributor(any()) } answers { active = firstArg() }
        group = mockk(relaxed = true)
        every { group.uuid() } returns "group"
        trip = mockk(relaxed = true)
        every { trip.group } returns group
        every { trip.tripId } returns 123L
        every { trip.saveURL } returns "selected-trip"
        home = TKUIHomeViewControllerFragment().apply {
            mapFragment = this@PreviewMapRestorationTest.map
            eventBus = ViewControllerEventBus
        }
        val factory = home.javaClass.getDeclaredMethod("initTripPreviewPagerFragmentListener")
        factory.isAccessible = true
        listener = factory.invoke(home) as TripPreviewPagerListener
    }

    @After fun tearDown() { unmockkAll() }

    @Test fun `restored preview trip is attached to the shared SDK map contributor`() {
        listener.reportPlannedTrip(trip, listOf(group))
        verify(exactly = 1) { anyConstructed<TripResultMapContributor>().initialize() }
        verify(exactly = 1) { map.setContributor(any<TripResultMapContributor>()) }
        verify { anyConstructed<TripResultMapContributor>().setTripGroupId("group", 123L, false, "selected-trip") }
    }

    @Test fun `repeated preview reports preserve manual camera position and reuse contributor`() {
        listener.reportPlannedTrip(trip, listOf(group))
        listener.reportPlannedTrip(trip, listOf(group))
        verify(exactly = 1) { map.setContributor(any<TripResultMapContributor>()) }
        verify { anyConstructed<TripResultMapContributor>().setTripGroupId("group", 123L, true, "selected-trip") }
    }
    @Test fun `leaving preview detaches only its owned contributor`() {
        listener.reportPlannedTrip(trip, listOf(group))
        val clear = home.javaClass.getDeclaredMethod("clearPreviewMapContributor")
        clear.isAccessible = true
        clear.invoke(home)
        verify(exactly = 1) { map.setContributor(null) }
    }

}
