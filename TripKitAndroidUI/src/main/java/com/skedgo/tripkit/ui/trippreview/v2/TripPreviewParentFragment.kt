package com.skedgo.tripkit.ui.trippreview.v2

import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.view.View
import android.view.ViewTreeObserver
import android.view.Gravity
import android.widget.FrameLayout
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModelProvider
import com.skedgo.tripkit.routing.TripSegment
import com.skedgo.tripkit.ui.R
import com.skedgo.tripkit.ui.TripKitUI
import com.skedgo.tripkit.ui.core.BaseDialog
import com.skedgo.tripkit.ui.core.BaseFragment
import com.skedgo.tripkit.ui.databinding.FragmentTripPreviewParentBinding
import com.skedgo.tripkit.ui.utils.ITEM_QUICK_BOOKING
import com.skedgo.tripkit.ui.utils.ITEM_SERVICE
import com.skedgo.tripkit.ui.utils.observe
import com.skedgo.tripkit.ui.utils.applyEdgeToEdgeSafeAreaPadding
import javax.inject.Inject

/**
 * To serve as a template for dialog based screens
 * TODO rename to a more generic name
 */
open class TripPreviewParentFragment : BaseDialog<FragmentTripPreviewParentBinding>() {

    override val isFullScreen: Boolean
        get() = true
    override val isFullWidth: Boolean
        get() = false
    override val isTransparentBackground: Boolean
        get() = true

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory

    protected val viewModel: TripPreviewParentViewModel by viewModels { viewModelFactory }

    protected lateinit var segment: TripSegment

    private var toolbarLayoutListener: ViewTreeObserver.OnGlobalLayoutListener? = null

    companion object {
        fun newInstance(segment: TripSegment): TripPreviewParentFragment =
            TripPreviewParentFragment().apply {
                this.segment = segment
            }
    }

    override fun onDismiss(dialog: DialogInterface) {
        onDismissed()
        super.onDismiss(dialog)
    }

    override fun onAttach(context: Context) {
        TripKitUI.getInstance().tripPreviewComponent().inject(this)
        super.onAttach(context)
    }

    override val layoutRes: Int
        get() = R.layout.fragment_trip_preview_parent

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.FullScreenDialog)
    }

    override fun onCreated(savedInstance: Bundle?) {
        binding.viewModel = viewModel
        binding.lifecycleOwner = viewLifecycleOwner
        initObservers()
        viewModel.setTripSegment(segment)
        //
        initViews()
    }

    fun getContainerResourceId() = R.id.content

    open fun showQuickBooking() {}

    override fun onClose() {}

    override fun onBackPressed() {
        val fragmentManager = getCurrentFragmentManager()
        if(fragmentManager.backStackEntryCount > 1) {
            fragmentManager.popBackStack()
        } else {
            onClose()
        }
    }

    open fun onSecondaryActionClick(segment: TripSegment?) {}

    open fun onDismissed() {}

    open fun getCurrentFragmentManager(): FragmentManager = childFragmentManager

    open fun goTo(fragment: Fragment) {}

    override fun onDestroyView() {
        toolbarLayoutListener?.let { listener ->
            binding.toolbar.viewTreeObserver.removeOnGlobalLayoutListener(listener)
        }
        toolbarLayoutListener = null
        super.onDestroyView()
    }

    private fun initObservers() {
        viewModel.apply {
            observe(segmentItemType) {
                it?.let { handleItemType(it) }
            }
        }
    }

    private fun initViews() {
        binding.layoutBack.setOnClickListener { onBackPressed() }
        binding.tvSecondaryAction.setOnClickListener { onSecondaryActionClick(segment) }
        binding.toolbar.applyEdgeToEdgeSafeAreaPadding(applyLeft = true, applyTop = true, applyRight = true)
        updateToolbarTitlePadding()
        val listener = ViewTreeObserver.OnGlobalLayoutListener { updateToolbarTitlePadding() }
        toolbarLayoutListener = listener
        binding.toolbar.viewTreeObserver.addOnGlobalLayoutListener(listener)
        viewModel.toolbarState.observe(viewLifecycleOwner) {
            if (view != null) binding.toolbar.post { updateToolbarTitlePadding() }
        }
    }

    /**
     * Keep the centered title between actions when words fit. At larger fonts, give it
     * a separate wrapping row instead of splitting words into the tiny middle zone.
     */
    private fun updateToolbarTitlePadding() {
        if (view == null) return
        val title = binding.toolbarTitle
        val back = binding.layoutBack
        val secondary = binding.tvSecondaryAction
        val width = (title.parent as View).width
        if (width == 0) return
        val backWidth = if (back.visibility == View.VISIBLE) back.width else 0
        val secondaryVisible = secondary.visibility == View.VISIBLE
        val endPadding = if (secondaryVisible) secondary.width else backWidth
        val longestWord = title.text.toString().split(Regex("\\s+"))
            .maxOfOrNull { title.paint.measureText(it) } ?: 0f
        val belowActions = longestWord > width - backWidth - endPadding
        val start = if (belowActions) 0 else backWidth.coerceAtLeast(0)
        val end = if (belowActions) 0 else endPadding.coerceAtLeast(0)
        if (title.paddingLeft != start || title.paddingRight != end) {
            title.setPadding(start, title.paddingTop, end, title.paddingBottom)
        }
        val params = title.layoutParams as FrameLayout.LayoutParams
        val margin = if (belowActions) binding.toolbarActions.height else 0
        val gravity = if (belowActions) Gravity.TOP else Gravity.CENTER_VERTICAL
        if (params.topMargin != margin || params.gravity != gravity) {
            params.topMargin = margin
            params.gravity = gravity
            title.layoutParams = params
        }
    }

    private fun handleItemType(type: Int) {
        when(type) {
            ITEM_QUICK_BOOKING, ITEM_SERVICE -> {
                showQuickBooking()
            }
            else -> {
                // Do nothing for now, just navigate back
                onBackPressed()
            }
        }
    }

}
