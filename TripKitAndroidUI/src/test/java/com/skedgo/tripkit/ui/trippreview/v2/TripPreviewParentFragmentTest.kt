package com.skedgo.tripkit.ui.trippreview.v2

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import com.skedgo.tripkit.ui.R
import android.os.Parcel
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import java.io.File
import com.skedgo.tripkit.routing.TripSegment
import com.skedgo.tripkit.ui.TripKitUI
import com.skedgo.tripkit.ui.trippreview.TripPreviewHeaderFragment
import com.skedgo.tripkit.ui.trippreview.segment.TripSegmentsSummaryData
import com.skedgo.tripkit.ui.utils.ITEM_SERVICE
import org.robolectric.Shadows.shadowOf
import android.os.Looper
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

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = android.app.Application::class)
class TripPreviewParentFragmentTest {
    class Host : FragmentActivity() {
        override fun onCreate(state: Bundle?) {
            setTheme(androidx.appcompat.R.style.Theme_AppCompat)
            super.onCreate(state)
        }
    }

    // Uses the published protected-field convention used by BookingPreviewFragment.
    // Reading in onCreated reproduces the production access without booking/network DI.
    class Preview : TripPreviewParentFragment() {
        var displayedId: String? = null
        fun assignSegment(value: TripSegment) { segment = value }
        override fun onCreated(savedInstance: Bundle?) {
            super.onCreated(savedInstance)
            displayedId = segment.id
        }
    }

    class NavigationPreview : TripPreviewParentFragment() {
        var closed = false
        fun assignSegment(value: TripSegment) { segment = value }
        override fun showQuickBooking() {
            childFragmentManager.beginTransaction().replace(getContainerResourceId(), Fragment())
                .addToBackStack("booking").commit()
        }
        override fun onClose() { closed = true }
    }

    class TransientBookingChild : Fragment() {
        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            check(savedInstanceState == null) { "Unrestorable booking child was recreated" }
        }
    }

    class BookingContainer : Fragment() {
        override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, state: Bundle?): View =
            FrameLayout(inflater.context).apply { id = android.R.id.custom }

        override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
            if (savedInstanceState == null) {
                childFragmentManager.beginTransaction()
                    .add(android.R.id.custom, TransientBookingChild(), "nested-booking-child").commitNow()
            }
        }
    }

    @Before
    fun setUp() {
        mockkObject(TripKitUI.Companion)
        val tripKitUI = mockk<TripKitUI>(relaxed = true)
        every { TripKitUI.getInstance() } returns tripKitUI
        every { tripKitUI.tripPreviewComponent().inject(any<TripPreviewParentFragment>()) } answers {
            firstArg<TripPreviewParentFragment>().viewModelFactory = object : ViewModelProvider.Factory {
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    modelClass.cast(TripPreviewParentViewModel())!!
            }
        }
    }

    @After
    fun tearDown() { unmockkAll() }

    @Test
    fun `existing factory initializes the actual preview view model`() {
        val host = Robolectric.buildActivity(Host::class.java).setup()
        val preview = TripPreviewParentFragment.newInstance(TripSegment().apply {
            id = "selected"; startStopCode = "stop"
        })
        preview.showNow(host.get().supportFragmentManager, "preview")
        shadowOf(Looper.getMainLooper()).idle()
        assertThat(ViewModelProvider(preview)[TripPreviewParentViewModel::class.java].segmentItemType.value)
            .isEqualTo(ITEM_SERVICE)
        host.pause().stop().destroy()
    }

    @Test
    fun `restored header Book action uses the reattached owner and exact segment`() {
        val host = Robolectric.buildActivity(Host::class.java).setup()
        val owner = Fragment()
        host.get().supportFragmentManager.beginTransaction().add(owner, "owner").commitNow()
        var originalCalls = 0
        val header = TripPreviewHeaderFragment.newInstance(false) { originalCalls++ }
        owner.childFragmentManager.beginTransaction().add(header, "header").commitNow()
        val selected = TripSegment().apply { id = "selected" }
        header.setHeaderItems(TripSegmentsSummaryData(emptyList(), selected))
        shadowOf(Looper.getMainLooper()).idle()
        header.binding.bAction.performClick()
        assertThat(originalCalls).isEqualTo(1)

        host.recreate()
        val restoredOwner = host.get().supportFragmentManager.findFragmentByTag("owner")!!
        val restoredHeader = restoredOwner.childFragmentManager.findFragmentByTag("header") as TripPreviewHeaderFragment
        var bookedSegment: TripSegment? = null
        restoredHeader.setOnQuickBookingClickListener { bookedSegment = it }
        restoredHeader.setHeaderItems(TripSegmentsSummaryData(emptyList(), selected))
        shadowOf(Looper.getMainLooper()).idle()
        restoredHeader.binding.bAction.performClick()
        assertThat(bookedSegment).isSameAs(selected)
        assertThat(originalCalls).isEqualTo(1)
        host.pause().stop().destroy()
    }

    @Test
    fun `normal subclass creation keeps selected segment`() {
        val host = Robolectric.buildActivity(Host::class.java).setup()
        val preview = Preview().apply { assignSegment(TripSegment().apply { id = "selected"; startStopCode = "stop" }) }
        preview.showNow(host.get().supportFragmentManager, "preview")
        assertThat(preview.displayedId).isEqualTo("selected")
        host.pause().stop().destroy()
    }

    @Test
    fun `activity recreation restores the same segment before view creation`() {
        val host = Robolectric.buildActivity(Host::class.java).setup()
        Preview().apply { assignSegment(TripSegment().apply { id = "selected"; startStopCode = "stop" }) }
            .showNow(host.get().supportFragmentManager, "preview")
        host.recreate()
        val restored = host.get().supportFragmentManager.findFragmentByTag("preview") as Preview
        assertThat(restored.displayedId).isEqualTo("selected")
        host.pause().stop().destroy()
    }

    @Test
    fun `parcelled activity state restores into a fresh host without original instance`() {
        val host = Robolectric.buildActivity(Host::class.java).setup()
        Preview().apply { assignSegment(TripSegment().apply { id = "selected"; startStopCode = "stop" }) }
            .showNow(host.get().supportFragmentManager, "preview")
        val state = Bundle()
        host.pause().saveInstanceState(state).stop().destroy()
        val parcel = Parcel.obtain()
        state.writeToParcel(parcel, 0)
        parcel.setDataPosition(0)
        val copiedState = Bundle.CREATOR.createFromParcel(parcel)
        parcel.recycle()
        val restoredHost = Robolectric.buildActivity(Host::class.java)
            .create(copiedState).start().resume().visible()
        val restored = restoredHost.get().supportFragmentManager.findFragmentByTag("preview") as Preview
        assertThat(restored.displayedId).isEqualTo("selected")
        restoredHost.pause().stop().destroy()
    }

    @Test
    fun `missing snapshot during saved activity restoration leaves host operational`() {
        assertUnavailableRestoration(corrupt = false)
    }

    @Test
    fun `corrupted snapshot during saved activity restoration leaves host operational`() {
        assertUnavailableRestoration(corrupt = true)
    }

    private fun assertUnavailableRestoration(corrupt: Boolean) {
        val host = Robolectric.buildActivity(Host::class.java).setup()
        Preview().apply { assignSegment(TripSegment().apply { id = "selected"; startStopCode = "stop" }) }
            .showNow(host.get().supportFragmentManager, "preview")
        val preview = host.get().supportFragmentManager.findFragmentByTag("preview")!!
        preview.childFragmentManager.beginTransaction()
            .add(R.id.content, BookingContainer(), "booking-child")
            .addToBackStack("booking-child").commit()
        preview.childFragmentManager.executePendingTransactions()
        val state = Bundle()
        host.pause().saveInstanceState(state).stop().destroy()
        val files = File(host.get().cacheDir, "trip-preview-segments").listFiles()!!
        assertThat(files).isNotEmpty()
        files.forEach { if (corrupt) it.writeText("{") else it.delete() }
        val restored = Robolectric.buildActivity(Host::class.java).create(state).start().resume().visible()
        restored.get().supportFragmentManager.executePendingTransactions()
        assertThat(restored.get().supportFragmentManager.findFragmentByTag("preview")).isNull()
        assertThat(restored.get().isFinishing).isFalse()
        // The host can still execute fragment transactions after discarding the invalid dialog.
        Preview().apply { assignSegment(TripSegment().apply { id = "replacement"; startStopCode = "stop" }) }
            .showNow(restored.get().supportFragmentManager, "replacement")
        assertThat((restored.get().supportFragmentManager.findFragmentByTag("replacement") as Preview).displayedId)
            .isEqualTo("replacement")
        restored.pause().stop().destroy()
    }

    @Test
    fun `recreation keeps one booking screen so Cancel closes instead of showing stale data`() {
        val host = Robolectric.buildActivity(Host::class.java).setup()
        NavigationPreview().apply {
            assignSegment(TripSegment().apply { id = "selected"; startStopCode = "stop" })
        }.showNow(host.get().supportFragmentManager, "preview")
        shadowOf(Looper.getMainLooper()).idle()
        repeat(2) {
            host.recreate()
            shadowOf(Looper.getMainLooper()).idle()
            val restored = host.get().supportFragmentManager.findFragmentByTag("preview") as NavigationPreview
            restored.childFragmentManager.executePendingTransactions()
            assertThat(restored.childFragmentManager.backStackEntryCount).isEqualTo(1)
            restored.onBackPressed()
            assertThat(restored.closed).isTrue()
        }
        host.pause().stop().destroy()
    }

    @Test
    fun `legacy fragment with no segment cannot crash host startup`() {
        val host = Robolectric.buildActivity(Host::class.java).setup()
        TripPreviewParentFragment().showNow(host.get().supportFragmentManager, "preview")
        host.get().supportFragmentManager.executePendingTransactions()
        assertThat(host.get().supportFragmentManager.findFragmentByTag("preview")).isNull()
        assertThat(host.get().isFinishing).isFalse()
        host.pause().stop().destroy()
    }
}
