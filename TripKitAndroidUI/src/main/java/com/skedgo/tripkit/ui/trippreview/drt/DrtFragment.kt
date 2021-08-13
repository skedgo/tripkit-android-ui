package com.skedgo.tripkit.ui.trippreview.drt

import android.content.Context
import android.os.Bundle
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.skedgo.tripkit.booking.quickbooking.QuickBookingType
import com.skedgo.tripkit.routing.TripSegment
import com.skedgo.tripkit.ui.R
import com.skedgo.tripkit.ui.TripKitUI
import com.skedgo.tripkit.ui.core.BaseFragment
import com.skedgo.tripkit.ui.core.addTo
import com.skedgo.tripkit.ui.databinding.FragmentDrtBinding
import com.skedgo.tripkit.ui.dialog.GenericListDialogFragment
import com.skedgo.tripkit.ui.dialog.GenericListItem
import com.skedgo.tripkit.ui.trippreview.TripPreviewPagerItemViewModel
import io.reactivex.android.schedulers.AndroidSchedulers
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject

class DrtFragment : BaseFragment<FragmentDrtBinding>(), DrtHandler {

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory

    private val viewModel: DrtViewModel by viewModels { viewModelFactory }

    private val pagerItemViewModel: TripPreviewPagerItemViewModel by viewModels()

    private var segment: TripSegment? = null

    override val layoutRes: Int
        get() = R.layout.fragment_drt

    override fun onBook() {
        viewModel.setBookingInProgress(true)
    }

    override fun onAttach(context: Context) {
        TripKitUI.getInstance().tripPreviewComponent().inject(this)
        super.onAttach(context)
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

                        if (drtItem.label.value == DrtItem.ADD_NOTE) {
                            //ADD Note
                        } else {
                            GenericListDialogFragment.newInstance(
                                    GenericListItem.parseOptions(
                                            drtItem.options.value ?: emptyList()
                                    ),
                                    isSingleSelection = drtItem.type.value == QuickBookingType.SINGLE_CHOICE,
                                    title = drtItem.label.value ?: "",
                                    onConfirmCallback = { selectedItems ->
                                        drtItem.setValue(
                                                if (selectedItems.isEmpty()) {
                                                    listOf(
                                                            viewModel.getDefaultValueByType(
                                                                    drtItem.type.value ?: "",
                                                                    drtItem.label.value ?: ""
                                                            )
                                                    )
                                                } else {
                                                    selectedItems.map { it.label }
                                                }
                                        )
                                    }
                            ).show(childFragmentManager, drtItem.label.value ?: "")
                        }

                        /*
                        when (drtItem.label.value) {
                            DrtItem.MOBILITY_OPTIONS -> {
                                GenericListDialogFragment.newInstance(
                                        GenericListItem.parseOptions(
                                                drtItem.options.value ?: emptyList()
                                        ), isSingleSelection = false,
                                        title = drtItem.label.value ?: "",
                                        onConfirmCallback = { selectedItems ->
                                            drtItem.setValue(
                                                    if (selectedItems.isEmpty()) {
                                                        listOf("Tap Change to make selections")
                                                    } else {
                                                        selectedItems.map { it.label }
                                                    }
                                            )
                                        }
                                ).show(childFragmentManager, drtItem.label.value ?: "")
                            }
                            DrtItem.PURPOSE -> {
                                GenericListDialogFragment.newInstance(
                                        GenericListItem.parseOptions(
                                                drtItem.options.value ?: emptyList()
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
                        */
                    }
                }.launchIn(lifecycleScope)
        segment?.let {
            pagerItemViewModel.setSegment(requireContext(), it)
            viewModel.setTripSegment(it)
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