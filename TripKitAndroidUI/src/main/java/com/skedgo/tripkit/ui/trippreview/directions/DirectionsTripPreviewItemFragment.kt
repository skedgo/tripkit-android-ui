package com.skedgo.tripkit.ui.trippreview.directions

import android.content.Intent
import android.content.pm.ApplicationInfo
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModelProviders
import androidx.lifecycle.lifecycleScope
import com.google.gson.Gson
import com.skedgo.tripkit.routing.TripSegment
import com.skedgo.tripkit.ui.core.BaseTripKitFragment
import com.skedgo.tripkit.ui.core.addTo
import com.skedgo.tripkit.ui.databinding.TripPreviewPagerDirectionsItemBinding
import io.reactivex.android.schedulers.AndroidSchedulers
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach


class DirectionsTripPreviewItemFragment : BaseTripKitFragment() {

    private val viewModel: DirectionsTripPreviewItemViewModel by viewModels()

    private var segment: TripSegment? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        //viewModel = ViewModelProviders.of(this).get("directionsTripPreview", DirectionsTripPreviewItemViewModel::class.java)

        segment?.let {
            viewModel.setSegment(requireContext(), it)
        }?: kotlin.run {
            savedInstanceState?.let {
                if(it.containsKey(ARGS_SEGMENT)){
                    segment = Gson().fromJson(it.getString(ARGS_SEGMENT), TripSegment::class.java)
                    segment?.let { viewModel.setSegment(requireContext(), it) }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.closeClicked.observable.observeOn(AndroidSchedulers.mainThread()).subscribe { onCloseButtonListener?.onClick(null) }.addTo(autoDisposable)
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

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        //outState.putString(ARGS_SEGMENT, Gson().toJson(segment))
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val binding = TripPreviewPagerDirectionsItemBinding.inflate(inflater)
        binding.lifecycleOwner = this
        binding.viewModel = viewModel
        return binding.root
    }

    companion object {

        const val ARGS_SEGMENT = "args_segment"

        fun newInstance(segment: TripSegment): DirectionsTripPreviewItemFragment {
            val fragment = DirectionsTripPreviewItemFragment()
            fragment.segment = segment
            return fragment
        }
    }
}