package com.skedgo.tripkit.ui.trippreview.drt

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.skedgo.tripkit.booking.quickbooking.QuickBookingType
import com.skedgo.tripkit.common.model.BookingConfirmationAction
import com.skedgo.tripkit.routing.TripSegment
import com.skedgo.tripkit.ui.R
import com.skedgo.tripkit.ui.TripKitUI
import com.skedgo.tripkit.ui.core.BaseFragment
import com.skedgo.tripkit.ui.core.addTo
import com.skedgo.tripkit.ui.core.rxproperty.asObservable
import com.skedgo.tripkit.ui.databinding.FragmentDrtBinding
import com.skedgo.tripkit.ui.dialog.GenericListDialogFragment
import com.skedgo.tripkit.ui.dialog.GenericListDisplayDialogFragment
import com.skedgo.tripkit.ui.dialog.GenericListItem
import com.skedgo.tripkit.ui.dialog.GenericNoteDialogFragment
import com.skedgo.tripkit.ui.generic.action_list.ActionListAdapter
import com.skedgo.tripkit.ui.trippreview.TripPreviewPagerItemViewModel
import com.skedgo.tripkit.ui.utils.observe
import com.skedgo.tripkit.ui.utils.showConfirmationPopUpDialog
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

    private var tripSegmentUpdateCallback: ((TripSegment) -> Unit)? = null

    @Inject
    lateinit var actionsAdapter: ActionListAdapter

    override val layoutRes: Int
        get() = R.layout.fragment_drt

    override fun onBook() {
        viewModel.book()
    }

    override fun onCancelRide() {

    }

    override fun onAttach(context: Context) {
        TripKitUI.getInstance().tripPreviewComponent().inject(this)
        super.onAttach(context)
    }

    override fun onCreated(savedInstance: Bundle?) {
        initBinding()
        initObserver()
        initViews()
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

                    if (drtItem.type.value.equals("notes", true)) {
                        GenericListDisplayDialogFragment.newInstance(
                                GenericListItem.parseOptions(
                                        drtItem.options.value ?: emptyList()
                                ),
                                title = drtItem.type.value ?: ""
                        ).show(childFragmentManager, drtItem.label.value ?: "")
                    } else {
                        val defaultValue = viewModel.getDefaultValueByType(
                                drtItem.type.value ?: "",
                                drtItem.label.value ?: ""
                        )

                        if (drtItem.label.value == DrtItem.ADD_NOTE) {
                            GenericNoteDialogFragment.newInstance(
                                    drtItem.label.value ?: "",
                                    if (drtItem.values.value?.firstOrNull() != defaultValue) {
                                        drtItem.values.value?.firstOrNull() ?: ""
                                    } else {
                                        ""
                                    },
                                    viewModel.bookingConfirmation.value != null
                            ) {
                                if (it.isEmpty()) {
                                    listOf(defaultValue)
                                } else {
                                    drtItem.setValue(listOf(it))
                                    viewModel.updateInputValue(drtItem)
                                }
                            }.show(childFragmentManager, drtItem.label.value ?: "")
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
                                        viewModel.updateInputValue(drtItem)
                                    },
                                    previousSelectedValues = if (drtItem.values.value != listOf(defaultValue)) {
                                        drtItem.values.value
                                    } else {
                                        null
                                    },
                                    viewOnlyMode = viewModel.bookingConfirmation.value != null
                            ).show(childFragmentManager, drtItem.label.value ?: "")
                        }
                    }

                    /*
                    if (viewModel.bookingConfirmation.value == null) {

                        val defaultValue = viewModel.getDefaultValueByType(
                                drtItem.type.value ?: "",
                                drtItem.label.value ?: ""
                        )

                        if (drtItem.label.value == DrtItem.ADD_NOTE) {
                            GenericNoteDialogFragment.newInstance(
                                    drtItem.label.value ?: "",
                                    if (drtItem.values.value?.firstOrNull() != defaultValue) {
                                        drtItem.values.value?.firstOrNull() ?: ""
                                    } else {
                                        ""
                                    }
                            ) {
                                if (it.isEmpty()) {
                                    listOf(defaultValue)
                                } else {
                                    drtItem.setValue(listOf(it))
                                    viewModel.updateInputValue(drtItem)
                                }
                            }.show(childFragmentManager, drtItem.label.value ?: "")
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
                                        viewModel.updateInputValue(drtItem)
                                    },
                                    previousSelectedValues = if (drtItem.values.value != listOf(defaultValue)) {
                                        drtItem.values.value
                                    } else {
                                        null
                                    }
                            ).show(childFragmentManager, drtItem.label.value ?: "")
                        }
                    } else {
                        if (drtItem.type.value.equals("notes", true)) {
                            GenericListDisplayDialogFragment.newInstance(
                                    GenericListItem.parseOptions(
                                            drtItem.options.value ?: emptyList()
                                    ),
                                    title = drtItem.type.value ?: ""
                            ).show(childFragmentManager, drtItem.label.value ?: "")
                        }
                    }
                    */
                }.launchIn(lifecycleScope)
        segment?.let {
            pagerItemViewModel.setSegment(requireContext(), it)
            viewModel.setTripSegment(it)
        }

        observe(viewModel.confirmationActions) {
            it?.let { actionsAdapter.collection = it }
        }

        observe(viewModel.segment) {
            it?.let { tripSegmentUpdateCallback?.invoke(it) }
        }
    }

    override fun onResume() {
        super.onResume()
        pagerItemViewModel.closeClicked.observable
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { onCloseButtonListener?.onClick(null) }.addTo(autoDisposable)

        viewModel.error.asObservable().observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    if (!it.isNullOrEmpty()) {
                        MaterialAlertDialogBuilder(requireContext())
                                .setMessage(it)
                                .setPositiveButton(resources.getString(R.string.ok)) { dialog, _ ->
                                    dialog.cancel()
                                }
                                .show()
                    }
                }.addTo(autoDisposable)
    }

    private fun initViews() {
        binding.drtRvActions.adapter = actionsAdapter
        actionsAdapter.clickListener = {
            when {
                it.bookingConfirmationAction?.externalURL() != null -> {
                    if (it.bookingConfirmationAction.type() == BookingConfirmationAction.TYPE_CALL) {
                        val intent = Intent(
                                Intent.ACTION_DIAL,
                                Uri.parse(it.bookingConfirmationAction.externalURL())
                        )
                        requireActivity().startActivity(intent)
                    }
                }
                it.label == "Cancel Ride" || it.bookingConfirmationAction?.type() == BookingConfirmationAction.TYPE_CANCEL -> {
                    requireContext().showConfirmationPopUpDialog(
                            title = "Cancel Ride",
                            message = "Are you sure you want to cancel this ride?",
                            positiveLabel = "Yes",
                            positiveCallback = {
                                viewModel.processAction(it.bookingConfirmationAction)
                            },
                            negativeLabel = "No"
                    )
                }
                else -> {
                    viewModel.processAction(it.bookingConfirmationAction)
                }
            }
        }
    }

    companion object {
        fun newInstance(
                segment: TripSegment,
                tripSegmentUpdateCallback: ((TripSegment) -> Unit)? = null
        ): DrtFragment {
            val fragment = DrtFragment()
            fragment.segment = segment
            fragment.tripSegmentUpdateCallback = tripSegmentUpdateCallback
            return fragment
        }
    }
}