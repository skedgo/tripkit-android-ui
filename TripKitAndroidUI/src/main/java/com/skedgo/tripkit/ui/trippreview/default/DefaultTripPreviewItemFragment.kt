package com.skedgo.tripkit.ui.trippreview.default

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.skedgo.tripkit.routing.TripSegment
import com.skedgo.tripkit.ui.core.BaseTripKitFragment
import com.skedgo.tripkit.ui.core.addTo
import com.skedgo.tripkit.ui.databinding.TripPreviewPagerItemBinding
import com.skedgo.tripkit.ui.trippreview.TripPreviewPagerItemViewModel
import com.skedgo.tripkit.ui.utils.TapAction
import io.reactivex.android.schedulers.AndroidSchedulers


class DefaultTripPreviewItemFragment(val segment: TripSegment) : BaseTripKitFragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val binding = TripPreviewPagerItemBinding.inflate(inflater)
        val vm = TripPreviewPagerItemViewModel()
        vm.closeClicked.observable.observeOn(AndroidSchedulers.mainThread()).subscribe{ onCloseButtonListener?.onClick(null) }.addTo(autoDisposable)
        vm.setSegment(context!!, segment)
        binding.viewModel = vm
        binding.lifecycleOwner = this
        return binding.root
    }
}