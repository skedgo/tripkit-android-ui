package com.skedgo.tripkit.ui.trippreview.external

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProviders
import com.skedgo.TripKit
import com.skedgo.tripkit.ExternalActionParams
import com.skedgo.tripkit.routing.TripSegment
import com.skedgo.tripkit.ui.TripKitUI
import com.skedgo.tripkit.ui.core.BaseTripKitFragment
import com.skedgo.tripkit.ui.core.addTo
import com.skedgo.tripkit.ui.databinding.TripPreviewExternalActionPagerItemBinding
import io.reactivex.android.schedulers.AndroidSchedulers

class ExternalActionTripPreviewItemFragment : BaseTripKitFragment() {
    private lateinit var viewModel: ExternalActionTripPreviewItemViewModel
    private val bookingResolver = TripKit.getInstance().bookingResolver

    private var tripSegment: TripSegment? = null

    override fun onAttach(context: Context) {
        TripKitUI.getInstance().tripPreviewComponent().inject(this)
        super.onAttach(context)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProviders.of(this).get(ExternalActionTripPreviewItemViewModel::class.java)
    }

    override fun onResume() {
        super.onResume()
        viewModel.closeClicked.observable.observeOn(AndroidSchedulers.mainThread()).subscribe { onCloseButtonListener?.onClick(null) }.addTo(autoDisposable)
        viewModel.actionChosen.observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    viewModel.enableButton.set(false)
                    tripPreviewPagerListener?.onServiceActionButtonClicked(it)
                }.addTo(autoDisposable)
        viewModel.enableButton.set(true)
    }


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val binding = TripPreviewExternalActionPagerItemBinding.inflate(inflater)
        binding.lifecycleOwner = this
        binding.viewModel = viewModel
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        tripSegment?.let {
            viewModel.setSegment(requireContext(), it)
        }
    }

    private fun doBooking(action: String) {
        val params: ExternalActionParams = ExternalActionParams.builder()
                .action(action)
                .segment(tripSegment)
                .build()
        bookingResolver.performExternalActionAsync(params)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    startActivity(it.data())
                }.addTo(autoDisposable)
    }

    companion object {
        fun newInstance(tripSegment: TripSegment): ExternalActionTripPreviewItemFragment {
            val fragment = ExternalActionTripPreviewItemFragment()
            fragment.tripSegment = tripSegment
            return fragment
        }
    }
}