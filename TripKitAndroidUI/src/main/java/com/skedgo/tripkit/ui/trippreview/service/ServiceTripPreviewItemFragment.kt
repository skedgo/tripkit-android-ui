package com.skedgo.tripkit.ui.trippreview.service

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.skedgo.tripkit.common.model.TransportMode
import com.skedgo.tripkit.common.model.realtimealert.RealtimeAlert
import com.skedgo.tripkit.data.regions.RegionService
import com.skedgo.tripkit.routing.SegmentType
import com.skedgo.tripkit.routing.Trip
import com.skedgo.tripkit.routing.TripGroup
import com.skedgo.tripkit.routing.TripSegment
import com.skedgo.tripkit.ui.R
import com.skedgo.tripkit.ui.TripKitUI
import com.skedgo.tripkit.ui.core.BaseFragment
import com.skedgo.tripkit.ui.core.addTo
import com.skedgo.tripkit.ui.databinding.TripPreviewServiceItemBinding
import com.skedgo.tripkit.ui.servicedetail.AlertClickListener
import com.skedgo.tripkit.ui.servicedetail.ServiceDetailViewModel
import com.skedgo.tripkit.ui.timetables.FetchAndLoadTimetable
import com.skedgo.tripkit.ui.tripresults.GetTransportIconTintStrategy
import com.skedgo.tripkit.ui.utils.createSummaryIcon
import com.skedgo.tripkit.ui.utils.getSegmentIconObservable
import com.skedgo.tripkit.ui.utils.OnSwipeTouchListener
import io.reactivex.android.schedulers.AndroidSchedulers
import javax.inject.Inject


class ServiceTripPreviewItemFragment : BaseFragment<TripPreviewServiceItemBinding>() {
    var time = 0L

    var segment: TripSegment? = null

    @Inject
    lateinit var fetchAndLoadTimetable: FetchAndLoadTimetable

    @Inject
    lateinit var regionService: RegionService

    @Inject
    lateinit var viewModel: ServiceDetailViewModel

    @Inject
    lateinit var prefs: SharedPreferences

    var positionInAdapter = 0

    private var showCloseButton = false

    override val layoutRes: Int
        get() = R.layout.trip_preview_service_item

    override val observeAccessibility: Boolean = false

    override fun getDefaultViewForAccessibility(): View? = null

    override fun refresh(position: Int) {
        positionInAdapter = position
        handleSegment()
    }

    override fun onAttach(context: Context) {
        TripKitUI.getInstance().tripPreviewComponent().inject(this)
        super.onAttach(context)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        //using shared pref to save the data since data too large for intent to handle
        segment?.let {
            with(prefs.edit()) {
                putString("${positionInAdapter}_$ARGS_TRIP_SEGMENT", gson.toJson(it))
                putString("${positionInAdapter}_$ARGS_TRIP_SEGMENT_TRIP", gson.toJson(it.trip))
                putString(
                    "${positionInAdapter}_$ARGS_TRIP_SEGMENT_TRIP_GROUP",
                    gson.toJson(it.trip?.group)
                )
                putBoolean("${positionInAdapter}_$ARGS_SHOW_CLOSE_BUTTON", showCloseButton)
                apply()
            }
        }
    }

    override fun onCreated(savedInstance: Bundle?) {
        binding.viewModel = viewModel
        binding.lifecycleOwner = viewLifecycleOwner
        binding.content.occupancyList.layoutManager =
            LinearLayoutManager(context, RecyclerView.HORIZONTAL, false)

        binding.content.occupancyList.isNestedScrollingEnabled = false
        binding.content.recyclerView.isNestedScrollingEnabled = true

        val swipeListener = OnSwipeTouchListener(requireContext(),
            object : OnSwipeTouchListener.SwipeGestureListener {
                override fun onSwipeRight() {
                    onNextPage?.invoke()
                }

                override fun onSwipeLeft() {
                    onPreviousPage?.invoke()
                }
            })

        swipeListener.touchCallback = { v, event ->
            v?.parent?.requestDisallowInterceptTouchEvent(true)
            v?.onTouchEvent(event)
        }

        binding.content.recyclerView.setOnTouchListener(swipeListener)

        binding.closeButton.setOnClickListener(onCloseButtonListener)

        handleSegment()

        viewModel.alertClickListener = object : AlertClickListener {
            override fun onAlertClick(alert: RealtimeAlert) {
                alert.url()?.let { url ->
                    requireActivity().startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                }
            }
        }
    }

    private fun handleSegment() {
        segment?.let {
            viewModel.setup(it)
            updateTitleIcon(it)
        } ?: kotlin.run {
            checkSegmentOnPrefs()
        }
        viewModel.showCloseButton.set(showCloseButton)
    }

    private fun checkSegmentOnPrefs() {

        with(prefs) {
            var segment: TripSegment? = null
            if (contains("${positionInAdapter}_$ARGS_TRIP_SEGMENT")) {
                segment = gson.fromJson(
                    getString("${positionInAdapter}_$ARGS_TRIP_SEGMENT", ""),
                    TripSegment::class.java
                )
                prefs.edit().remove("${positionInAdapter}_$ARGS_TRIP_SEGMENT").apply()
            }

            var trip: Trip? = null
            if (contains("${positionInAdapter}_$ARGS_TRIP_SEGMENT_TRIP")) {
                trip = gson.fromJson(
                    getString("${positionInAdapter}_$ARGS_TRIP_SEGMENT_TRIP", ""),
                    Trip::class.java
                )
                prefs.edit().remove("${positionInAdapter}_$ARGS_TRIP_SEGMENT_TRIP").apply()
            }

            var tripSegmentGroup: TripGroup? = null
            if (contains("${positionInAdapter}_$ARGS_TRIP_SEGMENT_TRIP_GROUP")) {
                tripSegmentGroup = gson.fromJson(
                    getString(
                        "${positionInAdapter}_$ARGS_TRIP_SEGMENT_TRIP_GROUP",
                        ""
                    ), TripGroup::class.java
                )
                prefs.edit().remove("${positionInAdapter}_$ARGS_TRIP_SEGMENT_TRIP").apply()
            }
            trip?.group = tripSegmentGroup
            if(trip != null) {
                segment?.trip = trip
            }
            segment?.let {
                this@ServiceTripPreviewItemFragment.segment = it
                viewModel.setup(it)
                updateTitleIcon(it)
            }

            if (contains("${positionInAdapter}_$ARGS_SHOW_CLOSE_BUTTON")) {
                showCloseButton = getBoolean("${positionInAdapter}_$ARGS_SHOW_CLOSE_BUTTON", false)
                prefs.edit().remove("${positionInAdapter}_$ARGS_SHOW_CLOSE_BUTTON").apply()
            }
        }
    }

    private fun updateTitleIcon(segment: TripSegment) {
        val canUseHeaderPipeline = segment.modeInfo?.localIconName != null && segment.darkVehicleIcon != 0
        if (!canUseHeaderPipeline) {
            val fallback = getLegacyFallbackIcon(segment)
            binding.titleIcon.setImageDrawable(fallback)
            binding.titleIcon.visibility = if (fallback != null) View.VISIBLE else View.GONE
            return
        }

        segment.getSegmentIconObservable(
            requireContext(),
            GetTransportIconTintStrategy(resources)
        ).map { drawable ->
            drawable?.let { segment.createSummaryIcon(requireContext(), it) }
        }.observeOn(AndroidSchedulers.mainThread())
            .subscribe({ summaryIcon ->
                val finalIcon = summaryIcon ?: getLegacyFallbackIcon(segment)
                binding.titleIcon.setImageDrawable(finalIcon)
                binding.titleIcon.visibility = if (finalIcon != null) View.VISIBLE else View.GONE
            }, {
                val fallback = getLegacyFallbackIcon(segment)
                binding.titleIcon.setImageDrawable(fallback)
                binding.titleIcon.visibility = if (fallback != null) View.VISIBLE else View.GONE
            }).addTo(autoDisposable)
    }

    private fun getLegacyFallbackIcon(segment: TripSegment): android.graphics.drawable.Drawable? {
        val localIconResId = TransportMode.getLocalIconResId(segment.transportModeId)
        return when {
            segment.getType() == SegmentType.ARRIVAL || segment.getType() == SegmentType.DEPARTURE ->
                ContextCompat.getDrawable(requireContext(), R.drawable.v4_ic_map_location)
            localIconResId != 0 -> ContextCompat.getDrawable(requireContext(), localIconResId)
            segment.action?.contains("<TIME>: Wait") == true ->
                ContextCompat.getDrawable(requireContext(), R.drawable.ic_wait)
            else -> null
        }
    }

    companion object {

        const val ARGS_TRIP_SEGMENT = "tripSegment"
        const val ARGS_TRIP_SEGMENT_TRIP = "tripSegmentTri"
        const val ARGS_TRIP_SEGMENT_TRIP_GROUP = "tripSegmentTripGroup"
        const val ARGS_SHOW_CLOSE_BUTTON = "showCloseButton"

        fun newInstance(
            segment: TripSegment,
            position: Int,
            showCloseButton: Boolean = false
        ): ServiceTripPreviewItemFragment {
            val fragment = ServiceTripPreviewItemFragment()
            fragment.showCloseButton = showCloseButton
            fragment.segment = segment
            fragment.positionInAdapter = position
            return fragment
        }
    }
}