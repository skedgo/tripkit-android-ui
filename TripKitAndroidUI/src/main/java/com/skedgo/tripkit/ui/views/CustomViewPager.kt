package com.skedgo.tripkit.ui.views

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import androidx.viewpager.widget.ViewPager

/**
 * To handle BottomSheet and Recyclerview touch and scroll issues with ViewPager
 */
class CustomViewPager(context: Context, attrs: AttributeSet) : ViewPager(context, attrs) {
    private var initialXValue: Float = 0f
    private var initialYValue: Float = 0f
    private val threshold = 50

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        when (ev.action) {
            MotionEvent.ACTION_DOWN -> {
                initialXValue = ev.x
                initialYValue = ev.y
                parent.requestDisallowInterceptTouchEvent(true)
            }
            MotionEvent.ACTION_MOVE -> {
                val diffX = Math.abs(ev.x - initialXValue)
                val diffY = Math.abs(ev.y - initialYValue)
                if (diffX > threshold && diffX > diffY) {
                    // Horizontal scroll, ViewPager should handle it
                    parent.requestDisallowInterceptTouchEvent(true)
                } else if (diffY > threshold && diffY > diffX) {
                    // Vertical scroll, let the parent (BottomSheet) handle it
                    parent.requestDisallowInterceptTouchEvent(false)
                }
            }
        }
        return super.onInterceptTouchEvent(ev)
    }
}