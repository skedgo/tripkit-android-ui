package com.skedgo.tripkit.ui.utils

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityManager
import android.view.accessibility.AccessibilityNodeInfo

open class AccessibilityDefaultViewManager constructor(
    private val context: Context?,
    private val observeAccessibility: Boolean = true
) {

    private var viewForAccessibility: View? = null
    open var accessibilityListener: () -> Unit = {}

    fun setDefaultViewForAccessibility(view: View?) {
        viewForAccessibility = view
    }

    private val handler = Handler(Looper.getMainLooper())
    private var registeredManager: AccessibilityManager? = null
    private val observer = AccessibilityManager.AccessibilityStateChangeListener {
        accessibilityListener.invoke()
        focusAccessibilityDefaultView(true)
    }

    fun setAccessibilityObserver() {
        if (observeAccessibility && registeredManager == null) {
            context?.getAccessibilityManager()?.let { manager ->
                manager.addAccessibilityStateChangeListener(observer)
                registeredManager = manager
            }
        }
    }

    fun removeAccessibilityObserver() {
        registeredManager?.removeAccessibilityStateChangeListener(observer)
        registeredManager = null
        handler.removeCallbacksAndMessages(null)
        viewForAccessibility = null
        accessibilityListener = {}
    }

    fun focusAccessibilityDefaultView(withDelay: Boolean, customDelay: Long? = null) {
        handler.postDelayed({
            if (context?.isTalkBackOn() == true) {
                viewForAccessibility?.apply {
                    performAccessibilityAction(
                        AccessibilityNodeInfo.ACTION_ACCESSIBILITY_FOCUS,
                        null
                    )
                    sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_FOCUSED)
                }
            }
        }, if (withDelay) customDelay ?: 500 else 0)

    }
}
