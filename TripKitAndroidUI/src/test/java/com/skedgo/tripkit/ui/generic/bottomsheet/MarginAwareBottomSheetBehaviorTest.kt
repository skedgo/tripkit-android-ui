package com.skedgo.tripkit.ui.generic.bottomsheet

import android.app.Activity
import org.robolectric.Robolectric
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.view.ViewCompat
import androidx.core.widget.NestedScrollView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.Shadows.shadowOf
import android.os.Looper
import java.time.Duration

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class MarginAwareBottomSheetBehaviorTest {
    private val context get() = RuntimeEnvironment.getApplication()
    private val parent = CoordinatorLayout(context)
    private val sheet = FrameLayout(context)
    private val list = NestedScrollView(context)
    private val behavior = MarginAwareBottomSheetBehavior<FrameLayout>()

    private fun open(topInset: Int = 63, height: Int = ViewGroup.LayoutParams.MATCH_PARENT,
                     state: Int = BottomSheetBehavior.STATE_EXPANDED, offset: Int = 0) {
        behavior.isFitToContents = height != ViewGroup.LayoutParams.MATCH_PARENT
        behavior.expandedOffset = offset
        behavior.peekHeight = 315
        behavior.state = state
        val params = CoordinatorLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, height)
        params.topMargin = topInset
        params.behavior = behavior
        sheet.addView(list, FrameLayout.LayoutParams(-1, -1))
        list.addView(View(context).apply { minimumHeight = 4000 }, ViewGroup.LayoutParams(-1, 4000))
        parent.addView(sheet, params)
        layout()
    }

    private fun layout() {
        parent.measure(View.MeasureSpec.makeMeasureSpec(1080, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(2400, View.MeasureSpec.EXACTLY))
        parent.layout(0, 0, 1080, 2400)
    }

    private fun scroll(dy: Int): Int {
        behavior.onStartNestedScroll(parent, sheet, list, list, ViewCompat.SCROLL_AXIS_VERTICAL,
            ViewCompat.TYPE_TOUCH)
        val consumed = IntArray(2)
        behavior.onNestedPreScroll(parent, sheet, list, 0, dy, consumed, ViewCompat.TYPE_TOUCH)
        return consumed[1]
    }

    @Test
    fun `nested expansion keeps the sheet through the navigation area`() {
        open()
        assertThat(sheet.measuredHeight).isEqualTo(2337)
        assertThat(sheet.bottom).isGreaterThanOrEqualTo(2400)
        scroll(200)
        assertThat(sheet.bottom).isGreaterThanOrEqualTo(2400)
        assertThat(sheet.top).isEqualTo(63)
    }

    @Test
    fun `half expanded sheet reaches the same safe expanded bounds`() {
        open(state = BottomSheetBehavior.STATE_HALF_EXPANDED)
        assertThat(sheet.top).isEqualTo(1200)
        assertThat(sheet.bottom).isGreaterThanOrEqualTo(2400)
        scroll(2000)
        assertThat(sheet.top).isEqualTo(63)
        assertThat(sheet.bottom).isEqualTo(2400)
    }

    @Test
    fun `collapsed sheet preserves its visible peek height`() {
        open(state = BottomSheetBehavior.STATE_COLLAPSED)
        assertThat(sheet.top).isEqualTo(2085)
        assertThat(sheet.bottom).isGreaterThanOrEqualTo(2400)
        scroll(2400)
        assertThat(sheet.top).isEqualTo(63)
        assertThat(sheet.bottom).isEqualTo(2400)
    }

    @Test
    fun `expanded layout and nested scroll agree after relayout`() {
        open()
        scroll(200)
        sheet.requestLayout()
        layout()
        assertThat(sheet.top).isEqualTo(63)
        assertThat(sheet.bottom).isEqualTo(2400)
        assertThat(scroll(500)).isZero()
        assertThat(sheet.top).isEqualTo(63)
        list.scrollTo(0, 4000)
        assertThat(list.scrollY).isGreaterThan(0)
        assertThat(list.getChildAt(0).bottom - list.scrollY).isEqualTo(list.height)
    }

    @Test
    fun `relayout while dragging retains the absolute position`() {
        open(state = BottomSheetBehavior.STATE_HALF_EXPANDED)
        scroll(100)
        val top = sheet.top
        assertThat(behavior.state).isEqualTo(BottomSheetBehavior.STATE_DRAGGING)
        sheet.requestLayout()
        layout()
        assertThat(sheet.top).isEqualTo(top)
        assertThat(sheet.bottom).isGreaterThanOrEqualTo(2400)
    }

    @Test
    fun `a changed top inset is reflected in the next expansion`() {
        open()
        (sheet.layoutParams as CoordinatorLayout.LayoutParams).topMargin = 96
        sheet.requestLayout()
        layout()
        scroll(200)
        assertThat(sheet.measuredHeight).isEqualTo(2304)
        assertThat(sheet.top).isEqualTo(96)
        assertThat(sheet.bottom).isEqualTo(2400)
    }

    @Test
    fun `increasing the inset while dragging clamps the saved position`() {
        open(state = BottomSheetBehavior.STATE_HALF_EXPANDED)
        scroll(1136)
        assertThat(sheet.top).isEqualTo(64)
        assertThat(behavior.state).isEqualTo(BottomSheetBehavior.STATE_DRAGGING)
        (sheet.layoutParams as CoordinatorLayout.LayoutParams).topMargin = 96
        sheet.requestLayout()
        layout()
        assertThat(sheet.bottom).isEqualTo(2400)
        assertThat(sheet.top).isEqualTo(96)
    }

    @Test
    fun `a stale settling target cannot cross an increased inset`() {
        open(state = BottomSheetBehavior.STATE_HALF_EXPANDED)
        val activity = Robolectric.buildActivity(Activity::class.java).setup().get()
        activity.setContentView(parent, ViewGroup.LayoutParams(1080, 2400))
        layout()
        behavior.state = BottomSheetBehavior.STATE_EXPANDED
        assertThat(behavior.state).isEqualTo(BottomSheetBehavior.STATE_SETTLING)
        (sheet.layoutParams as CoordinatorLayout.LayoutParams).topMargin = 96
        sheet.requestLayout()
        layout()
        repeat(100) {
            shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(16))
            assertThat(sheet.top).isGreaterThanOrEqualTo(96)
            assertThat(sheet.bottom).isGreaterThanOrEqualTo(2400)
        }
        assertThat(behavior.state).isEqualTo(BottomSheetBehavior.STATE_EXPANDED)
        assertThat(sheet.top).isEqualTo(96)
    }

    @Test
    fun `short fit to content sheets stay short and anchored`() {
        open(height = 500)
        assertThat(sheet.measuredHeight).isEqualTo(500)
        assertThat(sheet.top).isEqualTo(1900)
        assertThat(sheet.bottom).isEqualTo(2400)
    }

    @Test
    fun `zero inset preserves existing expanded geometry`() {
        open(topInset = 0)
        scroll(200)
        assertThat(sheet.top).isZero()
        assertThat(sheet.bottom).isEqualTo(2400)
    }

    @Test
    fun `an explicit larger expanded offset is preserved`() {
        open(offset = 180)
        assertThat(sheet.top).isEqualTo(180)
        scroll(200)
        assertThat(sheet.top).isEqualTo(180)
        assertThat(sheet.bottom).isGreaterThanOrEqualTo(2400)
    }
}
