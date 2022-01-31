package com.skedgo.tripkit.ui.core

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import com.skedgo.tripkit.ui.trippreview.TripPreviewPagerFragment
import com.skedgo.tripkit.ui.utils.AccessibilityDefaultViewManager

open class BaseTripKitFragment : Fragment() {

    val accessibilityDefaultViewManager: AccessibilityDefaultViewManager by lazy {
        AccessibilityDefaultViewManager(context)
    }

    var onCloseButtonListener: View.OnClickListener? = null
    var tripPreviewPagerListener: TripPreviewPagerFragment.Listener? = null

    fun setOnCloseButtonListener(listener: (View?) -> Unit) {
        this.onCloseButtonListener = View.OnClickListener { v -> listener(v) }
    }

    protected val autoDisposable = AutoDisposable()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        autoDisposable.bindTo(this.lifecycle)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        accessibilityDefaultViewManager.setAccessibilityObserver()
    }

    override fun onResume() {
        super.onResume()

        accessibilityDefaultViewManager.focusAccessibilityDefaultView(false)
    }

    open fun refresh(position: Int) {}
}