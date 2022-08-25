package com.skedgo.tripkit.ui.utils

import android.content.Context
import android.os.Handler
import android.view.View
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import javax.inject.Inject

open class AccessibilityDefaultViewManager constructor(
        private val context: Context?,
        private val observeAccessibility: Boolean = true
) {

    private var viewForAccessibility: View? = null
    open var accessibilityListener: () -> Unit = {}

    fun setDefaultViewForAccessibility(view: View?) {
        viewForAccessibility = view
    }

    fun setAccessibilityObserver() {
        if (observeAccessibility) {
            context?.getAccessibilityManager()?.let {
                it.addAccessibilityStateChangeListener {
                    accessibilityListener.invoke()
                    focusAccessibilityDefaultView(true)
                }
            }
        }
    }

    fun focusAccessibilityDefaultView(withDelay: Boolean, customDelay: Long? = null) {
        Handler().postDelayed({
            if (context?.isTalkBackOn() == true) {
                viewForAccessibility?.apply {
                    performAccessibilityAction(AccessibilityNodeInfo.ACTION_ACCESSIBILITY_FOCUS, null)
                    sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_FOCUSED)
                }
            }
        }, if (withDelay) customDelay?: 500 else 0)

    }
}
