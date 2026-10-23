package com.skedgo.tripkit.ui.map.home

import androidx.databinding.ObservableBoolean
import io.mockk.every
import io.mockk.mockk
import io.reactivex.subjects.PublishSubject
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class TripMapMarkerVisibilityTest {
    @Test
    fun `entering trip mode cancels the in flight POI renderer`() {
        val fragment = TripKitMapFragment()
        val visibility = ObservableBoolean(true)
        fragment.viewModel = mockk(relaxed = true) {
            every { showMarkers } returns visibility
        }
        val pending = PublishSubject.create<Unit>()
        TripKitMapFragment::class.java.getDeclaredField("markersDisposable").apply {
            isAccessible = true
        }.set(fragment, pending.subscribe())

        fragment.setShowMarkers(false, null)

        assertThat(visibility.get()).isFalse()
        assertThat(pending.hasObservers()).isFalse()
    }
}
