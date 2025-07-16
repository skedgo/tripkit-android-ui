package com.skedgo.tripkit.ui.generic.bottomsheet

import android.content.Context
import android.os.Bundle
import androidx.annotation.IdRes
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.skedgo.tripkit.ui.generic.card.TKUICardBaseFragment
import com.skedgo.tripkit.ui.generic.card.TKUICardDataManager
import com.skedgo.tripkit.ui.map.home.TripKitMapFragment
import com.skedgo.tripkit.ui.utils.isTalkBackOn

/**
 * Manages the display of card-based [Fragment]s inside a bottom sheet and integrates
 * with [TripKitMapFragment] and [TKUICardDataManager].
 *
 * This controller handles the instantiation, setup, and replacement of fragments
 * within a content frame, providing support for accessibility (TalkBack) and optional
 * back stack navigation.
 *
 * @property context The [Context] used to check accessibility features like TalkBack.
 * @property fragmentManager The [FragmentManager] used to add/replace fragments.
 * @property contentFrameId The resource ID of the container where fragments will be displayed.
 * @property bottomSheetCardsManager The [BottomSheetCardsManager] responsible for configuring fragments in a bottom sheet.
 */
class TKUICardViewControllerManager(
    private val context: Context,
    private val fragmentManager: FragmentManager,
    @IdRes private val contentFrameId: Int,
    private val bottomSheetCardsManager: BottomSheetCardsManager
) {
    var currentFragment: Fragment? = null
    var cardManager: TKUICardDataManager? = null
    var mapFragment: TripKitMapFragment? = null

    /**
     * Displays a new card fragment of the specified class type.
     *
     * @param T The type of [Fragment] to be shown.
     * @param fragmentClass The class of the fragment to be instantiated and shown.
     * @param fragmentArgs Optional [Bundle] arguments for the fragment.
     * @param mapFragment Optional map fragment to be passed into the card fragment.
     * @param cardManager Optional card data manager to be passed into the card fragment.
     * @param addToBackStack Whether to add the transaction to the back stack.
     * @return The instantiated and displayed fragment.
     */
    fun <T> showCard(
        fragmentClass: Class<T>,
        fragmentArgs: Bundle? = null,
        mapFragment: TripKitMapFragment? = null,
        cardManager: TKUICardDataManager? = null,
        addToBackStack: Boolean = false
    ): T where T : Fragment {
        this.mapFragment = mapFragment
        this.cardManager = cardManager

        val fragment = fragmentClass.newInstance().apply {
            arguments = fragmentArgs
        }

        if (fragment is TKUICardBaseFragment<*>) {
            fragment.mapFragment = mapFragment
            fragment.cardDataManager = cardManager
        }

        if (fragment is CardableFragment) {
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

        return fragment
    }

    /**
     * Displays a new card using an already instantiated [TKUICardBaseFragment].
     *
     * @param fragment The fragment instance to be shown.
     * @param mapFragment Optional map fragment to be passed into the card fragment.
     * @param cardManager Optional card data manager to be passed into the card fragment.
     * @param addToBackStack Whether to add the transaction to the back stack.
     */
    fun <T> showCard(
        fragment: TKUICardBaseFragment<*>,
        mapFragment: TripKitMapFragment? = null,
        cardManager: TKUICardDataManager? = null,
        addToBackStack: Boolean = false
    ) {
        this.mapFragment = mapFragment
        this.cardManager = cardManager

        fragment.mapFragment = mapFragment
        fragment.cardDataManager = cardManager

        bottomSheetCardsManager.setupFragment(
            fragment,
            if (context.isTalkBackOn()) {
                BottomSheetBehavior.STATE_EXPANDED
            } else {
                null
            }
        )

        currentFragment = fragment

        val transaction = fragmentManager.beginTransaction()
            .replace(contentFrameId, fragment)

        if (addToBackStack) {
            transaction.addToBackStack(fragment::class.java.simpleName)
        }

        transaction.commitAllowingStateLoss()
    }

    fun getFragmentByTag(tag: String) = fragmentManager.findFragmentByTag(tag)
    fun getFragmentById(id: Int) = fragmentManager.findFragmentById(id)

}
