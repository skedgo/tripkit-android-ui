package com.skedgo.tripkit.ui.views

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent

class CustomTouchInterceptor(context: Context, attrs: AttributeSet? = null, defStyle: Int = 0):
        MultiStateView(context, attrs, defStyle) {

    override fun dispatchTouchEvent(ev: MotionEvent?): Boolean {
        val handle = super.dispatchTouchEvent(ev)
        requestDisallowInterceptTouchEvent(true)
        return handle
    }
}