package com.skedgo.tripkit.ui.servicedetail

import android.view.View
import androidx.coordinatorlayout.widget.CoordinatorLayout
import com.google.android.material.bottomsheet.BottomSheetBehavior

/** Coordinates timeline stop focus with its enclosing sheet's layout. */
internal class ServiceStopMapFocus(private val view: View) {
    private var pending: Runnable? = null
    private var sheet: BottomSheetBehavior<View>? = null
    private var callback: BottomSheetBehavior.BottomSheetCallback? = null

    fun select(focus: () -> Unit) {
        clear()
        // The host's selection event requests a sheet state change on the main queue.
        // Check after that request so its padding/camera update cannot cancel our focus.
        val request = Runnable {
            pending = null
            val behavior = findSheet()
            if (behavior != null && behavior.state.isObscuringMap()) {
                val listener = object : BottomSheetBehavior.BottomSheetCallback() {
                    override fun onSlide(bottomSheet: View, slideOffset: Float) = Unit

                    override fun onStateChanged(bottomSheet: View, newState: Int) {
                        if (!newState.isObscuringMap()) {
                            clear()
                            focus()
                        }
                    }
                }
                sheet = behavior
                callback = listener
                behavior.addBottomSheetCallback(listener)
            } else {
                focus()
            }
        }
        pending = request
        view.post(request)
    }

    fun clear() {
        pending?.let { view.removeCallbacks(it) }
        pending = null
        callback?.let { sheet?.removeBottomSheetCallback(it) }
        callback = null
        sheet = null
    }

    @Suppress("UNCHECKED_CAST")
    private fun findSheet(): BottomSheetBehavior<View>? {
        var ancestor: View? = view
        while (ancestor != null) {
            val behavior = (ancestor.layoutParams as? CoordinatorLayout.LayoutParams)?.behavior
            if (behavior is BottomSheetBehavior<*>) return behavior as BottomSheetBehavior<View>
            ancestor = ancestor.parent as? View
        }
        return null
    }

    // An expanded sheet still covers the map. Its transition may itself be queued
    // by Material while layout is pending, so wait for the visible stable state.
    private fun Int.isObscuringMap() = this == BottomSheetBehavior.STATE_EXPANDED ||
        this == BottomSheetBehavior.STATE_DRAGGING || this == BottomSheetBehavior.STATE_SETTLING
}
