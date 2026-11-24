package com.skedgo.tripkit.ui.trippreview

import android.os.Bundle
import android.os.Parcel
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import com.skedgo.tripkit.ui.trippreview.directions.DirectionsTripPreviewItemViewModel
import com.skedgo.tripkit.common.model.ImmutableStreet
import com.skedgo.tripkit.common.model.Street
import com.skedgo.tripkit.common.model.TransportMode
import com.skedgo.tripkit.routing.TripSegment
import com.skedgo.tripkit.ui.TripKitUI
import com.skedgo.tripkit.ui.trippreview.directions.DirectionsTripPreviewItemFragment
import com.skedgo.tripkit.ui.trippreview.standard.StandardTripPreviewItemFragment
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkAll
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Fresh FragmentManager restoration, without retaining a ViewModelStore from configuration change. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], application = android.app.Application::class)
class WalkPreviewRestorationTest {
    class Host : FragmentActivity() {
        override fun onCreate(state: Bundle?) {
            setTheme(com.google.android.material.R.style.Theme_MaterialComponents_Light_NoActionBar)
            super.onCreate(state)
        }
    }

    @Before fun setup() {
        mockkObject(TripKitUI.Companion)
        every { TripKitUI.getInstance() } returns mockk(relaxed = true)
    }
    @After fun cleanup() { unmockkAll() }

    private fun selected(id: String) = TripSegment().apply {
        this.id = id
        segmentId = if (id == "first-walk") 1L else 3L
        transportModeId = TransportMode.ID_WALK
        metres = 335
        setStreets(arrayListOf(ImmutableStreet.builder().name("Cahill Walk")
            .metres(335f).instruction(Street.Instruction.CONTINUE_STRAIGHT).build()))
    }

    private fun recreateWithFreshStore(page: Fragment): Fragment {
        val host = Robolectric.buildActivity(Host::class.java).setup()
        val manager = host.get().supportFragmentManager
        manager.beginTransaction().add(page, "walk")
            .setMaxLifecycle(page, Lifecycle.State.CREATED).commitNow()
        val state = Bundle()
        host.pause().saveInstanceState(state).stop().destroy()
        // Android trims app caches under storage pressure even while a task has saved state.
        host.get().cacheDir.resolve("trip-preview-segments").deleteRecursively()
        val parcel = Parcel.obtain()
        parcel.writeBundle(state); parcel.setDataPosition(0)
        val copied = parcel.readBundle(javaClass.classLoader)!!
        parcel.recycle()
        val fresh = Robolectric.buildActivity(Host::class.java).create(copied).start()
        val restored = fresh.get().supportFragmentManager.findFragmentByTag("walk")!!
        // Keep fresh host alive while reading state; no retained original ViewModel exists.
        assertThat(restored).isNotSameAs(page)
        return restored
    }

    @Test fun `first standard Walk restores its exact segment after process-style recreation`() {
        val original = selected("first-walk")
        val restored = recreateWithFreshStore(StandardTripPreviewItemFragment.newInstance(original))
            as StandardTripPreviewItemFragment
        assertThat(restored.segment?.id).isEqualTo("first-walk")
        assertThat(restored.segment?.segmentId).isEqualTo(1L)
        assertThat(restored.segment?.metres).isEqualTo(335)
    }

    @Test fun `directions Walk restores street content without a retained ViewModel`() {
        val restored = recreateWithFreshStore(DirectionsTripPreviewItemFragment.newInstance(selected("first-walk")))
        val field = DirectionsTripPreviewItemFragment::class.java.getDeclaredField("segment").apply { isAccessible = true }
        val segment = field.get(restored) as TripSegment?
        assertThat(segment?.id).isEqualTo("first-walk")
        assertThat(segment?.streets?.map { it.name() }).containsExactly("Cahill Walk")
        val model = ViewModelProvider(restored)[DirectionsTripPreviewItemViewModel::class.java]
        assertThat(model.items.map { it.description.get() }).containsExactly("Along Cahill Walk")
    }

    @Test fun `final Walk retains its own identity instead of being replaced with first Walk`() {
        val restored = recreateWithFreshStore(StandardTripPreviewItemFragment.newInstance(selected("final-walk")))
            as StandardTripPreviewItemFragment
        assertThat(restored.segment?.id).isEqualTo("final-walk")
        assertThat(restored.segment?.segmentId).isEqualTo(3L)
    }
}
