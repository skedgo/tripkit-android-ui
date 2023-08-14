package com.skedgo.tripkit.ui.controller.homeviewcontroller

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.skedgo.tripkit.ui.R
import com.skedgo.tripkit.ui.core.BaseFragment
import com.skedgo.tripkit.ui.databinding.FragmentTkuiHomeBottomSheetBinding
import com.skedgo.tripkit.ui.utils.deFocusAndHideKeyboard

class TKUIHomeBottomSheetFragment : BaseFragment<FragmentTkuiHomeBottomSheetBinding>() {

    private val listener: TKUIHomeBottomSheetListener? = null

    override val layoutRes: Int
        get() = R.layout.fragment_tkui_home_bottom_sheet

    override fun onCreated(savedInstance: Bundle?) {

    }

    override val observeAccessibility: Boolean
        get() = false

    override fun getDefaultViewForAccessibility(): View? = null

    fun update(fragment: Fragment, tag: String? = null) {
        childFragmentManager
            .beginTransaction()
            .replace(R.id.container, fragment, tag)
            .addToBackStack(tag)
            .commit()
    }

    fun popActiveFragment() {
        requireContext().deFocusAndHideKeyboard(
            requireActivity().currentFocus
                ?: view?.rootView
        )

        if (childFragmentManager.backStackEntryCount == 1) {
            listener?.refreshMap()
        }

        listener?.removePinnedLocationMarker()

        childFragmentManager.popBackStackImmediate()
    }

    interface TKUIHomeBottomSheetListener {
        fun refreshMap()
        fun removePinnedLocationMarker()
    }
}