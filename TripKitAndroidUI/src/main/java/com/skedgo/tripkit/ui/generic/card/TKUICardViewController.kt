package com.skedgo.tripkit.ui.generic.card

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.skedgo.tripkit.ui.R
import com.skedgo.tripkit.ui.core.BaseBottomSheetDialogFragment
import com.skedgo.tripkit.ui.databinding.FragmentTkuiCardBinding
import com.skedgo.tripkit.ui.map.home.TripKitMapFragment


/**
 * A generic [BottomSheetDialogFragment] that dynamically hosts a content [Fragment] and allows
 * extensive configuration through the hosted fragment itself.
 *
 * This implementation supports:
 * - Optional close button visibility
 * - Dynamic loading of any Fragment class inside the bottom sheet
 * - Configurable peek height, behavior state, and hideable flag via the hosted [TKUICardBaseFragment]
 * - Disabling dismissal by outside touch
 * - Full-height bottom sheet layout
 * - Holding [TripKitMapFragment] instance and pass it to fragments with [TKUICardBaseFragment] instance
 *
 * The hosted fragment can optionally extend [TKUICardBaseFragment] to provide custom behavior such as:
 * - Initial [BottomSheetBehavior] state (`EXPANDED`, `HALF_EXPANDED`, etc.)
 * - Custom peek height via `@dimen` resource
 * - Whether the bottom sheet is hideable
 *
 * ### Example Usage
 * ```kotlin
 * val args = Bundle().apply {
 *     putString("key", "value")
 * }
 *
 * val sheet = TKUICardViewController.newInstance(
 *     showClose = true,
 *     fragmentClass = MyCustomFragment::class.java,
 *     fragmentArgs = args
 * )
 * sheet.show(parentFragmentManager, "TKUICard")
 * ```
 *
 * ### Closing the Bottom Sheet from the Hosted Fragment
 * ```kotlin
 * (parentFragment as? BottomSheetDialogFragment)?.dismiss()
 * ```
 *
 * @constructor Creates a new instance of [TKUICardViewController]
 * @see TKUICardBaseFragment for customizing behavior
 *
 * ---
 *
 * **Note:**
 * Imitation of `TGCardViewController` from iOS.
 * Currently, it is a separate library/module on iOS, but since we already have a lot of
 * libraries on our side, not sure if it'll be good to have this on another library.
 * Will just put it in the `TripKitUI` module for now.
 */
class TKUICardViewController : BaseBottomSheetDialogFragment<FragmentTkuiCardBinding>() {

    private var mapFragment: TripKitMapFragment? = null
    private var cardManager: TKUICardManager? = null

    override val layoutRes: Int
        get() = R.layout.fragment_tkui_card

    companion object {
        const val TAG = "TKUICardViewController"
        const val ARG_SHOW_CLOSE = "ARG_SHOW_CLOSE"
        const val ARG_FRAGMENT_CLASS_NAME = "ARG_FRAGMENT_CLASS_NAME"

        fun newInstance(
            showClose: Boolean = true,
            fragmentClass: Class<out Fragment>,
            fragmentArgs: Bundle? = null,
            mapFragment: TripKitMapFragment? = null,
            cardManager: TKUICardManager? = null,
            showOverlay: Boolean = true
        ): TKUICardViewController {
            return TKUICardViewController().apply {
                arguments = Bundle().apply {
                    putBoolean(ARG_SHOW_CLOSE, showClose)
                    putString(ARG_FRAGMENT_CLASS_NAME, fragmentClass.name)
                    putBoolean(ARG_SHOW_BACKGROUND_OVERLAY, showOverlay)
                    fragmentArgs?.let { putBundle("fragmentArgs", it) }
                }
                this.mapFragment = mapFragment
                this.cardManager = cardManager
            }
        }
    }

    override fun getTheme(): Int = com.google.android.material.R.style.Theme_MaterialComponents_BottomSheetDialog

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val showClose = arguments?.getBoolean(ARG_SHOW_CLOSE) ?: true
        var behaviorState = BottomSheetBehavior.STATE_EXPANDED
        val fragmentClassName = arguments?.getString(ARG_FRAGMENT_CLASS_NAME)
        var peekHeightValue = resources.getDimensionPixelSize(R.dimen.bottom_sheet_peek_height)
        var isHideable = false

        binding.btnClose.apply {
            visibility = if (showClose) View.VISIBLE else View.GONE
            setOnClickListener { dismiss() }
        }

        if (fragmentClassName != null) {
            val fragmentClass = Class.forName(fragmentClassName).asSubclass(Fragment::class.java)
            val fragmentArgs = arguments?.getBundle("fragmentArgs")
            val fragment = fragmentClass.newInstance().apply {
                arguments = fragmentArgs
            }

            if(fragment is TKUICardBaseFragment<*>) {
                behaviorState = fragment.defaultCardSettings().startingState
                peekHeightValue = fragment.defaultCardSettings().peekHeight
                fragment.mapFragment = mapFragment
                fragment.cardManager = cardManager
            }

            childFragmentManager.beginTransaction()
                .replace(R.id.contentFrame, fragment)
                .commitNowAllowingStateLoss()
        }

        dialog?.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)?.apply {
            layoutParams?.height = ViewGroup.LayoutParams.MATCH_PARENT
            requestLayout()

            val behavior = BottomSheetBehavior.from(this)
            behavior.peekHeight = peekHeightValue
        }

        dialog?.setCanceledOnTouchOutside(false)
        dialog?.setOnShowListener { dialog ->
            val bottomSheet = (dialog as? BottomSheetDialog)
                ?.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
            val behavior = BottomSheetBehavior.from(bottomSheet!!)
            behavior.isFitToContents = false
            behavior.halfExpandedRatio = 0.5f
            behavior.expandedOffset = 0
            behavior.state = behaviorState
            behavior.isHideable = isHideable
        }
    }

    fun generateTag() = "$TAG:${arguments?.getString(ARG_FRAGMENT_CLASS_NAME)}"
}
