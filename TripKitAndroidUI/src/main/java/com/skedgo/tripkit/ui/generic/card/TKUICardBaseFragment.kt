package com.skedgo.tripkit.ui.generic.card

import androidx.databinding.ViewDataBinding
import com.skedgo.tripkit.ui.core.BaseFragment

abstract class TKUICardBaseFragment<V : ViewDataBinding> : BaseFragment<V>() {

    abstract val behaviorState: Int
    abstract val peekHeightResourceValue: Int
    abstract val isHideable: Boolean

    fun closeDialog() {
        val parentFragment = requireParentFragment()
        if(parentFragment is TKUICardViewController) {
            parentFragment.dismiss()
        }
    }
}