package com.skedgo.tripkit.ui.trippreview.external

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import com.skedgo.TripKit
import com.skedgo.tripkit.ExternalActionParams
import com.skedgo.tripkit.routing.TripSegment
import com.skedgo.tripkit.ui.TripKitUI
import com.skedgo.tripkit.ui.core.BaseTripKitFragment
import com.skedgo.tripkit.ui.core.addTo
import com.skedgo.tripkit.ui.databinding.TripPreviewExternalActionPagerItemBinding
import com.skedgo.tripkit.ui.trippreview.Action
import io.reactivex.android.schedulers.AndroidSchedulers

class ExternalActionTripPreviewItemFragment : BaseTripKitFragment() {
    private val viewModel: ExternalActionTripPreviewItemViewModel by viewModels()
    private val bookingResolver = TripKit.getInstance().bookingResolver

    private var tripSegment: TripSegment? = null

    private var externalActionCallback: ((TripSegment?, Action?) -> Unit)? = null

    override fun onAttach(context: Context) {
        TripKitUI.getInstance().tripPreviewComponent().inject(this)
        super.onAttach(context)
    }

    /*
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProviders.of(this).get(ExternalActionTripPreviewItemViewModel::class.java)
    }
    */

    override fun onResume() {
        super.onResume()

        tripSegment?.let {
            viewModel.setSegment(requireContext(), it)
        }

        viewModel.closeClicked.observable
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    onCloseButtonListener?.onClick(null)
                }.addTo(autoDisposable)
        viewModel.externalActionChosen.observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    viewModel.enableActionButtons.set(false)
                    //tripPreviewPagerListener?.onExternalActionButtonClicked(it)
                    externalActionCallback?.invoke(tripSegment, it)
                }.addTo(autoDisposable)
        viewModel.enableActionButtons.set(true)
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
        fun newInstance(
                tripSegment: TripSegment,
                externalActionCallback: ((TripSegment?, Action?) -> Unit)? = null
        ): ExternalActionTripPreviewItemFragment {
            val fragment = ExternalActionTripPreviewItemFragment()
            fragment.externalActionCallback = externalActionCallback
            fragment.tripSegment = tripSegment
            return fragment
        }
    }
}