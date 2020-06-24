package com.skedgo.tripkit.ui.trippreview.nearby

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProviders
import com.skedgo.tripkit.routing.TripSegment
import com.skedgo.tripkit.ui.TripKitUI
import com.skedgo.tripkit.ui.core.BaseTripKitFragment
import com.skedgo.tripkit.ui.core.addTo
import com.skedgo.tripkit.ui.databinding.TripPreviewPagerModeLocationItemBinding
import io.reactivex.android.schedulers.AndroidSchedulers
import timber.log.Timber
import javax.inject.Inject


class ModeLocationTripPreviewItemFragment(val segment: TripSegment) : BaseTripKitFragment() {
    @Inject
    lateinit var sharedViewModelFactory: SharedNearbyTripPreviewItemViewModelFactory

    lateinit var sharedViewModel: SharedNearbyTripPreviewItemViewModel
    lateinit var viewModel: ModeLocationTripPreviewViewModel

    override fun onAttach(context: Context) {
        TripKitUI.getInstance().tripPreviewComponent().inject(this)
        super.onAttach(context)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sharedViewModel = ViewModelProviders.of(activity!!, sharedViewModelFactory).get("sharedNearbyViewModel", SharedNearbyTripPreviewItemViewModel::class.java)
        viewModel = ViewModelProviders.of(this).get("modeLocationViewModel", ModeLocationTripPreviewViewModel::class.java)
        sharedViewModel.setSegment(context!!, segment)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val binding = TripPreviewPagerModeLocationItemBinding.inflate(inflater)
        binding.lifecycleOwner = this
        binding.viewModel = viewModel
        binding.sharedViewModel = sharedViewModel
        return binding.root
    }

    override fun onResume() {
        super.onResume()
        sharedViewModel.closeClicked.observable.observeOn(AndroidSchedulers.mainThread()).subscribe{ onCloseButtonListener?.onClick(null) }.addTo(autoDisposable)
        sharedViewModel.locationDetails.observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    viewModel.set(it)
                }.addTo(autoDisposable)

    }

}