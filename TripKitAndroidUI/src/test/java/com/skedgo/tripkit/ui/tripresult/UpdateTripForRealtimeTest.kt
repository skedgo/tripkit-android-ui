package com.skedgo.tripkit.ui.tripresult

import com.skedgo.tripkit.TripUpdater
import com.skedgo.tripkit.logging.ErrorLogger
import com.skedgo.tripkit.routing.Trip
import com.skedgo.tripkit.routing.TripGroup
import com.skedgo.tripkit.ui.routingresults.FetchingRealtimeStatusRepository
import com.skedgo.tripkit.ui.routingresults.TripGroupRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.plugins.RxJavaPlugins
import io.reactivex.schedulers.Schedulers
import io.reactivex.schedulers.TestScheduler
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test

class UpdateTripForRealtimeTest {
    private val scheduler = TestScheduler()
    private val repository = mockk<TripGroupRepository>()
    private val updater = mockk<TripUpdater>()
    private lateinit var subject: UpdateTripForRealtime

    @Before fun setup() {
        RxJavaPlugins.setComputationSchedulerHandler { scheduler }
        RxJavaPlugins.setIoSchedulerHandler { Schedulers.trampoline() }
        subject = UpdateTripForRealtime(repository, mockk<ErrorLogger>(relaxed = true), mockk<FetchingRealtimeStatusRepository>(relaxed = true), updater)
    }
    @After fun teardown() { subject.stop(); RxJavaPlugins.reset() }

    private fun assertReplacement(booking: Boolean) {
        val original = Trip().apply { uuid = "persisted-favorite"; tripId = 17; updateURL = "https://example.test/update"; startTimeInSecs = 100 }
        val updated = Trip().apply { uuid = "new-response-instance"; tripId = 42; startTimeInSecs = 160; updateURL = "https://example.test/next" }
        val group = TripGroup().apply { uuid("group"); setTrips(arrayListOf(original)); displayTripId = original.tripId }
        val replacement = slot<Trip>()
        every { updater.getUpdateAsync(original.updateURL!!) } returns Observable.just(updated)
        every { repository.updateTrip("group", original.uuid, capture(replacement)) } returns Completable.complete()
        if (booking) subject.startForBooking(Observable.just(group)) else subject.start(Observable.just(group))
        scheduler.triggerActions()
        verify(exactly = 1) { repository.updateTrip("group", "persisted-favorite", any()) }
        assertThat(replacement.captured.uuid).isEqualTo("persisted-favorite")
        assertThat(replacement.captured.tripId).isEqualTo(17L)
        assertThat(replacement.captured.startTimeInSecs).isEqualTo(160L)
        assertThat(replacement.captured.updateURL).isEqualTo("https://example.test/next")
    }
    @Test fun `scheduled trip refresh keeps persisted Favorite and navigation identities while updating live data`() = assertReplacement(false)
    @Test fun `booking refresh keeps persisted trip identity as well`() = assertReplacement(true)
}
