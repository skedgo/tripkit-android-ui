package com.skedgo.tripkit.ui.generic.bottomsheet

import android.content.Context
import android.os.Bundle
import androidx.annotation.IdRes
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.skedgo.tripkit.ui.generic.card.TKUICardBaseFragment
import com.skedgo.tripkit.ui.generic.card.TKUICardManager
import com.skedgo.tripkit.ui.map.home.TripKitMapFragment
import com.skedgo.tripkit.ui.utils.isTalkBackOn

class TKUICardViewControllerManager(
    private val context: Context,
    private val fragmentManager: FragmentManager,
    @IdRes private val contentFrameId: Int,
    private val bottomSheetCardsManager: BottomSheetCardsManager
) {
    var currentFragment: Fragment? = null
    var cardManager: TKUICardManager? = null
    var mapFragment: TripKitMapFragment? = null

    fun showCard(
        fragmentClass: Class<out Fragment>,
        fragmentArgs: Bundle? = null,
        mapFragment: TripKitMapFragment? = null,
        cardManager: TKUICardManager? = null,
        addToBackStack: Boolean = false
    ) {
        this.mapFragment = mapFragment
        this.cardManager = cardManager

        val fragment = fragmentClass.newInstance().apply {
            arguments = fragmentArgs
        }

        if (fragment is TKUICardBaseFragment<*>) {
            fragment.mapFragment = mapFragment
            fragment.cardManager = cardManager
        }

        if(fragment is CardableFragment) {
            bottomSheetCardsManager.setupFragment(
                fragment,
                if (context.isTalkBackOn()) {
                    BottomSheetBehavior.STATE_EXPANDED
                } else {
                    null
                }
            )
        }

        currentFragment = fragment

        val transaction = fragmentManager.beginTransaction()
            .replace(contentFrameId, fragment)

        if (addToBackStack) {
            transaction.addToBackStack(fragmentClass.name)
        }

        transaction.commitAllowingStateLoss()

    }
}
