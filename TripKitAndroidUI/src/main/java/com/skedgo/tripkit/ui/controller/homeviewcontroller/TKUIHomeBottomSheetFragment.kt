package com.skedgo.tripkit.ui.controller.homeviewcontroller

import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.skedgo.tripkit.ui.R
import com.skedgo.tripkit.ui.TripKitUI
import com.skedgo.tripkit.ui.controller.ViewControllerEvent
import com.skedgo.tripkit.ui.controller.ViewControllerEventBus
import com.skedgo.tripkit.ui.controller.locationsearchcontroller.TKUILocationSearchViewControllerFragment
import com.skedgo.tripkit.ui.controller.routeviewcontroller.TKUIRouteFragment
import com.skedgo.tripkit.ui.controller.timetableviewcontroller.TKUITimetableControllerFragment
import com.skedgo.tripkit.ui.controller.tripdetailsviewcontroller.TKUITripDetailsViewControllerFragment
import com.skedgo.tripkit.ui.core.BaseFragment
import com.skedgo.tripkit.ui.core.BaseTripKitFragment
import com.skedgo.tripkit.ui.databinding.FragmentTkuiHomeBottomSheetBinding
import com.skedgo.tripkit.ui.utils.deFocusAndHideKeyboard
import timber.log.Timber
import javax.inject.Inject

class TKUIHomeBottomSheetFragment : BaseFragment<FragmentTkuiHomeBottomSheetBinding>() {

    @Inject
    lateinit var eventBus: ViewControllerEventBus

    private var listener: TKUIHomeBottomSheetListener? = null
    var currentFragmentTag: String? = ""

    override val layoutRes: Int
        get() = R.layout.fragment_tkui_home_bottom_sheet

    override fun onAttach(context: Context) {
        TripKitUI.getInstance().controllerComponent().inject(this)
        super.onAttach(context)
    }

    override fun onCreated(savedInstance: Bundle?) {

    }

    override val observeAccessibility: Boolean
        get() = false

    override fun getDefaultViewForAccessibility(): View? = null

    fun update(fragment: Fragment, tag: String? = null) {
        currentFragmentTag = tag
        childFragmentManager
            .beginTransaction()
            .replace(R.id.container, fragment, tag)
            .addToBackStack(tag)
            .commit()

        eventBus.publish(
            ViewControllerEvent.OnBottomSheetFragmentCountUpdate(
                childFragmentManager.backStackEntryCount + 1
            )
        )
    }

    fun popActiveFragment() {

        eventBus.publish(
            ViewControllerEvent.OnBottomSheetFragmentCountUpdate(
                childFragmentManager.backStackEntryCount - 1
            )
        )

        requireContext().deFocusAndHideKeyboard(
            requireActivity().currentFocus
                ?: view?.rootView
        )

        if (childFragmentManager.backStackEntryCount == 1) {
            listener?.refreshMap()
        }

        if(childFragmentManager.findFragmentByTag(TKUITripDetailsViewControllerFragment.TAG)?.isVisible == true) {
            listener?.reloadMapMarkers()
        }

        listener?.removePinnedLocationMarker()

        checkFragmentAndClearInstances()

        childFragmentManager.popBackStackImmediate()

        listener?.onFragmentPopped()
    }

    private fun checkFragmentAndClearInstances() {
        childFragmentManager.fragments.firstOrNull { it.isVisible }?.let {
            if(it is BaseFragment<*>) {
                it.clearInstances()
            }
        }
    }

    fun getFragmentByTag(tag: String): Fragment?
        = childFragmentManager.findFragmentByTag(tag)

    interface TKUIHomeBottomSheetListener {
        fun refreshMap()
        fun removePinnedLocationMarker()
        fun reloadMapMarkers()
        fun onFragmentPopped()
    }

    companion object {
        fun newInstance(listener: TKUIHomeBottomSheetListener? = null) =
            TKUIHomeBottomSheetFragment().apply {
                this.listener = listener
            }
    }
}