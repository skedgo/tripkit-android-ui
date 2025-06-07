package com.skedgo.tripkit.ui.generic.card

import androidx.databinding.ViewDataBinding
import com.skedgo.tripkit.ui.core.BaseFragment
import com.skedgo.tripkit.ui.generic.bottomsheet.CardableFragment
import com.skedgo.tripkit.ui.generic.bottomsheet.TKUICardHost
import com.skedgo.tripkit.ui.map.home.TripKitMapContributor
import com.skedgo.tripkit.ui.map.home.TripKitMapFragment


abstract class TKUICardBaseFragment<V : ViewDataBinding> : BaseFragment<V>(), CardableFragment {

    abstract val mapContributor: TripKitMapContributor?

    var mapFragment: TripKitMapFragment? = null
    var cardManager: TKUICardManager? = null

    val bottomSheetManager by lazy {
        parentFragment?.parentFragment as? TKUICardHost
            ?: parentFragment as? TKUICardHost
            ?: activity as? TKUICardHost
    }

    fun closeDialog() {
        val parentFragment = requireParentFragment()
        if(parentFragment is TKUICardViewController) {
            parentFragment.dismiss()
        }
    }
}