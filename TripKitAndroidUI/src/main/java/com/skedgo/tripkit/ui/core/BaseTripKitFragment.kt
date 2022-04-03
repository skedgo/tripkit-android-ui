package com.skedgo.tripkit.ui.core

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import com.skedgo.tripkit.ui.dialog.GenericLoadingDialog
import com.skedgo.tripkit.ui.trippreview.TripPreviewPagerFragment
import com.skedgo.tripkit.ui.utils.AccessibilityDefaultViewManager

open class BaseTripKitFragment : Fragment() {

    private val loadingDialog: GenericLoadingDialog by lazy(mode = LazyThreadSafetyMode.NONE) {
        GenericLoadingDialog(requireContext())
    }

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

    fun showLoading(isLoading: Boolean) {
        loadingDialog.let {
            if (isLoading && !loadingDialog.isShowing)
                loadingDialog.show()
            else if (!isLoading && loadingDialog.isShowing) {
                loadingDialog.dismiss()
            }
        }
    }

    override fun onDestroyView() {
        loadingDialog.dismiss()
        super.onDestroyView()
    }
}