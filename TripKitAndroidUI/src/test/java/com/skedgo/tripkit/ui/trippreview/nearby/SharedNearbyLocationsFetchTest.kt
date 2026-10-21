package com.skedgo.tripkit.ui.trippreview.nearby

import com.skedgo.tripkit.data.locations.LocationsFetchCoordinator
import com.skedgo.tripkit.data.locations.LocationsResponse
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.util.concurrent.atomic.AtomicInteger

/**
 * Request-count regression tests for the Nearby trip-preview `locations.json` query (#25936).
 *
 * Before this change [SharedNearbyTripPreviewItemViewModel] called `LocationsApi` directly, so
 * every live preview page issued its own GET even when several pages were asking for exactly
 * the same segment. `ViewPager2` keeps up to three pages alive (`offscreenPageLimit` 1-2) and
 * the ViewModel is shared across them via `requireParentFragment()`, so its single
 * `loadedSegment` guard cannot prevent that overlap.
 *
 * These tests assert the number of API invocations, not the emitted data — the ticket is about
 * call volume.
 */
@RunWith(JUnit4::class)
class SharedNearbyLocationsFetchTest {

    private val coordinator = LocationsFetchCoordinator()
    private val apiCalls = AtomicInteger(0)
    private val upstream = PublishSubject.create<List<LocationsResponse.Group>>()

    private val url = "https://ct-au-nsw-sydney.tripgo.skedgo.com/satapp/locations.json"

    /** Mirrors the production call: the API observable is only created inside the guard. */
    private fun fetch(key: String): Observable<List<LocationsResponse.Group>> =
        coordinator.shareInFlight(key) {
            Observable.defer {
                apiCalls.incrementAndGet()
                upstream
            }
        }

    private fun keyFor(
        lat: Double = -33.8688,
        lng: Double = 151.2093,
        mode: String? = "me_car-s"
    ) = SharedNearbyTripPreviewItemViewModel.nearbyRequestKey(url, lat, lng, mode)

    @Test
    fun `concurrent pages requesting the same segment issue one api call`() {
        val key = keyFor()

        val first = fetch(key).test()
        val second = fetch(key).test()
        val third = fetch(key).test()

        assertThat(apiCalls.get()).isEqualTo(1)

        upstream.onNext(emptyList())
        upstream.onComplete()

        first.assertValueCount(1)
        second.assertValueCount(1)
        third.assertValueCount(1)
    }

    @Test
    fun `segments at different locations still issue their own api call`() {
        fetch(keyFor(lat = -33.8688, lng = 151.2093)).test()
        fetch(keyFor(lat = -35.2809, lng = 149.1300)).test()

        assertThat(apiCalls.get()).isEqualTo(2)
    }

    @Test
    fun `same location with a different mode is not treated as the same request`() {
        fetch(keyFor(mode = "me_car-s")).test()
        fetch(keyFor(mode = "cy_bic-s")).test()

        assertThat(apiCalls.get()).isEqualTo(2)
    }

    @Test
    fun `a failed request does not block a later retry`() {
        val key = keyFor()

        val failing = fetch(key).test()
        upstream.onError(IllegalStateException("network down"))
        failing.assertError(IllegalStateException::class.java)

        fetch(key).test()

        assertThat(apiCalls.get()).isEqualTo(2)
    }

    @Test
    fun `cancelling the only subscriber releases the key for the next page`() {
        val key = keyFor()

        val cancelled = fetch(key).test()
        cancelled.dispose()

        fetch(key).test()

        assertThat(apiCalls.get()).isEqualTo(2)
    }

    @Test
    fun `a later page after completion refetches because nearby results are not TTL cached`() {
        val key = keyFor()

        val first = fetch(key).test()
        upstream.onNext(emptyList())
        upstream.onComplete()
        first.assertComplete()

        // Deliberate: free-floating vehicle and parking availability is time-sensitive and this
        // screen holds no persistence, so the Nearby path uses in-flight sharing only — never
        // TTL suppression. See SharedNearbyTripPreviewItemViewModel.nearbyRequestKey.
        fetch(key).test()

        assertThat(apiCalls.get()).isEqualTo(2)
    }

    @Test
    fun `key is stable for identical inputs`() {
        assertThat(keyFor()).isEqualTo(keyFor())
    }
}
