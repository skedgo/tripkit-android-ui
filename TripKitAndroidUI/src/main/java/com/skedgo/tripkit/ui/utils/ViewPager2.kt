package com.skedgo.tripkit.ui.utils

import android.view.MotionEvent
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2


fun ViewPager2.setupBottomSheetTouchFix() {
    val recyclerView = getChildAt(0) as? RecyclerView ?: return

    recyclerView.setOnTouchListener { v, event ->
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                v.parent.requestDisallowInterceptTouchEvent(true)
            }
            MotionEvent.ACTION_MOVE -> {
                // Allow BottomSheet to intercept if we can't scroll vertically anymore
                if (!recyclerView.canScrollVertically(-1) && event.y > 0) {
                    // Cannot scroll up anymore and pulling down -> let BottomSheet handle
                    v.parent.requestDisallowInterceptTouchEvent(false)
                } else if (!recyclerView.canScrollVertically(1) && event.y < 0) {
                    // Cannot scroll down anymore and pulling up -> let BottomSheet handle
                    v.parent.requestDisallowInterceptTouchEvent(false)
                } else {
                    // Otherwise ViewPager2/RecyclerView handles the touch
                    v.parent.requestDisallowInterceptTouchEvent(true)
                }
            }
        }
        false
    }
}