package com.skedgo.tripkit.ui.generic.bottomsheet

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.view.ViewCompat
import com.google.android.material.bottomsheet.BottomSheetBehavior

/**
 * Keeps inset margins in the same coordinate space during layout, dragging and nested scrolling.
 *
 * Material lays out a sheet relative to its top margin, but drags and nested scrolls use absolute
 * offsets. A top margin also reduces a match-parent sheet's measured height, so moving that sheet
 * to zero exposes the map underneath. Use the margin as the minimum expanded offset and remove
 * the extra margin origin from stable layout positions. Saved dragging/settling positions are
 * already absolute and are preserved while valid under the current inset. A settling animation
 * can retain an older target, so constrain its frames and completion to the current expanded bound.
 */
class MarginAwareBottomSheetBehavior<V : View> : BottomSheetBehavior<V> {
    private var topMargin = 0

    constructor() : super()
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)

    init {
        addBottomSheetCallback(object : BottomSheetCallback() {
            override fun onSlide(bottomSheet: View, slideOffset: Float) {
                constrainExpandedPosition(bottomSheet)
            }

            override fun onStateChanged(bottomSheet: View, newState: Int) {
                constrainExpandedPosition(bottomSheet)
            }
        })
    }

    private fun constrainExpandedPosition(child: View) {
        val adjustment = getExpandedOffset() - child.top
        if (adjustment > 0) ViewCompat.offsetTopAndBottom(child, adjustment)
    }

    override fun getExpandedOffset(): Int = maxOf(super.getExpandedOffset(), topMargin)

    override fun onLayoutChild(parent: CoordinatorLayout, child: V, layoutDirection: Int): Boolean {
        topMargin = (child.layoutParams as ViewGroup.MarginLayoutParams).topMargin
        val positionedFromMargin = state != STATE_DRAGGING && state != STATE_SETTLING
        val laidOut = super.onLayoutChild(parent, child, layoutDirection)
        if (positionedFromMargin) {
            ViewCompat.offsetTopAndBottom(child, -topMargin)
        }
        constrainExpandedPosition(child)
        return laidOut
    }
}
