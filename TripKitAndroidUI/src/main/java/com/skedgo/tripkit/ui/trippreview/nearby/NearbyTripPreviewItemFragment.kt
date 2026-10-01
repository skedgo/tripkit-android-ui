package com.skedgo.tripkit.ui.trippreview.nearby

import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import com.google.android.flexbox.FlexDirection
import com.google.android.flexbox.FlexboxLayoutManager
import com.skedgo.rxtry.subscribeWithErrorHandling
import com.skedgo.tripkit.routing.TripSegment
import com.skedgo.tripkit.ui.R
import com.skedgo.tripkit.ui.TripKitUI
import com.skedgo.tripkit.ui.core.BaseFragment
import com.skedgo.tripkit.ui.core.addTo
import com.skedgo.tripkit.ui.databinding.TripPreviewPagerNearbyItemBinding
import io.reactivex.android.schedulers.AndroidSchedulers
import javax.inject.Inject


class NearbyTripPreviewItemFragment : BaseFragment<TripPreviewPagerNearbyItemBinding>() {
    @Inject
    lateinit var sharedViewModelFactory: SharedNearbyTripPreviewItemViewModelFactory
    private val sharedViewModel: SharedNearbyTripPreviewItemViewModel by viewModels(
        ownerProducer = { requireParentFragment() },
        factoryProducer = { sharedViewModelFactory }
    )
    private val viewModel: NearbyTripPreviewItemViewModel by viewModels()

    var segment: TripSegment? = null

    override val layoutRes: Int
        get() = R.layout.trip_preview_pager_nearby_item

    override val observeAccessibility: Boolean = false

    override fun getDefaultViewForAccessibility(): View? = null

    override fun onAttach(context: Context) {
        TripKitUI.getInstance().tripPreviewComponent().inject(this)
        super.onAttach(context)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sharedViewModel.closeClicked.observable.observeOn(AndroidSchedulers.mainThread())
            .subscribeWithErrorHandling { onCloseButtonListener?.onClick(null) }.addTo(autoDisposable)
        //sharedViewModel.setSegment(context!!, segment)
        segment?.let {
            sharedViewModel.setSegment(requireContext(), it)
        }
    }

    override fun onCreated(savedInstance: Bundle?) {
        val layoutManager = FlexboxLayoutManager(context)
        layoutManager.flexDirection = FlexDirection.ROW
        binding.transportItemsView.layoutManager = layoutManager
        binding.lifecycleOwner = viewLifecycleOwner
        binding.sharedViewModel = sharedViewModel
        binding.viewModel = viewModel
    }

    override fun onResume() {
        super.onResume()
        viewModel.clearTransportModes()

        sharedViewModel.locationList
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeWithErrorHandling {
                viewModel.setLocations(it)
            }.addTo(autoDisposable)
        sharedViewModel.locationList
            .observeOn(AndroidSchedulers.mainThread())
            .flatMapIterable { item -> item }
            .filter { it.modeInfo != null }
            .map { location -> location.modeInfo!! }
            .distinct { it.id }
            .subscribeWithErrorHandling { mode -> viewModel.addMode(mode) }
            .addTo(autoDisposable)

    }

    companion object {
        fun newInstance(segment: TripSegment): NearbyTripPreviewItemFragment {
            val fragment = NearbyTripPreviewItemFragment()

            return fragment
        }
    }
}