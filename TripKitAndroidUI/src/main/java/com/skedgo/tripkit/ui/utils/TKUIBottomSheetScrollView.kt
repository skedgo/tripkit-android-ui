package com.skedgo.tripkit.ui.utils

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.FrameLayout
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.view.NestedScrollingChildHelper
import androidx.core.view.NestedScrollingParent2
import androidx.core.view.ViewCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetBehavior

/**
 * Provides a [NestedScrollView]-like functionality but specifically for the case
 * when a [BottomSheetBehavior] contains a [ViewPager2] that hosts multiple [RecyclerView]
 * because in that case, some bug causes the [RecyclerView] to have the same heights and
 * scroll state.
 *
 */
class TKUIBottomSheetScrollView(
    context: Context,
    attrs: AttributeSet?,
) : FrameLayout(context, attrs), NestedScrollingParent2 {
    private val childHelper = NestedScrollingChildHelper(this).apply {
        isNestedScrollingEnabled = true
    }
    private var behavior: BottomSheetBehavior<*>? = null
    var started = false
    private var canScroll = false
    private var pendingCanScroll = false

    // Used to determine if already reached the top of the list.
    private var dyPreScroll = 0

    init {
        ViewCompat.setNestedScrollingEnabled(this, true)
    }

    private val bottomSheetCallback = object : BottomSheetBehavior.BottomSheetCallback() {
        override fun onStateChanged(bottomSheet: View, newState: Int) {
            onNextScrollStop(newState == BottomSheetBehavior.STATE_EXPANDED)
        }

        override fun onSlide(bottomSheet: View, slideOffset: Float) = Unit
    }

    fun onNextScrollStop(canScroll: Boolean) {
        pendingCanScroll = canScroll
    }

    override fun onStartNestedScroll(child: View, target: View, axes: Int, type: Int): Boolean {
        // ViewPager2's RecyclerView does not participate in this nested scrolling.
        // This allows it to show its overscroll indicator.
        if (target is RecyclerView) {
            val layoutManager = target.layoutManager as LinearLayoutManager
            if (layoutManager.orientation == LinearLayoutManager.HORIZONTAL) {
                target.isNestedScrollingEnabled = false
            }
        }

        if (!started) {
            childHelper.startNestedScroll(axes, type)
            started = true
        }
        return true
    }

    override fun onNestedScrollAccepted(child: View, target: View, axes: Int, type: Int) = Unit

    override fun onStopNestedScroll(target: View, type: Int) {
        if (started) {
            childHelper.stopNestedScroll(type)
            started = false
            canScroll = pendingCanScroll
        }
    }

    override fun onNestedScroll(
        target: View,
        dxConsumed: Int,
        dyConsumed: Int,
        dxUnconsumed: Int,
        dyUnconsumed: Int,
        type: Int,
    ) {
        if (dyUnconsumed == dyPreScroll && dyPreScroll < 0) {
            canScroll = false
        }
    }

    override fun onNestedPreScroll(target: View, dx: Int, dy: Int, consumed: IntArray, type: Int) {
        if (!canScroll) {
            childHelper.dispatchNestedPreScroll(dx, dy, consumed, null, type)
            // Ensure all dy is consumed to prevent premature scrolling when not allowed.
            consumed[1] = dy
        } else {
            dyPreScroll = dy
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()

        behavior = findBottomSheetBehaviorParent(parent as View) as BottomSheetBehavior<*>?
        behavior?.addBottomSheetCallback(bottomSheetCallback)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        behavior?.removeBottomSheetCallback(bottomSheetCallback)
    }

    private fun findBottomSheetBehaviorParent(parent: View): CoordinatorLayout.Behavior<*>? {
        val params = parent.layoutParams
        return if (params is CoordinatorLayout.LayoutParams && params.behavior != null) {
            params.behavior
        } else {
            require(parent.parent is View) {
                "None of this view's ancestors are associated with BottomSheetBehavior"
            }
            findBottomSheetBehaviorParent(parent.parent as View)
        }
    }
}