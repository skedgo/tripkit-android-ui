package com.skedgo.tripkit.ui.trippreview.directions

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProviders
import com.skedgo.tripkit.routing.TripSegment
import com.skedgo.tripkit.ui.core.BaseTripKitFragment
import com.skedgo.tripkit.ui.core.addTo
import com.skedgo.tripkit.ui.databinding.TripPreviewPagerDirectionsItemBinding
import io.reactivex.android.schedulers.AndroidSchedulers


class DirectionsTripPreviewItemFragment(val segment: TripSegment) : BaseTripKitFragment() {
    private lateinit var viewModel: DirectionsTripPreviewItemViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProviders.of(this).get("directionsTripPreview", DirectionsTripPreviewItemViewModel::class.java)
        viewModel.setSegment(context!!, segment)
    }

    override fun onResume() {
        super.onResume()
        viewModel.closeClicked.observable.observeOn(AndroidSchedulers.mainThread()).subscribe{ onCloseButtonListener?.onClick(null) }.addTo(autoDisposable)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val binding = TripPreviewPagerDirectionsItemBinding.inflate(inflater)
        binding.lifecycleOwner = this
        binding.viewModel = viewModel
        return binding.root
    }
}