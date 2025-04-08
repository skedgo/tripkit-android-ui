package com.skedgo.tripkit.ui.generic.card

import androidx.databinding.ViewDataBinding
import com.skedgo.tripkit.ui.core.BaseFragment
import com.skedgo.tripkit.ui.map.home.TripKitMapContributor
import com.skedgo.tripkit.ui.map.home.TripKitMapFragment


abstract class TKUICardBaseFragment<V : ViewDataBinding> : BaseFragment<V>() {

    abstract val behaviorState: Int
    abstract val peekHeightResourceValue: Int
    abstract val isHideable: Boolean
    abstract val mapContributor: TripKitMapContributor?

    var mapFragment: TripKitMapFragment? = null

    fun closeDialog() {
        val parentFragment = requireParentFragment()
        if(parentFragment is TKUICardViewController) {
            parentFragment.dismiss()
        }
    }
}