package com.skedgo.tripkit.ui.utils

import android.view.View
import android.view.accessibility.AccessibilityNodeInfo

// to update Accessibility click read, from "Double tap to active" to
// "Double tap to {clickActionLabel}"
fun View.updateClickActionAccessibilityLabel(clickActionLabel: String) {
    accessibilityDelegate = object : View.AccessibilityDelegate() {
        override fun onInitializeAccessibilityNodeInfo(
            host: View,
            info: AccessibilityNodeInfo
        ) {
            super.onInitializeAccessibilityNodeInfo(host, info)
            info.addAction(
                AccessibilityNodeInfo.AccessibilityAction(
                    AccessibilityNodeInfo.ACTION_CLICK,
                    clickActionLabel
                )
            )
        }
    }
}