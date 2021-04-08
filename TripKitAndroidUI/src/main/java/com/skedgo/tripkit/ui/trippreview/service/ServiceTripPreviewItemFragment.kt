package com.skedgo.tripkit.ui.trippreview.service

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.Gson
import com.skedgo.tripkit.data.regions.RegionService
import com.skedgo.tripkit.routing.Trip
import com.skedgo.tripkit.routing.TripGroup
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
    private val gson = Gson()

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

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        segment?.let {
            outState.putString(ARGS_TRIP_SEGMENT, gson.toJson(it))
            outState.putString(ARGS_TRIP_SEGMENT_TRIP, gson.toJson(it.trip))
            outState.putString(ARGS_TRIP_SEGMENT_TRIP_GROUP, gson.toJson(it.trip?.group))
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val binding = TripPreviewServiceItemBinding.inflate(inflater)
        binding.viewModel = viewModel
        binding.lifecycleOwner = this
        binding.content.occupancyList.layoutManager = LinearLayoutManager(context, RecyclerView.HORIZONTAL, false)
        binding.closeButton.setOnClickListener(onCloseButtonListener)

        segment?.let {
            viewModel.setup(it)
        } ?: kotlin.run {
            checkSegmentOnSavedInstance(savedInstanceState)
        }

        return binding.root
    }

    private fun checkSegmentOnSavedInstance(savedInstanceState: Bundle?) {
        savedInstanceState?.let {
            val segment: TripSegment = gson.fromJson(
                    it.getString(ARGS_TRIP_SEGMENT, ""), TripSegment::class.java
            )
            val trip = gson.fromJson(
                    it.getString(ARGS_TRIP_SEGMENT_TRIP),
                    Trip::class.java
            )

            trip.group = gson.fromJson(
                    it.getString(ARGS_TRIP_SEGMENT_TRIP_GROUP),
                    TripGroup::class.java
            )
            segment.trip = trip
            this@ServiceTripPreviewItemFragment.segment = segment
            viewModel.setup(segment)

        }
    }

    companion object {

        const val ARGS_TRIP_SEGMENT = "tripSegment"
        const val ARGS_TRIP_SEGMENT_TRIP = "tripSegmentTri"
        const val ARGS_TRIP_SEGMENT_TRIP_GROUP = "tripSegmentTripGroup"

        fun newInstance(segment: TripSegment): ServiceTripPreviewItemFragment {
            val fragment = ServiceTripPreviewItemFragment()
            fragment.segment = segment
            return fragment
        }
    }
}