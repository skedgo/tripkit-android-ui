package com.skedgo.tripkit.ui.trippreview

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.lifecycle.ViewModelProviders
import androidx.viewpager.widget.ViewPager
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
import com.skedgo.tripkit.ui.tripresult.ARG_TRIP_GROUP_ID
import com.skedgo.tripkit.ui.utils.*
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import javax.inject.Inject


class TripPreviewPagerFragment : BaseTripKitFragment() {
    @Inject
    lateinit var tripGroupRepository: TripGroupRepository
    lateinit var adapter: TripPreviewPagerAdapter
    lateinit var binding: TripPreviewPagerBinding

    var currentPagerIndex = 0

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

        savedInstanceState?.getInt(ARG_CURRENT_PAGER_INDEX)?.let {
            currentPagerIndex = it
            binding.tripSegmentPager.currentItem = currentPagerIndex
            savedInstanceState.remove(ARG_CURRENT_PAGER_INDEX)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(ARG_CURRENT_PAGER_INDEX, currentPagerIndex)
    }

    fun load(tripGroupId: String, tripId: String, tripSegmentId: Long) {
        tripGroupRepository.getTripGroup(tripGroupId)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { tripGroup ->
                    val trip = tripGroup.trips?.find { it.uuid() == tripId }
                    trip?.let {
                        var activeIndex = adapter.setTripSegments(tripSegmentId,
                                trip.segments.filter { !it.isContinuation }.filter { it.type != SegmentType.DEPARTURE && it.type != SegmentType.ARRIVAL })
                        adapter.notifyDataSetChanged()
                        if(currentPagerIndex != 0 && activeIndex != currentPagerIndex){
                            activeIndex = currentPagerIndex
                        }
                        currentPagerIndex = activeIndex
                        binding.tripSegmentPager.currentItem = activeIndex
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

    private fun updateAdapter() {
        adapter = TripPreviewPagerAdapter(childFragmentManager)
        adapter.onCloseButtonListener = this.onCloseButtonListener
        adapter.tripPreviewPagerListener = this.tripPreviewPagerListener
        binding.tripSegmentPager.adapter = adapter
        binding.tripSegmentPager.offscreenPageLimit = 1
        setViewPagerListeners()
    }

    private fun setViewPagerListeners(){
        binding.tripSegmentPager.addOnPageChangeListener(object: ViewPager.OnPageChangeListener{
            override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {}

            override fun onPageSelected(position: Int) {
                (adapter.instantiateItem(
                        binding.tripSegmentPager,
                        position) as? BaseTripKitFragment)?.let {
                    it.onCloseButtonListener = this@TripPreviewPagerFragment.onCloseButtonListener
                    it.tripPreviewPagerListener = this@TripPreviewPagerFragment.tripPreviewPagerListener
                }
                currentPagerIndex = position
            }

            override fun onPageScrollStateChanged(state: Int) {}
        })
    }

    fun setTripSegment(segment: TripSegment, tripSegments: List<TripSegment>) {
        adapter.setTripSegments(segment.id, tripSegments.filter { !it.isContinuation }.filter { it.type != SegmentType.DEPARTURE && it.type != SegmentType.ARRIVAL })
        tripGroupRepository.updateTrip(segment.trip.group.uuid(), segment.trip.uuid(), segment.trip)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe()
                .addTo(autoDisposable)
    }

    fun updateTripSegment(tripSegments: List<TripSegment>) {
        tripSegments.forEachIndexed { _, segment ->
            when (segment.correctItemType()) {
                ITEM_SERVICE -> {
                    /*
                    val scheduledStop = ScheduledStop(segment.to)
                    scheduledStop.code = segment.startStopCode
                    scheduledStop.modeInfo = segment.modeInfo
                    scheduledStop.type = StopType.from(segment.modeInfo?.localIconName)
                    */
                    adapter.timetableFragment?.setBookingActions(segment.booking?.externalActions)
                }
            }
        }
    }

    fun updateListener(tripPreviewPagerListener: Listener) {
        this.tripPreviewPagerListener = tripPreviewPagerListener
    }

    interface Listener {
        fun onServiceActionButtonClicked(action: String?)
        fun onExternalActionButtonClicked(action: String?)
    }

    /*
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
    */

    companion object{

        const val ARG_CURRENT_PAGER_INDEX = "_a_current_pager_index"

        fun newInstance(
                tripGroupId: String,
                tripId: String,
                tripSegmentHashCode: Long,
                tripPreviewPagerListener: Listener): TripPreviewPagerFragment {
            val fragment = TripPreviewPagerFragment()
            fragment.arguments = bundleOf(
                    ARG_TRIP_GROUP_ID to tripGroupId,
                    ARG_TRIP_ID to tripId,
                    ARG_TRIP_SEGMENT_ID to tripSegmentHashCode
            )
            fragment.tripPreviewPagerListener = tripPreviewPagerListener
            return fragment
        }
    }
}