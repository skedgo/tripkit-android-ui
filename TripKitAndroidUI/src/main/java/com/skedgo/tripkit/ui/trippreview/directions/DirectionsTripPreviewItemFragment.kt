package com.skedgo.tripkit.ui.trippreview.directions

import android.content.Intent
import android.content.pm.ApplicationInfo
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProviders
import androidx.lifecycle.lifecycleScope
import com.skedgo.tripkit.routing.TripSegment
import com.skedgo.tripkit.ui.core.BaseTripKitFragment
import com.skedgo.tripkit.ui.core.addTo
import com.skedgo.tripkit.ui.databinding.TripPreviewPagerDirectionsItemBinding
import io.reactivex.android.schedulers.AndroidSchedulers
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach


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
        viewModel.showLaunchInMapsClicked.observable.onEach {
            it.segment?.let {
                val mode = if (it.isCycling) {
                    "b"
                } else if (it.isWalking || it.isWheelchair) {
                    "w"
                } else {
                    "d"
                }
                val uri = Uri.parse("google.navigation:mode=$mode&q=${it.to.lat},${it.to.lon}")
                val mapIntent = Intent(Intent.ACTION_VIEW, uri)
                mapIntent.setPackage("com.google.android.apps.maps")
                if (mapIntent.resolveActivity(requireActivity().packageManager) != null) {
                    startActivity(mapIntent)
                }
            }
        }.launchIn(lifecycleScope)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val binding = TripPreviewPagerDirectionsItemBinding.inflate(inflater)
        binding.lifecycleOwner = this
        binding.viewModel = viewModel
        return binding.root
    }
}