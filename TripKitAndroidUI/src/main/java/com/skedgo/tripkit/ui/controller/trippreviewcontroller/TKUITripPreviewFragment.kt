package com.skedgo.tripkit.ui.controller.trippreviewcontroller

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.viewpager.widget.ViewPager
import com.haroldadmin.cnradapter.NetworkResponse
import com.skedgo.tripkit.routing.SegmentType
import com.skedgo.tripkit.routing.Trip
import com.skedgo.tripkit.routing.TripGroup
import com.skedgo.tripkit.routing.TripSegment
import com.skedgo.tripkit.routing.getSummarySegments
import com.skedgo.tripkit.ui.tripresult.findSelectedTrip
import com.skedgo.tripkit.ui.tripresult.stableKey
import com.skedgo.tripkit.ui.R
import com.skedgo.tripkit.ui.TripKitUI
import com.skedgo.tripkit.ui.booking.apiv2.BookingV2TrackingService
import com.skedgo.tripkit.ui.core.BaseFragment
import com.skedgo.tripkit.ui.core.BaseTripKitFragment
import com.skedgo.tripkit.ui.core.addTo
import com.skedgo.tripkit.ui.core.logError
import com.skedgo.tripkit.ui.databinding.FragmentTkuiTripPreviewBinding
import com.skedgo.tripkit.ui.payment.PaymentData
import com.skedgo.tripkit.ui.timetables.TimetableFragment
import com.skedgo.tripkit.ui.trippreview.Action
import com.skedgo.tripkit.ui.trippreview.TripPreviewPagerListener
import com.skedgo.tripkit.ui.trippreview.TripPreviewPagerViewModel
import com.skedgo.tripkit.ui.trippreview.TripPreviewSharedViewModel
import com.skedgo.tripkit.ui.trippreview.segment.TripSegmentsSummaryData
import com.skedgo.tripkit.ui.tripresults.GetTransportIconTintStrategy
import com.skedgo.tripkit.ui.utils.ITEM_SERVICE
import com.skedgo.tripkit.ui.utils.correctItemType
import com.skedgo.tripkit.ui.utils.observe
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.subjects.PublishSubject
import kotlinx.coroutines.launch
import javax.inject.Inject

class TKUITripPreviewFragment : BaseFragment<FragmentTkuiTripPreviewBinding>() {

    @Inject
    lateinit var bookingService: BookingV2TrackingService

    @Inject
    lateinit var getTransportIconTintStrategy: GetTransportIconTintStrategy

    @Inject
    lateinit var viewModel: TripPreviewPagerViewModel

    private val sharedViewModel: TripPreviewSharedViewModel by viewModels({
        val parent = requireParentFragment()
        if (parent is com.skedgo.tripkit.ui.controller.homeviewcontroller.TKUIHomeBottomSheetFragment)
            parent.requireParentFragment() else parent
    })

    lateinit var adapter: TripPreviewPagerAdapter

    private var currentPagerIndex = 0
    private var previewHeadersCallback: ((TripSegmentsSummaryData) -> Unit)? = null
    private var paymentDataStream: PublishSubject<PaymentData>? = null
    private var ticketActionStream: PublishSubject<String>? = null

    private var tripGroupId: String = ""
    private var tripId: String = ""
    private var tripStableKey: String? = null
    private var tripNumericId: Long = 0
    private var tripSegmentHashCode: Long = 0L
    private var fromTripAction: Boolean = false

    private var selectedSegmentId: Long? = null
    private var updatingPages = false
    private var fromReload = false

    var latestTrip: Trip? = null

    override val layoutRes: Int
        get() = R.layout.fragment_tkui_trip_preview

    override val observeAccessibility: Boolean = false

    override fun getDefaultViewForAccessibility(): View? = null

    override fun onAttach(context: Context) {
        TripKitUI.getInstance().controllerComponent().inject(this)
        super.onAttach(context)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            tripGroupId = it.getString("tkui_group").orEmpty()
            tripId = it.getString("tkui_trip").orEmpty()
            tripSegmentHashCode = it.getLong("tkui_segment")
            fromTripAction = it.getBoolean("tkui_from_action")
        }
        tripStableKey = savedInstanceState?.getString("tkui_trip_key") ?: arguments?.getString("tkui_trip_key")
        tripNumericId = (savedInstanceState ?: arguments)?.getLong("tkui_trip_numeric") ?: 0L
        currentPagerIndex = savedInstanceState?.getInt(ARG_CURRENT_PAGER_INDEX) ?: currentPagerIndex
        if (savedInstanceState?.containsKey("tkui_selected_segment") == true)
            selectedSegmentId = savedInstanceState.getLong("tkui_selected_segment")
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(ARG_CURRENT_PAGER_INDEX, currentPagerIndex)
        selectedSegmentId?.let { outState.putLong("tkui_selected_segment", it) }
        outState.putString("tkui_trip_key", latestTrip?.stableKey ?: tripStableKey)
        outState.putLong("tkui_trip_numeric", latestTrip?.tripId ?: tripNumericId)
    }

    internal fun reconnectHost(listener: TripPreviewPagerListener, headers: (TripSegmentsSummaryData) -> Unit) {
        tripPreviewPagerListener = listener
        previewHeadersCallback = headers
        if (::adapter.isInitialized) adapter.tripPreviewPagerListener = listener
        viewModel.headersData.value?.let(headers)
        latestTrip?.let { listener.reportPlannedTrip(it, listOfNotNull(viewModel.tripGroup.value)) }
    }

    override fun clearInstances() {
        super.clearInstances()
        previewHeadersCallback = null
        paymentDataStream = null
        ticketActionStream = null
        latestTrip = null
    }

    override fun onCreated(savedInstance: Bundle?) {
        updateAdapter()
        initData(savedInstance)
        initObservers()
    }

    override fun onResume() {
        super.onResume()
        // BaseFragment may reuse its binding without invoking onCreated again.
        viewLifecycleOwner.observe(sharedViewModel.pageIndex) { selection ->
            selection?.let(::selectHeaderSegment)
        }
    }

    internal fun selectHeaderSegment(selection: Pair<Long, String>) {
        if (!::adapter.isInitialized || updatingPages) return
        val current = adapter.pages.getOrNull(currentPagerIndex)
        val index = if (current?.tripSegment?.segmentId == selection.first) currentPagerIndex
            else adapter.getSegmentPositionById(selection)
        if (index >= 0) {
            binding.vpTripPreview.setCurrentItem(index, false)
            publishPageSelection(index)
        }
    }

    private fun initObservers() {
        viewModel.apply {
            observe(tripGroup) {
                it?.let { tripGroup ->
                    generateTripList(tripGroup)
                }
            }

            observe(tripGroupFromPolling) {
                it?.let { tripGroup ->
                    val trip = tripGroup.findSelectedTrip(tripId, tripNumericId, tripStableKey)
                    trip?.let { latestTrip = trip }
                }
            }

            observe(headersData) {
                it?.let {
                    previewHeadersCallback?.invoke(it)
                    publishPageSelection(currentPagerIndex)
                }
            }
        }
    }

    private fun generateTripList(tripGroup: TripGroup) {
        if (fromReload) return

        val trip = tripGroup.findSelectedTrip(tripId, tripNumericId, tripStableKey)
        trip?.let {
            latestTrip = trip
            tripStableKey = trip.stableKey
            tripNumericId = trip.tripId
            val list = ArrayList<TripGroup>()
            list.add(tripGroup)
            tripPreviewPagerListener?.reportPlannedTrip(trip, list)

            viewModel.generatePreviewHeaders(
                requireContext(),
                it.getSummarySegments(),
                getTransportIconTintStrategy,
            )

            updatingPages = true
            var activeIndex =
                adapter.setTripSegments(
                    selectedSegmentId ?: tripSegmentHashCode,
                    trip.segmentList
                        .filter {
                            !it.isContinuation
                        }
                        .filter {
                            it.getType() != SegmentType.DEPARTURE &&
                                it.getType() != SegmentType.ARRIVAL
                        },
                    fromTripAction
                )
            adapter.notifyDataSetChanged()
            if (selectedSegmentId != null && adapter.pages.getOrNull(currentPagerIndex)?.tripSegment?.segmentId == selectedSegmentId)
                activeIndex = currentPagerIndex
            activeIndex = activeIndex.coerceIn(0, adapter.pages.lastIndex.coerceAtLeast(0))
            currentPagerIndex = activeIndex
            binding.vpTripPreview.setCurrentItem(activeIndex, false)
            updatingPages = false
            publishPageSelection(activeIndex)
        }
    }

    private fun initData(savedInstance: Bundle?) {
        viewModel.loadTripGroup(tripGroupId)
        viewModel.startUpdateTripPolling(tripGroupId)

        savedInstance?.getInt(ARG_CURRENT_PAGER_INDEX)?.let {
            currentPagerIndex = it
            binding.vpTripPreview.currentItem = currentPagerIndex
            savedInstance.remove(ARG_CURRENT_PAGER_INDEX)
        }
    }

    private fun publishPageSelection(position: Int) {
        val segment = adapter.pages.getOrNull(position)?.tripSegment ?: return
        currentPagerIndex = position
        selectedSegmentId = segment.segmentId
        val selection = segment.segmentId to segment.transportModeId.toString()
        if (sharedViewModel.pageIndex.value != selection)
            sharedViewModel.setPageIndex(selection.first, selection.second)
    }

    private fun updateAdapter() {
        adapter = TripPreviewPagerAdapter(childFragmentManager)
        adapter.onCloseButtonListener = this.onCloseButtonListener
        adapter.tripPreviewPagerListener = this.tripPreviewPagerListener
        adapter.paymentDataStream = this.paymentDataStream
        adapter.ticketActionStream = this.ticketActionStream
        binding.vpTripPreview.adapter = adapter
        binding.vpTripPreview.offscreenPageLimit = 2
        setViewPagerListeners()
        adapter.externalActionCallback = { segment, action ->
            if (segment != null && action != null) {
                logAction(segment, action)
            }
        }

        adapter.onSwipePage = {
            if (it) {
                if (binding.vpTripPreview.currentItem + 1 < adapter.pages.size) {
                    binding.vpTripPreview.currentItem = binding.vpTripPreview.currentItem + 1
                }
            } else {
                if (binding.vpTripPreview.currentItem > 0) {
                    binding.vpTripPreview.currentItem = binding.vpTripPreview.currentItem - 1
                }
            }
        }

        adapter.bottomSheetDragToggleCallback = {
            tripPreviewPagerListener?.onToggleBottomSheetDrag(it)
        }
    }

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
        binding.vpTripPreview.addOnPageChangeListener(object : ViewPager.OnPageChangeListener {
            override fun onPageScrolled(
                position: Int,
                positionOffset: Float,
                positionOffsetPixels: Int
            ) {
            }

            override fun onPageSelected(position: Int) {
                if (updatingPages || position != binding.vpTripPreview.currentItem || position !in adapter.pages.indices) return
                val selectedFragment = adapter.instantiateItem(binding.vpTripPreview, position)
                (selectedFragment as? BaseTripKitFragment)?.let {
                    it.onCloseButtonListener = this@TKUITripPreviewFragment.onCloseButtonListener
                    it.tripPreviewPagerListener =
                        this@TKUITripPreviewFragment.tripPreviewPagerListener
                    it.refresh(position)
                }
                publishPageSelection(position)

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
        fromReload = true
        tripPreviewPagerListener?.reportPlannedTrip(segment.trip, listOf(segment.trip?.group).filterNotNull())

        updatingPages = true
        val index = adapter.setTripSegments(
            selectedSegmentId ?: segment.segmentId,
            tripSegments
                .filter {
                    !it.isContinuation
                }.filter {
                    it.getType() != SegmentType.DEPARTURE && it.getType() != SegmentType.ARRIVAL
                }
        )

        val restoredIndex = if (adapter.pages.getOrNull(currentPagerIndex)?.tripSegment?.segmentId == selectedSegmentId)
            currentPagerIndex else index.coerceIn(0, adapter.pages.lastIndex.coerceAtLeast(0))
        binding.vpTripPreview.setCurrentItem(restoredIndex, false)
        updatingPages = false
        publishPageSelection(restoredIndex)

        segment.trip?.let { trip ->
            viewModel.generatePreviewHeaders(
                requireContext(),
                trip.getSummarySegments(),
                getTransportIconTintStrategy,
            )

            viewModel.updateTrip(trip.group?.uuid().orEmpty(), trip.uuid, trip)
        }
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

    fun getCurrentPagerItemType(): Int {
        return if (::adapter.isInitialized) adapter.pages.getOrNull(currentPagerIndex)?.type ?: -1 else -1
    }

    fun updateListener(tripPreviewPagerListener: TripPreviewPagerListener) {
        this.tripPreviewPagerListener = tripPreviewPagerListener
    }

    companion object {
        const val ARG_CURRENT_PAGER_INDEX = "_a_current_pager_index"
        const val TAG = "tripPreview"

        fun newInstance(
            tripGroupId: String,
            tripId: String,
            tripSegmentHashCode: Long,
            tripPreviewPagerListener: TripPreviewPagerListener,
            fromAction: Boolean = false,
            paymentDataStream: PublishSubject<PaymentData>? = null,
            ticketActionStream: PublishSubject<String>? = null,
            previewHeadersCallback: ((TripSegmentsSummaryData) -> Unit)? = null
        ) = TKUITripPreviewFragment().apply {
            arguments = Bundle().apply {
                putString("tkui_group", tripGroupId)
                putString("tkui_trip", tripId)
                putLong("tkui_segment", tripSegmentHashCode)
                putBoolean("tkui_from_action", fromAction)
            }
            this.tripGroupId = tripGroupId
            this.tripId = tripId
            this.tripSegmentHashCode = tripSegmentHashCode
            this.fromTripAction = fromAction
            this.tripPreviewPagerListener = tripPreviewPagerListener
            this.paymentDataStream = paymentDataStream
            this.ticketActionStream = ticketActionStream
            this.previewHeadersCallback = previewHeadersCallback
        }
    }
}