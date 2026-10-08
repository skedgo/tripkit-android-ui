package com.skedgo.tripkit.ui.servicedetail

import android.view.View
import android.view.ViewGroup
import androidx.coordinatorlayout.widget.CoordinatorLayout
import com.google.android.material.bottomsheet.BottomSheetBehavior
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class ServiceStopMapFocusTest {
    private val view = mockk<View>(relaxed = true)
    private val sheet = mockk<BottomSheetBehavior<View>>(relaxed = true)
    private val posted = mutableListOf<Runnable>()
    private val callbacks = mutableListOf<BottomSheetBehavior.BottomSheetCallback>()
    private var state = BottomSheetBehavior.STATE_EXPANDED
    private val writes = mutableListOf<String>()
    private val focus = ServiceStopMapFocus(view)

    init {
        every { view.layoutParams } returns CoordinatorLayout.LayoutParams(-1, -1).apply {
            behavior = sheet
        }
        every { view.post(any()) } answers { posted.add(firstArg()); true }
        every { view.removeCallbacks(any()) } answers { posted.remove(firstArg()); true }
        every { sheet.state } answers { state }
        every { sheet.addBottomSheetCallback(any()) } answers { callbacks.add(firstArg()); Unit }
        every { sheet.removeBottomSheetCallback(any()) } answers { callbacks.remove(firstArg()); Unit }
    }

    private fun drain() {
        while (posted.isNotEmpty()) posted.removeAt(0).run()
    }

    private fun settle() {
        state = BottomSheetBehavior.STATE_HALF_EXPANDED
        // Home refreshes padding and moves to its current camera target before later callbacks.
        writes.add("home camera")
        callbacks.toList().forEach { it.onStateChanged(view, state) }
        drain()
    }

    @Test
    fun `expanded selection focuses after the host finishes its sheet camera write`() {
        focus.select { writes.add("selected stop") }
        state = BottomSheetBehavior.STATE_SETTLING
        drain()
        assertThat(writes).isEmpty()
        settle()
        assertThat(writes).containsExactly("home camera", "selected stop")
    }

    @Test
    fun `already settled selection focuses once on the next layout turn`() {
        state = BottomSheetBehavior.STATE_HALF_EXPANDED
        focus.select { writes.add("A") }
        assertThat(writes).isEmpty()
        drain()
        assertThat(writes).containsExactly("A")
        assertThat(callbacks).isEmpty()
    }

    @Test
    fun `latest stop wins during repeated selection while the sheet settles`() {
        state = BottomSheetBehavior.STATE_SETTLING
        for (stop in listOf("A", "B", "C", "A")) {
            focus.select { writes.add(stop) }
            drain()
        }
        settle()
        assertThat(writes).containsExactly("home camera", "A")
        assertThat(callbacks).isEmpty()
    }

    @Test
    fun `destroying the view cancels queued selection`() {
        focus.select { writes.add("A") }
        focus.clear()
        drain()
        assertThat(writes).isEmpty()
    }

    @Test
    fun `destroying the view removes a pending sheet callback`() {
        state = BottomSheetBehavior.STATE_SETTLING
        focus.select { writes.add("A") }
        drain()
        focus.clear()
        settle()
        assertThat(writes).containsExactly("home camera")
        assertThat(callbacks).isEmpty()
    }

    @Test
    fun `standalone service details focus without an enclosing bottom sheet`() {
        every { view.layoutParams } returns null
        every { view.parent } returns null
        focus.select { writes.add("A") }
        drain()
        assertThat(writes).containsExactly("A")
    }

    @Test
    fun `dragging and settling callbacks do not focus until stable`() {
        state = BottomSheetBehavior.STATE_DRAGGING
        focus.select { writes.add("A") }
        drain()
        callbacks.toList().forEach { it.onStateChanged(view, BottomSheetBehavior.STATE_SETTLING) }
        assertThat(writes).isEmpty()
        settle()
        assertThat(writes).containsExactly("home camera", "A")
    }

    @Test
    fun `queued selections focus only the last stop`() {
        state = BottomSheetBehavior.STATE_HALF_EXPANDED
        for (stop in listOf("A", "B", "C", "A")) focus.select { writes.add(stop) }
        drain()
        assertThat(writes).containsExactly("A")
    }

    @Test
    fun `sheet behavior is found on a containing view`() {
        val parent = mockk<ViewGroup>(relaxed = true)
        every { parent.layoutParams } returns view.layoutParams
        every { view.layoutParams } returns null
        every { view.parent } returns parent
        state = BottomSheetBehavior.STATE_SETTLING
        focus.select { writes.add("A") }
        drain()
        assertThat(writes).isEmpty()
        settle()
        assertThat(writes).containsExactly("home camera", "A")
    }

    @Test
    fun `later sheet changes do not refocus a previously selected stop`() {
        state = BottomSheetBehavior.STATE_SETTLING
        focus.select { writes.add("A") }
        drain()
        settle()
        writes.clear()
        settle()
        assertThat(writes).containsExactly("home camera")
    }


    @Test
    fun `host transition queued behind the focus check cannot overwrite selection`() {
        // Rx delivers the host request first; Material can then post setState's
        // transition behind the focus check if its parent has requested layout.
        posted.add(Runnable {
            posted.add(Runnable { state = BottomSheetBehavior.STATE_SETTLING })
        })
        focus.select { writes.add("A") }
        drain()
        assertThat(writes).isEmpty()
        settle()
        assertThat(writes).containsExactly("home camera", "A")
    }

}
