package com.skedgo.tripkit.ui.utils

import android.content.Context
import android.util.AttributeSet
import androidx.core.widget.NestedScrollView

/**
 * A custom NestedScrollView that controls whether parent views can intercept touch events.
 *
 * - Forces the behavior of `requestDisallowInterceptTouchEvent` to either always allow
 *   or disallow parent interception based on the request.
 * - Useful inside complex nested scroll hierarchies (e.g., ViewPager2 inside BottomSheet)
 *   where you want to handle scroll gestures more predictably.
 *
 * Example Use Case:
 * - Prevent BottomSheet or parent ScrollViews from hijacking scroll events while
 *   user is interacting with this NestedScrollView.
 */
class LockableNestedScrollView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : NestedScrollView(context, attrs) {

    override fun requestDisallowInterceptTouchEvent(disallowIntercept: Boolean) {
        // Always disallow parent to intercept
        if (disallowIntercept) {
            super.requestDisallowInterceptTouchEvent(true)
        } else {
            super.requestDisallowInterceptTouchEvent(false)
        }
    }
}