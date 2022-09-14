package com.skedgo.tripkit.ui.core

import android.os.Bundle
import android.os.Handler
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import androidx.annotation.LayoutRes
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import androidx.fragment.app.Fragment
import com.google.gson.Gson
import com.skedgo.tripkit.ui.utils.getAccessibilityManager
import com.skedgo.tripkit.ui.utils.isTalkBackOn

/**
 * To act as a super class for all other fragments.
 * Passing ViewDataBinding to include initialization which is common for all fragments
 * that's using databinding
 * Sample usage SampleFragment<FragmentSampleBinding>
 */
abstract class BaseFragment<V : ViewDataBinding> : BaseTripKitPagerFragment() {

    protected val gson = Gson()

    @get:LayoutRes
    protected abstract val layoutRes: Int

    protected lateinit var baseView: View

    protected lateinit var binding: V

    protected abstract fun onCreated(savedInstance: Bundle?)

    protected abstract val observeAccessibility: Boolean

    protected abstract fun getDefaultViewForAccessibility(): View?

    private var previouslyInitialized = false

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View {
        if (!::binding.isInitialized) {
            binding = DataBindingUtil.inflate(inflater, layoutRes, container, false)
        } else {
            previouslyInitialized = true
        }
        baseView = binding.root

        return baseView
    }

    override fun onDestroyView() {
        super.onDestroyView()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (!previouslyInitialized) {
            onCreated(savedInstanceState)
        }

        if (observeAccessibility) {
            try {
                context?.getAccessibilityManager()?.let {
                    it.addAccessibilityStateChangeListener {
                        focusAccessibilityDefaultView(true)
                    }
                }
            } catch (e: Exception) {}
        }
    }

    override fun onResume() {
        super.onResume()
        focusAccessibilityDefaultView(false)
    }

    protected fun focusAccessibilityDefaultView(withDelay: Boolean) {
        try {
            Handler().postDelayed({
                try {
                    if (context?.isTalkBackOn() == true) {
                        getDefaultViewForAccessibility()?.apply {
                            performAccessibilityAction(
                                AccessibilityNodeInfo.ACTION_ACCESSIBILITY_FOCUS,
                                null
                            )
                            sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_FOCUSED)
                        }
                    }
                } catch (e: Exception) {
                }
            }, if (withDelay) 500 else 0)
        } catch (e: Exception) {}
    }

}