package com.skedgo.tripkit.ui.trippreview

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.viewModels
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.lifecycleScope
import androidx.viewpager.widget.ViewPager
import com.haroldadmin.cnradapter.NetworkResponse
import com.skedgo.TripKit
import com.skedgo.tripkit.ExternalActionParams
import com.skedgo.tripkit.bookingproviders.BookingResolver
import com.skedgo.tripkit.common.model.TransportMode
import com.skedgo.tripkit.routing.*
import com.skedgo.tripkit.ui.ARG_FROM_TRIP_ACTION
import com.skedgo.tripkit.ui.ARG_TRIP_ID
import com.skedgo.tripkit.ui.ARG_TRIP_SEGMENT_ID
import com.skedgo.tripkit.ui.TripKitUI
import com.skedgo.tripkit.ui.booking.apiv2.BookingV2TrackingService
import com.skedgo.tripkit.ui.core.BaseTripKitFragment
import com.skedgo.tripkit.ui.core.addTo
import com.skedgo.tripkit.ui.core.logError
import com.skedgo.tripkit.ui.databinding.TripPreviewPagerBinding
import com.skedgo.tripkit.ui.model.TimetableEntry
import com.skedgo.tripkit.ui.routingresults.TripGroupRepository
import com.skedgo.tripkit.ui.timetables.TimetableFragment
import com.skedgo.tripkit.ui.tripresult.ARG_TRIP_GROUP_ID
import com.skedgo.tripkit.ui.tripresults.GetTransportIconTintStrategy
import com.skedgo.tripkit.ui.utils.*
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject


class TripPreviewPagerFragment : BaseTripKitFragment() {
    @Inject
    lateinit var tripGroupRepository: TripGroupRepository

    @Inject
    lateinit var bookingService: BookingV2TrackingService

    @Inject
    lateinit var getTransportIconTintStrategy: GetTransportIconTintStrategy

    private val viewModel: TripPreviewPagerViewModel by viewModels()

    lateinit var adapter: TripPreviewPagerAdapter
    lateinit var binding: TripPreviewPagerBinding

    var currentPagerIndex = 0

    private var previewHeadersCallback: ((List<TripPreviewHeader>) -> Unit)? = null
    private var pageIndexStream: PublishSubject<Pair<Long, String>>? = null

    private var fromPageListener = false

    override fun onResume() {
        super.onResume()

        pageIndexStream?.observeOn(AndroidSchedulers.mainThread())
                ?.subscribeBy {
                    if (!fromPageListener) {
                        if (::adapter.isInitialized) {
                            val index = adapter.getSegmentPositionById(it)
                            if (index != -1) {
                                currentPagerIndex = index
                                binding.tripSegmentPager.currentItem = currentPagerIndex
                            }
                        }
                    } else {
                        fromPageListener = false
                    }
                }?.addTo(autoDisposable)
    }

    override fun onAttach(context: Context) {
        TripKitUI.getInstance().tripDetailsComponent().inject(this)
        super.onAttach(context)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        arguments?.let {
            load(
                    it.getString(ARG_TRIP_GROUP_ID, ""),
                    it.getString(ARG_TRIP_ID, ""),
                    it.getLong(ARG_TRIP_SEGMENT_ID, 0L),
                    it.getBoolean(ARG_FROM_TRIP_ACTION, false)
            )
        }

        savedInstanceState?.getInt(ARG_CURRENT_PAGER_INDEX)?.let {
            currentPagerIndex = it
            binding.tripSegmentPager.currentItem = currentPagerIndex
            savedInstanceState.remove(ARG_CURRENT_PAGER_INDEX)
        }

        viewModel.apply {
            observe(headers) {
                it?.let {
                    previewHeadersCallback?.invoke(it)
                    if (currentPagerIndex > 0) {
                        adapter.getSegmentByPosition(currentPagerIndex).let {
                            fromPageListener = true
                            pageIndexStream?.onNext(Pair(it.id, it.transportModeId.toString()))
                        }
                    }
                }
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(ARG_CURRENT_PAGER_INDEX, currentPagerIndex)
    }

    fun load(tripGroupId: String, tripId: String, tripSegmentId: Long, fromTripAction: Boolean) {
        tripGroupRepository.getTripGroup(tripGroupId)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ tripGroup ->

                    /*
                    //=== For testing isHideExactTimes purpose only while API is not yet updated ===
                    tripGroup.trips?.forEach { trip ->
                        trip.getSummarySegments().forEach { segment ->
                            if (segment.transportModeId == TransportMode.ID_WALK) {
                                segment.isHideExactTimes = true
                            }
                        }
                    }
                    // ===
                    */

                    val trip = tripGroup.trips?.find { it.uuid() == tripId }
                    trip?.let {

                        val list = ArrayList<TripGroup>()
                        list.add(tripGroup)
                        tripPreviewPagerListener?.reportPlannedTrip(trip, list)

                        viewModel.generatePreviewHeaders(
                                requireContext(),
                                it.getSummarySegments(),
                                getTransportIconTintStrategy,
                        )

                        var activeIndex =
                                adapter.setTripSegments(
                                        tripSegmentId,
                                        trip.segments
                                                .filter {
                                                    !it.isContinuation
                                                }
                                                .filter {
                                                    it.type != SegmentType.DEPARTURE &&
                                                            it.type != SegmentType.ARRIVAL
                                                },
                                        fromTripAction
                                )
                        adapter.notifyDataSetChanged()
                        if (currentPagerIndex != 0 && activeIndex != currentPagerIndex) {
                            activeIndex = currentPagerIndex
                        }
                        currentPagerIndex = activeIndex
                        binding.tripSegmentPager.currentItem = activeIndex
                    }
                }, {
                    it.printStackTrace()
                })
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
        binding.tripSegmentPager.offscreenPageLimit = 2
        setViewPagerListeners()
        adapter.externalActionCallback = { segment, action ->
            if (segment != null && action != null) {
                logAction(segment, action)
            }
        }

        adapter.onSwipePage = {
            if (it) {
                if (binding.tripSegmentPager.currentItem + 1 < adapter.pages.size) {
                    binding.tripSegmentPager.currentItem = binding.tripSegmentPager.currentItem + 1
                }
            } else {
                if (binding.tripSegmentPager.currentItem > 0) {
                    binding.tripSegmentPager.currentItem = binding.tripSegmentPager.currentItem - 1
                }
            }
        }
    }

    //TODO add viewmodel and move logic there (not just for this class).
    // Better for all logic to be moved in viewModel
    private fun logAction(segment: TripSegment, action: Action) {
        val trip = segment.trip
        lifecycleScope.launch {
            (segment.booking?.virtualBookingUrl ?: trip?.logURL)?.let {
                val result = bookingService.logTrip(it)
                if (result !is NetworkResponse.Success) {
                    result.logError()
                }
                proceedWithExternalAction(action)
            } ?: kotlin.run {
                proceedWithExternalAction(action)
            }
        }
    }

    private fun proceedWithExternalAction(action: Action) {
        if (action.appInstalled) {
            action.data?.let {
                activity?.startActivity(requireContext().packageManager.getLaunchIntentForPackage(it))
            }
        } else {
            action.data?.let { dataUrl ->
                try {
                    activity?.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(dataUrl)))
                } catch (e: ActivityNotFoundException) {
                    action.fallbackUrl?.let { fallbackUrl ->
                        activity?.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(fallbackUrl)))
                    }
                }
            }
        }
    }

    private fun setViewPagerListeners() {
        binding.tripSegmentPager.addOnPageChangeListener(object : ViewPager.OnPageChangeListener {
            override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {}

            override fun onPageSelected(position: Int) {
                val selectedFragment = adapter.instantiateItem(binding.tripSegmentPager, position)
                (selectedFragment as? BaseTripKitFragment)?.let {
                    it.onCloseButtonListener = this@TripPreviewPagerFragment.onCloseButtonListener
                    it.tripPreviewPagerListener = this@TripPreviewPagerFragment.tripPreviewPagerListener
                    it.refresh(position)
                }
                currentPagerIndex = position
                adapter.getSegmentByPosition(position).let {
                    fromPageListener = true
                    pageIndexStream?.onNext(Pair(it.id, it.transportModeId.toString()))
                }
                if (selectedFragment is TimetableFragment) {
                    adapter.bookingActions?.let {
                        selectedFragment.setBookingActions(it)
                    }
                }
            }

            override fun onPageScrollStateChanged(state: Int) {}
        })
    }

    fun setTripSegment(segment: TripSegment, tripSegments: List<TripSegment>) {

        /*
        //=== For testing isHideExactTimes purpose only while API is not yet updated ===
        tripSegments.forEach { segment ->
            if (segment.transportModeId == TransportMode.ID_WALK) {
                segment.isHideExactTimes = true
            }
        }
        // ===
        */

        tripPreviewPagerListener?.reportPlannedTrip(segment.trip, listOf(segment.trip.group))

        adapter.setTripSegments(
                segment.id,
                tripSegments
                        .filter {
                            !it.isContinuation
                        }.filter {
                            it.type != SegmentType.DEPARTURE && it.type != SegmentType.ARRIVAL
                        }
        )

        viewModel.generatePreviewHeaders(
                requireContext(),
                segment.trip.getSummarySegments(),
                getTransportIconTintStrategy,
        )

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
                    //adapter.timetableFragment?.setBookingActions(segment.booking?.externalActions)
                    adapter.bookingActions = segment.booking?.externalActions
                    adapter.segmentActionStream.onNext(segment)
                }
            }
        }
    }

    fun updateListener(tripPreviewPagerListener: Listener) {
        this.tripPreviewPagerListener = tripPreviewPagerListener
    }

    interface Listener {
        fun onServiceActionButtonClicked(_tripSegment: TripSegment?, action: String?)
        fun onTimetableEntryClicked(segment: TripSegment?, scope: CoroutineScope, entry: TimetableEntry)
        fun reportPlannedTrip(trip: Trip?, tripGroups: List<TripGroup>)
        fun onBottomSheetResize(): MutableLiveData<Int>
        fun onRestartHomePage()
        @Deprecated("UnusedClass")
        fun onExternalActionButtonClicked(action: String?)
    }

    companion object {

        const val ARG_CURRENT_PAGER_INDEX = "_a_current_pager_index"
        const val TAG = "tripPreview"

        fun newInstance(
                tripGroupId: String,
                tripId: String,
                tripSegmentHashCode: Long,
                tripPreviewPagerListener: Listener,
                fromAction: Boolean = false,
                pageIndexStream: PublishSubject<Pair<Long, String>>? = null,
                previewHeadersCallback: ((List<TripPreviewHeader>) -> Unit)? = null): TripPreviewPagerFragment {
            val fragment = TripPreviewPagerFragment()
            fragment.arguments = bundleOf(
                    ARG_TRIP_GROUP_ID to tripGroupId,
                    ARG_TRIP_ID to tripId,
                    ARG_TRIP_SEGMENT_ID to tripSegmentHashCode,
                    ARG_FROM_TRIP_ACTION to fromAction
            )
            fragment.tripPreviewPagerListener = tripPreviewPagerListener
            fragment.pageIndexStream = pageIndexStream
            fragment.previewHeadersCallback = previewHeadersCallback
            return fragment
        }
    }
}