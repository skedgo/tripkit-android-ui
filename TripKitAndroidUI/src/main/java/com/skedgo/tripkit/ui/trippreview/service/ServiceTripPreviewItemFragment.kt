package com.skedgo.tripkit.ui.trippreview.service

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.skedgo.tripkit.common.model.Location
import com.skedgo.tripkit.common.model.Region
import com.skedgo.tripkit.common.model.ScheduledStop
import com.skedgo.tripkit.data.regions.RegionService
import com.skedgo.tripkit.routing.TripSegment
import com.skedgo.tripkit.ui.TripKitUI
import com.skedgo.tripkit.ui.core.BaseTripKitFragment
import com.skedgo.tripkit.ui.core.addTo
import com.skedgo.tripkit.ui.databinding.TripPreviewServiceItemBinding
import com.skedgo.tripkit.ui.servicedetail.ServiceDetailViewModel
import com.skedgo.tripkit.ui.timetables.FetchAndLoadTimetable
import com.skedgo.tripkit.ui.utils.ignoreNetworkErrors
import io.reactivex.android.schedulers.AndroidSchedulers
import kotlinx.android.synthetic.main.trip_preview_service_item.view.*
import timber.log.Timber
import javax.inject.Inject
import kotlin.properties.Delegates


class ServiceTripPreviewItemFragment(val segment: TripSegment) : BaseTripKitFragment() {
    var time = 0L

    @Inject lateinit var fetchAndLoadTimetable: FetchAndLoadTimetable
    @Inject lateinit var regionService: RegionService
    @Inject lateinit var viewModel: ServiceDetailViewModel

    override fun onAttach(context: Context) {
        TripKitUI.getInstance().tripPreviewComponent().inject(this)
        super.onAttach(context)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val binding = TripPreviewServiceItemBinding.inflate(inflater)
        binding.viewModel = viewModel
        binding.lifecycleOwner = this
        binding.content.occupancyList.layoutManager = LinearLayoutManager(context, RecyclerView.HORIZONTAL, false)
        binding.closeButton.setOnClickListener(onCloseButtonListener)

        return binding.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel.setup(segment)
        viewModel.showCloseButton.set(true)
    }
}