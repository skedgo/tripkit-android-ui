package com.skedgo.tripkit.ui.trippreview.drt

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.skedgo.tripkit.routing.TripSegment
import com.skedgo.tripkit.ui.R
import com.skedgo.tripkit.ui.core.BaseFragment
import com.skedgo.tripkit.ui.core.BaseTripKitFragment
import com.skedgo.tripkit.ui.databinding.FragmentDrtBinding
import com.skedgo.tripkit.ui.trippreview.TripPreviewPagerItemViewModel
import com.skedgo.tripkit.ui.trippreview.standard.StandardTripPreviewItemFragment
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

class DrtFragment : BaseFragment<FragmentDrtBinding>() {

    private val viewModel: DrtViewModel by viewModels()

    private val pagerItemViewModel: TripPreviewPagerItemViewModel by viewModels()

    private var segment: TripSegment? = null

    override val layoutRes: Int
        get() = R.layout.fragment_drt

    override fun onCreated(savedInstance: Bundle?) {
        initBinding()
        initObserver()
    }

    private fun initBinding() {
        binding.lifecycleOwner = this
        binding.viewModel = viewModel
        binding.pagerItemViewModel = pagerItemViewModel
    }

    private fun initObserver() {
        viewModel.onItemChangeActionStream
                .onEach {
                    when (it.label.value) {
                        DrtItem.MOBILITY_OPTIONS -> {
                            //TODO Open Mobility option list
                        }
                        DrtItem.PURPOSE -> {
                            //TODO Open Purpose list
                        }
                        DrtItem.ADD_NOTE -> {
                            //TODO Open Add note
                        }
                    }
                    Log.e("MIKE", "value: ${it.label.value}")
                }.launchIn(lifecycleScope)
        segment?.let {
            pagerItemViewModel.setSegment(requireContext(), it)
        }
    }

    companion object {
        fun newInstance(segment: TripSegment): DrtFragment {
            val fragment = DrtFragment()
            fragment.segment = segment
            return fragment
        }
    }
}