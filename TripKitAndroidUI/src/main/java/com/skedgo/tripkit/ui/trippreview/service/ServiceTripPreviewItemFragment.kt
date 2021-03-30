package com.skedgo.tripkit.ui.trippreview.service

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.skedgo.tripkit.data.regions.RegionService
import com.skedgo.tripkit.routing.TripSegment
import com.skedgo.tripkit.ui.TripKitUI
import com.skedgo.tripkit.ui.core.BaseTripKitFragment
import com.skedgo.tripkit.ui.core.addTo
import com.skedgo.tripkit.ui.databinding.TripPreviewServiceItemBinding
import com.skedgo.tripkit.ui.servicedetail.ServiceDetailViewModel
import com.skedgo.tripkit.ui.timetables.FetchAndLoadTimetable
import io.reactivex.android.schedulers.AndroidSchedulers
import javax.inject.Inject


class ServiceTripPreviewItemFragment : BaseTripKitFragment() {
    var time = 0L

    var segment: TripSegment? = null

    @Inject
    lateinit var fetchAndLoadTimetable: FetchAndLoadTimetable

    @Inject
    lateinit var regionService: RegionService

    @Inject
    lateinit var viewModel: ServiceDetailViewModel

    override fun onAttach(context: Context) {
        TripKitUI.getInstance().tripPreviewComponent().inject(this)
        super.onAttach(context)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)


    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val binding = TripPreviewServiceItemBinding.inflate(inflater)
        binding.viewModel = viewModel
        binding.lifecycleOwner = this
        binding.content.occupancyList.layoutManager = LinearLayoutManager(context, RecyclerView.HORIZONTAL, false)
        binding.closeButton.setOnClickListener(onCloseButtonListener)

        segment?.let {
            viewModel.setup(it)
        }

        return binding.root
    }

    companion object{
        fun newInstance(segment: TripSegment): ServiceTripPreviewItemFragment{
            val fragment = ServiceTripPreviewItemFragment()
            fragment.segment = segment
            return fragment
        }
    }
}