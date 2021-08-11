package com.skedgo.tripkit.ui.trippreview.drt

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.google.gson.Gson
import com.skedgo.tripkit.routing.TripSegment
import com.skedgo.tripkit.ui.R
import com.skedgo.tripkit.ui.core.BaseFragment
import com.skedgo.tripkit.ui.core.BaseTripKitFragment
import com.skedgo.tripkit.ui.core.addTo
import com.skedgo.tripkit.ui.databinding.FragmentDrtBinding
import com.skedgo.tripkit.ui.dialog.GenericListDialogFragment
import com.skedgo.tripkit.ui.dialog.GenericListItem
import com.skedgo.tripkit.ui.trippreview.TripPreviewPagerItemViewModel
import com.skedgo.tripkit.ui.trippreview.standard.StandardTripPreviewItemFragment
import io.reactivex.android.schedulers.AndroidSchedulers
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

class DrtFragment : BaseFragment<FragmentDrtBinding>(), DrtHandler {

    private val viewModel: DrtViewModel by viewModels()

    private val pagerItemViewModel: TripPreviewPagerItemViewModel by viewModels()

    private var segment: TripSegment? = null

    override val layoutRes: Int
        get() = R.layout.fragment_drt

    override fun onBook() {
        viewModel.setBookingInProgress(true)
    }

    override fun onCreated(savedInstance: Bundle?) {
        initBinding()
        initObserver()
    }

    private fun initBinding() {
        binding.lifecycleOwner = this
        binding.viewModel = viewModel
        binding.pagerItemViewModel = pagerItemViewModel
        binding.handler = this
    }

    private fun initObserver() {
        viewModel.onItemChangeActionStream
                .onEach { drtItem ->

                    if (viewModel.bookingInProgress.value != true) {
                        when (drtItem.label.value) {
                            DrtItem.MOBILITY_OPTIONS -> {
                                GenericListDialogFragment.newInstance(
                                        GenericListItem.parse(
                                                listOf("Test 1", "Test 2", "Test 3")
                                        ), isSingleSelection = false,
                                        title = DrtItem.MOBILITY_OPTIONS,
                                        onConfirmCallback = { selectedItems ->
                                            drtItem.setValue(
                                                    if (selectedItems.isEmpty()) {
                                                        listOf("Tap Change to make selections")
                                                    } else {
                                                        selectedItems.map { it.label }
                                                    }
                                            )
                                        }
                                ).show(childFragmentManager, DrtItem.MOBILITY_OPTIONS)
                            }
                            DrtItem.PURPOSE -> {
                                GenericListDialogFragment.newInstance(
                                        GenericListItem.parse(
                                                listOf("Test A", "Test B", "Test C")
                                        ), isSingleSelection = true,
                                        title = DrtItem.PURPOSE,
                                        onConfirmCallback = { selectedItems ->
                                            drtItem.setValue(
                                                    if (selectedItems.isEmpty()) {
                                                        listOf("Tap Change to make selections")
                                                    } else {
                                                        selectedItems.map { it.label }
                                                    }
                                            )
                                        }
                                ).show(childFragmentManager, DrtItem.PURPOSE)
                            }
                            DrtItem.ADD_NOTE -> {
                                //TODO Open Add note

                            }
                        }
                    }
                }.launchIn(lifecycleScope)
        segment?.let {
            pagerItemViewModel.setSegment(requireContext(), it)
        }

        pagerItemViewModel.closeClicked.observable
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { onCloseButtonListener?.onClick(null) }.addTo(autoDisposable)
    }

    companion object {
        fun newInstance(segment: TripSegment): DrtFragment {
            val fragment = DrtFragment()
            fragment.segment = segment
            return fragment
        }
    }
}