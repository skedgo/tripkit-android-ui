package com.skedgo.tripkit.ui.trippreview

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProviders
import com.skedgo.tripkit.common.model.ScheduledStop
import com.skedgo.tripkit.common.model.StopType
import com.skedgo.tripkit.routing.SegmentType
import com.skedgo.tripkit.routing.Trip
import com.skedgo.tripkit.routing.TripSegment
import com.skedgo.tripkit.ui.ARG_TRIP_ID
import com.skedgo.tripkit.ui.ARG_TRIP_SEGMENT_ID
import com.skedgo.tripkit.ui.TripKitUI
import com.skedgo.tripkit.ui.core.BaseTripKitFragment
import com.skedgo.tripkit.ui.core.addTo
import com.skedgo.tripkit.ui.databinding.TripPreviewPagerBinding
import com.skedgo.tripkit.ui.routingresults.TripGroupRepository
import com.skedgo.tripkit.ui.timetables.TimetableFragment
import com.skedgo.tripkit.ui.trippreview.directions.DirectionsTripPreviewItemFragment
import com.skedgo.tripkit.ui.trippreview.external.ExternalActionTripPreviewItemFragment
import com.skedgo.tripkit.ui.trippreview.nearby.ModeLocationTripPreviewItemFragment
import com.skedgo.tripkit.ui.trippreview.nearby.NearbyTripPreviewItemFragment
import com.skedgo.tripkit.ui.trippreview.service.ServiceTripPreviewItemFragment
import com.skedgo.tripkit.ui.trippreview.standard.StandardTripPreviewItemFragment
import com.skedgo.tripkit.ui.tripresult.ARG_TRIP_GROUP_ID
import com.skedgo.tripkit.ui.utils.*
import io.reactivex.android.schedulers.AndroidSchedulers
import javax.inject.Inject


class TripPreviewPagerFragment : BaseTripKitFragment() {
    @Inject
    lateinit var tripGroupRepository: TripGroupRepository
    lateinit var adapter: TripPreviewPagerAdapter
    lateinit var binding: TripPreviewPagerBinding

    override fun onAttach(context: Context) {
        TripKitUI.getInstance().tripDetailsComponent().inject(this)
        super.onAttach(context)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        arguments?.let {
            load(it.getString(ARG_TRIP_GROUP_ID, ""),
                    it.getString(ARG_TRIP_ID, ""),
                    it.getLong(ARG_TRIP_SEGMENT_ID, 0L))
        }
    }

    fun load(tripGroupId: String, tripId: String, tripSegmentId: Long) {
        tripGroupRepository.getTripGroup(tripGroupId)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { tripGroup ->
                    val trip = tripGroup.trips?.find { it.uuid() == tripId }
                    trip?.let {
                        adapter.setTripSegments(tripSegmentId,
                                trip.segments.filter { !it.isContinuation }.filter { it.type != SegmentType.DEPARTURE && it.type != SegmentType.ARRIVAL })
                        adapter.notifyDataSetChanged()

                    }
                }
                .addTo(autoDisposable)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = TripPreviewPagerBinding.inflate(inflater)
        binding.lifecycleOwner = this
        updateAdapter()

        return binding.root
    }

    fun updateAdapter() {
        adapter = TripPreviewPagerAdapter(childFragmentManager)
        adapter.onCloseButtonListener = this.onCloseButtonListener
        adapter.tripPreviewPagerListener = this.tripPreviewPagerListener
        binding.tripSegmentPager.adapter = adapter
    }

    fun setTripSegment(segment: TripSegment, tripSegments: List<TripSegment>) {
        adapter.setTripSegments(segment.id, tripSegments.filter { !it.isContinuation }.filter { it.type != SegmentType.DEPARTURE && it.type != SegmentType.ARRIVAL })
    }

    fun updateTripSegment(tripSegments: List<TripSegment>) {
        tripSegments.forEachIndexed { _, segment ->
            when (segment.correctItemType()) {
                ITEM_SERVICE -> {
                    val scheduledStop = ScheduledStop(segment.to)
                    scheduledStop.code = segment.startStopCode
                    scheduledStop.modeInfo = segment.modeInfo
                    scheduledStop.type = StopType.from(segment.modeInfo?.localIconName)
                    adapter.timetableFragment!!.setBookingActions(segment.booking?.externalActions)
                }
            }
        }
    }

    fun updateListener(tripPreviewPagerListener: Listener) {
        this.tripPreviewPagerListener = tripPreviewPagerListener
    }

    interface Listener {
        fun onServiceActionButtonClicked(action: String?)
    }

    class Builder(val tripGroupId: String, val tripId: String, val tripSegmentHashCode: Long, val _tripPreviewPagerListener: Listener) {
        fun build(): TripPreviewPagerFragment {
            return TripPreviewPagerFragment().apply {
                val b = Bundle()
                b.putString(ARG_TRIP_GROUP_ID, tripGroupId)
                b.putString(ARG_TRIP_ID, tripId)
                b.putLong(ARG_TRIP_SEGMENT_ID, tripSegmentHashCode)
                this.tripPreviewPagerListener = _tripPreviewPagerListener
                arguments = b
            }
        }
    }
}