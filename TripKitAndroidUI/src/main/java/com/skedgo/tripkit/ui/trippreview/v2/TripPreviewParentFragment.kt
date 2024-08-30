package com.skedgo.tripkit.ui.trippreview.v2

import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModelProvider
import com.skedgo.tripkit.routing.TripSegment
import com.skedgo.tripkit.ui.R
import com.skedgo.tripkit.ui.TripKitUI
import com.skedgo.tripkit.ui.core.BaseFragment
import com.skedgo.tripkit.ui.databinding.FragmentTripPreviewParentBinding
import com.skedgo.tripkit.ui.utils.ITEM_QUICK_BOOKING
import com.skedgo.tripkit.ui.utils.observe
import javax.inject.Inject

open class TripPreviewParentFragment : BaseFragment<FragmentTripPreviewParentBinding>() {

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory

    protected val viewModel: TripPreviewParentViewModel by viewModels { viewModelFactory }

    protected lateinit var segment: TripSegment

    companion object {
        fun newInstance(segment: TripSegment): TripPreviewParentFragment =
            TripPreviewParentFragment().apply {
                this.segment = segment
            }
    }

    override fun onAttach(context: Context) {
        TripKitUI.getInstance().tripPreviewComponent().inject(this)
        super.onAttach(context)
    }

    override val layoutRes: Int
        get() = R.layout.fragment_trip_preview_parent

    override val observeAccessibility: Boolean
        get() = false

    override fun getDefaultViewForAccessibility(): View? = null

    override fun onCreated(savedInstance: Bundle?) {
        binding.viewModel = viewModel
        binding.lifecycleOwner = viewLifecycleOwner
        initObservers()
        viewModel.setTripSegment(segment)
    }

    private fun initObservers() {
        viewModel.apply {
            observe(segmentItemType) {
                it?.let { handleItemType(it) }
            }
        }
    }

    private fun handleItemType(type: Int) {
        when(type) {
            ITEM_QUICK_BOOKING -> {
                showQuickBooking()
            }
            else -> {
                // Do nothing for now
            }
        }
    }

    protected fun getContainerResourceId() = R.id.content

    open fun showQuickBooking() {}
}